# Tap Android SDK

Connect order, every time: `TapSdkFactory.getDefault(context)`, then `registerTapListener(...)` immediately, then `resume()` from `onResume` and `pause()` from `onPause`. `getDefault()` alone delivers no taps. There is no `connect()` or `start()` method. The SDK connects to Taps that are already paired in Android Bluetooth settings.

> Package: `implementation 'io.github.tapwithus:tap-android-sdk:0.3.6'` (Maven Central)
> Skills: https://github.com/TapWithUs/tap-android-sdk/tree/master/plugins/tap-android-sdk/skills
> Example app: `app/src/main/java/com/tapwithus/tapsdk/MainActivity.java`

BLE SDK for Tap Strap, Tap Strap 2, and TapXR. TapBand uses the **same BLE API** as TapXR. TapBand is coming soon (it is not for sale yet): https://www.tapwithus.com/tapband-waitlist/

The current TapXR developer firmware reports the **full gesture set**: taps, thumb swipes (up, down, left, right), thumb-to-finger pinches with hold, and fist with hold. Do not describe TapXR as a subset of gestures. Do not invent a firmware version number.

One SDK. Do not call it "SDK v1" or "SDK v2". Method names such as `isV2Tap` and types in `com.tapwithus.sdk.v2` are the framed BLE protocol, not a second SDK.

## Devices and protocol

`TapSdk` is one entry point. After connect it detects the BLE protocol:

- **Classic protocol** (`sdk.isV2Tap(id) == false`): Tap Strap, Tap Strap 2, and older TapXR firmware. Input modes such as `startControllerMode`.
- **Framed protocol** (`sdk.isV2Tap(id) == true`): current TapXR developer firmware, and TapBand when it ships. Feature switches, vision models (`TAPPING` / `AIR_GESTURE`), IMU motion with roll / pitch / yaw.

`tapIdentifier` is the Bluetooth address from `onTapConnected`. Write code that handles both protocols unless the user names the device. One `TapSdk` can serve several Taps at once.

## Required order

```java
TapSdk sdk = TapSdkFactory.getDefault(context);   // 1. instance; auto-connects paired Taps
sdk.registerTapListener(listener);                // 2. register before events arrive
// 3. lifecycle — without this, backgrounding leaves the Tap in Text mode
@Override protected void onResume() { super.onResume(); sdk.resume(); }
@Override protected void onPause()  { super.onPause();  sdk.pause();  }
// 4. from onTapConnected, enable the input you want:
//    Classic protocol: Controller mode is applied automatically while resumed.
//    Framed protocol: Controller mode enables MODEL_DETECTION + IMU_MOTION_DATA
//    but does not pick a vision model. Call startXRTappingState or startXRAirMouseState.
```

## Raw gestures and the Android processed layer

Every Tap SDK delivers **raw** gesture detections from the sensor on `onAirMouseInputReceived`. Decode framed-protocol codes with `UnifiedAirGesture.fromCode(...)` (100–114): swipes, pinches `AB`/`AC`/`AD`/`AE`, fist, and `_HOLD` variants. Gate on `sdk.isV2Tap(id)` before that decode — codes 100–102 also exist on classic TapXR packets.

Android also emits a **processed** air-gesture stream on `onXRAirGestureReceived` (`com.tapwithus.sdk.xr.XRAirGesture`): per-finger click, drag, drop, fist begin/end, and swipe. The SDK debounce-filters the raw stream and latches holds. Prefer this callback for UI actions. Keep the raw callback when you need the same events other SDKs see.

## Callbacks

Implement `com.tapwithus.sdk.TapListener`. The same methods fire for both protocols.

| Callback | Payload |
|----------|---------|
| `onTapConnected` / `onTapDisconnected` | `tapIdentifier` (Bluetooth address) |
| `onTapInputReceived` | `data` 1–31 finger bitmask, `repeatData` 1/2/3 (always 1 on the framed protocol) |
| `onAirMouseInputReceived` | Raw `AirMousePacket`. Framed: `UnifiedAirGesture.fromCode(gesture)`. Classic: `AIR_MOUSE_GESTURE_*` |
| `onXRAirGestureReceived` | Processed `XRAirGesture` (click, drag, drop, fist, swipe). Default method. |
| `onMouseInputReceived` | `MousePacket` `dx`, `dy`, `proximity` |
| `onImuMotionInputReceived` | Framed `ImuMotionPacket` `dx`, `dy`, `isMouse`, `roll`, `pitch`, `yaw` |
| `onRawSensorInputReceived` | `RawSensorData` |
| `onTapStandbyStateChanged` | Framed-protocol standby boolean |
| `onError` | code + description. `ERR_V2_NOT_SUPPORTED` (103) if a framed-protocol API is used on classic firmware |

Tapcode bits: thumb = 1, index = 2, middle = 4, ring = 8, pinky = 16. `TapSdk.toFingers(data)`.

Callbacks are not guaranteed to be on the main thread. Keep them short. Post UI work with a main-thread `Handler`.

## Gotchas

- Classic firmware boots in **Text mode** (the Tap types on the OS keyboard and the SDK gets no taps). On connect, while the app is resumed, the SDK switches to Controller mode. `pause()` switches back to Text mode.
- Framed-protocol Controller mode enables `MODEL_DETECTION` and `IMU_MOTION_DATA` only. Call `startXRTappingState` (`TAPPING` + `TRIGGER`) or `startXRAirMouseState` (`AIR_GESTURE` + `STREAM`) after connect, or set the vision model yourself.
- The framed protocol runs one vision model at a time.
- There is no scan or pair API. Pair the Tap in system Bluetooth settings first. On Android 12+ request `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` at runtime.
- In Text mode the app receives **no** tap input.
- Framed-protocol `repeatData` is always 1. Detect double taps with a time window. On classic firmware, `repeatData` is 1, 2, or 3.
- Framed-protocol raw mode streams the thumb IMU only (no finger accelerometers).
- Haptics: `sdk.vibrate(id, new int[]{onMs, offMs, ...})`. Each value is clamped to 10–2500 ms. Max 18 values.
- Framed-protocol `get*` calls are async (`TapV2Callback`). The value is null on a 2 second timeout.
- Do not invent APIs, characteristic UUIDs, or enum values. Check `com.tapwithus.sdk` and `com.tapwithus.sdk.v2`.
- Update firmware with the Tap Manager app. Classic raw sensors need Developer mode in Tap Manager.
- Test with the real device and ask the user what they see. There is no simulator.

## Skills

| Skill | Use for |
|-------|---------|
| `tap-android-getting-started` | Gradle dependency, permissions, pairing, connect, quickstart, troubleshooting |
| `tap-android-tapping` | tapcodes, finger combos, double taps, haptics, input modes |
| `tap-android-vision-models` | vision-model switching, raw `UnifiedAirGesture`, processed `XRAirGesture` |
| `tap-android-imu-motion` | pointer, tilt, roll / pitch / yaw, classic mouse packets |
| `tap-android-raw-sensors` | raw accelerometer / gyro, sensitivity |
| `tap-android-knob` | pinch-hold + twist to change a value (framed protocol) |
| `tap-android-dpad` | swipes, pinch select, hold to rotate or drag (framed protocol) |
| `tap-android-build-an-app` | complete Android apps: listener, main thread, connection status |
| `tap-android-sdk` | method-by-method API reference |

Skill files live in `plugins/tap-android-sdk/skills/<name>/SKILL.md` in the SDK repository. `./install-skills.sh` copies them into an app project.

## Contributing to this repository

Only for work on the SDK itself (not for apps that use it):

- Unit tests: `./gradlew :tap-android-sdk:test` (requires the Android SDK).
- When the public API changes, update the matching skill in `plugins/tap-android-sdk/skills/` and this file.
- Do not invent BLE UUIDs or enum values. Framed-protocol frame layout is covered by `TapV2EncoderTest`.
- `.cursor/skills/` and `.claude/skills/` in this repository are symlinks into `plugins/tap-android-sdk/skills/`. Run `./install-skills.sh` in an app project. Running it in this checkout copies over those links.
