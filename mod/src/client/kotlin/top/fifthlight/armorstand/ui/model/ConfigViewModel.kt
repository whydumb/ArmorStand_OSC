package top.fifthlight.armorstand.ui.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.state.ModelInstanceManager
import top.fifthlight.armorstand.ui.state.ConfigScreenState
import top.fifthlight.armorstand.util.ModelLoaders
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.relativeTo
import kotlin.io.path.visitFileTree

class ConfigViewModel(scope: CoroutineScope) : ViewModel(scope) {
    private val _uiState = MutableStateFlow(ConfigScreenState())
    val uiState = _uiState.asStateFlow()

    suspend fun loadItems() {
        val modelDir = ModelInstanceManager.modelDir
        withContext(Dispatchers.IO) {
            val items = runCatching {
                buildList {
                    modelDir.visitFileTree(maxDepth = 4) {
                        onVisitFile { path, attributes ->
                            if (path.extension.lowercase() in ModelLoaders.modelExtensions) {
                                add(path.relativeTo(modelDir))
                            }
                            FileVisitResult.CONTINUE
                        }
                    }
                }
            }.getOrNull() ?: listOf()
            _uiState.getAndUpdate {
                it.copy(modelPaths = items)
            }
        }
    }

    init {
        with(scope) {
            launch {
                loadItems()
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
}