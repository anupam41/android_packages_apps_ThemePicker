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
package com.nothing.wallpaper.picker.clock.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents Nothing OS's iconic Dot Matrix clock style configuration.
 *
 * Nothing OS features a proprietary dot-matrix typographic clock on the lock screen
 * with customizable weight, spacing, and glowing accent dots.
 */
@Parcelize
data class NTClockStyle(
    val id: String = "nothing_dot_matrix",
    val title: String = "Nothing Dot Matrix",
    val isBold: Boolean = true,
    val showSeconds: Boolean = false,
    val glowIntensity: Float = 0.8f,
) : Parcelable {
    companion object {
        const val CLOCK_ID = "nothing_dot_matrix"
    }
}
