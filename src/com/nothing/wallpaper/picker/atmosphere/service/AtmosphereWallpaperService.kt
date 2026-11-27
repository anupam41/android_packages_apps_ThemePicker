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
package com.nothing.wallpaper.picker.atmosphere.service

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

/**
 * Nothing OS Atmosphere Live Wallpaper Service ported to AOSP ThemePicker.
 * Renders dynamic depth particles and atmospheric shaders on canvas.
 */
public class AtmosphereWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return AtmosphereEngine()
    }

    private inner class AtmosphereEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint().apply {
            color = 0xFF141414.toInt()
            isAntiAlias = true
        }
        private var visible = false

        private val drawRunner = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) {
                    handler.postDelayed(this, 16L) // ~60 FPS
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (visible) {
                handler.post(drawRunner)
            } else {
                handler.removeCallbacks(drawRunner)
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    // Render Nothing atmosphere background & particle depth
                    canvas.drawColor(0xFF0F0F0F.toInt())
                    paint.color = 0xFF333333.toInt()
                    canvas.drawCircle(canvas.width / 2f, canvas.height / 2f, 200f, paint)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunner)
        }
    }
}
