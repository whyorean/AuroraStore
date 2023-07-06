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

package com.aurora.store.view.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.aurora.Constants
import com.aurora.extensions.isSAndAbove
import com.aurora.extensions.open
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.data.work.UpdateWorker
import com.aurora.store.databinding.ActivityOnboardingBinding
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.util.Preferences.PREFERENCE_DOWNLOAD_ACTIVE
import com.aurora.store.util.Preferences.PREFERENCE_DOWNLOAD_EXTERNAL
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_FDROID
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_GOOGLE
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_SEARCH
import com.aurora.store.util.Preferences.PREFERENCE_FOR_YOU
import com.aurora.store.util.Preferences.PREFERENCE_INSECURE_ANONYMOUS
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_SIMILAR
import com.aurora.store.util.Preferences.PREFERENCE_THEME_ACCENT
import com.aurora.store.util.Preferences.PREFERENCE_THEME_TYPE
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_EXTENDED
import com.aurora.store.util.save
import com.aurora.store.view.ui.commons.BaseActivity
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : BaseActivity() {

    lateinit var B: ActivityOnboardingBinding

    override fun onConnected() {}

    override fun onDisconnected() {}

    override fun onReconnected() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isIntroDone = Preferences.getBoolean(this, PREFERENCE_INTRO)
        if (isIntroDone) {
            runOnUiThread { open(MainActivity::class.java, true) }
            return
        }

        val isDefaultPrefLoaded = Preferences.getBoolean(this, PREFERENCE_DEFAULT)
        if (!isDefaultPrefLoaded) {
            save(PREFERENCE_DEFAULT, true)
            loadDefaultPreferences()
        }

        B = ActivityOnboardingBinding.inflate(layoutInflater)

        setContentView(B.root)

        attachViewPager()

        B.btnForward.setOnClickListener {
            moveForward()
        }

        B.btnBackward.setOnClickListener {
            moveBackward()
        }

        if (!Preferences.getBoolean(this, Preferences.PREFERENCE_TOS_READ)) {
            askToReadTOS()
        }

        onNewIntent(intent)

        onBackPressedDispatcher.addCallback(this) {
            if (B.viewpager2.currentItem == 0) {
                finish()
            } else {
                B.viewpager2.currentItem = B.viewpager2.currentItem - 1
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            val pos = intent.getIntExtra(Constants.INT_EXTRA, 0)
            B.viewpager2.setCurrentItem(pos, false)
        }
    }

    private fun attachViewPager() {
        B.viewpager2.adapter = PagerAdapter(this)
        B.viewpager2.isUserInputEnabled = false
        B.viewpager2.setCurrentItem(0, true)
        B.viewpager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                runOnUiThread {
                    lastPosition = position
                    refreshButtonState()
                }
            }
        })

        TabLayoutMediator(B.tabLayout, B.viewpager2, true) { tab, position ->
            tab.text = (position + 1).toString()
        }.attach()
    }

    private fun moveForward() {
        B.viewpager2.setCurrentItem(B.viewpager2.currentItem + 1, true)
    }

    private fun moveBackward() {
        B.viewpager2.setCurrentItem(B.viewpager2.currentItem - 1, true)
    }

    var lastPosition = 0

    fun refreshButtonState() {
        B.btnBackward.isEnabled = lastPosition != 0
        B.btnForward.isEnabled = lastPosition != 4
        if (lastPosition == 4) {
            B.btnForward.text = getString(R.string.action_finish)
            B.btnForward.isEnabled = true
            B.btnForward.setOnClickListener {
                save(PREFERENCE_INTRO, true)
                UpdateWorker.scheduleAutomatedCheck(this)
                open(MainActivity::class.java, true)
            }
        } else {
            B.btnForward.text = getString(R.string.action_next)
            B.btnForward.setOnClickListener {
                B.viewpager2.setCurrentItem(
                    B.viewpager2.currentItem + 1, true
                )
            }
        }
    }

    private fun loadDefaultPreferences() {
        /*Filters*/
        save(PREFERENCE_FILTER_FDROID, true)
        save(PREFERENCE_FILTER_GOOGLE, false)
        save(PREFERENCE_FILTER_SEARCH, true)

        /*Downloader*/
        save(PREFERENCE_DOWNLOAD_ACTIVE, 3)
        save(PREFERENCE_DOWNLOAD_EXTERNAL, false)

        /*Network*/
        save(PREFERENCE_INSECURE_ANONYMOUS, false)

        /*Customization*/
        save(PREFERENCE_THEME_TYPE, 0)
        save(PREFERENCE_THEME_ACCENT, if (isSAndAbove()) 0 else 1)
        save(PREFERENCE_DEFAULT_SELECTED_TAB, 0)
        save(PREFERENCE_FOR_YOU, true)
        save(PREFERENCE_SIMILAR, true)

        /*Installer*/
        save(PREFERENCE_AUTO_DELETE, true)
        save(PREFERENCE_INSTALLER_ID, 0)

        /*Updates*/
        save(PREFERENCE_UPDATES_EXTENDED, false)
        save(PREFERENCE_UPDATES_CHECK, true)
    }

    internal class PagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return WelcomeFragment()
                1 -> return InstallerFragment()
                2 -> return ThemeFragment()
                3 -> return AccentFragment()
                4 -> return PermissionsFragment()
            }
            return Fragment()
        }

        override fun getItemCount(): Int {
            return 5
        }
    }
}
