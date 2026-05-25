<p align="center">
  <img src="app-icon.png" alt="Karoo Power Graph app icon" width="120">
</p>

# Karoo Power Graph

A [Hammerhead Karoo](https://hammerhead.io/) extension that adds two graphical data fields showing the recent history of heart rate and power as a zone-colored curve.

This is a personal fork of [svenk0711/sk0711-graph](https://github.com/svenk0711/sk0711-graph), renamed so it can evolve with different behavior and install separately from the original extension.

Tested on Karoo 3, compatible with Karoo 2.

## Features

- **HR Zone Graph**, **Power Zone Graph** and **Power Zone Graph (NP)** as separate graphical data fields. The (NP) variant shows AVG and **Normalized Power** instead of MAX — useful for time trials.
- Curve color follows the Karoo zone of each sample (5 HR zones, 7 power zones).
- Current value, average, and max shown alongside the curve. AVG/MAX (or AVG/NP) are read from the Karoo's own streams (`AVERAGE_HR`, `MAX_HR`, `AVERAGE_POWER`, `MAX_POWER`, `NORMALIZED_POWER`) so they match the values other data fields on the same page display.
- **Tap a field** to cycle its time window: 1 min → 5 min → 20 min → Full ride. Each field keeps its own window.
- **Default time window** is configurable in the app's preview screen (1 min / 5 min / 20 min / Full) and persists across rides.
- Power curve uses a 7-second rolling average (HR is not smoothed).

## Install

Download `karoo-power-graph-1.0.1-debug.apk` from the [Releases](../../releases) page.

> **OTA updates**: starting with 0.1.6, the Karoo OS itself checks for newer releases and offers a one-tap update on the device. You only need a manual sideload for the initial install (or to upgrade from 0.1.5 or earlier).

> The released APK is a **debug build** — signed with Android's generic debug key rather than a stable release key. This is standard practice for sideloaded Karoo extensions and means the file name ends in `-debug.apk`. It is fully functional; the `-debug` suffix is a packaging detail, not a quality signal. If the signing story changes in a future release, it will be noted here.

**Karoo 3:** share the APK link via the Companion app, or install via ADB over USB.
**Karoo 2:** install via ADB:

```
adb install -r karoo-power-graph-1.0.1-debug.apk
```

Then in the Karoo ride-page editor, open the data field picker, find **Karoo Power Graph**, and add **HR Zone Graph**, **Power Zone Graph**, and/or **Power Zone Graph (NP)** to a page.

## Uninstall

On the Karoo itself (no ADB needed):

1. Main menu → **Settings**
2. Scroll to the bottom and open **System Info / Android Settings** (exact label depends on firmware)
3. Open **Apps**
4. Select **Karoo Power Graph** from the list
5. Tap **Uninstall**

## Usage

- Tap the field to cycle the visible time window (1 min → 5 min → 20 min → Full).
- Zones come from the Karoo's own HR-zone and power-zone configuration (set under the rider profile).

## Known issues

- **Updating from 0.1.3 to any later version fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.** 0.1.3 was signed with a different debug keystore, so Android refuses the in-place update. **Workaround:** uninstall 0.1.3 first, then sideload the new version. Your existing data field placements on Karoo ride pages are preserved across the reinstall — the field IDs are unchanged. From 0.1.4 onward all builds use the same keystore, so 0.1.4 → 0.1.5 (and later) updates work normally.
- **Extension may restart on long rides (~4 h+).** On extended rides, Karoo can display "Application restarted" one or more times. The recording itself is unaffected; only the curve buffer is reset. Likely cause is memory pressure from per-frame bitmap allocations in the renderer. Tracked upstream in [#5](https://github.com/svenk0711/sk0711-graph/issues/5).

## Build from source

Requirements: Android Studio (Giraffe or later) and a GitHub Personal Access Token with `read:packages` scope — the `karoo-ext` SDK is hosted on GitHub Packages.

Add credentials to `local.properties`:

```
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

Then:

```
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/karoo-power-graph-1.0.1-debug.apk`.

## License

[Apache License 2.0](LICENSE).
