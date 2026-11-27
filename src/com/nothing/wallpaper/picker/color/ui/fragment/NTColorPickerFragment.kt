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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Nothing OS color picker fragment. Extends AOSP's color picker system to surface
 * wallpaper-derived and preset color options using the existing ColorPickerViewModel,
 * matching the layout and tab structure observed in Nothing OS (wallpaper color tab +
 * preset color tab + color option icon grid).
 */
@AndroidEntryPoint
class NTColorPickerFragment : Fragment() {

    private val optionsViewModel: ThemePickerCustomizationOptionsViewModel by activityViewModels()
    private val colorUpdateViewModel: ColorUpdateViewModel by activityViewModels()

    private lateinit var colorPickerViewModel: ColorPickerViewModel
    private lateinit var colorOptionsRecyclerView: RecyclerView
    private lateinit var wallpaperColorTab: TextView
    private lateinit var presetColorTab: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        colorPickerViewModel = optionsViewModel.colorPickerViewModel2

        // Inflate the standard AOSP color picker floating sheet layout as our root.
        // Nothing OS uses the same underlying layout (color_picker_fragment) with additional
        // tab headers for wallpaper color vs preset color sections.
        val root = inflater.inflate(
            com.android.themepicker.R.layout.colors_floating_sheet,
            container,
            false,
        )
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        colorOptionsRecyclerView = view.requireViewById(
            com.android.themepicker.R.id.color_option_list,
        )
        wallpaperColorTab = view.requireViewById(
            com.android.themepicker.R.id.wallpaper_color_tab,
        )
        presetColorTab = view.requireViewById(
            com.android.themepicker.R.id.preset_color_tab,
        )

        colorOptionsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        bindTabSelection()
        bindColorOptions()
    }

    private fun bindTabSelection() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                colorPickerViewModel.colorTypeTabs.collect { tabs ->
                    tabs.forEach { tab ->
                        val tabView = if (tab.name ==
                            getString(com.android.themepicker.R.string.wallpaper_color_tab)
                        ) {
                            wallpaperColorTab
                        } else {
                            presetColorTab
                        }
                        tabView.isSelected = tab.isSelected
                        tabView.setOnClickListener { tab.onClick() }
                    }
                }
            }
        }
    }

    private fun bindColorOptions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                colorPickerViewModel.colorOptions.collect { options ->
                    colorOptionsRecyclerView.adapter = NTColorOptionAdapter(
                        options = options.map { it.payload },
                        onItemClick = { /* handled by OptionItemViewModel2.onClicked */ },
                        colorUpdateViewModel = colorUpdateViewModel,
                        lifecycleOwner = viewLifecycleOwner,
                    )
                }
            }
        }
    }

    /**
     * RecyclerView adapter that renders each color option using the AOSP ColorOptionIconView,
     * then delegates the icon binding (color swatch rendering + selection stroke update) to
     * the existing AOSP ColorOptionIconBinder.
     */
    private inner class NTColorOptionAdapter(
        private val options: List<ColorOptionIconViewModel>,
        private val onItemClick: (Int) -> Unit,
        private val colorUpdateViewModel: ColorUpdateViewModel,
        private val lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    ) : RecyclerView.Adapter<NTColorOptionAdapter.ViewHolder>() {

        inner class ViewHolder(val iconView: ColorOptionIconView) :
            RecyclerView.ViewHolder(iconView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val iconView = ColorOptionIconView(parent.context)
            return ViewHolder(iconView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val vm = options[position]
            ColorOptionIconBinder.bind(
                view = holder.iconView,
                viewModel = vm,
                colorUpdateViewModel = colorUpdateViewModel,
                shouldAnimateColor = { true },
                lifecycleOwner = lifecycleOwner,
            )
            holder.iconView.setOnClickListener { onItemClick(position) }
        }

        override fun getItemCount(): Int = options.size
    }
}
