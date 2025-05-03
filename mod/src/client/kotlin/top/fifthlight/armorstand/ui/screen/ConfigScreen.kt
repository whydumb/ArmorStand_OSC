package top.fifthlight.armorstand.ui.screen

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.TabButtonWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import top.fifthlight.armorstand.ui.component.BorderLayout
import top.fifthlight.armorstand.ui.component.Insets
import top.fifthlight.armorstand.ui.component.LinearLayout
import top.fifthlight.armorstand.ui.component.Surface
import top.fifthlight.armorstand.ui.dsl.*
import top.fifthlight.armorstand.ui.model.ConfigViewModel
import top.fifthlight.armorstand.ui.state.ConfigScreenState
import top.fifthlight.armorstand.ui.util.Dimensions

class ConfigScreen(parent: Screen? = null) : ArmorStandScreen<ConfigScreen, ConfigViewModel>(
    parent = parent,
    viewModelFactory = ::ConfigViewModel,
    title = Text.translatable("armorstand.config"),
) {
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_1 && hasControlDown()) {
            client?.setScreen(DebugScreen(this))
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun ScreenContext<ConfigScreen>.createLayout() {
        borderLayout(
            dimensions = dimension(),
            direction = BorderLayout.Direction.VERTICAL,
        ) {
            first(
                linearLayout(
                    direction = LinearLayout.Direction.HORIZONTAL,
                    align = LinearLayout.Align.CENTER,
                    dimensions = Dimensions(height = 32),
                ) {
                    add(label(title), positioner { alignVerticalCenter() })
                }
            )
            center(
                borderLayout(direction = BorderLayout.Direction.HORIZONTAL) {
                    val margin = 8
                    val padding = 8
                    val gap = 8
                    val currentWidth = width / 2 - margin * 2
                    val currentHeight = height - 32 * 2
                    first(
                        linearLayout(
                            direction = LinearLayout.Direction.VERTICAL,
                            dimensions = Dimensions(width = currentWidth, height = currentHeight),
                            padding = Insets(padding),
                            gap = gap,
                            surface = Surface.listBackgroundWithSeparator(),
                        ) {
                            val currentWidth = currentWidth - padding * 2
                            val currentHeight = currentHeight - padding * 2

                            val toolbarActions = listOf(
                                autoWidthButton(Text.literal("Sort: A-Z")) {

                                },
                                autoWidthButton(Text.literal("Refresh")) {

                                },
                            )

                            fun searchBox(width: Int) = button(
                                text = Text.literal("Search"),
                                dimensions = Dimensions(width = width, height = 20),
                            ) {

                            }
                            if (currentWidth >= 400) {
                                add(
                                    linearLayout(
                                        direction = LinearLayout.Direction.HORIZONTAL,
                                        dimensions = Dimensions(width = currentWidth, height = 20),
                                        gap = gap,
                                    ) {
                                        val searchWidth =
                                            currentWidth - toolbarActions.sumOf { it.width } - toolbarActions.size * gap
                                        add(searchBox(searchWidth), positioner { alignVerticalCenter() })
                                        toolbarActions.forEach { add(it, positioner { alignVerticalCenter() }) }
                                    }
                                )
                            } else {
                                add(searchBox(currentWidth))
                                add(
                                    linearLayout(
                                        direction = LinearLayout.Direction.HORIZONTAL,
                                        dimensions = Dimensions(width = currentWidth, height = 20),
                                        align = LinearLayout.Align.END,
                                        gap = gap,
                                    ) {
                                        toolbarActions.forEach { add(it, positioner { alignVerticalCenter() }) }
                                    }
                                )
                            }
                        }, positioner { margin(margin, 0) }
                    )
                    second(
                        linearLayout(
                            direction = LinearLayout.Direction.VERTICAL,
                            dimensions = Dimensions(width = currentWidth, height = currentHeight),
                            surface = Surface.listBackgroundWithSeparator(),
                        ) {
                            val tabAreaHeight = currentHeight
                            add(dynamicLinearLayout(
                                direction = LinearLayout.Direction.VERTICAL,
                                dimensions = Dimensions(width = currentWidth, height = tabAreaHeight),
                                gap = gap,
                                padding = Insets(padding),
                                data = viewModel.uiState.map { it.selectedTab }.distinctUntilChanged(),
                            ) { selectedTab ->
                                val currentWidth = currentWidth - padding * 2
                                val currentHeight = tabAreaHeight - padding * 2
                                when (selectedTab) {
                                    ConfigScreenState.SelectedTab.PREVIEW -> {
                                        val configElements = listOf(
                                            checkbox(
                                                text = Text.translatable("armorstand.config.send_model_data"),
                                                value = viewModel.uiState.map { it.sendModelData },
                                                onValueChanged = viewModel::updateSendModelData,
                                            ),
                                            checkbox(
                                                text = Text.translatable("armorstand.config.show_other_players"),
                                                value = viewModel.uiState.map { it.showOtherPlayerModel },
                                                onValueChanged = viewModel::updateShowOtherPlayerModel,
                                            ),
                                            slider(
                                                textFactory = { Text.translatable("armorstand.config.model_scale", it) },
                                                dimensions = Dimensions(width = currentWidth, height = 20),
                                                min = 0.0,
                                                max = 4.0,
                                                value = viewModel.uiState.map { it.modelScale },
                                                onValueChanged = viewModel::updateModelScale,
                                            ),
                                        )
                                        val previewHeight =
                                            currentHeight - configElements.sumOf { it.height } - configElements.size * gap
                                        add(
                                            linearLayout(
                                                direction = LinearLayout.Direction.VERTICAL,
                                                dimensions = Dimensions(width = currentWidth, height = previewHeight),
                                                surface = Surface.combine(
                                                    Surface.color(0xFF383838u.toInt()),
                                                    Surface.border(0xFF161616u.toInt()),
                                                ),
                                                align = LinearLayout.Align.CENTER,
                                            ) {
                                                add(label(text = Text.literal("PREVIEW")), positioner { alignHorizontalCenter() })
                                            }
                                        )
                                        configElements.forEach { add(it) }
                                    }
                                    ConfigScreenState.SelectedTab.METADATA -> {

                                    }
                                }
                            })
                        }, positioner { margin(margin, 0) })
                }
            )
            second(
                linearLayout(
                    direction = LinearLayout.Direction.HORIZONTAL,
                    align = LinearLayout.Align.CENTER,
                    dimensions = Dimensions(height = 32),
                    gap = 8,
                ) {
                    add(button(ScreenTexts.BACK) {
                        close()
                    }, positioner { alignVerticalCenter() })
                    add(button(Text.translatable("armorstand.config.open_model_directory")) {
                        viewModel.openModelDir()
                    }, positioner { alignVerticalCenter() })
                }
            )
        }
    }
}