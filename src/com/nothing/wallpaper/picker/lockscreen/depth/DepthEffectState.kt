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
package com.nothing.wallpaper.picker.lockscreen.depth

/**
 * Sealed hierarchy representing the state of Nothing OS's depth-of-field lock screen effect.
 *
 * Matches [com.nothing.wallpaper.picker.lockscreen.depth.DepthEffectState] and its subtypes
 * from the NothingThemePicker bytecode:
 *   - Initializing   – the ML subject segmenter is loading
 *   - DepthOn        – depth effect is active; [blurRadiusPx] controls background blur
 *   - DepthOff       – depth effect is inactive (user toggled off)
 *   - ModuleUnavailable    – device does not support the depth ML module
 *   - DepthNotApplicable   – current wallpaper cannot have depth applied (e.g. solid color)
 */
sealed class DepthEffectState {

    data object Initializing : DepthEffectState()

    data class DepthOn(
        val blurRadiusPx: Float,
        val subjectAlpha: Float = 1f,
    ) : DepthEffectState()

    data object DepthOff : DepthEffectState()

    data class ModuleUnavailable(val reason: NotApplicableReason) : DepthEffectState()

    data class DepthNotApplicable(val reason: NotApplicableReason) : DepthEffectState()
}
