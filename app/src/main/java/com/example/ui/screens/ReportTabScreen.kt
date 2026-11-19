package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.Category
import com.example.model.DayStat
import com.example.model.NameValueStat
import com.example.model.UiState
import com.example.pdf.PdfReportGenerator
import com.example.pdf.PdfReportSpec
import com.example.pdf.PdfSummaryBox
import com.example.pdf.PdfTableData
import com.example.ui.components.DailyBarChartCard
import com.example.ui.components.ItemThumbnail
import com.example.ui.components.LottieLoadingCard
import com.example.ui.components.MonthPickerBar
import com.example.ui.components.PdfExportBar
import com.example.ui.components.SummaryMetricTile
import com.example.ui.components.categoryIcon
import com.example.ui.components.categoryLabel
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCard
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportTabScreen(
    viewModel: MainViewModel,
    langCode: String,
    onShowMessage: (String) -> Unit,
    onTaskSuccess: (String) -> Unit = onShowMessage,
) {
    val entriesState by viewModel.entriesState.collectAsStateWithLifecycle()
    val peopleState by viewModel.peopleState.collectAsStateWithLifecycle()
    val itemsState by viewModel.itemsState.collectAsStateWithLifecycle()
    val month by viewModel.reportMonth.collectAsStateWithLifecycle()
    val personFilter by viewModel.reportPersonFilter.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var personMenuExpanded by remember { mutableStateOf(false) }

    val srUnit = stringResource(R.string.currency_sr)
    val everyoneLabel = stringResource(R.string.everyone)
    val dash = stringResource(R.string.none_dash)
    val pdfSavedMsg = stringResource(R.string.pdf_saved)
    val pdfErrorMsg = stringResource(R.string.pdf_error)

    val allEntries = (entriesState as? UiState.Success)?.data.orEmpty()
    val allPeople = (peopleState as? UiState.Success)?.data.orEmpty()
    val allItems = (itemsState as? UiState.Success)?.data.orEmpty()
    val photoByBaseName = remember(allItems) {
        allItems.associate { it.name.lowercase() to it.photo }
    }

    val personOptions = remember(allPeople, allEntries) {
        (allPeople.map { it.name } + allEntries.map { it.person })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    val filteredEntries = remember(allEntries, month, personFilter) {
        allEntries.filter { entry ->
            FormatUtils.yearMonthOf(entry.ts) == month &&
                (personFilter == null || entry.person.equals(personFilter, ignoreCase = true))
        }
    }

    val totalLoss = remember(filteredEntries) { filteredEntries.sumOf { it.total } }
    val totalPieces = remember(filteredEntries) { filteredEntries.sumOf { it.qty.toInt() } }
    val entriesCount = filteredEntries.size

    val byPerson = remember(filteredEntries) {
        filteredEntries.groupBy { it.person }
            .map { (name, list) ->
                NameValueStat(
                    name = name,
                    pieces = list.sumOf { it.qty.toInt() },
                    amount = list.sumOf { it.total },
                    entriesCount = list.size,
                )
            }
            .sortedByDescending { it.amount }
    }

    val byItem = remember(filteredEntries, photoByBaseName) {
        filteredEntries.groupBy { it.base.ifBlank { it.item } }
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

    val byCategory = remember(filteredEntries) {
        Category.entries.map { cat ->
            val list = filteredEntries.filter { it.cat.equals(cat.key, ignoreCase = true) }
            cat to NameValueStat(
                name = cat.key,
                pieces = list.sumOf { it.qty.toInt() },
                amount = list.sumOf { it.total },
            )
        }
    }

    val dailyStats = remember(filteredEntries) {
        filteredEntries.groupBy { FormatUtils.dayOfMonthOf(it.ts) }
            .map { (day, list) ->
                DayStat(
                    dayOfMonth = day,
                    pieces = list.sumOf { it.qty.toInt() },
                    amount = list.sumOf { it.total },
                )
            }
            .sortedBy { it.dayOfMonth }
    }

    val topPerson = byPerson.firstOrNull()?.name ?: dash
    val topItem = byItem.firstOrNull()?.name ?: dash

    val appName = stringResource(R.string.app_name)
    val pdfTitle = stringResource(R.string.pdf_free_title)
    val totalLossLabel = stringResource(R.string.total_loss)
    val totalPiecesLabel = stringResource(R.string.total_pieces)
    val entriesCountLabel = stringResource(R.string.entries_count)
    val dailyChartLabel = stringResource(R.string.daily_chart)
    val byItemLabel = stringResource(R.string.by_item)
    val byPersonLabel = stringResource(R.string.by_person)
    val colName = stringResource(R.string.pdf_col_name)
    val colPieces = stringResource(R.string.pdf_col_pieces)
    val colTotal = stringResource(R.string.pdf_col_total)
    val colEntries = stringResource(R.string.pdf_col_entries)
    val pageFooterPattern = stringResource(R.string.pdf_page_footer)

    fun buildSpec(): PdfReportSpec = PdfReportSpec(
        title = pdfTitle,
        month = month,
        scopeLabel = personFilter ?: everyoneLabel,
        langCode = langCode,
        appName = appName,
        summaryBoxes = listOf(
            PdfSummaryBox(totalLossLabel, FormatUtils.formatCurrency(totalLoss, langCode, srUnit)),
            PdfSummaryBox(totalPiecesLabel, FormatUtils.formatNumber(totalPieces, langCode)),
            PdfSummaryBox(entriesCountLabel, FormatUtils.formatNumber(entriesCount, langCode)),
        ),
        dailyChartTitle = dailyChartLabel,
        dailyStats = dailyStats,
        chartUseCurrency = true,
        byItemTitle = byItemLabel,
        byItemStats = byItem,
        showMoneyInItems = true,
        tables = listOf(
            PdfTableData(
                title = byPersonLabel,
                columns = listOf(colName, colEntries, colPieces, colTotal),
                columnWeights = listOf(2.2f, 1f, 1f, 1.4f),
                rows = byPerson.map { stat ->
                    listOf(
                        stat.name,
                        FormatUtils.formatNumber(stat.entriesCount, langCode),
                        FormatUtils.formatNumber(stat.pieces, langCode),
                        FormatUtils.formatCurrency(stat.amount, langCode, srUnit),
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
        item {
            MonthPickerBar(
                yearMonth = month,
                langCode = langCode,
                onPrevMonth = { viewModel.shiftReportMonth(-1) },
                onNextMonth = { viewModel.shiftReportMonth(1) },
            )
        }

        item {
            ExposedDropdownMenuBox(
                expanded = personMenuExpanded,
                onExpandedChange = { personMenuExpanded = !personMenuExpanded },
            ) {
                OutlinedTextField(
                    value = personFilter ?: everyoneLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.filter_person)) },
                    trailingIcon = {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = TealPrimary)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("report_person_filter"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                )
                ExposedDropdownMenu(
                    expanded = personMenuExpanded,
                    onDismissRequest = { personMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(everyoneLabel) },
                        onClick = {
                            viewModel.setReportPersonFilter(null)
                            personMenuExpanded = false
                        },
                    )
                    personOptions.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                viewModel.setReportPersonFilter(name)
                                personMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        when (val state = entriesState) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SummaryMetricTile(
                            label = stringResource(R.string.top_person),
                            value = topPerson,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryMetricTile(
                            label = stringResource(R.string.top_item),
                            value = topItem,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    PdfExportBar(
                        onDownloadClick = {
                            createPdfLauncher.launch(FormatUtils.pdfFilename("notebook", month))
                        },
                        onShareClick = {
                            scope.launch {
                                val res = withContext(Dispatchers.IO) {
                                    PdfReportGenerator.writeReportToShareUri(
                                        context,
                                        FormatUtils.pdfFilename("notebook", month),
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
                                }.onFailure {
                                    onShowMessage(pdfErrorMsg)
                                }
                            }
                        },
                    )
                }

                item {
                    DailyBarChartCard(
                        title = dailyChartLabel,
                        yearMonth = month,
                        dailyStats = dailyStats,
                        useCurrency = true,
                        langCode = langCode,
                        srUnit = srUnit,
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.by_category),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        byCategory.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { (cat, stat) ->
                                    GlassCard(cornerRadius = 14.dp, modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(categoryIcon(cat), contentDescription = null, tint = TealBright, modifier = Modifier.size(20.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(categoryLabel(cat), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                                Text(
                                                    text = FormatUtils.formatCurrency(stat.amount, langCode, srUnit),
                                                    style = MonoNumberStyle,
                                                    color = TextPrimary,
                                                )
                                                Text(
                                                    text = stringResource(R.string.pieces_count, FormatUtils.formatNumber(stat.pieces, langCode)),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMuted,
                                                )
                                            }
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (byPerson.isNotEmpty()) {
                    item {
                        Text(
                            text = byPersonLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }
                    items(byPerson, key = { "p_${it.name}" }) { stat ->
                        GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stat.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(
                                        text = stringResource(R.string.pieces_count, FormatUtils.formatNumber(stat.pieces, langCode)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                                Text(
                                    text = FormatUtils.formatCurrency(stat.amount, langCode, srUnit),
                                    style = MonoNumberStyle,
                                    color = TealBright,
                                )
                            }
                        }
                    }
                }

                if (byItem.isNotEmpty()) {
                    item {
                        Text(
                            text = byItemLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }
                    items(byItem, key = { "i_${it.name}" }) { stat ->
                        GlassCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                ItemThumbnail(name = stat.name, photoBase64 = stat.photo, size = 44.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stat.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(
                                        text = stringResource(R.string.pieces_count, FormatUtils.formatNumber(stat.pieces, langCode)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                                Text(
                                    text = FormatUtils.formatCurrency(stat.amount, langCode, srUnit),
                                    style = MonoNumberStyle,
                                    color = TealBright,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
