package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Generic Firebase Cloud Firestore repository providing type-safe Kotlin Coroutine CRUD operations,
 * real-time Flow observation, staff-scoped (`uid`) query filtering, and offline persistence.
 */
open class FirestoreRepository<T : Any>(
    protected val db: FirebaseFirestore,
    val collectionPath: String,
    private val modelClass: Class<T>,
    protected val auth: FirebaseAuth = Firebase.auth,
    private val uidField: String = "uid",
    private val defaultSort: (List<T>) -> List<T> = { it },
) {
    constructor(
        context: Context,
        collectionPath: String,
        modelClass: Class<T>,
        defaultSort: (List<T>) -> List<T> = { it },
    ) : this(
        db = createConfiguredFirestore(context.applicationContext),
        collectionPath = collectionPath,
        modelClass = modelClass,
        defaultSort = defaultSort,
    )

    protected fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        require(!uid.isNullOrBlank()) { "User must be signed in before accessing Firestore." }
        return uid
    }

    // ---- Read: Staff-filtered (whereEqualTo("uid", uid)) & Admin-wide ----

    fun observeForStaff(uid: String): Flow<List<T>> = flow {
        require(uid.isNotBlank()) { "User ID must not be blank." }
        db.collection(collectionPath)
            .whereEqualTo(uidField, uid)
            .snapshots()
            .map { snapshot ->
                defaultSort(
                    snapshot.toObjects(modelClass, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, collectionPath)
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeAll(): Flow<List<T>> = flow {
        val uid = requireUserId()
        db.collection(collectionPath)
            .snapshots()
            .map { snapshot ->
                defaultSort(
                    snapshot.toObjects(modelClass, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, collectionPath)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun getForStaff(uid: String): Result<List<T>> = try {
        require(uid.isNotBlank()) { "User ID must not be blank." }
        val snapshot = db.collection(collectionPath)
            .whereEqualTo(uidField, uid)
            .get()
            .await()
        val results = defaultSort(
            snapshot.toObjects(modelClass, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
        Result.success(results)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, collectionPath)
        Result.failure(e)
    }

    suspend fun getAll(): Result<List<T>> = try {
        requireUserId()
        val snapshot = db.collection(collectionPath)
            .get()
            .await()
        val results = defaultSort(
            snapshot.toObjects(modelClass, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
        Result.success(results)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, collectionPath)
        Result.failure(e)
    }

    suspend fun getById(docId: String): Result<T?> {
        var path = "$collectionPath/$docId"
        return try {
            requireUserId()
            require(docId.isNotBlank()) { "Document ID must not be blank" }
            val docRef = db.collection(collectionPath).document(docId)
            path = docRef.path
            val snapshot = docRef.get().await()
            val item = if (snapshot.exists()) {
                snapshot.toObject(modelClass, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    // ---- Create / Update / Delete ----

    suspend fun create(
        fields: Map<String, Any?>,
        docId: String? = null,
        includeCreatedAt: Boolean = true,
        includeUpdatedAt: Boolean = false,
    ): Result<String> {
        var path = collectionPath
        return try {
            val uid = requireUserId()
            val docRef = if (docId.isNullOrBlank()) {
                db.collection(collectionPath).document()
            } else {
                db.collection(collectionPath).document(docId)
            }
            path = docRef.path

            val payload = buildMap<String, Any> {
                put(uidField, uid)
                fields.forEach { (key, value) ->
                    if (key != "id" && value != null) {
                        put(key, value)
                    }
                }
                if (includeCreatedAt) {
                    put("createdAt", FieldValue.serverTimestamp())
                }
                if (includeUpdatedAt) {
                    put("updatedAt", FieldValue.serverTimestamp())
                }
            }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun update(
        docId: String,
        updates: Map<String, Any?>,
        includeUpdatedAt: Boolean = false,
    ): Result<Unit> {
        var path = "$collectionPath/$docId"
        return try {
            requireUserId()
            require(docId.isNotBlank()) { "Document ID must not be blank" }
            val docRef = db.collection(collectionPath).document(docId)
            path = docRef.path

            val payload = buildMap<String, Any> {
                updates.forEach { (key, value) ->
                    if (key != "id" && key != uidField && key != "createdAt" && value != null) {
                        put(key, value)
                    }
                }
                if (includeUpdatedAt) {
                    put("updatedAt", FieldValue.serverTimestamp())
                }
            }
            require(payload.isNotEmpty()) { "Update payload must not be empty" }
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun delete(docId: String): Result<Unit> {
        var path = "$collectionPath/$docId"
        return try {
            requireUserId()
            require(docId.isNotBlank()) { "Document ID must not be blank" }
            val docRef = db.collection(collectionPath).document(docId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    companion object {
        fun createConfiguredFirestore(appContext: Context): FirebaseFirestore {
            val dbId = appContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            try {
                db.firestoreSettings = firestoreSettings {
                    setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                }
            } catch (_: IllegalStateException) {
                // Settings already applied on this Firestore instance
            }
            return db
        }
    }
}
