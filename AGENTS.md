# Tap Android SDK

Connect order, every time: `TapSdkFactory.getDefault(context)`, then `registerTapListener(...)` immediately, then `resume()` from `onResume` and `pause()` from `onPause`. `getDefault()` alone delivers no taps. There is no `connect()` or `start()` method. The SDK connects to Taps that are already paired in Android Bluetooth settings.

> Package: `implementation 'io.github.tapwithus:tap-android-sdk:0.3.6'` (Maven Central)
> Skills: https://github.com/TapWithUs/tap-android-sdk/tree/master/plugins/tap-android-sdk/skills
> Example app: `app/src/main/java/com/tapwithus/tapsdk/MainActivity.java`

BLE SDK for Tap Strap, Tap Strap 2, TapXR, and TapBand. It receives taps, air gestures, mouse / IMU motion, and raw sensor data, and sends haptics and mode commands.

## Two protocols, one entry point

`TapSdk` detects the firmware protocol after connect:

- **v1** (classic firmware): input modes (`startControllerMode`, `startTextMode`, `startControllerWithMouseHIDMode`, `startControllerWithFullHIDMode`, `startRawSensorMode`)
- **v2** (framed firmware, TapBand and TapXR with V2 firmware): `DeviceFeature` switches, vision models (`VisionSensorModel.TAPPING` / `AIR_GESTURE`), IMU motion with roll / pitch / yaw

Check with `sdk.isV2Tap(tapIdentifier)`. `tapIdentifier` is the Bluetooth address from `onTapConnected`. Write code that handles both unless the user names the device. One `TapSdk` can serve several Taps at once.

## Required order

```java
TapSdk sdk = TapSdkFactory.getDefault(context);   // 1. instance; auto-connects paired Taps
sdk.registerTapListener(listener);                // 2. register before events arrive
// 3. lifecycle — without this, backgrounding leaves the Tap in Text mode
@Override protected void onResume() { super.onResume(); sdk.resume(); }
@Override protected void onPause()  { super.onPause();  sdk.pause();  }
// 4. from onTapConnected, enable the input you want:
//    v1 Controller mode is applied automatically.
//    v2 Controller mode enables MODEL_DETECTION + IMU_MOTION_DATA but does not
//    pick a vision model. Call startXRTappingState or startXRAirMouseState.
```

## Callbacks

Implement `com.tapwithus.sdk.TapListener`. The same methods fire for v1 and v2.

| Callback | Payload |
|----------|---------|
| `onTapConnected` / `onTapDisconnected` | `tapIdentifier` (Bluetooth address) |
| `onTapInputReceived` | `data` 1–31 finger bitmask, `repeatData` 1/2/3 (always 1 on v2) |
| `onAirMouseInputReceived` | `AirMousePacket`. v2: `UnifiedAirGesture.fromCode(gesture)`. v1: `AIR_MOUSE_GESTURE_*` |
| `onMouseInputReceived` | `MousePacket` `dx`, `dy`, `proximity` |
| `onImuMotionInputReceived` | v2 `ImuMotionPacket` `dx`, `dy`, `isMouse`, `roll`, `pitch`, `yaw` |
| `onRawSensorInputReceived` | `RawSensorData` |
| `onTapStandbyStateChanged` | v2 standby boolean |
| `onError` | code + description. `ERR_V2_NOT_SUPPORTED` (103) if a v2 API is used on v1 |

Tapcode bits: thumb = 1, index = 2, middle = 4, ring = 8, pinky = 16. `TapSdk.toFingers(data)`.

Callbacks are not guaranteed to be on the main thread. Keep them short. Post UI work with a main-thread `Handler`.

## Gotchas

- v1 boots in **Text mode** (the Tap types on the OS keyboard and the SDK gets no taps). On connect, while the app is resumed, the SDK switches to Controller mode. `pause()` switches back to Text mode.
- v2 Controller mode enables `MODEL_DETECTION` and `IMU_MOTION_DATA` only. Call `startXRTappingState` (`TAPPING` + `TRIGGER`) or `startXRAirMouseState` (`AIR_GESTURE` + `STREAM`) after connect, or set the vision model yourself.
- v2 runs one vision model at a time.
- There is no scan or pair API. Pair the Tap in system Bluetooth settings first. On Android 12+ request `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` at runtime.
- In Text mode the app receives **no** tap input.
- v2 `repeatData` is always 1. Detect double taps with a time window on v2. On v1, `repeatData` is 1, 2, or 3.
- v2 raw mode streams the thumb IMU only (no finger accelerometers).
- Haptics: `sdk.vibrate(id, new int[]{onMs, offMs, ...})`. Each value is clamped to 10–2500 ms. Max 18 values.
- `get*` V2 calls are async (`TapV2Callback`). The value is null on a 2 second timeout.
- Do not invent APIs, characteristic UUIDs, or enum values. Check `com.tapwithus.sdk` and `com.tapwithus.sdk.v2`.
- Update firmware with the Tap Manager app. v1 raw sensors need Developer mode in Tap Manager.
- Test with the real device and ask the user what they see. There is no simulator.

## Skills

| Skill | Use for |
|-------|---------|
| `tap-android-getting-started` | Gradle dependency, permissions, pairing, connect, quickstart, troubleshooting |
| `tap-android-tapping` | tapcodes, finger combos, double taps, haptics, input modes |
| `tap-android-vision-models` | v2 model switching, `UnifiedAirGesture` (swipe, pinch, hold, fist) |
| `tap-android-imu-motion` | pointer, tilt, roll / pitch / yaw, v1 mouse |
| `tap-android-raw-sensors` | raw accelerometer / gyro, sensitivity |
| `tap-android-knob` | v2 pinch-hold + twist to change a value |
| `tap-android-dpad` | v2 swipes, pinch select, hold to rotate or drag |
| `tap-android-build-an-app` | complete Android apps: listener, main thread, connection status |
| `tap-android-sdk` | method-by-method API reference |

Skill files live in `plugins/tap-android-sdk/skills/<name>/SKILL.md` in the SDK repository. `./install-skills.sh` copies them into an app project.

## Contributing to this repository

Only for work on the SDK itself (not for apps that use it):

- Unit tests: `./gradlew :tap-android-sdk:test` (requires the Android SDK).
- When the public API changes, update the matching skill in `plugins/tap-android-sdk/skills/` and this file.
- Do not invent BLE UUIDs or enum values. V2 frame layout is covered by `TapV2EncoderTest`.
- `.cursor/skills/` and `.claude/skills/` in this repository are symlinks into `plugins/tap-android-sdk/skills/`. Run `./install-skills.sh` in an app project. Running it in this checkout copies over those links.
