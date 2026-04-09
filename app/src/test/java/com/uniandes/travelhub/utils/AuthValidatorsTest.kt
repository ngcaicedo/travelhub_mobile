package com.uniandes.travelhub.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorsTest {

    // ----- email -----

    @Test
    fun `valid emails are accepted`() {
        assertTrue(AuthValidators.isValidEmail("ada@example.com"))
        assertTrue(AuthValidators.isValidEmail("ada.lovelace+travel@sub.example.co"))
        assertTrue(AuthValidators.isValidEmail("  ada@example.com  "))
    }

    @Test
    fun `invalid emails are rejected`() {
        assertFalse(AuthValidators.isValidEmail(""))
        assertFalse(AuthValidators.isValidEmail("ada"))
        assertFalse(AuthValidators.isValidEmail("ada@"))
        assertFalse(AuthValidators.isValidEmail("ada@example"))
        assertFalse(AuthValidators.isValidEmail("@example.com"))
        assertFalse(AuthValidators.isValidEmail("ada@@example.com"))
        assertFalse(AuthValidators.isValidEmail("ada example@example.com"))
    }

    // ----- phone -----

    @Test
    fun `valid phones are accepted including international format and separators`() {
        assertTrue(AuthValidators.isValidPhone("+573001234567"))
        assertTrue(AuthValidators.isValidPhone("3001234567"))
        assertTrue(AuthValidators.isValidPhone("+1 (555) 000-0000"))
        assertTrue(AuthValidators.isValidPhone("555-000-0000"))
    }

    @Test
    fun `invalid phones are rejected`() {
        assertFalse(AuthValidators.isValidPhone(""))
        assertFalse(AuthValidators.isValidPhone("123"))
        assertFalse(AuthValidators.isValidPhone("+1234567890123456"))
        assertFalse(AuthValidators.isValidPhone("abcdefghij"))
        assertFalse(AuthValidators.isValidPhone("+57-300-CALL-NOW"))
    }

    // ----- otp -----

    @Test
    fun `OTP must be exactly 6 digits`() {
        assertTrue(AuthValidators.isValidOtp("123456"))
        assertTrue(AuthValidators.isValidOtp("000000"))
    }

    @Test
    fun `OTP rejects wrong length and non-digit characters`() {
        assertFalse(AuthValidators.isValidOtp(""))
        assertFalse(AuthValidators.isValidOtp("12345"))
        assertFalse(AuthValidators.isValidOtp("1234567"))
        assertFalse(AuthValidators.isValidOtp("12345a"))
        assertFalse(AuthValidators.isValidOtp("12 456"))
    }

    // ----- password strength gate -----

    @Test
    fun `isStrongEnough requires score 3 or more`() {
        assertFalse(AuthValidators.isStrongEnough(""))
        assertFalse(AuthValidators.isStrongEnough("abcdefgh"))
        assertFalse(AuthValidators.isStrongEnough("Abcdefgh"))
        assertTrue(AuthValidators.isStrongEnough("Abcdefg1"))
        assertTrue(AuthValidators.isStrongEnough("Abcdefg1!"))
    }
}
