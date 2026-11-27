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
package com.nothing.wallpaper.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting AI-generated creative wallpapers locally.
 *
 * Matches [com.nothing.wallpaper.db.CreativeWallpaper] from the NothingThemePicker bytecode.
 * Nothing OS saves generated wallpapers in a local Room database so they appear in the
 * history grid even when offline.
 */
@Entity(tableName = "creative_wallpapers")
data class CreativeWallpaper(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "prompt_id") val promptId: String,
    @ColumnInfo(name = "image_url") val imageUrl: String,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
    @ColumnInfo(name = "is_applied") val isApplied: Boolean = false,
)
