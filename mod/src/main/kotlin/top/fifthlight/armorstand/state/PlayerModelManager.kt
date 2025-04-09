package top.fifthlight.armorstand.state

import com.mojang.logging.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.MinecraftClient
import org.slf4j.Logger
import top.fifthlight.armorstand.ArmorStand
import top.fifthlight.armorstand.animation.AnimationItem
import top.fifthlight.armorstand.animation.AnimationLoader
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.model.ModelInstance
import top.fifthlight.armorstand.model.ModelLoader
import top.fifthlight.armorstand.model.RenderNode
import top.fifthlight.armorstand.model.RenderScene
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.util.ModelLoaders
import java.util.*
import kotlin.time.measureTimedValue

object PlayerModelManager {
    private val LOGGER = LogUtils.getLogger()

    private val client = MinecraftClient.getInstance()
    private val inGame = MutableStateFlow(false)

    private data class ModelData(
        val scene: RenderScene,
        val animations: List<AnimationItem>,
    )
    private val selfModel = MutableStateFlow<ModelData?>(null)

    data class ModelEntry(
        val instance: ModelInstance,
        val controller: ModelController,
    ) : AbstractRefCount() {
        init {
            instance.increaseReferenceCount()
        }

        override fun onClosed() {
            instance.decreaseReferenceCount()
        }
    }

    private val selfEntry = MutableStateFlow<ModelEntry?>(null)

    fun dumpSelfModel() {
        fun RenderNode.getNodeDescription(): String = when (this) {
            is RenderNode.Group -> "Group (${children.size} children)"
            is RenderNode.Transform -> "Transform[targetIndex=$transformIndex]"
            is RenderNode.Mesh -> "Mesh[primitiveSize=${mesh.primitives.size}, skinIndex=$skinIndex]"
            is RenderNode.Joint -> "Joint[skinIndex=$skinIndex, jointIndex=$jointIndex]"
        }

        fun RenderNode.dumpTreeInternal(
            logger: Logger,
            prefix: String,
            isLastChild: Boolean,
            isFirst: Boolean = false,
        ) {
            val currentSymbol = if (isFirst) "" else if (isLastChild) "└── " else "├── "
            logger.info("$prefix$currentSymbol${getNodeDescription()}")

            val children = this.toList()
            val newPrefix = prefix + if (isFirst) "" else if (isLastChild) "    " else "│   "

            children.forEachIndexed { index, child ->
                val isLast = index == children.lastIndex
                child.dumpTreeInternal(logger, newPrefix, isLast)
            }
        }

        fun RenderNode.dumpTree(logger: Logger) {
            dumpTreeInternal(logger, "", true, true)
        }

        selfModel.value?.scene?.rootNode?.dumpTree(LOGGER) ?: run {
            LOGGER.info("No root node loaded.")
        }
    }

    fun initialize() {
        ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
            inGame.value = true
        }
        ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
            inGame.value = false
        }
        with(ArmorStand.scope) {
            launch {
                ConfigHolder.config.collectLatest { config ->
                    val currentModel = selfModel.value
                    currentModel?.scene?.decreaseReferenceCount()
                    selfModel.value = null
                    val modelLoadResult = runCatching {
                        val (value, duration) = measureTimedValue {
                            config.modelPath?.let { path -> ModelLoaders.probeAndLoad(path) }
                        }
                        LOGGER.info("Model loaded, duration: $duration")
                        value
                    }.let {
                        it.exceptionOrNull()?.let { LOGGER.warn("Model load failed", it) }
                        it.getOrNull()
                    }
                    modelLoadResult?.takeIf { it.scene != null }?.let { result ->
                        LOGGER.info("Model metadata: ${result.metadata}")

                        val scene = ModelLoader().loadScene(result.scene!!)
                        scene.increaseReferenceCount()

                        val animations = result.animations?.map { AnimationLoader.load(scene, it) } ?: listOf()

                        val modelData = ModelData(
                            scene = scene,
                            animations = animations,
                        )
                        selfModel.value = modelData
                    }
                }
            }
            launch {
                selfModel.combine(inGame, ::Pair).collectLatest { (entry, inGame) ->
                    val newInstance = if (!inGame) {
                        null
                    } else if (entry == null) {
                        null
                    } else {
                        val instance = ModelInstance(entry.scene)
                        ModelEntry(
                            instance = instance,
                            controller = if (entry.animations.isEmpty()) {
                                ModelController.LiveUpdated(instance.scene)
                            } else {
                                ModelController.Predefined(entry.animations)
                            },
                        )
                    }
                    val newEntry = newInstance?.also { it.increaseReferenceCount() }
                    val currentEntry = selfEntry.value
                    currentEntry?.decreaseReferenceCount()
                    selfEntry.value = newEntry
                }
            }
        }
    }

    operator fun get(playerUuid: UUID): ModelEntry? {
        if (!inGame.value) {
            return null
        }
        if (playerUuid == client.player?.uuid) {
            return selfEntry.value
        }
        return null
    }
}