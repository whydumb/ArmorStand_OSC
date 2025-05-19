package top.fifthlight.armorstand.state

import com.mojang.logging.LogUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import top.fifthlight.armorstand.ArmorStand
import top.fifthlight.armorstand.animation.AnimationItem
import top.fifthlight.armorstand.animation.AnimationLoader
import top.fifthlight.armorstand.config.ConfigHolder
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
    const val INSTANCE_EXPIRE_NS = 30 * TimeUtil.NANOSECONDS_PER_SECOND
    const val MODEL_EXPIRE_NS = 10 * TimeUtil.NANOSECONDS_PER_SECOND
    private val client = MinecraftClient.getInstance()
    val modelDir: Path = FabricLoader.getInstance().gameDir.resolve("models")
    private val selfUuid: UUID?
        get() = client.player?.uuid
    var selfPath: Path? = null
        private set
    private var selfItem: Item = Item.Empty
    private val modelPaths = mutableMapOf<UUID, Path>()
    private val modelItems = mutableMapOf<UUID, Item>()
    private val modelCache = mutableMapOf<Path, ModelCache>()
    private val modelLoadJobs = mutableMapOf<Path, Job>()

    sealed class ModelCache {
        data object Failed : ModelCache()

        data class Loaded(
            val scene: RenderScene,
            val animation: List<AnimationItem>,
            var lastAccessTime: Long,
        ) : RefCount by scene, ModelCache()
    }

    sealed interface Item {
        data object Empty : Item

        data object Loading : Item

        class Model(
            val path: Path,
            var lastAccessTime: Long,
            val instance: ModelInstance,
            val controller: ModelController,
        ) : RefCount by instance, Item
    }

    fun initialize() {
        ArmorStand.instance.scope.launch {
            ConfigHolder.config
                .map { it.modelPath }
                .distinctUntilChanged()
                .collect { updateSelfPath(it) }
        }
        WorldRenderEvents.AFTER_ENTITIES.register {
            cleanup()
        }
    }

    fun cleanup() {
        val now = System.nanoTime()
        val usedPaths = mutableSetOf<Path>()
        modelItems.values.removeAll { item ->
            when (item) {
                is Item.Model -> {
                    if (now - item.lastAccessTime > INSTANCE_EXPIRE_NS) {
                        item.decreaseReferenceCount()
                        true
                    } else {
                        usedPaths.add(item.path)
                        false
                    }
                }

                else -> false
            }
        }

        modelCache.entries.removeAll { (path, cache) ->
            if (path == selfPath) {
                false
            } else {
                if (path !in usedPaths) {
                    if (cache is ModelCache.Loaded) {
                        if (now - cache.lastAccessTime < MODEL_EXPIRE_NS) {
                            return@removeAll false
                        }
                        cache.decreaseReferenceCount()
                    }
                    true
                } else {
                    if (cache is ModelCache.Loaded) {
                        cache.lastAccessTime = now
                    }
                    false
                }
            }
        }
    }

    fun updateSelfPath(path: Path?) {
        selfPath = path
        (selfItem as? Item.Model)?.decreaseReferenceCount()
        selfItem = Item.Empty
        if (path != null) {
            loadModel(path) { result ->
                // Preload the model
                if (selfPath != path) {
                    return@loadModel
                }
                selfItem = when (val result = result as? ModelCache.Loaded) {
                    null -> Item.Empty
                    else -> Item.Model(
                        path = path,
                        lastAccessTime = System.nanoTime(),
                        instance = ModelInstance(result.scene),
                        controller = if (result.animation.isNotEmpty()) {
                            ModelController.Predefined(result.animation)
                        } else {
                            ModelController.LiveUpdated(result.scene)
                        }
                    ).also {
                        it.increaseReferenceCount()
                    }
                }
                // If not in game, manually trigger cleanup
                if (selfUuid == null) {
                    cleanup()
                }
            }
        }
    }

    fun updatePlayerModel(uuid: UUID, path: String?) {
        if (uuid == selfUuid) {
            return
        }
        if (path == null) {
            removeModelItem(uuid)
            modelPaths.remove(uuid)
        } else {
            // TODO SAFETY path sanitizing
            if (uuid != selfUuid) {
                modelPaths[uuid] = Path.of(path)
            }
        }
    }

    private fun loadModel(path: Path, onLoaded: (ModelCache) -> Unit = {}) {
        (modelCache[path] as? ModelCache.Loaded)?.let {
            onLoaded(it)
            return
        }
        modelLoadJobs.getOrPut(path) {
            ArmorStand.instance.scope.launch {
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
                        scene.increaseReferenceCount()

                        val animations = result.animations?.map { AnimationLoader.load(scene, it) } ?: listOf()

                        ModelCache.Loaded(
                            scene = scene,
                            animation = animations,
                            lastAccessTime = System.nanoTime(),
                        )
                    } ?: ModelCache.Failed
                    modelCache[path] = result
                    modelLoadJobs.remove(path)
                    result
                }
                LOGGER.info("Model $path loaded, duration: $duration")
                onLoaded(result)
            }
        }
    }

    private fun loadItem(uuid: UUID, path: Path, time: Long): Item {
        if (uuid == selfUuid) {
            return selfItem
        }
        val cache = modelCache[path]
        if (cache == null) {
            loadModel(path)
            return Item.Loading.also {
                putModelItem(uuid, it)
            }
        }
        if (cache !is ModelCache.Loaded) {
            return Item.Empty
        }
        cache.lastAccessTime = time
        val scene = cache.scene
        return Item.Model(
            path = path,
            lastAccessTime = time,
            instance = ModelInstance(scene),
            controller = ModelController.LiveUpdated(scene)
        ).also {
            it.increaseReferenceCount()
        }
    }

    private fun getModelPath(uuid: UUID) = selfPath/*if (uuid == selfUuid) {
        selfPath
    } else {
        modelPaths[uuid]
    }*/

    private fun getModelItem(uuid: UUID) = if (uuid == selfUuid) {
        selfItem
    } else if (ConfigHolder.config.value.showOtherPlayerModel) {
        modelItems[uuid]
    } else {
        null
    }

    private fun putModelItem(uuid: UUID, item: Item) = if (uuid == selfUuid) {
        selfItem = item
    } else {
        modelItems[uuid] = item
    }

    private fun removeModelItem(uuid: UUID) = if (uuid == selfUuid) {
        selfItem.also {
            (it as? Item.Model)?.decreaseReferenceCount()
            selfItem = Item.Empty
        }
    } else {
        modelItems.remove(uuid)?.let {
            (it as? Item.Model)?.decreaseReferenceCount()
        }
    }

    fun get(uuid: UUID, time: Long): Item = when (val item = getModelItem(uuid)) {
        is Item.Model -> getModelPath(uuid).let { path ->
            if (item.path == path) {
                item.also { it.lastAccessTime = time }
            } else {
                Item.Empty
            }
        }

        Item.Loading, Item.Empty, null -> run {
            val path = getModelPath(uuid)
            if (path == null) {
                return Item.Empty
            }
            loadItem(uuid, path, time)
        }
    }

    fun getItems() = modelItems.toMap().let {
        if (selfUuid != null) {
            it + Pair(selfUuid, selfItem)
        } else {
            it
        }
    }

    fun getCache() = modelCache.toMap()
}