package com.uniandes.travelhub.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordStrengthTest {

    @Test
    fun `empty password scores 0 and tier NONE`() {
        assertEquals(0, PasswordStrength.score(""))
        assertEquals(PasswordTier.NONE, PasswordStrength.tier(""))
    }

    @Test
    fun `short password with only lowercase scores 0`() {
        assertEquals(0, PasswordStrength.score("abcdef"))
    }

    @Test
    fun `length only meets one criterion`() {
        assertEquals(1, PasswordStrength.score("abcdefgh"))
        assertEquals(PasswordTier.WEAK, PasswordStrength.tier("abcdefgh"))
    }

    @Test
    fun `length plus uppercase scores 2 and tier MEDIUM`() {
        assertEquals(2, PasswordStrength.score("Abcdefgh"))
        assertEquals(PasswordTier.MEDIUM, PasswordStrength.tier("Abcdefgh"))
    }

    @Test
    fun `length plus uppercase plus digit scores 3 and tier STRONG`() {
        assertEquals(3, PasswordStrength.score("Abcdefg1"))
        assertEquals(PasswordTier.STRONG, PasswordStrength.tier("Abcdefg1"))
    }

    @Test
    fun `all four criteria met scores 4 and tier VERY_STRONG`() {
        assertEquals(4, PasswordStrength.score("Abcdefg1!"))
        assertEquals(PasswordTier.VERY_STRONG, PasswordStrength.tier("Abcdefg1!"))
    }

    @Test
    fun `digit only short password scores 1 even without length`() {
        assertEquals(1, PasswordStrength.score("1234567"))
    }

    @Test
    fun `special character only short password scores 1`() {
        assertEquals(1, PasswordStrength.score("!@#"))
    }

    @Test
    fun `accented characters count as special characters`() {
        assertEquals(1, PasswordStrength.score("ñoño"))
        assertEquals(4, PasswordStrength.score("ABCDefñ1"))
    }

    @Test
    fun `eight chars with mixed case digit and special hits all criteria`() {
        assertEquals(4, PasswordStrength.score("Abcd123!"))
        assertEquals(PasswordTier.VERY_STRONG, PasswordStrength.tier("Abcd123!"))
    }
}
