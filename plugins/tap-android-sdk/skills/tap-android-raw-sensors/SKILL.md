---
name: tap-android-raw-sensors
description: >-
  Stream raw accelerometer and gyroscope samples from a Tap on Android (v1 raw
  mode or v2 RAW_IMU_DATA) and set sensitivity. Use for raw sensors, raw IMU,
  accelerometer, gyro, data logging, or custom gesture recognition. Requires
  tap-android-getting-started order: getDefault, registerTapListener, resume and pause.
---

# Raw sensors

For logging and custom gesture models. For a cursor or orientation, prefer `tap-android-imu-motion` (less data, already integrated).

Samples arrive on `onRawSensorInputReceived(id, RawSensorData)`. Keep the callback short: copy the sample onto a queue and write files on another thread.

## Packet

`RawSensorData` fields: `timestamp` (device clock, milliseconds), `dataType`, `points` (`Point3` x, y, z).

| `dataType` | Contents | Indexes |
|------------|----------|---------|
| `RawSensorData.DataType.Device` | finger accelerometers, thumb first | `iDEV_THUMB`, `iDEV_INDEX`, `iDEV_MIDDLE`, `iDEV_RING`, `iDEV_PINKY` |
| `RawSensorData.DataType.IMU` | thumb gyro and accelerometer | `iIMU_GYRO`, `iIMU_ACCELEROMETER` |

```java
Point3 thumb = rsData.getPoint(RawSensorData.iDEV_THUMB);
if (thumb != null) {
    double x = thumb.x, y = thumb.y, z = thumb.z;
}
```

Use `timestamp` for spacing between samples, not `System.currentTimeMillis()`.

## v1

```java
sdk.registerTapListener(listener);
// from onTapConnected, or once you know the id:
sdk.startRawSensorMode(id, (byte) 0, (byte) 0, (byte) 0); // 0 = device default
// leave raw mode
sdk.startControllerMode(id);
```

Arguments are `deviceAccelerometerSensitivity` (finger accelerometers, 1–4), `imuGyroSensitivity` (1–5), and `imuAccelerometerSensitivity` (1–4). `0` keeps the default and is not a named range. For 1 and up, those integers are the same as `ImuGyroSensitivity` (`DPS125` = 1 … `DPS2000` = 5) and `ImuAcclSensitivity` (`G2` = 1 … `G16` = 4).

- Raw streaming on v1 needs **Developer mode** in the Tap Manager app.
- Tap Strap streams finger accelerometers. Tap Strap 2 and TapXR also stream the thumb IMU.
- `pause()` stops the raw-mode refresh loop. `resume()` starts it again. Forward both.

## v2

`startRawSensorMode` enables `DeviceFeature.RAW_IMU_DATA` and disables model detection and IMU-motion. v2 streams the **thumb IMU only**. There are no finger accelerometers. `DataType.Device` packets are not produced.

```java
sdk.setFeature(id, DeviceFeature.RAW_IMU_DATA, true);
sdk.setImuSensitivity(id, ImuGyroSensitivity.DPS500, ImuAcclSensitivity.G4);
sdk.getImuSensitivity(id, (identifier, sensitivity) -> {
    // null on timeout
    // sensitivity.getGyroSensitivity() is DPS125..DPS2000, or null if the byte is 0
    // sensitivity.getAccelerometerSensitivity() is G2..G16, or null if the byte is 0
});
```

`ImuGyroSensitivity` is `DPS125`(1), `DPS250`(2), `DPS500`(3), `DPS1000`(4), `DPS2000`(5). `ImuAcclSensitivity` is `G2`(1), `G4`(2), `G8`(3), `G16`(4). Those integers are what Python sends. The int overload `setImuSensitivity(id, int, int)` is deprecated: it still clamps to gyro 0–5 and accelerometer 0–4 and sends that byte, including 0, so older callers do not change meaning. New code uses the enums.

Turn raw mode off with `setFeature(id, DeviceFeature.RAW_IMU_DATA, false)` or `startControllerMode(id)`, then re-select a vision model if you want taps again (`startXRTappingState`).

## Logging

Append samples to an `ArrayDeque` or a single-thread executor from the callback. Write CSV off that callback. Columns that stay unambiguous: `timestamp`, `type` (`device` or `imu`), then x, y, z per point. Ask the user how long to record. There is no sample script in the SDK; the callback above is the whole integration.

## Docs in this repo

- Field list: `tap-android-sdk` skill, `reference.md`
- README section "Raw Sensor Mode"
