package top.fifthlight.armorstand.ui.screen

import net.minecraft.client.gui.screen.Screen
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import top.fifthlight.armorstand.ui.component.BorderLayout
import top.fifthlight.armorstand.ui.component.LinearLayout
import top.fifthlight.armorstand.ui.dsl.*
import top.fifthlight.armorstand.ui.util.Dimensions

class DebugScreen(parent: Screen? = null) : BaseArmorStandScreen<DebugScreen>(
    title = Text.translatable("armorstand.debug_screen"),
    parent = parent
) {
    override fun ScreenContext<DebugScreen>.createLayout() {
        borderLayout(
            dimensions = dimension(),
            direction = BorderLayout.Direction.VERTICAL,
        ) {
            first(linearLayout(
                direction = LinearLayout.Direction.HORIZONTAL,
                align = LinearLayout.Align.CENTER,
                dimensions = Dimensions(height = 32),
            ) {
                add(label(title), positioner { alignVerticalCenter() })
            })
            center(linearLayout(LinearLayout.Direction.VERTICAL, gap = 8) {
                add(label(Text.translatable("armorstand.debug_screen.tip")), positioner { alignHorizontalCenter() })
                add(button(Text.translatable("armorstand.debug_screen.database")) {
                    client.setScreen(DatabaseScreen(screen))
                }, positioner { alignHorizontalCenter() })
            })
            second(linearLayout(
                direction = LinearLayout.Direction.HORIZONTAL,
                align = LinearLayout.Align.CENTER,
                dimensions = Dimensions(height = 32),
            ) {
                add(button(ScreenTexts.BACK) {
                    close()
                }, positioner { alignVerticalCenter() })
            })
        }
    }
}