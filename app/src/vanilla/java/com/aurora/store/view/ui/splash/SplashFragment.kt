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

package com.aurora.store.view.ui.splash

import android.accounts.AccountManager
import android.util.Log
import androidx.navigation.fragment.findNavController
import com.aurora.store.R
import com.aurora.store.data.model.AuthState
import com.aurora.store.util.CertUtil.GOOGLE_ACCOUNT_TYPE
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : BaseFlavouredSplashFragment() {
    private val TAG = SplashFragment::class.java.simpleName

    override fun attachActions() {
        binding.btnAnonymous.addOnClickListener {
            if (viewModel.authState.value != AuthState.Fetching) {
                binding.btnAnonymous.updateProgress(true)
                viewModel.buildAnonymousAuthData()
            }
        }

        binding.btnGoogle.addOnClickListener {
            if (viewModel.authState.value != AuthState.Fetching) {
                binding.btnGoogle.updateProgress(true)
                if (canLoginWithMicroG) {
                    Log.i(TAG, "Found supported microG, trying to request credentials")
                    val accountIntent = AccountManager.newChooseAccountIntent(
                        null,
                        null,
                        arrayOf(GOOGLE_ACCOUNT_TYPE),
                        null,
                        null,
                        null,
                        null
                    )
                    startForAccount.launch(accountIntent)
                } else {
                    findNavController().navigate(R.id.googleFragment)
                }
            }
        }
    }

    override fun resetActions() {
        binding.btnGoogle.apply {
            updateProgress(false)
            isEnabled = true
        }

        binding.btnAnonymous.apply {
            updateProgress(false)
            isEnabled = true
        }
    }
}
