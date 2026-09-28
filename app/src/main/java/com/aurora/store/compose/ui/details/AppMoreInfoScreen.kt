/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details

import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.aurora.extensions.copyToClipBoard
import com.aurora.extensions.appInfo
import com.aurora.extensions.requiresGMS
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.datasafety.Report as DataSafetyReport
import com.aurora.store.R
import com.aurora.store.compose.composable.Info
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.ui.details.composable.Compatibility
import com.aurora.store.compose.ui.details.composable.DataSafety
import com.aurora.store.compose.ui.details.composable.DetailsPortalCard
import com.aurora.store.compose.ui.details.composable.DeveloperDetails
import com.aurora.store.compose.ui.details.composable.Privacy
import com.aurora.store.compose.ui.details.composable.Testing
import com.aurora.store.compose.ui.details.composable.UserReview
import com.aurora.store.compose.ui.details.navigation.ExtraScreen
import com.aurora.store.data.model.AppState
import com.aurora.store.data.model.Report
import com.aurora.store.data.model.Scores
import com.aurora.store.util.PackageUtil

@Composable
internal fun AppMoreInfoScreen(
    app: App,
    state: AppState,
    userReview: Review?,
    isAnonymous: Boolean,
    plexusScores: Scores?,
    dataSafetyReport: DataSafetyReport?,
    exodusReport: Report?,
    onNavigateToExtra: (NavKey) -> Unit,
    onTestingSubscriptionChange: (subscribe: Boolean) -> Unit,
    onSubmitReview: (rating: Int, title: String, comment: String) -> Unit,
    onDeleteReview: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.details_more_info),
                boxedNavigationIcon = true
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { AppVersionAndTechnicalInfo(app = app, state = state) }

                if (!isAnonymous && app.isInstalled) {
                    item {
                        UserReview(
                            review = userReview,
                            onSubmit = onSubmitReview,
                            onDelete = onDeleteReview
                        )
                    }
                }

                if (!isAnonymous && app.testingProgram?.isAvailable == true) {
                    item {
                        Testing(
                            isSubscribed = app.testingProgram!!.isSubscribed,
                            onTestingSubscriptionChange = onTestingSubscriptionChange
                        )
                    }
                }

                item {
                    Compatibility(needsGms = app.requiresGMS(), plexusScores = plexusScores)
                }

                item {
                    if (app.permissions.isNotEmpty()) {
                        DetailsPortalCard(
                            title = stringResource(R.string.details_permission),
                            description = stringResource(
                                R.string.permissions_requested,
                                app.permissions.size
                            ),
                            icon = painterResource(R.drawable.ic_android),
                            onClick = { onNavigateToExtra(ExtraScreen.Permission) }
                        )
                    } else {
                        SectionHeader(
                            title = stringResource(R.string.details_permission),
                            subtitle = stringResource(R.string.details_no_permission)
                        )
                    }
                }

                if (dataSafetyReport != null) {
                    item {
                        DataSafety(
                            report = dataSafetyReport,
                            privacyPolicyUrl = app.privacyPolicyUrl
                        )
                    }
                }

                item {
                    Privacy(
                        report = exodusReport,
                        onNavigateToDetailsExodus = if (exodusReport != null &&
                            exodusReport.id != -1
                        ) {
                            { onNavigateToExtra(ExtraScreen.Exodus) }
                        } else {
                            null
                        }
                    )
                }

                item {
                    DeveloperDetails(
                        address = app.developerAddress,
                        website = app.developerWebsite,
                        email = app.developerEmail
                    )
                }
            }

            ScrollHint(
                listState = listState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun AppVersionAndTechnicalInfo(app: App, state: AppState) {
    val versionName = if (state is AppState.Installed) state.versionName else app.versionName
    val versionCode = if (state is AppState.Installed) state.versionCode else app.versionCode

    SectionHeader(title = stringResource(R.string.details_app_version))
    if (state is AppState.Updatable) {
        Info(
            title = AnnotatedString(text = stringResource(R.string.details_more_installed_version)),
            description = AnnotatedString(
                text = stringResource(
                    R.string.version,
                    PackageUtil.getInstalledVersionName(LocalContext.current, app.packageName),
                    PackageUtil.getInstalledVersionCode(LocalContext.current, app.packageName)
                )
            ),
            painter = painterResource(R.drawable.ic_updates)
        )
        Info(
            title = AnnotatedString(text = stringResource(R.string.details_more_available_version)),
            description = AnnotatedString(
                text = stringResource(R.string.version, app.versionName, app.versionCode)
            ),
            painter = painterResource(R.drawable.ic_updates)
        )
    } else {
        Info(
            title = AnnotatedString(text = stringResource(R.string.details_more_version)),
            description = AnnotatedString(
                text = stringResource(R.string.version, versionName, versionCode)
            ),
            painter = painterResource(R.drawable.ic_updates)
        )
    }

    SectionHeader(title = stringResource(R.string.details_technical_details))
    Info(
        title = AnnotatedString(text = stringResource(R.string.details_more_package_name)),
        description = AnnotatedString(text = app.packageName),
        painter = painterResource(R.drawable.ic_android)
    )
    Info(
        title = AnnotatedString(text = stringResource(R.string.details_more_target_api)),
        description = AnnotatedString(text = "API ${app.targetSdk}"),
        painter = painterResource(R.drawable.ic_menu_about)
    )
    Info(
        title = AnnotatedString(text = stringResource(R.string.details_more_content_rating)),
        description = AnnotatedString(text = app.contentRating.title),
        painter = painterResource(R.drawable.ic_menu_about)
    )

    val certHashes = app.certificateSetList
        .mapNotNull { it.sha256.takeIf(String::isNotBlank) }
        .map(::decodeBase64UrlToHex)
    if (certHashes.isNotEmpty()) {
        val context = LocalContext.current
        certHashes.forEachIndexed { index, certHash ->
            val title = if (certHashes.size == 1) {
                stringResource(R.string.details_more_certificate_hash)
            } else {
                "${stringResource(R.string.details_more_certificate_hash)} ${index + 1}"
            }
            Info(
                title = AnnotatedString(text = title),
                description = AnnotatedString(text = certHash),
                painter = painterResource(R.drawable.ic_menu_about),
                onClick = {
                    context.copyToClipBoard(certHash)
                    context.toast(R.string.toast_clipboard_copied)
                }
            )
        }
    }

    app.appInfo.appInfoMap.forEach { (title, subtitle) ->
        Info(
            title = AnnotatedString(
                text = title.replace("_", " ")
                    .lowercase(LocalLocale.current.platformLocale)
                    .replaceFirstChar {
                        if (it.isLowerCase()) {
                            it.titlecase(LocalLocale.current.platformLocale)
                        } else {
                            it.toString()
                        }
                    }
            ),
            description = AnnotatedString(text = subtitle),
            painter = painterResource(R.drawable.ic_menu_about)
        )
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun decodeBase64UrlToHex(base64Url: String) =
    Base64.decode(base64Url, Base64.URL_SAFE).toHexString()
