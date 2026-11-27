package com.android.customization.picker.icon.ui.binder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.android.themepicker.R

class AppIconPickerBinder {
    companion object {
        const val NOTHING_ICON_PACK_PACKAGE = "com.nothing.hearthstone"

        fun isNothingIconPackInstalled(context: Context): Boolean {
            return try {
                context.packageManager.getPackageInfo(NOTHING_ICON_PACK_PACKAGE, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        fun applyNothingIconPack(context: Context) {
            val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("active_icon_pack", NOTHING_ICON_PACK_PACKAGE).apply()
        }
    }
}
