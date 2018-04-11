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
  implementation 'com.tapwithus:tap-sdk:1.0.4'
```

Getting instance of TapSdk
==========================
The entry point of TAP SDK is the `com.tapwithus.sdk.TapSdk` class.

If your application might be using the default `TapSdk` instance, a `TapSdk` single instance can be retrieved simply by using the `TapSdkFactory` helper class by calling `TapSdkFactory.getDefault(context)`. This helper method will return a single instance of `TapSdk` relatively to the passed context lifecycle.

If an alternative `TapSdk` instance is required, or if you might be using a dependency injection framework where custom scopes is implemented, call `new TapSdk(context.getApplicationContext(), BluetoothAdapter.getDefaultAdapter())` instead, passing the appropriate arguments.

Key features
============
##### Controller Mode & Text Mode
As stated in the official [Tap BLE API Documentation](https://www.tapwithus.com/wp-content/uploads/2018/04/TapBLEAPIdocumentation_1_0_0_20180408-1.pdf), once you turn ON the TAP device, by default, the TAP will be in Text Mode, meaning that the TAP functions as a bluetooth keyboard, every recognized tap will be mapped to a letter, and no input data will be sent to the SDK.

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
`com.tapwithus.sdk.TapListener` is an interface, describing the various data you can retrieve from `TapSdk`.

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

Debugging
=========
It is often desirable and useful to print out more `TapSdk` inner logs in LogCat. You can manually enable inner log prints by calling `tapSdk.enableDebug()`, and corresponding, you can disable inner log prints by calling `tapSdk.disableDebug`

Example app
===========
The Android Studio project contains an example app where you can see how to use the features of `TapSdk`.

Support
===========
Please refer to the issues tab.
