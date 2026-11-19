package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DayStat
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealGlowButton
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealTint
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun MonthPickerBar(
    yearMonth: YearMonth,
    langCode: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        cornerRadius = 16.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onPrevMonth,
                modifier = Modifier.size(48.dp).testTag("month_prev_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = TealBright,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = FormatUtils.formatMonthYear(yearMonth, langCode),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
            }
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(48.dp).testTag("month_next_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = TealBright,
                )
            }
        }
    }
}

@Composable
fun SummaryMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    GlassCard(
        highlighted = highlighted,
        cornerRadius = 16.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TealPrimary),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = if (highlighted) MaterialTheme.typography.headlineMedium else MonoNumberStyle,
                    color = if (highlighted) TealBright else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun DailyBarChartCard(
    title: String,
    yearMonth: YearMonth,
    dailyStats: List<DayStat>,
    useCurrency: Boolean,
    langCode: String,
    srUnit: String,
    modifier: Modifier = Modifier,
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val byDay = remember(dailyStats) { dailyStats.associateBy { it.dayOfMonth } }
    val maxValue = remember(dailyStats, useCurrency) {
        (1..daysInMonth).maxOfOrNull { day ->
            val stat = byDay[day]
            if (useCurrency) stat?.amount ?: 0.0 else (stat?.pieces ?: 0).toDouble()
        }?.coerceAtLeast(1.0) ?: 1.0
    }
    val peakDay = remember(dailyStats, useCurrency) {
        dailyStats.maxByOrNull { if (useCurrency) it.amount else it.pieces.toDouble() }
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                if (peakDay != null) {
                    val peakText = if (useCurrency) {
                        FormatUtils.formatCurrency(peakDay.amount, langCode, srUnit)
                    } else {
                        stringResource(R.string.pieces_count, FormatUtils.formatNumber(peakDay.pieces, langCode))
                    }
                    Text(
                        text = "${FormatUtils.formatNumber(peakDay.dayOfMonth, langCode)}: $peakText",
                        style = MaterialTheme.typography.labelSmall,
                        color = TealBright,
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
            ) {
                val step = size.width / daysInMonth.toFloat()
                val barW = (step * 0.6f).coerceIn(4f, 18f)
                for (day in 1..daysInMonth) {
                    val x = (day - 1) * step + (step - barW) / 2f
                    drawRoundRect(
                        color = TealTint,
                        topLeft = Offset(x, 0f),
                        size = Size(barW, size.height),
                        cornerRadius = CornerRadius(4f, 4f),
                    )
                    val stat = byDay[day]
                    val value = if (useCurrency) stat?.amount ?: 0.0 else (stat?.pieces ?: 0).toDouble()
                    if (value > 0.0) {
                        val ratio = (value / maxValue).toFloat().coerceIn(0.04f, 1f)
                        val barH = size.height * ratio
                        drawRoundRect(
                            color = TealPrimary,
                            topLeft = Offset(x, size.height - barH),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(4f, 4f),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(1, 8, 15, 22, daysInMonth).distinct().forEach { d ->
                    Text(
                        text = FormatUtils.formatNumber(d, langCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerRow(
    tsMillis: Long,
    langCode: String,
    onTsChanged: (Long) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val zone = ZoneId.systemDefault()
    val currentDateTime = remember(tsMillis) {
        Instant.ofEpochMilli(tsMillis).atZone(zone).toLocalDateTime()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlassCard(
            cornerRadius = 14.dp,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable { showDatePicker = true }
                .testTag("pick_date_button"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                Column {
                    Text(stringResource(R.string.date_label), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(FormatUtils.formatDateOnly(tsMillis, langCode), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }
        }

        GlassCard(
            cornerRadius = 14.dp,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable { showTimePicker = true }
                .testTag("pick_time_button"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                Column {
                    Text(stringResource(R.string.time_label), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(FormatUtils.formatTimeOnly(tsMillis, langCode), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }
        }
    }

    if (showDatePicker) {
        val initialUtcMillis = currentDateTime.toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = dateState.selectedDateMillis
                        if (selectedMillis != null) {
                            val pickedDate = Instant.ofEpochMilli(selectedMillis).atZone(ZoneOffset.UTC).toLocalDate()
                            val updated = LocalDateTime.of(pickedDate, currentDateTime.toLocalTime())
                                .atZone(zone)
                                .toInstant()
                                .toEpochMilli()
                            onTsChanged(updated)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = currentDateTime.hour,
            initialMinute = currentDateTime.minute,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.time_label)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedTime = LocalTime.of(timeState.hour, timeState.minute)
                        val updated = LocalDateTime.of(currentDateTime.toLocalDate(), pickedTime)
                            .atZone(zone)
                            .toInstant()
                            .toEpochMilli()
                        onTsChanged(updated)
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun PdfExportBar(
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TealGlowButton(
            onClick = onDownloadClick,
            modifier = Modifier.weight(1f).testTag("download_pdf_button"),
        ) {
            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.download_pdf), fontWeight = FontWeight.Bold)
        }
        GlassPillButton(
            onClick = onShareClick,
            modifier = Modifier.testTag("share_pdf_button"),
        ) {
            Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share_pdf), tint = TealBright, modifier = Modifier.size(18.dp))
        }
    }
}
