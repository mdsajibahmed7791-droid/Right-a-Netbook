package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.Category
import com.example.model.DayStat
import com.example.model.NameValueStat
import com.example.model.StaffStat
import com.example.model.UiState
import com.example.pdf.PdfReportGenerator
import com.example.pdf.PdfReportSpec
import com.example.pdf.PdfSummaryBox
import com.example.pdf.PdfTableData
import com.example.ui.components.DailyBarChartCard
import com.example.ui.components.LottieLoadingCard
import com.example.ui.components.MonthPickerBar
import com.example.ui.components.PdfExportBar
import com.example.ui.components.SummaryMetricTile
import com.example.ui.components.categoryIcon
import com.example.ui.components.categoryLabel
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealTint
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.viewmodel.AdminViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTabScreen(
    viewModel: AdminViewModel,
    currentUid: String,
    langCode: String,
    onShowMessage: (String) -> Unit,
    onTaskSuccess: (String) -> Unit = onShowMessage,
) {
    val allEntriesState by viewModel.allEntriesState.collectAsStateWithLifecycle()
    val allDlEntriesState by viewModel.allDlEntriesState.collectAsStateWithLifecycle()
    val allStaffState by viewModel.allStaffState.collectAsStateWithLifecycle()
    val allItemsState by viewModel.allItemsState.collectAsStateWithLifecycle()
    val month by viewModel.adminMonth.collectAsStateWithLifecycle()
    val selectedStaffUid by viewModel.selectedStaffUid.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var staffMenuExpanded by remember { mutableStateOf(false) }

    val srUnit = stringResource(R.string.currency_sr)
    val allStaffLabel = stringResource(R.string.all_staff)
    val dash = stringResource(R.string.none_dash)
    val pdfSavedMsg = stringResource(R.string.pdf_saved)
    val pdfErrorMsg = stringResource(R.string.pdf_error)
    val uidCopiedMsg = stringResource(R.string.uid_copied)

    val allEntries = (allEntriesState as? UiState.Success)?.data.orEmpty()
    val allDlEntries = (allDlEntriesState as? UiState.Success)?.data.orEmpty()
    val staffProfiles = (allStaffState as? UiState.Success)?.data.orEmpty()
    val allItems = (allItemsState as? UiState.Success)?.data.orEmpty()

    val staffEmailByUid = remember(staffProfiles) {
        staffProfiles.associate { it.uid to it.email }
    }
    val photoByBaseName = remember(allItems) {
        allItems.associate { it.name.lowercase() to it.photo }
    }

    val allStaffUids = remember(staffProfiles, allEntries, allDlEntries) {
        (staffProfiles.map { it.uid } + allEntries.map { it.uid } + allDlEntries.map { it.uid })
            .filter { it.isNotBlank() }
            .distinct()
    }

    val monthEntries = remember(allEntries, month, selectedStaffUid) {
        allEntries.filter { entry ->
            FormatUtils.yearMonthOf(entry.ts) == month &&
                (selectedStaffUid == null || entry.uid == selectedStaffUid)
        }
    }

    val monthDlEntries = remember(allDlEntries, month, selectedStaffUid) {
        allDlEntries.filter { entry ->
            FormatUtils.yearMonthOf(entry.ts) == month &&
                (selectedStaffUid == null || entry.uid == selectedStaffUid)
        }
    }

    val totalLoss = remember(monthEntries) { monthEntries.sumOf { it.total } }
    val totalPieces = remember(monthEntries) { monthEntries.sumOf { it.qty.toInt() } }
    val entriesCount = monthEntries.size

    val byStaff = remember(allEntries, month, staffEmailByUid) {
        allEntries.filter { FormatUtils.yearMonthOf(it.ts) == month }
            .groupBy { it.uid }
            .map { (uid, list) ->
                StaffStat(
                    uid = uid,
                    email = staffEmailByUid[uid] ?: uid.take(10),
                    entriesCount = list.size,
                    pieces = list.sumOf { it.qty.toInt() },
                    amount = list.sumOf { it.total },
                )
            }
            .sortedByDescending { it.amount }
    }

    val byPerson = remember(monthEntries) {
        monthEntries.groupBy { it.person }
            .map { (name, list) ->
                NameValueStat(name = name, pieces = list.sumOf { it.qty.toInt() }, amount = list.sumOf { it.total }, entriesCount = list.size)
            }
            .sortedByDescending { it.amount }
    }

    val byItem = remember(monthEntries, photoByBaseName) {
        monthEntries.groupBy { it.base.ifBlank { it.item } }
            .map { (name, list) ->
                NameValueStat(
                    name = name,
                    pieces = list.sumOf { it.qty.toInt() },
                    amount = list.sumOf { it.total },
                    photo = photoByBaseName[name.lowercase()],
                    entriesCount = list.size,
                )
            }
            .sortedByDescending { it.amount }
    }

    val byCategory = remember(monthEntries) {
        Category.entries.map { cat ->
            val list = monthEntries.filter { it.cat.equals(cat.key, ignoreCase = true) }
            cat to list.sumOf { it.total }
        }
    }
    val maxCategoryLoss = (byCategory.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)

    val dailyStats = remember(monthEntries) {
        monthEntries.groupBy { FormatUtils.dayOfMonthOf(it.ts) }
            .map { (day, list) ->
                DayStat(dayOfMonth = day, pieces = list.sumOf { it.qty.toInt() }, amount = list.sumOf { it.total })
            }
            .sortedBy { it.dayOfMonth }
    }

    val dlByItem = remember(monthDlEntries) {
        monthDlEntries.groupBy { it.base.ifBlank { it.item } }
            .map { (name, list) ->
                NameValueStat(name = name, pieces = list.sumOf { it.qty.toInt() }, amount = 0.0)
            }
            .sortedByDescending { it.pieces }
    }

    val dlByDay = remember(monthDlEntries, langCode) {
        monthDlEntries.groupBy { FormatUtils.dayOfMonthOf(it.ts) }
            .map { (day, list) ->
                val summary = list.groupBy { it.item }
                    .map { (label, g) -> "$label ×${FormatUtils.formatNumber(g.sumOf { it.qty.toInt() }, langCode)}" }
                    .joinToString(", ")
                DayStat(dayOfMonth = day, pieces = list.sumOf { it.qty.toInt() }, amount = 0.0, summaryText = summary)
            }
            .sortedByDescending { it.dayOfMonth }
    }

    val topStaff = byStaff.firstOrNull()?.email ?: dash
    val topPerson = byPerson.firstOrNull()?.name ?: dash
    val topItem = byItem.firstOrNull()?.name ?: dash

    val selectedStaffDisplay = selectedStaffUid?.let { staffEmailByUid[it] ?: it.take(10) } ?: allStaffLabel

    val appName = stringResource(R.string.app_name)
    val pdfTitle = stringResource(R.string.pdf_admin_title)
    val totalLossLabel = stringResource(R.string.total_loss)
    val totalPiecesLabel = stringResource(R.string.total_pieces)
    val entriesCountLabel = stringResource(R.string.entries_count)
    val lossByDayLabel = stringResource(R.string.loss_by_day)
    val byItemLabel = stringResource(R.string.by_item)
    val byStaffLabel = stringResource(R.string.by_staff)
    val byPersonLabel = stringResource(R.string.by_person)
    val colStaff = stringResource(R.string.staff_email_col)
    val colName = stringResource(R.string.pdf_col_name)
    val colEntries = stringResource(R.string.pdf_col_entries)
    val colPieces = stringResource(R.string.pdf_col_pieces)
    val colTotal = stringResource(R.string.pdf_col_total)
    val pageFooterPattern = stringResource(R.string.pdf_page_footer)

    fun buildSpec(): PdfReportSpec = PdfReportSpec(
        title = pdfTitle,
        month = month,
        scopeLabel = selectedStaffDisplay,
        langCode = langCode,
        appName = appName,
        summaryBoxes = listOf(
            PdfSummaryBox(totalLossLabel, FormatUtils.formatCurrency(totalLoss, langCode, srUnit)),
            PdfSummaryBox(totalPiecesLabel, FormatUtils.formatNumber(totalPieces, langCode)),
            PdfSummaryBox(entriesCountLabel, FormatUtils.formatNumber(entriesCount, langCode)),
        ),
        dailyChartTitle = lossByDayLabel,
        dailyStats = dailyStats,
        chartUseCurrency = true,
        byItemTitle = byItemLabel,
        byItemStats = byItem,
        showMoneyInItems = true,
        tables = listOf(
            PdfTableData(
                title = byStaffLabel,
                columns = listOf(colStaff, colEntries, colPieces, colTotal),
                columnWeights = listOf(2.4f, 0.9f, 0.9f, 1.4f),
                rows = byStaff.map { s ->
                    listOf(
                        s.email,
                        FormatUtils.formatNumber(s.entriesCount, langCode),
                        FormatUtils.formatNumber(s.pieces, langCode),
                        FormatUtils.formatCurrency(s.amount, langCode, srUnit),
                    )
                },
            ),
            PdfTableData(
                title = byPersonLabel,
                columns = listOf(colName, colEntries, colPieces, colTotal),
                columnWeights = listOf(2.2f, 0.9f, 0.9f, 1.4f),
                rows = byPerson.map { p ->
                    listOf(
                        p.name,
                        FormatUtils.formatNumber(p.entriesCount, langCode),
                        FormatUtils.formatNumber(p.pieces, langCode),
                        FormatUtils.formatCurrency(p.amount, langCode, srUnit),
                    )
                },
            ),
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
        item {
            MonthPickerBar(
                yearMonth = month,
                langCode = langCode,
                onPrevMonth = { viewModel.shiftAdminMonth(-1) },
                onNextMonth = { viewModel.shiftAdminMonth(1) },
            )
        }

        item {
            ExposedDropdownMenuBox(
                expanded = staffMenuExpanded,
                onExpandedChange = { staffMenuExpanded = !staffMenuExpanded },
            ) {
                OutlinedTextField(
                    value = selectedStaffDisplay,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.filter_staff)) },
                    trailingIcon = {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = TealPrimary)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("admin_staff_filter"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                )
                ExposedDropdownMenu(
                    expanded = staffMenuExpanded,
                    onDismissRequest = { staffMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(allStaffLabel) },
                        onClick = {
                            viewModel.selectStaffUid(null)
                            staffMenuExpanded = false
                        },
                    )
                    allStaffUids.forEach { uid ->
                        val email = staffEmailByUid[uid] ?: uid
                        DropdownMenuItem(
                            text = { Text(email) },
                            onClick = {
                                viewModel.selectStaffUid(uid)
                                staffMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        when (val state = allEntriesState) {
            is UiState.Loading -> {
                item {
                    LottieLoadingCard()
                }
            }
            is UiState.Error -> {
                item {
                    GlassCard(errorBorder = true, modifier = Modifier.fillMaxWidth()) {
                        Text(state.message, color = ErrorCoral, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            is UiState.Success -> {
                item {
                    SummaryMetricTile(
                        label = totalLossLabel,
                        value = FormatUtils.formatCurrency(totalLoss, langCode, srUnit),
                        highlighted = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryMetricTile(
                            label = totalPiecesLabel,
                            value = FormatUtils.formatNumber(totalPieces, langCode),
                            modifier = Modifier.weight(1f),
                        )
                        SummaryMetricTile(
                            label = entriesCountLabel,
                            value = FormatUtils.formatNumber(entriesCount, langCode),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryMetricTile(label = stringResource(R.string.top_staff), value = topStaff, modifier = Modifier.weight(1f))
                        SummaryMetricTile(label = stringResource(R.string.top_person), value = topPerson, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    SummaryMetricTile(label = stringResource(R.string.top_item), value = topItem, modifier = Modifier.fillMaxWidth())
                }

                item {
                    PdfExportBar(
                        onDownloadClick = {
                            createPdfLauncher.launch(FormatUtils.pdfFilename("notebook-admin", month))
                        },
                        onShareClick = {
                            scope.launch {
                                val res = withContext(Dispatchers.IO) {
                                    PdfReportGenerator.writeReportToShareUri(
                                        context,
                                        FormatUtils.pdfFilename("notebook-admin", month),
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

                // Per-staff table
                if (byStaff.isNotEmpty()) {
                    item {
                        Text(byStaffLabel, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(colStaff, style = MaterialTheme.typography.labelSmall, color = TealBright, modifier = Modifier.weight(1.8f))
                                    Text(colEntries, style = MaterialTheme.typography.labelSmall, color = TealBright, modifier = Modifier.weight(0.7f))
                                    Text(colPieces, style = MaterialTheme.typography.labelSmall, color = TealBright, modifier = Modifier.weight(0.7f))
                                    Text(colTotal, style = MaterialTheme.typography.labelSmall, color = TealBright, modifier = Modifier.weight(1.1f))
                                }
                                byStaff.forEach { stat ->
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            stat.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1.8f),
                                        )
                                        Text(FormatUtils.formatNumber(stat.entriesCount, langCode), style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(0.7f))
                                        Text(FormatUtils.formatNumber(stat.pieces, langCode), style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(0.7f))
                                        Text(FormatUtils.formatCurrency(stat.amount, langCode, srUnit), style = MaterialTheme.typography.bodySmall, color = TealBright, modifier = Modifier.weight(1.1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Loss per day chart
                item {
                    DailyBarChartCard(
                        title = lossByDayLabel,
                        yearMonth = month,
                        dailyStats = dailyStats,
                        useCurrency = true,
                        langCode = langCode,
                        srUnit = srUnit,
                    )
                }

                // Loss per category chart
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(R.string.loss_by_category), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            byCategory.forEach { (cat, loss) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(categoryIcon(cat), contentDescription = null, tint = TealBright, modifier = Modifier.size(16.dp))
                                            Text(categoryLabel(cat), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        }
                                        Text(FormatUtils.formatCurrency(loss, langCode, srUnit), style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                    }
                                    LinearProgressIndicator(
                                        progress = { (loss / maxCategoryLoss).toFloat().coerceIn(0f, 1f) },
                                        color = TealPrimary,
                                        trackColor = TealTint,
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Delivery summary across all staff
                item {
                    Text(stringResource(R.string.delivery_summary_admin), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                if (dlByItem.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.no_deliveries_yet), color = TextMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
                        }
                    }
                } else {
                    items(dlByItem, key = { "adm_dl_item_${it.name}" }) { stat ->
                        GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stat.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(stringResource(R.string.pieces_count, FormatUtils.formatNumber(stat.pieces, langCode)), style = MonoNumberStyle, color = TealBright)
                            }
                        }
                    }
                    items(dlByDay, key = { "adm_dl_day_${it.dayOfMonth}" }) { day ->
                        GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${FormatUtils.formatNumber(day.dayOfMonth, langCode)}: ${day.summaryText}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
                                Text(stringResource(R.string.pieces_count, FormatUtils.formatNumber(day.pieces, langCode)), style = MaterialTheme.typography.labelSmall, color = TealBright)
                            }
                        }
                    }
                }

                // Read-only entries list
                item {
                    Text(stringResource(R.string.all_entries_readonly), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                items(monthEntries.take(40), key = { "adm_e_${it.id}" }) { entry ->
                    val staffMail = staffEmailByUid[entry.uid] ?: entry.uid.take(8)
                    GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${entry.item} × ${FormatUtils.formatNumber(entry.qty, langCode)}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("${entry.person} • $staffMail • ${FormatUtils.formatDateTime(entry.ts, langCode)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Text(FormatUtils.formatCurrency(entry.total, langCode, srUnit), style = MonoNumberStyle, color = TealBright)
                        }
                    }
                }
            }
        }

        // Staff Directory & Role Setup Card
        item {
            GlassCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, tint = TealBright)
                        Text(stringResource(R.string.staff_directory_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Text(stringResource(R.string.staff_directory_desc), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(stringResource(R.string.your_uid, currentUid), style = MaterialTheme.typography.labelSmall, color = TealBright)
                    GlassPillButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("UID", currentUid))
                            onTaskSuccess(uidCopiedMsg)
                        },
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = TealBright, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.copy_uid))
                    }
                }
            }
        }
    }
}
