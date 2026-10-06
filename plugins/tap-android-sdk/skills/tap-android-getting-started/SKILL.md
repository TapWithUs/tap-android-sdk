---
name: tap-android-getting-started
description: >-
  First skill for any Tap Android app. The order is
  TapSdkFactory.getDefault(context), then registerTapListener, then resume()
  in onResume and pause() in onPause. getDefault() alone delivers no taps.
  There is no connect() or start() method. Use for the Gradle dependency,
  Bluetooth permissions, pairing, "connect to my Tap", setup, or no tap events.
---

# Tap getting started (Android)

Works for both protocols. `TapSdk` talks to classic firmware (v1) and framed firmware (v2, TapBand and V2 TapXR). Check with `sdk.isV2Tap(tapIdentifier)`. Write code that handles both unless the user says which device they have.

The same calls work from Kotlin. Names below are the Java API in `com.tapwithus.sdk`.

## 1. Ask the user (once)

- Which device: Tap Strap, Tap Strap 2, TapXR, or TapBand?
- Android version (Android 12+ needs runtime Bluetooth permissions)?
- Is the Tap turned on, charged, and paired in Android Bluetooth settings?
- Is the firmware up to date (update it in the Tap Manager app)?

## 2. Install

`mavenCentral()` must be in the repositories. Add the dependency:

```groovy
implementation 'io.github.tapwithus:tap-android-sdk:0.3.6'
```

Declare permissions in the app `AndroidManifest.xml`. The SDK reads bonded devices. It does not scan or show a pair UI.

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

On Android 12 (API 31)+ request `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` at runtime before expecting connections. `BLUETOOTH_CONNECT` is what `getBondedDevices()` requires.

## 3. Connect and listen

There is no `connect()` and no `start()`. The required order is:

1. `TapSdk sdk = TapSdkFactory.getDefault(context);` — one instance for the process. It connects to Taps that are already paired.
2. `sdk.registerTapListener(listener);` — register immediately, before the first `onTapConnected`. A listener registered late misses the connection callback.
3. `sdk.resume()` in `onResume`, `sdk.pause()` in `onPause`. `pause()` puts connected Taps in Text mode. `resume()` restores the mode you set. If `isPaused` is true, a new connection does not start Controller mode and does not notify `onTapConnected`.
4. Enable input from `onTapConnected`:
   - v1: Controller mode is applied automatically while resumed. The SDK gets taps. Text mode types on the OS keyboard and sends **no** tap events.
   - v2: Controller mode enables `DeviceFeature.MODEL_DETECTION` and `IMU_MOTION_DATA` only. It does not pick a vision model. Call `sdk.startXRTappingState(tapIdentifier)` for taps, or `sdk.startXRAirMouseState(tapIdentifier)` for air gestures.
5. Keep the Activity (or Service) alive. Do not `finish()` when the Tap connects.

```java
public class TapActivity extends AppCompatActivity {
    private TapSdk sdk;
    private final TapListener listener = new EmptyTapListener() {
        @Override public void onTapConnected(String id) {
            if (sdk.isV2Tap(id)) {
                sdk.startXRTappingState(id);
            }
        }
        @Override public void onTapInputReceived(String id, int data, int repeatData) {
            boolean[] fingers = TapSdk.toFingers(data); // [thumb, index, middle, ring, pinky]
            runOnUiThread(() -> showFingers(fingers));
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sdk = TapSdkFactory.getDefault(this);
        sdk.registerTapListener(listener);
    }

    @Override protected void onResume() { super.onResume(); sdk.resume(); }
    @Override protected void onPause()  { super.onPause();  sdk.pause(); }
    @Override protected void onDestroy() {
        sdk.unregisterTapListener(listener);
        super.onDestroy();
    }
}
```

`TapListener` has many abstract methods. Only `onImuMotionInputReceived` and `onTapStandbyStateChanged` have defaults. Write a small base class in the app with empty bodies for the rest (the snippet's `EmptyTapListener` is that class — it is not in the SDK). Do not invent a `connect()` wrapper that hides `resume()` / `pause()`. The example app's `MainActivity` is a full listener.

## 4. What each protocol sends

| Callback | v1 | v2 |
|----------|----|----|
| `onTapInputReceived` | `data` bitmask, `repeatData` 1/2/3 | same bitmask, `repeatData` always 1 |
| `onAirMouseInputReceived` | `AirMousePacket.AIR_MOUSE_GESTURE_*` | `UnifiedAirGesture` codes 100–114 |
| mouse / motion | `onMouseInputReceived` (`dx`, `dy`, `proximity`) | also `onImuMotionInputReceived` (adds roll, pitch, yaw) |
| connect | `onTapConnected(tapIdentifier)` | same. `sdk.isV2Tap(id)` is true |

`tapIdentifier` is the Bluetooth MAC address. Pass it to every per-device call. `sdk.getConnectedTaps()` returns the set of connected ids.

## 5. Callback rules

- Callbacks run on the Bluetooth stack's thread, not necessarily the main thread. Do not touch Views directly. Post to `new Handler(Looper.getMainLooper())` or `runOnUiThread`.
- Keep the callback short. Do not block.
- `sdk.vibrate(id, new int[]{100, 100, 100})` gives haptic feedback (10–2500 ms per value, up to 18 values).
- `sdk.getCachedTap(id)` reads name, battery, firmware, and hardware version after the SDK has cached them (`onTapChanged` fires when cache fields update).

## Troubleshooting

| Problem | Fix |
|---------|-----|
| No device connects | Turn the Tap on, pair it in Android Bluetooth settings, close Tap Manager, grant Bluetooth permission, then reopen the app. |
| Logcat: no permission / SecurityException | Request `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` at runtime (Android 12+). |
| Connected but no tap events (v1) | Call `sdk.resume()` and `sdk.startControllerMode(id)`. |
| Connected but no tap events (v2) | Call `sdk.startXRTappingState(id)` after `onTapConnected`. |
| Letters appear in other apps when tapping | The Tap is in Text mode. `resume()` or `startControllerMode(id)`. |
| Events stop after leaving the app | `pause()` restores Text mode on purpose. `resume()` switches back. |
| `onError` code 103 | A V2-only API was called on a v1 Tap (`TapSdk.ERR_V2_NOT_SUPPORTED`). |
| Bluetooth is off | `onBluetoothTurnedOff` fires. Ask the user to turn Bluetooth on. |

Debug logging: `sdk.enableDebug()` and watch Logcat.

## More

- API listing: the `tap-android-sdk` skill (`reference.md`)
- Working example in this repo: `app/src/main/java/com/tapwithus/tapsdk/MainActivity.java`
- Next skills: `tap-android-tapping`, `tap-android-vision-models`, `tap-android-imu-motion`, `tap-android-raw-sensors`, `tap-android-knob`, `tap-android-dpad`, `tap-android-build-an-app`
