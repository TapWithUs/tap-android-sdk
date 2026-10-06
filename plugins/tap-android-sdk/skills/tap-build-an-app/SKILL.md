---
name: tap-build-an-app
description: >-
  Build a complete Android app or interactive experience controlled by a Tap —
  games, presentations, media control, or creative tools. Use when the user
  asks to build, make, or create something with their Tap on Android. Start
  with tap-getting-started: TapSdkFactory.getDefault, registerTapListener,
  resume and pause. getDefault() alone delivers no taps.
---

# Build an Android app with a Tap

The user may not be an Android developer. Keep the project small, say how to run it (Android Studio, Run 'app'), and test with the real device.

## Workflow

1. **Ask** (only what is missing): device (Tap Strap / Tap Strap 2 / TapXR / TapBand), Android version, and what the app should do.
2. **Connect first.** Follow `tap-getting-started` and confirm taps arrive before building anything else. Order: `TapSdkFactory.getDefault(context)`, `registerTapListener`, `resume()` / `pause()`.
3. **Pick the input** (load the matching skill):
   - finger taps and combos: `tap-tapping`
   - swipes, pinches, fist: `tap-vision-models` (v2)
   - pointer, tilt, rotation: `tap-imu-motion`
   - twist-to-adjust value: `tap-knob` (v2)
   - directions + select + drag/rotate: `tap-dpad` (v2)
   - data recording: `tap-raw-sensors`
4. **Build one interaction, test it with the user, then add the next.**
5. **Give feedback**: `sdk.vibrate(...)` and on-screen state, so the user knows the gesture was seen.
6. **Deliver**: one Android Studio module, the Maven dependency, and the permission block from `tap-getting-started`.

## Core pattern

SDK callbacks are not the main thread. Copy the event and update the UI on the main looper. Do not draw in the callback.

```java
private final Handler main = new Handler(Looper.getMainLooper());

@Override
public void onTapInputReceived(String id, int data, int repeatData) {
    main.post(() -> {
        status.setText("Connected");
        applyTap(data);
    });
}
```

Show connection status on screen: "Waiting for Tap…", then the device name from `sdk.getCachedTap(id)` inside `onTapConnected` / `onTapChanged`, and "Disconnected" from `onTapDisconnected`.

Add a keyboard or button fallback (volume keys, on-screen buttons) so the screen can be tried in the emulator with no Tap. The emulator will not connect to a real Tap.

## Lifecycle

```java
@Override protected void onResume() { super.onResume(); sdk.resume(); }
@Override protected void onPause()  { super.onPause();  sdk.pause(); }
```

`pause()` returns the Tap to Text mode so it works as a keyboard in other apps. That is the right default.

If the app must keep receiving taps while stopped, call `sdk.disablePauseResumeHandling()` and tell the user the Tap will not return to keyboard mode by itself. Do this only when they asked for background input.

Unregister the listener in `onDestroy`. Call `sdk.close()` only when the process is done with Bluetooth (it releases the shared manager from `TapSdkFactory`).

## Output

| Output | How |
|--------|-----|
| On-screen UI | Views or Jetpack Compose. Post callback results to the main thread. |
| Sound | `SoundPool` or `ToneGenerator` from the main thread. |
| Another Activity in the same app | Start it from the main-thread handler. |
| The system back / home action | An `AccessibilityService` the user turns on in Settings. Prefer in-app controls. Do not ship an accessibility service for an app that only needs its own UI. |

Do not add libraries to the SDK module. App dependencies belong in the app `build.gradle`.

## Rules

- Dependency: `implementation 'io.github.tapwithus:tap-android-sdk:0.3.6'` with `mavenCentral()`.
- Handle v1 and v2 unless the user named the device. `sdk.isV2Tap(id)`.
- One `TapSdk` can see several paired Taps. Key state by `tapIdentifier`.
- `minSdk` of the SDK is 23. Runtime Bluetooth permission is required on API 31+.
- If the device does not connect, use the troubleshooting table in `tap-getting-started`.
- There is no simulator. Ask the user what they see on the device.
