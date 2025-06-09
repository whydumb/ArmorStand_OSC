package top.fifthlight.armorstand.ui.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.minecraft.util.Util
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.manage.ModelManager
import top.fifthlight.armorstand.ui.state.ConfigScreenState
import java.nio.file.Path

class ConfigViewModel(scope: CoroutineScope) : ViewModel(scope) {
    private val _uiState = MutableStateFlow(ConfigScreenState())
    val uiState = _uiState.asStateFlow()

    private fun findLargestMultiple(base: Int, maximum: Int) = maximum / base * base

    private data class SearchParam(
        val offset: Int,
        val pageSize: Int?,
        val searchString: String,
        val order: ModelManager.Order,
        val ascend: Boolean,
    ) {
        constructor(state: ConfigScreenState): this(
            offset = state.currentOffset,
            pageSize = state.pageSize,
            searchString = state.searchString,
            order = state.order,
            ascend = state.sortAscend,
        )
    }

    init {
        with(scope) {
            launch {
                ModelManager.scheduleScan()
                fun clearItems() {
                    _uiState.getAndUpdate { state ->
                        state.copy(
                            currentPageItems = null
                        )
                    }
                }
                uiState.map(::SearchParam).distinctUntilChanged().collectLatest { param ->
                    clearItems()
                    val searchStr = param.searchString.takeIf { it.isNotBlank() }
                    ModelManager.lastScanTime.collectLatest {
                        val totalItems = ModelManager.getTotalModels(searchStr)
                        _uiState.getAndUpdate { state ->
                            state.copy(
                                totalItems = totalItems,
                                currentOffset = if (param.pageSize != null) {
                                    if (state.currentOffset + param.pageSize > totalItems) {
                                        // to last page
                                        findLargestMultiple(param.pageSize, totalItems - 1)
                                    } else {
                                        // to current page
                                        findLargestMultiple(param.pageSize, state.currentOffset)
                                    }
                                } else {
                                    0
                                },
                            )
                        }
                        param.pageSize?.let { pageSize ->
                            clearItems()
                            uiState.map { it.currentOffset }.distinctUntilChanged().collectLatest { offset ->
                                val items = ModelManager.getModel(
                                    offset = offset,
                                    length = pageSize,
                                    searchString = searchStr,
                                    order = param.order,
                                    ascend = param.ascend,
                                )
                                _uiState.getAndUpdate { state ->
                                    state.copy(currentPageItems = items)
                                }
                            }
                        }
                    }
                }
            }
            launch {
                ConfigHolder.config.collect { config ->
                    _uiState.getAndUpdate {
                        it.copy(
                            currentModel = config.modelPath,
                            showOtherPlayerModel = config.showOtherPlayerModel,
                            sendModelData = config.sendModelData,
                            modelScale = config.modelScale,
                        )
                    }
                }
            }
        }
    }

    fun selectModel(path: Path?) {
        ConfigHolder.update {
            copy(model = path?.toString())
        }
    }

    fun updatePageSize(pageSize: Int?) {
        pageSize?.let {
            check(pageSize > 0) { "Invalid page size: $pageSize" }
        }
        _uiState.getAndUpdate { state ->
            state.copy(pageSize = pageSize)
        }
    }

    fun updatePageIndex(delta: Int) {
        _uiState.getAndUpdate { state ->
            if (state.pageSize == null) {
                return@getAndUpdate state
            }
            val target = (state.currentOffset + delta * state.pageSize).coerceAtLeast(0)
            state.copy(
                currentOffset = if (target >= state.totalItems) {
                    findLargestMultiple(state.pageSize, state.totalItems - 1).coerceAtLeast(0)
                } else {
                    findLargestMultiple(
                        base = state.pageSize,
                        maximum = target,
                    ).coerceAtLeast(0)
                }
            )
        }
    }

    fun updateShowOtherPlayerModel(showOtherPlayerModel: Boolean) {
        ConfigHolder.update {
            copy(showOtherPlayerModel = showOtherPlayerModel)
        }
    }

    fun updateSendModelData(sendModelData: Boolean) {
        ConfigHolder.update {
            copy(sendModelData = sendModelData)
        }
    }

    fun updateModelScale(modelScale: Double) {
        ConfigHolder.update {
            copy(modelScale = modelScale)
        }
    }

    fun updateSearchString(searchString: String) {
        _uiState.getAndUpdate { state ->
            state.copy(searchString = searchString)
        }
    }

    fun updateSearchParam(order: ModelManager.Order, ascend: Boolean) {
        _uiState.getAndUpdate { state ->
            state.copy(
                order = order,
                sortAscend = ascend,
            )
        }
    }

    fun refreshModels() {
        scope.launch {
            ModelManager.scheduleScan()
        }
    }

    fun openModelDir() {
        Util.getOperatingSystem().open(ModelManager.modelDir.toUri())
    }
}