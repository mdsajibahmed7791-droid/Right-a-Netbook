package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage(val code: String, val label: String) {
    EN("en", "EN"),
    BN("bn", "বাংলা"),
    AR("ar", "العربية");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: EN
    }
}

enum class Category(val key: String) {
    FOOD("food"),
    COFFEE("coffee"),
    TEA("tea"),
    JUICE("juice"),
    COLD_DRINKS("cold drinks"),
    MOJITO("mojito"),
    CAKE("cake");

    companion object {
        fun fromKey(key: String?): Category =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: FOOD
    }
}

enum class NoteScope(val key: String) {
    FREE("free"),
    DELIVERY("delivery");
}

enum class MainTab {
    FREE,
    ITEMS,
    PEOPLE,
    REPORT,
    DELIVERY,
    ADMIN,
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class MenuItem(
    @DocumentId val id: String = "",
    val uid: String = "",
    val cat: String = Category.FOOD.key,
    val name: String = "",
    val price: Double = 0.0,
    val photo: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Person(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FreeEntry(
    @DocumentId val id: String = "",
    val uid: String = "",
    val item: String = "",
    val base: String = "",
    val cat: String = Category.FOOD.key,
    val price: Double = 0.0,
    val qty: Long = 1L,
    val total: Double = 0.0,
    val person: String = "",
    val ts: Long = 0L,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SavedNote(
    @DocumentId val id: String = "",
    val uid: String = "",
    val scope: String = NoteScope.FREE.key,
    val itemId: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DeliveryItem(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    val photo: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DeliveryEntry(
    @DocumentId val id: String = "",
    val uid: String = "",
    val item: String = "",
    val base: String = "",
    val qty: Long = 1L,
    val ts: Long = 0L,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StaffProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class CatalogProduct(
    val id: String,
    val name: String,
    val cat: String?,
    val price: Double,
    val photo: String?,
    val isDeliveryOnly: Boolean = false,
)

data class CartLine(
    val lineKey: String,
    val productId: String,
    val baseName: String,
    val cat: String,
    val unitPrice: Double,
    val notes: List<String>,
    val qty: Int,
) {
    val labelWithNotes: String
        get() = if (notes.isEmpty()) baseName else "$baseName (${notes.joinToString(", ")})"
    val lineTotal: Double
        get() = unitPrice * qty
}

data class NameValueStat(
    val name: String,
    val pieces: Int,
    val amount: Double,
    val photo: String? = null,
    val entriesCount: Int = 0,
)

data class DayStat(
    val dayOfMonth: Int,
    val pieces: Int,
    val amount: Double,
    val summaryText: String = "",
)

data class StaffStat(
    val uid: String,
    val email: String,
    val entriesCount: Int,
    val pieces: Int,
    val amount: Double,
)
