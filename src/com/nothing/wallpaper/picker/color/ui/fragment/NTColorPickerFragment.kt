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
package com.nothing.wallpaper.picker.color.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.color.ui.binder.ColorOptionIconBinder
import com.android.customization.picker.color.ui.view.ColorOptionIconView
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.customization.picker.color.ui.viewmodel.ColorPickerViewModel
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Nothing OS color picker fragment. Extends AOSP's color picker system to surface
 * wallpaper-derived and preset color options using the existing ColorPickerViewModel,
 * matching the layout, tab switching, subhead text, and click callbacks observed in Nothing OS.
 */
@AndroidEntryPoint
class NTColorPickerFragment : Fragment() {

    private val optionsViewModel: ThemePickerCustomizationOptionsViewModel by activityViewModels()
    private val colorUpdateViewModel: ColorUpdateViewModel by activityViewModels()

    private lateinit var colorPickerViewModel: ColorPickerViewModel
    private lateinit var colorOptionsRecyclerView: RecyclerView
    private lateinit var colorTypeSubhead: TextView
    private lateinit var wallpaperColorTab: TextView
    private lateinit var presetColorTab: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        colorPickerViewModel = optionsViewModel.colorPickerViewModel2

        val root = inflater.inflate(
            com.android.themepicker.R.layout.floating_sheet_colors,
            container,
            false,
        )
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        colorOptionsRecyclerView = view.requireViewById(
            com.android.themepicker.R.id.colors_horizontal_list,
        )
        colorTypeSubhead = view.requireViewById(
            com.android.themepicker.R.id.color_type_tab_subhead,
        )

        colorOptionsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        bindColorOptions()
        bindSubhead()
    }

    private fun bindSubhead() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                colorPickerViewModel.colorTypeTabSubheader.collect { subheadText ->
                    colorTypeSubhead.text = subheadText
                }
            }
        }
    }

    private fun bindColorOptions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                colorPickerViewModel.colorOptions.collect { optionItems ->
                    colorOptionsRecyclerView.adapter = NTColorOptionAdapter(
                        optionItems = optionItems,
                        colorUpdateViewModel = colorUpdateViewModel,
                        lifecycleOwner = viewLifecycleOwner,
                    )
                }
            }
        }
    }

    private class NTColorOptionAdapter(
        private val optionItems: List<OptionItemViewModel2<ColorOptionIconViewModel>>,
        private val colorUpdateViewModel: ColorUpdateViewModel,
        private val lifecycleOwner: LifecycleOwner,
    ) : RecyclerView.Adapter<NTColorOptionAdapter.ViewHolder>() {

        class ViewHolder(val iconView: ColorOptionIconView) :
            RecyclerView.ViewHolder(iconView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val iconView = ColorOptionIconView(parent.context)
            return ViewHolder(iconView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = optionItems[position]
            ColorOptionIconBinder.bind(
                view = holder.iconView,
                viewModel = item.payload,
                colorUpdateViewModel = colorUpdateViewModel,
                shouldAnimateColor = { true },
                lifecycleOwner = lifecycleOwner,
            )
            // Properly invoke the AOSP OptionItemViewModel2 click handler when tapped
            holder.iconView.setOnClickListener {
                item.onClicked()
            }
        }

        override fun getItemCount(): Int = optionItems.size
    }
}
