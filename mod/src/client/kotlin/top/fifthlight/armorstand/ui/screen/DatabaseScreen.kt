package top.fifthlight.armorstand.ui.screen

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import top.fifthlight.armorstand.ui.component.BorderLayout
import top.fifthlight.armorstand.ui.component.LinearLayout
import top.fifthlight.armorstand.ui.model.DatabaseViewModel
import top.fifthlight.armorstand.ui.util.autoWidthButton
import top.fifthlight.armorstand.ui.util.textField

class DatabaseScreen(parent: Screen? = null) : ArmorStandScreen<DatabaseScreen, DatabaseViewModel>(
    parent = parent,
    viewModelFactory = ::DatabaseViewModel,
    title = Text.translatable("armorstand.debug_screen.database")
) {
    private val topBar by lazy {
        TextWidget(width, 32, title, currentClient.textRenderer)
    }
    private val closeButton = ButtonWidget.builder(ScreenTexts.BACK) { close() }.build()
    private val queryInput by lazy {
        textField(
            placeHolder = Text.literal("Enter SQL…").formatted(Formatting.ITALIC).formatted(Formatting.GRAY),
            text = viewModel.uiState.map { it.query }.distinctUntilChanged(),
            onChanged = viewModel::updateQuery,
        )
    }
    private val executeButton by lazy {
        autoWidthButton(
            text = Text.translatable("armorstand.debug_database.execute_query"),
        ) {
            viewModel.submitQuery()
        }
    }

    override fun init() {
        val rootLayout = BorderLayout(
            width = width,
            height = height,
            direction = BorderLayout.Direction.VERTICAL,
        )
        rootLayout.setFirstElement(topBar) { topBar, width, height -> topBar.width = width }
        rootLayout.setCenterElement(
            BorderLayout(
                width = width,
                direction = BorderLayout.Direction.VERTICAL,
            ).apply {
                setFirstElement(
                    BorderLayout(
                        width = width,
                        height = 20,
                        direction = BorderLayout.Direction.HORIZONTAL,
                    ).apply {
                        setCenterElement(queryInput)
                        setSecondElement(executeButton)
                    },
                )
            }
        )
        rootLayout.setSecondElement(
            LinearLayout(
                width = width,
                height = 32,
                direction = LinearLayout.Direction.HORIZONTAL,
                align = LinearLayout.Align.CENTER,
                gap = 8,
            ).apply {
                add(closeButton, Positioner.create().apply { alignVerticalCenter() })
            }
        )
        rootLayout.refreshPositions()
        rootLayout.forEachChild { addDrawableChild(it) }
    }
}
