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

import android.app.ActivityOptions
import android.content.Intent
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aurora.Constants
import com.aurora.extensions.getEmptyActivityBundle
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Category
import com.aurora.store.MobileNavigationDirections
import com.aurora.store.view.ui.details.DevProfileActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.lang.reflect.Modifier

open class BaseFragment : Fragment {

    constructor(): super()

    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    var gson: Gson = GsonBuilder().excludeFieldsWithModifiers(
        Modifier.TRANSIENT
    ).create()

    fun openDetailsActivity(app: App) {
        findNavController().navigate(
            MobileNavigationDirections.actionGlobalAppDetailsFragment(app.packageName)
        )
    }

    fun openCategoryBrowseActivity(category: Category) {
        val intent = Intent(context, CategoryBrowseActivity::class.java)
        intent.putExtra(Constants.STRING_EXTRA, category.title)
        intent.putExtra(Constants.BROWSE_EXTRA, category.browseUrl)
        val options = ActivityOptions.makeSceneTransitionAnimation(requireActivity())
        startActivity(intent, options.toBundle())
    }

    fun openStreamBrowseActivity(browseUrl: String, title: String = "") {
        val intent = if (browseUrl.lowercase().contains("expanded"))
            Intent(requireContext(), ExpandedStreamBrowseActivity::class.java)
        else if (browseUrl.lowercase().contains("developer"))
            Intent(requireContext(), DevProfileActivity::class.java)
        else
            Intent(requireContext(), StreamBrowseActivity::class.java)

        intent.putExtra(Constants.BROWSE_EXTRA, browseUrl)
        intent.putExtra(Constants.STRING_EXTRA, title)
        startActivity(intent, requireContext().getEmptyActivityBundle())
    }

    fun openEditorStreamBrowseActivity(browseUrl: String, title: String = "") {
        val intent = Intent(requireContext(), EditorStreamBrowseActivity::class.java)
        intent.putExtra(Constants.BROWSE_EXTRA, browseUrl)
        intent.putExtra(Constants.STRING_EXTRA, title)
        startActivity(intent, requireContext().getEmptyActivityBundle())
    }
}
