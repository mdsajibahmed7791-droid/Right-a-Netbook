package com.example

import com.example.model.CartLine
import com.example.model.Category
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ExampleUnitTest {
    @Test
    fun cartLine_formatsLabelAndTotal() {
        val lineNoNotes = CartLine(
            lineKey = "1|",
            productId = "1",
            baseName = "Pasta",
            cat = Category.FOOD.key,
            unitPrice = 25.0,
            notes = emptyList(),
            qty = 2,
        )
        assertEquals("Pasta", lineNoNotes.labelWithNotes)
        assertEquals(50.0, lineNoNotes.lineTotal, 0.001)

        val lineWithNotes = lineNoNotes.copy(notes = listOf("Chicken", "White sauce"))
        assertEquals("Pasta (Chicken, White sauce)", lineWithNotes.labelWithNotes)
    }

    @Test
    fun formatUtils_formatsCurrencyAndPdfFilename() {
        assertEquals("25 SR", FormatUtils.formatCurrency(25.0, "en", "SR"))
        assertEquals("notebook-2026-11.pdf", FormatUtils.pdfFilename("notebook", YearMonth.of(2026, 11)))
        assertTrue(FormatUtils.formatCurrency(25.0, "bn", "SR").endsWith("SR"))
        assertTrue(FormatUtils.formatCurrency(25.0, "ar", "SR").endsWith("SR"))
    }

    @Test
    fun categoryFromKey_handlesAllCategories() {
        assertEquals(Category.COLD_DRINKS, Category.fromKey("cold drinks"))
        assertEquals(Category.MOJITO, Category.fromKey("mojito"))
        assertEquals(Category.FOOD, Category.fromKey("unknown"))
    }
}
