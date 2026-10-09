## Summary

This PR proposes a UI-focused Material 3 Expressive refresh for Aurora Store. It is based on upstream `master` at `e17b1a4a2be9c325dbced6946b5cc91e83c13b02` and is maintained in my public fork: [aryan-j/AuroraStore](https://github.com/aryan-j/AuroraStore/tree/codex/material-3-expressive-ui-proposal).

The goal is to show the redesign on a real device and invite maintainer feedback on whether this direction belongs in Aurora Store, should be split into smaller changes, or should remain a separately maintained variant.

## Before and after

Captured from Aurora Store 4.8.4 and the UI proposal on the same Nothing Phone (2). Catalog contents can change between sessions.

### For You

![Stock and Material 3 Expressive For You comparison](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/comparison-for-you.png)

### Home card actions

The redesigned home cards expose the existing install/open flow without requiring users to open app details first.

![Stock and Material 3 Expressive home app card actions comparison](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/comparison-home-actions.png)

### Top Charts

![Stock and Material 3 Expressive Top Charts comparison](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/comparison-top-charts.png)

### Categories

![Stock and Material 3 Expressive Categories comparison](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/comparison-categories.png)

### App details

![Stock and Material 3 Expressive app details comparison](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/comparison-app-details.png)

### Search

![Material 3 full-screen search and list results](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/feature-search.png)

### Settings

![Stock and Material 3 Expressive Settings comparison](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/comparison-settings.png)

### Customization

This section shows the restored System/Light/Dark theme choice and Dynamic colors setting within the redesigned settings UI.

![Stock and Material 3 Expressive Customization comparison](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/comparison-customization.png)

## What changed

- Reworked the Apps and Games landing pages, keeping For You, Top Charts, Categories, and chart filters accessible from the same page.
- Added Install/Open actions to home app cards, reusing the existing install and launch flows.
- Added a floating bottom navigation and integrated full-screen search, with Material 3 list rows for suggestions and results.
- Rebuilt category and chart rows around Material 3 list components, with consistent spacing, rounded outer corners, and contained icons.
- Refreshed app details with a screenshot carousel, compact metric pills including version, clearer section portals, and an expandable featured review.
- Redesigned the settings landing and preference rows, and restored Aurora's original System/Light/Dark and Dynamic colors controls.
- Updated menus, icon buttons, connected actions, navigation, progress feedback, spacing, and motion to use Material 3 / Expressive patterns.

## Behavior and minimum Android version

The changes are concentrated in Compose presentation. Store repositories, network/data flows, update workers, downloads, installation, and the upstream startup/page-retention policy are left intact. The branch does include a small cancellation improvement for stale typed search suggestions, a preferred display refresh-rate request, and a baseline profile for the redesigned UI; these are disclosed in the patch notes. Home card actions call the existing install/open flows. The original theme mode and dynamic color preferences are restored and exposed in the redesigned settings UI.

The current `androidx.compose.material3:material3:1.5.0-alpha29` artifact declares API 24, so this proposal now sets `minSdk` to 24. See the [Material 3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3) and [AndroidX minimum SDK guidance](https://developer.android.com/jetpack/androidx/versions). Compose stack would require replacing or pinning the AndroidX Compose artifacts that declare API 24, then porting any Expressive APIs that differ in the compatible versions. I would appreciate guidance on whether API 24 is acceptable here or whether API 23 support should be a hard requirement.

## Maintainer feedback

Would you be open to this UI direction? If so, would you prefer the work split into smaller PRs? In any case, I would absolutely love it if you spent a few moments using the app with this new UI, I believe you may find some things of value here.
