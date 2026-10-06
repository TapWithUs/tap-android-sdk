---
name: tap-android-tapping
description: >-
  Decode Tap tapcodes on Android (which fingers tapped), map finger combinations
  to app actions, and give haptic feedback. Use for tapping, finger combos,
  chords, tap-to-action, v1 input modes, or the v2 TAPPING model. Start from
  tap-android-getting-started: TapSdkFactory.getDefault, registerTapListener, resume
  and pause. getDefault() alone delivers no taps.
---

# Tapping

Works on v1 and v2. Start from the `tap-android-getting-started` skill for connection. `tapIdentifier` comes from `onTapConnected`.

## Tapcode bitmask

`onTapInputReceived` gives `data`, an int 1–31. Each bit is one finger:

| Bit | Value | Finger |
|-----|-------|--------|
| 0 | 1 | thumb |
| 1 | 2 | index |
| 2 | 4 | middle |
| 3 | 8 | ring |
| 4 | 16 | pinky |

Examples: `1` thumb, `2` index, `3` thumb+index, `6` index+middle, `31` all five.

```java
boolean[] fingers = TapSdk.toFingers(data); // [thumb, index, middle, ring, pinky]

static final int NEXT = 2;      // index
static final int PREVIOUS = 4;  // middle
static final int SELECT = 6;    // index + middle
static final int QUIT = 31;     // all five
```

Single-finger taps are the most reliable. Prefer them for frequent actions. Use 2-finger combos next. Use 3+ finger combos only for rare actions.

## Turn on tap events

- **v1**: Controller mode is the default after connect while the Activity is resumed.
  - `startTextMode(id)`: the Tap types on the OS keyboard. The SDK gets no taps.
  - `startControllerMode(id)`: the SDK gets tap, mouse, and air-gesture events. No typing.
  - `startControllerWithMouseHIDMode(id)`: controller plus the system mouse cursor.
  - `startControllerWithFullHIDMode(id)`: controller plus keyboard HID.
  - `setDefaultMode(TapInputMode.controller(), true)` sets the mode for new connections and, when the second argument is true, for Taps already connected.
- **v2**: Controller mode enables `MODEL_DETECTION` and `IMU_MOTION_DATA` but does not select a vision model. For taps call `startXRTappingState(id)`, which sets `VisionSensorModel.TAPPING` and `VisionSensorOpMode.TRIGGER`. The direct calls are `setFeature`, `setVisionSensorModel`, and `setVisionSensorOpMode`.

`pause()` switches every connected Tap to Text mode. `resume()` restores the mode you set. Skipping those calls is the usual reason taps work once and then stop.

## Double taps and multi-taps

On **v1**, `repeatData` is 1 (single), 2 (double), or 3 (triple). Use it.

On **v2**, `repeatData` is always 1. Detect double taps yourself:

```java
private int lastCode;
private long lastNs;

void onTap(int data) {
    long now = System.nanoTime();
    if (data == lastCode && now - lastNs < 350_000_000L) {
        handleDouble(data);
        lastCode = 0;
        return;
    }
    lastCode = data;
    lastNs = now;
    handleSingle(data);
}
```

If a single and a double must not both fire, delay the single action ~350 ms and cancel it when the second tap arrives. Use a main-thread `Handler`.

## Haptic feedback

```java
sdk.vibrate(tapIdentifier, new int[]{80});
sdk.vibrate(tapIdentifier, new int[]{100, 100, 100}); // on, pause, on
```

Values are on/off durations in milliseconds, clamped to 10–2500. Up to 18 values. The SDK scales them to 10 ms units on the wire. `vibrate` is safe to call from the callback thread.

## Shift and switch

`onTapShiftSwitchReceived` reports a separate bitmask. `TapSdk.toShiftAndSwitch(data)` returns `{shift, switchState}` where shift is 0 off, 1 on, 2 locked.

## Docs in this repo

- Modes and `TapListener`: `tap-android-sdk` skill, `reference.md`
- Example: `app/src/main/java/com/tapwithus/tapsdk/MainActivity.java`
