package com.uniandes.travelhub.utils

import java.nio.charset.StandardCharsets

private val suspiciousMarkers = listOf("Ã", "Â", "â€", "â€™", "â€œ", "â€", "â€¢")

fun sanitizeDisplayText(value: String): String {
    if (suspiciousMarkers.none(value::contains)) {
        return value
    }

    return runCatching {
        val bytes = value.toByteArray(StandardCharsets.ISO_8859_1)
        String(bytes, StandardCharsets.UTF_8)
    }.getOrDefault(value)
}
