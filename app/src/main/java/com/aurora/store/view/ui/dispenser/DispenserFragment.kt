package com.aurora.store.view.ui.dispenser

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.copyToClipBoard
import com.aurora.store.R
import com.aurora.store.databinding.FragmentDispenserBinding
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import com.aurora.store.view.epoxy.views.DispenserViewModel_
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DispenserFragment : Fragment(R.layout.fragment_dispenser),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var _binding: FragmentDispenserBinding? = null
    private val binding get() = _binding!!

    private val dispensers: Set<String>
        get() = Preferences.getStringSet(requireContext(), PREFERENCE_DISPENSER_URLS)

    private lateinit var sharedPreferences: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDispenserBinding.bind(view)

        sharedPreferences = Preferences.getPrefs(view.context)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        binding.addFab.setOnClickListener {
            findNavController().navigate(R.id.inputDispenserDialog)
        }

        setupDispensers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        _binding = null
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
