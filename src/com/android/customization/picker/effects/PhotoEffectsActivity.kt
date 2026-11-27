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
import java.io.FileOutputStream
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
            val scale = minOf(1.0, 3072.0 / maxOf(info.size.width, info.size.height))
            if (scale < 1.0) decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
        }
        val output = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
        if (effect == R.string.photo_effect_glass) {
            val stripe = 32
            canvas.drawBitmap(image, 0f, 0f, paint)
            for (x in 0 until image.width step stripe) {
                val r = minOf(x + stripe, image.width)
                canvas.drawBitmap(image, Rect(x, 0, r, image.height), Rect(x + 4, 0, r + 4, image.height), paint)
            }
        } else {
            val scaled = Bitmap.createScaledBitmap(image, image.width / 8, image.height / 8, true)
            canvas.drawBitmap(Bitmap.createScaledBitmap(scaled, image.width, image.height, true), 0f, 0f, paint)
        }
        val dir = File(cacheDir, "wallpaper-effects")
        dir.mkdirs()
        val file = File(dir, "effect_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> output.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return FileProvider.getUriForFile(this, "${packageName}.effects.provider", file)
    }
}
