package com.uniandes.travelhub.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.uniandes.travelhub.models.reservations.CachedCheckInQr
import kotlinx.coroutines.flow.first

private val Context.checkInQrDataStore by preferencesDataStore(name = "travelhub_checkin_qr")

interface CheckInQrCacheStore {
    suspend fun get(reservationId: String): CachedCheckInQr?
    suspend fun put(value: CachedCheckInQr)
    suspend fun remove(reservationId: String)
}

class DataStoreCheckInQrCacheStore(
    private val dataStore: DataStore<Preferences>,
) : CheckInQrCacheStore {
    private val adapter = Moshi.Builder().build().adapter(CachedCheckInQr::class.java)

    override suspend fun get(reservationId: String): CachedCheckInQr? {
        val payload = dataStore.data.first()[key(reservationId)] ?: return null
        return runCatching { adapter.fromJson(payload) }.getOrNull()
    }

    override suspend fun put(value: CachedCheckInQr) {
        val raw = adapter.toJson(value)
        dataStore.edit { prefs ->
            prefs[key(value.reservationId)] = raw
        }
    }

    override suspend fun remove(reservationId: String) {
        dataStore.edit { prefs -> prefs.remove(key(reservationId)) }
    }

    private fun key(reservationId: String) = stringPreferencesKey("checkin_qr_$reservationId")

    companion object {
        @Volatile
        private var instance: CheckInQrCacheStore? = null

        fun getInstance(context: Context): CheckInQrCacheStore =
            instance ?: synchronized(this) {
                instance ?: DataStoreCheckInQrCacheStore(
                    context.applicationContext.checkInQrDataStore
                ).also { instance = it }
            }
    }
}
