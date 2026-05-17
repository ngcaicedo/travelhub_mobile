package com.uniandes.travelhub.utils

enum class PasswordTier { NONE, WEAK, MEDIUM, STRONG, VERY_STRONG }

object PasswordStrength {

    private val UPPERCASE = Regex("[A-Z]")
    private val DIGIT = Regex("[0-9]")
    private val SPECIAL = Regex("[^A-Za-z0-9]")

    fun score(password: String): Int {
        var s = 0
        if (password.length >= 8) s++
        if (UPPERCASE.containsMatchIn(password)) s++
        if (DIGIT.containsMatchIn(password)) s++
        if (SPECIAL.containsMatchIn(password)) s++
        return s
    }

    fun tier(password: String): PasswordTier = when (score(password)) {
        0 -> PasswordTier.NONE
        1 -> PasswordTier.WEAK
        2 -> PasswordTier.MEDIUM
        3 -> PasswordTier.STRONG
        else -> PasswordTier.VERY_STRONG
    }
}
