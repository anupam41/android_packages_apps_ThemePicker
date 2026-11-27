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
package com.nothing.wallpaper.picker.hearthstone.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Nothing Hearthstone / Glyph Sound & Theme integration service ported to AOSP ThemePicker.
 * Manages Nothing Glyph LED synchronization and audio haptics for theme transitions.
 */
public class HearthstoneService : Service() {

    companion object {
        private const val TAG = "HearthstoneService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Nothing Hearthstone Engine Initialized")
        initHearthstoneEngine()
    }

    private fun initHearthstoneEngine() {
        // Initialize Nothing Glyph light matrix controllers and sound effect triggers
    }

    override fun onDestroy() {
        Log.d(TAG, "Nothing Hearthstone Engine Destroyed")
        super.onDestroy()
    }
}
