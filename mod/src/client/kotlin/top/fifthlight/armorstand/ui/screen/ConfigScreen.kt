package top.fifthlight.armorstand.ui.screen

import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.core.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Colors
import top.fifthlight.armorstand.ui.component.*
import top.fifthlight.armorstand.ui.model.ConfigViewModel
import top.fifthlight.armorstand.ui.state.ConfigScreenState
import top.fifthlight.armorstand.ui.util.sync
import kotlin.time.Duration.Companion.seconds

class ConfigScreen(parent: Screen? = null) : ArmorStandScreen<FlowLayout, ConfigViewModel>(
    parent = parent,
    viewModelFactory = ::ConfigViewModel,
    title = Text.translatable("armorstand.config")
) {
    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        return OwoUIAdapter.create(this, Containers::verticalFlow)
    }

    private fun modelList() = Containers.verticalFlow(Sizing.expand(50), Sizing.fill()).child(
        Containers.verticalScroll(
            Sizing.fill(),
            Sizing.fill(),
            viewModel.uiState.map {
                Pair(it.currentModel, it.modelItems)
            }.distinctUntilChanged().component(
                scope = scope,
                containerFactory = {
                    EditableFlowLayout(Sizing.fill(), Sizing.content(), FlowLayout.Algorithm.VERTICAL).apply {
                        margins(Insets.of(0, 0, 0, 8))
                        allowOverflow(false)
                    }
                },
                onUpdate = { (currentModel, modelPaths) ->
                    val baseRenderer = ButtonComponent.Renderer.flat(0, 0x33FFFFFF, 0)
                    refreshList(modelPaths) { item ->
                        Components
                            .button(Text.literal(item.path.fileName.toString())) {
                                viewModel.selectModel(item.path)
                            }
                            .apply {
                                if (item.path == currentModel) {
                                    stackedRenderer(
                                        baseRenderer,
                                        BorderButtonRenderer(Colors.WHITE)
                                    )
                                } else {
                                    renderer(baseRenderer)
                                }
                            }
                            .horizontalSizing(Sizing.fill())
                    }
                }
            ))
            .scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
            .scrollbarThiccness(8))
        .padding(Insets.vertical(2))
        .surface(ArmorstandSurfaces.LIST_BACKGROUND_WITH_SEPARATOR)

    private fun modelPreview() = Containers.verticalFlow(Sizing.fill(), Sizing.expand())
        .child(ArmorstandComponents.loadingIcon())
        .verticalAlignment(VerticalAlignment.CENTER)
        .horizontalAlignment(HorizontalAlignment.CENTER)

    private fun settings() = Containers.verticalFlow(Sizing.fill(), Sizing.content())
        .child(
            Components.checkbox(Text.translatable("armorstand.config.show_other_players"))
                .sync(
                    scope,
                    viewModel.uiState,
                    ConfigScreenState::showOtherPlayerModel,
                    viewModel::updateShowOtherPlayerModel
                )
        )
        .child(
            Components.checkbox(Text.translatable("armorstand.config.send_model_data"))
                .sync(scope, viewModel.uiState, ConfigScreenState::sendModelData, viewModel::updateSendModelData)
        )
        .child(
            Components.discreteSlider(Sizing.fill(), 0.1, 4.0)
                .sync(scope, viewModel.uiState, ConfigScreenState::modelScale, viewModel::updateModelScale)
                .decimalPlaces(2)
                .scrollStep(0.1)
                .message { Text.translatable("armorstand.config.model_scale", it) })
        .gap(4)

    private fun rightPanel() = Containers.verticalFlow(Sizing.expand(50), Sizing.fill())
        .child(
            Containers.verticalFlow(Sizing.fill(), Sizing.fill())
                .child(modelPreview())
                .child(settings())
                .gap(4)
        )
        .padding(Insets.of(8).add(2, 2, 0, 0))
        .surface(ArmorstandSurfaces.LIST_BACKGROUND_WITH_SEPARATOR)

    private fun mainPanel() = Containers.horizontalFlow(Sizing.fill(), Sizing.expand())
        .child(modelList())
        .child(rightPanel())
        .gap(16)
        .padding(Insets.horizontal(16))

    private fun topPanel() = Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(32))
        .child(Components.label(this.title).shadow(true).apply {
            var lastClicked: Instant? = null
            var triggerCount = 0
            textClickHandler {
                val now = Clock.System.now()
                lastClicked
                    ?.let { last -> now - last }
                    ?.takeIf { duration -> duration < 1.seconds }
                    ?.let {
                        triggerCount++
                        if (triggerCount == 3) {
                            triggerCount = 0
                            MinecraftClient.getInstance().setScreen(DebugScreen(this@ConfigScreen))
                        }
                    } ?: run {
                    triggerCount = 1
                }
                lastClicked = now
                true
            }
        })
        .horizontalAlignment(HorizontalAlignment.CENTER)
        .verticalAlignment(VerticalAlignment.CENTER)

    private fun bottomPanel() = Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(32))
        .child(Components.button(ScreenTexts.DONE) {
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