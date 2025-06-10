package top.fifthlight.armorstand.state

import com.mojang.logging.LogUtils
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.blazerod.animation.AnimationItem
import top.fifthlight.blazerod.animation.AnimationLoader
import top.fifthlight.blazerod.model.Metadata
import top.fifthlight.blazerod.model.ModelBufferManager
import top.fifthlight.blazerod.model.ModelInstance
import top.fifthlight.blazerod.model.ModelLoader
import top.fifthlight.blazerod.model.RenderScene
import top.fifthlight.blazerod.util.RefCount
import top.fifthlight.blazerod.util.TimeUtil
import top.fifthlight.blazerod.model.ModelFileLoaders
import java.nio.file.Path
import java.util.*
import kotlin.time.measureTimedValue

object ModelInstanceManager {
    private val LOGGER = LogUtils.getLogger()
    const val INSTANCE_EXPIRE_NS: Long = 30L * TimeUtil.NANOSECONDS_PER_SECOND
    private val client = MinecraftClient.getInstance()
    private val selfUuid: UUID?
        get() = client.player?.uuid
    val modelDir: Path = System.getProperty("armorstand.modelDir")?.let {
        Path.of(it).toAbsolutePath()
    } ?: FabricLoader.getInstance().gameDir.resolve("models")
    val modelCaches = mutableMapOf<Path, ModelCache>()
    val modelInstanceItems = mutableMapOf<UUID, ModelInstanceItem>()

    sealed class ModelCache {
        data object Failed : ModelCache()

        data class Loaded(
            val metadata: Metadata?,
            val scene: RenderScene,
            val animations: List<AnimationItem>,
        ) : RefCount by scene, ModelCache()
    }

    sealed interface ModelInstanceItem {
        val path: Path

        data class Failed(
            override val path: Path,
        ) : ModelInstanceItem

        class Model(
            override val path: Path,
            val animations: List<AnimationItem>,
            var lastAccessTime: Long,
            val metadata: Metadata?,
            val instance: ModelInstance,
            var controller: ModelController,
        ) : RefCount by instance, ModelInstanceItem
    }

    private fun loadModel(path: Path): ModelCache {
        val (result, duration) = measureTimedValue {
            val modelLoadResult = runCatching {
                ModelFileLoaders.probeAndLoad(modelDir.resolve(path).toAbsolutePath())
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
                    animations = animations,
                    metadata = result.metadata,
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

    fun get(uuid: UUID, time: Long?): ModelInstanceItem? {
        val isSelf = uuid == selfUuid
        if (isSelf && !ConfigHolder.config.value.showOtherPlayerModel) {
            return null
        }

        val path = ClientModelPathManager.getPath(uuid)
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
                    lastAccessTime?.let { time ->
                        it.lastAccessTime = time
                    }
                }
                return item
            } else if (lastAccessTime != null) {
                val prevItem = modelInstanceItems.remove(uuid)
                (prevItem as? ModelInstanceItem.Model)?.decreaseReferenceCount()
            }
        }
        if (lastAccessTime == null) {
            return null
        }

        val newItem = when (val cache = loadCache(path)) {
            ModelCache.Failed -> ModelInstanceItem.Failed(path = path)
            is ModelCache.Loaded -> {
                val scene = cache.scene
                ModelInstanceItem.Model(
                    path = path,
                    animations = cache.animations,
                    metadata = cache.metadata,
                    lastAccessTime = lastAccessTime,
                    instance = ModelInstance(scene, ModelBufferManager.getEntry(scene)),
                    controller = when (val animation = cache.animations.firstOrNull()) {
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
                if (item.path == ClientModelPathManager.selfPath) {
                    return@removeIf false
                } else {
                    (item as? ModelInstanceItem.Model)?.decreaseReferenceCount()
                    return@removeIf true
                }
            }
            val pathInvalid = item.path != ClientModelPathManager.getPath(uuid)
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
            if (path == ClientModelPathManager.selfPath) {
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
        WorldRenderEvents.AFTER_ENTITIES.register {
            cleanup(System.nanoTime())
        }
    }
}
