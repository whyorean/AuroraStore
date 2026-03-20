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

package com.aurora.store.view.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.aurora.store.data.providers.PermissionProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.lang.reflect.ParameterizedType

abstract class BaseDialogSheet<ViewBindingType : ViewBinding> : BottomSheetDialogFragment() {

    private var _binding: ViewBindingType? = null
    protected val binding get() = _binding!!

    lateinit var permissionProvider: PermissionProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionProvider = PermissionProvider(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val type = (javaClass.genericSuperclass as ParameterizedType)
            .actualTypeArguments[0] as Class<ViewBindingType>

        val method = type.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )

        _binding = method.invoke(null, inflater, container, false) as ViewBindingType

        return binding.root
    }

    override fun onDestroyView() {
        permissionProvider.unregister()
        _binding = null
        super.onDestroyView()
    }
}
