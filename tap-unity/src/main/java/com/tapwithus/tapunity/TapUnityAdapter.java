package com.tapwithus.tapunity;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import com.tapwithus.sdk.FeatureVersionSupport;
import com.tapwithus.sdk.TapListener;
import com.tapwithus.sdk.TapSdk;
import com.tapwithus.sdk.TapSdkFactory;
import com.tapwithus.sdk.airmouse.AirMousePacket;
import com.tapwithus.sdk.mode.TapInputMode;
import com.tapwithus.sdk.mouse.MousePacket;
import com.tapwithus.sdk.tap.Tap;
import com.tapwithus.sdk.mode.RawSensorData;
import com.unity3d.player.UnityPlayer;

@SuppressWarnings("unused")
public class TapUnityAdapter {

    private static final int ERR_CACHED_TAP = 2001;

    private static final String UNITY_ARGS_SEPARATOR = "|";

    private static final String UNITY_GAME_OBJECT = "TapInputAndroid";

    private static final String UNITY_BLUETOOTH_ON_CALLBACK = "onBluetoothTurnedOn";
    private static final String UNITY_BLUETOOTH_OFF_CALLBACK = "onBluetoothTurnedOff";
    private static final String UNITY_TAP_CONNECTED_CALLBACK = "onTapConnected";
    private static final String UNITY_TAP_DISCONNECTED_CALLBACK = "onTapDisconnected";
    private static final String UNITY_TAP_RESUMED_CALLBACK = "onTapResumed";
    private static final String UNITY_TAP_CHANGED_CALLBACK = "onTapChanged";
    private static final String UNITY_GET_CACHED_TAP_CALLBACK = "onCachedTapRetrieved";
    private static final String UNITY_CONTROLLER_MODE_CALLBACK = "onControllerModeStarted";
    private static final String UNITY_TEXT_MODE_CALLBACK = "onTextModeStarted";
    private static final String UNITY_TAP_INPUT_CALLBACK = "onTapInputReceived";
    private static final String UNITY_TAP_SHIFTSWITCH_CALLBACK = "onTapShiftSwitchReceived";
    private static final String UNITY_RAW_SENSOR_DATA_CALLBACK = "onRawSensorDataReceived";
    private static final String UNITY_MOUSE_INPUT_CALLBACK = "onMouseInputReceived";
    private static final String UNITY_AIRMOUSE_INPUT_CALLBACK = "onAirGestureInputReceived";
    private static final String UNITY_TAP_CHANGED_STATE_CALLBACK = "onTapChangedAirGestureState";
    private static final String UNITY_CONNECTED_TAPS_CALLBACK = "onConnectedTapsReceived";
    private static final String UNITY_GET_MODE_CALLBACK = "onModeReceived";
    private static final String UNITY_ERROR_CALLBACK = "onError";

    protected TapSdk tapSdk;
    private boolean debug = false;

    protected TapUnityAdapter() { }

    public TapUnityAdapter(Context context) {
//        tapSdk = sdk;
//        tapSdk.registerTapListener(tapListener);
        tapSdk = TapSdkFactory.getDefault(context);
        tapSdk.registerTapListener(tapListener);
    }

    public void enableDebug() {
        this.debug = true;
        tapSdk.enableDebug();
    }

    public void disableDebug() {
        this.debug = false;
        tapSdk.disableDebug();
    }

    public void resume() {
        log("resume");
        tapSdk.resume();
    }

    public void pause() {
        log("pause");
        tapSdk.pause();
    }

    public void destroy() {
        tapSdk.unregisterTapListener(tapListener);
        tapSdk.close();
    }

    // Air Mouse
//    public void setMouseHIDEnabledInRawModeForAllTaps(boolean enable) {
//        tapSdk.setMouseHIDEnabledInRawModeForAllTaps(enable);
//    }

    public boolean isAnyTapInAirGestureState() {
        return tapSdk.isAnyTapInAirGestureState();
    }

    public boolean isAnyTapSupportsAirGesture() {
        return tapSdk.isAnyTapSupportsAirGesture();
    }

    public void startControllerMode(@NonNull String tapIdentifier) {
        tapSdk.startControllerMode(tapIdentifier);
    }

    public void startTextMode(@NonNull String tapIdentifier) {
        tapSdk.startTextMode(tapIdentifier);
    }

    public void startControllerWithMouseHIDMode(@NonNull String tapIdentifier) {
        tapSdk.startControllerWithMouseHIDMode(tapIdentifier);
    }

    public void startControllerWithFullHIDMode(@NonNull String tapIdentifier) {
        tapSdk.startControllerWithFullHIDMode(tapIdentifier);
    }

    public void startRawSensorMode(@NonNull String tapIdentifier, int deviceAccelerometerSensitivity, int imuGyroSensitivity, int imuAccelerometerSensitivity) {
        tapSdk.startRawSensorMode(tapIdentifier,(byte)deviceAccelerometerSensitivity, (byte)imuGyroSensitivity, (byte)imuAccelerometerSensitivity);
    }

    public void setDefaultControllerMode(boolean apply) {
        tapSdk.setDefaultMode(TapInputMode.controller(), apply);
    }

    public void setDefaultTextMode(boolean apply) {
        tapSdk.setDefaultMode(TapInputMode.text(), apply);
    }

    public void setDefaultControllerWithMouseHIDMode(boolean apply) {
        tapSdk.setDefaultMode(TapInputMode.controllerWithMouseHID(), apply);
    }

    public void vibrate(@NonNull String tapIdentifier, @NonNull String durations, @NonNull String delimiter) {
        String newDelimiter = "\\" + delimiter;
        String[] dursSplit = durations.split(newDelimiter);
        int[] durs= new int[dursSplit.length];
        for (int i=0; i<dursSplit.length; i++) {
            try {
                int converted = Integer.parseInt(dursSplit[i].trim());
                if (i < durs.length) {
                    durs[i] = converted;
                }
            } catch (Exception e) {
                return;
            }
        }
        tapSdk.vibrate(tapIdentifier, durs);

    }

    public void getConnectedTaps() {
        String connectedTapsString = tapSdk.getConnectedTaps().toString();
        String connectedTapsArg = connectedTapsString
                .substring(1, connectedTapsString.length() - 1)
                .replaceAll(", ", UNITY_ARGS_SEPARATOR);
        UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_CONNECTED_TAPS_CALLBACK, connectedTapsArg);
    }

//    public void getMode(@NonNull String tapIdentifier) {
//        int mode = tapSdk.getMode(tapIdentifier);
//        String modeArg = tapIdentifier + UNITY_ARGS_SEPARATOR + mode;
//        UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_GET_MODE_CALLBACK, modeArg);
//    }

    public void getCachedTap(@NonNull String tapIdentifier) {
        Tap tap = tapSdk.getCachedTap(tapIdentifier);
        if (tap == null) {
            onError(tapIdentifier, ERR_CACHED_TAP, "Unable to retrieved cached Tap");
            return;
        }

        String cachedTapArg = tap.getIdentifier() + UNITY_ARGS_SEPARATOR +
                tap.getName() + UNITY_ARGS_SEPARATOR +
                tap.getBattery() + UNITY_ARGS_SEPARATOR +
                tap.getSerialNumber() + UNITY_ARGS_SEPARATOR +
                tap.getHwVer() + UNITY_ARGS_SEPARATOR +
                FeatureVersionSupport.semVerToInt(tap.getFwVer());

        UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_GET_CACHED_TAP_CALLBACK, cachedTapArg);
    }

    protected TapListener tapListener = new TapListener() {

        @Override
        public void onBluetoothTurnedOn() {
            log("Bluetooth turned ON");
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_BLUETOOTH_ON_CALLBACK, "");
        }

        @Override
        public void onBluetoothTurnedOff() {
            log("Bluetooth turned OFF");
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_BLUETOOTH_OFF_CALLBACK, "");
        }

        @Override
        public void onTapStartConnecting(@NonNull String tapIdentifier) {
            log("TAP start connecting " + tapIdentifier);
        }

        @Override
        public void onTapConnected(@NonNull String tapIdentifier) {
            log("TAP connected " + tapIdentifier);
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_TAP_CONNECTED_CALLBACK, tapIdentifier);
        }

        @Override
        public void onTapDisconnected(@NonNull String tapIdentifier) {
            log("TAP disconnected " + tapIdentifier);
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_TAP_DISCONNECTED_CALLBACK, tapIdentifier);
        }

        @Override
        public void onTapResumed(@NonNull String tapIdentifier) {
            log("TAP resumed " + tapIdentifier);
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_TAP_RESUMED_CALLBACK, tapIdentifier);
        }

        @Override
        public void onTapChanged(@NonNull String tapIdentifier) {
            log("TAP changed " + tapIdentifier);
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_TAP_CHANGED_CALLBACK, tapIdentifier);
        }

//        @Override
//        public void onControllerModeStarted(@NonNull String tapIdentifier) {
//            log("Controller mode started " + tapIdentifier);
//            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_CONTROLLER_MODE_CALLBACK, tapIdentifier);
//        }
//
//        @Override
//        public void onTextModeStarted(@NonNull String tapIdentifier) {
//            log("Text mode started " + tapIdentifier);
//            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_TEXT_MODE_CALLBACK, tapIdentifier);
//        }

        @Override
        public void onTapInputReceived(@NonNull String tapIdentifier, int data, int repeatData) {
            log(tapIdentifier + " TAP input received " + data);
            log("Repeat info = " + repeatData);

            String args = tapIdentifier + UNITY_ARGS_SEPARATOR + data + UNITY_ARGS_SEPARATOR + repeatData;
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_TAP_INPUT_CALLBACK, args);
        }

        @Override
        public void onTapShiftSwitchReceived(@NonNull String tapIdentifier, int data) {
            log(tapIdentifier + " TAP ShiftSwitch received " + data);

            String args = tapIdentifier + UNITY_ARGS_SEPARATOR + data;
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_TAP_SHIFTSWITCH_CALLBACK, args);
        }

        @Override
        public void onAirMouseInputReceived(@NonNull String tapIdentifier, @NonNull AirMousePacket data) {
            log(tapIdentifier + " TAP AirMouse input received " + data.gesture);
            String args = tapIdentifier + UNITY_ARGS_SEPARATOR + data.gesture.getInt();
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_AIRMOUSE_INPUT_CALLBACK, args);

        }

        @Override
        public void onRawSensorInputReceived(@NonNull String tapIdentifier,@NonNull RawSensorData rsData) {
            log(tapIdentifier = "TAP RawSensor Data received" + rsData.toString());
            String args = tapIdentifier + UNITY_ARGS_SEPARATOR + rsData.rawString("^");
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_RAW_SENSOR_DATA_CALLBACK, args);
        }

        @Override
        public void onTapChangedState(@NonNull String tapIdentifier, int state) {
            log(tapIdentifier + " TAP changed state " + state);
            String args = tapIdentifier + UNITY_ARGS_SEPARATOR + state;
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_TAP_CHANGED_STATE_CALLBACK, args);
        }

        @Override
        public void onMouseInputReceived(@NonNull String tapIdentifier, @NonNull MousePacket data) {
            log(tapIdentifier + " mouse input received " + data.dx.getInt() + ", " + data.dy.getInt());

            String args = tapIdentifier + UNITY_ARGS_SEPARATOR + data.dx.getInt() + UNITY_ARGS_SEPARATOR + data.dy.getInt() + UNITY_ARGS_SEPARATOR + data.proximity.getInt();
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_MOUSE_INPUT_CALLBACK, args);
        }

        @Override
        public void onError(@NonNull String tapIdentifier, int code, @NonNull String description) {
            log("Error - " + tapIdentifier + ", " + code + ", " + description);
            TapUnityAdapter.this.onError(tapIdentifier, code, description);
        }
    };

    private void onError(@NonNull String tapIdentifier, int code, @NonNull String description) {
        String args = tapIdentifier + UNITY_ARGS_SEPARATOR + code + UNITY_ARGS_SEPARATOR + description;
        UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_ERROR_CALLBACK, args);
    }

    private void log(@NonNull String message) {
        if (debug) {
            Log.d("TapUnityAdapter", message);
        }
    }
}
