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

package com.aurora.store.view.ui.details

import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.aurora.Constants
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.databinding.ActivityScreenshotBinding
import com.aurora.store.util.extensions.close
import com.aurora.store.view.epoxy.views.details.LargeScreenshotViewModel_
import com.aurora.store.view.ui.commons.BaseActivity
import com.google.gson.reflect.TypeToken

class ScreenshotActivity : BaseActivity() {

    private lateinit var B: ActivityScreenshotBinding
    private lateinit var artworks: MutableList<Artwork>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewUtil.configureActivityLayout(this, false)

        B = ActivityScreenshotBinding.inflate(layoutInflater)
        setContentView(B.root)

        attachToolbar()
        attachRecycler()

        if (intent != null) {
            val rawArtWorks = intent.getStringExtra(Constants.STRING_EXTRA).toString()
            val position = intent.getIntExtra(Constants.INT_EXTRA, 0)
            artworks = gson.fromJson(rawArtWorks, object : TypeToken<List<Artwork?>?>() {}.type)
            updateController(artworks, position)
        } else {
            close()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun attachToolbar() {
        setSupportActionBar(B.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.elevation = 0f
            actionBar.title = ""
        }
    }

    private fun attachRecycler() {
        B.recyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@ScreenshotActivity,
                RecyclerView.HORIZONTAL,
                false
            )
            PagerSnapHelper().attachToRecyclerView(this)
        }
    }

    private fun updateController(artworks: MutableList<Artwork>, position: Int) {
        B.recyclerView.withModels {
            artworks.forEach {
                add(
                    LargeScreenshotViewModel_()
                        .id(it.url)
                        .artwork(it)
                )
            }
            B.recyclerView.scrollToPosition(position)
        }
    }

    override fun onConnected() {

    }

    override fun onDisconnected() {
    }

    override fun onReconnected() {
    }
}