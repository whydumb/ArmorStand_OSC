package top.fifthlight.armorstand.ui.screen

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import top.fifthlight.armorstand.ui.component.ArmorstandSurfaces

class DebugScreen(parent: Screen? = null) : BaseArmorStandScreen<FlowLayout>(
    title = Text.translatable("armorstand.debug_screen"),
    parent = parent
) {
    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        return OwoUIAdapter.create(this, Containers::verticalFlow)
    }

    private fun functions() = Containers.ltrTextFlow(Sizing.fill(), Sizing.expand())
        .child(Components.button(Text.translatable("armorstand.debug_screen.database")) {
            MinecraftClient.getInstance().setScreen(DatabaseScreen(this))
        })
        .padding(Insets.horizontal(16))

    private fun mainPanel() = Containers.verticalFlow(Sizing.fill(), Sizing.expand())
        .child(Components.label(Text.translatable("armorstand.debug_screen.tip")).shadow(true))
        .child(functions())
        .gap(8)
        .horizontalAlignment(HorizontalAlignment.CENTER)

    private fun topPanel() = Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(32))
        .child(Components.label(this.title).shadow(true))
        .horizontalAlignment(HorizontalAlignment.CENTER)
        .verticalAlignment(VerticalAlignment.CENTER)

    private fun bottomPanel() = Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(32))
        .child(Components.button(ScreenTexts.BACK) {
            close()
        }.horizontalSizing(Sizing.fixed(150)))
        .horizontalAlignment(HorizontalAlignment.CENTER)
        .verticalAlignment(VerticalAlignment.CENTER)

    override fun build(rootComponent: FlowLayout) = with(rootComponent) {
        surface(ArmorstandSurfaces.SCREEN_BACKGROUND)
        child(topPanel())
        child(mainPanel())
        child(bottomPanel())
        Unit
    }
}