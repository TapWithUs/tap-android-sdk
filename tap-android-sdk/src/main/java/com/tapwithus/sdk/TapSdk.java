package com.tapwithus.sdk;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.tapwithus.sdk.bluetooth.BluetoothManager;
import com.tapwithus.sdk.bluetooth.MousePacket;
import com.tapwithus.sdk.bluetooth.TapBluetoothListener;
import com.tapwithus.sdk.bluetooth.TapBluetoothManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TapSdk {

    private static final int NUM_OF_MODES = 2;
    public static final int MODE_TEXT = 1;
    public static final int MODE_CONTROLLER = 2;

    private static final String TAG = "TapSdk";

    private TapBluetoothManager tapBluetoothManager;
    private ListenerManager<TapListener> tapListeners = new ListenerManager<>();
    private Map<String, Integer> modeSubscribers = new ConcurrentHashMap<>();
    private List<String> notifyOnConnectedAfterControllerModeStarted = new CopyOnWriteArrayList<>();

    public TapSdk(Context context, BluetoothAdapter bluetoothAdapter) {
        BluetoothManager bluetoothManager = new BluetoothManager(context, bluetoothAdapter);
        tapBluetoothManager = new TapBluetoothManager(bluetoothManager);
        tapBluetoothManager.registerTapBluetoothListener(tapBluetoothListener);
    }

    public void enableDebug() {
        tapBluetoothManager.enableDebug();
    }

    public void disableDebug() {
        tapBluetoothManager.disableDebug();
    }

    public void resume() {
        tapBluetoothManager.registerTapBluetoothListener(tapBluetoothListener);
        List<String> actuallyConnectTaps = getConnectedTaps();
        // Check if a new TAP was connected while the app was in background
        for (String tapIdentifier: actuallyConnectTaps) {
            if (!modeSubscribers.containsKey(tapIdentifier)) {
                modeSubscribers.put(tapIdentifier, MODE_CONTROLLER);
            }
        }
        List<String> controllerModeSubscribers = getTapsInMode(MODE_CONTROLLER);
        for (String tapIdentifier: controllerModeSubscribers) {
            if (actuallyConnectTaps.contains(tapIdentifier)) {
                tapBluetoothManager.startControllerMode(tapIdentifier);
            } else {
                controllerModeSubscribers.remove(tapIdentifier);
            }
        }
    }

    public void pause() {
        List<String> controllerModeSubscribers = getTapsInMode(MODE_CONTROLLER);
        for (String tapIdentifier: controllerModeSubscribers) {
            tapBluetoothManager.startTextMode(tapIdentifier);
        }
        tapBluetoothManager.unregisterTapBluetoothListener(tapBluetoothListener);
    }

    @NonNull
    public ArrayList<String> getConnectedTaps() {
        return tapBluetoothManager.getConnectedTaps();
    }

    public void refreshConnections() {
        tapBluetoothManager.refreshConnections();
    }

    public void registerTapListener(@NonNull TapListener listener) {
        tapListeners.registerListener(listener);
    }

    public void unregisterTapListener(@NonNull TapListener listener) {
        tapListeners.unregisterListener(listener);
    }

    public void startMode(String tapIdentifier, int mode) {
        if (!isModeValid(mode)) {
            Log.e(TAG, "subscribeMode - Invalid mode passed.");
            return;
        }

        modeSubscribers.put(tapIdentifier, mode);
        switch (mode) {
            case MODE_TEXT:
                tapBluetoothManager.startTextMode(tapIdentifier);
                break;
            case MODE_CONTROLLER:
                tapBluetoothManager.startControllerMode(tapIdentifier);
                break;
        }
    }

    private boolean isModeValid(int mode) {
        return mode >= 1 && mode >> NUM_OF_MODES <= 0;
    }

    public int getMode(String tapIdentifier) {
        return modeSubscribers.containsKey(tapIdentifier) ? modeSubscribers.get(tapIdentifier) : 0;
    }

    public List<String> getTapsInMode(int mode) {
        List<String> taps = new ArrayList<>();

        if (isModeValid(mode)) {
            for (Map.Entry<String, Integer> entry : modeSubscribers.entrySet()) {
                String tapIdentifier = entry.getKey();
                if (isInMode(tapIdentifier, mode)) {
                    taps.add(tapIdentifier);
                }
            }
        }

        return taps;
    }

    public boolean isInMode(String tapIdentifier, int mode) {
        if (!modeSubscribers.containsKey(tapIdentifier)) {
            return false;
        }
        return (modeSubscribers.get(tapIdentifier) & mode) == mode;
    }

    public void readName(@NonNull String tapIdentifier) {
        tapBluetoothManager.readName(tapIdentifier);
    }

    public void writeName(@NonNull String tapIdentifier, @NonNull String name) {
        tapBluetoothManager.writeName(tapIdentifier, name);
    }

    public void readCharacteristic(@NonNull String tapAddress, @NonNull UUID serviceUUID, @NonNull UUID characteristicUUID) {
        tapBluetoothManager.readCharacteristic(tapAddress, serviceUUID, characteristicUUID);
    }

    public void writeCharacteristic(@NonNull String tapAddress, @NonNull UUID serviceUUID, @NonNull UUID characteristicUUID, @NonNull byte[] data) {
        tapBluetoothManager.writeCharacteristic(tapAddress, serviceUUID, characteristicUUID, data);
    }

    public void setupNotification(@NonNull String tapAddress, @NonNull UUID serviceUUID, @NonNull UUID characteristicUUID) {
        tapBluetoothManager.setupNotification(tapAddress, serviceUUID, characteristicUUID);
    }

    public static boolean[] toFingers(int tapInput) {
        final boolean[] fingers = new boolean[5];
        for (int i = 0; i < 5; i++) {
            fingers[i] = (1 << i & tapInput) != 0;
        }
        return fingers;
    }

    public void close() {
        tapBluetoothManager.close();
    }

    private TapBluetoothListener tapBluetoothListener = new TapBluetoothListener() {

        @Override
        public void onBluetoothTurnedOn() {
            notifyOnBluetoothTurnedOn();
        }

        @Override
        public void onBluetoothTurnedOff() {
            notifyOnBluetoothTurnedOff();
        }

        @Override
        public void onTapConnected(String tapAddress) {
            List<String> textModeSubscribers = getTapsInMode(MODE_TEXT);
            if (textModeSubscribers.contains(tapAddress)) {
                modeSubscribers.put(tapAddress, MODE_TEXT);
                notifyOnTapConnected(tapAddress);
            } else {
                notifyOnConnectedAfterControllerModeStarted.add(tapAddress);
                startMode(tapAddress, MODE_CONTROLLER);
            }
        }

        @Override
        public void onTapAlreadyConnected(String tapAddress) {
            List<String> textModeSubscribers = getTapsInMode(MODE_TEXT);
            if (textModeSubscribers.contains(tapAddress)) {
                modeSubscribers.put(tapAddress, MODE_TEXT);
                notifyOnTapConnected(tapAddress);
            } else {
                notifyOnConnectedAfterControllerModeStarted.add(tapAddress);
                startMode(tapAddress, MODE_CONTROLLER);
            }
        }

        @Override
        public void onTapDisconnected(String tapAddress) {
            notifyOnTapDisconnected(tapAddress);
        }

        @Override
        public void onNameRead(String tapAddress, String name) {
            notifyOnNameRead(tapAddress, name);
        }

        @Override
        public void onNameWrite(String tapAddress, String name) {
            notifyOnNameWrite(tapAddress, name);
        }

        @Override
        public void onCharacteristicRead(String tapAddress, UUID characteristic, byte[] data) {
            notifyOnCharacteristicRead(tapAddress, characteristic, data);
        }

        @Override
        public void onCharacteristicWrite(String tapAddress, UUID characteristic, byte[] data) {
            notifyOnCharacteristicWrite(tapAddress, characteristic, data);
        }

        @Override
        public void onNotificationSubscribed(String tapAddress, UUID characteristic) {
            notifyOnNotificationSubscribed(tapAddress, characteristic);
        }

        @Override
        public void onNotificationReceived(String tapAddress, UUID characteristic, byte[] data) {
            notifyOnNotificationReceived(tapAddress, characteristic, data);
        }

        @Override
        public void onControllerModeStarted(String tapAddress) {
            Log.e("A", "onControllerModeStarted");
            if (notifyOnConnectedAfterControllerModeStarted.contains(tapAddress)) {
                notifyOnConnectedAfterControllerModeStarted.remove(tapAddress);
                notifyOnTapConnected(tapAddress);
            } else {
                notifyOnControllerModeStarted(tapAddress);
            }
        }

        @Override
        public void onTextModeStarted(String tapAddress) {
            Log.e("A", "onTextModeStarted");
            if (notifyOnConnectedAfterControllerModeStarted.contains(tapAddress)) {
                notifyOnConnectedAfterControllerModeStarted.remove(tapAddress);
                notifyOnTapConnected(tapAddress);
            } else {
                notifyOnTextModeStarted(tapAddress);
            }
        }

        @Override
        public void onTapInputReceived(String tapAddress, int data) {
            notifyOnTapInputReceived(tapAddress, data);
        }

        @Override
        public void onMouseInputReceived(String tapAddress, MousePacket data) {
            notifyOnMouseInputReceived(tapAddress, data);
        }
    };

    private void notifyOnBluetoothTurnedOn() {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onBluetoothTurnedOn();
            }
        });
    }

    private void notifyOnBluetoothTurnedOff() {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onBluetoothTurnedOff();
            }
        });
    }

    private void notifyOnTapConnected(final String tapIdentifier) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTapConnected(tapIdentifier);
            }
        });
    }

    private void notifyOnTapDisconnected(final String tapIdentifier) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTapDisconnected(tapIdentifier);
            }
        });
    }

    private void notifyOnNameRead(@NonNull final String tapIdentifier, @NonNull final String name) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onNameRead(tapIdentifier, name);
            }
        });
    }

    private void notifyOnNameWrite(@NonNull final String tapIdentifier, @NonNull final String name) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onNameWrite(tapIdentifier, name);
            }
        });
    }

    private void notifyOnCharacteristicRead(final String tapIdentifier, final UUID characteristic, final byte[] data) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onCharacteristicRead(tapIdentifier, characteristic, data);
            }
        });
    }

    private void notifyOnCharacteristicWrite(final String tapIdentifier, final UUID characteristic, final byte[] data) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onCharacteristicWrite(tapIdentifier, characteristic, data);
            }
        });
    }

    private void notifyOnNotificationSubscribed(final String tapIdentifier, final UUID characteristic) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onNotificationSubscribed(tapIdentifier, characteristic);
            }
        });
    }

    private void notifyOnNotificationReceived(final String tapIdentifier, final UUID characteristic, final byte[] data) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onNotificationReceived(tapIdentifier, characteristic, data);
            }
        });
    }

    private void notifyOnControllerModeStarted(final String tapIdentifier) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onControllerModeStarted(tapIdentifier);
            }
        });
    }

    private void notifyOnTextModeStarted(final String tapIdentifier) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTextModeStarted(tapIdentifier);
            }
        });
    }

    private void notifyOnTapInputReceived(final String tapIdentifier, final int data) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTapInputReceived(tapIdentifier, data);
            }
        });
    }

    private void notifyOnMouseInputReceived(final String tapIdentifier, final MousePacket data) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onMouseInputReceived(tapIdentifier, data);
            }
        });
    }
}
