# TAP Official Android SDK

Updates
=======

October 2026 - Product messaging: current TapXR developer firmware reports the **full gesture set** (taps, thumb swipes, pinches with hold, fist with hold). TapBand uses the **same BLE API** as TapXR and is coming soon. Android documents raw sensor detections and the processed `onXRAirGestureReceived` layer. Coding-agent skills are in [AI-Assisted Development](#ai-assisted-development). There is one SDK — do not call it "SDK v1" or "SDK v2".

September 2026 - Added **framed BLE protocol** support - feature toggles, vision sensor configuration, IMU motion + Euler angles, standby state, unified air gestures and keepalive (See below). Aligned with [tap-python-sdk](https://github.com/TapWithUs/tap-python-sdk) and tap-ios-sdk.

July 2024 - Added **TAPXR Gestures** (See below).


What Is This?
=============
TAP Android SDK lets you build an Android app that receives input from TAP devices: Tap Strap, Tap Strap 2, and TapXR. Each tap is an array of fingers, or a binary combination integer (explanation follows), so the TAP device can act as a controller for your app.

TapBand uses the **same BLE API** as TapXR. TapBand is coming soon (it is not for sale yet). Join the waitlist: https://www.tapwithus.com/tapband-waitlist/

The current TapXR developer firmware reports the **full gesture set**: taps, thumb swipes (up, down, left, right), thumb-to-finger pinches with hold, and fist with hold. Do not invent a firmware version number.

Every Tap SDK delivers **raw** gesture detections from the sensor. Android also offers a **processed** air-gesture layer (`onXRAirGestureReceived`) with per-finger click, drag, drop, fist begin/end, and swipe (debounce and hold latch).

Getting started
===============
To add TAP SDK library to your project:
- Make sure you have mavenCentral in your Gradle repositories.
- Add the following Gradle dependency to your build.gradle:
```Groovy
  implementation 'io.github.tapwithus:tap-android-sdk:0.3.6'
```

Getting instance of TapSdk
==========================
The entry point of TAP SDK is the `com.tapwithus.sdk.TapSdk` class.

If your application might be using the default `TapSdk` instance, a `TapSdk` single instance can be retrieved simply by using the `TapSdkFactory` helper class by calling `TapSdkFactory.getDefault(context)`. This helper method will return a single instance of `TapSdk` relatively to the passed context lifecycle.

If an alternative `TapSdk` instance is required, or if you might be using a dependency injection framework where custom scopes is implemented, call the following commands instead, passing the appropriate arguments:
```Java
BluetoothManager bluetoothManager = new BluetoothManager(context.getApplicationContext(), BluetoothAdapter.getDefaultAdapter());
TapBluetoothManager tapBluetoothManager = new TapBluetoothManager(bluetoothManager);
TapSdk sdk = new TapSdk(tapBluetoothManager);
```

Key features
============
##### Input Modes
As stated in the official [Tap BLE API Documentation](https://www.tapwithus.com/api), once you turn ON the TAP device, by default, it will be booted into Text Mode, meaning that the TAP device functions as a bluetooth keyboard, every recognized tap will be mapped to a letter, and __no input data will be sent to the SDK__.

When using the SDK, it is required to get the input data for a specific TAP device. In order to achieve this goal, after a connection with the TAP been established, we need to switch the TAP device to Controller Mode. In addition, it is important we switch back to Text Mode once the application goes to background, so the regular TAP behaviour will be restored.

To simplify the process TAP SDK will perform the needed actions in order to correctly connect and switch between Modes _automatically_, __so you don't have to.__

Two additional modes to Text and Controller :
Controller with Mouse HID: Behaves as a controller, but also allow the TAP to control the mouse cursor).
Raw Sensor Mode: Streams raw sensor data (from Gyro and Accelerometer sensors on TAP). More or that later... 

What now?
=========
Once a `TapSdk` instance been instantiated, TAP SDK will start doing his magic, by performing the following actions:
* Auto establishing connections with paired TAP devices.
* After a connection with a TAP device been established successfully, switching it to Controller Mode.
* When the application goes to background, switching back to Text Mode.
* When the application returns from background, switching back to Controller Mode.

The only thing you need to take care of is to register for the necessary events.

Registering TapListener
=======================
`com.tapwithus.sdk.TapListener` is an interface, describing the various callbacks you can get back from `TapSdk`.

```Java
public interface TapListener {
    void onBluetoothTurnedOn();
    void onBluetoothTurnedOff();
    void onTapStartConnecting(@NonNull String tapIdentifier);
    void onTapConnected(@NonNull String tapIdentifier);
    void onTapDisconnected(@NonNull String tapIdentifier);
    void onTapResumed(@NonNull String tapIdentifier);
    void onTapChanged(@NonNull String tapIdentifier);
    void onTapInputReceived(@NonNull String tapIdentifier, int data, int repeatData);
    void onTapShiftSwitchReceived(@NonNull String tapIdentifier, int data);
    void onMouseInputReceived(@NonNull String tapIdentifier, @NonNull MousePacket data);
    void onAirMouseInputReceived(@NonNull String tapIdentifier, @NonNull AirMousePacket data);
    void onRawSensorInputReceived(@NonNull String tapIdentifier, @NonNull RawSensorData rsData);
    void onTapChangedState(@NonNull String tapIdentifier, int state);
    void onError(@NonNull String tapIdentifier, int code, @NonNull String description);
    // default methods:
    default void onXRAirGestureReceived(@NonNull String tapIdentifier, @NonNull XRAirGesture gesture) { }
    default void onImuMotionInputReceived(@NonNull String tapIdentifier, @NonNull ImuMotionPacket packet) { }
    default void onTapStandbyStateChanged(@NonNull String tapIdentifier, boolean standby) { }
}
```
Implement it and pass it to `TapSdk` by calling `sdk.registerTapListener(tapListener)`.

___
#### Important Note:  
`onTapInputReceived` is a callback function which will be triggered every time a specific TAP device was being tapped, and which fingers were tapped. The returned data is an integer representing a 8-bit unsigned number, between 1 and 31. It's binary form represents the fingers that are tapped. The LSB is thumb finger, the MSB (bit number 5) is the pinky finger. For example: if combination equals 3 - it's binary form is 10100, Which means that the thumb and the middle fingers were tapped. For your convenience, you can convert the binary format into fingers boolean array by calling static function `TapSdk.toFingers(tapInput)` listed below.. The repeat data parameter is one for single taps, 2 for double 3 for triple
`onTapShiftSwitchReceived` is a callback function which will be triggered every time a specific TAP device was being tapped, holds the state of Shift (0=off, 1=on, 2=locked) and Switch (0=off, 1=on). The returned data is an integer representing a 8-bit unsigned number, between 1 and 31. It's binary form represents the fingers that are tapped. The LSB is thumb finger, the MSB (bit number 5) is the pinky finger. For example: if combination equals 3 - it's binary form is 10100, Which means that the thumb and the middle fingers were tapped. For your convenience, you can convert the binary format into an array of 2 ints by calling static function `TapSdk.toShiftAndSwitch(tapShiftSwitch)` listed below.
___

Tap Class
=========
Using `Tap` class you can get basic information of your TAP device.
A `Tap` instance can be generated only from connected, cached TAP devices, by simply calling `sdk.getCachedTap(tapIdentifier)`.

Debugging
=========
It is often desirable and useful to print out more `TapSdk` inner logs in LogCat. You can manually enable inner log prints by calling `sdk.enableDebug()`, and corresponding, you can disable inner log prints by calling `sdk.disableDebug()`

TapSdk API
==========
#### `void resume()` & `void pause()`
> As mentioned, to correctly switch between Modes, `TapSdk` needs to be aware of your application's lifecycle, in particular when your application goes to the background and return from it, so it is needed for you to call the corresponding methods when such events occur.
&nbsp;  
&nbsp;
#### `void clearCacheOnTapDisconnection(boolean clearCacheOnTapDisconnection)`
> When TAP device is connecting, `TapSdk` will cache some of it's data for quicker reconnection. You can change the default `TapSdk`'s behaviour by calling this method with the desired, new configuration.
&nbsp;
&nbsp;
#### `void enableAutoSetControllerModeOnConnection() & void disableAutoSetControllerModeOnConnection()`
#### ** DEPRECATED ** 
> Use SetDefaultMode instead:
```java
public void setDefaultMode(TapInputMode mode, Boolean applyImmediate)
```
This will set the default mode for new connected devices. 
pass true to applyImmediate if you wish to apply this mode to current connected devices.
&nbsp;
&nbsp;
#### `void enablePauseResumeHandling() & void disablePauseResumeHandling()`
> One of `TapSdk`'s key features is the background handling. By calling `disablePauseResumeHandling` method you can disable this functionality, so going to background will not effect any TAP device, and it'll remain in the same Mode as it was before going to background..
&nbsp;
&nbsp;
#### `Set<String> getConnectedTaps()`
> If you wish at any point in your application, you can retrieve a set of connected TAPs.
&nbsp;  
&nbsp;
#### `void registerTapListener(TapListener listener)`
> Pass `TapListener` to get all `TapSdk` callbacks.
&nbsp;  
&nbsp;
#### `void unregisterTapListener(TapListener listener)`
> Unregister registered `TapListener`.
&nbsp;  
&nbsp;
#### `void startMode(String tapIdentifier, int mode)`
#### ** DEPRECATED **
Use the following methods to change TAP mode:
```java
public void startControllerMode(@NonNull String tapIdentifier);
public  void startTextMode(@NonNull String tapIdentifier);
public void startControllerWithMouseHIDMode(@NonNull String tapIdentifier); 
public void startRawSensorMode(@NonNull String tapIdentifier, byte deviceAccelerometerSensitivity, byte imuGyroSensitivity, byte imuAccelerometerSensitivity);
public void startControllerWithFullHIDMode(@NonNull String tapIdentifier);
```
&nbsp;
&nbsp;
#### `int getMode(String tapIdentifier)`
#### ** DEPRECATED **
&nbsp;
&nbsp;
#### `boolean isInMode(String tapIdentifier, int mode)`
#### ** DEPRECATED **
> Another helper method to check what Mode is enabled for a specific TAP device.
&nbsp;  
&nbsp;
#### `public void vibrate(@NonNull String tapIdentifier, int[] durations)`
Send haptic/vibrations to a TAP device.
durations: An array of durations in the format of haptic, pause, haptic, pause ... You can specify up to 18 elements in this array. The rest will be ignored.
Each array element is defined in milliseconds.
When [tapIdentifiers] is null or missing - the mode will be applied to ALL connected TAPs.
Example:
```java
sdk.vibrate(tapIdentifier, new int[] { 500,100,500});
```
Will send two 500 milliseconds haptics with a 100 milliseconds pause in the middle.
&nbsp;
&nbsp;
#### `void writeName(String tapIdentifier, String name)`
> Write TAP device's name.
&nbsp;  
&nbsp;
#### `void close()`
> Releasing associated inner bluetooth manager.
&nbsp;  
&nbsp;
#### `static boolean[] toFingers(int tapInput)`
> As said before, the `tapInput` is an unsigned 8-bit integer. to convert it to array of booleans:
```Java
boolean[] fingers = TapSdk.toFingers(tapInput);
```
> While:  
fingers\[0\] indicates if the thumb was tapped.
fingers\[1\] indicates if the index finger was tapped.
fingers\[2\] indicates if the middle finger was tapped.
fingers\[3\] indicates if the ring finger was tapped.
fingers\[4\] indicates if the pinky finger was tapped.
&nbsp;  
&nbsp;

#### `static boolean[] toShiftAndSwitch(int tapShiftSwitch)`
> As said before, the `tapInput` is an unsigned 8-bit integer. to convert it to array of booleans:
```Java
int[] shiftSwitch = TapSdk.toShiftAndSwitch(tapShiftSwitch);
```
> While:
shiftSwitch\[0\] indicates the shift value (0=off, 1=on, 2=locked).
shiftSwitch\[1\] indicates the switch value (0=off, !0=on).
&nbsp;
&nbsp;

# Raw Sensor Mode
In raw sensors mode, the TAP continuously sends raw data from the following sensors:
1. Five 3-axis accelerometers on each finger ring.
2. IMU (3-axis accelerometer + gyro) located on the thumb (**for TAP Strap 2 only**).

### To put a TAP into Raw Sensor Mode
```java
public void startRawSensorMode(@NonNull String tapIdentifier, byte deviceAccelerometerSensitivity, byte imuGyroSensitivity, byte imuAccelerometerSensitivity);

...

sdk.startRawSensorMode(tapIdentifier, (byte)0,(byte)0,(byte)0);
```
When puting TAP in Raw Sensor Mode, the sensitivities of the values can be defined by the developer.
deviceAccelerometer refers to the sensitivities of the fingers' accelerometers. Range: 1 to 4.
imuGyro refers to the gyro sensitivity on the thumb's sensor. Range: 1 to 4.
imuAccelerometer refers to the accelerometer sensitivity on the thumb's sensor. Range: 1 to 5.
The default value for all sensitivities is 0. 

### Stream callback:

```java
public void onRawSensorInputReceived(@NonNull String tapIdentifier,@NonNull RawSensorData rsData) {
    //RawSensorData Object has a timestamp, dataType and an array points(x,y,z).
    if (rsData .dataType == RawSensorData.DataType.Device) {
        // Fingers accelerometer.
        // Each point in array represents the accelerometer value of a finger (thumb, index, middle, ring, pinky).
        Point3 thumb = rsData.getPoint(RawSensorData.iDEV_INDEX);
        if (thumb != null) {
            double x = thumb.x;
            double y = thumb.y;
            double z = thumb.z;
        }
        // Etc... use indexes: RawSensorData.iDEV_THUMB, RawSensorData.iDEV_INDEX, RawSensorData.iDEV_MIDDLE, RawSensorData.iDEV_RING, RawSensorData.iDEV_PINKY
    } else if (data.dataType == RawSensorData.DataType.IMU) {
        // Refers to an additional accelerometer on the Thumb sensor and a Gyro (placed on the thumb unit as well).
        Point3 gyro = rsData.getPoint(RawSensorData.iIMU_GYRO);
        if (point3 != null) {
            double x = gyro.x;
            double y = gyro.y;
            double z = gyro.z;
        }
        // Etc... use indexes: RawSensorData.iIMU_GYRO, RawSensorData.iIMU_ACCELEROMETER
    }
}
```

[For more information about raw sensor mode click here](https://tapwithus.atlassian.net/wiki/spaces/TD/pages/792002574/Tap+Strap+Raw+Sensors+Mode)

# Gestures — raw detections and the Android processed layer

Current TapXR developer firmware reports the full gesture set. TapBand uses the same BLE API (coming soon: https://www.tapwithus.com/tapband-waitlist/).

Every Tap SDK delivers **raw** detections from the sensor. On Android they arrive on `onAirMouseInputReceived`. Android also runs a **processed** layer and delivers `onXRAirGestureReceived`.

## Raw detections (`onAirMouseInputReceived`)

On the framed protocol (`sdk.isV2Tap(id)`), decode with `UnifiedAirGesture.fromCode`. Gate on `isV2Tap` first: codes 100–102 also exist on classic TapXR packets.

```java
@Override
public void onAirMouseInputReceived(@NonNull String tapIdentifier, @NonNull AirMousePacket data) {
    if (!sdk.isV2Tap(tapIdentifier)) {
        return;
    }
    UnifiedAirGesture gesture = UnifiedAirGesture.fromCode(data.gesture.getInt());
    if (gesture == null) {
        return;
    }
    switch (gesture) {
        case LEFT:    // thumb swipe left
        case RIGHT:
        case UP:
        case DOWN:
        case AB:      // thumb-index pinch (AC/AD/AE for the other fingers)
        case FIST:
        case AB_HOLD: // held pinch — also AC/AD/AE/FIST _HOLD
        case NONE:    // hand relaxed / hold released
            break;
    }
}
```

```kotlin
override fun onAirMouseInputReceived(tapIdentifier: String, data: AirMousePacket) {
    if (!sdk.isV2Tap(tapIdentifier)) return
    when (UnifiedAirGesture.fromCode(data.gesture.getInt())) {
        UnifiedAirGesture.AB -> { /* short pinch */ }
        UnifiedAirGesture.AB_HOLD -> { /* pinch held */ }
        UnifiedAirGesture.LEFT -> { /* swipe */ }
        else -> {}
    }
}
```

`UnifiedAirGesture` codes 100–114: `NONE`, `LEFT`/`RIGHT`/`UP`/`DOWN`, pinches `AB`/`AC`/`AD`/`AE`, `FIST`, and `_HOLD` variants.

On classic firmware, use `AirMousePacket.AIR_MOUSE_GESTURE_*` (0–11). Older TapXR continuous hand states `XR_AIR_GESTURE_NONE` (100), `XR_AIR_GESTURE_THUMB_INDEX` (101), `XR_AIR_GESTURE_THUMB_MIDDLE` (102) still arrive several times per second. Take the majority of the last 3 samples if you consume that path.

## Processed layer (`onXRAirGestureReceived`)

Prefer this callback for UI. The SDK debounce-filters the raw stream and latches holds (`GesturePostprocessor` on the framed protocol; `XRGesturesMain` on classic firmware and framed-protocol hardware 4.x.x).

```java
@Override
public void onXRAirGestureReceived(@NonNull String tapIdentifier, @NonNull XRAirGesture gesture) {
    switch (gesture) {
        case CLICK_INDEX: // short pinch, index — also CLICK_MIDDLE / RING / PINKY
        case DRAG_INDEX:  // hold latched
        case DROP:        // hold released after a drag
        case FIST_BEGIN:
        case FIST_END:
        case SWIPE_LEFT:  // also SWIPE_RIGHT / UP / DOWN
            break;
        default:
            break;
    }
}
```

```kotlin
override fun onXRAirGestureReceived(tapIdentifier: String, gesture: XRAirGesture) {
    when (gesture) {
        XRAirGesture.CLICK_INDEX -> { }
        XRAirGesture.DRAG_INDEX -> { }
        XRAirGesture.DROP -> { }
        XRAirGesture.FIST_BEGIN -> { }
        XRAirGesture.SWIPE_UP -> { }
        else -> {}
    }
}
```

Raw `onAirMouseInputReceived` still fires for the same packets.

## TapXRState

In addition to TAPInputMode, TapXR has input states that select tapping vs air gestures. The same calls apply on the framed protocol (they map to vision-model commands).

You can force a TapXR to switch state as follows:

AIRMOUSE - The TAPXR will operate in AIRMOUSE mode ONLY.
TAPPING - The TAPXR will operate in TAPPING mode only.
USERCONTROL - The user will freely switch states as wished.


Examples:

```java
tapSdk.setDefaultXRState(TapXRState.tapping(), true);
tapSdk.setDefaultXRState(TapXRState.userControl(), false);
tapSdk.startXRAirMouseState("identifier");
```
You can change the state of individual connected device or devices by calling one of these methods:

```java
public void startXRUserControlState(@NonNull String tapIdentifier)
public void startXRAirMouseState(@NonNull String tapIdentifier)
public void startXRTappingState(@NonNull String tapIdentifier)
```

or call this function to set the default XR State:

```java
public void setDefaultXRState(TapXRState state, Boolean applyImmediate)
```

While calling setDefaultTAPXRState, it'll set the default state that will be applied to newly connected devices. 
If you wish to apply this state to already-connected devices, call with "applyImmediate": true.


# Framed BLE protocol (September 2026)

There is one SDK. Current TapXR developer firmware, and TapBand when it ships, use a framed BLE protocol. Older Tap Strap / TapXR firmware uses the classic characteristic-per-stream protocol. `TapSdk` detects the protocol after connection — no extra connect code is required for basic usage.

`isV2Tap` and types in `com.tapwithus.sdk.v2` name that framed protocol. Do not call this "SDK v1" or "SDK v2". TapBand uses the same BLE API as TapXR (coming soon: https://www.tapwithus.com/tapband-waitlist/).

* Tap events keep arriving via `onTapInputReceived` (framed-protocol taps always report `repeatData == 1`).
* Mouse / pointer movement keeps arriving via `onMouseInputReceived`, and additionally via `onImuMotionInputReceived` which includes Euler angles (roll, pitch, yaw).
* Raw sensor data keeps arriving via `onRawSensorInputReceived` (framed protocol streams the thumb IMU only — there are no finger accelerometers).
* Air gestures keep arriving via `onAirMouseInputReceived` (`UnifiedAirGesture` codes 100-114) and, processed, via `onXRAirGestureReceived`.
* The existing mode API (`startControllerMode`, `startTextMode`, `startRawSensorMode`, XR states...) is translated automatically into framed feature commands.
* `vibrate` is sent through the framed protocol automatically.
* A keepalive message is sent automatically every 10 seconds to every connected framed-protocol Tap (matching tap-ios-sdk and tap-python-sdk). You can also send one manually with `sdk.sendKeepAlive(tapIdentifier)`.

You can check the protocol of a connected Tap with:

```java
boolean framed = sdk.isV2Tap(tapIdentifier);
```

### Device features

Instead of a single input mode, the framed protocol exposes independent feature toggles (`com.tapwithus.sdk.v2.DeviceFeature`): `RAW_IMU_DATA`, `MODEL_DETECTION` (taps + air gestures), `IMU_MOTION_DATA`, `STANDBY_GESTURE_DETECTION`. For fine-grained control you can set them directly:

```java
sdk.setFeature(tapIdentifier, DeviceFeature.MODEL_DETECTION, true);
sdk.getFeature(tapIdentifier, DeviceFeature.MODEL_DETECTION, (identifier, enabled) -> {
    // enabled is null on timeout
});
```

### Unified air gestures

See [Gestures — raw detections and the Android processed layer](#gestures--raw-detections-and-the-android-processed-layer) for Java and Kotlin examples of raw `UnifiedAirGesture` and processed `XRAirGesture`.

### Vision sensor (model & op-mode)

The vision sensor runs one detection model at a time - `TAPPING` or `AIR_GESTURE` - with an operating mode controlling when it streams. The existing XR state API maps onto it automatically: `startXRTappingState` selects the `TAPPING` model with op-mode `TRIGGER`, and `startXRAirMouseState` selects the `AIR_GESTURE` model with op-mode `STREAM`. For direct control:

```java
sdk.setVisionSensorModel(tapIdentifier, VisionSensorModel.AIR_GESTURE); // or TAPPING
sdk.setVisionSensorOpMode(tapIdentifier, VisionSensorOpMode.STREAM);    // or TRIGGER / STREAM_ON_TRIGGER
sdk.getVisionSensorModel(tapIdentifier, (identifier, model) -> { });
sdk.getVisionSensorOpMode(tapIdentifier, (identifier, opMode) -> { });
```

### IMU sensitivity

```java
sdk.setImuSensitivity(tapIdentifier, /* gyro 0-5 */ 3, /* accelerometer 0-4 */ 2);
sdk.getImuSensitivity(tapIdentifier, (identifier, sensitivity) -> { });
```

### Standby state

```java
sdk.setStandbyState(tapIdentifier, true);
sdk.getStandbyState(tapIdentifier, (identifier, standby) -> { });
```

Standby changes are also delivered through the listener callback:

```java
default void onTapStandbyStateChanged(@NonNull String tapIdentifier, boolean standby) { }
```

### IMU motion with orientation

```java
default void onImuMotionInputReceived(@NonNull String tapIdentifier, @NonNull ImuMotionPacket packet) {
    // packet.dx, packet.dy, packet.isMouse, packet.roll, packet.pitch, packet.yaw
}
```

All `get*` calls reply asynchronously on a `TapV2Callback`; if the device doesn't answer within 2 seconds the callback fires with a `null` value. Calling a framed-protocol API on classic firmware reports error `TapSdk.ERR_V2_NOT_SUPPORTED` (103) via `onError`. After standby, send the input mode / XR state again if events stop (`startXRTappingState` or `startXRAirMouseState`).

Example app
===========
The Android Studio project contains an example app (the `app` module) where you can see how to use some of the features of `TapSdk`. Each connected Tap is shown as a list row demonstrating:

* Tap input (finger combination + repeat), shift/switch state and mouse/air-gesture events.
* The detected BLE protocol (`Framed` or `Classic`, via `sdk.isV2Tap`).
* The last **raw** air gesture (`UnifiedAirGesture` on the framed protocol, `AirMousePacket` constants on classic firmware).
* The last **processed** air gesture (`onXRAirGestureReceived` / `XRAirGesture`: click, drag, drop, fist, swipe).
* Switching between Tapping and AirMouse detection by tapping the mode label (`startXRTappingState` / `startXRAirMouseState`).
* Changing input modes (text / controller / raw sensor...) by tapping the row, and sending haptics via `vibrate`.

Listener source: `app/src/main/java/com/tapwithus/tapsdk/MainActivity.java`.

AI-Assisted Development
=======================

Install the Tap skills into your coding agent, then describe the Android app you want. The agent knows the Android order: `TapSdkFactory.getDefault(context)`, then `registerTapListener`, then `resume()` / `pause()`. There is no `connect()` or `start()` call. `getDefault()` alone delivers no taps.

The installer writes into the **current folder**. Add `-g` to install for your user instead, so the skills are available in every project.

| Tool | This folder (default) | Every project (`-g`) |
|------|------------------------|----------------------|
| Claude Code | `.claude/skills/` | `claude plugin install … --scope user` |
| Codex | `.agents/skills/` | `codex plugin add …` |
| Cursor | `.cursor/skills/` + [`.cursor/rules/tap-sdk.mdc`](.cursor/rules/tap-sdk.mdc) | `~/.cursor/skills/` + `~/.cursor/rules/tap-sdk.mdc` |
| Any agent that reads `AGENTS.md` | [`AGENTS.md`](AGENTS.md) | `~/.codex/AGENTS.md` |

Run the commands below in your app's project folder (or from a clone of this repository).

```console
./install-skills.sh claude    # .claude/skills/
./install-skills.sh codex     # .agents/skills/
./install-skills.sh cursor    # .cursor/skills/ and .cursor/rules/
./install-skills.sh agents    # ./AGENTS.md
./install-skills.sh all       # all four, this folder
./install-skills.sh -g all    # all four, every project
```

Or, without a clone (after these files are on `master`):

```console
curl -sL https://raw.githubusercontent.com/TapWithUs/tap-android-sdk/master/install-skills.sh | bash -s cursor
curl -sL https://raw.githubusercontent.com/TapWithUs/tap-android-sdk/master/install-skills.sh | bash
curl -sL https://raw.githubusercontent.com/TapWithUs/tap-android-sdk/master/install-skills.sh | bash -s -- -g all
```

`-g claude` and `-g codex` need the `claude` and `codex` commands. You can also open `/plugins` in Codex and install **Tap Android SDK** from the marketplace list.

#### What's included

- **tap-android-getting-started**: Gradle dependency, Bluetooth permissions, pairing, connect, troubleshooting
- **tap-android-tapping**: which fingers tapped, finger combos, double taps, haptics, input modes
- **tap-android-vision-models**: tapping vs air-gesture models; raw `UnifiedAirGesture` and processed `XRAirGesture`
- **tap-android-imu-motion**: pointer movement, tilt, roll / pitch / yaw (framed protocol) and mouse events (classic)
- **tap-android-raw-sensors**: raw accelerometer and gyro streams, sensitivity
- **tap-android-knob**: hold a pinch and twist to turn a value up or down (framed protocol)
- **tap-android-dpad**: swipe for directions, pinch to select, hold to rotate or drag (framed protocol)
- **tap-android-build-an-app**: turn Tap events into an Android app (main thread, connection status, lifecycle)
- **tap-android-sdk**: method-by-method API reference

Skill source: [`plugins/tap-android-sdk/skills`](plugins/tap-android-sdk/skills).

#### Onboard your agent

1. Open your Android project in the coding agent.
2. Install the skills (see above).
3. Turn on your Tap, charge it, pair it in Android Bluetooth settings, and update its firmware with Tap Manager.
4. Tell the agent which device you have (Tap Strap, Tap Strap 2, or TapXR). TapBand uses the same API as TapXR and is coming soon.
5. Ask it to connect first: *"Use the tap-android-getting-started skill to connect to my Tap and show my taps."* Tap your fingers and tell the agent what you see.
6. When taps arrive, describe your app. Build one interaction at a time and try each one with the device.

Support
===========
Please refer to the issues tab.
