package com.tapwithus.tapunity;

import android.content.Context;
import android.util.Log;

import com.tapwithus.sdk.TapListener;
import com.tapwithus.sdk.TapSdk;
import com.tapwithus.sdk.TapSdkFactory;
import com.unity3d.player.UnityPlayer;

import java.util.Arrays;
import java.util.UUID;

public class TapUnityAdapter {

    private static final String UNITY_ARGS_DIVIDER = "|";
    private static final String UNITY_GAME_OBJECT = "TapInputAndroid";
    private static final String UNITY_BLUETOOTH_ON_CALLBACK = "onBluetoothTurnedOn";
    private static final String UNITY_BLUETOOTH_OFF_CALLBACK = "onBluetoothTurnedOff";
    private static final String UNITY_DEVICE_CONNECTED_CALLBACK = "onDeviceConnected";
    private static final String UNITY_DEVICE_DISCONNECTED_CALLBACK = "onDeviceDisconnected";
    private static final String UNITY_NAME_READ_CALLBACK = "onNameRead";
    private static final String UNITY_CONTROLLER_MODE_CALLBACK = "onControllerModeStarted";
    private static final String UNITY_TEXT_MODE_CALLBACK = "onTextModeStarted";
    private static final String UNITY_TAP_INPUT_CALLBACK = "onTapInputReceived";

    private TapSdk tapSdk;

    public TapUnityAdapter(Context context) {
        tapSdk = TapSdkFactory.getDefault(context);
    }

    public void enableDebug() {
        tapSdk.enableDebug();
    }

    public void disableDebug() {
        tapSdk.disableDebug();
    }

    public void resume() {
        tapSdk.registerTapListener(tapListener);
        tapSdk.resume();
    }

    public void pause() {
        tapSdk.pause();
        tapSdk.unregisterTapListener(tapListener);
    }

    public void destroy() {
        tapSdk.unregisterTapListener(tapListener);
        tapSdk.close();
    }

    public void startControllerMode(String tapIdentifier) {
        tapSdk.startControllerMode(tapIdentifier);
    }

    public void startTextMode(String tapIdentifier) {
        tapSdk.startTextMode(tapIdentifier);
    }

    public void readName(String tapIdentifier) {
        tapSdk.readName(tapIdentifier);
    }

    private TapListener tapListener = new TapListener() {

        @Override
        public void onBluetoothTurnedOn() {
            log("Bluetooth turned ON");
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_BLUETOOTH_ON_CALLBACK, null);
        }

        @Override
        public void onBluetoothTurnedOff() {
            log("Bluetooth turned OFF");
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_BLUETOOTH_OFF_CALLBACK, null);
        }

        @Override
        public void onTapConnected(String tapIdentifier) {
            log("TAP connected " + tapIdentifier);
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_DEVICE_CONNECTED_CALLBACK, tapIdentifier);
        }

        @Override
        public void onTapDisconnected(String tapIdentifier) {
            log("TAP disconnected " + tapIdentifier);
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_DEVICE_DISCONNECTED_CALLBACK, tapIdentifier);
        }

        @Override
        public void onNameRead(String tapIdentifier, String name) {
            log(tapIdentifier + " Name read " + name);

            String args = tapIdentifier + UNITY_ARGS_DIVIDER + name;
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_NAME_READ_CALLBACK, args);
        }

        @Override
        public void onNameWrite(String tapIdentifier, String name) {
            log(tapIdentifier + " Name write " + name);
        }

        @Override
        public void onCharacteristicRead(String tapIdentifier, UUID characteristic, byte[] data) {
            log(tapIdentifier + " " + characteristic.toString() + " Characteristic read " + Arrays.toString(data));
        }

        @Override
        public void onCharacteristicWrite(String tapIdentifier, UUID characteristic, byte[] data) {
            log(tapIdentifier + " " + characteristic.toString() + " Characteristic write " + Arrays.toString(data));
        }

        @Override
        public void onControllerModeStarted(String tapIdentifier) {
            log("Controller mode started " + tapIdentifier);
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_CONTROLLER_MODE_CALLBACK, tapIdentifier);
        }

        @Override
        public void onTextModeStarted(String tapIdentifier) {
            log("Text mode started " + tapIdentifier);
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_TEXT_MODE_CALLBACK, tapIdentifier);
        }

        @Override
        public void onTapInputReceived(String tapIdentifier, int data) {
            log(tapIdentifier + " TAP input received " + String.valueOf(data));

            String args = tapIdentifier + UNITY_ARGS_DIVIDER + data;
            UnityPlayer.UnitySendMessage(UNITY_GAME_OBJECT, UNITY_TAP_INPUT_CALLBACK, args);
        }
    };

    private void log(String message) {
        Log.e(this.getClass().getSimpleName(), message);
    }
}
