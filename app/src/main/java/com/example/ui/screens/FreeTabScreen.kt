package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.CartLine
import com.example.model.CatalogProduct
import com.example.model.Category
import com.example.model.FreeEntry
import com.example.model.NoteScope
import com.example.model.Person
import com.example.model.UiState
import com.example.ui.components.CategoryTileGrid
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DateTimePickerRow
import com.example.ui.components.FloatingOrderBar
import com.example.ui.components.GlassSearchBar
import com.example.ui.components.IllustratedEmptyStateCard
import com.example.ui.components.ItemNotesBottomSheet
import com.example.ui.components.LottieButtonSpinner
import com.example.ui.components.LottieLoadingCard
import com.example.ui.components.PosProductCard
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCard
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealGlowButton
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun FreeTabScreen(
    viewModel: MainViewModel,
    langCode: String,
    onShowMessage: (String) -> Unit,
    onTaskSuccess: (String) -> Unit = onShowMessage,
) {
    val itemsState by viewModel.itemsState.collectAsStateWithLifecycle()
    val peopleState by viewModel.peopleState.collectAsStateWithLifecycle()
    val entriesState by viewModel.entriesState.collectAsStateWithLifecycle()
    val notesState by viewModel.notesState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.freeSelectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.freeSearchQuery.collectAsStateWithLifecycle()
    val cart by viewModel.freeCart.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var activeProduct by remember { mutableStateOf<CatalogProduct?>(null) }
    var showReviewSheet by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<FreeEntry?>(null) }

    val allItems = (itemsState as? UiState.Success)?.data.orEmpty()
    val allPeople = (peopleState as? UiState.Success)?.data.orEmpty()
    val allEntries = (entriesState as? UiState.Success)?.data.orEmpty()
    val allNotes = (notesState as? UiState.Success)?.data.orEmpty()

    val filteredProducts = remember(allItems, selectedCategory, searchQuery) {
        val q = searchQuery.trim()
        allItems.filter { item ->
            if (q.isNotEmpty()) {
                item.name.contains(q, ignoreCase = true)
            } else {
                selectedCategory == null || item.cat.equals(selectedCategory!!.key, ignoreCase = true)
            }
        }.map {
            CatalogProduct(
                id = it.id,
                name = it.name,
                cat = it.cat,
                price = it.price,
                photo = it.photo,
            )
        }
    }

    val qtyByProductId = remember(cart) {
        cart.groupBy { it.productId }.mapValues { (_, lines) -> lines.sumOf { it.qty } }
    }
    val totalPieces = remember(cart) { cart.sumOf { it.qty } }
    val totalPrice = remember(cart) { cart.sumOf { it.lineTotal } }
    val srUnit = stringResource(R.string.currency_sr)
    val savedSuccessMsg = stringResource(R.string.saved_success)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (cart.isNotEmpty()) 96.dp else 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CategoryTileGrid(
                    selectedKey = selectedCategory?.key,
                    onSelectKey = { key ->
                        viewModel.selectFreeCategory(key?.let { Category.fromKey(it) })
                    },
                )
            }

            item {
                GlassSearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::updateFreeSearch,
                )
            }

            when (val state = itemsState) {
                is UiState.Loading -> {
                    item {
                        LottieLoadingCard()
                    }
                }
                is UiState.Error -> {
                    item {
                        GlassCard(errorBorder = true, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = state.message,
                                color = ErrorCoral,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (filteredProducts.isEmpty()) {
                        item {
                            IllustratedEmptyStateCard(
                                imageRes = R.drawable.img_empty_items_1795044790155,
                                message = stringResource(R.string.no_items_found),
                            )
                        }
                    } else {
                        items(filteredProducts.chunked(2), key = { row -> row.joinToString("_") { it.id } }) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                rowItems.forEach { product ->
                                    PosProductCard(
                                        product = product,
                                        addedQty = qtyByProductId[product.id] ?: 0,
                                        showPrice = true,
                                        langCode = langCode,
                                        onClick = { activeProduct = product },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (rowItems.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.recent_entries),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
            }

            val recentEntries = allEntries.take(15)
            if (recentEntries.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.no_entries_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                items(recentEntries, key = { it.id }) { entry ->
                    GlassCard(
                        cornerRadius = 14.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${entry.item} × ${FormatUtils.formatNumber(entry.qty, langCode)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${entry.person} • ${FormatUtils.formatDateTime(entry.ts, langCode)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                                if (!entry.note.isNullOrBlank()) {
                                    Text(
                                        text = entry.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                    )
                                }
                            }
                            Text(
                                text = FormatUtils.formatCurrency(entry.total, langCode, srUnit),
                                style = MonoNumberStyle,
                                color = TealBright,
                            )
                            IconButton(
                                onClick = { entryToDelete = entry },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = ErrorCoral,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (cart.isNotEmpty()) {
            FloatingOrderBar(
                totalPieces = totalPieces,
                totalPrice = totalPrice,
                langCode = langCode,
                onReviewClick = { showReviewSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )
        }
    }

    activeProduct?.let { product ->
        val itemNotes = allNotes.filter {
            it.scope == NoteScope.FREE.key && it.itemId == product.id
        }
        ItemNotesBottomSheet(
            product = product,
            savedNotes = itemNotes,
            showPrice = true,
            langCode = langCode,
            onDismiss = { activeProduct = null },
            onAddSavedNote = { text -> viewModel.addNote(NoteScope.FREE, product.id, text) },
            onDeleteSavedNote = { noteId -> viewModel.deleteNote(noteId) },
            onAddToOrder = { selectedNotes, qty ->
                viewModel.addToFreeCart(
                    productId = product.id,
                    baseName = product.name,
                    cat = product.cat ?: Category.FOOD.key,
                    unitPrice = product.price,
                    selectedNotes = selectedNotes,
                    qty = qty,
                )
                activeProduct = null
            },
        )
    }

    if (showReviewSheet && cart.isNotEmpty()) {
        FreeOrderReviewSheet(
            lines = cart,
            people = allPeople,
            langCode = langCode,
            onDismiss = { showReviewSheet = false },
            onUpdateQty = viewModel::updateFreeCartLineQty,
            onSave = { personName, tsMillis, note ->
                val res = viewModel.saveFreeOrder(
                    personName = personName,
                    tsMillis = tsMillis,
                    orderNote = note,
                    existingPeopleNames = allPeople.map { it.name }.toSet(),
                )
                if (res.isSuccess) {
                    showReviewSheet = false
                    onTaskSuccess(savedSuccessMsg)
                }
                res
            },
        )
    }

    entryToDelete?.let { entry ->
        ConfirmDeleteDialog(
            itemName = entry.item,
            onConfirm = {
                val id = entry.id
                entryToDelete = null
                scope.launch {
                    val res = viewModel.deleteEntry(id)
                    if (res.isFailure) {
                        onShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Delete failed")
                    }
                }
            },
            onDismiss = { entryToDelete = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreeOrderReviewSheet(
    lines: List<CartLine>,
    people: List<Person>,
    langCode: String,
    onDismiss: () -> Unit,
    onUpdateQty: (lineKey: String, delta: Int) -> Unit,
    onSave: suspend (personName: String, tsMillis: Long, orderNote: String?) -> Result<Unit>,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val srUnit = stringResource(R.string.currency_sr)

    var selectedDropdownPerson by remember { mutableStateOf("") }
    var newPersonInput by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var tsMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var orderNote by remember { mutableStateOf("") }
    var showTakenByError by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val effectivePerson = newPersonInput.trim().ifEmpty { selectedDropdownPerson.trim() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E1D2B),
        contentColor = TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.review_order),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )

            lines.forEach { line ->
                GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = line.baseName,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            if (line.notes.isNotEmpty()) {
                                Text(
                                    text = line.notes.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TealBright,
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.unit_price_label,
                                    FormatUtils.formatCurrency(line.unitPrice, langCode, srUnit),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                border = BorderStroke(1.dp, GlassBorder),
                                color = Color.Transparent,
                                modifier = Modifier.size(48.dp),
                                onClick = { onUpdateQty(line.lineKey, -1) },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Remove, contentDescription = "-", tint = TextPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                text = FormatUtils.formatNumber(line.qty, langCode),
                                style = MonoNumberStyle,
                                color = TealBright,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                            Surface(
                                shape = CircleShape,
                                border = BorderStroke(1.dp, TealPrimary),
                                color = Color.Transparent,
                                modifier = Modifier.size(48.dp),
                                onClick = { onUpdateQty(line.lineKey, 1) },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Add, contentDescription = "+", tint = TealBright, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Taken by dropdown
            Text(
                text = stringResource(R.string.taken_by),
                style = MaterialTheme.typography.titleSmall,
                color = if (showTakenByError) ErrorCoral else TextSecondary,
            )

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
            ) {
                OutlinedTextField(
                    value = selectedDropdownPerson,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(stringResource(R.string.select_person), color = TextMuted) },
                    trailingIcon = {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = TealPrimary)
                    },
                    isError = showTakenByError && newPersonInput.isBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("taken_by_dropdown"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (showTakenByError) ErrorCoral else TealPrimary,
                        unfocusedBorderColor = if (showTakenByError) ErrorCoral else GlassBorder,
                        errorBorderColor = ErrorCoral,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                ) {
                    people.forEach { person ->
                        DropdownMenuItem(
                            text = { Text(person.name) },
                            onClick = {
                                selectedDropdownPerson = person.name
                                newPersonInput = ""
                                showTakenByError = false
                                dropdownExpanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = newPersonInput,
                onValueChange = {
                    newPersonInput = it
                    if (it.isNotBlank()) showTakenByError = false
                },
                singleLine = true,
                isError = showTakenByError,
                placeholder = { Text(stringResource(R.string.new_person_name), color = TextMuted) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_person_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (showTakenByError) ErrorCoral else TealPrimary,
                    unfocusedBorderColor = if (showTakenByError) ErrorCoral else GlassBorder,
                    errorBorderColor = ErrorCoral,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
            )

            if (showTakenByError) {
                Text(
                    text = stringResource(R.string.taken_by_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorCoral,
                )
            }

            DateTimePickerRow(
                tsMillis = tsMillis,
                langCode = langCode,
                onTsChanged = { tsMillis = it },
            )

            OutlinedTextField(
                value = orderNote,
                onValueChange = { orderNote = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.optional_note_hint), color = TextMuted) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
            )

            if (saveError != null) {
                Text(
                    text = saveError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorCoral,
                )
            }

            val total = lines.sumOf { it.lineTotal }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = FormatUtils.formatCurrency(total, langCode, srUnit),
                    style = MaterialTheme.typography.titleLarge,
                    color = TealBright,
                )
                TealGlowButton(
                    onClick = {
                        if (effectivePerson.isEmpty()) {
                            showTakenByError = true
                            return@TealGlowButton
                        }
                        if (isSaving) return@TealGlowButton
                        isSaving = true
                        saveError = null
                        scope.launch {
                            val res = onSave(effectivePerson, tsMillis, orderNote)
                            isSaving = false
                            if (res.isFailure) {
                                saveError = res.exceptionOrNull()?.localizedMessage ?: "Failed to save"
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.testTag("save_free_order_button"),
                ) {
                    if (isSaving) {
                        LottieButtonSpinner(size = 24.dp)
                    } else {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
