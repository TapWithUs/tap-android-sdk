---
name: tap-knob
description: >-
  Build a virtual knob on Android with a Tap v2 — hold a pinch and twist the
  wrist to turn a value up or down (volume, brightness, zoom, scroll). Use for
  knob, dial, twist, rotate-to-adjust, or continuous value control. v2 only.
  Requires tap-getting-started order: getDefault, registerTapListener, resume
  and pause.
---

# Knob (pinch-hold + twist)

**v2 only** (`sdk.isV2Tap(id)`). It needs air-gesture holds and IMU roll. v1 mouse packets have no roll. This is the same interaction as the Knob in the TAP_EV sandbox (https://tapwithus.github.io/TAP_EV/).

## How it works

1. The user holds a pinch: `AB_HOLD`..`AE_HOLD` (codes 110–113). That **grabs** a knob. Each pinch is a separate knob, so one hand controls 4 values.
2. While the pinch is held, wrist **roll** from `onImuMotionInputReceived` turns the knob. The roll at grab time is the reference. Every `stepDeg` of roll away from it is one step.
3. The user relaxes the hand. After `releaseAfter` consecutive `NONE` (100) packets, the knob is **released**. Waiting for several packets stops one noisy packet from dropping the grab.

## Use the template

Copy [java/KnobTracker.java](java/KnobTracker.java) into the app. It has no Android or SDK imports.

- `onGesture(code)` returns `"start"`, `"release"`, or null.
- `onRoll(roll)` returns signed int steps (0 when idle).
- `active` is the knob index 0–3 (AB, AC, AD, AE), or null.

Required device setup, from `onTapConnected` when `sdk.isV2Tap(id)`:

```java
sdk.setFeature(id, DeviceFeature.MODEL_DETECTION, true);
sdk.setVisionSensorModel(id, VisionSensorModel.AIR_GESTURE);
sdk.setVisionSensorOpMode(id, VisionSensorOpMode.STREAM);
sdk.setFeature(id, DeviceFeature.IMU_MOTION_DATA, true);
```

`startXRAirMouseState(id)` covers the vision model and STREAM op-mode. Controller mode already enables `IMU_MOTION_DATA`. Calling both is safe.

Wiring (callbacks may be off the main thread; update Views on the main thread):

```java
KnobTracker knob = new KnobTracker(2.0, 4);
int[] values = {50, 50, 50, 50};

void onAir(AirMousePacket data) {
    UnifiedAirGesture gesture = UnifiedAirGesture.fromCode(data.gesture.getInt());
    if (gesture == null) return;
    String event = knob.onGesture(gesture.getCode());
    if ("start".equals(event)) {
        sdk.vibrate(id, new int[]{40});
    }
}

void onMotion(ImuMotionPacket packet) {
    int steps = knob.onRoll(packet.roll);
    if (steps != 0 && knob.active != null) {
        int i = knob.active;
        values[i] = Math.max(0, Math.min(100, values[i] + steps));
    }
}
```

If `isV2Tap` is false, tell the user the knob needs a TapBand or a TapXR on V2 firmware. Do not fake roll from v1 `MousePacket` dx/dy.

## Tuning

| Setting | Default | Effect |
|---------|---------|--------|
| `stepDeg` | 1 (template example uses 2) | Degrees of roll per step. Larger = slower. 1–2 for fine values, 5–10 for menu items. |
| `releaseAfter` | 4 | `NONE` packets before release. Raise it if the knob drops while still pinching. |

- If the direction feels inverted, use `-steps`.
- Buzz once on grab (`vibrate(id, new int[]{40})`). Do not buzz on every step.
- Clamp values.
- Roll wraps at ±180. `KnobTracker.wrapDegrees` handles the crossing.

## Ideas

- Four properties of one object: AB = size, AC = brightness, AD = opacity, AE = roundness.
- Scroll a list: one step = one item, with `stepDeg = 8`.

See also `tap-dpad` and `tap-vision-models`.
