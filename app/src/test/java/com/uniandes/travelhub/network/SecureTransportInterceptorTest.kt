package com.uniandes.travelhub.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class SecureTransportInterceptorTest {
    @Test
    fun `rejects cleartext requests when cleartext is disabled`() {
        val server = MockWebServer().also { it.start() }
        val client = OkHttpClient.Builder()
            .addInterceptor(SecureTransportInterceptor(allowCleartext = false))
            .build()
        val request = Request.Builder().url(server.url("/")).build()

        assertThrows(IOException::class.java) {
            client.newCall(request).execute()
        }

        server.shutdown()
    }

    @Test
    fun `allows cleartext requests in explicit debug mode`() {
        val server = MockWebServer().also {
            it.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
            it.start()
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(SecureTransportInterceptor(allowCleartext = true))
            .build()
        val request = Request.Builder().url(server.url("/")).build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }

        server.shutdown()
    }
}
