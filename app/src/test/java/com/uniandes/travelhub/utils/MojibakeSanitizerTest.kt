package com.uniandes.travelhub.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MojibakeSanitizerTest {

    @Test
    fun `sanitizeDisplayText fixes common mojibake`() {
        assertEquals("Bogotá", sanitizeDisplayText("BogotÃ¡"))
        assertEquals("Excelente opción", sanitizeDisplayText("Excelente opciÃ³n"))
        assertEquals("Recepción", sanitizeDisplayText("RecepciÃ³n"))
        assertEquals("Andrés Melo", sanitizeDisplayText("AndrÃ©s Melo"))
    }

    @Test
    fun `sanitizeDisplayText leaves valid utf8 text untouched`() {
        val value = "Laura Sánchez"
        assertEquals(value, sanitizeDisplayText(value))
    }
}
