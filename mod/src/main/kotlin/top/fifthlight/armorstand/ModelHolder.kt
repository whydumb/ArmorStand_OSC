package top.fifthlight.armorstand

import com.mojang.logging.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.model.ModelLoader
import top.fifthlight.armorstand.model.RenderScene
import top.fifthlight.renderer.model.gltf.GltfLoader
import kotlin.time.measureTimedValue

object ModelHolder {
    private val LOGGER = LogUtils.getLogger()
    private var initialized: Boolean = false
    var model: RenderScene? = null
        private set

    fun initialize(scope: CoroutineScope) {
        if (initialized) {
            return
        }
        initialized = true
        scope.launch {
            ConfigHolder.config.collect {
                model?.decreaseReferenceCount()
                model = null
                model = runCatching {
                    val (value, duration) = measureTimedValue {
                        it.modelPath
                            ?.let { path -> GltfLoader.loadBinary(path) }
                            ?.let { ModelLoader().loadScene(it) }
                            ?.also { it.increaseReferenceCount() }
                    }
                    LOGGER.info("Model loaded, duration: $duration")
                    value
                }.let {
                    it.exceptionOrNull()?.let { LOGGER.warn("Model load failed", it) }
                    it.getOrNull()
                }
            }
        }
    }
}
