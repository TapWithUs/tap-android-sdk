---
name: tap-android-sdk
description: >-
  API reference for the TAP Android SDK (com.tapwithus.sdk): TapSdk,
  TapListener, modes, framed-protocol features, haptics. Use when you need a
  method, callback, or data-type detail. For a new app, use
  tap-android-getting-started first. Order: TapSdkFactory.getDefault(context),
  then registerTapListener, then resume() in onResume and pause() in onPause.
  getDefault() alone delivers no taps. There is no connect() or start() method.
---

# TAP Android SDK

SDK for receiving input from TAP wearable devices (TAP Strap, Tap Strap 2, TapXR) over BLE. TapBand uses the same BLE API as TapXR and is coming soon: https://www.tapwithus.com/tapband-waitlist/

Entry point: `com.tapwithus.sdk.TapSdk`. Published as `io.github.tapwithus:tap-android-sdk` on Maven Central. There is one SDK. Do not call it "SDK v1" or "SDK v2". Types in `com.tapwithus.sdk.v2` are the framed BLE protocol.

Current TapXR developer firmware reports the full gesture set: taps, thumb swipes, pinches with hold, fist with hold.

Task skills (connect, taps, air gestures, motion, knob, D-Pad, full apps) live next to this file under `plugins/tap-android-sdk/skills/`. Start with `tap-android-getting-started`. This file is the method-level reference.

## Required order

1. `TapSdk sdk = TapSdkFactory.getDefault(context);`
2. `sdk.registerTapListener(listener);` immediately, before the first connection callback.
3. `sdk.resume()` in `onResume`, `sdk.pause()` in `onPause`.
4. On `onTapConnected`: classic firmware is already in Controller mode while the app is resumed. On the framed protocol, Controller mode enables `MODEL_DETECTION` and `IMU_MOTION_DATA` but does not select a vision model. Call `sdk.startXRTappingState(tapIdentifier)` for taps or `sdk.startXRAirMouseState(tapIdentifier)` for air gestures.

`getDefault()` alone delivers no taps. There is no `connect()` or `start()`.

## Quick start

1. Add the dependency (requires `mavenCentral()`):

```groovy
implementation 'io.github.tapwithus:tap-android-sdk:0.3.6'
```

2. Declare Bluetooth permissions in `AndroidManifest.xml` and request
   `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN` at runtime (Android 12+):

```xml
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

3. Get an instance and register a listener:

```java
TapSdk sdk = TapSdkFactory.getDefault(context);
sdk.registerTapListener(tapListener); // implements com.tapwithus.sdk.TapListener
```

4. Forward lifecycle events (required for correct mode switching):

```java
@Override protected void onResume() { super.onResume(); sdk.resume(); }
@Override protected void onPause()  { super.onPause();  sdk.pause();  }
```

The SDK auto-connects to *already-paired* TAP devices — there is no scan/pair API;
users pair the TAP in Android Bluetooth settings first. On connect, while resumed,
the SDK switches the device to Controller Mode. `pause()` restores Text Mode
(regular keyboard behavior). On the framed protocol, Controller Mode does not
select a vision model; call `startXRTappingState` or `startXRAirMouseState` from
`onTapConnected`.

## Receiving input

Implement `TapListener`. Key callbacks:

```java
void onTapConnected(String tapIdentifier);        // tapIdentifier = BT address, used in all calls
void onTapDisconnected(String tapIdentifier);
void onTapInputReceived(String tapIdentifier, int data, int repeatData);
void onTapShiftSwitchReceived(String tapIdentifier, int data);
void onMouseInputReceived(String tapIdentifier, MousePacket data);
void onAirMouseInputReceived(String tapIdentifier, AirMousePacket data); // raw packet
void onRawSensorInputReceived(String tapIdentifier, RawSensorData rsData);
void onError(String tapIdentifier, int code, String description);
// default methods:
default void onXRAirGestureReceived(String tapIdentifier, XRAirGesture gesture);
default void onImuMotionInputReceived(String tapIdentifier, ImuMotionPacket packet);
default void onTapStandbyStateChanged(String tapIdentifier, boolean standby);
```

`onAirMouseInputReceived` is the raw characteristic value. Prefer `onXRAirGestureReceived` for pinches, drags, swipes, and fists. The SDK runs each packet through `GesturePostprocessor` (framed protocol, hardware other than 4.x.x) or `XRGesturesMain` (classic protocol, and framed-protocol hardware 4.x.x, after mapping the new class indexes onto the classic XR states).

### Decoding tap input

`data` is a 1–31 bitmask of tapped fingers (LSB = thumb ... bit 4 = pinky).
`repeatData` is 1/2/3 for single/double/triple tap (always 1 on the framed protocol).

```java
boolean[] fingers = TapSdk.toFingers(data);   // [thumb, index, middle, ring, pinky]
int[] ss = TapSdk.toShiftAndSwitch(shiftSwitchData); // ss[0]: 0=off,1=shift,2=locked; ss[1]: switch
```

## Input modes

Per-device, by `tapIdentifier`:

```java
sdk.startControllerMode(tapIdentifier);            // default; taps sent to app
sdk.startTextMode(tapIdentifier);                  // device acts as BT keyboard, no SDK input
sdk.startControllerWithMouseHIDMode(tapIdentifier);
sdk.startControllerWithFullHIDMode(tapIdentifier);
sdk.startRawSensorMode(tapIdentifier, devAccelSens, imuGyroSens, imuAccelSens); // bytes
```

Default mode for newly connected devices:

```java
sdk.setDefaultMode(TapInputMode.controller(), /* applyImmediate */ true);
```

`startMode`/`getMode`/`isInMode` are deprecated — use the methods above.

## Common operations

```java
Set<String> taps = sdk.getConnectedTaps();
Tap tap = sdk.getCachedTap(tapIdentifier);         // name, battery, fw/hw version
sdk.vibrate(tapIdentifier, new int[]{500, 100, 500}); // haptic ms: on,pause,on,...max 18
sdk.writeName(tapIdentifier, "MyTap");
sdk.enableDebug();                                 // verbose LogCat
sdk.close();                                       // release BT resources
```

## TapXR states (air mouse vs tapping)

```java
sdk.startXRTappingState(tapIdentifier);   // tapping detection only
sdk.startXRAirMouseState(tapIdentifier);  // air-mouse detection only
sdk.startXRUserControlState(tapIdentifier); // user switches freely
sdk.setDefaultXRState(TapXRState.tapping(), /* applyImmediate */ true);
```

## Framed protocol (`com.tapwithus.sdk.v2`)

Current TapXR developer firmware and TapBand use a framed BLE protocol. `TapSdk` detects it after connect — the callbacks and mode APIs above work unchanged. Check with `sdk.isV2Tap(tapIdentifier)`.

Raw class indexes still arrive in `onAirMouseInputReceived` (`UnifiedAirGesture`, codes 100–114). Processed gestures arrive in `onXRAirGestureReceived` as `com.tapwithus.sdk.xr.XRAirGesture` (`CLICK_INDEX`, `DRAG_INDEX`, `DROP`, `FIST_BEGIN`/`FIST_END`, swipes, and so on). Hardware 4.x.x framed Taps are converted from the new indexes (AB=105 … FIST=109) to the classic XR states before `XRGesturesMain`.

Framed-protocol configuration (all `get*` reply async on `TapV2Callback`; value is `null` on 2s timeout; calling on classic firmware raises error `TapSdk.ERR_V2_NOT_SUPPORTED` = 103):

```java
sdk.setFeature(id, DeviceFeature.MODEL_DETECTION, true); // RAW_IMU_DATA, IMU_MOTION_DATA, STANDBY_GESTURE_DETECTION...
sdk.setVisionSensorModel(id, VisionSensorModel.AIR_GESTURE); // or TAPPING
sdk.setVisionSensorOpMode(id, VisionSensorOpMode.STREAM);    // or TRIGGER / STREAM_ON_TRIGGER
sdk.setImuSensitivity(id, /*gyro 0-5*/ 3, /*accel 0-4*/ 2);
sdk.setStandbyState(id, true);
sdk.sendKeepAlive(id); // sent automatically every 10s anyway
```

## Pitfalls

- Not calling `sdk.resume()`/`sdk.pause()` breaks Controller/Text mode switching. While paused, a new connection does not notify `onTapConnected`.
- No scan API: devices must be paired in system Bluetooth settings before the SDK sees them.
- In Text Mode the app receives **no** tap input — that is by design.
- On the framed protocol, Controller mode alone does not select `TAPPING` vs `AIR_GESTURE`. Call `startXRTappingState` or `startXRAirMouseState`.
- Raw sensor mode on the framed protocol streams only the thumb IMU (no finger accelerometers).
- `repeatData` (double/triple tap) is always 1 on the framed protocol.
- Callbacks are not guaranteed to be on the main thread.

## Additional resources

- Full API listing and data-type details: [reference.md](reference.md)
- Task skills: `tap-android-getting-started`, `tap-android-tapping`, `tap-android-vision-models`, `tap-android-imu-motion`, `tap-android-raw-sensors`, `tap-android-knob`, `tap-android-dpad`, `tap-android-build-an-app`
- Working example: the `app` module (`app/src/main/java/com/tapwithus/tapsdk/MainActivity.java`)
- Project README covers the same ground with more prose: `README.md`
