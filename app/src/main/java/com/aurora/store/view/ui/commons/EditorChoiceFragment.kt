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
import android.view.View
import androidx.fragment.app.viewModels
import com.aurora.Constants
import com.aurora.gplayapi.data.models.editor.EditorChoiceCluster
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.store.R
import com.aurora.store.databinding.FragmentForYouBinding
import com.aurora.store.view.epoxy.controller.EditorChoiceController
import com.aurora.store.viewmodel.editorschoice.EditorChoiceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditorChoiceFragment : BaseFragment(R.layout.fragment_for_you),
    EditorChoiceController.Callbacks {

    private var _binding: FragmentForYouBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditorChoiceViewModel by viewModels()

    companion object {
        @JvmStatic
        fun newInstance(pageType: Int): EditorChoiceFragment {
            return EditorChoiceFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.PAGE_TYPE, pageType)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentForYouBinding.bind(view)

        var pageType = 0
        val bundle = arguments
        if (bundle != null) {
            pageType = bundle.getInt(Constants.PAGE_TYPE, 0)
        }

        val editorChoiceController = EditorChoiceController(this)
        binding.recycler.setController(editorChoiceController)

        when (pageType) {
            0 -> viewModel.getEditorChoiceStream(StreamContract.Category.APPLICATION)
            1 -> viewModel.getEditorChoiceStream(StreamContract.Category.GAME)
        }

        viewModel.liveData.observe(viewLifecycleOwner) {
            editorChoiceController.setData(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onClick(editorChoiceCluster: EditorChoiceCluster) {
        openEditorStreamBrowseFragment(
            editorChoiceCluster.clusterBrowseUrl,
            editorChoiceCluster.clusterTitle
        )
    }
}
