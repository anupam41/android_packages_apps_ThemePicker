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

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.clock.model.ClockStyleModel
import com.android.customization.picker.clock.ui.viewmodel.ClockPickerViewModel
import com.android.customization.picker.common.ui.view.SingleRowListItemSpacing
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.binder.SliderColorBinder
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter2
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnSliderTouchListener
import java.lang.ref.WeakReference
import kotlinx.coroutines.launch

/**
 * Binder for Nothing OS's distinct clock picking UI surface.
 *
 * Replaces the default AOSP multi-tab clock floating sheet layout with Nothing OS's clean
 * card styling while preserving the underlying AOSP [ClockPickerViewModel] and [ClockRegistry]
 * pipeline — ensuring all Pixel and SystemUI clock plugins are fully supported and rendered natively.
 */
object NTClockFloatingSheetBinder {

    fun bind(
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val viewModel = optionsViewModel.clockPickerViewModel
        val appContext = view.context.applicationContext
        val isFloatingSheetActive = { optionsViewModel.selectedOption.value == com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK }

        val recyclerView = view.requireViewById<RecyclerView>(R.id.nt_clock_style_list)
        val sliderContainer = view.requireViewById<View>(R.id.nt_clock_slider_container)
        val axisSlider = view.requireViewById<Slider>(R.id.nt_clock_axis_slider).also {
            SliderColorBinder.bind(
                slider = it,
                colorUpdateViewModel = colorUpdateViewModel,
                shouldAnimateColor = isFloatingSheetActive,
                lifecycleOwner = lifecycleOwner,
            )
        }

        axisSlider.setLabelFormatter { value ->
            val mappedValueInt = Math.round(value + 1.0f)
            mappedValueInt.toString()
        }

        ColorUpdateBinder.bind(
            setColor = { color ->
                view.requireViewById<TextView>(R.id.nt_clock_picker_title).setTextColor(color)
            },
            color = colorUpdateViewModel.colorOnSurface,
            shouldAnimate = isFloatingSheetActive,
            lifecycleOwner = lifecycleOwner,
        )

        ColorUpdateBinder.bind(
            setColor = { color ->
                view.requireViewById<TextView>(R.id.nt_clock_size_label).setTextColor(color)
            },
            color = colorUpdateViewModel.colorOnSurfaceVariant,
            shouldAnimate = isFloatingSheetActive,
            lifecycleOwner = lifecycleOwner,
        )

        val clockStyleAdapter = OptionItemAdapter2(
            layoutResourceId = R.layout.clock_style_option,
            lifecycleOwner = lifecycleOwner,
            bindPayload = { itemView: View, styleModel: ClockStyleModel ->
                itemView.findViewById<ImageView>(R.id.foreground)?.setImageDrawable(styleModel.thumbnail)
                return@OptionItemAdapter2 null
            },
            colorUpdateViewModel = WeakReference(colorUpdateViewModel),
            shouldAnimateColor = isFloatingSheetActive,
        )

        recyclerView.adapter = clockStyleAdapter
        recyclerView.layoutManager = LinearLayoutManager(appContext, LinearLayoutManager.HORIZONTAL, false)

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.clockStyleOptions.collect { styleOptions ->
                        clockStyleAdapter.setItems(styleOptions) {
                            val indexToFocus = styleOptions.indexOfFirst { it.isSelected.value }.coerceAtLeast(0)
                            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(indexToFocus, 0)
                        }
                    }
                }

                launch {
                    viewModel.shouldShowPresetSlider.collect { show ->
                        sliderContainer.visibility = if (show) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.axisPresetsSliderViewModel.collect { axisViewModel ->
                        val avm = axisViewModel ?: return@collect
                        axisSlider.valueFrom = avm.valueFrom
                        axisSlider.valueTo = avm.valueTo
                        axisSlider.stepSize = avm.stepSize
                        axisSlider.clearOnSliderTouchListeners()
                        axisSlider.addOnSliderTouchListener(
                            object : OnSliderTouchListener {
                                override fun onStartTrackingTouch(slider: Slider) {}
                                override fun onStopTrackingTouch(slider: Slider) {
                                    if (!optionsViewModel.isAccessibilityEnabled(slider.context)) {
                                        avm.onSliderStopTrackingTouch(slider.value)
                                    }
                                }
                            }
                        )
                        axisSlider.clearOnChangeListeners()
                        axisSlider.addOnChangeListener { slider, value, _ ->
                            if (optionsViewModel.isAccessibilityEnabled(slider.context)) {
                                avm.onSliderStopTrackingTouch(value)
                            }
                        }
                    }
                }

                launch {
                    viewModel.axisPresetsSliderSelectedValue.collect { value ->
                        axisSlider.value = value
                    }
                }
            }
        }
    }
}
