package com.uniandes.travelhub.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.squareup.moshi.Moshi
import com.uniandes.travelhub.BuildConfig
import com.uniandes.travelhub.models.reservations.CheckInQrPayload
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CheckInQrCodec {
    private const val VERSION_PREFIX = "thci1"
    private const val IV_SIZE_BYTES = 12
    private const val TAG_LENGTH_BITS = 128
    private val moshi = Moshi.Builder().build()
    private val payloadAdapter = moshi.adapter(CheckInQrPayload::class.java)
    private val secretKeySpec: SecretKeySpec by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest("${BuildConfig.APPLICATION_ID}:checkin:travelhub".toByteArray(StandardCharsets.UTF_8))
        SecretKeySpec(keyBytes, "AES")
    }

    fun encodeEncryptedPayload(payload: CheckInQrPayload): String {
        val iv = ByteArray(IV_SIZE_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val plaintext = payloadAdapter.toJson(payload).toByteArray(StandardCharsets.UTF_8)
        val cipherBytes = cipher.doFinal(plaintext)
        val data = ByteBuffer.allocate(iv.size + cipherBytes.size)
            .put(iv)
            .put(cipherBytes)
            .array()
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(data)
        return "$VERSION_PREFIX.$encoded"
    }

    internal fun decodeEncryptedPayloadForTest(encoded: String): CheckInQrPayload {
        val prefixRemoved = encoded.removePrefix("$VERSION_PREFIX.")
        val bytes = Base64.getUrlDecoder().decode(prefixRemoved)
        val iv = bytes.copyOfRange(0, IV_SIZE_BYTES)
        val cipherBytes = bytes.copyOfRange(IV_SIZE_BYTES, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val plaintext = cipher.doFinal(cipherBytes).toString(StandardCharsets.UTF_8)
        return requireNotNull(payloadAdapter.fromJson(plaintext))
    }

    fun createQrBitmap(content: String, sizePx: Int = 768): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                pixels[y * sizePx + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }
}
