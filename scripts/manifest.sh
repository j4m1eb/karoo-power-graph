#!/usr/bin/env bash
# Generate manifest.json for a Karoo OTA release.
#
# Reads versionName + versionCode from app/build.gradle.kts and writes a
# KarooAppManifest-shaped JSON to stdout (or to a file if -o is given).
#
# Use after `gh release create`:
#   ./scripts/manifest.sh -o /tmp/manifest.json
#   gh release upload v$(VER) /tmp/manifest.json
#
# Karoo OS (since karoo-ext 1.1.1) periodically fetches
# https://github.com/<owner>/<repo>/releases/latest/download/manifest.json
# (configured via MANIFEST_URL in AndroidManifest.xml), compares
# latestVersionCode against the installed versionCode, and offers an in-OS
# update when a newer one is available.
#
# Schema: io.hammerhead.karooext.models.KarooAppManifest

set -euo pipefail

REPO="svenk0711/sk0711-graph"
GRADLE="app/build.gradle.kts"

OUTFILE=""
if [ "${1:-}" = "-o" ] && [ -n "${2:-}" ]; then
    OUTFILE="$2"
fi

if [ ! -f "$GRADLE" ]; then
    echo "error: $GRADLE not found — run from project root" >&2
    exit 1
fi

VERSION_NAME=$(grep -oE 'versionName = "[^"]+"' "$GRADLE" | sed -E 's/versionName = "(.+)"/\1/')
VERSION_CODE=$(grep -oE 'versionCode = [0-9]+' "$GRADLE" | sed -E 's/versionCode = ([0-9]+)/\1/')

if [ -z "$VERSION_NAME" ] || [ -z "$VERSION_CODE" ]; then
    echo "error: could not parse versionName / versionCode from $GRADLE" >&2
    exit 1
fi

APK_NAME="sk0711-graph-${VERSION_NAME}-debug.apk"
APK_URL="https://github.com/${REPO}/releases/download/v${VERSION_NAME}/${APK_NAME}"
ICON_URL="https://github.com/${REPO}/raw/master/app-icon.png"
SAMPLE_URL="https://github.com/${REPO}/raw/master/sample1.jpg"

JSON=$(cat <<EOF
{
  "label": "sk0711-graph",
  "packageName": "com.sk0711.graph",
  "latestApkUrl": "${APK_URL}",
  "latestVersion": "${VERSION_NAME}",
  "latestVersionCode": ${VERSION_CODE},
  "iconUrl": "${ICON_URL}",
  "developer": "svenk0711",
  "description": "Karoo extension: HR and Power as zone-coloured history curves with current value, AVG and MAX or NP. Configurable default time window; tap a field to cycle 1 min / 5 min / Full.",
  "releaseNotes": "See https://github.com/${REPO}/releases/tag/v${VERSION_NAME}",
  "screenshotUrls": ["${SAMPLE_URL}"]
}
EOF
)

if [ -n "$OUTFILE" ]; then
    printf "%s\n" "$JSON" > "$OUTFILE"
    echo "wrote $OUTFILE (version $VERSION_NAME / code $VERSION_CODE)"
else
    printf "%s\n" "$JSON"
fi
