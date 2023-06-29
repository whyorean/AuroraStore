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

package com.aurora.store.view.ui.preferences

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aurora.extensions.restartApp
import com.aurora.store.R
import com.aurora.store.databinding.ActivitySettingBinding
import com.aurora.store.view.ui.commons.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : BaseActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var B: ActivitySettingBinding

    companion object {
        var shouldRestart = false
        const val titleTag = "titleTag"
    }

    override fun onConnected() {
    }

    override fun onDisconnected() {
    }

    override fun onReconnected() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        B = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(B.root)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, MainPreference())
                .commit()
        } else {
            title = savedInstanceState.getCharSequence(titleTag)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                B.layoutToolbarAction.toolbar.setTitle(R.string.title_settings)
                if (shouldRestart)
                    askRestart()
            }
        }

        attachToolbar()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun attachToolbar() {
        setSupportActionBar(B.layoutToolbarAction.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.elevation = 0f
            actionBar.setTitle(R.string.title_settings)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(titleTag, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        preference: Preference
    ): Boolean {
        with(supportFragmentManager) {
            val args = preference.extras

            val fragment = fragmentFactory.instantiate(
                classLoader,
                preference.fragment.toString()
            ).apply {
                arguments = args
                setTargetFragment(caller, 0)
            }

            beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit()

            B.layoutToolbarAction.toolbar.title = preference.title
        }

        return true
    }

    private fun askRestart() {
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.action_restart))
            .setMessage(getString(R.string.pref_dialog_to_apply_restart))
            .setPositiveButton(getString(R.string.action_restart)) { _, _ ->
                shouldRestart = false
                restartApp()
            }
            .setNegativeButton(getString(R.string.action_later)) { dialog, _ -> dialog.dismiss() }
        builder.create()
        builder.show()
    }
}
