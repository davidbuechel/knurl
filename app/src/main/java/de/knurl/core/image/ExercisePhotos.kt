package de.knurl.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Photos of own exercises: small square WebPs (about 40 KB) stored as base64 in Firestore, so they
 * sync without Cloud Storage (we stay on the Spark plan, see docs/firebase.md).
 */
object ExercisePhotos {
    const val SIZE_PX = 480
    private const val QUALITY = 75

    /** Rules allow 200 000 characters; a 480 px WebP is far below. Larger results are re-encoded smaller. */
    private const val MAX_BASE64 = 190_000

    /** Reads the picked or taken image, crops the centre square, scales it and returns base64 WebP. */
    fun fromUri(context: Context, uri: Uri): String? = runCatching {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // Decode close to the target size right away: camera photos are 12+ MP.
            val shortSide = min(info.size.width, info.size.height)
            val factor = (shortSide / (SIZE_PX * 2)).coerceAtLeast(1)
            decoder.setTargetSize(info.size.width / factor, info.size.height / factor)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val side = min(decoded.width, decoded.height)
        val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, SIZE_PX, SIZE_PX, true)
        var quality = QUALITY
        var encoded: String
        do {
            encoded = Base64.encodeToString(scaled.webp(quality), Base64.NO_WRAP)
            quality -= 15
        } while (encoded.length > MAX_BASE64 && quality > 20)
        encoded.takeIf { it.length <= MAX_BASE64 }
    }.getOrNull()

    private fun Bitmap.webp(quality: Int): ByteArray = ByteArrayOutputStream().use { out ->
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        compress(format, quality, out)
        out.toByteArray()
    }

    /** Decoded photos, keyed by the base64 text itself, so a changed photo is decoded anew. */
    private val cache = LruCache<Int, ImageBitmap>(64)

    fun decode(base64: String): ImageBitmap? {
        val key = base64.hashCode() * 31 + base64.length
        cache.get(key)?.let { return it }
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()?.also { cache.put(key, it) }
    }
}
