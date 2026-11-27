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
 * Reason why the depth effect cannot be applied to this wallpaper or device.
 *
 * Matches [com.nothing.wallpaper.picker.lockscreen.depth.NotApplicableReason] from the bytecode.
 */
enum class NotApplicableReason {
    /** The ML subject segmenter module is not installed on this device. */
    MODULE_NOT_INSTALLED,
    /** The wallpaper image has no distinguishable foreground subject. */
    NO_SUBJECT_DETECTED,
    /** The detected subject is too transparent to separate from the background. */
    SUBJECT_TOO_TRANSPARENT,
    /** The input image could not be decoded. */
    INPUT_NULL,
}
