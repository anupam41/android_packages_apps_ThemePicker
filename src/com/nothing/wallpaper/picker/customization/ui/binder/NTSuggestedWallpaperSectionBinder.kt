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
package com.nothing.wallpaper.picker.customization.ui.binder

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.model.color.ColorOption
import com.android.customization.picker.color.ui.viewmodel.ColorPickerViewModel
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import kotlinx.coroutines.launch

/**
 * Binder for the Nothing OS "Suggested Wallpaper" section in the ThemePicker home screen.
 *
 * Nothing OS shows a horizontally-scrolling strip of suggested wallpapers derived from the
 * current color option above the full wallpaper grid. This binder:
 *   1. Observes the selected color option from ColorPickerViewModel to derive suggestions.
 *   2. Populates a horizontal RecyclerView with thumbnail + label rows.
 *   3. Applies dynamic color tinting to the section header via ColorUpdateBinder.
 *
 * The implementation mirrors the structure of ThemePickerSuggestedEntryBinder but for
 * wallpaper suggestions rather than theme pack suggestions.
 */
object NTSuggestedWallpaperSectionBinder {

    fun bind(
        sectionView: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        onSuggestedWallpaperClick: (ColorOption) -> Unit,
    ) {
        val suggestedRV = sectionView.requireViewById<RecyclerView>(
            com.android.themepicker.R.id.nt_suggested_wallpaper_rv,
        )
        val sectionTitle = sectionView.requireViewById<TextView>(
            com.android.themepicker.R.id.nt_suggested_wallpaper_title,
        )

        suggestedRV.layoutManager =
            LinearLayoutManager(sectionView.context, LinearLayoutManager.HORIZONTAL, false)

        // Tint the section header text with the current dynamic color.
        ColorUpdateBinder.bind(
            setColor = { color -> sectionTitle.setTextColor(color) },
            color = colorUpdateViewModel.colorOnSurface,
            shouldAnimate = { optionsViewModel.selectedOption.value == null },
            lifecycleOwner = lifecycleOwner,
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                optionsViewModel.colorPickerViewModel2.selectedColorOption.collect { selected ->
                    // When the color option changes, emit pseudo-suggestions derived from that
                    // color. In a full implementation these would come from a wallpaper content
                    // provider or server-side catalogue keyed by seed color.
                    val suggestions: List<ColorOption> = listOfNotNull(selected)
                    suggestedRV.adapter = SuggestedWallpaperAdapter(
                        suggestions,
                        onSuggestedWallpaperClick,
                    )
                }
            }
        }
    }

    private class SuggestedWallpaperAdapter(
        private val suggestions: List<ColorOption>,
        private val onClick: (ColorOption) -> Unit,
    ) : RecyclerView.Adapter<SuggestedWallpaperAdapter.VH>() {

        inner class VH(val root: ViewGroup) : RecyclerView.ViewHolder(root) {
            val thumbnail: ImageView = root.requireViewById(
                com.android.themepicker.R.id.nt_suggested_wallpaper_thumbnail,
            )
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = android.view.LayoutInflater.from(parent.context).inflate(
                com.android.themepicker.R.layout.nt_suggested_wallpaper_item,
                parent,
                false,
            ) as ViewGroup
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val option = suggestions[position]
            // Tint the thumbnail placeholder with the color option's light seed color
            // so it always shows a non-white, non-empty preview even without a real image.
            holder.thumbnail.setBackgroundColor(option.seedColor)
            holder.root.setOnClickListener { onClick(option) }
        }

        override fun getItemCount(): Int = suggestions.size
    }
}
