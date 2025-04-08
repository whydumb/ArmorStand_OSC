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
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.model.ModelInstance
import top.fifthlight.armorstand.model.ModelLoader
import top.fifthlight.armorstand.model.RenderNode
import top.fifthlight.armorstand.model.RenderScene
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.renderer.model.gltf.GltfLoader
import top.fifthlight.renderer.model.pmx.PmxLoader
import java.util.*
import kotlin.time.measureTimedValue

object PlayerModelManager {
    private val LOGGER = LogUtils.getLogger()

    private val client = MinecraftClient.getInstance()
    private val selfModel = MutableStateFlow<RenderScene?>(null)
    private val inGame = MutableStateFlow(false)

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
                                ?.let { path ->
                                    when (ModelFormatProber.probe(path)) {
                                        ModelFormatProber.Result.GLTF_BINARY -> GltfLoader.loadBinary(path)
                                        ModelFormatProber.Result.PMX -> PmxLoader.load(path)
                                        ModelFormatProber.Result.LIKELY_GLTF_TEXT -> error("Unsupported file")
                                        ModelFormatProber.Result.UNKNOWN -> error("Unknown file format")
                                    }
                                }
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
                        val instance = ModelInstance(model)
                        ModelEntry(
                            instance = instance,
                            controller = ModelController.LiveUpdated(instance.scene),
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