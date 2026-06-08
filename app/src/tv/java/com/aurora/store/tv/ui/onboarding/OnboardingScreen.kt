/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.aurora.store.R
import com.aurora.store.compose.composable.PermissionList
import com.aurora.store.tv.viewmodel.OnboardingViewModel
import com.aurora.store.viewmodel.commons.PermissionRationaleViewModel
import kotlinx.coroutines.android.awaitFrame

private enum class OnboardingStep { WELCOME, PERMISSIONS }

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
    permissionViewModel: PermissionRationaleViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(OnboardingStep.WELCOME) }
    val focusRequester = remember { FocusRequester() }

    // Focus the primary action whenever the step changes so the remote always has a starting point.
    LaunchedEffect(step) {
        awaitFrame()
        runCatching { focusRequester.requestFocus() }
    }

    when (step) {
        OnboardingStep.WELCOME -> WelcomeStep(
            onNext = { step = OnboardingStep.PERMISSIONS },
            primaryFocusRequester = focusRequester
        )

        OnboardingStep.PERMISSIONS -> PermissionsStep(
            permissionViewModel = permissionViewModel,
            onFinish = {
                viewModel.finishOnboarding()
                onFinished()
            },
            primaryFocusRequester = focusRequester
        )
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit, primaryFocusRequester: FocusRequester) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.tv_screen_padding)),
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.spacing_large),
            Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onNext,
            modifier = Modifier.focusRequester(primaryFocusRequester)
        ) {
            Text(stringResource(R.string.action_next))
        }
    }
}

@Composable
private fun PermissionsStep(
    permissionViewModel: PermissionRationaleViewModel,
    onFinish: () -> Unit,
    primaryFocusRequester: FocusRequester
) {
    val permissions by permissionViewModel.permissions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.tv_screen_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
    ) {
        PermissionList(
            modifier = Modifier.weight(1f),
            permissions = permissions,
            header = {
                Column(
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_small))
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_title_permissions),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = stringResource(R.string.onboarding_permission_select),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            onPermissionCallback = { permissionViewModel.refreshPermissionsList() }
        )

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(primaryFocusRequester)
        ) {
            Text(stringResource(R.string.action_finish))
        }
    }
}
