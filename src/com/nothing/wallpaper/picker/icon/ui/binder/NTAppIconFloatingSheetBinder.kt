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
package com.nothing.wallpaper.picker.icon.ui.binder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.icon.shared.model.IconStyleModel
import com.android.customization.picker.icon.ui.viewmodel.AppIconPickerViewModel
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter2
import java.lang.ref.WeakReference
import kotlinx.coroutines.launch

/**
 * Nothing OS App Icon & Monochrome Theme floating sheet binder.
 *
 * Surfaces Nothing Icon Pack and Monochrome icon themes in Nothing OS's clean card UI style
 * while wrapping AOSP's [AppIconPickerViewModel].
 */
object NTAppIconFloatingSheetBinder {

    fun bind(
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val viewModel = optionsViewModel.appIconPickerViewModel
        val isSheetActive = { optionsViewModel.selectedOption.value == com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.APP_ICONS }

        val recyclerView = view.requireViewById<RecyclerView>(R.id.shape_options)
        recyclerView.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)

        ColorUpdateBinder.bind(
            setColor = { color ->
                view.findViewById<TextView>(R.id.show_app_labels_title)?.setTextColor(color)
            },
            color = colorUpdateViewModel.colorOnSurface,
            shouldAnimate = isSheetActive,
            lifecycleOwner = lifecycleOwner,
        )

        val adapter = OptionItemAdapter2(
            layoutResourceId = R.layout.icon_style_option2,
            lifecycleOwner = lifecycleOwner,
            bindPayload = { itemView: View, styleModel: IconStyleModel ->
                itemView.findViewById<ImageView>(R.id.foreground)?.setImageDrawable(styleModel.thumbnail)
                return@OptionItemAdapter2 null
            },
            colorUpdateViewModel = WeakReference(colorUpdateViewModel),
            shouldAnimateColor = isSheetActive,
        )
        recyclerView.adapter = adapter

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.shapeOptions.collect { options ->
                        adapter.setItems(options) {
                            val focusIndex = options.indexOfFirst { it.isSelected.value }.coerceAtLeast(0)
                            (recyclerView.layoutManager as LinearLayoutManager).scrollToPosition(focusIndex)
                        }
                    }
                }
            }
        }
    }
}
