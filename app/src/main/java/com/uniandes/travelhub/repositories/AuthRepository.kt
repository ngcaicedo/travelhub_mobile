package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.models.auth.LoginRequest
import com.uniandes.travelhub.models.auth.RegisterRequest
import com.uniandes.travelhub.models.auth.UserResponse
import com.uniandes.travelhub.models.auth.VerifyOtpRequest
import com.uniandes.travelhub.network.ApiErrorParser
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.SecurityApi
import com.uniandes.travelhub.network.UsersApi
import kotlinx.coroutines.flow.Flow

class AuthException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class AuthRepository(
    private val securityApi: SecurityApi,
    private val usersApi: UsersApi,
    private val tokenStore: AuthTokenStore,
    private val errorParser: (Throwable, String) -> String = ApiErrorParser::getApiErrorMessage
) {

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        securityApi.login(LoginRequest(email = email, password = password))
        Unit
    }.recoverFailure("No fue posible iniciar sesión")

    suspend fun verifyOtp(email: String, otpCode: String): Result<UserRole> = runCatching {
        val token = securityApi.verifyOtp(VerifyOtpRequest(email = email, otpCode = otpCode))
        tokenStore.saveSession(token.accessToken, token.role)
        token.role
    }.recoverFailure("No fue posible verificar el código")

    suspend fun register(payload: RegisterRequest): Result<UserResponse> = runCatching {
        usersApi.register(payload)
    }.recoverFailure("No fue posible crear la cuenta")

    suspend fun logout() {
        tokenStore.clear()
    }

    fun observeToken(): Flow<String?> = tokenStore.tokenFlow

    fun observeRole(): Flow<UserRole?> = tokenStore.roleFlow

    private fun <T> Result<T>.recoverFailure(fallback: String): Result<T> = fold(
        onSuccess = { this },
        onFailure = { throwable ->
            val message = errorParser(throwable, fallback)
            Result.failure(AuthException(message, throwable))
        }
    )
}
