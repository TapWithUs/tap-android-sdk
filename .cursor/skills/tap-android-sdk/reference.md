# TAP Android SDK — API Reference

Complete public API surface. Package root: `com.tapwithus.sdk`.
Everywhere below, `tapIdentifier` is the device's Bluetooth address (`String`),
delivered first via `TapListener.onTapConnected`.

## TapSdk

### Instantiation

```java
// Default shared instance (recommended)
TapSdk sdk = TapSdkFactory.getDefault(context);

// Manual construction (e.g. for dependency injection)
BluetoothManager bluetoothManager = new BluetoothManager(context.getApplicationContext(), BluetoothAdapter.getDefaultAdapter());
TapBluetoothManager tapBluetoothManager = new TapBluetoothManager(bluetoothManager);
TapSdk sdk = new TapSdk(tapBluetoothManager);
```

### Lifecycle and configuration

| Method | Purpose |
|---|---|
| `void resume()` / `void pause()` | Must be called from the app's `onResume`/`onPause`; drives automatic Controller/Text mode switching |
| `void enablePauseResumeHandling()` / `void disablePauseResumeHandling()` | Toggle automatic background mode restore |
| `void clearCacheOnTapDisconnection(boolean)` | Whether cached device data is cleared on disconnect |
| `void setDefaultMode(TapInputMode mode, Boolean applyImmediate)` | Mode applied to newly connected devices; `applyImmediate` also applies to already-connected ones |
| `void setDefaultXRState(TapXRState state, Boolean applyImmediate)` | Same, for TapXR input states |
| `void enableDebug()` / `void disableDebug()` | Verbose SDK logs in LogCat |
| `void close()` | Release the inner Bluetooth manager |
| `void refreshConnections()` | Re-check connections |
| `void refreshBond(String tapIdentifier)` | Refresh BLE bond |
| `boolean isConnectionInProgress()` / `isConnectionInProgress(String address)` | Connection state queries |

### Devices and listeners

| Method | Purpose |
|---|---|
| `void registerTapListener(TapListener l)` / `void unregisterTapListener(TapListener l)` | Subscribe to all callbacks |
| `Set<String> getConnectedTaps()` | Identifiers of connected TAPs |
| `Tap getCachedTap(String tapIdentifier)` | Cached device info (see `Tap` below) |
| `void ignoreTap(String)` / `unignoreTap(String)` / `Set<String> getIgnoredTaps()` / `boolean isTapIgnored(String)` | Exclude devices from SDK handling |

### Input modes (per device)

| Method | Mode |
|---|---|
| `startControllerMode(String)` | Taps/gestures delivered to the app (default) |
| `startTextMode(String)` | Device acts as a Bluetooth keyboard; no SDK input |
| `startControllerWithMouseHIDMode(String)` | Controller + system mouse cursor |
| `startControllerWithFullHIDMode(String)` | Controller + full HID |
| `startRawSensorMode(String, byte devAccelSens, byte imuGyroSens, byte imuAccelSens)` | Raw sensor streaming. Sensitivities: device accelerometer 1–4, IMU gyro 1–4, IMU accelerometer 1–5, 0 = default |

Deprecated: `startMode(String, int)`, `getMode(String)`, `isInMode(String, int)`.

`TapInputMode` factories: `controller()`, `text()`, `controllerWithMouseHID()`,
`controllerWithFullHID()`, `rawSensorData(byte, byte, byte)`.
Constants: `TEXT=1, CONTROLLER=2, CONTROLLER_WITH_MOUSEHID=3, RAW_SENSOR=4, CONTROLLER_WITH_FULLHID=5`.

### TapXR input states

| Method | Effect |
|---|---|
| `startXRUserControlState(String)` | User switches freely between tapping and air mouse |
| `startXRAirMouseState(String)` | Air-mouse detection only |
| `startXRTappingState(String)` | Tapping detection only |

`TapXRState` factories: `none()`, `userControl()`, `tapping()`, `airMouse()`.
Query helpers: `isTapInAirMouseState(String)`, `isAnyTapInAirGestureState()`,
`isAnyTapSupportsAirGesture()`, `isAirMouseSupported(String)`.

### Actions

| Method | Purpose |
|---|---|
| `void vibrate(String tapIdentifier, int[] durations)` | Haptics; alternating on/pause durations in ms, max 18 elements |
| `void writeName(String tapIdentifier, String name)` | Rename device |
| `void requestShiftSwitchState(String tapIdentifier)` | Ask for current shift/switch state |
| `void requestTap(String tapIdentifier, byte combination)` | Simulate a tap |

### Static helpers

```java
boolean[] fingers = TapSdk.toFingers(int tapInput);
// fingers[0]=thumb, [1]=index, [2]=middle, [3]=ring, [4]=pinky

int[] ss = TapSdk.toShiftAndSwitch(int data);
// ss[0]: 0=off, 1=shift, 2=locked;  ss[1]: 0=off, non-zero=on
```

### Error codes

| Constant | Value | Meaning |
|---|---|---|
| `TapSdk.ERR_SUBSCRIBE_MODE` | 101 | Mode subscription failed |
| `TapSdk.ERR_HAPTIC` | 102 | Haptic write failed |
| `TapSdk.ERR_V2_NOT_SUPPORTED` | 103 | V2-only API called on a v1 device |

## TapListener

```java
public interface TapListener {
    void onBluetoothTurnedOn();
    void onBluetoothTurnedOff();
    void onTapStartConnecting(String tapIdentifier);
    void onTapConnected(String tapIdentifier);
    void onTapDisconnected(String tapIdentifier);
    void onTapResumed(String tapIdentifier);
    void onTapChanged(String tapIdentifier);
    void onTapInputReceived(String tapIdentifier, int data, int repeatData);
    void onTapShiftSwitchReceived(String tapIdentifier, int data);
    void onMouseInputReceived(String tapIdentifier, MousePacket data);
    void onAirMouseInputReceived(String tapIdentifier, AirMousePacket data);
    void onRawSensorInputReceived(String tapIdentifier, RawSensorData rsData);
    void onTapChangedState(String tapIdentifier, int state);
    void onError(String tapIdentifier, int code, String description);
    // V2-only, default methods:
    default void onImuMotionInputReceived(String tapIdentifier, ImuMotionPacket packet) { }
    default void onTapStandbyStateChanged(String tapIdentifier, boolean standby) { }
}
```

Notes:
- `onTapInputReceived`: `data` is 1–31 finger bitmask (LSB=thumb), `repeatData` 1/2/3 = single/double/triple (V2 always 1).
- `onTapChangedState`: reports `TapXRState` constants (`NONE=0, USER_CONTROL=1, TAPPING=2, AIR_MOUSE=4`).

## Data classes

### Tap (`com.tapwithus.sdk.tap.Tap`)

Getters: `getIdentifier()`, `getName()`, `getBattery()`, `getSerialNumber()`,
`getHwVer()`, `getFwVer()`, `getBootloaderVer()`.

### MousePacket (`com.tapwithus.sdk.mouse.MousePacket`)

`PacketValue` fields (use `.getInt()`): `dx`, `dy`, `dt`, `proximity`.

### AirMousePacket (`com.tapwithus.sdk.airmouse.AirMousePacket`)

`PacketValue` fields: `gesture`, `state`. Classic gesture constants:

```java
AIR_MOUSE_GESTURE_NONE = 0            AIR_MOUSE_GESTURE_LEFT = 6
AIR_MOUSE_GESTURE_GENERAL = 1         AIR_MOUSE_GESTURE_LEFT_TWO_FINGERS = 7
AIR_MOUSE_GESTURE_UP = 2              AIR_MOUSE_GESTURE_RIGHT = 8
AIR_MOUSE_GESTURE_UP_TWO_FINGERS = 3  AIR_MOUSE_GESTURE_RIGHT_TWO_FINGERS = 9
AIR_MOUSE_GESTURE_DOWN = 4            AIR_MOUSE_GESTURE_INDEX_TO_THUMB_TOUCH = 10
AIR_MOUSE_GESTURE_DOWN_TWO_FINGERS = 5  AIR_MOUSE_GESTURE_MIDDLE_TO_THUMB_TOUCH = 11
// TapXR continuous hand states (sent multiple times/second; take majority of last 3):
XR_AIR_GESTURE_NONE = 100, XR_AIR_GESTURE_THUMB_INDEX = 101, XR_AIR_GESTURE_THUMB_MIDDLE = 102
```

### RawSensorData (`com.tapwithus.sdk.mode.RawSensorData`)

Fields: `int timestamp`, `DataType dataType` (`Device` or `IMU`), `Point3[] points`.
`Point3 getPoint(int index)` with indexes:

- `DataType.Device` (finger accelerometers): `iDEV_THUMB=0, iDEV_INDEX=1, iDEV_MIDDLE=2, iDEV_RING=3, iDEV_PINKY=4`
- `DataType.IMU` (thumb unit, TAP Strap 2 / V2 devices): `iIMU_GYRO=0, iIMU_ACCELEROMETER=1`

`Point3` has `double x, y, z`.

## V2 API (`com.tapwithus.sdk.v2`)

All `get*` methods reply asynchronously on `TapV2Callback<T>`
(`void onResult(String tapIdentifier, @Nullable T value)`); `value` is `null`
if the device doesn't answer within 2 seconds. Calling any of these on a v1
device fires `onError` with `ERR_V2_NOT_SUPPORTED` (103).

```java
boolean isV2Tap(String tapIdentifier);

void setFeature(String id, DeviceFeature feature, boolean enable);
void getFeature(String id, DeviceFeature feature, TapV2Callback<Boolean> cb);

void setVisionSensorModel(String id, VisionSensorModel model);
void getVisionSensorModel(String id, TapV2Callback<VisionSensorModel> cb);
void setVisionSensorOpMode(String id, VisionSensorOpMode mode);
void getVisionSensorOpMode(String id, TapV2Callback<VisionSensorOpMode> cb);

void setImuSensitivity(String id, int gyro /*0-5*/, int accelerometer /*0-4*/);
void getImuSensitivity(String id, TapV2Callback<ImuSensitivity> cb);

void setStandbyState(String id, boolean standby);
void getStandbyState(String id, TapV2Callback<Boolean> cb);

void sendKeepAlive(String id); // automatic every 10s; manual send optional
```

### Enums

- `DeviceFeature`: `RAW_IMU_DATA(0)`, `MODEL_DETECTION(1)` (taps + air gestures), `IMU_MOTION_DATA(2)`, `TRIGGER_DETECTIONS(3)`, `STANDBY_GESTURE_DETECTION(4)`
- `VisionSensorModel`: `TAPPING`, `AIR_GESTURE` (one model active at a time)
- `VisionSensorOpMode`: `TRIGGER`, `STREAM`, `STREAM_ON_TRIGGER`
- `UnifiedAirGesture` (codes 100–114, decode with `fromCode(int)` — returns `null` for non-V2 codes): `NONE(100)`, `LEFT(101)`, `RIGHT(102)`, `UP(103)`, `DOWN(104)`, `AB(105)`, `AC(106)`, `AD(107)`, `AE(108)`, `FIST(109)`, `AB_HOLD(110)`, `AC_HOLD(111)`, `AD_HOLD(112)`, `AE_HOLD(113)`, `FIST_HOLD(114)`. AB/AC/AD/AE = thumb pinching index/middle/ring/pinky; `_HOLD` = held pinch (drag).

### ImuMotionPacket

Public fields: `int dx, dy` (pointer delta), `boolean isMouse`, `int roll, pitch, yaw` (Euler angles).
Delivered via `onImuMotionInputReceived` when `IMU_MOTION_DATA` is enabled (default in controller mode).

### Mode mapping on V2 devices

The classic mode API is translated to V2 feature commands automatically:

- `startControllerMode` → enables `MODEL_DETECTION` + `IMU_MOTION_DATA`
- `startRawSensorMode` → enables `RAW_IMU_DATA` (thumb IMU only; no finger accelerometers on V2)
- `startXRTappingState` → vision model `TAPPING`, op-mode `TRIGGER`
- `startXRAirMouseState` → vision model `AIR_GESTURE`, op-mode `STREAM`
- `vibrate` → framed haptic command
