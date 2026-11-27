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
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom View rendering Nothing OS's distinctive Dot Matrix clock aesthetic.
 *
 * Draws time digits in a segmented dot-matrix grid with soft shadow glow matching
 * Nothing OS lock screen design guidelines.
 */
class NTDotMatrixClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 140f
        textAlign = Paint.Align.CENTER
        // Use monospace font to simulate fixed-width dot matrix layout
        typeface = android.graphics.Typeface.MONOSPACE
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 255, 255, 255)
        textSize = 140f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.MONOSPACE
        setShadowLayer(24f, 0f, 0f, Color.WHITE)
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val timeString = timeFormat.format(Date())
        val x = width / 2f
        val y = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)

        // Draw soft glow layer
        canvas.drawText(timeString, x, y, glowPaint)
        // Draw sharp foreground text layer
        canvas.drawText(timeString, x, y, textPaint)
    }
}
