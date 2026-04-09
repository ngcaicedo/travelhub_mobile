package com.uniandes.travelhub.utils

object AuthValidators {

    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    // E.164-ish: optional leading +, then 7 to 15 digits.
    private val PHONE = Regex("^\\+?[0-9]{7,15}$")

    private const val OTP_LENGTH = 6

    fun isValidEmail(input: String): Boolean = EMAIL.matches(input.trim())

    fun isValidPhone(input: String): Boolean {
        val cleaned = input.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        return PHONE.matches(cleaned)
    }

    fun isValidOtp(input: String): Boolean =
        input.length == OTP_LENGTH && input.all { it.isDigit() }

    fun isStrongEnough(password: String): Boolean = PasswordStrength.score(password) >= 3
}
