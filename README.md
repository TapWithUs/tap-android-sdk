# TAP Official Android SDK

What Is This?
=============
TAP Android SDK allows you to build an Android app that can receive inputs from TAP devices,
In a way that each tap is being interpreted as an array of fingers that are tapped, or as a binary combination integer (explanation follows), Thus allowing the TAP device to act as a controller for your app!

Getting started
===============
To add TAP SDK library to your project:
- Make sure you have JCenter in your Gradle repositories.
- Add the following Gradle dependency to your build.gradle:
```Groovy
  implementation 'com.tapwithus:tap-android-sdk:0.1.10'
```

Getting instance of TapSdk
==========================
The entry point of TAP SDK is the `com.tapwithus.sdk.TapSdk` class.

If your application might be using the default `TapSdk` instance, a `TapSdk` single instance can be retrieved simply by using the `TapSdkFactory` helper class by calling `TapSdkFactory.getDefault(context)`. This helper method will return a single instance of `TapSdk` relatively to the passed context lifecycle.

If an alternative `TapSdk` instance is required, or if you might be using a dependency injection framework where custom scopes is implemented, call `new TapSdk(context.getApplicationContext(), BluetoothAdapter.getDefaultAdapter())` instead, passing the appropriate arguments.

Key features
============
##### Controller Mode & Text Mode
As stated in the official [Tap BLE API Documentation](https://www.tapwithus.com/api), once you turn ON the TAP device, by default, the TAP will be in Text Mode, meaning that the TAP functions as a bluetooth keyboard, every recognized tap will be mapped to a letter, and no input data will be sent to the SDK.

When using the SDK, it is required to get input data for a specific TAP device. In order to achieve this goal, after a connection with the TAP been established, we need to switch the TAP device to Controller Mode. In addition, it is important we switch back to Text Mode once the application goes to background, so the regular TAP behaviour will be restroed.

To simplify the process TAP SDK will perform the needed actions in order to correctly connect and switch between Modes automatically, __so you don't have to.__

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
    void onTapConnected(String tapIdentifier);
    void onTapDisconnected(String tapIdentifier);
    void onNameRead(String tapIdentifier, String name);
    void onNameWrite(String tapIdentifier, String name);
    void onCharacteristicRead(String tapIdentifier, UUID characteristic, byte[] data);
    void onCharacteristicWrite(String tapIdentifier, UUID characteristic, byte[] data);
    void onControllerModeStarted(String tapIdentifier);
    void onTextModeStarted(String tapIdentifier);
    void onTapInputReceived(String tapIdentifier, int data);
}
```
Just implement it and pass it to `TapSdk` class by calling `tapSdk.registerTapListener(tapListener)`.

__Important Note__:  
`void onTapInputReceived(String tapIdentifier, int data)` is a callback function which will be triggered every time a specific TAP device was being tapped, and which fingers were tapped. The returned data is an integer representing a 8-bit unsigned number, between 1 and 31. It's binary form represents the fingers that are tapped. The LSB is thumb finger, the MSB (bit number 5) is the pinky finger. For example: if combination equals 3 - it's binary form is 10100, Which means that the thumb and the middle fingers are tapped. For your convenience, you can convert the binary format into fingers boolean array by calling static funtion `TapSdk.toFingers(tapInput)` listed below.

TapSdk API
==========
#### `void resume()` and `void pause()`
> As mentioned, to correctly switch between Modes, `TapSdk` needs to be aware of your application's lifecycle, in particular when your application goes to the background and return from it, so it is needed for you to call the corresponding methods when such events occur.
&nbsp;
&nbsp;  
&nbsp;
#### `ArrayList<String> getConnectedTaps()`
> If you wish at any point in your application, you can receive a list of connected TAPs.

#### `void registerTapListener(TapListener listener)`
> Pass `TapListener` to get all `TapSdk` callbacks.

#### `void unregisterTapListener(TapListener listener)`
> Unregister registered `TapListener`.

#### `void startTextMode(String tapIdentifier)`
> If your application need to use the TAP device as regular bluetooth keyboard, you can manually switch to Text mode and passing the relevant TAP identifier.

#### `void startControllerMode(String tapIdentifier)`
> Manually switch to Controller mode, passing the relevant TAP identifier.

#### `boolean isControllerModeEnabled(String tapIdentifier)`
> Check if Controller Mode is enabled for a specific TAP device.

#### `void readName(String tapIdentifier)`
> Read TAP name.

#### `void writeName(String tapIdentifier, String name)`
> Write TAP name.

#### `void readCharacteristic(String tapAddress, UUID serviceUUID, UUID characteristicUUID)`
> Read characteristic from TAP device using given service UUID and characteristic UUID.

#### `void writeCharacteristic(String tapAddress, UUID serviceUUID, UUID characteristicUUID, byte[] data)`
> Write characteristic in TAP device using given service UUID and characteristic UUID.

#### `void close()`
> Releasing assosiated inner bluetooth manager.

#### `static boolean[] toFingers(int tapInput)`
> As said before, the `tapInput` is an unsigned 8-bit integer. to convert it to array of booleans:
```Java
boolean[] fingers = TapSdk.toFingers(tapInput);
```
> While:  
fingers[0] indicates if the thumb wqas tapped.  
fingers[1] indicates if the index finger was tapped.  
fingers[2] indicates if the middle finger was tapped.  
fingers[3] indicates if the ring finger was tapped.  
fingers[4] indicates if the pinky finger was tapped.

Debugging
=========
It is often desirable and useful to print out more `TapSdk` inner logs in LogCat. You can manually enable inner log prints by calling `tapSdk.enableDebug()`, and corresponding, you can disable inner log prints by calling `tapSdk.disableDebug`

Example app
===========
The Android Studio project contains an example app where you can see how to use the features of `TapSdk`.

Support
===========
Please refer to the issues tab.
