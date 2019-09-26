package com.tapwithus.tapunity;

import android.support.annotation.NonNull;
import android.util.Log;

import com.tapwithus.sdk.FeatureVersionSupport;
import com.tapwithus.sdk.TapListener;
import com.tapwithus.sdk.TapSdk;
import com.tapwithus.sdk.mouse.MousePacket;
import com.tapwithus.sdk.tap.Tap;
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
    private static final String UNITY_MOUSE_INPUT_CALLBACK = "onMouseInputReceived";
    private static final String UNITY_CONNECTED_TAPS_CALLBACK = "onConnectedTapsReceived";
    private static final String UNITY_GET_MODE_CALLBACK = "onModeReceived";
    private static final String UNITY_ERROR_CALLBACK = "onError";

    protected TapSdk tapSdk;
    private boolean debug = false;

    protected TapUnityAdapter() { }

    public TapUnityAdapter(TapSdk sdk) {
        tapSdk = sdk;
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

    public void startControllerMode(@NonNull String tapIdentifier) {
        tapSdk.startMode(tapIdentifier, TapSdk.MODE_CONTROLLER);
    }

    public void startTextMode(@NonNull String tapIdentifier) {
        tapSdk.startMode(tapIdentifier, TapSdk.MODE_TEXT);
    }

    public void getConnectedTaps() {
        String connectedTapsString = tapSdk.getConnectedTaps().toString();
        String connectedTapsArg = connectedTapsString
                .substring(1, connectedTapsString.length() - 1)
                .replaceAll(", ", UNITY_ARGS_SEPARATOR);
        UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_CONNECTED_TAPS_CALLBACK, connectedTapsArg);
    }

    public void getMode(@NonNull String tapIdentifier) {
        int mode = tapSdk.getMode(tapIdentifier);
        String modeArg = tapIdentifier + UNITY_ARGS_SEPARATOR + mode;
        UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_GET_MODE_CALLBACK, modeArg);
    }

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

        @Override
        public void onControllerModeStarted(@NonNull String tapIdentifier) {
            log("Controller mode started " + tapIdentifier);
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_CONTROLLER_MODE_CALLBACK, tapIdentifier);
        }

        @Override
        public void onTextModeStarted(@NonNull String tapIdentifier) {
            log("Text mode started " + tapIdentifier);
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_TEXT_MODE_CALLBACK, tapIdentifier);
        }

        @Override
        public void onTapInputReceived(@NonNull String tapIdentifier, int data) {
            log(tapIdentifier + " TAP input received " + String.valueOf(data));

            String args = tapIdentifier + UNITY_ARGS_SEPARATOR + data;
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_TAP_INPUT_CALLBACK, args);
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
