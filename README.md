<p align="center">
  <img src="app-icon.png" alt="Karoo Power Graph app icon" width="120">
</p>

# Karoo Power Graph

Karoo Power Graph is a Hammerhead Karoo extension for showing heart rate and power as compact, zone-coloured history graphs inside a ride data field.

This is Jamie Bishop's personal fork of [svenk0711/sk0711-graph](https://github.com/svenk0711/sk0711-graph). The original extension and Karoo data-field integration came from Sven's work; this fork keeps that foundation but changes the behaviour, styling, naming, package id, release assets, and graph presentation to suit my own Karoo setup.

Tested on Karoo 3. It should also work on Karoo 2.

## What It Adds

- **HR Zone Graph**: live heart rate with a zone-coloured history curve.
- **Power Zone Graph**: live 3-second smoothed power with AVG and MAX.
- **Power Zone Graph (NP)**: live 3-second smoothed power with AVG and Normalized Power, useful for time trials or pacing-focused pages.
- **Tap-to-cycle time windows**: 1 min -> 5 min -> 20 min -> Full ride.
- **Configurable default window** in the app preview screen.
- **Karoo-native stats** for AVG, MAX, and NP, read directly from Karoo streams so they match other Karoo data fields.
- **Karoo-native zones** for HR and power, using the rider profile's current zone setup.
- **Lower-density chart rendering** so short windows keep power detail without looking like a dense comb of one-second slices.
- **Compact field layout** for shorter Karoo data fields, with aligned time-window and AVG/MAX/NP columns.
- **Bright zone palette** tuned for quick reading on the Karoo screen.

## Current Behaviour

The large live power number uses Karoo's own `SMOOTHED_3S_AVERAGE_POWER` stream. It is not instant power and it is not calculated separately by this app.

AVG, MAX, and NP are also read from Karoo's own streams:

- `AVERAGE_HR`
- `MAX_HR`
- `AVERAGE_POWER`
- `MAX_POWER`
- `NORMALIZED_POWER`

The chart is tuned for readability without changing the live value:

- power shape is lightly smoothed only within continuous runs of the same zone, keeping colour and height aligned
- `1 min` remains detailed
- `5 min`, `20 min`, and `Full` draw fewer visual slices so the graph is less jagged on the Karoo display
- power graphs use a zero floor and a minimum 0-400W vertical scale to avoid exaggerating steady low-power riding

The app folds pauses out of the graph's time axis, so the curve continues from where the ride resumed instead of drawing a long interpolated line across a pause.

## Install

Download `karoo-power-graph-1.1-debug.apk` from the [Releases](../../releases) page.

Karoo OS can use the included OTA manifest for future updates. For the first install, sideload the APK.

Karoo 3:

- Share the APK link via the Hammerhead Companion app, or install via ADB over USB.

Karoo 2 / ADB:

```bash
adb install -r karoo-power-graph-1.1-debug.apk
```

Then open the Karoo ride-page editor, choose **Karoo Power Graph**, and add one of:

- **HR Zone Graph**
- **Power Zone Graph**
- **Power Zone Graph (NP)**

The released APK is a debug build signed with Android's debug key. That is normal for sideloaded Karoo extensions; the `-debug` suffix is a packaging detail.

## Usage

Tap a graph field during a ride to cycle:

```text
1 min -> 5 min -> 20 min -> Full
```

Each field keeps its own current window. The default starting window can be changed in the app's preview/settings screen.

## Build From Source

Requirements:

- Android Studio or the Android command-line build tools
- Java 21
- A GitHub Personal Access Token with `read:packages` scope, because Hammerhead's `karoo-ext` SDK is hosted on GitHub Packages

Add credentials to `local.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

Build:

```bash
./gradlew test :app:assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/karoo-power-graph-1.1-debug.apk
```

Generate the OTA manifest:

```bash
bash scripts/manifest.sh -o manifest.json
```

## Uninstall

On the Karoo:

1. Open **Settings**.
2. Open **System Info / Android Settings**.
3. Open **Apps**.
4. Select **Karoo Power Graph**.
5. Tap **Uninstall**.

## Credits

This project is based on [svenk0711/sk0711-graph](https://github.com/svenk0711/sk0711-graph) by Sven. The original project provided the starting point for the Karoo extension structure and graph data fields.

This fork changes the identity and behaviour enough to install separately and evolve independently.

## License

[Apache License 2.0](LICENSE).
