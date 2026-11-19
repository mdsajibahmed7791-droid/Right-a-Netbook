package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.CartLine
import com.example.model.CatalogProduct
import com.example.model.DayStat
import com.example.model.DeliveryEntry
import com.example.model.DeliveryItem
import com.example.model.NameValueStat
import com.example.model.NoteScope
import com.example.model.UiState
import com.example.pdf.PdfReportGenerator
import com.example.pdf.PdfReportSpec
import com.example.pdf.PdfSummaryBox
import com.example.pdf.PdfTableData
import com.example.ui.components.CategoryTileGrid
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DailyBarChartCard
import com.example.ui.components.DateTimePickerRow
import com.example.ui.components.FloatingOrderBar
import com.example.ui.components.GlassSearchBar
import com.example.ui.components.IllustratedEmptyStateCard
import com.example.ui.components.ItemNotesBottomSheet
import com.example.ui.components.ItemThumbnail
import com.example.ui.components.LottieButtonSpinner
import com.example.ui.components.LottieLoadingCard
import com.example.ui.components.MonthPickerBar
import com.example.ui.components.PdfExportBar
import com.example.ui.components.PosProductCard
import com.example.ui.components.SummaryMetricTile
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealGlowButton
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.util.ImageUtils
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeliveryTabScreen(
    viewModel: MainViewModel,
    langCode: String,
    onShowMessage: (String) -> Unit,
    onTaskSuccess: (String) -> Unit = onShowMessage,
) {
    var modeIndex by remember { mutableIntStateOf(0) } // 0 = Log POS, 1 = Report & Items

    Column(modifier = Modifier.fillMaxSize()) {
        // Segmented toggle pill
        GlassCard(
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                listOf(
                    0 to stringResource(R.string.delivery_log_mode),
                    1 to stringResource(R.string.delivery_report_mode),
                ).forEach { (idx, label) ->
                    val selected = modeIndex == idx
                    Surface(
                        shape = CircleShape,
                        color = if (selected) TealPrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { modeIndex = idx }
                            .testTag("delivery_mode_$idx"),
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) NavyDeep else TextSecondary,
                            )
                        }
                    }
                }
            }
        }

        if (modeIndex == 0) {
            DeliveryPosContent(viewModel, langCode, onShowMessage, onTaskSuccess)
        } else {
            DeliveryReportAndItemsContent(viewModel, langCode, onShowMessage, onTaskSuccess)
        }
    }
}

@Composable
private fun DeliveryPosContent(
    viewModel: MainViewModel,
    langCode: String,
    onShowMessage: (String) -> Unit,
    onTaskSuccess: (String) -> Unit,
) {
    val itemsState by viewModel.itemsState.collectAsStateWithLifecycle()
    val dlItemsState by viewModel.dlItemsState.collectAsStateWithLifecycle()
    val dlEntriesState by viewModel.dlEntriesState.collectAsStateWithLifecycle()
    val notesState by viewModel.notesState.collectAsStateWithLifecycle()

    val selectedCat by viewModel.deliverySelectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.deliverySearchQuery.collectAsStateWithLifecycle()
    val cart by viewModel.deliveryCart.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var activeProduct by remember { mutableStateOf<CatalogProduct?>(null) }
    var showReviewSheet by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<DeliveryEntry?>(null) }

    val menuItems = (itemsState as? UiState.Success)?.data.orEmpty()
    val dlItems = (dlItemsState as? UiState.Success)?.data.orEmpty()
    val dlEntries = (dlEntriesState as? UiState.Success)?.data.orEmpty()
    val allNotes = (notesState as? UiState.Success)?.data.orEmpty()

    val combinedCatalog = remember(menuItems, dlItems) {
        val fromMenu = menuItems.map {
            CatalogProduct(
                id = it.id,
                name = it.name,
                cat = it.cat,
                price = 0.0,
                photo = it.photo,
                isDeliveryOnly = false,
            )
        }
        val fromDl = dlItems.map {
            CatalogProduct(
                id = "dl_${it.id}",
                name = it.name,
                cat = "delivery_only",
                price = 0.0,
                photo = it.photo,
                isDeliveryOnly = true,
            )
        }
        fromMenu + fromDl
    }

    val filteredProducts = remember(combinedCatalog, selectedCat, searchQuery) {
        val q = searchQuery.trim()
        combinedCatalog.filter { prod ->
            if (q.isNotEmpty()) {
                prod.name.contains(q, ignoreCase = true)
            } else {
                selectedCat == null || prod.cat.equals(selectedCat, ignoreCase = true)
            }
        }
    }

    val qtyByProductId = remember(cart) {
        cart.groupBy { it.productId }.mapValues { (_, lines) -> lines.sumOf { it.qty } }
    }
    val totalPieces = remember(cart) { cart.sumOf { it.qty } }
    val savedMsg = stringResource(R.string.saved_success)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = if (cart.isNotEmpty()) 96.dp else 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CategoryTileGrid(
                    selectedKey = selectedCat,
                    onSelectKey = viewModel::selectDeliveryCategory,
                    includeDeliveryOnlyFilter = true,
                )
            }

            item {
                GlassSearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::updateDeliverySearch,
                )
            }

            if (itemsState is UiState.Loading && dlItemsState is UiState.Loading) {
                item {
                    LottieLoadingCard()
                }
            } else if (filteredProducts.isEmpty()) {
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
                                showPrice = false, // NO money anywhere in Delivery tab
                                langCode = langCode,
                                onClick = { activeProduct = product },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.recent_deliveries),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
            }

            val recent = dlEntries.take(15)
            if (recent.isEmpty()) {
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
                items(recent, key = { it.id }) { entry ->
                    GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${entry.item} × ${FormatUtils.formatNumber(entry.qty, langCode)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = FormatUtils.formatDateTime(entry.ts, langCode),
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
                totalPrice = null, // NO money anywhere
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
            it.scope == NoteScope.DELIVERY.key && it.itemId == product.id
        }
        ItemNotesBottomSheet(
            product = product,
            savedNotes = itemNotes,
            showPrice = false,
            langCode = langCode,
            onDismiss = { activeProduct = null },
            onAddSavedNote = { text -> viewModel.addNote(NoteScope.DELIVERY, product.id, text) },
            onDeleteSavedNote = { noteId -> viewModel.deleteNote(noteId) },
            onAddToOrder = { selectedNotes, qty ->
                viewModel.addToDeliveryCart(
                    productId = product.id,
                    baseName = product.name,
                    selectedNotes = selectedNotes,
                    qty = qty,
                )
                activeProduct = null
            },
        )
    }

    if (showReviewSheet && cart.isNotEmpty()) {
        DeliveryReviewSheet(
            lines = cart,
            langCode = langCode,
            onDismiss = { showReviewSheet = false },
            onUpdateQty = viewModel::updateDeliveryCartLineQty,
            onSave = { tsMillis, note ->
                val res = viewModel.saveDeliveryOrder(tsMillis, note)
                if (res.isSuccess) {
                    showReviewSheet = false
                    onTaskSuccess(savedMsg)
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
                scope.launch { viewModel.deleteDeliveryEntry(id) }
            },
            onDismiss = { entryToDelete = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryReviewSheet(
    lines: List<CartLine>,
    langCode: String,
    onDismiss: () -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onSave: suspend (Long, String?) -> Result<Unit>,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var tsMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

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
                text = stringResource(R.string.review_delivery),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )

            lines.forEach { line ->
                GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(line.baseName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            if (line.notes.isNotEmpty()) {
                                Text(line.notes.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = TealBright)
                            }
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

            DateTimePickerRow(
                tsMillis = tsMillis,
                langCode = langCode,
                onTsChanged = { tsMillis = it },
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
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
                Text(saveError.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
            }

            TealGlowButton(
                onClick = {
                    if (isSaving) return@TealGlowButton
                    isSaving = true
                    saveError = null
                    scope.launch {
                        val res = onSave(tsMillis, note)
                        isSaving = false
                        if (res.isFailure) {
                            saveError = res.exceptionOrNull()?.localizedMessage ?: "Failed to save"
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().testTag("save_delivery_button"),
            ) {
                if (isSaving) {
                    LottieButtonSpinner(size = 24.dp)
                } else {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DeliveryReportAndItemsContent(
    viewModel: MainViewModel,
    langCode: String,
    onShowMessage: (String) -> Unit,
    onTaskSuccess: (String) -> Unit,
) {
    val dlEntriesState by viewModel.dlEntriesState.collectAsStateWithLifecycle()
    val dlItemsState by viewModel.dlItemsState.collectAsStateWithLifecycle()
    val itemsState by viewModel.itemsState.collectAsStateWithLifecycle()
    val month by viewModel.deliveryReportMonth.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dlNameInput by remember { mutableStateOf("") }
    var dlPhotoBase64 by remember { mutableStateOf<String?>(null) }
    var dlFormError by remember { mutableStateOf<String?>(null) }
    var isSavingDlItem by remember { mutableStateOf(false) }
    var dlItemToDelete by remember { mutableStateOf<DeliveryItem?>(null) }

    val errItemName = stringResource(R.string.error_item_name)
    val savedMsg = stringResource(R.string.saved_success)
    val pdfSavedMsg = stringResource(R.string.pdf_saved)
    val pdfErrorMsg = stringResource(R.string.pdf_error)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val encoded = withContext(Dispatchers.IO) {
                    ImageUtils.encodeUriToBase64Thumbnail(context, uri)
                }
                if (encoded != null) dlPhotoBase64 = encoded
            }
        }
    }

    val allDlEntries = (dlEntriesState as? UiState.Success)?.data.orEmpty()
    val allDlItems = (dlItemsState as? UiState.Success)?.data.orEmpty()
    val menuItems = (itemsState as? UiState.Success)?.data.orEmpty()

    val photoByBaseName = remember(menuItems, allDlItems) {
        buildMap {
            menuItems.forEach { put(it.name.lowercase(), it.photo) }
            allDlItems.forEach { put(it.name.lowercase(), it.photo) }
        }
    }

    val monthEntries = remember(allDlEntries, month) {
        allDlEntries.filter { FormatUtils.yearMonthOf(it.ts) == month }
    }

    val monthPieces = remember(monthEntries) { monthEntries.sumOf { it.qty.toInt() } }
    val todayPieces = remember(allDlEntries) {
        allDlEntries.filter { FormatUtils.isToday(it.ts) }.sumOf { it.qty.toInt() }
    }
    val deliveryDaysCount = remember(monthEntries) {
        monthEntries.map { FormatUtils.dayOfMonthOf(it.ts) }.distinct().size
    }

    val perDayStats = remember(monthEntries, langCode) {
        monthEntries.groupBy { FormatUtils.dayOfMonthOf(it.ts) }
            .map { (day, list) ->
                val itemSummary = list.groupBy { it.item }
                    .map { (itemLabel, group) ->
                        "$itemLabel ×${FormatUtils.formatNumber(group.sumOf { it.qty.toInt() }, langCode)}"
                    }
                    .joinToString(", ")
                DayStat(
                    dayOfMonth = day,
                    pieces = list.sumOf { it.qty.toInt() },
                    amount = 0.0,
                    summaryText = itemSummary,
                )
            }
            .sortedByDescending { it.dayOfMonth }
    }

    val perItemStats = remember(monthEntries, photoByBaseName) {
        monthEntries.groupBy { it.base.ifBlank { it.item } }
            .map { (name, list) ->
                NameValueStat(
                    name = name,
                    pieces = list.sumOf { it.qty.toInt() },
                    amount = 0.0,
                    photo = photoByBaseName[name.lowercase()],
                )
            }
            .sortedByDescending { it.pieces }
    }

    val appName = stringResource(R.string.app_name)
    val pdfTitle = stringResource(R.string.pdf_delivery_title)
    val monthPiecesLabel = stringResource(R.string.delivery_this_month)
    val todayPiecesLabel = stringResource(R.string.delivery_today)
    val deliveryDaysLabel = stringResource(R.string.delivery_days)
    val dailyChartLabel = stringResource(R.string.daily_chart)
    val byItemLabel = stringResource(R.string.by_item)
    val perDayLabel = stringResource(R.string.per_day_log)
    val colDay = stringResource(R.string.pdf_col_day)
    val colDetails = stringResource(R.string.pdf_col_details)
    val colPieces = stringResource(R.string.pdf_col_pieces)
    val pageFooterPattern = stringResource(R.string.pdf_page_footer)
    val srUnit = stringResource(R.string.currency_sr)

    fun buildSpec(): PdfReportSpec = PdfReportSpec(
        title = pdfTitle,
        month = month,
        scopeLabel = appName,
        langCode = langCode,
        appName = appName,
        summaryBoxes = listOf(
            PdfSummaryBox(monthPiecesLabel, FormatUtils.formatNumber(monthPieces, langCode)),
            PdfSummaryBox(todayPiecesLabel, FormatUtils.formatNumber(todayPieces, langCode)),
            PdfSummaryBox(deliveryDaysLabel, FormatUtils.formatNumber(deliveryDaysCount, langCode)),
        ),
        dailyChartTitle = dailyChartLabel,
        dailyStats = perDayStats,
        chartUseCurrency = false,
        byItemTitle = byItemLabel,
        byItemStats = perItemStats,
        showMoneyInItems = false,
        tables = listOf(
            PdfTableData(
                title = perDayLabel,
                columns = listOf(colDay, colDetails, colPieces),
                columnWeights = listOf(0.8f, 3.2f, 0.9f),
                rows = perDayStats.sortedBy { it.dayOfMonth }.map { d ->
                    listOf(
                        FormatUtils.formatNumber(d.dayOfMonth, langCode),
                        d.summaryText,
                        FormatUtils.formatNumber(d.pieces, langCode),
                    )
                },
            )
        ),
        srUnit = srUnit,
        pageFooterPattern = pageFooterPattern,
    )

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    PdfReportGenerator.writeReportToUri(context, uri, buildSpec())
                }
                if (res.isSuccess) onTaskSuccess(pdfSavedMsg) else onShowMessage(pdfErrorMsg)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (dlEntriesState is UiState.Loading) {
            item {
                LottieLoadingCard()
            }
        }

        item {
            MonthPickerBar(
                yearMonth = month,
                langCode = langCode,
                onPrevMonth = { viewModel.shiftDeliveryReportMonth(-1) },
                onNextMonth = { viewModel.shiftDeliveryReportMonth(1) },
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryMetricTile(
                    label = monthPiecesLabel,
                    value = FormatUtils.formatNumber(monthPieces, langCode),
                    highlighted = true,
                    modifier = Modifier.weight(1f),
                )
                SummaryMetricTile(
                    label = todayPiecesLabel,
                    value = FormatUtils.formatNumber(todayPieces, langCode),
                    modifier = Modifier.weight(1f),
                )
                SummaryMetricTile(
                    label = deliveryDaysLabel,
                    value = FormatUtils.formatNumber(deliveryDaysCount, langCode),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            PdfExportBar(
                onDownloadClick = {
                    createPdfLauncher.launch(FormatUtils.pdfFilename("notebook-delivery", month))
                },
                onShareClick = {
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            PdfReportGenerator.writeReportToShareUri(
                                context,
                                FormatUtils.pdfFilename("notebook-delivery", month),
                                buildSpec(),
                            )
                        }
                        res.onSuccess { uri ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, pdfTitle))
                        }.onFailure { onShowMessage(pdfErrorMsg) }
                    }
                },
            )
        }

        item {
            DailyBarChartCard(
                title = dailyChartLabel,
                yearMonth = month,
                dailyStats = perDayStats,
                useCurrency = false,
                langCode = langCode,
                srUnit = srUnit,
            )
        }

        if (perDayStats.isNotEmpty()) {
            item {
                Text(perDayLabel, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            items(perDayStats, key = { "day_${it.dayOfMonth}" }) { day ->
                GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TealPrimary,
                        ) {
                            Text(
                                text = FormatUtils.formatNumber(day.dayOfMonth, langCode),
                                style = MonoNumberStyle,
                                color = NavyDeep,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                        Text(
                            text = day.summaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.pieces_count, FormatUtils.formatNumber(day.pieces, langCode)),
                            style = MonoNumberStyle,
                            color = TealBright,
                        )
                    }
                }
            }
        } else {
            item {
                IllustratedEmptyStateCard(
                    imageRes = R.drawable.img_empty_deliveries_1795044790094,
                    message = stringResource(R.string.no_deliveries_yet),
                )
            }
        }

        if (perItemStats.isNotEmpty()) {
            item {
                Text(byItemLabel, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            items(perItemStats, key = { "dl_stat_${it.name}" }) { stat ->
                GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ItemThumbnail(name = stat.name, photoBase64 = stat.photo, size = 44.dp)
                        Text(
                            text = stat.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.pieces_count, FormatUtils.formatNumber(stat.pieces, langCode)),
                            style = MonoNumberStyle,
                            color = TealBright,
                        )
                    }
                }
            }
        }

        // Add delivery-only items section
        item {
            GlassCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = TealBright)
                        Text(
                            text = stringResource(R.string.add_delivery_item),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }
                    OutlinedTextField(
                        value = dlNameInput,
                        onValueChange = { dlNameInput = it; dlFormError = null },
                        singleLine = true,
                        label = { Text(stringResource(R.string.item_name_hint)) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("dl_item_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (dlPhotoBase64 != null) {
                            Box {
                                ItemThumbnail(name = dlNameInput.ifBlank { "D" }, photoBase64 = dlPhotoBase64, size = 46.dp)
                                IconButton(
                                    onClick = { dlPhotoBase64 = null },
                                    modifier = Modifier.align(Alignment.TopEnd).size(22.dp),
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = null, tint = ErrorCoral, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        GlassPillButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = TealBright, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.gallery))
                        }
                        TealGlowButton(
                            onClick = {
                                val clean = dlNameInput.trim()
                                if (clean.isEmpty()) {
                                    dlFormError = errItemName
                                    return@TealGlowButton
                                }
                                isSavingDlItem = true
                                scope.launch {
                                    val res = viewModel.addDeliveryItem(clean, dlPhotoBase64)
                                    isSavingDlItem = false
                                    if (res.isSuccess) {
                                        dlNameInput = ""
                                        dlPhotoBase64 = null
                                        onTaskSuccess(savedMsg)
                                    } else {
                                        dlFormError = res.exceptionOrNull()?.localizedMessage
                                    }
                                }
                            },
                            enabled = !isSavingDlItem,
                            modifier = Modifier.testTag("add_dl_item_button"),
                        ) {
                            if (isSavingDlItem) {
                                LottieButtonSpinner(size = 24.dp)
                            } else {
                                Text(stringResource(R.string.add), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (dlFormError != null) {
                        Text(dlFormError.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (allDlItems.isNotEmpty()) {
            item {
                Text(stringResource(R.string.delivery_only_items), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            items(allDlItems, key = { it.id }) { dlItem ->
                GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ItemThumbnail(name = dlItem.name, photoBase64 = dlItem.photo, size = 44.dp)
                        Text(
                            text = dlItem.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { dlItemToDelete = dlItem },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.delete), tint = ErrorCoral)
                        }
                    }
                }
            }
        }
    }

    dlItemToDelete?.let { item ->
        ConfirmDeleteDialog(
            itemName = item.name,
            onConfirm = {
                val id = item.id
                dlItemToDelete = null
                scope.launch { viewModel.deleteDeliveryItem(id) }
            },
            onDismiss = { dlItemToDelete = null },
        )
    }
}
