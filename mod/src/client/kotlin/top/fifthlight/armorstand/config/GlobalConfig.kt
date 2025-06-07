package top.fifthlight.armorstand.config

import com.mojang.logging.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.InvalidPathException
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

@Serializable
data class GlobalConfig(
    val model: String? = null,
    val showOtherPlayerModel: Boolean = true,
    val sendModelData: Boolean = true,
    val modelScale: Double = 1.0,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GlobalConfig::class.java)
    }

    val modelPath by lazy {
        try {
            model?.let { Path(it) }
        } catch (ex: InvalidPathException) {
            logger.warn("Bad model path", ex)
            null
        }
    }
}

object ConfigHolder {
    private val configFile = FabricLoader.getInstance().configDir.resolve("armorstand.json")
    private val _config = MutableStateFlow(GlobalConfig())
    val config = _config.asStateFlow()

    @OptIn(ExperimentalSerializationApi::class)
    fun read() {
        runCatching {
            _config.value = configFile.inputStream().use { Json.decodeFromStream<GlobalConfig>(it) }
        }
    }

    fun update(editor: GlobalConfig.() -> GlobalConfig) {
        save(_config.updateAndGet(editor))
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun save(config: GlobalConfig) {
        configFile.parent.createDirectories()
        configFile.outputStream().use { Json.encodeToStream<GlobalConfig>(config, it) }
    }
}