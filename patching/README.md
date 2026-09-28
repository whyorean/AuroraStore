# Aurora Store Expressive UI Patch

This folder keeps the downstream UI work reproducible against Aurora Store source updates. The complete source delta is `aurora-expressive-ui.patch`; `MANIFEST.md` lists every changed source path included in that patch.

## Source baseline and inspection

- Upstream: <https://github.com/whyorean/AuroraStore>
- Fork: <https://github.com/aryan-j/AuroraStore>
- Baseline branch: `master`
- Baseline commit: the full SHA in [`BASE_COMMIT`](BASE_COMMIT)
- Baseline version inspected: upstream `master` as fetched on 2026-09-27

## Current patch scope — 2026-09-28

The current generated patch is the source of truth for this fork. It is primarily a Compose UI and resource redesign, plus the small build/resource changes required to support it. It does not replace Aurora's startup policy, page-retention policy, repositories, data sources, workers, download/install behavior, or backend logic.

- The main and inner pagers, startup requests, Categories trigger, Top Charts pagination, downloads collection, and app-details flow collection follow the recorded upstream behavior. The pages are not artificially warmed or held in memory by this patch.
- Non-Compose source changes are limited to a window refresh-rate preference (`ComposeActivity`, capped at 120 Hz), cancellation of stale typed search suggestions (`SearchViewModel`), and a Material 3 minimum SDK/dependency adjustment. Aurora's existing suggestion filtering and five-item limit are retained. Cancellation only applies to user-entered nonblank queries; it adds no startup prefetch.
- The baseline profile contains compilation rules for the redesigned UI. It does not execute or precompose the screens at startup.
- The proposal preserves the upstream launcher artwork and app display name so the contribution stays focused on the UI. It retains the upstream GPL notices; the GPL does not itself grant trademark rights.

No changes are made under the app's data, repository, worker, or installer source packages. When Aurora updates those areas without changing a touched UI surface, this patch should remain straightforward to apply. UI or navigation changes in overlapping files still need a targeted port and review.

At inspection time, `HEAD`, `origin/master`, and `upstream/master` all resolved to `e17b1a4a2be9c325dbced6946b5cc91e83c13b02`; the fork and upstream had no commit divergence. The current UI work is a local working-tree delta on that commit; there is not yet a UI commit on the fork's `master`. The manifest and patch capture the modified working tree, including new files and assets.

The original code uses a primary tab row for Apps/Games sections and a secondary tab row for chart filters. Its details screen has the original vertically arranged app information and action components. The existing app already uses `MaterialExpressiveTheme`; this patch adds the expressive motion scheme, revised screen layouts and native expressive components across those existing Compose surfaces. Typography uses Material 3's default `FontFamily.Default` system sans, matching the original app.

The modified app was inspected on the paired Android device. The Apps screen now keeps For You, Top Charts, and Categories in one page with the chart choices shown in context and a floating bottom navigation/search bar. The details screen uses the redesigned app header, metrics carousel, action group, screenshot carousel, review card, and clearer portals into more information and reviews. The details overflow menu was also inspected after constraining its leading icons to the standard 24dp size.

## Change map

The patch is the exact source of truth; the map below is a reader's guide.

| Area | Changes |
| --- | --- |
| Home, Games, and Updates | Reworks the Apps/Games tab UI and chart/category views; brings chart filters into the main page; updates app/update list treatments; adds the floating bottom bar with integrated search and content clearance. |
| App details and subpages | Reorganizes the app header, metrics, connected actions, screenshots, changelog, featured review, and navigation portals; moves version/technical detail into More info; preserves existing detail destinations. |
| Shared visual system | Uses the original system typography with expressive motion; standardizes app-icon, tile, and arrow shapes; revises spacing, icon buttons, list treatments, and menu styling. |
| Other Compose surfaces | Applies the shared menu, list, and control styling to search, accounts, updates, downloads, favorites, blacklist, spoofing, MicroG, and related sheets/screens. |
| Resources and build | Adds strings and icons, updates the Compose Material version, and sets `minSdk` to 24 to match the current AndroidX Compose dependency floor. |

No repository, network API, database, worker, or installer implementation files are part of this UI patch. Existing actions and navigation callbacks are retained in the changed Compose screens. The earlier API 26 setting was higher than required; the current Compose Material 3 artifact declares API 24 as its minimum. Returning to Aurora's original API 23 floor would require pinning or replacing Compose dependencies that now declare API 24 and porting any expressive components whose APIs differ in those compatible versions. Removing a single animation is not sufficient to restore API 23. The Material 3 dependency is an alpha release, so upstream library changes may require follow-up adjustments.

The search UI's typeahead path is the only existing ViewModel behavior tuned for responsiveness: it cancels stale suggestion fetches and skips empty queries. The Search API contract, submitted search behavior, and other app data flows are unchanged.

## Apply to a clean Aurora checkout

Use a full Git checkout of upstream or the fork, on `master` or a descendant of the baseline. The target checkout must have a clean working tree. The script validates ancestry, checks whether the patch is already present, tests applicability, and then applies it with Git's three-way patch support. It stages the resulting source changes for review.

```sh
git clone https://github.com/whyorean/AuroraStore.git /path/to/AuroraStore
# Run this helper from the root of the fork checkout that contains patching/.
./patching/apply-patch.sh /path/to/AuroraStore
```

If a later upstream commit changes the same UI code, the script stops instead of forcing hunks through. Resolve that UI overlap in the target checkout, inspect the affected screens, then build it:

```sh
cd /path/to/AuroraStore
./gradlew :app:assembleDebug
```

Backend-only upstream commits should usually apply without manual changes because the patch's touched files are Compose UI, resources, and build configuration. That is an expectation to verify for each update, not a guarantee.

## Refresh the patch after UI work

Make UI changes on a checkout whose recorded base is `BASE_COMMIT`, then run:

```sh
./patching/export-patch.sh
```

The exporter snapshots the source tree using a temporary Git index. It does not stage or reset the user's normal index and includes added files and binary assets. It writes the complete binary-capable patch and regenerates the file manifest with a SHA-256 checksum, excluding this `patching/` folder from the app patch itself.

To export a manually ported patch from a later upstream checkout, pass that checkout and its exact clean upstream base commit:

```sh
./patching/export-patch.sh /path/to/ported/AuroraStore <new-upstream-commit>
```

When a new baseline is supplied, the exporter updates `BASE_COMMIT` only after successfully generating the patch and manifest. Record a short human-readable entry in this file describing the newly ported UI work and any upstream UI overlap that needed manual work. Then apply the refreshed patch to another clean checkout at that baseline and build it before calling the update reproducible.

## Porting across upstream updates

1. Fetch the new upstream `master` and record its full commit SHA.
2. Apply the current patch to that commit with `apply-patch.sh`.
3. If Git reports overlap, port the affected UI changes deliberately; do not force-apply rejected hunks. Backend-only updates should leave the UI patch's source files untouched.
4. Inspect the app screens affected by the port and run `:app:assembleDebug`.
5. Export from the ported checkout with the new upstream SHA using `export-patch.sh`.
6. Review the new patch and manifest, then verify it applies to a second clean checkout of that upstream SHA.

If Aurora changes a screen's UI or navigation structure, the patch may need targeted redesign work in that area. The patch is reproducible source code, not a promise that every future upstream UI revision can be merged without review.

## Verification recorded for this snapshot

- Upstream and fork `master` refs were fetched and compared; both matched the recorded baseline.
- The modified app was installed on the paired Android device and its Apps and app-details screens were inspected.
- `:app:assembleDebug` succeeded for this working tree.
- `git diff --check` passed.
- The exporter-generated manifest records the exact patch file inventory.

## UI performance follow-up — 2026-09-27

This follow-up changes only UI code introduced by the expressive redesign:

- Main bottom-tab selection animates between precomposed pages with a medium-low spring; all three pages remain warm so a tap does not need to build a feed. The selected tab follows the pager target so its motion starts with the page change.
- Removed item-placement animations from static app rows and the append-only Top Charts list. Their contents do not reorder, so these per-item animations added work without conveying a move.
- Preserved Aurora's startup prefetch for the default Top Charts list after the first UI-only deferral caused that tab to remain on skeletons. Other chart filters still load on selection, pagination pauses while Top Charts is hidden, and Categories data loads when its tab becomes visible.
- Top Charts resets its screen-local list to rank 1 when the selected chart or loaded chart cluster changes, and uses filter-specific row keys so switching filters does not retain a stale scroll anchor. On-device checks confirmed both Top Free and Top Grossing populate and open at rank 1 after switching between them.
- Shared one shimmer animation across each loading list (instead of a separate animation per skeleton block), reducing independent infinite transitions on loading screens.
- Memoized screenshot and feed-cluster de-duplication, avoiding repeated scans while building the details carousel and home feed.

The initial connected-device sample before these changes recorded 9 janky frames out of 238 during two main-tab changes (3.78%; 90th percentile 16 ms). The early post-change sample recorded 3 out of 277 (1.08%; 90th percentile 11 ms). These are short debug-build samples with different frame counts, not a controlled comparison against the original app; they are only directional evidence. One long-frame outlier remained in that early post-change sample. At that time, the details page stayed on its loading indicator; a loaded-details trace was captured later and is recorded below.

The updated sources compiled successfully with `:app:assembleDebug` using the installed Java 21 and Android SDK. The current patch and manifest were regenerated after this follow-up.

The bottom bar selection animation now drives tab padding, icon slot width, icon fade/scale, and selected/unselected colors from a single transition. The selected tab uses `FloatingToolbarDefaults.ContainerShape`, the same shape passed to `HorizontalFloatingToolbar`, so their corner radii stay matched. The tab and pager use a no-bounce medium-low spring to restore the slower timing used elsewhere in the app. This replaces the separate `AnimatedVisibility` and `animateContentSize` layout animations, which could finish at different times and produce a small final clip/snap. A dynamic surface fade under the bar follows the layered surface-gradient approach in Google Chat's `compose_screen_bottom_bar_background` resource; the Material 3 floating toolbar retains its own elevation shadow, without an added shadow layer. The updated debug APK compiled, installed on the Nothing Phone (2), and was checked while switching tabs; the selected tab settled without a final clip/snap and used the toolbar's matching corner radius. The frame samples below refer to earlier builds and remain directional only.

The final installed debug build was sampled on the connected device: 12 main-tab selections recorded 24 janky frames out of 1,246 (1.93%; p90 13 ms, p99 29 ms), and ten vertical swipes on a loaded details page recorded 4 out of 1,373 (0.29%; p90 12 ms, p99 15 ms). A five-second Top Charts skeleton sample recorded 18 janky frames out of 994 (1.81%; p90 19 ms, p99 31 ms) immediately before restoring the default-chart prefetch. These are single-device debug traces, not a controlled original-versus-redesign benchmark. Restoring the original prefetch fixed Top Charts loading, so the skeleton sample is only a loading-state reference and does not represent the normal populated view. The shared shimmer reduces animation count structurally, but this sample does not establish a before/after improvement for loading.

## Search, chart-list, and review-list follow-up — 2026-09-27

- Kept Top Charts on the official Material 3 `SegmentedListItem` component and explicitly centered its content vertically. Material 3's expressive list default switches taller rows to top alignment; these chart rows have a three-line content stack, which made the rank, app icon, title, and supporting metadata appear high in their containers.
- Rebuilt search around the current state-based Material 3 `SearchBar`. Pressing Search in the floating bottom bar opens the full-screen contained search immediately, focuses the field, and shows the keyboard. The field keeps consistent side insets and status-bar spacing. Search suggestions and submitted app results use clickable M3 `ListItem` components, with app icons, developer names, metadata, and separators retained. The 2026-09-28 follow-up below moved submitted results into the same contained surface so its close animation always returns to the live home screen.
- Replaced the custom review row layout with the current M3 `ListItem` API in the shared review component. Review rows now use the M3 headline, overline, supporting, leading, and trailing slots; the review text, star rating, avatar, and expandable arrow remain available in both the featured detail review and the full reviews screen.
- `:app:assembleVanillaDebug` succeeded and the updated build was installed on the Nothing Phone (2). Device captures confirmed Search opens directly into its full-screen contained state with the keyboard visible (`mInputShown=true`), along with submitted search results, the featured review row, and the full review list. The initial review-row and result-list builds were also checked during implementation; the final build compiled without Material 3 deprecation warnings for ListItem.

## UI efficiency follow-up — 2026-09-27

- Search no longer starts an empty typeahead request when opened; it waits 180 ms after the latest typed edit and cancels older suggestion requests. Bottom-bar search skips paging, filters, and result rows until a query is submitted. The expanded search field opens with the keyboard immediately.
- Moved review, privacy, data-safety, and compatibility-flow collection into the More info destination. Their asynchronous updates no longer invalidate the primary details page while it is visible.
- Cached the inline recommendation cluster normalization and de-duplicated package rows, matching the carousel path and avoiding duplicate lazy-list keys. Neither renderer filters by the “Similar apps” title. The unchanged details-stream loader drops empty clusters, so when “More by Canva” appears without “Similar apps,” the loaded stream did not provide a non-empty Similar apps cluster.
- Reused remembered Coil requests for app, category, review, and search list images, including progress-updating app icons. The screenshot carousel now retains its size-aware request across image-state recompositions.
- Remembered the home backdrop gradient, the Apps/Games tab definitions, details metrics and snap behavior, rating aggregates, and review-filter metadata to avoid rebuilding them during unrelated state updates.
- The main screen now observes download-flow emissions without collecting the entire download list into composition state. It maps progress updates to active package membership and suppresses unchanged sets, so progress ticks do not update composition state or recompose the retained home pager.
- No build, device interaction, or performance trace was run for this source-only pass, as requested. Review the regenerated source patch before building it.

## Search startup and refresh-rate follow-up — 2026-09-27

- Bottom-bar search no longer subscribes to the Paging result stream until a query is submitted. The collapsed SearchBar anchor stays composed inside the floating toolbar so Material 3 can animate the full-screen surface from that exact location; paging and result rows stay out until submit.
- The standalone Search destination has no route cross-fade. Bottom-bar search keeps the main destination active and displays its results inside the same contained search surface, avoiding a route transition during the open/close motion.
- `ComposeActivity` requests a 120 Hz preferred window refresh rate. Android 14 and newer receive the 120 Hz preference directly; earlier versions select the highest supported mode at or below 120 Hz. This is a preference: Android can still choose 90 Hz or lower due to the display's supported modes, system settings, power policy, or thermal state.
- `:app:assembleVanillaDebug` succeeded and the resulting APK installed on the connected wireless-ADB phone. The app was not launched and the applied display refresh rate was not measured; the OS may choose a different rate than the requested 120 Hz.

## Search back-navigation follow-up — 2026-09-27 (superseded)

- This route-based version used a handler inside the dialog window to pop Search. Device checks at the time showed keyboard-first dismissal and a separate route exit. The 2026-09-28 follow-up below replaces that behavior for the bottom-bar entry point; the standalone Search destination remains available to callers that navigate directly to it.

## Search surface motion follow-up — 2026-09-28 (superseded)

This records an intermediate contained-search implementation. The current bottom-bar entry keeps the Apps/Games page underneath a full-screen Search overlay; its 280 ms slide and Back behavior are documented in the follow-up below.

- Search results now stay inside the full-screen Material 3 contained search surface instead of pushing a separate Search destination. That keeps the live home page beneath the surface and lets a single Back collapse search to its bottom-toolbar anchor without revealing an empty route.
- The collapsed bottom-bar anchor now has its own stable magnifier field, separate from the expanded field's back and clear controls. The anchor is 64dp to match the adjacent floating toolbar height and the Google Chat reference. This avoids swapping the leading/trailing controls while the search surface morphs.
- The activity temporarily uses `adjustNothing` while the search is opening, expanded, or closing, then restores its original soft-input mode after the collapse completes. This prevents the keyboard from moving the hidden toolbar anchor during the shared search transition.
- Submitting a query shows the existing M3 result rows and filters in the expanded surface; selecting a result collapses search and opens that app's details. Returning from details preserves the query and results for the next search open.
- `:app:assembleVanillaDebug` succeeded and the installed debug APK was checked on the Nothing Phone (2). On-device checks loaded Canva results, confirmed one Back returned to the Apps feed with no blank route, opened Canva details from a result, and returned to Apps. The compact search button and toolbar were aligned at the same height in the installed UI.

## Search transition responsiveness follow-up — 2026-09-28

- Bottom-bar Search is a full-screen overlay above the still-composed Apps/Games page. It uses a 280 ms decelerating slide for both entrance and exit, so closing returns to the live feed without a blank route.
- Back changes the overlay state before asking Android to dismiss the keyboard, letting the Search panel start moving immediately. The focused field remains active through its exit animation.
- The opening and closing transitions keep the Apps screen visible underneath; no separate blank surface or fade is introduced.
- `:app:assembleVanillaDebug` succeeded and the updated debug APK was installed on the Nothing Phone (2). On-device checks opened Search with the keyboard ready and verified that one Back press returned to the Apps/Games page.

## UI responsiveness follow-up — 2026-09-28

- The Apps/Games inner pager now retains only the selected page and its neighbor. Keeping all three pages warm for both Apps and Games was eagerly composing six feed/list surfaces and starting their visible image work; the three main bottom-navigation pages still use Aurora's original retention behavior.
- Categories starts its existing load as soon as it becomes the pager target, rather than waiting for the page animation to finish. Top Charts keeps Aurora's eager Top Free prefetch and now avoids requesting Top Free a second time when its page becomes visible.
- App details switches directly between Loading, Error, and content. The previous short `AnimatedContent` transition composed both a loading surface and the full details tree during the same frames, even though the navigation page already has its own entrance motion.
- After the app details content appears, the screenshot carousel waits 180 ms before creating its large image painters and keeps an equal-size static Material placeholder in the meantime. Carousel images skip the full-area shimmer and crossfade; the full-screen screenshot viewer keeps its existing loading treatment.
- Search changes its visible state before requesting keyboard dismissal, so the overlay exit starts without waiting on IME work.
- The final `:app:assembleVanillaDebug` build was installed and inspected on the Nothing Phone (2). Top Charts populated; Categories and For You switched; The Vault: Logic Puzzle Box opened in details; Search opened with the keyboard and one Back returned to Games.

Short connected-device `gfxinfo` samples on the 120 Hz debug build included the transition and nearby settle frames: Top Charts 9/179 janky frames (p90 16 ms); Categories 5/99 (p90 13 ms); For You 2/80 (p90 8 ms); Apps-to-Games 1/98 (p90 14 ms); Vault details 4/106 (p90 10 ms, p95 21 ms); Search open 1/99 (p90 8 ms); Search close 2/37 (p90 10 ms). A repeated warm Vault open recorded 5/92 janky frames (p90 7 ms, p99 77 ms). These are short debug-build samples, not a controlled comparison with the stock app, and isolated long frames remain.

`git diff --check` passed and the patch bundle/manifest were regenerated from the recorded upstream baseline.

## Transition and carousel behavior follow-up — 2026-09-28

- Removed the experimental per-screenshot stagger. Once the existing 180 ms details-page startup delay expires, visible carousel items create their normal image requests directly.
- Matched the App Details Back transition to the stock Aurora 4.8.3 source: the revealed screen enters with its original `spring(dampingRatio = 0.8f, stiffness = 380f)`, and the details screen exits with the default `slideOutHorizontally` animation, without the newer shared fade. The checkout's newer root transition remains in use for other routes.
- Restored a vertical expand/collapse animation for the Top Charts filter group when switching between the main tabs.
- The updated Vanilla Debug APK built successfully, installed, and launched on the connected Nothing Phone (2). It is ready for the next on-device inspection; no new performance sample was taken for this handoff.

## Earlier comparison build restored — 2026-09-28

- Recreated the earlier comparison configuration at the user's request: a 480 ms screenshot-section delay followed by 100 ms between carousel image requests, a details-only spring slide that keeps the feed still during pop, and fade-only visibility for the Top Charts filters.
- Built and installed this version for on-device comparison. No new lag measurement was taken.

## Stock startup work queue and system font — 2026-09-28

- Matched the original outer pager retention: Apps, Games, and Updates compose immediately, with no delayed page warming. The Apps/Games inner pager now uses the stock default `beyondViewportPageCount` of zero, so hidden chart/category pages do not compose before selection; Aurora's existing Top Free data prefetch remains unchanged.
- Restored the original data triggers as well: the Apps/Games Top Free startup request, the Top Charts selected-filter request and pagination trigger, and the Categories load effect all use their baseline keys and visibility behavior. MainScreen again collects the complete downloads list as stock. Details keeps the original root-level collectors for review, compatibility, data-safety, and Exodus state, even though those sections now render inside More info.
- Removed the 480 ms screenshot delay and per-item 100 ms request stagger. The lazy carousel now requests images for its visible items when that section is composed, without a separate UI warm-up queue.
- Removed the bundled Google Sans Flex font and typography override. The baseline `MaterialExpressiveTheme` used the default Android system sans, so the redesigned screens now do as well.
- This source snapshot contains no Google Sans Flex asset or custom typography override. The build was installed and launched after this change; no new frame trace has been collected.

The later responsiveness follow-up had temporarily gated Categories and pagination on pager visibility, avoided the baseline Top Free request on Top Charts entry, mapped downloads into a reduced active-package stream, and moved details collectors into the More info destination. This audit removes those changes so startup requests, stream collection, and UI state lifetime match Aurora's baseline. The visual tab, list, details, and search components remain redesigned. The existing search typeahead responsiveness work remains limited to typed search interaction and is documented above; it does not add startup prefetch or retain hidden pages.

## Navigation and progress indicator follow-up — 2026-09-28

- Search no longer leaves the main floating navigation pill visible underneath the full-screen results surface. Its list viewport extends into the transparent system navigation area, with bottom list padding preserving access to the final rows.
- The main medium app bar now uses Material 3's exit-until-collapsed scroll behavior. Its lower edge clips to the shared 20dp UI radius while it collapses. Expanded and collapsed states share one opaque surface color, preventing the content behind the collapsing bar from briefly tinting it.
- Replaced the circular app download/install progress ring with Material 3 `CircularWavyProgressIndicator`, matching the supplied wavy-ring reference. The existing icon scale and rounded-square-to-circle morph remain unchanged. This uses the project's existing Material 3 1.5.0-alpha29 dependency.
- `:app:assembleDebug` succeeded. The updated `com.aurora.store.debug` APK was installed alongside the stock `com.aurora.store` 4.8.4 app. Device inspection confirmed the collapsing home app bar and that submitted Canva results render into the transparent navigation region without the floating pill behind them. No real download was started for the indicator check.

## Cold start and details transition follow-up — 2026-09-28

- The App Details push now slides in an opaque destination while the previous page slides out and fades over 120 ms. This keeps the new surface from blending with outgoing feed items during the handoff. The App Details pop and predictive-pop transitions remain unchanged to preserve the stock-style Back motion.
- Rechecked the forward transition on the connected phone using the Nightly app. At the first two captured frames, the incoming details surface showed its loading indicator without old feed content over it; the Canva details content was visible by the 500 ms frame. The stock `com.aurora.store` package was not modified.
- Added manual Baseline Profile rules for the redesigned navigation, home, Apps/Games, For You, carousel, and details composables. The rules only change compilation of UI code; they do not change Aurora's startup queue, retained pages, data requests, lifecycle, or state ownership. The first draft used incomplete wildcard signatures and was discarded by profile compilation; the final rules use the documented trailing wildcard form.
- After rebuilding and installing the optimized Vanilla Nightly APK, ProfileInstaller returned success and Android reported the package compiled with `speed-profile`. Its packaged binary profile grew from 17,766 to 18,513 bytes and its R8-transformed profile now includes the selected app UI classes. This confirms the rules reached the installed app.
- Four cold `am start -W` samples after resetting compilation were 269/262/238/263 ms for the previous Nightly build and 263/281/267/272 ms for stock. After installing the profile and compiling Nightly with `speed-profile`, four samples were 231/232/216/214 ms. These are directional measurements across packages with separate app data, but the profile reduced Nightly startup time in this run and brought it close to stock. The debuggable Debug variant includes LeakCanary and is not a fair startup comparison with stock; these measurements use the optimized Nightly variant.
- Rebuilt and installed the Debug package as well. Its APK does not include a compiled Baseline Profile, and Android reports `run-from-apk` for that debuggable variant. It took 1,049 ms on the first launch after install, then 570/569/566/558 ms after resetting compilation and clearing its profile data. This development build remains slower than stock; the release-like Nightly is the meaningful comparison for end-user startup. No debug-only tooling or non-UI code was changed.
- No page warming, startup prefetch, lifecycle, or backend/data-flow behavior was changed. Both `:app:assembleVanillaDebug` and `:app:assembleVanillaNightly` succeeded; both modified packages are installed on the connected device, with Debug open on Canva details for inspection.

## List and gutter consistency follow-up — 2026-09-28

- Replaced the shared custom app/account/download/settings row layout with the official Material 3 `ListItem`, using shared 16dp horizontal content insets and 8dp vertical insets. Updated the technical info and Exodus tracker rows to use Material 3 list slots as well.
- Top Charts, Categories, and Updates use expressive `SegmentedListItem` rows. Their lists share a 16dp outer side gutter, while row content uses the same 16dp inset; section headings and loading placeholders align with those row edges.
- Category artwork now sits inside a 48dp rounded `secondaryContainer` box using the shared app-icon corner radius, with a centered tinted category glyph.
- `:app:assembleVanillaDebug` succeeded and the APK installed as `com.aurora.store.debug` alongside stock. On-device inspection confirmed the Categories page's rounded leading icon containers, and consistent 16dp outer gutters on Categories, Top Charts, and Updates. Top Charts rank/icon/text content is vertically aligned in its list rows. The stock app was not modified.

### Shared list corner and stream sub-section follow-up — 2026-09-28

- Added shared `ListItemShapes` helpers based on Aurora's `radius_large` token (28dp), matching the home app-card radius. M3 segmented lists now round only the top edge of the first row and the bottom edge of the last row; middle rows stay square. One-off list cards keep the shared full radius across interaction states. App-icon masks retain their 20dp radius.
- “Apps made in Australia” and other For You stream section portals now open a vertical list built from official M3 `SegmentedListItem` rows. The rows use shared 16dp side gutters, consistent inter-row spacing, centered app artwork with the common icon radius, and app title/developer/metadata slots. Paging keys use package names for stable composition.
- The same M3 stream list is used by expanded stream and developer app sections.
- `:app:assembleVanillaDebug` succeeded and the updated APK was installed on the connected phone. Device inspection of “Apps made in Australia” confirmed the first/last-only corner treatment and shared side gutters.

## App Details transition rollback — 2026-09-28

- Removed the per-destination App Details slide and short outgoing-feed fade that had been added in the cold-start/details transition follow-up. App Details now uses the existing shared navigation transition again for both forward and Back navigation.
- This is a UI-animation-only rollback; screen state, detail loading, and backend behavior are unchanged.
- Rebuilt and installed the Vanilla Debug APK for on-device checking.

## Top Charts filter height motion — 2026-09-28

- Restored vertical expand/collapse motion for the Top Charts sub-filter group as the Apps/Games pager moves between Top Charts and For You. The group expands from the top over 160ms and shrinks toward the top over 120ms, with matching fades; tab and data behavior are unchanged.
- `:app:assembleVanillaNightly` succeeded. The unsigned Nightly artifact was signed with the repository's existing AOSP test key to update the already-installed Nightly package, then installed and opened on the connected phone for user inspection.

## App details metric inset — 2026-09-28

- Increased only the trailing content inset inside each details metric pill from 10dp to 14dp. The icon-side and vertical insets are unchanged, giving the metric text a little more space from the right edge without changing pill height.
- `:app:assembleVanillaNightly` succeeded; the updated APK was signed with the existing AOSP test key, installed over the Nightly package, and opened on the phone to inspect the details metrics.

## Details media loading and metric list efficiency — 2026-09-28

- Reintroduced a 480 ms delay before the details screenshot carousel starts image requests, followed by a 100 ms stagger per carousel item. The carousel keeps its measured rounded placeholder during the delay. This affects only the details screenshot carousel; the full-screen screenshot viewer is unchanged.
- Kept the metrics pills immediately available. They use local vector icons and cached text/layout data, with no network image work or continuous animation to stagger. Added stable metric keys and a shared content type so list recompositions can reuse item compositions.
- Restored the version as a metric pill using Aurora's existing version fields and installed-version helper. It shows the installed version name, or the installed-to-available pair when an update is available. No package/version discovery or backend logic was changed; the display lookup is keyed to the package and update state and runs on the IO dispatcher.
- `:app:assembleVanillaNightly` succeeded; the signed APK was installed over the Nightly package and opened for on-device comparison.
