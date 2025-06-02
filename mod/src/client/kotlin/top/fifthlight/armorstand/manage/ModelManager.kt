package top.fifthlight.armorstand.manage

import com.mojang.logging.LogUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.fabricmc.loader.api.FabricLoader
import top.fifthlight.armorstand.ArmorStand
import top.fifthlight.armorstand.util.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.*
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.*
import kotlin.io.path.*
import kotlin.time.measureTime

object ModelManager {
    private val LOGGER = LogUtils.getLogger()
    val modelDir: Path = FabricLoader.getInstance().gameDir.resolve("models").toAbsolutePath()
    private const val DATABASE_NAME = ".cache"
    private val databaseFile = modelDir.resolve("$DATABASE_NAME.mv.db").toAbsolutePath()
    private const val DATABASE_VERSION = 0
    private val databaseDispatcher = Dispatchers.IO.limitedParallelism(1)

    private var _connection: Connection? = null

    suspend fun <T> transaction(block: suspend Connection.() -> T): T {
        val connection = _connection ?: error("Database is not connected")
        return withContext(databaseDispatcher) {
            connection.transaction { block() }
        }
    }

    private fun connect(scope: CoroutineScope) {
        if (_connection != null) {
            return
        }
        Class.forName("org.h2.Driver")
        val databaseRelativePath = databaseFile.relativeTo(Paths.get(".").toAbsolutePath())
        val connection = DriverManager.getConnection(
            "jdbc:h2:./${databaseRelativePath.toString().removeSuffix(".mv.db")}",
            Properties()
        )
        _connection = connection
        scope.launch {
            try {
                awaitCancellation()
            } finally {
                LOGGER.info("Database closed")
                connection.close()
            }
        }
        runCatching {
            Files.setAttribute(databaseFile, "dos:hidden", true)
        }
        LOGGER.info("Opened database")
    }

    private fun Connection.checkVersionMatches(): Boolean {
        query("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE LOWER(TABLE_NAME) = 'version';").use { result ->
            result.skipToInitialRow()
            if (result.getInt(1) < 1) {
                LOGGER.info("No version item in database, create new tables")
                return false
            }
        }
        query("SELECT version FROM version;").use { result ->
            result.skipToInitialRow()
            val version = result.getInt(1)
            if (version != DATABASE_VERSION) {
                LOGGER.info("Version not match, recreate tables")
                return false
            }
            LOGGER.info("Database version {}", version)
        }
        return true
    }

    private suspend fun createTables() {
        transaction {
            if (checkVersionMatches()) {
                return@transaction
            }
            execute("DROP TABLE IF EXISTS version;")
            execute("DROP TABLE IF EXISTS model;")
            execute("CREATE TABLE version (version INTEGER);")
            prepare("INSERT INTO version (version) VALUES (?);") { setInt(1, DATABASE_VERSION) }
            execute("CREATE TABLE model (path VARCHAR PRIMARY KEY, lastChanged BIGINT NOT NULL, sha256 BINARY(32) NOT NULL);")
            LOGGER.info("Created tables")
        }
    }

    private fun ResultSet.readModelItem(): ModelItem? {
        val path = Path.of(getString(1)).normalize()
        if (!modelDir.resolve(path).exists()) {
            return null
        }
        val lastChanged = getLong(2)
        val sha256 = getBytes(3)
        return ModelItem(
            path = path,
            lastChanged = lastChanged,
            sha256 = sha256,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getModel(): Flow<List<ModelItem>> = lastScanTime.mapLatest {
        if (it == null) {
            scheduleScan()
            awaitCancellation()
        } else {
            transaction {
                query("SELECT path, lastChanged, sha256 FROM model ORDER BY lastChanged DESC").use { result ->
                    buildList {
                        while (result.next()) {
                            result.readModelItem()?.let { add(it) }
                        }
                    }
                }
            }
        }
    }

    suspend fun getModel(path: Path) = transaction {
        prepareQuery("SELECT path, lastChanged, sha256 FROM model WHERE path = ?") {
            setString(1, path.normalize().toString())
        }.use { result ->
            if (result.next()) {
                result.readModelItem()
            } else {
                null
            }
        }
    }

    suspend fun getModel(sha256: ByteArray) = transaction {
        prepareQuery("SELECT path, lastChanged, sha256 FROM model WHERE sha256 = ?") {
            setBytes(1, sha256)
        }.use { result ->
            if (result.next()) {
                result.readModelItem()
            } else {
                null
            }
        }
    }

    private fun calculateSha256(path: Path): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        FileChannel.open(path).use { channel ->
            val buffer = ByteBuffer.allocate(64 * 1024)
            while (true) {
                buffer.clear()
                if (channel.read(buffer) == -1) {
                    break
                }
                buffer.flip()
                digest.update(buffer)
            }
        }
        return digest.digest()
    }

    private val lastScanTime = MutableStateFlow<Instant?>(null)
    private suspend fun doScan() = withContext(Dispatchers.IO) {
        suspend fun processFile(relativePath: Path) {
            val pathStr = relativePath.normalize().toString()
            val path = modelDir.resolve(relativePath)
            val lastChanged = path.getLastModifiedTime().toMillis()
            transaction {
                LOGGER.trace("Process file {}", relativePath)
                prepareQuery("SELECT COUNT(*) FROM model WHERE path = ? AND lastChanged = ? LIMIT 1;") {
                    setString(1, pathStr)
                    setLong(2, lastChanged)
                }.use { result ->
                    result.skipToInitialRow()
                    if (result.getInt(1) > 0) {
                        LOGGER.trace("File already in cache, skip.")
                        return@transaction
                    }
                }
            }
            val sha256 = calculateSha256(path)
            LOGGER.trace("SHA-256 for file {} is {}", relativePath, sha256.toHexString())
            transaction {
                prepare("DELETE FROM model WHERE path = ?;") {
                    setString(1, pathStr)
                }
                prepare("INSERT INTO model (path, lastChanged, sha256) VALUES (?, ?, ?);") {
                    setString(1, pathStr)
                    setLong(2, lastChanged)
                    setBytes(3, sha256)
                }
            }
        }

        val time = measureTime {
            coroutineScope {
                val processDispatcher = Dispatchers.IO.limitedParallelism(4)
                modelDir.visitFileTree {
                    onVisitFile { file, _ ->
                        if (file.extension in ModelLoaders.modelExtensions) {
                            launch {
                                withContext(processDispatcher) {
                                    try {
                                        processFile(file.toAbsolutePath().relativeTo(modelDir))
                                    } catch (ex: Exception) {
                                        LOGGER.warn("Failed to process file $file", ex)
                                    }
                                }
                            }
                        }
                        FileVisitResult.CONTINUE
                    }
                }
            }
        }
        lastScanTime.value = Clock.System.now()
        LOGGER.info("Finish scanning models, took $time")
    }

    private var scanning = false
    private var scanScheduled = false
    private val scanLock = Mutex()
    private val scanScope = ArmorStand.instance.scope + Dispatchers.IO
    suspend fun scheduleScan(): Unit = scanLock.withLock {
        if (scanning) {
            scanScheduled = true
            return
        }
        scanScheduled = false
        scanning = true
        scanScope.launch {
            try {
                doScan()
                scanLock.withLock {
                    if (scanScheduled) {
                        scanScheduled = false
                        launch {
                            scheduleScan()
                        }
                    }
                    scanning = false
                }
            } catch (ex: Exception) {
                scanLock.withLock {
                    scanning = false
                    scanScheduled = false
                }
                LOGGER.warn("Failed to scan model", ex)
            }
        }
    }

    private fun listenModelDir(scope: CoroutineScope) = scope.launch {
        withContext(Dispatchers.IO) {
            var watcher: WatchService? = null
            var watchKey: WatchKey? = null
            try {
                watcher = FileSystems.getDefault().newWatchService().also { watcher ->
                    modelDir.register(
                        watcher,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                    )
                }
                while (true) {
                    watchKey = watcher.take()
                    for (event in watchKey.pollEvents()) {
                        when (event.kind()) {
                            StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE -> Unit
                            else -> continue
                        }

                        val file = event.context() as Path
                        if (file == databaseFile.relativeTo(modelDir)) {
                            continue
                        }

                        val extension = file.extension
                        if (extension !in ModelLoaders.modelExtensions && extension !in ModelLoaders.animationExtensions) {
                            continue
                        }

                        scheduleScan()
                        break
                    }
                    if (!watchKey.reset()) {
                        break
                    }
                }
            } catch (ex: Exception) {
                LOGGER.warn("Failed to watch model directory", ex)
            } finally {
                watchKey?.cancel()
                watcher?.close()
            }
        }
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        modelDir.createDirectories()
        val scope = ArmorStand.instance.scope
        try {
            connect(scope)
        } catch (ex: Exception) {
            try {
                LOGGER.warn("Failed to open database, delete existing database and retry")
                databaseFile.deleteIfExists()
                connect(scope)
            } catch (ex: Exception) {
                throw RuntimeException("Failed to open model database", ex)
            }
        }
        createTables()
        listenModelDir(scope)
        scheduleScan()
    }
}