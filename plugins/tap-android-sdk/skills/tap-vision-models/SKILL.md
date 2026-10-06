---
name: tap-vision-models
description: >-
  Switch a Tap v2 vision model on Android between TAPPING and AIR_GESTURE, set
  the op mode, and decode UnifiedAirGesture (swipes, pinches, holds, fist).
  Use for air gestures, pinch, swipe, fist, model switching, or v1
  AirMousePacket gestures. Requires tap-getting-started order:
  getDefault, registerTapListener, resume and pause.
---

# Vision models and air gestures

**v2** (`sdk.isV2Tap(id)`) for model switching. v1 air gestures are at the end.

The v2 device runs one vision model at a time. Gestures arrive on `onAirMouseInputReceived`. Decode with `UnifiedAirGesture.fromCode(data.gesture.getInt())`. That returns null for classic (v1) codes.

| `VisionSensorModel` | Output | Callback |
|---------------------|--------|----------|
| `TAPPING` | finger taps | `onTapInputReceived(id, data, repeatData)` |
| `AIR_GESTURE` | swipes, pinches, holds, fist | `onAirMouseInputReceived` with codes 100–114 |

| `VisionSensorOpMode` | Meaning | Use with |
|----------------------|---------|----------|
| `TRIGGER` | Run the model when a tap-like trigger occurs | `TAPPING` |
| `STREAM` | Run the model all the time | `AIR_GESTURE` |
| `STREAM_ON_TRIGGER` | Start streaming after a trigger | experiments |

## Enable and switch

Prefer the XR-state helpers. They map onto the vision sensor on v2:

```java
sdk.startXRTappingState(id);    // TAPPING + TRIGGER
sdk.startXRAirMouseState(id);   // AIR_GESTURE + STREAM
sdk.startXRUserControlState(id); // user switches on the device
```

Direct control (v2 only; on a v1 device `onError` fires with `TapSdk.ERR_V2_NOT_SUPPORTED` = 103):

```java
sdk.setFeature(id, DeviceFeature.MODEL_DETECTION, true);
sdk.setVisionSensorModel(id, VisionSensorModel.AIR_GESTURE);
sdk.setVisionSensorOpMode(id, VisionSensorOpMode.STREAM);
```

Call these after `onTapConnected`. You can switch at runtime. Reads are async and null on a 2 second timeout:

```java
sdk.getVisionSensorModel(id, (identifier, model) -> { /* model may be null */ });
sdk.getVisionSensorOpMode(id, (identifier, opMode) -> { });
```

Controller mode on connect enables `MODEL_DETECTION` but leaves the vision model unset (`TapXRState.none()` is the default). If taps never arrive on v2, call `startXRTappingState`.

## UnifiedAirGesture codes

Letters: A = thumb, B = index, C = middle, D = ring, E = pinky. "AB" = thumb touches index (pinch).

| Code | Name | Meaning |
|------|------|---------|
| 100 | `NONE` | No gesture / hand relaxed. Use it as "release" after a hold. |
| 101 | `LEFT` | Swipe left |
| 102 | `RIGHT` | Swipe right |
| 103 | `UP` | Swipe up |
| 104 | `DOWN` | Swipe down |
| 105–108 | `AB` / `AC` / `AD` / `AE` | Short pinch: thumb + index / middle / ring / pinky |
| 109 | `FIST` | Fist |
| 110–113 | `AB_HOLD` … `AE_HOLD` | Pinch held |
| 114 | `FIST_HOLD` | Fist held |

```java
@Override
public void onAirMouseInputReceived(String id, AirMousePacket data) {
    if (!sdk.isV2Tap(id)) {
        return;
    }
    UnifiedAirGesture gesture = UnifiedAirGesture.fromCode(data.gesture.getInt());
    if (gesture == UnifiedAirGesture.LEFT) {
        goBack();
    }
}
```

In `STREAM` mode the same code repeats many times per second. Act when the code **changes**, or ignore repeats within about 40 ms. Holds repeat until the hand relaxes and `NONE` (100) arrives. Wait for a few `NONE` packets in a row before you treat a hold as released (see `tap-dpad`).

## Switch models from a gesture

Fist-hold switches to tapping. All-five-finger tap (31) switches back. Do the mode write off the hot path if you also update UI; `setVisionSensorModel` itself only queues a BLE write.

```java
if (gesture == UnifiedAirGesture.FIST_HOLD) {
    sdk.startXRTappingState(id);
}
// in onTapInputReceived, when data == 31:
sdk.startXRAirMouseState(id);
```

## Standby

`DeviceFeature.STANDBY_GESTURE_DETECTION` lets the user put the device in standby with a gesture. `onTapStandbyStateChanged(id, standby)` reports changes. `sdk.setStandbyState(id, false)` wakes it. `sdk.getStandbyState(id, callback)` reads it (null on timeout).

## v1 air gestures (TapXR / Tap Strap 2 in Controller mode)

v1 has no vision-model API. In Controller mode, `onAirMouseInputReceived` uses `AirMousePacket` constants:

`AIR_MOUSE_GESTURE_NONE` (0), `GENERAL` (1), `UP` / `DOWN` / `LEFT` / `RIGHT` and the `_TWO_FINGERS` variants, `INDEX_TO_THUMB_TOUCH` (10), `MIDDLE_TO_THUMB_TOUCH` (11).

TapXR continuous hand states (several times a second; take the majority of the last 3): `XR_AIR_GESTURE_NONE` (100), `XR_AIR_GESTURE_THUMB_INDEX` (101), `XR_AIR_GESTURE_THUMB_MIDDLE` (102). These are not the v2 `UnifiedAirGesture` codes. Gate on `sdk.isV2Tap(id)` before calling `UnifiedAirGesture.fromCode`.

`onTapChangedState(id, state)` fires when an air-mouse packet has gesture 20. `state` is the device byte (`AirMousePacket.state`). The SDK treats `1` as air-mouse and then drops tap callbacks. That value is not `TapXRState.AIR_MOUSE` (4).

Force the modality with `startXRTappingState`, `startXRAirMouseState`, or `startXRUserControlState`. That is the Android equivalent of choosing air-mouse versus tapping. There is no separate `setInputType` method.
