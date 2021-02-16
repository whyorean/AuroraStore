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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.aurora.Constants
import com.aurora.store.R
import com.aurora.store.databinding.ActivityOnboardingBinding
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT
import com.aurora.store.util.Preferences.PREFERENCE_DOWNLOAD_ACTIVE
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_FDROID
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_GOOGLE
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_THEME_ACCENT
import com.aurora.store.util.Preferences.PREFERENCE_THEME_TYPE
import com.aurora.store.util.extensions.open
import com.aurora.store.util.save
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.view.ui.splash.SplashActivity
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
            runOnUiThread { open(SplashActivity::class.java, true) }
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

        onNewIntent(intent)
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
                    B.btnBackward.isEnabled = position != 0
                    if (position == 3) {
                        B.btnForward.text = getString(R.string.action_finish)
                        B.btnForward.setOnClickListener {
                            save(PREFERENCE_INTRO, true)
                            open(SplashActivity::class.java, true)
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

    override fun onBackPressed() {
        if (B.viewpager2.currentItem == 0) {
            super.onBackPressed()
        } else {
            B.viewpager2.currentItem = B.viewpager2.currentItem - 1
        }
    }

    private fun loadDefaultPreferences() {
        /*Filters*/
        save(PREFERENCE_FILTER_FDROID, true)
        save(PREFERENCE_FILTER_GOOGLE, false)

        /*Downloader*/
        save(PREFERENCE_DOWNLOAD_ACTIVE, 3)

        /*Theme*/
        save(PREFERENCE_THEME_TYPE, 0)
        save(PREFERENCE_THEME_ACCENT, 0)

        /*Installer*/
        save(PREFERENCE_AUTO_DELETE, false)
        save(PREFERENCE_INSTALLER_ID, 0)
    }

    internal class PagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return WelcomeFragment()
                1 -> return InstallerFragment()
                2 -> return ThemeFragment()
                3 -> return AccentFragment()
            }
            return Fragment()
        }

        override fun getItemCount(): Int {
            return 4
        }
    }
}