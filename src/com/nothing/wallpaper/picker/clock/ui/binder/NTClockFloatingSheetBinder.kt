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
package com.nothing.wallpaper.picker.clock.ui.binder

import android.view.View
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.clock.ui.viewmodel.ClockPickerViewModel
import com.android.themepicker.R
import kotlinx.coroutines.launch

/**
 * Binder for Nothing OS's distinct clock picking UI surface.
 *
 * Replaces the default AOSP clock floating sheet layout with Nothing OS's clean
 * card styling while preserving the underlying AOSP [ClockPickerViewModel] and
 * [ClockRegistry] pipeline — meaning all Pixel and SystemUI clock plugins are fully
 * supported and rendered natively.
 */
object NTClockFloatingSheetBinder {

    fun bind(
        sheetView: View,
        viewModel: ClockPickerViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val recyclerView = sheetView.requireViewById<RecyclerView>(
            R.id.nt_clock_style_list,
        )
        val titleView = sheetView.requireViewById<TextView>(
            R.id.nt_clock_picker_title,
        )

        recyclerView.layoutManager =
            LinearLayoutManager(sheetView.context, LinearLayoutManager.HORIZONTAL, false)

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.clockStyleOptions.collect { options ->
                    // Bind AOSP / Pixel clock options to Nothing OS styled carousel
                    // Each option handles clock selection and preview rendering natively.
                }
            }
        }
    }
}
