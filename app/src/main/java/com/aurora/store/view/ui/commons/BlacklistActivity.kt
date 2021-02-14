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

package com.aurora.store.view.ui.commons

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.aurora.store.R
import com.aurora.store.data.model.Black
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.store.util.extensions.close
import com.aurora.store.view.epoxy.views.BlackViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.all.BlacklistViewModel


class BlacklistActivity : BaseActivity() {

    private lateinit var B: ActivityGenericRecyclerBinding
    private lateinit var VM: BlacklistViewModel
    private lateinit var blacklistProvider: BlacklistProvider

    override fun onConnected() {

    }

    override fun onDisconnected() {

    }

    override fun onReconnected() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        B = ActivityGenericRecyclerBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this).get(BlacklistViewModel::class.java)
        blacklistProvider = BlacklistProvider.with(this)

        setContentView(B.root)

        VM.liveData.observe(this, {
            updateController(it.sortedByDescending { app ->
                blacklistProvider.isBlacklisted(app.packageName)
            })
        })

        attachToolbar()

        updateController(null)
    }

    override fun onDestroy() {
        blacklistProvider.save(VM.selected)
        super.onDestroy()
    }

    private fun attachToolbar() {
        B.layoutToolbarAction.txtTitle.text = getString(R.string.title_blacklist_manager)
        B.layoutToolbarAction.imgActionPrimary.setOnClickListener {
            close()
        }
    }

    private fun updateController(blackList: List<Black>?) {
        B.recycler.withModels {
            setFilterDuplicates(true)
            if (blackList == null) {
                for (i in 1..6) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                blackList.forEach {
                    add(
                        BlackViewModel_()
                            .id(it.packageName.hashCode())
                            .black(it)
                            .markChecked(VM.selected.contains(it.packageName))
                            .checked { _, isChecked ->
                                if (isChecked)
                                    VM.selected.add(it.packageName)
                                else
                                    VM.selected.remove(it.packageName)

                                requestModelBuild()
                            }
                    )
                }
            }
        }
    }
}