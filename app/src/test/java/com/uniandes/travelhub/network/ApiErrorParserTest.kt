package com.uniandes.travelhub.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErrorParserTest {

    private val fallback = "Algo salió mal"

    private fun httpException(status: Int, body: String): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        val response = Response.error<Any>(status, responseBody)
        return HttpException(response)
    }

    @Test
    fun `parses string detail returned by FastAPI`() {
        val ex = httpException(401, """{"detail":"Credenciales inválidas"}""")

        val message = ApiErrorParser.getApiErrorMessage(ex, fallback)

        assertEquals("Credenciales inválidas", message)
    }

    @Test
    fun `parses array detail joining all msg fields with commas`() {
        val body = """
            {
              "detail": [
                {"loc":["body","email"], "msg":"value is not a valid email", "type":"value_error.email"},
                {"loc":["body","password"], "msg":"ensure this value has at least 8 characters", "type":"value_error"}
              ]
            }
        """.trimIndent()
        val ex = httpException(422, body)

        val message = ApiErrorParser.getApiErrorMessage(ex, fallback)

        assertEquals(
            "value is not a valid email, ensure this value has at least 8 characters",
            message
        )
    }

    @Test
    fun `falls back when body is empty`() {
        val ex = httpException(500, "")

        val message = ApiErrorParser.getApiErrorMessage(ex, fallback)

        assertEquals(fallback, message)
    }

    @Test
    fun `falls back when body is not valid JSON`() {
        val ex = httpException(500, "<html>Internal Error</html>")

        val message = ApiErrorParser.getApiErrorMessage(ex, fallback)

        assertEquals(fallback, message)
    }

    @Test
    fun `falls back when JSON has no detail field`() {
        val ex = httpException(400, """{"error":"bad"}""")

        val message = ApiErrorParser.getApiErrorMessage(ex, fallback)

        assertEquals(fallback, message)
    }

    @Test
    fun `falls back when array detail has no msg fields`() {
        val ex = httpException(422, """{"detail":[{"loc":["body"]}]}""")

        val message = ApiErrorParser.getApiErrorMessage(ex, fallback)

        assertEquals(fallback, message)
    }

    @Test
    fun `IOException returns fallback message`() {
        val message = ApiErrorParser.getApiErrorMessage(IOException("no network"), fallback)

        assertEquals(fallback, message)
    }

    @Test
    fun `unknown throwable uses its message`() {
        val message = ApiErrorParser.getApiErrorMessage(RuntimeException("boom"), fallback)

        assertEquals("boom", message)
    }
}
