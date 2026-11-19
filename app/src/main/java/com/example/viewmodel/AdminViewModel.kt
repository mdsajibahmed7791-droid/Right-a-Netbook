package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NotebookRepository
import com.example.data.unwrapFirestoreError
import com.example.model.DeliveryEntry
import com.example.model.DeliveryItem
import com.example.model.FreeEntry
import com.example.model.MenuItem
import com.example.model.StaffProfile
import com.example.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth

class AdminViewModel(
    repository: NotebookRepository,
) : ViewModel() {

    val allEntriesState: StateFlow<UiState<List<FreeEntry>>> = repository.observeAllEntries()
        .map<List<FreeEntry>, UiState<List<FreeEntry>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapFirestoreError(it).message ?: "Failed to load admin entries")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allDlEntriesState: StateFlow<UiState<List<DeliveryEntry>>> = repository.observeAllDlEntries()
        .map<List<DeliveryEntry>, UiState<List<DeliveryEntry>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapFirestoreError(it).message ?: "Failed to load admin deliveries")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allStaffState: StateFlow<UiState<List<StaffProfile>>> = repository.observeAllStaff()
        .map<List<StaffProfile>, UiState<List<StaffProfile>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapFirestoreError(it).message ?: "Failed to load staff roster")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allItemsState: StateFlow<UiState<List<MenuItem>>> = repository.observeAllItems()
        .map<List<MenuItem>, UiState<List<MenuItem>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapFirestoreError(it).message ?: "Failed to load catalog")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allDlItemsState: StateFlow<UiState<List<DeliveryItem>>> = repository.observeAllDlItems()
        .map<List<DeliveryItem>, UiState<List<DeliveryItem>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapFirestoreError(it).message ?: "Failed to load delivery catalog")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private val _adminMonth = MutableStateFlow(YearMonth.now())
    val adminMonth: StateFlow<YearMonth> = _adminMonth.asStateFlow()

    private val _selectedStaffUid = MutableStateFlow<String?>(null)
    val selectedStaffUid: StateFlow<String?> = _selectedStaffUid.asStateFlow()

    fun shiftAdminMonth(deltaMonths: Long) {
        _adminMonth.update { it.plusMonths(deltaMonths) }
    }

    fun selectStaffUid(uid: String?) {
        _selectedStaffUid.value = uid
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
