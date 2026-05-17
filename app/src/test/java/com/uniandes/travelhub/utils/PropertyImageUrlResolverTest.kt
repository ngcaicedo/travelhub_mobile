package com.uniandes.travelhub.utils

import com.uniandes.travelhub.models.properties.PropertyImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PropertyImageUrlResolverTest {

    @Test
    fun `buildResponsiveImageUrl rewrites width quality and webp`() {
        val result = buildResponsiveImageUrl(
            baseUrl = "https://images.unsplash.com/photo-123?w=800&q=80",
            targetWidthPx = 480
        )

        assertTrue(result.contains("w=480"))
        assertTrue(result.contains("q=75"))
        assertTrue(result.contains("fm=webp"))
        assertTrue(result.contains("auto=format"))
    }

    @Test
    fun `resolvePropertyImageUrl uses hires source when requested`() {
        val image = PropertyImage(
            id = "1",
            url = "https://images.unsplash.com/photo-low?w=800&q=80",
            urlHires = "https://images.unsplash.com/photo-hires?w=1920&q=90",
            isCover = true,
            position = 0
        )

        val result = resolvePropertyImageUrl(
            image = image,
            targetWidthPx = 720,
            preferHighRes = true
        )

        assertTrue(result.contains("photo-hires"))
        assertTrue(result.contains("w=720"))
    }

    @Test
    fun `sortPropertyImages prioritizes cover before position`() {
        val sorted = sortPropertyImages(
            listOf(
                PropertyImage(id = "2", url = "b", isCover = false, position = 1),
                PropertyImage(id = "3", url = "c", isCover = true, position = 5),
                PropertyImage(id = "1", url = "a", isCover = false, position = 0)
            )
        )

        assertEquals("3", sorted.first().id)
        assertEquals(listOf("3", "1", "2"), sorted.map { it.id })
    }
}
