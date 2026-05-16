package com.uniandes.travelhub.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CheckInQrCodecTest {

    @Test
    fun `createQrBitmap returns a bitmap with the requested size`() {
        val bitmap = CheckInQrCodec.createQrBitmap("thci1.sample-payload", sizePx = 256)

        assertNotNull(bitmap)
        assertEquals(256, bitmap.width)
        assertEquals(256, bitmap.height)
    }
}
