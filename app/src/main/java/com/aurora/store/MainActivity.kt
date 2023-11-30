/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *  Copyright (C) 2022, The Calyx Institute
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

package com.aurora.store

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED
import androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED
import androidx.lifecycle.lifecycleScope
import androidx.navigation.FloatingWindow
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import coil.load
import coil.transform.RoundedCornersTransformation
import com.aurora.Constants
import com.aurora.extensions.accentColor
import com.aurora.extensions.applyThemeAccent
import com.aurora.extensions.isMAndAbove
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.toast
import com.aurora.store.data.model.NetworkStatus
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.data.providers.NetworkProvider
import com.aurora.store.databinding.ActivityMainBinding
import com.aurora.store.util.Log
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.util.Preferences.PREFERENCE_SELF_UPDATE
import com.aurora.store.view.ui.sheets.NetworkDialogSheet
import com.aurora.store.view.ui.sheets.SelfUpdateSheet
import com.aurora.store.viewmodel.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var B: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appConfig: AppBarConfiguration

    private val viewModel: MainViewModel by viewModels()

    private val startForPermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (!it) toast(R.string.permissions_denied)
        }

    // TopLevelFragments
    private val topLevelFrags = listOf(
        R.id.appsContainerFragment,
        R.id.gamesContainerFragment,
        R.id.updatesFragment
    )

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeAccent()
        super.onCreate(savedInstanceState)

        B = ActivityMainBinding.inflate(layoutInflater)
        setContentView(B.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        this.lifecycleScope.launch {
            NetworkProvider(applicationContext).networkStatus.collect {
                when (it) {
                    NetworkStatus.AVAILABLE -> {
                        if (!supportFragmentManager.isDestroyed && isIntroDone()) {
                            val fragment = supportFragmentManager
                                .findFragmentByTag(NetworkDialogSheet.TAG)
                            fragment?.let {
                                supportFragmentManager.beginTransaction()
                                    .remove(fragment)
                                    .commitAllowingStateLoss()
                            }
                        }
                    }

                    NetworkStatus.LOST -> {
                        if (!supportFragmentManager.isDestroyed && isIntroDone()) {
                            supportFragmentManager.beginTransaction()
                                .add(NetworkDialogSheet.newInstance(), NetworkDialogSheet.TAG)
                                .commitAllowingStateLoss()
                        }
                    }
                }
            }
        }

        // Toolbar
        setSupportActionBar(B.toolbar)

        attachNavigation()
        attachDrawer()
        attachSearch()

        /*Check only if download to external storage is enabled*/
        if (Preferences.getBoolean(this, Preferences.PREFERENCE_DOWNLOAD_EXTERNAL)) {
            if (isRAndAbove()) {
                checkExternalStorageManagerPermission()
            } else {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startForPermissions.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        /* Check self update only for stable release, skip debug & nightlies*/
        if (Preferences.getBoolean(this, PREFERENCE_SELF_UPDATE)) {
            if (BuildConfig.APPLICATION_ID == Constants.APP_ID) viewModel.checkSelfUpdate(this)
            this.lifecycleScope.launch {
                viewModel.selfUpdateAvailable.collect {
                    if (it != null) {
                        showUpdatesSheet(it)
                    } else {
                        Log.i("No Aurora Store update available")
                    }
                }
            }
        }

        // Handle quick exit from back actions
        val defaultTab = when (Preferences.getInteger(this, PREFERENCE_DEFAULT_SELECTED_TAB)) {
            1 -> R.id.gamesContainerFragment
            2 -> R.id.updatesFragment
            else -> R.id.appsContainerFragment
        }
        onBackPressedDispatcher.addCallback(this) {
            if (!B.drawerLayout.isOpen) {
                if (navController.currentDestination?.id in topLevelFrags) {
                    if (navController.currentDestination?.id == defaultTab) {
                        finish()
                    } else {
                        navController.navigate(defaultTab)
                    }
                } else if (navHostFragment.childFragmentManager.backStackEntryCount == 0) {
                    // We are on either on onboarding or splash fragment
                    finish()
                } else {
                    navController.navigateUp()
                }
            } else {
                B.drawerLayout.close()
            }
        }

        // Handle intents
        when (intent?.action) {
            Constants.NAVIGATION_UPDATES -> B.navView.selectedItemId = R.id.updatesFragment
            else -> Log.i("Unhandled intent action: ${intent.action}")
        }

        // Handle views on fragments
        navController.addOnDestinationChangedListener { _, navDestination, _ ->
            if (navDestination !is FloatingWindow) {
                when (navDestination.id) {
                    in topLevelFrags -> {
                        B.searchFab.visibility = View.VISIBLE
                        B.navView.visibility = View.VISIBLE
                        B.toolbar.visibility = View.VISIBLE
                        B.drawerLayout.setDrawerLockMode(LOCK_MODE_UNLOCKED)
                    }

                    else -> {
                        hideTopLevelOnlyViews()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Show warning if battery optimizations is enabled
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (isMAndAbove() && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
            menu.findItem(R.id.menu_doze_info)?.isVisible = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_download_manager -> {
                navController.navigate(R.id.downloadFragment)
                return true
            }
            R.id.menu_doze_info -> {
                navController.navigate(R.id.dozeWarningSheet)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun hideTopLevelOnlyViews() {
        B.searchFab.visibility = View.GONE
        B.navView.visibility = View.GONE
        B.toolbar.visibility = View.GONE
        B.drawerLayout.setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED)
    }

    private fun attachSearch() {
        B.searchFab.setOnClickListener {
            navController.navigate(R.id.searchSuggestionFragment)
        }
    }

    private fun attachNavigation() {
        val bottomNavigationView: BottomNavigationView = B.navView
        bottomNavigationView.setupWithNavController(navController)

        bottomNavigationView.apply {
            val alphaColor = ColorUtils.setAlphaComponent(this@MainActivity.accentColor(), 100)
            itemActiveIndicatorColor = ColorStateList.valueOf(alphaColor)
        }
    }

    private fun attachDrawer() {
        val headerView: View = B.navigation.getHeaderView(0)

        headerView.let {
            it.findViewById<ImageView>(R.id.img)?.load(R.mipmap.ic_launcher) {
                transformations(RoundedCornersTransformation(8F))
            }
            it.findViewById<TextView>(R.id.txt_name)?.text = getString(R.string.app_name)
            it.findViewById<TextView>(R.id.txt_email)?.text =
                ("v${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}")
        }

        appConfig = AppBarConfiguration.Builder(topLevelFrags.toSet())
            .setOpenableLayout(B.root)
            .build()
        setupActionBarWithNavController(navController, appConfig)
        B.navigation.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appConfig)
    }

    private fun checkExternalStorageManagerPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager())
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

    private fun showUpdatesSheet(selfUpdate: SelfUpdate) {
        if (!supportFragmentManager.isDestroyed) {
            val sheet = SelfUpdateSheet.newInstance(selfUpdate)
            sheet.isCancelable = false
            sheet.show(supportFragmentManager, SelfUpdateSheet.TAG)
        }
    }

    private fun isIntroDone(): Boolean {
        return Preferences.getBoolean(this@MainActivity, Preferences.PREFERENCE_INTRO)
    }
}
