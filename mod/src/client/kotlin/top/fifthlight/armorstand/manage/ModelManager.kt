package top.fifthlight.armorstand.manage

import com.mojang.logging.LogUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.minecraft.util.Identifier
import top.fifthlight.armorstand.ArmorStand
import top.fifthlight.armorstand.state.ModelInstanceManager
import top.fifthlight.armorstand.util.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.*
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.io.path.*
import kotlin.time.measureTime

object ModelManager {
    private val LOGGER = LogUtils.getLogger()
    val modelDir: Path
        get() = ModelInstanceManager.modelDir
    private const val DATABASE_NAME = ".cache"
    private val databaseFile = modelDir.resolve("$DATABASE_NAME.mv.db").toAbsolutePath()
    private const val DATABASE_VERSION = 0
    var connectionPool: Pool<Connection>? = null
        private set

    inline fun <T> transaction(crossinline block: Connection.() -> T): T {
        val connectionPool = connectionPool ?: error("Database is not connected")
        return connectionPool.transaction(block)
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

    private fun ResultSet.readModelItem() = ModelItem(
        path = Path.of(getString(1)).normalize(),
        name = getString(2),
        lastChanged = getLong(3),
        hash = ModelHash(getBytes(4)),
    )

    private suspend fun waitUntilFirstScan(): Instant {
        if (_lastScanTime.value == null) {
            scheduleScan()
        }
        return _lastScanTime.mapNotNull { it }.first()
    }

    suspend fun getTotalModels(
        searchString: String? = null,
    ): Int {
        waitUntilFirstScan()
        return transaction {
            if (searchString == null) {
                query("SELECT COUNT(*) FROM model")
            } else {
                prepareQuery("SELECT COUNT(*) FROM model WHERE LOCATE(LOWER(?), LOWER(name)) > 0") {
                    setString(1, searchString)
                }
            }.use { result ->
                result.skipToInitialRow()
                result.getInt(1)
            }
        }
    }

    enum class Order {
        NAME,
        LAST_CHANGED,
    }

    suspend fun getModel(
        offset: Int,
        length: Int,
        searchString: String? = null,
        order: Order = Order.NAME,
        ascend: Boolean = true,
    ): List<ModelItem> {
        waitUntilFirstScan()
        return transaction {
            val order = when (order) {
                Order.NAME -> "name"
                Order.LAST_CHANGED -> "lastChanged"
            }
            val sort = when (ascend) {
                true -> "ASC"
                false -> "DESC"
            }
            if (searchString == null) {
                prepareQuery("SELECT path, name, lastChanged, sha256 FROM model ORDER BY $order $sort LIMIT ? OFFSET ?") {
                    setInt(1, length)
                    setInt(2, offset)
                }
            } else {
                prepareQuery("SELECT path, name, lastChanged, sha256 FROM model WHERE LOCATE(LOWER(?), LOWER(name)) > 0 ORDER BY $order $sort LIMIT ? OFFSET ?") {
                    setString(1, searchString)
                    setInt(2, length)
                    setInt(3, offset)
                }
            }.use { result ->
                buildList {
                    while (result.next()) {
                        add(result.readModelItem())
                    }
                }
            }
        }
    }

    suspend fun getModel(path: Path): ModelItem? {
        waitUntilFirstScan()
        return transaction {
            prepareQuery("SELECT path, name, lastChanged, sha256 FROM model WHERE path = ?") {
                setString(1, path.normalize().toString())
            }.use { result ->
                if (result.next()) {
                    result.readModelItem()
                } else {
                    null
                }
            }
        }
    }

    suspend fun getModel(modelHash: ModelHash): ModelItem? {
        waitUntilFirstScan()
        return transaction {
            prepareQuery("SELECT path, name, lastChanged, sha256 FROM model WHERE sha256 = ?") {
                setBytes(1, modelHash.hash)
            }.use { result ->
                if (result.next()) {
                    result.readModelItem()
                } else {
                    null
                }
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

    private val _lastScanTime = MutableStateFlow<Instant?>(null)
    val lastScanTime = _lastScanTime.asStateFlow()
    private suspend fun doScan() = withContext(Dispatchers.IO) {
        transaction {
            execute("DROP TABLE IF EXISTS scanned_model_paths")
            execute(
                """
                    CREATE TEMPORARY TABLE scanned_model_paths(
                        path VARCHAR PRIMARY KEY
                    )
                """
            )
        }

        fun processFile(relativePath: Path) {
            val pathStr = relativePath.normalize().toString()
            val name = relativePath.fileName.toString()
            val path = modelDir.resolve(relativePath)
            val lastChanged = path.getLastModifiedTime().toMillis()
            transaction {
                LOGGER.trace("Process file {}", relativePath)
                prepare("INSERT INTO scanned_model_paths (path) VALUES (?)") {
                    setString(1, pathStr)
                }
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
                val sha256 = calculateSha256(path)
                LOGGER.trace("SHA-256 for file {} is {}", relativePath, sha256.toHexString())
                prepare("DELETE FROM model WHERE path = ?;") {
                    setString(1, pathStr)
                }
                prepare("INSERT INTO model (path, name, lastChanged, sha256) VALUES (?, ?, ?, ?);") {
                    setString(1, pathStr)
                    setString(2, name)
                    setLong(3, lastChanged)
                    setBytes(4, sha256)
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

        transaction {
            execute(
                """
                    DELETE FROM model
                    WHERE NOT EXISTS
                    (SELECT 1 FROM scanned_model_paths WHERE scanned_model_paths.path = model.path)
                """
            )
            execute("DROP TABLE scanned_model_paths")
        }

        _lastScanTime.value = Clock.System.now()
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

    private fun Connection.createTables() {
        transaction {
            if (checkVersionMatches()) {
                return@transaction
            }
            execute("DROP TABLE IF EXISTS version;")
            execute("DROP TABLE IF EXISTS model;")
            execute("CREATE TABLE version (version INTEGER);")
            prepare("INSERT INTO version (version) VALUES (?);") { setInt(1, DATABASE_VERSION) }
            execute(
                """
                CREATE TABLE model (
                    path VARCHAR PRIMARY KEY,
                    name VARCHAR NOT NULL,
                    lastChanged BIGINT NOT NULL,
                    sha256 BINARY(32) NOT NULL
                );
                """
            )
            LOGGER.info("Created tables")
        }
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        modelDir.createDirectories()

        Class.forName("org.h2.Driver")
        val databaseRelativePath = databaseFile.relativeTo(Paths.get(".").toAbsolutePath())
        val jdbcUrl = "jdbc:h2:./${databaseRelativePath.toString().removeSuffix(".mv.db")}"
        val connectionPool = ThreadSafeObjectPool(
            ObjectPool<Connection>(
                identifier = Identifier.of("armorstand", "database_connection"),
                create = {
                    DriverManager.getConnection(jdbcUrl)
                },
                onAcquired = {
                    autoCommit = false
                    isReadOnly = false
                },
            )
        )
        val connection = try {
            connectionPool.acquire()
        } catch (ex: Exception) {
            try {
                LOGGER.warn("Failed to open database, delete existing database and retry", ex)
                databaseFile.deleteIfExists()
                connectionPool.acquire()
            } catch (ex: Exception) {
                throw RuntimeException("Failed to open model database", ex)
            }
        }
        try {
            runCatching {
                Files.setAttribute(databaseFile, "dos:hidden", true)
            }
            connection.createTables()
        } finally {
            connectionPool.release(connection)
        }
        this@ModelManager.connectionPool = connectionPool

        LOGGER.info("Initialized database")

        listenModelDir(ArmorStand.instance.scope)
        scheduleScan()
    }
}