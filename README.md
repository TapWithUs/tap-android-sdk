# TAP Official Android SDK

What Is This?
=============
TAP Android SDK allows you to build an Android app that can receive inputs from TAP devices,
In a way that each tap is being interpreted as an array of fingers that are tapped, or as a binary combination integer (explanation follows), thus allowing the TAP device to act as a controller for your app!

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
    void onTapChangedState(@NonNull String tapIdentifier, @NonNull int state);
    void onError(@NonNull String tapIdentifier, int code, @NonNull String description);
}
```
Just implement it and pass it to `TapSdk` class by calling `sdk.registerTapListener(tapListener)`.

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


Example app
===========
The Android Studio project contains an example app where you can see how to use some of the features of `TapSdk`.

Support
===========
Please refer to the issues tab.
