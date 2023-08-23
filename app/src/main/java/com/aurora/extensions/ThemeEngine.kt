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

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.aurora.store.util.CommonUtil

fun Fragment.applyTheme(themeId: Int) {
    val themeStyle = CommonUtil.getThemeStyleById(themeId)

    if (themeId == 0) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        (requireActivity() as AppCompatActivity).applyDayNightMask()
    } else {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    /*Apply Theme*/
    requireContext().theme.applyStyle(themeStyle, true)
    (requireActivity() as AppCompatActivity).recreate()

    if (themeId == 1) {
        (requireActivity() as AppCompatActivity).setLightConfiguration()
    }
}

fun AppCompatActivity.applyTheme(themeId: Int, accentId: Int = 1) {
    val themeStyle = CommonUtil.getThemeStyleById(themeId)
    val accentStyle = CommonUtil.getAccentStyleById(accentId)

    if (themeId == 0) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        applyDayNightMask()
    } else {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    /*Apply Theme*/
    setTheme(themeStyle)

    /*Apply Accent*/
    theme.applyStyle(accentStyle, true)

    /*Apply Light Configuration*/
    if (themeId == 1) {
        setLightConfiguration()
    }
}

fun AppCompatActivity.applyDayNightMask() {
    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
        setLightConfiguration()
    }
}

fun AppCompatActivity.setLightConfiguration() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        setLightConfigurationO()
    } else {
        setLightConfigurationO()
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun AppCompatActivity.setLightConfigurationR() {
    window?.insetsController?.setSystemBarsAppearance(
        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
    )
}

private fun AppCompatActivity.setLightConfigurationO() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        setLightStatusBar()
        setLightNavigationBar()
    } else {
        window.statusBarColor = ColorUtils.setAlphaComponent(Color.BLACK, 120)
    }
}

private fun AppCompatActivity.setLightStatusBar() {
    var flags = window.decorView.systemUiVisibility
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
    window.decorView.systemUiVisibility = flags
}

private fun AppCompatActivity.setLightNavigationBar() {
    var flags = window.decorView.systemUiVisibility
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        window.navigationBarColor = getStyledAttributeColor(android.R.attr.colorBackground)
        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }
    window.decorView.systemUiVisibility = flags
}
