package com.uniandes.travelhub.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.uniandes.travelhub.models.properties.Property
import kotlinx.coroutines.flow.firstOrNull

private val Context.propertyCacheDataStore by preferencesDataStore(name = "travelhub_property_cache")

interface PropertyCacheStore {
    suspend fun getProperty(id: String): Property?
    suspend fun saveProperty(property: Property)
    suspend fun removeProperty(id: String)
}

class DataStorePropertyCacheStore private constructor(
    private val dataStore: DataStore<Preferences>,
    moshi: Moshi
) : PropertyCacheStore {

    private val adapter: JsonAdapter<Property> = moshi.adapter(Property::class.java)

    override suspend fun getProperty(id: String): Property? {
        val json = dataStore.data.firstOrNull()?.get(propertyKey(id)) ?: return null
        return runCatching { adapter.fromJson(json) }.getOrNull()
    }

    override suspend fun saveProperty(property: Property) {
        val json = adapter.toJson(property)
        dataStore.edit { prefs ->
            prefs[propertyKey(property.id)] = json
        }
    }

    override suspend fun removeProperty(id: String) {
        dataStore.edit { prefs ->
            prefs.remove(propertyKey(id))
        }
    }

    companion object {
        @Volatile
        private var instance: DataStorePropertyCacheStore? = null

        fun getInstance(
            context: Context,
            moshi: Moshi = Moshi.Builder().build()
        ): DataStorePropertyCacheStore =
            instance ?: synchronized(this) {
                instance ?: DataStorePropertyCacheStore(
                    dataStore = context.applicationContext.propertyCacheDataStore,
                    moshi = moshi
                ).also { instance = it }
            }

        private fun propertyKey(id: String): Preferences.Key<String> =
            stringPreferencesKey("property_cache_$id")
    }
}
