package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.airbnb.lottie.LottieCompositionFactory
import com.example.data.FirestoreRepository
import com.example.data.NotebookRepository
import com.example.model.Category
import com.example.model.MenuItem
import com.example.model.Person
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Notebook", appName)
    }

    @Test
    fun `generic repository configures firestore with persistent offline cache`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val db = FirestoreRepository.createConfiguredFirestore(context)
        assertTrue(db.firestoreSettings.cacheSettings is PersistentCacheSettings)
    }

    @Test
    fun `generic FirestoreRepository CRUD validates auth and staff uid filter`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val auth = FirebaseAuth.getInstance()
        auth.signOut()
        val db = FirestoreRepository.createConfiguredFirestore(context)

        val itemsRepo = FirestoreRepository(db, "items", MenuItem::class.java, auth)
        val peopleRepo = FirestoreRepository(db, "people", Person::class.java, auth)

        assertTrue(itemsRepo.create(mapOf("name" to "Latte", "price" to 15.0)).isFailure)
        assertTrue(itemsRepo.update("item_1", mapOf("name" to "Espresso")).isFailure)
        assertTrue(itemsRepo.delete("item_1").isFailure)
        assertTrue(itemsRepo.getById("item_1").isFailure)
        assertTrue(itemsRepo.getAll().isFailure)

        assertTrue(peopleRepo.create(mapOf("name" to "Omar")).isFailure)
        assertTrue(peopleRepo.update("person_1", mapOf("name" to "Tariq")).isFailure)
        assertTrue(peopleRepo.delete("person_1").isFailure)

        val blankUidFailure = runCatching { itemsRepo.observeForStaff("").first() }
        assertTrue(blankUidFailure.isFailure)
    }

    @Test
    fun `notebook repository items and people CRUD validates auth and inputs cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val auth = FirebaseAuth.getInstance()
        auth.signOut()
        val repository = NotebookRepository(
            db = NotebookRepository.createConfiguredFirestore(context),
            auth = auth,
        )

        assertTrue(repository.addItem(Category.COFFEE, "Latte", 15.0, null).isFailure)
        assertTrue(repository.updateItem("item_1", Category.COFFEE, "Latte", 15.0).isFailure)
        assertTrue(repository.deleteItem("item_1").isFailure)
        assertTrue(repository.getItemById("item_1").isFailure)

        assertTrue(repository.addPerson("Omar").isFailure)
        assertTrue(repository.updatePerson("person_1", "Omar").isFailure)
        assertTrue(repository.deletePerson("person_1").isFailure)
        assertTrue(repository.getPersonById("person_1").isFailure)
    }

    @Test
    fun `lottie raw resources parse cleanly for loading and task completion`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loadingResult = LottieCompositionFactory.fromRawResSync(context, R.raw.lottie_loading_pulse)
        val successResult = LottieCompositionFactory.fromRawResSync(context, R.raw.lottie_task_success)

        assertNotNull(loadingResult.value)
        assertNotNull(successResult.value)
        assertTrue(loadingResult.value!!.durationFrames > 0f)
        assertTrue(successResult.value!!.durationFrames > 0f)
    }
}
