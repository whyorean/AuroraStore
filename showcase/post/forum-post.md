# Material 3 Expressive UI proposal for Aurora Store — before/after screenshots

I’ve been working on an independent, UI-focused redesign proposal for Aurora Store 4.8.4. The goal was to modernize the Compose interface while leaving Aurora’s store data, repositories, install/update flows, and startup/page-retention policy under the upstream implementation.

This is an unofficial community proposal, not an official Aurora Store release. The upstream pull request is [PR #113](https://github.com/whyorean/AuroraStore/pull/113), and my public fork is [aryan-j/AuroraStore](https://github.com/aryan-j/AuroraStore/tree/codex/material-3-expressive-ui-proposal). I’d like maintainer and community feedback before treating this as a separate distributed app.

The comparisons below are direct captures from the stock 4.8.4 app and the redesigned UI on the same Nothing Phone (2). Catalog content can vary between sessions.

## For You

![Stock Aurora Store and Material 3 Expressive For You comparison](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/comparison-for-you.png)

## Top Charts

![Stock Aurora Store and Material 3 Expressive Top Charts comparison](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/comparison-top-charts.png)

## Categories

![Stock Aurora Store and Material 3 Expressive Categories comparison](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/comparison-categories.png)

## App details

![Stock Aurora Store and Material 3 Expressive app details comparison](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/comparison-app-details.png)

## Search

![Material 3 full-screen search and list results](https://raw.githubusercontent.com/aryan-j/AuroraStore/codex/aurora-ui-showcase/showcase/comparisons/feature-search.png)

### Main changes

- A new home layout for For You, Top Charts, Categories, and chart filters, with floating bottom navigation and integrated full-screen search.
- Material 3 list rows for charts, categories, search results, and reviews, with consistent spacing, grouped corners, and contained icons.
- A refreshed details screen with larger screenshot carousel, app version in the metrics row, clearer section portals, and an expandable featured review.
- Material 3 expressive menus, icon buttons, connected install actions, navigation, progress feedback, and motion.

The proposal keeps the upstream name and launcher artwork. It is based on GPL-3.0 source, but the GPL does not grant trademark rights; I’m asking the maintainers for guidance before promoting a separately distributed app. My fork has an earlier AOSP test-signed preview artifact in Releases; it is not an official or stable build, and I’m not recommending it for general installation while that branding question is open.

The Compose UI is the main scope. A few related responsiveness changes are documented in the patch notes: cancellation of stale typed search suggestions, a preferred refresh-rate request, and a baseline profile. The store backend, data flow, download/install code, and startup/page-retention policy remain upstream.

The proposal currently targets Android API 24 because the selected Material 3 artifact declares that minimum. The previous preview used API 26, which was higher than needed. Getting back to Aurora’s original API 23 would require selecting Compose dependencies that still support API 23 and porting any expressive components whose APIs differ; removing one animation would not be enough.

I used Codex AI assistance during implementation and documentation, and I’m responsible for reviewing and explaining the changes. I’d be interested to hear whether this direction fits Aurora Store, whether the maintainers would prefer smaller PRs, or whether a distinctly branded community variant would be more appropriate.
