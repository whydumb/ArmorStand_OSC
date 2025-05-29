package top.fifthlight.armorstand.state

import com.mojang.logging.LogUtils
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPassImpl
import top.fifthlight.armorstand.ArmorStand
import top.fifthlight.armorstand.ArmorStandClient
import top.fifthlight.armorstand.animation.AnimationItem
import top.fifthlight.armorstand.animation.AnimationLoader
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.model.ModelBufferManager
import top.fifthlight.armorstand.model.ModelInstance
import top.fifthlight.armorstand.model.ModelLoader
import top.fifthlight.armorstand.model.RenderScene
import top.fifthlight.armorstand.util.ModelLoaders
import top.fifthlight.armorstand.util.RefCount
import top.fifthlight.armorstand.util.TimeUtil
import java.nio.file.Path
import java.util.*
import kotlin.time.measureTimedValue

object ModelInstanceManager {
    private val LOGGER = LogUtils.getLogger()
    const val INSTANCE_EXPIRE_NS: Long = 30L * TimeUtil.NANOSECONDS_PER_SECOND
    const val MODEL_EXPIRE_NS: Long = 10L * TimeUtil.NANOSECONDS_PER_SECOND
    private val client = MinecraftClient.getInstance()
    val modelDir: Path = System.getProperty("armorstand.modelDir")?.let {
        Path.of(it).toAbsolutePath()
    } ?: FabricLoader.getInstance().gameDir.resolve("models")
    private val selfUuid: UUID?
        get() = client.player?.uuid
    var selfPath: Path? = null
        private set
    private val modelPaths = mutableMapOf<UUID, Path>()
    val modelCaches = mutableMapOf<Path, ModelCache>()
    val modelInstanceItems = mutableMapOf<UUID, ModelInstanceItem>()

    sealed class ModelCache {
        data object Failed : ModelCache()

        data class Loaded(
            val scene: RenderScene,
            val animation: List<AnimationItem>,
        ) : RefCount by scene, ModelCache()
    }

    sealed interface ModelInstanceItem {
        val path: Path

        data class Failed(
            override val path: Path,
        ) : ModelInstanceItem

        class Model(
            override val path: Path,
            var lastAccessTime: Long,
            val instance: ModelInstance,
            val controller: ModelController,
        ) : RefCount by instance, ModelInstanceItem
    }

    private fun loadModel(path: Path): ModelCache {
        val (result, duration) = measureTimedValue {
            val modelLoadResult = runCatching {
                ModelLoaders.probeAndLoad(modelDir.resolve(path).toAbsolutePath())
            }.let { value ->
                value.exceptionOrNull()?.let { LOGGER.warn("Model load failed", it) }
                value.getOrNull()
            }
            val result = modelLoadResult?.takeIf { it.model != null }?.let { result ->
                LOGGER.info("Model metadata: ${result.metadata}")

                val scene = ModelLoader().loadModel(result.model!!)
                val animations = result.animations?.map { AnimationLoader.load(scene, it) } ?: listOf()

                ModelCache.Loaded(
                    scene = scene,
                    animation = animations,
                )
            } ?: ModelCache.Failed
            result
        }
        LOGGER.info("Model $path loaded, duration: $duration")
        return result
    }

    private fun loadCache(path: Path): ModelCache = modelCaches.getOrPut(path) {
        val item = loadModel(path)
        (item as? ModelCache.Loaded)?.increaseReferenceCount()
        item
    }

    fun updatePlayerModel(uuid: UUID, path: String?) {
        if (uuid == selfUuid) {
            return
        }
        if (path == null) {
            modelPaths.remove(uuid)
        } else {
            // TODO SAFETY path sanitizing
            modelPaths[uuid] = Path.of(path)
        }
    }

    fun get(uuid: UUID, time: Long): ModelInstanceItem? {
        val isSelf = uuid == selfUuid
        val path = if (isSelf) {
            selfPath
        } else {
            modelPaths[uuid] ?: run {
                if (ArmorStandClient.debug) {
                    selfPath?.let { selfPath ->
                        modelPaths[uuid] = selfPath
                        selfPath
                    }
                } else {
                    null
                }
            }
        }

        if (path == null) {
            return null
        }

        val lastAccessTime = if (isSelf) {
            -1
        } else {
            time
        }

        val item = modelInstanceItems[uuid]
        if (item != null) {
            if (item.path == path) {
                (item as? ModelInstanceItem.Model)?.let {
                    it.lastAccessTime = lastAccessTime
                }
                return item
            } else {
                val prevItem = modelInstanceItems.remove(uuid)
                (prevItem as? ModelInstanceItem.Model)?.decreaseReferenceCount()
            }
        }

        val newItem = when (val cache = loadCache(path)) {
            ModelCache.Failed -> ModelInstanceItem.Failed(path = path)
            is ModelCache.Loaded -> {
                val scene = cache.scene
                val animation = cache.animation.takeIf { it.isNotEmpty() }
                ModelInstanceItem.Model(
                    path = path,
                    lastAccessTime = lastAccessTime,
                    instance = ModelInstance(scene, ModelBufferManager.getEntry(scene)),
                    controller = when (animation) {
                        null -> ModelController.LiveUpdated(scene)
                        else -> ModelController.Predefined(animation)
                    },
                ).also {
                    it.increaseReferenceCount()
                }
            }
        }
        val prevItem = modelInstanceItems.remove(uuid)
        (prevItem as? ModelInstanceItem.Model)?.decreaseReferenceCount()
        modelInstanceItems[uuid] = newItem
        LOGGER.info("Loaded model $path for uuid $uuid")
        return newItem
    }

    fun cleanup(time: Long) {
        val usedPaths = mutableSetOf<Path>()

        // cleaned unused model instances
        modelInstanceItems.entries.removeIf { (uuid, item) ->
            if (uuid == selfUuid) {
                if (item.path == selfPath) {
                    return@removeIf false
                } else {
                    (item as? ModelInstanceItem.Model)?.decreaseReferenceCount()
                    return@removeIf true
                }
            }
            val pathInvalid = item.path != modelPaths[uuid]
            if (pathInvalid) {
                (item as? ModelInstanceItem.Model)?.decreaseReferenceCount()
                return@removeIf true
            }
            when (item) {
                is ModelInstanceItem.Failed -> false
                is ModelInstanceItem.Model -> {
                    val timeSinceLastUsed = time - item.lastAccessTime
                    val expired = timeSinceLastUsed > INSTANCE_EXPIRE_NS
                    if (expired) {
                        item.decreaseReferenceCount()
                    } else {
                        usedPaths.add(item.path)
                    }
                    expired
                }
            }
        }

        // cleaned unused model caches
        modelCaches.entries.removeIf { (path, item) ->
            if (path == selfPath) {
                return@removeIf false
            }
            val remove = path !in usedPaths
            if (remove && item is ModelCache.Loaded) {
                item.scene.decreaseReferenceCount()
            }
            remove
        }
    }

    fun initialize() {
        ArmorStand.instance.scope.launch {
            ConfigHolder.config
                .map { it.modelPath }
                .distinctUntilChanged()
                .collect { selfPath = it }
        }
        WorldRenderEvents.AFTER_ENTITIES.register {
            cleanup(System.nanoTime())
        }
    }
}
