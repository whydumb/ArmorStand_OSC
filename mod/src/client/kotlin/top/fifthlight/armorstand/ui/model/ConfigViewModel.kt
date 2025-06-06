package top.fifthlight.armorstand.ui.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
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
                uiState.map { it.pageSize }.distinctUntilChanged().collectLatest { pageSize ->
                    clearItems()
                    ModelManager.lastScanTime.collectLatest {
                        val totalItems = ModelManager.getTotalModels()
                        _uiState.getAndUpdate { state ->
                            state.copy(
                                totalItems = totalItems,
                                currentOffset = if (pageSize != null) {
                                    if (state.currentOffset + pageSize > totalItems) {
                                        // to last page
                                        findLargestMultiple(pageSize, totalItems - 1)
                                    } else {
                                        // to current page
                                        findLargestMultiple(pageSize, state.currentOffset)
                                    }
                                } else {
                                    0
                                },
                            )
                        }
                        pageSize?.let { pageSize ->
                            clearItems()
                            uiState.map { it.currentOffset }.distinctUntilChanged().collectLatest { offset ->
                                val items = ModelManager.getModel(offset, pageSize)
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

    fun selectModel(path: Path) {
        ConfigHolder.update {
            copy(model = path.toString())
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

    fun refreshModels() {
        scope.launch {
            ModelManager.scheduleScan()
        }
    }

    fun openModelDir() {
        Util.getOperatingSystem().open(ModelManager.modelDir.toUri())
    }
}