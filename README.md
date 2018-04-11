# tap-android-sdk

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

Debugging
=========
It is often desirable and useful to print out more `TapSdk` inner logs in LogCat. You can manually enable inner log prints by calling `tapSdk.enableDebug()`, and corresponding, you can disable inner log prints by calling `tapSdk.disableDebug`

Support
===========
Please refer to the issues tab.
