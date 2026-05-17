package com.uniandes.travelhub.notifications

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.pendingDataStore by preferencesDataStore(name = "travelhub_pending_notifications")

/**
 * Guarda los audit ids de notificaciones recibidas para reportar 'opened' al
 * backend cuando el usuario las abra (vía deep link o desde la lista).
 *
 * Persistir aquí desacopla el envío del open-event del ciclo de vida del
 * messaging service y permite reintentar si la app se cierra antes.
 */
class PendingNotificationStore(private val context: Context) {

    private val key = stringSetPreferencesKey("pending_audit_ids")

    suspend fun add(auditId: String) {
        context.pendingDataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = current + auditId
        }
    }

    suspend fun remove(auditId: String) {
        context.pendingDataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = current - auditId
        }
    }

    suspend fun all(): Set<String> =
        context.pendingDataStore.data.map { it[key] ?: emptySet() }.first()
}
