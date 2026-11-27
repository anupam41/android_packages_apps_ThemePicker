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
 * An AI-generated wallpaper returned by the Nothing creative wallpaper backend.
 *
 * Matches [com.nothing.wallpaper.picker.creative.model.Wallpaper] from the bytecode.
 * [promptId] links back to the [Prompt] that was used to generate it.
 * [imageUrl] is the CDN URL of the full-resolution result.
 * [thumbnailUrl] is a smaller preview shown in the history grid.
 * [generatedAt] is the epoch-millis timestamp of generation.
 */
@Parcelize
data class Wallpaper(
    val id: String,
    val promptId: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val generatedAt: Long,
) : Parcelable
