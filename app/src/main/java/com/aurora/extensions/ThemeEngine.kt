/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.extensions

import android.app.UiModeManager
import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.Preferences

fun AppCompatActivity.applyThemeAccent() {
    val themeId = Preferences.getInteger(this, Preferences.PREFERENCE_THEME_TYPE)
    val accentId = Preferences.getInteger(this, Preferences.PREFERENCE_THEME_ACCENT)
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

    val themeStyle = CommonUtil.getThemeStyleById(themeId)
    val accentStyle = CommonUtil.getAccentStyleById(accentId)

    /*Apply Theme*/
    setTheme(themeStyle)
    theme.applyStyle(accentStyle, true)

    when (themeId) {
        0 -> {
            if (isSAndAbove()) {
                uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_CUSTOM)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }

        1 -> {
            setSystemBarConfiguration(light = true)
            if (isSAndAbove()) {
                uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        else -> {
            setSystemBarConfiguration(light = false)
            if (isSAndAbove()) {
                uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }
}

private fun AppCompatActivity.setSystemBarConfiguration(light: Boolean) {
    WindowInsetsControllerCompat(this.window, this.window.decorView.rootView).apply {
        // Status bar color
        if (isMAndAbove()) {
            isAppearanceLightStatusBars = light
        } else {
            window.statusBarColor = ColorUtils.setAlphaComponent(Color.BLACK, 120)
        }

        // Navigation bar color
        if (isOMR1AndAbove()) {
            isAppearanceLightNavigationBars = light
            window.navigationBarColor = getStyledAttributeColor(android.R.attr.colorBackground)
        }
    }
}
