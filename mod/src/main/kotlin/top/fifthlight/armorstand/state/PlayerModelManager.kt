package top.fifthlight.armorstand.state

import com.mojang.logging.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.MinecraftClient
import org.slf4j.Logger
import top.fifthlight.armorstand.ArmorStand
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.model.ModelInstance
import top.fifthlight.armorstand.model.ModelLoader
import top.fifthlight.armorstand.model.RenderNode
import top.fifthlight.armorstand.model.RenderScene
import top.fifthlight.renderer.model.gltf.GltfLoader
import java.util.*
import kotlin.time.measureTimedValue

object PlayerModelManager {
    private val LOGGER = LogUtils.getLogger()

    private val client = MinecraftClient.getInstance()
    private val selfModel = MutableStateFlow<RenderScene?>(null)
    private val inGame = MutableStateFlow(false)
    private val selfInstance = MutableStateFlow<ModelInstance?>(null)

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

        selfModel.value?.rootNode?.dumpTree(LOGGER) ?: run {
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
                    currentModel?.decreaseReferenceCount()
                    selfModel.value = null
                    val newModel = runCatching {
                        val (value, duration) = measureTimedValue {
                            config.modelPath
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
                    selfModel.value = newModel
                }
            }
            launch {
                selfModel.combine(inGame, ::Pair).collectLatest { (model, inGame) ->
                    val newInstance = if (!inGame) {
                        null
                    } else if (model == null) {
                        null
                    } else {
                        ModelInstance(model)
                    }
                    newInstance?.increaseReferenceCount()
                    val currentInstance = selfInstance.value
                    currentInstance?.decreaseReferenceCount()
                    selfInstance.value = newInstance
                }
            }
        }
    }

    operator fun get(playerUuid: UUID): ModelInstance? {
        if (!inGame.value) {
            return null
        }
        if (playerUuid == client.player?.uuid) {
            return selfInstance.value
        }
        return null
    }
}