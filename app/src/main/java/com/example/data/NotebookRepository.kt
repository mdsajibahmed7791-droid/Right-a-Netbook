package com.example.data

import android.content.Context
import com.example.model.CartLine
import com.example.model.Category
import com.example.model.DeliveryEntry
import com.example.model.DeliveryItem
import com.example.model.FreeEntry
import com.example.model.MenuItem
import com.example.model.NoteScope
import com.example.model.Person
import com.example.model.SavedNote
import com.example.model.StaffProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NotebookRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext)
    )

    val itemsRepo = FirestoreRepository(db, "items", MenuItem::class.java, auth) { list ->
        list.sortedBy { it.name.lowercase() }
    }
    val peopleRepo = FirestoreRepository(db, "people", Person::class.java, auth) { list ->
        list.sortedBy { it.name.lowercase() }
    }
    val entriesRepo = FirestoreRepository(db, "entries", FreeEntry::class.java, auth) { list ->
        list.sortedByDescending { it.ts }
    }
    val notesRepo = FirestoreRepository(db, "notes", SavedNote::class.java, auth) { list ->
        list.sortedBy { it.text.lowercase() }
    }
    val dlItemsRepo = FirestoreRepository(db, "dlItems", DeliveryItem::class.java, auth) { list ->
        list.sortedBy { it.name.lowercase() }
    }
    val dlEntriesRepo = FirestoreRepository(db, "dlEntries", DeliveryEntry::class.java, auth) { list ->
        list.sortedByDescending { it.ts }
    }
    val staffRepo = FirestoreRepository(db, "staff", StaffProfile::class.java, auth) { list ->
        list.sortedBy { it.email.lowercase() }
    }

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        require(!uid.isNullOrBlank()) { "User must be signed in before accessing Firestore." }
        return uid
    }

    private fun isBootstrapAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.isEmailVerified && user.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)
    }

    fun observeIsAdmin(uid: String): Flow<Boolean> = flow {
        if (uid.isBlank()) {
            emit(false)
            return@flow
        }
        if (isBootstrapAdmin()) {
            emit(true)
            return@flow
        }
        val path = "admins/$uid"
        db.collection("admins").document(uid)
            .snapshots()
            .map { snapshot -> snapshot.exists() || isBootstrapAdmin() }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                }
                emit(isBootstrapAdmin())
            }
            .collect { emit(it) }
    }

    suspend fun upsertCurrentStaffProfile(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val email = auth.currentUser?.email?.take(160) ?: return Result.success(Unit)
        val displayName = (auth.currentUser?.displayName ?: "").take(100)
        return staffRepo.create(
            fields = mapOf(
                "email" to email,
                "displayName" to displayName,
            ),
            docId = uid,
            includeCreatedAt = false,
            includeUpdatedAt = true,
        ).map { }
    }

    // ---- Staff-scoped realtime streams & reads (filtered by uid) ----

    fun observeItems(uid: String): Flow<List<MenuItem>> = itemsRepo.observeForStaff(uid)

    suspend fun getItems(uid: String): Result<List<MenuItem>> = itemsRepo.getForStaff(uid)

    suspend fun getItemById(itemId: String): Result<MenuItem?> = itemsRepo.getById(itemId)

    fun observePeople(uid: String): Flow<List<Person>> = peopleRepo.observeForStaff(uid)

    suspend fun getPeople(uid: String): Result<List<Person>> = peopleRepo.getForStaff(uid)

    suspend fun getPersonById(personId: String): Result<Person?> = peopleRepo.getById(personId)

    fun observeEntries(uid: String): Flow<List<FreeEntry>> = entriesRepo.observeForStaff(uid)

    fun observeNotes(uid: String): Flow<List<SavedNote>> = notesRepo.observeForStaff(uid)

    fun observeDlItems(uid: String): Flow<List<DeliveryItem>> = dlItemsRepo.observeForStaff(uid)

    fun observeDlEntries(uid: String): Flow<List<DeliveryEntry>> = dlEntriesRepo.observeForStaff(uid)

    // ---- Admin-wide realtime streams ----

    fun observeAllEntries(): Flow<List<FreeEntry>> = entriesRepo.observeAll()

    fun observeAllDlEntries(): Flow<List<DeliveryEntry>> = dlEntriesRepo.observeAll()

    fun observeAllStaff(): Flow<List<StaffProfile>> = staffRepo.observeAll()

    fun observeAllItems(): Flow<List<MenuItem>> = itemsRepo.observeAll()

    fun observeAllDlItems(): Flow<List<DeliveryItem>> = dlItemsRepo.observeAll()

    // ---- Mutations (all validated & wrapped in Result<T>) ----

    suspend fun addItem(cat: Category, name: String, price: Double, photoBase64: String?): Result<Unit> {
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Item name must not be blank"))
        }
        if (price !in 0.0..100000.0) {
            return Result.failure(IllegalArgumentException("Price must be between 0 and 100000"))
        }
        val cleanPhoto = photoBase64?.takeIf { it.isNotBlank() && it.length <= 150000 }
        return itemsRepo.create(
            fields = mapOf(
                "cat" to cat.key,
                "name" to cleanName,
                "price" to price,
                "photo" to cleanPhoto,
            ),
            includeCreatedAt = true,
            includeUpdatedAt = true,
        ).map { }
    }

    suspend fun updateItem(
        itemId: String,
        cat: Category,
        name: String,
        price: Double,
        photoBase64: String? = null,
        clearPhoto: Boolean = false,
    ): Result<Unit> {
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Item name must not be blank"))
        }
        if (price !in 0.0..100000.0) {
            return Result.failure(IllegalArgumentException("Price must be between 0 and 100000"))
        }
        val updates = mutableMapOf<String, Any?>(
            "cat" to cat.key,
            "name" to cleanName,
            "price" to price,
        )
        when {
            clearPhoto -> updates["photo"] = FieldValue.delete()
            photoBase64 != null -> {
                if (photoBase64.isBlank() || photoBase64.length > 150000) {
                    return Result.failure(IllegalArgumentException("Invalid photo size"))
                }
                updates["photo"] = photoBase64
            }
        }
        return itemsRepo.update(itemId, updates, includeUpdatedAt = true)
    }

    suspend fun updateItemPhoto(itemId: String, photoBase64: String): Result<Unit> {
        if (photoBase64.isBlank() || photoBase64.length > 150000) {
            return Result.failure(IllegalArgumentException("Invalid photo size"))
        }
        return itemsRepo.update(itemId, mapOf("photo" to photoBase64), includeUpdatedAt = true)
    }

    suspend fun deleteItem(itemId: String): Result<Unit> = itemsRepo.delete(itemId)

    suspend fun addPerson(name: String): Result<Unit> {
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Name must not be blank"))
        }
        return peopleRepo.create(
            fields = mapOf("name" to cleanName),
            includeCreatedAt = true,
            includeUpdatedAt = false,
        ).map { }
    }

    suspend fun updatePerson(personId: String, name: String): Result<Unit> {
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Name must not be blank"))
        }
        return peopleRepo.update(personId, mapOf("name" to cleanName), includeUpdatedAt = false)
    }

    suspend fun deletePerson(personId: String): Result<Unit> = peopleRepo.delete(personId)

    suspend fun addNote(scope: NoteScope, itemId: String, text: String): Result<Unit> {
        val cleanText = text.trim().take(80)
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Note text must not be blank"))
        }
        if (itemId.isBlank()) {
            return Result.failure(IllegalArgumentException("Item ID must not be blank"))
        }
        return notesRepo.create(
            fields = mapOf(
                "scope" to scope.key,
                "itemId" to itemId.take(128),
                "text" to cleanText,
            ),
            includeCreatedAt = true,
            includeUpdatedAt = false,
        ).map { }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> = notesRepo.delete(noteId)

    suspend fun saveFreeOrder(
        lines: List<CartLine>,
        personName: String,
        tsMillis: Long,
        orderNote: String?,
        existingPeopleNames: Set<String>,
    ): Result<Unit> {
        val path = "entries"
        return try {
            val uid = requireUserId()
            val cleanPerson = personName.trim().take(80)
            require(cleanPerson.isNotEmpty()) { "Taken by is required" }
            require(lines.isNotEmpty()) { "Order cannot be empty" }
            require(tsMillis > 0L) { "Invalid timestamp" }
            val cleanOrderNote = orderNote?.trim()?.takeIf { it.isNotEmpty() }?.take(250)

            val batch = db.batch()
            if (existingPeopleNames.none { it.equals(cleanPerson, ignoreCase = true) }) {
                val personRef = db.collection("people").document()
                batch.set(
                    personRef,
                    mapOf(
                        "uid" to uid,
                        "name" to cleanPerson,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            for (line in lines) {
                require(line.qty >= 1) { "Quantity must be at least 1" }
                val docRef = db.collection("entries").document()
                val payload = mapOf(
                    "uid" to uid,
                    "item" to line.labelWithNotes.take(200),
                    "base" to line.baseName.take(80),
                    "cat" to Category.fromKey(line.cat).key,
                    "price" to line.unitPrice,
                    "qty" to line.qty.toLong(),
                    "total" to line.lineTotal,
                    "person" to cleanPerson,
                    "ts" to tsMillis,
                    "note" to cleanOrderNote,
                    "createdAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null }
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteEntry(entryId: String): Result<Unit> = entriesRepo.delete(entryId)

    suspend fun addDlItem(name: String, photoBase64: String?): Result<Unit> {
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Item name must not be blank"))
        }
        val cleanPhoto = photoBase64?.takeIf { it.isNotBlank() && it.length <= 150000 }
        return dlItemsRepo.create(
            fields = mapOf(
                "name" to cleanName,
                "photo" to cleanPhoto,
            ),
            includeCreatedAt = true,
            includeUpdatedAt = true,
        ).map { }
    }

    suspend fun deleteDlItem(dlItemId: String): Result<Unit> = dlItemsRepo.delete(dlItemId)

    suspend fun saveDeliveryOrder(
        lines: List<CartLine>,
        tsMillis: Long,
        orderNote: String?,
    ): Result<Unit> {
        val path = "dlEntries"
        return try {
            val uid = requireUserId()
            require(lines.isNotEmpty()) { "Delivery order cannot be empty" }
            require(tsMillis > 0L) { "Invalid timestamp" }
            val cleanOrderNote = orderNote?.trim()?.takeIf { it.isNotEmpty() }?.take(250)

            val batch = db.batch()
            for (line in lines) {
                require(line.qty >= 1) { "Quantity must be at least 1" }
                val docRef = db.collection("dlEntries").document()
                val payload = mapOf(
                    "uid" to uid,
                    "item" to line.labelWithNotes.take(200),
                    "base" to line.baseName.take(80),
                    "qty" to line.qty.toLong(),
                    "ts" to tsMillis,
                    "note" to cleanOrderNote,
                    "createdAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null }
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDlEntry(dlEntryId: String): Result<Unit> = dlEntriesRepo.delete(dlEntryId)

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "sojibahmed186@gmail.com"

        fun createConfiguredFirestore(appContext: Context): FirebaseFirestore =
            FirestoreRepository.createConfiguredFirestore(appContext)
    }
}
