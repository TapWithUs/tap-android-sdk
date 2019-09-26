# TAP Official Android SDK

What Is This?
=============
TAP Android SDK allows you to build an Android app that can receive inputs from TAP devices,
In a way that each tap is being interpreted as an array of fingers that are tapped, or as a binary combination integer (explanation follows), thus allowing the TAP device to act as a controller for your app!

Getting started
===============
To add TAP SDK library to your project:
- Make sure you have JCenter in your Gradle repositories.
- Add the following Gradle dependency to your build.gradle:
```Groovy
  implementation 'com.tapwithus:tap-android-sdk:0.3.3'
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
##### Controller Mode & Text Mode
As stated in the official [Tap BLE API Documentation](https://www.tapwithus.com/api), once you turn ON the TAP device, by default, it will be booted into Text Mode, meaning that the TAP device functions as a bluetooth keyboard, every recognized tap will be mapped to a letter, and __no input data will be sent to the SDK__.

When using the SDK, it is required to get the input data for a specific TAP device. In order to achieve this goal, after a connection with the TAP been established, we need to switch the TAP device to Controller Mode. In addition, it is important we switch back to Text Mode once the application goes to background, so the regular TAP behaviour will be restored.

To simplify the process TAP SDK will perform the needed actions in order to correctly connect and switch between Modes _automatically_, __so you don't have to.__

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
    void onTapStartConnecting(String tapIdentifier);
    void onTapConnected(String tapIdentifier);
    void onTapDisconnected(String tapIdentifier);
    void onTapResumed(String tapIdentifier);
    void onTapChanged(String tapIdentifier);
    void onControllerModeStarted(String tapIdentifier);
    void onTextModeStarted(String tapIdentifier);
    void onTapInputReceived(String tapIdentifier, int data);
    void onMouseInputReceived(String tapIdentifier, MousePacket data);
    void onError(String tapIdentifier, int code, String description);
}
```
Just implement it and pass it to `TapSdk` class by calling `sdk.registerTapListener(tapListener)`.

___
#### Important Note:  
`onTapInputReceived` is a callback function which will be triggered every time a specific TAP device was being tapped, and which fingers were tapped. The returned data is an integer representing a 8-bit unsigned number, between 1 and 31. It's binary form represents the fingers that are tapped. The LSB is thumb finger, the MSB (bit number 5) is the pinky finger. For example: if combination equals 3 - it's binary form is 10100, Which means that the thumb and the middle fingers were tapped. For your convenience, you can convert the binary format into fingers boolean array by calling static function `TapSdk.toFingers(tapInput)` listed below.
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
> As described in the 'Key features' section, the default `TapSdk` behaviour is to switch the connected TAP device to controller mode once it connected. Calling `disableAutoSetControllerModeOnConnection` method will disable this functionality, so each connected TAP device will remain in its initial Mode.
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
> If your application needs to use the TAP device as a regular bluetooth keyboard, you can manually switch to Text mode by passing the relevant TAP identifier and `TapSdk.MODE_TEXT` as the second argument. Or you can manually switch to Controller mode, by passing the relevant TAP identifier and `TapSdk.MODE_CONTROLLER`.
&nbsp;
&nbsp;
#### `int getMode(String tapIdentifier)`
> Retrieve the TAP's Mode. Can be `MODE_TEXT` or `MODE_CONTROLLER`.
&nbsp;
&nbsp;
#### `boolean isInMode(String tapIdentifier, int mode)`
> Another helper method to check what Mode is enabled for a specific TAP device.
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

Example app
===========
The Android Studio project contains an example app where you can see how to use some of the features of `TapSdk`.

Support
===========
Please refer to the issues tab.
