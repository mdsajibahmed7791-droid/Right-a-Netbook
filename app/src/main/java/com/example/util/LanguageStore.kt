package com.example.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notebookPrefs by preferencesDataStore(name = "notebook_settings")

class LanguageStore(private val context: Context) {
    private val langKey = stringPreferencesKey("app_language")

    val languageFlow: Flow<AppLanguage> = context.notebookPrefs.data
        .map { prefs -> AppLanguage.fromCode(prefs[langKey]) }

    suspend fun setLanguage(language: AppLanguage) {
        context.notebookPrefs.edit { prefs ->
            prefs[langKey] = language.code
        }
    }
}
