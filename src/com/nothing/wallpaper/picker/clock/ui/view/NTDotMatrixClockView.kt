/*
 * Copyright (C) 2026 Nothing Technology Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nothing.wallpaper.picker.clock.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.nothing.wallpaper.picker.clock.model.NTClockStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom View rendering Nothing OS's distinctive Dot Matrix clock aesthetic.
 *
 * Draws time digits in a segmented dot-matrix grid with soft shadow glow matching
 * Nothing OS lock screen design guidelines, supporting precise top-of-minute/second ticking
 * and cached SimpleDateFormat instances to avoid GC churn.
 */
class NTDotMatrixClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int,
) : View(context, attrs, defStyleAttr) {

    var clockStyle: NTClockStyle = NTClockStyle()
        set(value) {
            field = value
            updatePaints()
            updateDateFormat()
            invalidate()
        }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 140f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255)
        textSize = 140f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setShadowLayer(32f, 0f, 0f, Color.WHITE)
    }

    private lateinit var cachedTimeFormat: SimpleDateFormat

    private val tickRunnable = object : Runnable {
        override fun run() {
            invalidate()
            val now = System.currentTimeMillis()
            val interval = if (clockStyle.showSeconds) {
                1000L - (now % 1000L)
            } else {
                60000L - (now % 60000L)
            }
            postDelayed(this, interval.coerceAtLeast(100L))
        }
    }

    init {
        updatePaints()
        updateDateFormat()
    }

    private fun updatePaints() {
        val tfStyle = if (clockStyle.isBold) Typeface.BOLD else Typeface.NORMAL
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, tfStyle)
        glowPaint.typeface = Typeface.create(Typeface.MONOSPACE, tfStyle)

        val alpha = (200 * clockStyle.glowIntensity).toInt().coerceIn(0, 255)
        glowPaint.color = Color.argb(alpha, 255, 255, 255)
    }

    private fun updateDateFormat() {
        cachedTimeFormat = if (clockStyle.showSeconds) {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        } else {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(tickRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(tickRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val timeString = cachedTimeFormat.format(Date())
        val x = width / 2f
        val y = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)

        // Draw soft glow layer
        canvas.drawText(timeString, x, y, glowPaint)
        // Draw sharp foreground text layer
        canvas.drawText(timeString, x, y, textPaint)
    }
}
