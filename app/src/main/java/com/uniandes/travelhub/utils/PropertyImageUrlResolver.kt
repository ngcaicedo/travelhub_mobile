package com.uniandes.travelhub.utils

import com.uniandes.travelhub.models.properties.PropertyImage
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private const val DEFAULT_IMAGE_QUALITY = 75

fun buildResponsiveImageUrl(
    baseUrl: String,
    targetWidthPx: Int,
    quality: Int = DEFAULT_IMAGE_QUALITY,
    format: String = "webp"
): String {
    val httpUrl = baseUrl.toHttpUrlOrNull() ?: return baseUrl

    return httpUrl.newBuilder()
        .apply {
            val keysToPreserve = httpUrl.queryParameterNames.filterNot { it in setOf("w", "q", "fm", "auto") }
            query(null)
            keysToPreserve.forEach { key ->
                httpUrl.queryParameterValues(key).forEach { value ->
                    addQueryParameter(key, value)
                }
            }
            addQueryParameter("w", targetWidthPx.toString())
            addQueryParameter("q", quality.toString())
            addQueryParameter("fm", format)
            addQueryParameter("auto", "format")
        }
        .build()
        .toString()
}

fun resolvePropertyImageUrl(
    image: PropertyImage,
    targetWidthPx: Int,
    preferHighRes: Boolean = false
): String {
    val source = if (preferHighRes) image.urlHires ?: image.url else image.url
    return buildResponsiveImageUrl(source, targetWidthPx)
}

fun sortPropertyImages(images: List<PropertyImage>): List<PropertyImage> {
    return images.sortedWith(
        compareByDescending<PropertyImage> { it.isCover }
            .thenBy { it.position }
    )
}
