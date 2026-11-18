---
name: tap-android-vision-models
description: >-
  Switch a Tap vision model on Android between TAPPING and AIR_GESTURE, decode
  raw UnifiedAirGesture (swipes, pinches, holds, fist), and handle processed
  XRAirGesture (click, drag, drop, fist, swipe). Use for air gestures, pinch,
  swipe, fist, model switching, or classic AirMousePacket gestures. Requires
  tap-android-getting-started order: getDefault, registerTapListener, resume
  and pause.
---

# Vision models and air gestures

Current TapXR developer firmware reports the **full gesture set** (the same set TapBand uses): taps, thumb swipes up/down/left/right, thumb-to-finger pinches with hold, and fist with hold. TapBand uses the same BLE API as TapXR and is coming soon: https://www.tapwithus.com/tapband-waitlist/

Vision-model switching needs the framed protocol (`sdk.isV2Tap(id)`). Classic-protocol air gestures are at the end.

The device runs one vision model at a time.

| `VisionSensorModel` | Output | Callback |
|---------------------|--------|----------|
| `TAPPING` | finger taps | `onTapInputReceived(id, data, repeatData)` |
| `AIR_GESTURE` | swipes, pinches, holds, fist | raw `onAirMouseInputReceived` (codes 100–114) and processed `onXRAirGestureReceived` |

| `VisionSensorOpMode` | Meaning | Use with |
|----------------------|---------|----------|
| `TRIGGER` | Run the model when a tap-like trigger occurs | `TAPPING` |
| `STREAM` | Run the model all the time | `AIR_GESTURE` |
| `STREAM_ON_TRIGGER` | Start streaming after a trigger | experiments |

## Enable and switch

Prefer the XR-state helpers. They map onto the vision sensor on the framed protocol:

```java
sdk.startXRTappingState(id);    // TAPPING + TRIGGER
sdk.startXRAirMouseState(id);   // AIR_GESTURE + STREAM
sdk.startXRUserControlState(id); // user switches on the device
```

Direct control (framed protocol only; on classic firmware `onError` fires with `TapSdk.ERR_V2_NOT_SUPPORTED` = 103):

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

Controller mode on connect enables `MODEL_DETECTION` but leaves the vision model unset (`TapXRState.none()` is the default). If taps never arrive on framed firmware, call `startXRTappingState`.

## Raw detections (all Tap SDKs)

Every Tap SDK delivers **raw** gesture detections from the sensor. On Android they arrive on `onAirMouseInputReceived`. Decode with `UnifiedAirGesture.fromCode(data.gesture.getInt())`. **Gate on `sdk.isV2Tap(id)` first** — codes 100–102 also exist on classic TapXR packets (`XR_AIR_GESTURE_NONE` / `THUMB_INDEX` / `THUMB_MIDDLE`) and must not be read as `UnifiedAirGesture`.

Letters: A = thumb, B = index, C = middle, D = ring, E = pinky. "AB" = thumb touches index (pinch).

| Code | Name | Meaning |
|------|------|---------|
| 100 | `NONE` | No gesture / hand relaxed. Use it as "release" after a hold. |
| 101 | `LEFT` | Thumb swipe left |
| 102 | `RIGHT` | Thumb swipe right |
| 103 | `UP` | Thumb swipe up |
| 104 | `DOWN` | Thumb swipe down |
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

Kotlin:

```kotlin
override fun onAirMouseInputReceived(id: String, data: AirMousePacket) {
    if (!sdk.isV2Tap(id)) return
    when (UnifiedAirGesture.fromCode(data.gesture.getInt())) {
        UnifiedAirGesture.LEFT -> goBack()
        else -> {}
    }
}
```

In `STREAM` mode the same code repeats many times per second. Act when the code **changes**, or ignore repeats within about 40 ms. Holds repeat until the hand relaxes and `NONE` (100) arrives. Wait for a few `NONE` packets in a row before you treat a hold as released (see `tap-android-dpad`).

## Processed layer (Android)

Android also runs the raw stream through `GesturePostprocessor` (framed protocol) or `XRGesturesMain` (classic protocol, and framed-protocol hardware 4.x.x). The result is `onXRAirGestureReceived(id, XRAirGesture)`.

Prefer this callback for UI: it debounce-filters the stream and latches holds.

| `XRAirGesture` | Meaning |
|----------------|---------|
| `CLICK_INDEX` / `CLICK_MIDDLE` / `CLICK_RING` / `CLICK_PINKY` | Short pinch, one event |
| `DRAG_INDEX` / `DRAG_MIDDLE` / `DRAG_RING` / `DRAG_PINKY` | Hold latched (pinch held) |
| `DROP` | Hold released after a drag |
| `FIST_BEGIN` / `FIST_END` | Fist hold start / end |
| `SWIPE_LEFT` / `SWIPE_RIGHT` / `SWIPE_UP` / `SWIPE_DOWN` | Thumb swipe, one event |
| `POTENTIAL_DRAG_OR_CLICK_*` | Ambiguous press on the classic processor only |
| `NONE` | Idle |

```java
@Override
public void onXRAirGestureReceived(String id, XRAirGesture gesture) {
    switch (gesture) {
        case CLICK_INDEX: select(); break;
        case DRAG_INDEX: grab(); break;
        case DROP: drop(); break;
        case FIST_BEGIN: hideMenu(); break;
        case FIST_END: showMenu(); break;
        case SWIPE_LEFT: goBack(); break;
        default: break;
    }
}
```

```kotlin
override fun onXRAirGestureReceived(id: String, gesture: XRAirGesture) {
    when (gesture) {
        XRAirGesture.CLICK_INDEX -> select()
        XRAirGesture.DRAG_INDEX -> grab()
        XRAirGesture.DROP -> drop()
        XRAirGesture.FIST_BEGIN -> hideMenu()
        XRAirGesture.SWIPE_LEFT -> goBack()
        else -> {}
    }
}
```

`onXRAirGestureReceived` is a default method on `TapListener`. You can override it without touching the other callbacks. Raw `onAirMouseInputReceived` still fires for the same packets.

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

`DeviceFeature.STANDBY_GESTURE_DETECTION` lets the user put the device in standby with a gesture. `onTapStandbyStateChanged(id, standby)` reports changes. `sdk.setStandbyState(id, false)` wakes it. `sdk.getStandbyState(id, callback)` reads it (null on timeout). After standby, send the input mode / XR state again if events stop (`startXRTappingState` or `startXRAirMouseState`).

## Classic-protocol air gestures

Classic firmware has no vision-model API. In Controller mode, `onAirMouseInputReceived` uses `AirMousePacket` constants:

`AIR_MOUSE_GESTURE_NONE` (0), `GENERAL` (1), `UP` / `DOWN` / `LEFT` / `RIGHT` and the `_TWO_FINGERS` variants, `INDEX_TO_THUMB_TOUCH` (10), `MIDDLE_TO_THUMB_TOUCH` (11).

Older TapXR continuous hand states (several times a second; take the majority of the last 3): `XR_AIR_GESTURE_NONE` (100), `XR_AIR_GESTURE_THUMB_INDEX` (101), `XR_AIR_GESTURE_THUMB_MIDDLE` (102). These overlap numerically with framed `UnifiedAirGesture` codes. Gate on `sdk.isV2Tap(id)` before `UnifiedAirGesture.fromCode`.

`onTapChangedState(id, state)` fires when an air-mouse packet has gesture 20. `state` is the device byte (`AirMousePacket.state`). The SDK treats `1` as air-mouse and then drops tap callbacks. That value is not `TapXRState.AIR_MOUSE` (4).

Force the modality with `startXRTappingState`, `startXRAirMouseState`, or `startXRUserControlState`. There is no separate `setInputType` method.
