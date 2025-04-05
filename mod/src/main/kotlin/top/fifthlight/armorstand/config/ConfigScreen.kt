package top.fifthlight.armorstand.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget
import net.minecraft.client.input.KeyCodes
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import top.fifthlight.armorstand.ArmorStand
import top.fifthlight.armorstand.util.ClientThreadDispatcher
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

class ConfigScreen(private val parent: Screen? = null) : Screen(Text.translatable("armorstand.screen.config")) {
    private val modelDir = FabricLoader.getInstance().gameDir.resolve("models")
    private val allowedExtensions = listOf("vrm", "glb", "gltf")
    private val items = MutableStateFlow(listOf<Path>())
    private val scope = CoroutineScope(ClientThreadDispatcher)

    private val layout: ThreePartsLayoutWidget = ThreePartsLayoutWidget(this)
    private var list: ModelList? = null

    private fun loadItems() {
        val items = runCatching {
            modelDir.listDirectoryEntries().filter { it.isRegularFile() && it.extension.lowercase() in allowedExtensions }
        }.getOrNull() ?: listOf()
        this.items.value = items
    }

    init {
        loadItems()
    }

    override fun init() {
        val list = list ?: ModelList(MinecraftClient.getInstance()).also { list = it }
        layout.addHeader(this.title, this.textRenderer)
        layout.addBody(list)
        layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE) { close() }.width(200).build())
        layout.forEachChild { widget -> addDrawableChild(widget) }
        refreshWidgetPositions()
    }

    override fun refreshWidgetPositions() {
        layout.refreshPositions()
    }

    override fun close() {
        scope.cancel()
        client?.setScreen(parent)
    }

    private inner class ModelList(
        private val client: MinecraftClient,
    ) : AlwaysSelectedEntryListWidget<ModelList.Entry>(
        client,
        this@ConfigScreen.width,
        this@ConfigScreen.height - 53,
        0,
        18
    ) {
        init {
            scope.launch {
                items.collectLatest {
                    clearEntries()
                    addEntry(Entry("EMPTY", null))
                    val entries = it.associate {
                        val entry = Entry(it.fileName.toString(), it)
                        addEntry(entry)
                        Pair(it, entry)
                    }
                    ConfigHolder.config.collect {
                        val index = entries.keys.indexOf(it.modelPath)
                        if (index != -1) {
                            setSelected(index + 1)
                        } else {
                            setSelected(0)
                        }
                        selectedOrNull?.let { selected ->
                            centerScrollOn(selected)
                        }
                    }
                }
            }
        }

        override fun getRowWidth(): Int {
            return super.getRowWidth() + 50
        }

        inner class Entry(
            private val name: String,
            private val path: Path?,
        ) : AlwaysSelectedEntryListWidget.Entry<Entry>() {
            override fun render(
                drawContext: DrawContext,
                index: Int,
                top: Int,
                left: Int,
                width: Int,
                height: Int,
                mouseX: Int,
                mouseY: Int,
                hovering: Boolean,
                partialTick: Float
            ) {
                drawContext.drawCenteredTextWithShadow(
                    client.textRenderer,
                    name,
                    this@ModelList.width / 2,
                    (top + height / 2) - client.textRenderer.fontHeight / 2,
                    0xFFFFFFFFu.toInt()
                )
            }

            override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
                if (KeyCodes.isToggle(keyCode)) {
                    this.select()
                    return true
                } else {
                    return super.keyPressed(keyCode, scanCode, modifiers)
                }
            }

            override fun getNarration(): Text? = Text.literal(name)

            override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
                this.select()
                return super.mouseClicked(mouseX, mouseY, button)
            }

            private fun select() {
                ConfigHolder.update { copy(model = path?.toString()) }
            }
        }
    }
}