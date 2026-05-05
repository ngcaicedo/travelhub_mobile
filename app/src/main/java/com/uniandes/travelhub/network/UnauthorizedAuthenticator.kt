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
 *
 * Same lazy-provider pattern as [AuthInterceptor]: the OkHttp client may be built
 * before [RetrofitFactory.init] runs, so we read the store lazily.
 */
class UnauthorizedAuthenticator(
    private val tokenStoreProvider: () -> AuthTokenStore?,
) : Authenticator {

    constructor(tokenStore: AuthTokenStore) : this({ tokenStore })

    override fun authenticate(route: Route?, response: Response): Request? {
        val tokenStore = tokenStoreProvider() ?: return null
        runBlocking { tokenStore.clear() }
        return null
    }
}
