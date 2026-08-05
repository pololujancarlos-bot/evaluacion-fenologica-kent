package com.example.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.EvaluationEntity
import com.example.data.local.SampleData
import com.example.data.repository.EvaluationRepository
import com.example.domain.AgronomicAnalyzer
import com.example.domain.AgronomicSummary
import com.example.domain.FieldComparisonResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SyncUiState {
    object Idle : SyncUiState
    object Syncing : SyncUiState
    data class Success(val message: String) : SyncUiState
    data class Error(val message: String) : SyncUiState
}

class EvaluationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EvaluationRepository

    init {
        val dao = AppDatabase.getDatabase(application).evaluationDao()
        repository = EvaluationRepository(dao, application)

        // Seed initial sample data if database is empty on first run
        viewModelScope.launch {
            val currentList = dao.getAllEvaluations().first()
            if (currentList.isEmpty()) {
                SampleData.getSampleEvaluations().forEach { sample ->
                    dao.insertEvaluation(sample)
                }
            }
        }
    }

    val pendingCount: StateFlow<Int> = repository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedSectorFilter = MutableStateFlow("Todos")
    val selectedSectorFilter = _selectedSectorFilter.asStateFlow()

    private val _selectedLoteFilter = MutableStateFlow("Todos")
    val selectedLoteFilter = _selectedLoteFilter.asStateFlow()

    private val _selectedPlantFilter = MutableStateFlow("Todas")
    val selectedPlantFilter = _selectedPlantFilter.asStateFlow()

    private val _syncUiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncUiState = _syncUiState.asStateFlow()

    val evaluationsList: StateFlow<List<EvaluationEntity>> = combine(
        repository.allEvaluations,
        _searchQuery,
        _selectedSectorFilter,
        _selectedLoteFilter,
        _selectedPlantFilter
    ) { list, query, sector, lote, plant ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.tecnico.contains(query, ignoreCase = true) ||
                    item.lote.contains(query, ignoreCase = true) ||
                    item.sector.contains(query, ignoreCase = true) ||
                    item.observaciones.contains(query, ignoreCase = true)

            val matchesSector = sector == "Todos" || item.sector == sector
            val matchesLote = lote == "Todos" || item.lote == lote
            val matchesPlant = plant == "Todas" || item.planta == plant

            matchesQuery && matchesSector && matchesLote && matchesPlant
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fieldComparisons: StateFlow<List<FieldComparisonResult>> = repository.allEvaluations
        .combine(_selectedSectorFilter) { list, _ ->
            AgronomicAnalyzer.compareFields(list)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSectorFilter(sector: String) {
        _selectedSectorFilter.value = sector
    }

    fun setLoteFilter(lote: String) {
        _selectedLoteFilter.value = lote
    }

    fun setPlantFilter(plant: String) {
        _selectedPlantFilter.value = plant
    }

    fun saveEvaluation(entity: EvaluationEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.saveEvaluation(entity)
            onComplete()
        }
    }

    fun deleteEvaluation(id: String) {
        viewModelScope.launch {
            repository.deleteEvaluation(id)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _syncUiState.value = SyncUiState.Syncing
            val result = repository.syncPendingRecords()
            result.fold(
                onSuccess = { count ->
                    _syncUiState.value = SyncUiState.Success(
                        if (count > 0) "¡$count registros sincronizados correctamente con la nube!"
                        else "No hay registros pendientes de sincronización."
                    )
                },
                onFailure = { err ->
                    _syncUiState.value = SyncUiState.Error(err.message ?: "Error al sincronizar")
                }
            )
        }
    }

    fun resetSyncState() {
        _syncUiState.value = SyncUiState.Idle
    }

    fun getAnalysisFor(entity: EvaluationEntity): AgronomicSummary {
        return AgronomicAnalyzer.analyzeRecord(entity)
    }
}
