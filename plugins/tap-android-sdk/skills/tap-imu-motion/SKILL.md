---
name: tap-imu-motion
description: >-
  Use Tap hand motion on Android — v2 ImuMotionPacket (dx, dy, roll, pitch, yaw)
  and v1 MousePacket — to move a cursor, steer, tilt, or rotate. Use for air
  mouse, pointer, cursor, tilt, euler angles, or motion-driven UI. Requires
  tap-getting-started order: getDefault, registerTapListener, resume and pause.
---

# IMU motion and mouse

The two protocols report motion on different callbacks. Both can be connected at once; branch on `sdk.isV2Tap(id)`.

## v2: IMU motion

Controller mode enables `DeviceFeature.IMU_MOTION_DATA` on connect. You can also call `sdk.setFeature(id, DeviceFeature.IMU_MOTION_DATA, true)` after `onTapConnected`.

```java
@Override
public void onImuMotionInputReceived(String id, ImuMotionPacket packet) {
    int dx = packet.dx;
    int dy = packet.dy;
    boolean isMouse = packet.isMouse;
    int roll = packet.roll;
    int pitch = packet.pitch;
    int yaw = packet.yaw;
}
```

- `dx`, `dy`: signed pointer deltas since the last packet. Add them to a cursor position.
- `isMouse`: true when the device treats the motion as pointer movement.
- `roll`, `pitch`, `yaw`: signed orientation in degrees.
- Turn the feature off with `setFeature(id, DeviceFeature.IMU_MOTION_DATA, false)` to save battery.
- Motion works together with `MODEL_DETECTION` (air gestures). The `tap-knob` and `tap-dpad` skills combine them.
- v1 devices do not call `onImuMotionInputReceived`.

## v1: mouse events

Controller mode (or controller-with-mouse HID) delivers:

```java
@Override
public void onMouseInputReceived(String id, MousePacket data) {
    int vx = data.dx.getInt();
    int vy = data.dy.getInt();
    int proximity = data.proximity.getInt();
}
```

- `dx` / `dy`: signed velocities (`PacketValue.getInt()`).
- `proximity`: non-zero when a surface is detected (optical mouse on Tap Strap).
- v1 has no euler angles on this callback. For orientation on v1, use raw IMU (`tap-raw-sensors`).
- Choosing air-mouse versus tapping on TapXR is `startXRAirMouseState(id)` / `startXRTappingState(id)`, not a separate input-type enum.

v2 also forwards pointer deltas on `onMouseInputReceived`. Prefer `onImuMotionInputReceived` when you need roll, pitch, and yaw.

## Patterns

Post to the main thread before touching a View.

**Cursor** (clamp to bounds, optional gain):

```java
float x = 400, y = 300;
void onMotion(int dx, int dy) {
    x = Math.max(0, Math.min(width, x + dx * gain));
    y = Math.max(0, Math.min(height, y + dy * gain));
}
```

**Relative rotation (roll as a dial)**: store the roll when the user starts (for example on a pinch-hold), then use `roll - reference`. Do not use absolute roll. It depends on how the hand is held. See `tap-knob`.

**Smoothing**: `smooth = alpha * value + (1 - alpha) * smooth` with `alpha` around 0.3.

**Dead zone**: ignore `Math.abs(dx) + Math.abs(dy) < 2` so a still hand does not drift.

**Rate**: count packets per second if timing matters. Do not assume a fixed rate.

If a direction is inverted on the user's device, flip the sign. Ask the user to test and tell you.

## Docs in this repo

- `ImuMotionPacket` and `MousePacket`: `tap-android-sdk` skill, `reference.md`
- Example app handles mouse and air-gesture rows in `MainActivity`
