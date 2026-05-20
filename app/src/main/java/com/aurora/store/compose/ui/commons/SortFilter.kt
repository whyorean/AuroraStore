/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import android.content.Context
import com.aurora.store.R
import com.aurora.store.util.Preferences
import java.util.Locale

/**
 * User-controlled axis for ordering a list of installed apps.
 */
enum class SortBy {
    NAME,
    DATE_INSTALLED,
    DATE_UPDATED,
    PACKAGE_NAME,
    SIZE
}

enum class SortOrder { ASC, DESC }

enum class AppType { USER, SYSTEM }

/**
 * Sort + filter state shared by screens that list installed apps. Persisted via
 * [Preferences] using a per-screen [SortFilterPrefKeys] so each screen restores
 * its own last selection.
 *
 * [installer] is the package name of the installing source to filter by, or null
 * for "all installers". [appTypes] is a non-empty subset; the UI guards against
 * the empty case by always keeping at least one type selected.
 */
data class SortFilterState(
    val sortBy: SortBy = SortBy.NAME,
    val sortOrder: SortOrder = SortOrder.ASC,
    val appTypes: Set<AppType> = setOf(AppType.USER),
    val installer: String? = null
)

/**
 * SharedPreferences keys a screen uses to persist its [SortFilterState].
 */
data class SortFilterPrefKeys(
    val sortBy: String,
    val sortOrder: String,
    val appTypes: String,
    val installer: String
)

fun SortFilterState.save(context: Context, keys: SortFilterPrefKeys) {
    Preferences.putString(context, keys.sortBy, sortBy.name)
    Preferences.putString(context, keys.sortOrder, sortOrder.name)
    Preferences.putStringSet(context, keys.appTypes, appTypes.map { it.name }.toSet())
    if (installer == null) {
        Preferences.remove(context, keys.installer)
    } else {
        Preferences.putString(context, keys.installer, installer)
    }
}

fun loadSortFilterState(context: Context, keys: SortFilterPrefKeys): SortFilterState {
    val sortBy = enumValueOrDefault(Preferences.getString(context, keys.sortBy), SortBy.NAME)
    val sortOrder = enumValueOrDefault(
        Preferences.getString(context, keys.sortOrder),
        SortOrder.ASC
    )
    val appTypes = Preferences.getStringSet(context, keys.appTypes)
        .mapNotNull { runCatching { AppType.valueOf(it) }.getOrNull() }
        .toSet()
        .ifEmpty { setOf(AppType.USER) }
    val installer = Preferences.getString(context, keys.installer).ifBlank { null }
    return SortFilterState(sortBy, sortOrder, appTypes, installer)
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T =
    runCatching { enumValueOf<T>(name) }.getOrDefault(default)

fun SortBy.labelRes(): Int = when (this) {
    SortBy.NAME -> R.string.installed_sort_name
    SortBy.DATE_INSTALLED -> R.string.installed_sort_date_installed
    SortBy.DATE_UPDATED -> R.string.installed_sort_date_updated
    SortBy.PACKAGE_NAME -> R.string.installed_sort_package_name
    SortBy.SIZE -> R.string.installed_sort_size
}

fun SortOrder.labelRes(): Int = when (this) {
    SortOrder.ASC -> R.string.installed_sort_ascending
    SortOrder.DESC -> R.string.installed_sort_descending
}

fun AppType.labelRes(): Int = when (this) {
    AppType.USER -> R.string.installed_filter_user_apps
    AppType.SYSTEM -> R.string.installed_filter_system_apps
}

/**
 * Per-app metadata that [applyFilter] and [applySort] read. Each consumer screen
 * has its own enriched model (e.g. `EnrichedPackage`, `BlacklistAppItem`) that
 * implements this interface so the shared filter/sort logic stays type-safe.
 */
interface InstalledAppMeta {
    val packageName: String
    val label: String
    val firstInstallTime: Long
    val lastUpdateTime: Long
    val sizeBytes: Long
    val isSystem: Boolean
    val installer: String?
}

fun <T : InstalledAppMeta> List<T>.applyFilter(state: SortFilterState): List<T> {
    val typeFilter: (T) -> Boolean = when {
        state.appTypes.containsAll(listOf(AppType.USER, AppType.SYSTEM)) -> { _ -> true }
        AppType.USER in state.appTypes -> { pkg -> !pkg.isSystem }
        AppType.SYSTEM in state.appTypes -> { pkg -> pkg.isSystem }
        else -> { _ -> false }
    }
    val installerFilter: (T) -> Boolean = state.installer
        ?.let { wanted -> { pkg: T -> pkg.installer == wanted } }
        ?: { _ -> true }
    return filter { typeFilter(it) && installerFilter(it) }
}

fun <T : InstalledAppMeta> List<T>.applySort(state: SortFilterState): List<T> {
    val comparator: Comparator<T> = when (state.sortBy) {
        SortBy.NAME -> compareBy { it.label.lowercase(Locale.getDefault()) }
        SortBy.DATE_INSTALLED -> compareBy { it.firstInstallTime }
        SortBy.DATE_UPDATED -> compareBy { it.lastUpdateTime }
        SortBy.PACKAGE_NAME -> compareBy { it.packageName.lowercase(Locale.getDefault()) }
        SortBy.SIZE -> compareBy { it.sizeBytes }
    }
    return sortedWith(
        if (state.sortOrder == SortOrder.ASC) comparator else comparator.reversed()
    )
}
