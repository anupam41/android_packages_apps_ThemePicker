package com.nothing.systemui.shared.clocks.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import java.util.Calendar

class NDotClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        color = 0xFFFFFFFF.toInt()
        textSize = 120f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val bounds = Rect()
    private val calendar = Calendar.getInstance()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        calendar.timeInMillis = System.currentTimeMillis()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timeText = String.format("%02d:%02d", hour, minute)

        paint.getTextBounds(timeText, 0, timeText.length, bounds)
        val x = (width - bounds.width()) / 2f - bounds.left
        val y = (height + bounds.height()) / 2f - bounds.bottom

        // Dot-matrix shadow / glow effect
        paint.color = 0x33FFFFFF.toInt()
        canvas.drawText(timeText, x + 4f, y + 4f, paint)

        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawText(timeText, x, y, paint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        invalidate()
    }
}
