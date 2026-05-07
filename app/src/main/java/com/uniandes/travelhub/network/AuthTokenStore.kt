package com.uniandes.travelhub.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.utils.JwtUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "travelhub_auth")

class AuthTokenStore(private val dataStore: DataStore<Preferences>) {

    val tokenFlow: Flow<String?> = dataStore.data.map { it[KEY_TOKEN] }

    val roleFlow: Flow<UserRole?> = dataStore.data.map { prefs ->
        prefs[KEY_ROLE]?.let { raw -> runCatching { UserRole.valueOf(raw) }.getOrNull() }
    }

    val localeFlow: Flow<String?> = dataStore.data.map { it[KEY_LOCALE] }
    val emailFlow: Flow<String?> = dataStore.data.map { it[KEY_EMAIL] }
    val userIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_TOKEN]?.let { JwtUtils.extractSubject(it) }
    }

    suspend fun saveSession(token: String, role: UserRole, email: String? = null) {
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_ROLE] = role.name
            if (!email.isNullOrBlank()) prefs[KEY_EMAIL] = email
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_ROLE)
            prefs.remove(KEY_EMAIL)
        }
    }

    suspend fun saveLocale(languageTag: String) {
        dataStore.edit { prefs -> prefs[KEY_LOCALE] = languageTag }
    }

    companion object {
        private val KEY_TOKEN: Preferences.Key<String> = stringPreferencesKey("auth_token")
        private val KEY_ROLE: Preferences.Key<String> = stringPreferencesKey("auth_role")
        private val KEY_LOCALE: Preferences.Key<String> = stringPreferencesKey("app_locale")
        private val KEY_EMAIL: Preferences.Key<String> = stringPreferencesKey("auth_email")

        @Volatile
        private var instance: AuthTokenStore? = null

        fun getInstance(context: Context): AuthTokenStore =
            instance ?: synchronized(this) {
                instance ?: AuthTokenStore(context.applicationContext.authDataStore).also {
                    instance = it
                }
            }
    }
}
