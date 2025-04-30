package top.fifthlight.armorstand.ui.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fifthlight.armorstand.manage.ModelManager
import top.fifthlight.armorstand.ui.state.DatabaseScreenState
import top.fifthlight.armorstand.util.execute
import top.fifthlight.armorstand.util.query
import java.sql.Connection
import java.sql.ResultSet
import java.util.Properties
import kotlin.io.use
import kotlin.time.measureTimedValue

class DatabaseViewModel(scope: CoroutineScope) : ViewModel(scope) {
    private val _uiState = MutableStateFlow(DatabaseScreenState())
    val uiState = _uiState.asStateFlow()

    fun updateQuery(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
    }

    fun submitQuery() {
        scope.launch {
            _uiState.value = _uiState.value.copy(state = DatabaseScreenState.QueryState.Loading)

            try {
                val result = withContext(Dispatchers.IO) {
                    executeQuery(_uiState.value.query)
                }
                _uiState.value = _uiState.value.copy(state = result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(state = DatabaseScreenState.QueryState.Failed(e.message))
            }
        }
    }

    private suspend fun executeQuery(query: String): DatabaseScreenState.QueryState =
        ModelManager.transaction {
            createStatement().use { stmt ->
                val (value, duration) = measureTimedValue {
                    stmt.execute(query)
                }
                if (value) {
                    val (headers, rows) = parseResultSet(stmt.resultSet)
                    DatabaseScreenState.QueryState.Result(duration, headers, rows)
                } else {
                    DatabaseScreenState.QueryState.Updated(duration, stmt.updateCount)
                }
            }
        }

    private fun parseResultSet(rs: ResultSet): Pair<List<String>, List<List<String>>> {
        val metaData = rs.metaData
        val headers = (1..metaData.columnCount).map { metaData.getColumnName(it) }
        val rows = buildList {
            while (rs.next()) {
                add((1..metaData.columnCount).map { rs.getString(it) ?: "null" })
            }
        }
        return Pair(headers, rows)
    }
}

