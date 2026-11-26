package com.android.customization.picker.effects

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.android.themepicker.R
import com.android.wallpaper.model.ImageWallpaperInfo
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import java.io.File
import java.io.IOException
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EffectImageProvider : FileProvider()

class PhotoEffectsActivity : ComponentActivity() {
    private var source: Uri? = null
    private var effect: Int = 0
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        source = uri
        if (uri == null) finish() else showEffects()
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        source = state?.getString("source")?.let(Uri::parse)
        effect = state?.getInt("effect") ?: 0
        when {
            state == null -> pickPhoto.launch("image/*")
            effect != 0 -> preview()
            source != null -> showEffects()
        }
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        state.putString("source", source?.toString())
        state.putInt("effect", effect)
    }

    private fun showEffects() {
        val effects = listOf(R.string.photo_effect_glass, R.string.photo_effect_blur)
        AlertDialog.Builder(this)
            .setTitle(R.string.photo_effects_title)
            .setItems(effects.map { getString(it) }.toTypedArray()) { _, index ->
                effect = effects[index]
                preview()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun preview() {
        val uri = source ?: return finish()
        setContentView(
            TextView(this).apply {
                gravity = Gravity.CENTER
                setText(R.string.photo_effect_rendering)
            }
        )
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { render(uri) }
                val intent = WallpaperPreviewActivity.intentBuilder(this@PhotoEffectsActivity, false)
                    .wallpaperInfo(ImageWallpaperInfo(result))
                    .viewAsHome(true)
                    .build()
                startActivity(intent)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(this@PhotoEffectsActivity, R.string.photo_effect_failed, Toast.LENGTH_LONG)
                    .show()
            }
            finish()
        }
    }

    private fun render(uri: Uri): Uri {
        val input = ImageDecoder.createSource(contentResolver, uri)
        val image = ImageDecoder.decodeBitmap(input) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val scale = minOf(1.0, 3072.0 / maxOf(width, height))
            decoder.setTargetSize(
                maxOf(1, (width * scale).roundToInt()),
                maxOf(1, (height * scale).roundToInt()),
            )
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val width = image.width
        val height = image.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        if (effect == R.string.photo_effect_blur) {
            val small = Bitmap.createScaledBitmap(image, maxOf(1, width / 48), maxOf(1, height / 48), true)
            canvas.drawBitmap(small, null, Rect(0, 0, width, height), paint)
        } else {
            val period = maxOf(8, width / 32)
            val from = Rect(0, 0, 0, height)
            val to = Rect(0, 0, 0, height)
            for (x in 0 until width) {
                val shift = (period * 0.22 * sin(2 * PI * (x % period) / period)).roundToInt()
                from.left = (x + shift).coerceIn(0, width - 1)
                from.right = from.left + 1
                to.left = x
                to.right = x + 1
                canvas.drawBitmap(image, from, to, paint)
            }
        }
        val dir = File(cacheDir, "wallpaper-effects").apply { mkdirs() }
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
        val file = File.createTempFile("preview-", ".png", dir)
        try {
            file.outputStream().buffered().use {
                if (!output.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("PNG encode failed")
            }
            return FileProvider.getUriForFile(this, "com.android.wallpaper.photoeffects", file)
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }
}
