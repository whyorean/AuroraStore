package com.aurora.store.view.ui.dispenser

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.copyToClipBoard
import com.aurora.store.R
import com.aurora.store.databinding.FragmentDispenserBinding
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import com.aurora.store.view.epoxy.views.DispenserViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DispenserFragment : BaseFragment<FragmentDispenserBinding>(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val dispensers: Set<String>
        get() = Preferences.getStringSet(requireContext(), PREFERENCE_DISPENSER_URLS)

    private lateinit var sharedPreferences: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = Preferences.getPrefs(view.context)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        binding.addFab.setOnClickListener {
            findNavController().navigate(R.id.inputDispenserDialog)
        }

        setupDispensers()
    }

    override fun onDestroyView() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREFERENCE_DISPENSER_URLS) setupDispensers()
    }

    private fun setupDispensers() {
        if (dispensers.isNotEmpty()) {
            binding.noDispensersTextView.visibility = View.GONE

            binding.epoxyRecycler.visibility = View.VISIBLE
            binding.epoxyRecycler.withModels {
                setFilterDuplicates(true)
                dispensers.forEachIndexed { index, url ->
                    add(
                        DispenserViewModel_()
                            .id(index)
                            .url(url)
                            .copy { _ -> requireContext().copyToClipBoard(url) }
                            .clear { _ ->
                                findNavController().navigate(
                                    DispenserFragmentDirections
                                        .actionDispenserFragmentToRemoveDispenserDialog(url)
                                )
                            }
                    )
                }
            }
        } else {
            binding.epoxyRecycler.visibility = View.GONE
            binding.noDispensersTextView.visibility = View.VISIBLE
        }
    }
}
