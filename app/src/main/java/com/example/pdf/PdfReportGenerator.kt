package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.DayStat
import com.example.model.NameValueStat
import com.example.model.StaffStat
import com.example.util.FormatUtils
import com.example.util.ImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.YearMonth

data class PdfSummaryBox(
    val label: String,
    val value: String,
)

data class PdfTableData(
    val title: String,
    val columns: List<String>,
    val columnWeights: List<Float>,
    val rows: List<List<String>>,
)

data class PdfReportSpec(
    val title: String,
    val month: YearMonth,
    val scopeLabel: String,
    val langCode: String,
    val appName: String,
    val summaryBoxes: List<PdfSummaryBox>,
    val dailyChartTitle: String,
    val dailyStats: List<DayStat>,
    val chartUseCurrency: Boolean,
    val byItemTitle: String,
    val byItemStats: List<NameValueStat>,
    val showMoneyInItems: Boolean,
    val tables: List<PdfTableData>,
    val srUnit: String,
    val pageFooterPattern: String,
)

object PdfReportGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 32f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_X * 2
    private const val TOP_MARGIN = 30f
    private const val FOOTER_Y = PAGE_HEIGHT - 30f
    private const val MAX_Y = PAGE_HEIGHT - 54f

    private val COLOR_NAVY = Color.parseColor("#0A1220")
    private val COLOR_TEAL_DEEP = Color.parseColor("#1A5A5E")
    private val COLOR_TEAL = Color.parseColor("#34B5A5")
    private val COLOR_TEAL_LIGHT = Color.parseColor("#E6F7F5")
    private val COLOR_BG_CARD = Color.parseColor("#F4F8F9")
    private val COLOR_ZEBRA = Color.parseColor("#EAF3F4")
    private val COLOR_BORDER = Color.parseColor("#CBE0E2")
    private val COLOR_TEXT_DARK = Color.parseColor("#0D1B2A")
    private val COLOR_TEXT_MUTED = Color.parseColor("#4B646B")

    fun writeReportToUri(context: Context, uri: Uri, spec: PdfReportSpec): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            generatePdf(context, spec, out)
        } ?: error("Unable to open output stream")
    }

    fun writeReportToShareUri(context: Context, filename: String, spec: PdfReportSpec): Result<Uri> = runCatching {
        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, filename)
        FileOutputStream(file).use { out ->
            generatePdf(context, spec, out)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun generatePdf(context: Context, spec: PdfReportSpec, outputStream: OutputStream) {
        val isRtl = spec.langCode == "ar"
        val pages = mutableListOf<MutableList<(Canvas) -> Unit>>()
        var currentOps = mutableListOf<(Canvas) -> Unit>()
        pages.add(currentOps)
        var y = 0f

        fun newPage() {
            currentOps = mutableListOf()
            pages.add(currentOps)
            y = TOP_MARGIN
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > MAX_Y) {
                newPage()
            }
        }

        // 1. Top Banner (Page 1)
        val bannerHeight = 112f
        val monthText = FormatUtils.formatMonthYear(spec.month, spec.langCode)
        val scopeText = context.getString(R.string.pdf_for_label, spec.scopeLabel)
        currentOps.add { canvas ->
            drawBanner(canvas, spec.title, monthText, scopeText, bannerHeight, isRtl)
        }
        y = bannerHeight + 18f

        // 2. Three Summary Boxes
        val boxHeight = 66f
        ensureSpace(boxHeight + 16f)
        val boxes = spec.summaryBoxes.take(3)
        currentOps.add { canvas ->
            drawSummaryBoxes(canvas, y, boxHeight, boxes, isRtl)
        }
        y += boxHeight + 18f

        // 3. Daily Bar Chart Card
        val chartHeight = 176f
        ensureSpace(chartHeight + 18f)
        val daysInMonth = spec.month.lengthOfMonth()
        val chartTop = y
        currentOps.add { canvas ->
            drawDailyChartCard(
                canvas = canvas,
                top = chartTop,
                height = chartHeight,
                title = spec.dailyChartTitle,
                daysInMonth = daysInMonth,
                dailyStats = spec.dailyStats,
                useCurrency = spec.chartUseCurrency,
                langCode = spec.langCode,
                isRtl = isRtl,
            )
        }
        y += chartHeight + 18f

        // 4. "By item" rows with photo, name, qty, and proportional teal bar
        if (spec.byItemStats.isNotEmpty()) {
            ensureSpace(56f)
            val sectionHeaderY = y
            currentOps.add { canvas ->
                drawSectionHeading(canvas, spec.byItemTitle, sectionHeaderY, isRtl)
            }
            y += 26f

            val maxItemMetric = spec.byItemStats.maxOfOrNull {
                if (spec.showMoneyInItems) it.amount else it.pieces.toDouble()
            }?.coerceAtLeast(1.0) ?: 1.0

            val rowHeight = 42f
            for (stat in spec.byItemStats) {
                ensureSpace(rowHeight + 6f)
                val rowY = y
                val bitmap = ImageUtils.decodeBase64ToBitmap(stat.photo)
                val metric = if (spec.showMoneyInItems) stat.amount else stat.pieces.toDouble()
                val ratio = (metric / maxItemMetric).toFloat().coerceIn(0.04f, 1f)
                val qtyText = FormatUtils.formatNumber(stat.pieces, spec.langCode)
                val valueText = if (spec.showMoneyInItems) {
                    "$qtyText • ${FormatUtils.formatCurrency(stat.amount, spec.langCode, spec.srUnit)}"
                } else {
                    context.getString(R.string.pieces_count, qtyText)
                }
                currentOps.add { canvas ->
                    drawByItemRow(
                        canvas = canvas,
                        top = rowY,
                        height = rowHeight,
                        name = stat.name,
                        valueText = valueText,
                        ratio = ratio,
                        photo = bitmap,
                        isRtl = isRtl,
                    )
                }
                y += rowHeight + 6f
            }
            y += 10f
        }

        // 5. Zebra Tables with repeated headers across pages
        for (table in spec.tables) {
            if (table.rows.isEmpty()) continue
            val headerHeight = 28f
            val minSection = 26f + headerHeight + 28f
            ensureSpace(minSection)

            val titleY = y
            currentOps.add { canvas ->
                drawSectionHeading(canvas, table.title, titleY, isRtl)
            }
            y += 26f

            fun emitTableHeader() {
                val hy = y
                currentOps.add { canvas ->
                    drawTableHeader(canvas, hy, headerHeight, table.columns, table.columnWeights, isRtl)
                }
                y += headerHeight
            }

            emitTableHeader()

            for ((rowIndex, row) in table.rows.withIndex()) {
                val rowHeight = measureTableRowHeight(row, table.columnWeights)
                if (y + rowHeight > MAX_Y) {
                    newPage()
                    emitTableHeader()
                }
                val ry = y
                val shaded = rowIndex % 2 == 1
                currentOps.add { canvas ->
                    drawTableRow(canvas, ry, rowHeight, row, table.columnWeights, shaded, isRtl)
                }
                y += rowHeight
            }
            y += 16f
        }

        val totalPages = pages.size
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, ops ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                ops.forEach { op -> op(canvas) }
                val footerRight = String.format(
                    spec.pageFooterPattern,
                    FormatUtils.formatNumber(index + 1, spec.langCode),
                    FormatUtils.formatNumber(totalPages, spec.langCode),
                )
                drawFooter(canvas, spec.appName, footerRight, isRtl)
                document.finishPage(page)
            }
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private fun drawBanner(
        canvas: Canvas,
        title: String,
        month: String,
        scope: String,
        height: Float,
        isRtl: Boolean,
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, PAGE_WIDTH.toFloat(), height,
                COLOR_NAVY, COLOR_TEAL_DEEP, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), height, bgPaint)

        // Glowing wave lines inside banner
        val wavePaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(95, 52, 181, 165)
        }
        val wavePaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.argb(140, 78, 212, 196)
        }
        val path1 = Path().apply {
            moveTo(0f, height * 0.78f)
            cubicTo(PAGE_WIDTH * 0.30f, height * 0.48f, PAGE_WIDTH * 0.68f, height * 1.02f, PAGE_WIDTH.toFloat(), height * 0.64f)
        }
        val path2 = Path().apply {
            moveTo(0f, height * 0.90f)
            cubicTo(PAGE_WIDTH * 0.36f, height * 0.66f, PAGE_WIDTH * 0.74f, height * 0.96f, PAGE_WIDTH.toFloat(), height * 0.80f)
        }
        canvas.drawPath(path1, wavePaint1)
        canvas.drawPath(path2, wavePaint2)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBEAE7")
            textSize = 12f
        }

        drawStaticText(canvas, title, MARGIN_X, 20f, CONTENT_WIDTH, titlePaint, isRtl)
        drawStaticText(canvas, month, MARGIN_X, 52f, CONTENT_WIDTH, subPaint, isRtl)
        drawStaticText(canvas, scope, MARGIN_X, 72f, CONTENT_WIDTH, subPaint, isRtl)
    }

    private fun drawSummaryBoxes(
        canvas: Canvas,
        top: Float,
        height: Float,
        boxes: List<PdfSummaryBox>,
        isRtl: Boolean,
    ) {
        if (boxes.isEmpty()) return
        val gap = 10f
        val boxWidth = (CONTENT_WIDTH - gap * (boxes.size - 1)) / boxes.size
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_BG_CARD }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = COLOR_BORDER
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 10f
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val orderedBoxes = if (isRtl) boxes.reversed() else boxes
        orderedBoxes.forEachIndexed { i, box ->
            val left = MARGIN_X + i * (boxWidth + gap)
            val rect = RectF(left, top, left + boxWidth, top + height)
            canvas.drawRoundRect(rect, 10f, 10f, cardPaint)
            canvas.drawRoundRect(rect, 10f, 10f, borderPaint)

            // Teal accent bar on start edge
            val barRect = if (isRtl) {
                RectF(rect.right - 5f, rect.top + 8f, rect.right, rect.bottom - 8f)
            } else {
                RectF(rect.left, rect.top + 8f, rect.left + 5f, rect.bottom - 8f)
            }
            canvas.drawRoundRect(barRect, 3f, 3f, accentPaint)

            val textWidth = boxWidth - 22f
            val textX = if (isRtl) left + 10f else left + 12f
            drawStaticText(canvas, box.label, textX, top + 10f, textWidth, labelPaint, isRtl)
            drawStaticText(canvas, box.value, textX, top + 30f, textWidth, valuePaint, isRtl)
        }
    }

    private fun drawDailyChartCard(
        canvas: Canvas,
        top: Float,
        height: Float,
        title: String,
        daysInMonth: Int,
        dailyStats: List<DayStat>,
        useCurrency: Boolean,
        langCode: String,
        isRtl: Boolean,
    ) {
        val rect = RectF(MARGIN_X, top, MARGIN_X + CONTENT_WIDTH, top + height)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_BG_CARD }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = COLOR_BORDER
        }
        canvas.drawRoundRect(rect, 12f, 12f, cardPaint)
        canvas.drawRoundRect(rect, 12f, 12f, borderPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        drawStaticText(canvas, title, MARGIN_X + 14f, top + 10f, CONTENT_WIDTH - 28f, titlePaint, isRtl)

        val byDay = dailyStats.associateBy { it.dayOfMonth }
        val maxVal = (1..daysInMonth).maxOfOrNull { day ->
            val stat = byDay[day]
            if (useCurrency) stat?.amount ?: 0.0 else (stat?.pieces ?: 0).toDouble()
        }?.coerceAtLeast(1.0) ?: 1.0

        val chartLeft = MARGIN_X + 14f
        val chartRight = MARGIN_X + CONTENT_WIDTH - 14f
        val chartWidth = chartRight - chartLeft
        val barAreaBottom = top + height - 24f
        val barAreaTop = top + 46f
        val barAreaHeight = (barAreaBottom - barAreaTop).coerceAtLeast(30f)

        val step = chartWidth / daysInMonth.toFloat()
        val barWidth = (step * 0.62f).coerceIn(4f, 12f)
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL }
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL_LIGHT }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 7.2f
            textAlign = Paint.Align.CENTER
        }
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 7.5f
            textAlign = Paint.Align.CENTER
        }

        for (day in 1..daysInMonth) {
            val visualIdx = if (isRtl) (daysInMonth - day) else (day - 1)
            val cx = chartLeft + visualIdx * step + step / 2f
            val stat = byDay[day]
            val value = if (useCurrency) stat?.amount ?: 0.0 else (stat?.pieces ?: 0).toDouble()
            val ratio = (value / maxVal).toFloat().coerceIn(0f, 1f)
            val barLeft = cx - barWidth / 2f
            val barRight = cx + barWidth / 2f

            canvas.drawRoundRect(
                RectF(barLeft, barAreaTop, barRight, barAreaBottom),
                3f, 3f, trackPaint,
            )
            if (value > 0.0) {
                val barTop = (barAreaBottom - barAreaHeight * ratio).coerceAtMost(barAreaBottom - 3f)
                canvas.drawRoundRect(
                    RectF(barLeft, barTop, barRight, barAreaBottom),
                    3f, 3f, barPaint,
                )
                val label = if (useCurrency) FormatUtils.formatDecimal(value, langCode)
                else FormatUtils.formatNumber(value.toInt(), langCode)
                canvas.drawText(label, cx, barTop - 3f, valPaint)
            }
            if (day == 1 || day % 5 == 0 || day == daysInMonth) {
                canvas.drawText(
                    FormatUtils.formatNumber(day, langCode),
                    cx,
                    barAreaBottom + 12f,
                    dayPaint,
                )
            }
        }
    }

    private fun drawSectionHeading(canvas: Canvas, title: String, top: Float, isRtl: Boolean) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        drawStaticText(canvas, title, MARGIN_X, top + 2f, CONTENT_WIDTH, paint, isRtl)
    }

    private fun drawByItemRow(
        canvas: Canvas,
        top: Float,
        height: Float,
        name: String,
        valueText: String,
        ratio: Float,
        photo: Bitmap?,
        isRtl: Boolean,
    ) {
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_BG_CARD }
        val rect = RectF(MARGIN_X, top, MARGIN_X + CONTENT_WIDTH, top + height)
        canvas.drawRoundRect(rect, 8f, 8f, cardPaint)

        val thumbSize = 28f
        val thumbY = top + (height - thumbSize) / 2f
        val thumbLeft = if (isRtl) rect.right - 10f - thumbSize else rect.left + 10f
        val thumbRect = RectF(thumbLeft, thumbY, thumbLeft + thumbSize, thumbY + thumbSize)

        if (photo != null) {
            canvas.save()
            val clip = Path().apply { addRoundRect(thumbRect, 6f, 6f, Path.Direction.CW) }
            canvas.clipPath(clip)
            canvas.drawBitmap(photo, null, thumbRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL_DEEP }
            canvas.drawRoundRect(thumbRect, 6f, 6f, badgePaint)
            val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 12f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            canvas.drawText(letter, thumbRect.centerX(), thumbRect.centerY() + 4f, letterPaint)
        }

        val textStart = if (isRtl) rect.left + 12f else thumbRect.right + 10f
        val textEnd = if (isRtl) thumbRect.left - 10f else rect.right - 12f
        val innerWidth = (textEnd - textStart).coerceAtLeast(80f)

        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 9.5f
        }

        drawStaticText(canvas, name, textStart, top + 6f, innerWidth * 0.65f, namePaint, isRtl, alignStart = true)
        drawStaticText(canvas, valueText, textStart, top + 6f, innerWidth, valuePaint, !isRtl, alignStart = true)

        // Proportional teal bar
        val barTop = top + 27f
        val barBottom = barTop + 6f
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL_LIGHT }
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL }
        canvas.drawRoundRect(RectF(textStart, barTop, textEnd, barBottom), 3f, 3f, trackPaint)
        val filledW = innerWidth * ratio
        val barRect = if (isRtl) {
            RectF(textEnd - filledW, barTop, textEnd, barBottom)
        } else {
            RectF(textStart, barTop, textStart + filledW, barBottom)
        }
        canvas.drawRoundRect(barRect, 3f, 3f, barPaint)
    }

    private fun drawTableHeader(
        canvas: Canvas,
        top: Float,
        height: Float,
        columns: List<String>,
        weights: List<Float>,
        isRtl: Boolean,
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL_DEEP }
        val rect = RectF(MARGIN_X, top, MARGIN_X + CONTENT_WIDTH, top + height)
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        drawTableCells(canvas, top + 7f, columns, weights, textPaint, isRtl)
    }

    private fun measureTableRowHeight(row: List<String>, weights: List<Float>): Float {
        val totalWeight = weights.sum().coerceAtLeast(1f)
        val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f }
        var maxH = 22f
        row.forEachIndexed { idx, text ->
            val w = (CONTENT_WIDTH * (weights.getOrElse(idx) { 1f } / totalWeight) - 12f).coerceAtLeast(30f).toInt()
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, cellPaint, w).build()
            maxH = maxOf(maxH, layout.height + 12f)
        }
        return maxH
    }

    private fun drawTableRow(
        canvas: Canvas,
        top: Float,
        height: Float,
        row: List<String>,
        weights: List<Float>,
        shaded: Boolean,
        isRtl: Boolean,
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (shaded) COLOR_ZEBRA else Color.WHITE
        }
        canvas.drawRect(MARGIN_X, top, MARGIN_X + CONTENT_WIDTH, top + height, bgPaint)
        val linePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.7f
        }
        canvas.drawLine(MARGIN_X, top + height, MARGIN_X + CONTENT_WIDTH, top + height, linePaint)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 9.5f
        }
        drawTableCells(canvas, top + 6f, row, weights, textPaint, isRtl)
    }

    private fun drawTableCells(
        canvas: Canvas,
        textTop: Float,
        cells: List<String>,
        weights: List<Float>,
        paint: TextPaint,
        isRtl: Boolean,
    ) {
        val totalWeight = weights.sum().coerceAtLeast(1f)
        val orderedIndices = if (isRtl) cells.indices.reversed() else cells.indices
        var cursorX = MARGIN_X
        for (idx in orderedIndices) {
            val colWidth = CONTENT_WIDTH * (weights.getOrElse(idx) { 1f } / totalWeight)
            val cellText = cells.getOrElse(idx) { "" }
            drawStaticText(
                canvas = canvas,
                text = cellText,
                x = cursorX + 6f,
                y = textTop,
                maxWidth = (colWidth - 12f).coerceAtLeast(24f),
                paint = paint,
                isRtl = isRtl,
            )
            cursorX += colWidth
        }
    }

    private fun drawFooter(canvas: Canvas, appName: String, pageLabel: String, isRtl: Boolean) {
        val linePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.8f
        }
        canvas.drawLine(MARGIN_X, FOOTER_Y - 10f, MARGIN_X + CONTENT_WIDTH, FOOTER_Y - 10f, linePaint)

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 9f
        }
        drawStaticText(canvas, appName, MARGIN_X, FOOTER_Y - 4f, CONTENT_WIDTH / 2f, footerPaint, isRtl, alignStart = true)
        drawStaticText(canvas, pageLabel, MARGIN_X + CONTENT_WIDTH / 2f, FOOTER_Y - 4f, CONTENT_WIDTH / 2f, footerPaint, !isRtl, alignStart = true)
    }

    private fun drawStaticText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: TextPaint,
        isRtl: Boolean,
        alignStart: Boolean = true,
    ) {
        val widthInt = maxWidth.toInt().coerceAtLeast(16)
        val alignment = if (alignStart) {
            if (isRtl) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
        } else {
            if (isRtl) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, widthInt)
            .setAlignment(alignment)
            .setTextDirection(
                if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR
            )
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}
