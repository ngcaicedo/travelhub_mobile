package com.uniandes.travelhub.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.uniandes.travelhub.models.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AuthTokenStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: AuthTokenStore

    @Before
    fun setUp() {
        val file = tempFolder.newFile("test_auth.preferences_pb").apply { delete() }
        dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        store = AuthTokenStore(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `tokenFlow and roleFlow start as null`() = runTest(dispatcher) {
        assertNull(store.tokenFlow.first())
        assertNull(store.roleFlow.first())
    }

    @Test
    fun `saveSession persists token and role`() = runTest(dispatcher) {
        store.saveSession("jwt.payload.sig", UserRole.HOTEL_PARTNER, email = "ada@example.com")

        assertEquals("jwt.payload.sig", store.tokenFlow.first())
        assertEquals(UserRole.HOTEL_PARTNER, store.roleFlow.first())
        assertEquals("ada@example.com", store.emailFlow.first())
    }

    @Test
    fun `clear removes token and role but keeps locale`() = runTest(dispatcher) {
        store.saveSession("jwt", UserRole.TRAVELER, email = "ada@example.com")
        store.saveLocale("pt")

        store.clear()

        assertNull(store.tokenFlow.first())
        assertNull(store.roleFlow.first())
        assertNull(store.emailFlow.first())
        assertEquals("pt", store.localeFlow.first())
    }

    @Test
    fun `tokenFlow emits new value after saveSession`() = runTest(dispatcher) {
        store.tokenFlow.test {
            assertNull(awaitItem())
            store.saveSession("new-token", UserRole.TRAVELER, email = "ada@example.com")
            assertEquals("new-token", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveLocale persists language tag`() = runTest(dispatcher) {
        store.saveLocale("en")

        assertEquals("en", store.localeFlow.first())
    }

    @Test
    fun `roleFlow returns null when stored role name is invalid`() = runTest(dispatcher) {
        // Inject a corrupt role string directly into the underlying DataStore so we
        // can verify the runCatching/valueOf guard in roleFlow handles it gracefully.
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("auth_role")] = "GARBAGE"
        }

        assertNull(store.roleFlow.first())
    }
}
