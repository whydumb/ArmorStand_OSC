package top.fifthlight.armorstand.ui.screen

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.core.*
import kotlinx.coroutines.flow.map
import net.minecraft.client.gui.screen.Screen
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Colors
import top.fifthlight.armorstand.ui.component.ArmorstandSurfaces
import top.fifthlight.armorstand.ui.component.component
import top.fifthlight.armorstand.ui.model.DatabaseViewModel
import top.fifthlight.armorstand.ui.state.DatabaseScreenState
import top.fifthlight.armorstand.ui.util.sync

class DatabaseScreen(parent: Screen? = null) : ArmorStandScreen<FlowLayout, DatabaseViewModel>(
    parent = parent,
    viewModelFactory = ::DatabaseViewModel,
    title = Text.translatable("armorstand.debug_screen.database")
) {
    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        return OwoUIAdapter.create(this, Containers::verticalFlow)
    }

    private fun queryInput() = Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(50))
        .child(
            Components.textArea(Sizing.expand(), Sizing.fill())
                .sync(scope, viewModel.uiState, DatabaseScreenState::query, viewModel::updateQuery)
        )
        .child(Components.button(Text.translatable("armorstand.debug_database.execute_query")) { viewModel.submitQuery() }
            .verticalSizing(Sizing.fill()))
        .gap(4)

    private fun resultDisplay() = viewModel.uiState.map { it.state }.component(
        scope = scope,
        containerFactory = {
            Containers.verticalFlow(Sizing.fill(), Sizing.expand()).gap(4)
        },
        onUpdate = { state ->
            clearChildren()
            when (state) {
                DatabaseScreenState.QueryState.Empty -> Unit

                is DatabaseScreenState.QueryState.Failed -> child(
                    Components.label(state.error?.let {
                        Text.translatable("armorstand.debug_database.query_failed_with_message", it)
                    } ?: run {
                        Text.translatable("armorstand.debug_database.query_failed")
                    }).shadow(true)
                )

                DatabaseScreenState.QueryState.Loading -> child(
                    Components.label(Text.translatable("armorstand.debug_database.querying")).shadow(true)
                )

                is DatabaseScreenState.QueryState.Updated -> child(
                    Components.label(
                        Text.translatable(
                            "armorstand.debug_database.executed",
                            state.updateCount,
                            state.duration.toString()
                        )
                    ).shadow(true)
                )

                is DatabaseScreenState.QueryState.Result -> child(
                    Containers.verticalFlow(Sizing.fill(), Sizing.fill())
                        .child(
                            Components.label(
                                Text.translatable(
                                    "armorstand.debug_database.queried_time",
                                    state.duration.toString()
                                )
                            ).shadow(true)
                        )
                        .child(resultTable(state.headers, state.rows))
                        .gap(4)
                )
            }
        }
    )

    private fun resultTable(headers: List<String>, rows: List<List<String>>) =
        Containers.verticalFlow(Sizing.fill(), Sizing.expand())
            .child(
                Containers.verticalScroll(
                    Sizing.fill(),
                    Sizing.fill(),
                    Containers.grid(Sizing.content(), Sizing.content(), rows.size + 1, headers.size).apply {
                        for ((index, header) in headers.withIndex()) {
                            val child = Containers.verticalFlow(Sizing.content(), Sizing.content())
                                .child(Components.label(Text.literal(header)).shadow(true))
                                .padding(Insets.of(4))
                                .surface(ArmorstandSurfaces.bottomLine(Colors.WHITE))
                            child(child, 0, index)
                        }
                        for ((y, row) in rows.withIndex()) {
                            for ((x, column) in row.withIndex()) {
                                child(
                                    Components
                                        .label(Text.literal(column)).shadow(true)
                                        .margins(Insets.of(4)),
                                    y + 1,
                                    x,
                                )
                            }
                        }
                    })
                    .scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
                    .scrollbarThiccness(8)
            )
            .padding(Insets.vertical(2))
            .surface(ArmorstandSurfaces.LIST_BACKGROUND_WITH_SEPARATOR)

    private fun mainPanel() = Containers.verticalFlow(Sizing.fill(), Sizing.expand())
        .child(queryInput())
        .child(resultDisplay())
        .gap(8)
        .padding(Insets.horizontal(8))
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
