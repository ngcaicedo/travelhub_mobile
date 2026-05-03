package com.uniandes.travelhub.network

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Clears the persisted session whenever a 401 is received so the next observe of
 * `roleFlow`/`tokenFlow` emits null and the navigation graph routes back to login.
 *
 * Returning `null` tells OkHttp not to retry — we don't have a refresh-token flow.
 */
class UnauthorizedAuthenticator(private val tokenStore: AuthTokenStore) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        runBlocking { tokenStore.clear() }
        return null
    }
}
