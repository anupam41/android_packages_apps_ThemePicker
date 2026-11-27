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
package com.nothing.wallpaper.picker.quickaffordance.ui.binder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter2
import java.lang.ref.WeakReference
import kotlinx.coroutines.launch

/**
 * Nothing OS Lock Screen Widgets and Shortcuts floating sheet binder.
 *
 * Surfaces lock screen quick affordances / widgets in Nothing OS's card-styled UI layout
 * wrapping AOSP's [KeyguardQuickAffordancePickerViewModel2].
 */
object NTShortcutFloatingSheetBinder {

    fun bind(
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val viewModel = optionsViewModel.keyguardQuickAffordancePickerViewModel2
        val isSheetActive = { optionsViewModel.selectedOption.value == com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.SHORTCUTS }

        val recyclerView = view.requireViewById<RecyclerView>(R.id.quick_affordance_horizontal_list)
        recyclerView.layoutManager = GridLayoutManager(view.context, 2, GridLayoutManager.HORIZONTAL, false)

        val adapter = OptionItemAdapter2(
            layoutResourceId = R.layout.quick_affordance_list_item2,
            lifecycleOwner = lifecycleOwner,
            bindPayload = { itemView: View, icon: Icon ->
                val imageView = itemView.requireViewById<ImageView>(com.android.wallpaper.R.id.foreground)
                IconViewBinder.bind(imageView, icon)
                return@OptionItemAdapter2 null
            },
            colorUpdateViewModel = WeakReference(colorUpdateViewModel),
            shouldAnimateColor = isSheetActive,
        )
        recyclerView.adapter = adapter

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.quickAffordances.collect { affordances ->
                        adapter.setItems(affordances)
                    }
                }
            }
        }
    }
}
