package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NotebookRepository
import com.example.data.unwrapFirestoreError
import com.example.model.CartLine
import com.example.model.Category
import com.example.model.DeliveryEntry
import com.example.model.DeliveryItem
import com.example.model.FreeEntry
import com.example.model.MenuItem
import com.example.model.NoteScope
import com.example.model.Person
import com.example.model.SavedNote
import com.example.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

class MainViewModel(
    private val repository: NotebookRepository,
    val currentUserId: String,
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.upsertCurrentStaffProfile()
        }
    }

    val isAdmin: StateFlow<Boolean> = repository.observeIsAdmin(currentUserId)
        .catch { emit(false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = false,
        )

    val itemsState: StateFlow<UiState<List<MenuItem>>> = repository.observeItems(currentUserId)
        .map<List<MenuItem>, UiState<List<MenuItem>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapFirestoreError(it).message ?: "Failed to load items")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val peopleState: StateFlow<UiState<List<Person>>> = repository.observePeople(currentUserId)
        .map<List<Person>, UiState<List<Person>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapFirestoreError(it).message ?: "Failed to load people")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val entriesState: StateFlow<UiState<List<FreeEntry>>> = repository.observeEntries(currentUserId)
        .map<List<FreeEntry>, UiState<List<FreeEntry>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapFirestoreError(it).message ?: "Failed to load entries")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val notesState: StateFlow<UiState<List<SavedNote>>> = repository.observeNotes(currentUserId)
        .map<List<SavedNote>, UiState<List<SavedNote>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapFirestoreError(it).message ?: "Failed to load notes")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val dlItemsState: StateFlow<UiState<List<DeliveryItem>>> = repository.observeDlItems(currentUserId)
        .map<List<DeliveryItem>, UiState<List<DeliveryItem>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapFirestoreError(it).message ?: "Failed to load delivery items")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val dlEntriesState: StateFlow<UiState<List<DeliveryEntry>>> = repository.observeDlEntries(currentUserId)
        .map<List<DeliveryEntry>, UiState<List<DeliveryEntry>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapFirestoreError(it).message ?: "Failed to load delivery logs")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    // ---- Free Tab POS state (survives rotation) ----
    private val _freeSelectedCategory = MutableStateFlow<Category?>(null)
    val freeSelectedCategory: StateFlow<Category?> = _freeSelectedCategory.asStateFlow()

    private val _freeSearchQuery = MutableStateFlow("")
    val freeSearchQuery: StateFlow<String> = _freeSearchQuery.asStateFlow()

    private val _freeCart = MutableStateFlow<List<CartLine>>(emptyList())
    val freeCart: StateFlow<List<CartLine>> = _freeCart.asStateFlow()

    // ---- Delivery Tab POS state (survives rotation) ----
    private val _deliverySelectedCategory = MutableStateFlow<String?>(null) // null = All, "delivery_only", or Category.key
    val deliverySelectedCategory: StateFlow<String?> = _deliverySelectedCategory.asStateFlow()

    private val _deliverySearchQuery = MutableStateFlow("")
    val deliverySearchQuery: StateFlow<String> = _deliverySearchQuery.asStateFlow()

    private val _deliveryCart = MutableStateFlow<List<CartLine>>(emptyList())
    val deliveryCart: StateFlow<List<CartLine>> = _deliveryCart.asStateFlow()

    // ---- Report Filters (survive rotation) ----
    private val _reportMonth = MutableStateFlow(YearMonth.now())
    val reportMonth: StateFlow<YearMonth> = _reportMonth.asStateFlow()

    private val _reportPersonFilter = MutableStateFlow<String?>(null)
    val reportPersonFilter: StateFlow<String?> = _reportPersonFilter.asStateFlow()

    private val _deliveryReportMonth = MutableStateFlow(YearMonth.now())
    val deliveryReportMonth: StateFlow<YearMonth> = _deliveryReportMonth.asStateFlow()

    // ---- Free POS Actions ----
    fun selectFreeCategory(category: Category?) {
        _freeSelectedCategory.value = category
    }

    fun updateFreeSearch(query: String) {
        _freeSearchQuery.value = query
    }

    fun addToFreeCart(
        productId: String,
        baseName: String,
        cat: String,
        unitPrice: Double,
        selectedNotes: List<String>,
        qty: Int,
    ) {
        if (qty < 1) return
        val normalizedNotes = selectedNotes.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        val key = "$productId|${normalizedNotes.joinToString(",")}"
        _freeCart.update { current ->
            val existing = current.indexOfFirst { it.lineKey == key }
            if (existing >= 0) {
                current.toMutableList().apply {
                    val old = this[existing]
                    this[existing] = old.copy(qty = old.qty + qty)
                }
            } else {
                current + CartLine(
                    lineKey = key,
                    productId = productId,
                    baseName = baseName,
                    cat = cat,
                    unitPrice = unitPrice,
                    notes = normalizedNotes,
                    qty = qty,
                )
            }
        }
    }

    fun updateFreeCartLineQty(lineKey: String, delta: Int) {
        _freeCart.update { current ->
            current.mapNotNull { line ->
                if (line.lineKey == lineKey) {
                    val nextQty = line.qty + delta
                    if (nextQty <= 0) null else line.copy(qty = nextQty)
                } else {
                    line
                }
            }
        }
    }

    fun clearFreeCart() {
        _freeCart.value = emptyList()
    }

    // ---- Delivery POS Actions ----
    fun selectDeliveryCategory(catKey: String?) {
        _deliverySelectedCategory.value = catKey
    }

    fun updateDeliverySearch(query: String) {
        _deliverySearchQuery.value = query
    }

    fun addToDeliveryCart(
        productId: String,
        baseName: String,
        selectedNotes: List<String>,
        qty: Int,
    ) {
        if (qty < 1) return
        val normalizedNotes = selectedNotes.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        val key = "$productId|${normalizedNotes.joinToString(",")}"
        _deliveryCart.update { current ->
            val existing = current.indexOfFirst { it.lineKey == key }
            if (existing >= 0) {
                current.toMutableList().apply {
                    val old = this[existing]
                    this[existing] = old.copy(qty = old.qty + qty)
                }
            } else {
                current + CartLine(
                    lineKey = key,
                    productId = productId,
                    baseName = baseName,
                    cat = "",
                    unitPrice = 0.0,
                    notes = normalizedNotes,
                    qty = qty,
                )
            }
        }
    }

    fun updateDeliveryCartLineQty(lineKey: String, delta: Int) {
        _deliveryCart.update { current ->
            current.mapNotNull { line ->
                if (line.lineKey == lineKey) {
                    val nextQty = line.qty + delta
                    if (nextQty <= 0) null else line.copy(qty = nextQty)
                } else {
                    line
                }
            }
        }
    }

    fun clearDeliveryCart() {
        _deliveryCart.value = emptyList()
    }

    // ---- Report Filters ----
    fun shiftReportMonth(deltaMonths: Long) {
        _reportMonth.update { it.plusMonths(deltaMonths) }
    }

    fun setReportPersonFilter(person: String?) {
        _reportPersonFilter.value = person
    }

    fun shiftDeliveryReportMonth(deltaMonths: Long) {
        _deliveryReportMonth.update { it.plusMonths(deltaMonths) }
    }

    // ---- Repository Mutations ----
    suspend fun addMenuItem(cat: Category, name: String, price: Double, photoBase64: String?): Result<Unit> =
        repository.addItem(cat, name, price, photoBase64)

    suspend fun updateMenuItem(
        itemId: String,
        cat: Category,
        name: String,
        price: Double,
        photoBase64: String? = null,
        clearPhoto: Boolean = false,
    ): Result<Unit> = repository.updateItem(itemId, cat, name, price, photoBase64, clearPhoto)

    suspend fun updateMenuItemPhoto(itemId: String, photoBase64: String): Result<Unit> =
        repository.updateItemPhoto(itemId, photoBase64)

    suspend fun deleteMenuItem(itemId: String): Result<Unit> =
        repository.deleteItem(itemId)

    suspend fun addPerson(name: String): Result<Unit> =
        repository.addPerson(name)

    suspend fun updatePerson(personId: String, name: String): Result<Unit> =
        repository.updatePerson(personId, name)

    suspend fun deletePerson(personId: String): Result<Unit> =
        repository.deletePerson(personId)

    suspend fun addNote(scope: NoteScope, itemId: String, text: String): Result<Unit> =
        repository.addNote(scope, itemId, text)

    suspend fun deleteNote(noteId: String): Result<Unit> =
        repository.deleteNote(noteId)

    suspend fun saveFreeOrder(
        personName: String,
        tsMillis: Long,
        orderNote: String?,
        existingPeopleNames: Set<String>,
    ): Result<Unit> {
        val lines = _freeCart.value
        val result = repository.saveFreeOrder(lines, personName, tsMillis, orderNote, existingPeopleNames)
        if (result.isSuccess) {
            _freeCart.value = emptyList()
        }
        return result
    }

    suspend fun deleteEntry(entryId: String): Result<Unit> =
        repository.deleteEntry(entryId)

    suspend fun addDeliveryItem(name: String, photoBase64: String?): Result<Unit> =
        repository.addDlItem(name, photoBase64)

    suspend fun deleteDeliveryItem(dlItemId: String): Result<Unit> =
        repository.deleteDlItem(dlItemId)

    suspend fun saveDeliveryOrder(tsMillis: Long, orderNote: String?): Result<Unit> {
        val lines = _deliveryCart.value
        val result = repository.saveDeliveryOrder(lines, tsMillis, orderNote)
        if (result.isSuccess) {
            _deliveryCart.value = emptyList()
        }
        return result
    }

    suspend fun deleteDeliveryEntry(dlEntryId: String): Result<Unit> =
        repository.deleteDlEntry(dlEntryId)

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
