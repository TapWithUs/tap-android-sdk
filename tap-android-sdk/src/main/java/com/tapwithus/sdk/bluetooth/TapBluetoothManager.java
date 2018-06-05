package com.tapwithus.sdk.bluetooth;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.tapwithus.sdk.ListenerManager;
import com.tapwithus.sdk.NotifyAction;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class TapBluetoothManager {

    private static final String TAG = "TapBluetoothManager";
    private static final String UTF8 = "UTF-8";
    private static final byte[] CONTROLLER_MODE_DATA = new byte[] { 0x3, 0xc, 0x0, 0x1 };
    private static final byte[] TEXT_MODE_DATA = new byte[] { 0x3, 0xc, 0x0, 0x0 };
    private static final int RAW_MODE_LOOP_DELAY = 10000;

    private static final UUID TAP = UUID.fromString("C3FF0001-1D8B-40FD-A56F-C7BD5D0F3370");
    private static final UUID TAP_DATA = UUID.fromString("C3FF0005-1D8B-40FD-A56F-C7BD5D0F3370");
    private static final UUID NUS = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID RX = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID NAME = UUID.fromString("C3FF0003-1D8B-40FD-A56F-C7BD5D0F3370");
    private static final UUID MOUSE_DATA = UUID.fromString("C3FF0006-1D8B-40FD-A56F-C7BD5D0F3370");

    protected BluetoothManager bluetoothManager;
    private List<String> tapInputSubscribers = new CopyOnWriteArrayList<>();
    private List<String> controllerModeSubscribers = new CopyOnWriteArrayList<>();
    private Handler rawModeHandler;
    private Runnable rawModeRunnable;
    private ListenerManager<TapBluetoothListener> tapBluetoothListeners = new ListenerManager<>();
    private Boolean debug = false;

    public TapBluetoothManager(@NonNull BluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
        this.bluetoothManager.registerBluetoothListener(bluetoothListener);
        startRawModeLoop();
    }

    public void enableDebug() {
        debug = true;
        bluetoothManager.enableDebug();
    }

    public void disableDebug() {
        debug = false;
        bluetoothManager.disableDebug();
    }

    public void registerTapBluetoothListener(@NonNull TapBluetoothListener listener) {
        tapBluetoothListeners.registerListener(listener);
    }

    public void unregisterTapBluetoothListener(@NonNull TapBluetoothListener listener) {
        tapBluetoothListeners.unregisterListener(listener);
    }

    @NonNull
    public ArrayList<String> getConnectedTaps() {
        return bluetoothManager.getConnectedDevices();
    }

    public void refreshConnections() {
        bluetoothManager.refreshConnections();
    }

    public void startControllerMode(@NonNull String tapAddress) {
        if (!tapInputSubscribers.contains(tapAddress)) {
            tapInputSubscribers.add(tapAddress);
            controllerModeSubscribers.add(tapAddress);
            enableControllerMode(tapAddress);
        }
    }

    public void startTextMode(@NonNull String tapAddress) {
        if (tapInputSubscribers.contains(tapAddress)) {
            tapInputSubscribers.remove(tapAddress);
            if (controllerModeSubscribers.contains(tapAddress)) {
                controllerModeSubscribers.remove(tapAddress);
            }
            enableTextMode(tapAddress);
        }
    }

    public void readName(@NonNull String tapAddress) {
        bluetoothManager.readCharacteristic(tapAddress, TAP, NAME);
    }

    public void writeName(@NonNull String tapAddress, @NonNull String name) {
        try {
            bluetoothManager.writeCharacteristic(tapAddress, TAP, NAME, name.getBytes(UTF8));
        } catch (UnsupportedEncodingException e) {
            logError("Unable to encode name");
        }
    }

    public void readCharacteristic(@NonNull String tapAddress, @NonNull UUID serviceUUID, @NonNull UUID characteristicUUID) {
        bluetoothManager.readCharacteristic(tapAddress, serviceUUID, characteristicUUID);
    }

    public void writeCharacteristic(@NonNull String tapAddress, @NonNull UUID serviceUUID, @NonNull UUID characteristicUUID, @NonNull byte[] data) {
        bluetoothManager.writeCharacteristic(tapAddress, serviceUUID, characteristicUUID, data);
    }

    public void close() {
        bluetoothManager.close();
        stopRawModeLoop();
    }

    private BluetoothListener bluetoothListener = new BluetoothListener() {

        @Override
        public void onBluetoothTurnedOn() {
            log("Bluetooth turned ON");
            notifyOnBluetoothTurnedOn();
        }

        @Override
        public void onBluetoothTurnedOff() {
            log("Bluetooth turned OFF");
            notifyOnBluetoothTurnedOff();
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            log("Device Connected");
            setupTapNotification(deviceAddress);
        }

        @Override
        public void onDeviceAlreadyConnected(String deviceAddress) {
            log("Device is Already Connected");
            notifyOnTapAlreadyConnected(deviceAddress);
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            log("Device Disconnected");
            if (tapInputSubscribers.contains(deviceAddress)) {
                tapInputSubscribers.remove(deviceAddress);
            }
            notifyOnTapDisconnected(deviceAddress);
        }

        @Override
        public void onCharacteristicRead(String deviceAddress, UUID characteristic, byte[] data) {
            log("Characteristic Read");
            if (characteristic.equals(NAME)) {
                try {
                    notifyOnNameRead(deviceAddress, new String(data, UTF8));
                } catch (UnsupportedEncodingException e) {
                    logError("Unable to encode name");
                }
            } else {
                notifyOnCharacteristicRead(deviceAddress, characteristic, data);
            }
        }

        @Override
        public void onCharacteristicWrite(String deviceAddress, UUID characteristic, byte[] data) {
            if (characteristic.equals(RX)) {
                if (Arrays.equals(data, CONTROLLER_MODE_DATA)) {
                    log("Controller Mode Started");
                    if (controllerModeSubscribers.contains(deviceAddress)) {
                        controllerModeSubscribers.remove(deviceAddress);
                        notifyOnControllerModeStarted(deviceAddress);
                    }
                } else if (Arrays.equals(data, TEXT_MODE_DATA)) {
                    log("Text Mode Started");
                    notifyOnTextModeStarted(deviceAddress);
                }
            } else if (characteristic.equals(NAME)) {
                try {
                    log("Name Changed");
                    notifyOnNameWrite(deviceAddress, new String(data, UTF8));
                } catch (UnsupportedEncodingException e) {
                    logError("Unable to encode name");
                }
            } else {
                log("Characteristic Write");
                notifyOnCharacteristicWrite(deviceAddress, characteristic, data);
            }
        }

        @Override
        public void onNotificationSubscribed(String deviceAddress, UUID characteristic) {
            log("Notification Subscribed");

            if (characteristic.equals(TAP_DATA)) {
                setupMouseNotification(deviceAddress);
            } else if (characteristic.equals(MOUSE_DATA)) {
                notifyOnTapConnected(deviceAddress);
            } else {
                notifyOnNotificationSubscribed(deviceAddress, characteristic);
            }
        }

        @Override
        public void onNotificationReceived(String deviceAddress, UUID characteristic, byte[] data) {
            if (data == null) {
                logError("Unable to read notification data");
                return;
            }

            log("Notification Received " + Arrays.toString(data));

            if (characteristic.equals(TAP_DATA)) {
                if (data[0] != 0) {
                    notifyOnTapInputReceived(deviceAddress, data[0]);
                }
            } else if (characteristic.equals(MOUSE_DATA)) {
                data = Arrays.copyOfRange(data, 1, data.length - 1);
                MousePacket mousePacket = new MousePacket(data);
                notifyOnMouseInputReceived(deviceAddress, mousePacket);
            }
        }
    };

    public void setupNotification(@NonNull String tapAddress, @NonNull UUID serviceUUID, @NonNull UUID characteristicUUID) {
        bluetoothManager.setupNotification(tapAddress, serviceUUID, characteristicUUID);
    }

    private void setupTapNotification(@NonNull String tapAddress) {
        bluetoothManager.setupNotification(tapAddress, TAP, TAP_DATA);
    }

    public void setupMouseNotification(@NonNull String tapAddress) {
        bluetoothManager.setupNotification(tapAddress, TAP, MOUSE_DATA);
    }

    private void startRawModeLoop() {
        if (rawModeHandler != null && rawModeRunnable != null) {
            log("Raw Mode Loop already exists");
            return;
        }

        log("startRawModeLoop");

        rawModeHandler = new Handler(Looper.getMainLooper());
        rawModeRunnable = new Runnable() {
            @Override
            public void run() {
                for (String tapAddress: tapInputSubscribers) {
                    enableControllerMode(tapAddress);
                }
                rawModeHandler.postDelayed(this, RAW_MODE_LOOP_DELAY);
            }
        };

        rawModeHandler.postDelayed(rawModeRunnable, 0);
    }

    private void stopRawModeLoop() {
        log("Stopping raw mode");

        if (rawModeHandler != null && rawModeRunnable != null) {
            rawModeHandler.removeCallbacks(rawModeRunnable);
        }
        rawModeHandler = null;
        rawModeRunnable = null;
    }

    private void enableControllerMode(String tapAddress) {
        bluetoothManager.writeCharacteristic(tapAddress, NUS, RX, CONTROLLER_MODE_DATA);
    }

    private void enableTextMode(String tapAddress) {
        bluetoothManager.writeCharacteristic(tapAddress, NUS, RX, TEXT_MODE_DATA);
    }

    private void notifyOnBluetoothTurnedOn() {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onBluetoothTurnedOn();
            }
        });
    }

    private void notifyOnBluetoothTurnedOff() {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onBluetoothTurnedOff();
            }
        });
    }

    private void notifyOnTapConnected(final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onTapConnected(tapAddress);
            }
        });
    }

    private void notifyOnTapAlreadyConnected(final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onTapAlreadyConnected(tapAddress);
            }
        });
    }

    private void notifyOnTapDisconnected(final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onTapDisconnected(tapAddress);
            }
        });
    }

    private void notifyOnNameRead(@NonNull final String tapAddress, @NonNull final String name) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onNameRead(tapAddress, name);
            }
        });
    }

    private void notifyOnNameWrite(@NonNull final String tapAddress, @NonNull final String name) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onNameWrite(tapAddress, name);
            }
        });
    }

    private void notifyOnCharacteristicRead(final String tapAddress, final UUID characteristic, final byte[] data) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onCharacteristicRead(tapAddress, characteristic, data);
            }
        });
    }

    private void notifyOnCharacteristicWrite(final String tapAddress, final UUID characteristic, final byte[] data) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onCharacteristicWrite(tapAddress, characteristic, data);
            }
        });
    }

    private void notifyOnNotificationSubscribed(final String tapAddress, final UUID characteristic) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onNotificationSubscribed(tapAddress, characteristic);
            }
        });
    }

    private void notifyOnControllerModeStarted(final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onControllerModeStarted(tapAddress);
            }
        });
    }

    private void notifyOnTextModeStarted(final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onTextModeStarted(tapAddress);
            }
        });
    }

    private void notifyOnTapInputReceived(final String tapAddress, final int data) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onTapInputReceived(tapAddress, data);
            }
        });
    }

    private void notifyOnMouseInputReceived(final String tapAddress, final MousePacket data) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onMouseInputReceived(tapAddress, data);
            }
        });
    }

    private void log(String message) {
        if (debug) {
            Log.d(TAG, message);
        }
    }

    private void logError(String message) {
        Log.e(TAG, message);
    }
}
