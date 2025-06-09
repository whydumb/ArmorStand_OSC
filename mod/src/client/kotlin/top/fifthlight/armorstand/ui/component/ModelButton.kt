package top.fifthlight.armorstand.ui.component

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.texture.TextureManager
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.blazerod.extension.NativeImageExt
import top.fifthlight.armorstand.manage.ModelItem
import top.fifthlight.armorstand.manage.ModelManager
import top.fifthlight.armorstand.util.ThreadExecutorDispatcher
import top.fifthlight.blazerod.model.util.readToBuffer
import java.nio.channels.FileChannel

class ModelButton(
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0,
    modelItem: ModelItem,
    private val textRenderer: TextRenderer,
    private val padding: Insets = Insets(),
    onPressAction: () -> Unit,
) : ButtonWidget(
    x,
    y,
    width,
    height,
    Text.literal(modelItem.name),
    { onPressAction.invoke() },
    DEFAULT_NARRATION_SUPPLIER,
), AutoCloseable {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(ModelButton::class.java)
        private val PLACEHOLDER_ICON: Identifier = Identifier.of("armorstand", "thumbnail_placeholder")
        private const val ICON_WIDTH = 32
        private const val ICON_HEIGHT = 32
    }

    private var closed = false
    private fun requireOpen() = require(!closed) { "Model button already closed" }

    private data class ModelIcon(
        val textureManager: TextureManager,
        val identifier: Identifier,
        val texture: NativeImageBackedTexture,
        val width: Int,
        val height: Int,
    ) : AutoCloseable {
        private var closed = false

        override fun close() {
            if (closed) {
                return
            }
            closed = true
            textureManager.destroyTexture(identifier)
        }
    }

    private sealed class ModelIconState : AutoCloseable {
        override fun close() = Unit

        data object Loading : ModelIconState()
        data object None : ModelIconState()
        data object Failed : ModelIconState()
        data class Loaded(
            val icon: ModelIcon,
        ) : ModelIconState() {
            override fun close() = icon.close()
        }
    }

    private val scope = CoroutineScope(ThreadExecutorDispatcher(MinecraftClient.getInstance()) + Job())
    private var iconState: ModelIconState = ModelIconState.Loading
    private var checked = false

    init {
        scope.launch {
            ConfigHolder.config.map { it.modelPath }.distinctUntilChanged().collect {
                checked = it == modelItem.path
            }
        }
        scope.launch {
            val path = withContext(Dispatchers.IO) {
                ModelManager.modelDir.resolve(modelItem.path).toAbsolutePath()
            }
            val thumbnail = ModelManager.getModelThumbnail(modelItem)
            try {
                when (thumbnail) {
                    is ModelManager.ModelThumbnail.Embed -> {
                        val buffer = withContext(Dispatchers.IO) {
                            FileChannel.open(path).use {
                                it.readToBuffer(
                                    offset = thumbnail.offset,
                                    length = thumbnail.length,
                                    readSizeLimit = 32 * 1024 * 1024,
                                )
                            }
                        }

                        val width: Int
                        val height: Int
                        val identifier = Identifier.of("armorstand", "models/${modelItem.hash}")
                        val texture = withContext(Dispatchers.Default) {
                            NativeImageExt.read(thumbnail.type, buffer)
                        }.use { image ->
                            width = image.width
                            height = image.height
                            NativeImageBackedTexture({ "Model icon for ${modelItem.hash}" }, image)
                        }
                        texture.setClamp(true)
                        texture.setFilter(true, false)
                        val icon = try {
                            val textureManager = MinecraftClient.getInstance().textureManager
                            textureManager.registerTexture(identifier, texture)
                            ModelIcon(
                                textureManager = textureManager,
                                identifier = identifier,
                                texture = texture,
                                width = width,
                                height = height,
                            )
                        } catch (ex: Throwable) {
                            texture.close()
                            throw ex
                        }

                        iconState = ModelIconState.Loaded(icon)
                    }

                    ModelManager.ModelThumbnail.None -> {
                        iconState = ModelIconState.None
                    }
                }
            } catch (ex: Exception) {
                LOGGER.warn("Failed to read model icon", ex)
                iconState = ModelIconState.Failed
            }
        }
    }

    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        requireOpen()
        if (active && checked) {
            context.fill(
                x,
                y,
                x + width,
                y + height,
                0x66000000u.toInt(),
            )
        }
        if (active && hovered) {
            context.drawBorder(
                x,
                y,
                width,
                height,
                0x99000000u.toInt(),
            )
        } else if (isSelected) {
            context.drawBorder(
                x,
                y,
                width,
                height,
                0x44000000u.toInt(),
            )
        }
        val top = y + padding.top
        val bottom = y + height - padding.bottom
        val left = x + padding.left
        val right = x + width - padding.right
        val imageBottom = bottom - textRenderer.fontHeight - 8
        val imageWidth = right - left
        val imageHeight = imageBottom - top
        when (val state = iconState) {
            is ModelIconState.Loaded -> {
                val icon = state.icon
                val iconAspect = icon.width.toFloat() / icon.height.toFloat()
                val targetAspect = imageWidth.toFloat() / imageHeight.toFloat()

                if (iconAspect > targetAspect) {
                    val scaledHeight = (imageWidth / iconAspect).toInt()
                    val yOffset = (imageHeight - scaledHeight) / 2

                    context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        icon.identifier,
                        left,
                        top + yOffset,
                        0f,
                        0f,
                        imageWidth,
                        scaledHeight,
                        icon.width,
                        icon.height,
                        icon.width,
                        icon.height,
                    )
                } else {
                    val scaledWidth = (imageHeight * iconAspect).toInt()
                    val xOffset = (imageWidth - scaledWidth) / 2

                    context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        icon.identifier,
                        left + xOffset,
                        top,
                        0f,
                        0f,
                        scaledWidth,
                        imageHeight,
                        icon.width,
                        icon.height,
                        icon.width,
                        icon.height,
                    )
                }
            }

            ModelIconState.Loading -> {
                context.drawGuiTexture(
                    RenderPipelines.GUI_TEXTURED,
                    LoadingOverlay.LOADING_ICON,
                    (left + right - LoadingOverlay.ICON_WIDTH) / 2,
                    (top + imageBottom - LoadingOverlay.ICON_HEIGHT) / 2,
                    LoadingOverlay.ICON_WIDTH,
                    LoadingOverlay.ICON_HEIGHT,
                )
            }

            ModelIconState.None -> {
                context.drawGuiTexture(
                    RenderPipelines.GUI_TEXTURED,
                    PLACEHOLDER_ICON,
                    (left + right - LoadingOverlay.ICON_WIDTH) / 2,
                    (top + imageBottom - LoadingOverlay.ICON_HEIGHT) / 2,
                    ICON_WIDTH,
                    ICON_HEIGHT,
                )
            }

            ModelIconState.Failed -> {}
        }
        drawScrollableText(
            context,
            textRenderer,
            message,
            left,
            bottom - textRenderer.fontHeight,
            right,
            bottom,
            Colors.WHITE,
        )
    }

    override fun close() {
        scope.cancel()
        iconState.close()
    }
}
