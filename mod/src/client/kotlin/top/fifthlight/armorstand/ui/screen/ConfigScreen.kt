package top.fifthlight.armorstand.ui.screen

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.tab.TabManager
import net.minecraft.client.gui.widget.*
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.lwjgl.glfw.GLFW
import top.fifthlight.armorstand.manage.ModelManager
import top.fifthlight.armorstand.ui.component.*
import top.fifthlight.armorstand.ui.model.ConfigViewModel
import top.fifthlight.armorstand.ui.util.autoWidthButton
import top.fifthlight.armorstand.ui.util.checkbox
import top.fifthlight.armorstand.ui.util.slider
import top.fifthlight.armorstand.ui.util.textField
import top.fifthlight.armorstand.util.ceilDiv

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

    private val topBar by lazy {
        TextWidget(width, 32, title, currentClient.textRenderer)
    }

    private val closeButton = ButtonWidget.builder(ScreenTexts.BACK) { close() }.build()

    private val openModelDirectoryButton =
        ButtonWidget.builder(Text.translatable("armorstand.config.open_model_directory")) {
            viewModel.openModelDir()
        }.build()

    private val sortButton = run {
        fun sortText(order: ModelManager.Order, ascend: Boolean): String {
            val order = when (order) {
                ModelManager.Order.NAME -> "name"
                ModelManager.Order.LAST_CHANGED -> "last_changed"
            }
            val sort = when (ascend) {
                true -> "asc"
                false -> "desc"
            }
            return "armorstand.config.sort.$order.$sort"
        }

        ButtonWidget.builder(Text.translatable("armorstand.config.sort.name.asc")) {
            val (order, ascend) = viewModel.uiState.value.let { Pair(it.order, it.sortAscend) }
            if (ascend) {
                // switch ascend
                viewModel.updateSearchParam(order, false)
            } else {
                // next order
                val index = (ModelManager.Order.entries.indexOf(order) + 1) % ModelManager.Order.entries.size
                val newOrder = ModelManager.Order.entries[index]
                viewModel.updateSearchParam(newOrder, true)
            }
        }.size(100, 20).build().also { button ->
            scope.launch {
                viewModel.uiState.collect { state ->
                    button.message = Text.translatable(sortText(state.order, state.sortAscend))
                }
            }
        }
    }

    private val refreshButton by lazy {
        autoWidthButton(Text.translatable("armorstand.config.refresh")) {
            viewModel.refreshModels()
        }
    }

    private val searchBox by lazy {
        textField(
            placeHolder = Text.translatable("armorstand.config.search_placeholder")
                .formatted(Formatting.ITALIC)
                .formatted(Formatting.GRAY),
            text = viewModel.uiState.map { it.searchString }.distinctUntilChanged(),
            onChanged = viewModel::updateSearchString,
        )
    }

    private val pager by lazy {
        PagingWidget(
            textRenderer = textRenderer,
            currentPage = viewModel.uiState.value.currentOffset,
            totalPages = viewModel.uiState.value.totalItems,
            onPrevPage = {
                viewModel.updatePageIndex(-1)
            },
            onNextPage = {
                viewModel.updatePageIndex(1)
            },
        ).also { pager ->
            scope.launch {
                viewModel.uiState.map { state ->
                    Triple(state.currentOffset, state.totalItems, state.pageSize)
                }.distinctUntilChanged().collect { (currentOffset, totalItems, pageSize) ->
                    pageSize?.let { pageSize ->
                        pager.currentPage = (currentOffset / pageSize) + 1
                        pager.totalPages = totalItems ceilDiv pageSize
                        pager.refresh()
                    }
                }
            }
        }
    }

    private val modelGrid by lazy {
        GridLayout(
            cellWidth = 64,
            cellHeight = 80,
            padding = Insets(horizonal = 8),
            verticalGap = 8,
        ).also { grid ->
            scope.launch {
                viewModel.uiState.map { it.currentPageItems }.distinctUntilChanged().collect { items ->
                    grid.forEachChild {
                        if (it is AutoCloseable) {
                            it.close()
                        }
                        remove(it)
                    }
                    grid.clear()
                    items?.let {
                        for (item in items) {
                            val button = ModelButton(
                                modelItem = item,
                                textRenderer = textRenderer,
                                padding = Insets(8),
                            ) {
                                viewModel.selectModel(item.path)
                            }
                            grid.add(button)
                        }
                        grid.refreshPositions()
                        grid.forEachChild { addDrawableChild(it) }
                    }
                }
            }
        }
    }

    private val loadingOverlay by lazy {
        LoadingOverlay(modelGrid).also { overlay ->
            scope.launch {
                viewModel.uiState.collect { state ->
                    overlay.loading = state.currentPageItems == null
                }
            }
        }
    }

    private val previewTab = LayoutScreenTab(
        title = Text.translatable("armorstand.config.tab.preview"),
        padding = Insets(8),
        layout = BorderLayout(
            direction = BorderLayout.Direction.VERTICAL,
        ).apply {
            setCenterElement(
                ModelWidget(
                    surface = Surface.color(0xFF383838u) + Surface.border(0xFF161616u)
                ),
            )
            val gap = 8
            val padding = 8
            val options = listOf(
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
                    textFactory = { slider, text -> Text.translatable("armorstand.config.model_scale", text) },
                    min = 0.0,
                    max = 4.0,
                    value = viewModel.uiState.map { it.modelScale },
                    onValueChanged = { userTriggered, value ->
                        viewModel.updateModelScale(value)
                    },
                ),
            )
            val optionsHeight = options.sumOf { it.height } + gap * (options.size - 1) + padding
            setSecondElement(
                LinearLayout(
                    direction = LinearLayout.Direction.VERTICAL,
                    height = optionsHeight,
                    padding = Insets(top = padding),
                    gap = gap,
                ).apply {
                    options.forEach { add(it, expand = true) }
                }
            )
        },
    )

    private val metadataTab = LayoutScreenTab(
        title = Text.translatable("armorstand.config.tab.metadata"),
        padding = Insets(8),
        layout = LinearLayout().apply {

        },
    )

    private val tabManager = TabManager(
        { addDrawableChild(it) },
        { remove(it) },
    )

    private var tabNavigationWidget = TabNavigationWidget.builder(tabManager, width)
        .tabs(previewTab, metadataTab)
        .build()

    private var initialized = false
    override fun init() {
        val layout = BorderLayout(
            width = width,
            height = height,
            direction = BorderLayout.Direction.VERTICAL,
        )
        layout.setFirstElement(topBar) { topBar, width, height -> topBar.width = width }
        val padding = 8
        val gap = 8
        layout.setCenterElement(
            SpreadLayout(
                width = width,
                height = height - 32 * 2,
                padding = Insets(horizonal = padding),
                gap = gap,
            ).apply {
                add(
                    BorderLayout(
                        direction = BorderLayout.Direction.VERTICAL,
                        surface = Surface.listBackgroundWithSeparator(),
                    ).apply {
                        setFirstElement(
                            LinearLayout(
                                direction = LinearLayout.Direction.VERTICAL,
                                padding = Insets(padding),
                                gap = gap,
                            ).apply {
                                val toolbarActions = listOf(sortButton, refreshButton)
                                if (this@ConfigScreen.width >= 600) {
                                    add(
                                        widget = BorderLayout(
                                            height = 20,
                                            direction = BorderLayout.Direction.HORIZONTAL,
                                        ).apply {
                                            setCenterElement(searchBox)
                                            setSecondElement(
                                                LinearLayout(
                                                    direction = LinearLayout.Direction.HORIZONTAL,
                                                    align = LinearLayout.Align.END,
                                                    height = 20,
                                                    gap = gap,
                                                    padding = Insets(left = padding),
                                                ).apply {
                                                    toolbarActions.forEach {
                                                        add(it, Positioner.create().apply { alignVerticalCenter() })
                                                    }
                                                    pack()
                                                }
                                            )
                                        },
                                        expand = true,
                                    )
                                } else {
                                    add(searchBox, expand = true)
                                    add(
                                        widget = LinearLayout(
                                            direction = LinearLayout.Direction.HORIZONTAL,
                                            height = 20,
                                            align = LinearLayout.Align.END,
                                            gap = gap,
                                        ).apply {
                                            toolbarActions.forEach {
                                                add(it, Positioner.create().apply { alignVerticalCenter() })
                                            }
                                        },
                                        expand = true,
                                    )
                                }
                                pack()
                            }
                        )
                        setCenterElement(loadingOverlay)
                        setSecondElement(pager, Positioner.create().margin(padding))
                        addDrawable(this)
                        addDrawable(loadingOverlay)
                    }
                )
                add(
                    TabNavigationWrapper(
                        tabManager = tabManager,
                        inner = tabNavigationWidget,
                        surface = Surface.combine(
                            Surface.padding(Insets(bottom = 2), Surface.listBackground()),
                            Surface.footerSeparator(),
                        ),
                    ).also {
                        addDrawableChild(it)
                    }
                )
            }
        )
        layout.setSecondElement(
            LinearLayout(
                width = width,
                height = 32,
                direction = LinearLayout.Direction.HORIZONTAL,
                align = LinearLayout.Align.CENTER,
                gap = gap,
            ).apply {
                add(closeButton, Positioner.create().apply { alignVerticalCenter() })
                add(openModelDirectoryButton, Positioner.create().apply { alignVerticalCenter() })
            }
        )

        layout.refreshPositions()

        val (rows, columns) = modelGrid.calculateSize()
        viewModel.updatePageSize((rows * columns).takeIf { it > 0 })
        modelGrid.forEachChild { remove(it) }

        layout.forEachChild { addDrawableChild(it) }

        pager.init()
        addDrawableChild(pager)
        if (!initialized) {
            initialized = true
            tabNavigationWidget.selectTab(0, false)
        } else {
            tabManager.currentTab?.forEachChild { addDrawableChild(it) }
        }
    }

    override fun close() {
        modelGrid.forEachChild {
            if (it is AutoCloseable) {
                it.close()
            }
        }
        super.close()
    }
}
