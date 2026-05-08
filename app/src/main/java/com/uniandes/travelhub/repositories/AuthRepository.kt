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

class AuthException(message: String?, cause: Throwable? = null) : RuntimeException(message, cause)

class AuthRepository(
    private val securityApi: SecurityApi,
    private val usersApi: UsersApi,
    private val tokenStore: AuthTokenStore,
    private val parseDetail: (Throwable) -> String? = ApiErrorParser::parseBackendDetail,
) {

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        securityApi.login(LoginRequest(email = email, password = password))
        Unit
    }.recoverFailure()

    suspend fun verifyOtp(email: String, otpCode: String): Result<UserRole> = runCatching {
        val token = securityApi.verifyOtp(VerifyOtpRequest(email = email, otpCode = otpCode))
        val normalizedRole = UserRole.fromWire(token.role)
            ?: throw AuthException("Rol no soportado: ${token.role}")
        tokenStore.saveSession(token.accessToken, normalizedRole, email = email)
        normalizedRole
    }.recoverFailure()

    suspend fun register(payload: RegisterRequest): Result<UserResponse> = runCatching {
        usersApi.register(payload)
    }.recoverFailure()

    suspend fun logout() {
        tokenStore.clear()
    }

    fun observeToken(): Flow<String?> = tokenStore.tokenFlow

    fun observeRole(): Flow<UserRole?> = tokenStore.roleFlow

    private fun <T> Result<T>.recoverFailure(): Result<T> = fold(
        onSuccess = { this },
        onFailure = { Result.failure(AuthException(parseDetail(it), it)) }
    )
}
