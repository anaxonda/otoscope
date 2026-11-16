<h1 align="center">
<a href="https://github.com/rbeilvert/otoscope/"><img align="center" src="fastlane/metadata/android/en-US/images/icon.png" alt="otoscope" width="100" /></a><br>
Otoscope LED — JEGOAT fork
</h1>

<p align="center">
    A FOSS Android app for cheap Wi-Fi otoscope cameras.
</p>

---

## What this fork changes

This fork of [rbeilvert/otoscope](https://github.com/rbeilvert/otoscope) adds the JEGOAT ring-light command documented in [upstream issue #3](https://github.com/rbeilvert/otoscope/issues/3). It is based on upstream commit [`02baa4d`](https://github.com/rbeilvert/otoscope/commit/02baa4d).

- **Light on when connecting:** each new JEGOAT streaming session requests 100% LED brightness.
- **On/off control:** open **Options → Ring light** while streaming. The switch sends 100% for on and 0% for off; this build does not add a brightness slider.
- **Camera Wi-Fi routing:** LED commands use the session's existing Wi-Fi-bound control socket, alongside telemetry. No root access, SSH, or ADB commands are needed for normal use.
- **Distinct app name:** the installed app is called **Otoscope LED**. The downloadable debug APK uses package `dev.rubec.otoscope.debug`, so it installs alongside the original app.

JEGOAT telemetry does not report LED state, so the switch shows the requested state rather than a hardware-confirmed reading. Reconnecting starts with the light on again.

### Download and install

Download [`app-debug.apk`](https://github.com/anaxonda/otoscope/releases/download/v0.11.1-jegoat-led/app-debug.apk) from the [JEGOAT LED release](https://github.com/anaxonda/otoscope/releases/tag/v0.11.1-jegoat-led). This is a debug-signed build, version `0.11.1-debug`.

Install the APK on Android 10 or newer, open **Otoscope LED**, choose **Pair via Bluetooth**, select your `softish-…` camera, and approve the camera Wi-Fi connection. The light turns on when streaming starts. For USB installation:

```bash
adb install -r app-debug.apk
```

APK SHA-256:

```text
58367bca7d065f7b11048fe74a7265f5bf9115e08beb235bbd57672a899a8954
```

### Protocol and validation

The LED packet is `10 03 LL HH` sent over UDP to the camera's control port, normally `192.168.1.1:61500`. `LL HH` is a little-endian uint16 brightness from 0 to 100. This fork uses `10 03 64 00` for on and `10 03 00 00` for off.

Tested on a JEGOAT reporting `txw810`, firmware v111, with a Samsung Galaxy S10e (`SM-G970F`). The 50% command was physically confirmed to turn the LEDs on. The installed patched app streamed video, sent its initial 100% command, and sent 0%/100% commands through the new Ring light switch. Other camera families were not hardware-tested for this change.

Build and regression checks passed: `assembleDebug`, `testDebugUnitTest` (**129 tests, zero failures**), and `git diff --check`. The release contains the same APK installed during this test.

To build from source with JDK 17 and Android SDK API 37:

```bash
./gradlew assembleDebug testDebugUnitTest --no-daemon --max-workers=1 \
  -Dorg.gradle.parallel=false \
  '-Dorg.gradle.jvmargs=-Xmx1024m -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8' \
  -Pkotlin.compiler.execution.strategy=in-process
```

The source change is in [`JegoatSession.kt`](app/src/main/kotlin/dev/rubec/otoscope/stream/jegoat/JegoatSession.kt); the existing generic Ring light UI is reused. The app-name change is in [`strings.xml`](app/src/main/res/values/strings.xml). Upstream functionality and its GPL-3.0-or-later license are retained.

## Upstream app

Otoscope is a reverse-engineered drop-in replacement for the proprietary companion apps these cameras ship with, with none of their ad SDKs, analytics, or trackers.

- Discovers the camera either over Bluetooth Low Energy or by scanning nearby Wi-Fi, depending on the model.
- Joins the camera's Wi-Fi access point in an isolated, process-bound network. No impact on your saved Wi-Fi config.
- Streams live video over the camera's native protocol — UDP or RTSP MJPEG — decoded in pure Kotlin. No proprietary `.so`, no FFmpeg dependency.
- Auto-rotates the image using the camera's on-board accelerometer / gyro, clipped to a circular mask matching the otoscope lens.
- Horizontal-mirror toggle for self-examination, and a switch to turn auto-rotation off.
- Fullscreen view of the camera image; the stream options fold away so the image keeps most of the screen.
- Ring-light on/off toggle and charging indicator on models that expose them.
- Photo and video capture, written straight to `Pictures/Otoscope` and `Movies/Otoscope`, with an in-app gallery to review, share and delete them.
- Optional free-text caption (e.g. "Left ear") burnt into the corner of every capture.

## Hardware compatibility

The app supports a set of camera families across two discovery paths.
Pick your family on the home screen; the app scans the right way and hands the video off to the vendor-specific protocol automatically.

| Family        | Discovery | SSID prefix                      | Wi-Fi auth | Video | Companion app |
|---------------|-----------|----------------------------------| ---------- | ----- | ------------- |
| **Xylla**     | BLE       | `Enjoy-XXXXXX`<br>`JesHome-XXXX` | open | UDP/8032 MJPEG | "AIR-Look" (`com.air.airlook`) |
| **iTiMO**     | BLE       | `iTiMO-XXXXXX`<br>`jetion_XXXX`    | open | UDP/8031 MJPEG | "iTiMO" (`com.molink.john.itimo`) |
| **JEGOAT**    | BLE       | `softish-XXXXXX`                 | WPA2 | UDP/61501 MJPEG | "EarVision" (`com.atomath.wifi_camera`) |
| **EarFairy**  | Wi-Fi     | `Cooleer_XXXXXX`                 | open | RTSP/7070 MJPEG | "Cooleer" (`com.cooingdv.cooleer`) |
| **Soulear** (i4season) | Wi-Fi | `Soulear-XXXXX`           | open | UDP MJPEG, control on 10005/10006 | "Soulear" (`com.i4season.bkCamera_soulear`) |
| **Y38**       | Wi-Fi     | `AIR-ES-XXXXXX`                  | open | UDP/8032 MJPEG (Xylla-compatible) | "AIR-Look" (`com.air.airlook`) |

If you have a wireless otoscope that isn't picked up by Otoscope, please file an issue including preferably
the original companion application ID, a screenshot of the BLE advertisement (e.g. from nRF Connect),
the Wi-Fi SSID if available and any scan/connect logs from `adb logcat`.

## Build

Requires Android Studio Ladybug+ or a CLI Android SDK with API 37 + JDK 17.

```bash
# Bootstrap the Gradle wrapper once. If you have any modern Gradle:
gradle wrapper --gradle-version 9.6.1 --distribution-type bin

# Debug build (no signing required):
./gradlew assembleDebug

# Install on a connected device:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Status

| Feature                    | State                                   |
| -------------------------- |-----------------------------------------|
| BLE discovery              | ✅ for Xylla, iTiMO, JEAGOAT             |
| Wi-Fi discovery            | ✅ for EarFairy, Soulear, Y38            |
| Wi-Fi join                 | ✅ open or WPA2                          |
| Live video                 | ✅ MJPEG decoded in-app (UDP or RTSP)    |
| Auto-rotate + circular mask | ✅ driven by camera accelerometer / gyro |
| Horizontal-mirror toggle   | ✅ for self-examination                  |
| Screen stays awake         | ✅ during live stream                    |
| Battery + model readout    | ✅ where the camera exposes it           |
| Photo / video capture      | ✅ JPEG stills and H.264 MP4 clips        |
| Capture gallery            | ✅ in-app review, share and delete       |
| Brightness control    | 🚧 On-Off implemented for EarFairy, Soulear, JEGOAT |

## License

[GPL-3.0-or-later](LICENSE). The project is independent of and not affiliated with any of the OEMs or companion-app publishers. All trademark references are nominative.
