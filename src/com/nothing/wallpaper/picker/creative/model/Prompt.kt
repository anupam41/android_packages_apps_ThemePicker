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
package com.nothing.wallpaper.picker.creative.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A prompt the user can send to the Nothing AI creative wallpaper generator.
 *
 * Matches the class [com.nothing.wallpaper.picker.creative.model.Prompt] identified in the
 * NothingThemePicker bytecode. Prompts belong to a [PromptCategory] and carry the user-visible
 * label plus the opaque server-side ID used in the network request.
 */
@Parcelize
data class Prompt(
    val id: String,
    val label: String,
    val categoryId: String,
    val previewImageUrl: String?,
) : Parcelable
