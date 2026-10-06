---
name: tap-android-dpad
description: >-
  Build a D-Pad style controller on Android with a Tap v2 — swipes for
  directions, short pinches to select, pinch-hold that locks into rotate
  (twist) or drag (move), fist-hold to hide. Use for D-Pad, menu navigation,
  drag and drop, or game controls. v2 only. Requires tap-android-getting-started
  order: getDefault, registerTapListener, resume and pause.
---

# D-Pad (swipe, pinch, hold to rotate or drag)

**v2 only** (`sdk.isV2Tap(id)`). It needs air gestures and IMU motion. This is the same interaction as the D-Pad page in the TAP_EV sandbox (https://tapwithus.github.io/TAP_EV/).

## Interaction

| User does | Codes | Event |
|-----------|-------|-------|
| Swipe left / right / up / down | 101–104 | `direction` + `left` / `right` / `up` / `down` |
| Short pinch thumb + index / middle / ring / pinky | 105–108 (AB–AE) | `pinch` index 0–3 (only when nothing is held) |
| Hold a pinch | 110–113 | `hold`, mode `pending` |
| …and twist 25° or more within 1 s | IMU roll | mode `rotate`, then `rotate` degrees |
| …and do not twist | — | after 1 s mode `drag`, then `drag` dx, dy |
| Relax the hand | 4 × 100 (`NONE`) | `release` |
| Hold a fist | 114 | `hide`; then 2 × `NONE` gives `show` |

Repeats of the same code within 40 ms are ignored. Gesture packets stream continuously.

## Use the template

Copy [java/DpadStateMachine.java](java/DpadStateMachine.java) (package `com.tapwithus.tapsdk.sample`). It has no Android or SDK imports. The example app's `GestureSampleActivity` compiles this file together with the knob tracker.

- `onGesture(code, nowNanos)` feeds air-gesture codes.
- `onMotion(dx, dy, roll, nowNanos)` feeds IMU motion.
- `tick(nowNanos)` locks drag mode on time if motion packets stop. Call it from a frame loop if you want the 1 s timeout even when the hand is still.
- `held`, `mode`, `hidden` are the current state for drawing.

`nowNanos` is `System.nanoTime()`.

Required device setup after `onTapConnected` for a v2 Tap:

```java
sdk.startXRAirMouseState(id); // AIR_GESTURE + STREAM
sdk.setFeature(id, DeviceFeature.IMU_MOTION_DATA, true);
sdk.setFeature(id, DeviceFeature.MODEL_DETECTION, true);
```

Wiring:

```java
DpadStateMachine dpad = new DpadStateMachine();

void onAir(String id, AirMousePacket data) {
    UnifiedAirGesture gesture = UnifiedAirGesture.fromCode(data.gesture.getInt());
    if (gesture == null) return;
    for (DpadStateMachine.Event event : dpad.onGesture(gesture.getCode(), System.nanoTime())) {
        if ("hold".equals(event.kind) || "mode".equals(event.kind) || "pinch".equals(event.kind)) {
            sdk.vibrate(id, new int[]{40});
        }
        apply(event);
    }
}

void onMotion(ImuMotionPacket packet) {
    for (DpadStateMachine.Event event : dpad.onMotion(packet.dx, packet.dy, packet.roll, System.nanoTime())) {
        apply(event);
    }
}
```

If the device is v1, say so and offer tap combos (`tap-android-tapping`) instead of this recipe.

## Tuning

| Argument | Default | Effect |
|----------|---------|--------|
| `rotateLockDeg` | 25 | Roll travel needed to choose rotate. Raise it if drag is chosen too rarely. |
| `lockWindowNs` | 1 second | Time to decide between rotate and drag. |
| `releaseAfter` | 4 | `NONE` packets before release. |
| `showAfter` | 2 | `NONE` packets after a fist before showing again. |
| `debounceNs` | 40 ms | Ignore repeats of the same code in this window. |

- `drag` deltas are raw device units. Multiply by a gain and clamp to the screen.
- `rotate` degrees are relative to the start of the hold.
- If a drag should not stay after release, snap the item back on `release`.

## Ideas

- In-app grid: swipes move focus, pinch AB = OK, pinch AC = back, fist = close.
- Photo viewer: swipes = next / previous, hold + twist = rotate, hold + drag = pan.

See also `tap-android-knob` and `tap-android-vision-models`.
