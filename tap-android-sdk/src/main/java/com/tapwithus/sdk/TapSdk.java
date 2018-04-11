package com.tapwithus.sdk;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.support.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.BluetoothManager;
import com.tapwithus.sdk.bluetooth.TapBluetoothListener;
import com.tapwithus.sdk.bluetooth.TapBluetoothManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class TapSdk {

    private TapBluetoothManager tapBluetoothManager;
    private ListenerManager<TapListener> tapListeners = new ListenerManager<>();
    private List<String> controllerModeSubscribers = new CopyOnWriteArrayList<>();
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
        for (String tapIdentifier: controllerModeSubscribers) {
            if (actuallyConnectTaps.contains(tapIdentifier)) {
                tapBluetoothManager.startControllerMode(tapIdentifier);
            } else {
                controllerModeSubscribers.remove(tapIdentifier);
            }
        }
        refreshConnections();
    }

    public void pause() {
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

    public void startControllerMode(@NonNull String tapIdentifier) {
        if (!controllerModeSubscribers.contains(tapIdentifier)) {
            controllerModeSubscribers.add(tapIdentifier);
        }
        tapBluetoothManager.startControllerMode(tapIdentifier);
    }

    public boolean isControllerModeEnabled(String tapIdentifier) {
        return controllerModeSubscribers.contains(tapIdentifier);
    }

    public void startTextMode(@NonNull String tapIdentifier) {
        if (controllerModeSubscribers.contains(tapIdentifier)) {
            controllerModeSubscribers.remove(tapIdentifier);
        }
        tapBluetoothManager.startTextMode(tapIdentifier);
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
            notifyOnTapConnected(tapAddress);
            if (controllerModeSubscribers.contains(tapAddress)) {
                tapBluetoothManager.startControllerMode(tapAddress);
            }
        }

        @Override
        public void onTapAlreadyConnected(String tapAddress) {
            if (controllerModeSubscribers.contains(tapAddress)) {
                tapBluetoothManager.startControllerMode(tapAddress);
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
        public void onControllerModeStarted(String tapAddress) {
            if (notifyOnConnectedAfterControllerModeStarted.contains(tapAddress)) {
                notifyOnConnectedAfterControllerModeStarted.remove(tapAddress);
                notifyOnTapConnected(tapAddress);
            } else {
                notifyOnControllerModeStarted(tapAddress);
            }
        }

        @Override
        public void onTextModeStarted(String tapAddress) {
            notifyOnTextModeStarted(tapAddress);
        }

        @Override
        public void onTapInputReceived(String tapAddress, int data) {
            notifyOnTapInputReceived(tapAddress, data);
        }

        @Override
        public void onMouseInputReceived(String tapAddress, byte[] data) {

        }
    };

    private void notifyOnBluetoothTurnedOn() {
        tapListeners.notifyListeners(new ListenerManager.NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onBluetoothTurnedOn();
            }
        });
    }

    private void notifyOnBluetoothTurnedOff() {
        tapListeners.notifyListeners(new ListenerManager.NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onBluetoothTurnedOff();
            }
        });
    }

    private void notifyOnTapConnected(final String tapIdentifier) {
        tapListeners.notifyListeners(new ListenerManager.NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTapConnected(tapIdentifier);
            }
        });
    }

    private void notifyOnTapDisconnected(final String tapIdentifier) {
        tapListeners.notifyListeners(new ListenerManager.NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTapDisconnected(tapIdentifier);
            }
        });
    }

    private void notifyOnNameRead(@NonNull final String tapIdentifier, @NonNull final String name) {
        tapListeners.notifyListeners(new ListenerManager.NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onNameRead(tapIdentifier, name);
            }
        });
    }

    private void notifyOnNameWrite(@NonNull final String tapIdentifier, @NonNull final String name) {
        tapListeners.notifyListeners(new ListenerManager.NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onNameWrite(tapIdentifier, name);
            }
        });
    }

    private void notifyOnCharacteristicRead(final String tapIdentifier, final UUID characteristic, final byte[] data) {
        tapListeners.notifyListeners(new ListenerManager.NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onCharacteristicRead(tapIdentifier, characteristic, data);
            }
        });
    }

    private void notifyOnCharacteristicWrite(final String tapIdentifier, final UUID characteristic, final byte[] data) {
        tapListeners.notifyListeners(new ListenerManager.NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onCharacteristicWrite(tapIdentifier, characteristic, data);
            }
        });
    }

    private void notifyOnControllerModeStarted(final String tapIdentifier) {
        tapListeners.notifyListeners(new ListenerManager.NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onControllerModeStarted(tapIdentifier);
            }
        });
    }

    private void notifyOnTextModeStarted(final String tapIdentifier) {
        tapListeners.notifyListeners(new ListenerManager.NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTextModeStarted(tapIdentifier);
            }
        });
    }

    private void notifyOnTapInputReceived(final String tapIdentifier, final int data) {
        tapListeners.notifyListeners(new ListenerManager.NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTapInputReceived(tapIdentifier, data);
            }
        });
    }
}
