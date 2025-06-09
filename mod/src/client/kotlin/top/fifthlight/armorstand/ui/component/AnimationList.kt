package top.fifthlight.armorstand.ui.component

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget
import net.minecraft.text.Text
import net.minecraft.util.Colors
import top.fifthlight.armorstand.ui.state.AnimationScreenState

class AnimationList(
    client: MinecraftClient,
    width: Int = 0,
    height: Int = 0,
    x: Int = 0,
    y: Int = 0,
    val onClicked: (AnimationScreenState.AnimationItem) -> Unit,
) : AlwaysSelectedEntryListWidget<AnimationList.Entry>(
    client,
    width,
    height,
    y,
    42,
) {
    override fun drawMenuListBackground(context: DrawContext) = Unit
    override fun drawHeaderAndFooterSeparators(context: DrawContext) = Unit

    init {
        this.x = x
    }

    companion object {
        private const val ENTRY_PADDING = 8
    }

    override fun getRowWidth() = width

    fun setEntries(entries: List<AnimationScreenState.AnimationItem>) {
        clearEntries()
        for (item in entries) {
            addEntry(Entry(item))
        }
    }

    inner class Entry(val item: AnimationScreenState.AnimationItem) : AlwaysSelectedEntryListWidget.Entry<Entry>() {
        private val name = item.name?.let { name -> Text.literal(name) } ?: Text.translatable("armorstand.animation.name.unnamed")
        private val length = run {
            val minutes = (item.duration / 60).toInt().toString().padStart(2, '0')
            val seconds = (item.duration % 60).toInt().toString().padStart(2, '0')
            Text.literal("$minutes:$seconds")
        }
        private val source = when (val source = item.source) {
            is AnimationScreenState.AnimationItem.Source.Embed -> Text.literal("Embed in model")
            is AnimationScreenState.AnimationItem.Source.External -> Text.literal("File: ${source.name}")
        }

        override fun getNarration(): Text = Text.empty()

        override fun render(
            context: DrawContext,
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickProgress: Float,
        ) {
            val textRenderer = client.textRenderer
            val timeWidth = textRenderer.getWidth(length)
            val nameTextWidth = textRenderer.getWidth(name)
            val timeExtraPadding = 2
            val nameAreaWidth = entryWidth - ENTRY_PADDING * 3 - timeWidth - timeExtraPadding
            drawScrollableText(
                context,
                textRenderer,
                name,
                x + ENTRY_PADDING + nameTextWidth / 2,
                x + ENTRY_PADDING,
                y + ENTRY_PADDING,
                x + ENTRY_PADDING + nameAreaWidth,
                y + ENTRY_PADDING + textRenderer.fontHeight,
                Colors.WHITE,
            )
            context.drawTextWithShadow(
                textRenderer,
                length,
                x + entryWidth - ENTRY_PADDING - timeWidth - timeExtraPadding,
                y + ENTRY_PADDING,
                Colors.GRAY,
            )
            context.drawTextWithShadow(
                textRenderer,
                source,
                x + ENTRY_PADDING,
                y + entryHeight - ENTRY_PADDING - textRenderer.fontHeight,
                Colors.GRAY,
            )
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            onClicked(item)
            return true
        }
    }
}