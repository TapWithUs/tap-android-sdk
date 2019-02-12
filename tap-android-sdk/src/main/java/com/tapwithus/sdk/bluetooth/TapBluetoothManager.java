package com.tapwithus.sdk.bluetooth;

import android.support.annotation.NonNull;
import android.util.Log;

import com.tapwithus.sdk.ListenerManager;
import com.tapwithus.sdk.NotifyAction;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class TapBluetoothManager {

    private static final String TAG = "TapBluetoothManager";
    private static final String UTF8 = "UTF-8";
    private static final byte[] CONTROLLER_MODE_DATA = new byte[] { 0x3, 0xc, 0x0, 0x1 };
    private static final byte[] TEXT_MODE_DATA = new byte[] { 0x3, 0xc, 0x0, 0x0 };

    protected static final UUID TAP = UUID.fromString("C3FF0001-1D8B-40FD-A56F-C7BD5D0F3370");
    protected static final UUID DEVICE_INFORMATION = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    protected static final UUID BATTERY = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    protected static final UUID BATTERY_LEVEL = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB");
    protected static final UUID SERIAL_NAME_STRING = UUID.fromString("00002A25-0000-1000-8000-00805F9B34FB");
    protected static final UUID HARDWARE_REVISION_STRING = UUID.fromString("00002A27-0000-1000-8000-00805F9B34FB");
    protected static final UUID FIRMWARE_REVISION_STRING = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB");
    protected static final UUID TAP_DATA = UUID.fromString("C3FF0005-1D8B-40FD-A56F-C7BD5D0F3370");
    protected static final UUID NUS = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    protected static final UUID RX = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    protected static final UUID NAME = UUID.fromString("C3FF0003-1D8B-40FD-A56F-C7BD5D0F3370");
    protected static final UUID MOUSE_DATA = UUID.fromString("C3FF0006-1D8B-40FD-A56F-C7BD5D0F3370");

    public static final int ERR_ENCODE = 51;

    protected BluetoothManager bluetoothManager;
    private ListenerManager<TapBluetoothListener> tapBluetoothListeners = new ListenerManager<>();
    private boolean debug = false;

    public TapBluetoothManager(@NonNull BluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
        this.bluetoothManager.registerBluetoothListener(bluetoothListener);
    }

    public void enableDebug() {
        debug = true;
        bluetoothManager.enableDebug();
    }

    public void disableDebug() {
        debug = false;
        bluetoothManager.disableDebug();
    }

    public boolean isClosing() {
        return bluetoothManager.isClosing();
    }

    public int numOfConnectedTaps() {
        return bluetoothManager.numOfConnectedDevices();
    }

    public void registerTapBluetoothListener(@NonNull TapBluetoothListener listener) {
        tapBluetoothListeners.registerListener(listener);
    }

    public void unregisterTapBluetoothListener(@NonNull TapBluetoothListener listener) {
        tapBluetoothListeners.unregisterListener(listener);
    }

    @NonNull
    public Set<String> getConnectedTaps() {
        return bluetoothManager.getConnectedDevices();
    }

    public void refreshConnections() {
        bluetoothManager.refreshConnections();
    }

    public void startControllerMode(@NonNull String tapAddress) {
        bluetoothManager.writeCharacteristic(tapAddress, NUS, RX, CONTROLLER_MODE_DATA);
    }

    public void startTextMode(@NonNull String tapAddress) {
        bluetoothManager.writeCharacteristic(tapAddress, NUS, RX, TEXT_MODE_DATA);
    }

    public void readName(@NonNull String tapAddress) {
        log("readName");
        bluetoothManager.readCharacteristic(tapAddress, TAP, NAME);
    }

    public void writeName(@NonNull String tapAddress, @NonNull String name) {
        log("writeName");
        try {
            bluetoothManager.writeCharacteristic(tapAddress, TAP, NAME, name.getBytes(UTF8));
        } catch (UnsupportedEncodingException e) {
            notifyOnError(tapAddress, ERR_ENCODE, "Unable to encode name");
        }
    }

    public void readBattery(@NonNull String tapAddress) {
        log("readBattery");
        bluetoothManager.readCharacteristic(tapAddress, BATTERY, BATTERY_LEVEL);
    }

    public void readSerialNumber(@NonNull String tapAddress) {
        log("readSerialNumber");
        bluetoothManager.readCharacteristic(tapAddress, DEVICE_INFORMATION, SERIAL_NAME_STRING);
    }

    public void readHwVer(@NonNull String tapAddress) {
        log("readHwVer");
        bluetoothManager.readCharacteristic(tapAddress, DEVICE_INFORMATION, HARDWARE_REVISION_STRING);
    }

    public void readFwVer(@NonNull String tapAddress) {
        log("readFwVer");
        bluetoothManager.readCharacteristic(tapAddress, DEVICE_INFORMATION, FIRMWARE_REVISION_STRING);
    }

    public void setupTapNotification(@NonNull String tapAddress) {
        log("setupTapNotification");
        bluetoothManager.setupNotification(tapAddress, TAP, TAP_DATA);
    }

    public void setupMouseNotification(@NonNull String tapAddress) {
        log("setupMouseNotification");
        bluetoothManager.setupNotification(tapAddress, TAP, MOUSE_DATA);
    }

    public boolean isConnectionInProgress() {
        return bluetoothManager.isConnectionInProgress();
    }

    public boolean isConnectionInProgress(@NonNull String deviceAddress) {
        return bluetoothManager.isConnectionInProgress(deviceAddress);
    }

    public void close() {
        bluetoothManager.close();
    }

    public void restartBluetooth() {
        bluetoothManager.restartBluetooth();
    }

    public void refreshBond(@NonNull String tapAddress) {
        bluetoothManager.refreshBond(tapAddress);
    }

    @SuppressWarnings("FieldCanBeLocal")
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
        public void onDeviceStartConnecting(@NonNull String deviceAddress) {
            log("Started connecting to device - " + deviceAddress);
            notifyOnTapStartConnecting(deviceAddress);
        }

        @Override
        public void onDeviceConnected(@NonNull String deviceAddress) {
            log("Device Connected");
            notifyOnTapConnected(deviceAddress);
        }

        @Override
        public void onDeviceAlreadyConnected(@NonNull String deviceAddress) {
            log("Device is Already Connected");
            notifyOnTapAlreadyConnected(deviceAddress);
        }

        @Override
        public void onDeviceDisconnected(@NonNull String deviceAddress) {
            log("Device Disconnected");
            notifyOnTapDisconnected(deviceAddress);

//            if (bluetoothManager.isClosing() && bluetoothManager.numOfConnectedDevices() == 0) {
//                tapBluetoothListeners.removeAllListeners();
//            }
        }

        @Override
        public void onCharacteristicRead(@NonNull String deviceAddress, @NonNull UUID characteristic, @NonNull byte[] data) {
            log("Characteristic Read");
            if (characteristic.equals(NAME)) {
                try {
                    notifyOnNameRead(deviceAddress, new String(data, UTF8));
                } catch (UnsupportedEncodingException e) {
                    notifyOnError(deviceAddress, ERR_ENCODE, "Unable to encode name");
                }
            } else if (characteristic.equals(BATTERY_LEVEL)) {
                notifyOnBatteryRead(deviceAddress, data[0] & 0xFF);
            } else if (characteristic.equals(SERIAL_NAME_STRING)) {
                notifyOnSerialNumberRead(deviceAddress, new String(data));
            } else if (characteristic.equals(HARDWARE_REVISION_STRING)) {
                notifyOnHwVerRead(deviceAddress, new String(data));
            } else if (characteristic.equals(FIRMWARE_REVISION_STRING)) {
                notifyOnFwVerRead(deviceAddress, new String((data)));
            }
        }

        @Override
        public void onCharacteristicWrite(@NonNull String deviceAddress, @NonNull UUID characteristic, @NonNull byte[] data) {
            if (characteristic.equals(RX)) {
                if (Arrays.equals(data, CONTROLLER_MODE_DATA)) {
                    log("Controller Mode Started");
                    notifyOnControllerModeStarted(deviceAddress);
                } else if (Arrays.equals(data, TEXT_MODE_DATA)) {
                    log("Text Mode Started");
                    notifyOnTextModeStarted(deviceAddress);
                }
            } else if (characteristic.equals(NAME)) {
                try {
                    log("Name Changed");
                    notifyOnNameWrite(deviceAddress, new String(data, UTF8));
                } catch (UnsupportedEncodingException e) {
                    notifyOnError(deviceAddress, ERR_ENCODE, "Unable to encode name");
                }
            }
        }

        @Override
        public void onNotificationSubscribed(@NonNull String deviceAddress, @NonNull UUID characteristic) {
            log("Notification Subscribed");

            if (characteristic.equals(TAP_DATA)) {
                notifyOnTapInputSubscribed(deviceAddress);
            } else if (characteristic.equals(MOUSE_DATA)) {
                notifyOnMouseInputSubscribed(deviceAddress);
            }
        }

        @Override
        public void onNotificationReceived(@NonNull String deviceAddress, @NonNull UUID characteristic, @NonNull byte[] data) {
            log("Notification Received " + Arrays.toString(data));

            if (characteristic.equals(TAP_DATA)) {
                if (data[0] != 0) {
                    notifyOnTapInputReceived(deviceAddress, data[0]);
                }
            } else if (characteristic.equals(MOUSE_DATA)) {
                data = Arrays.copyOfRange(data, 1, data.length);
                MousePacket mousePacket = new MousePacket(data);
                notifyOnMouseInputReceived(deviceAddress, mousePacket);
            }
        }

        @Override
        public void onError(@NonNull String deviceAddress, int code, @NonNull String description) {
            notifyOnError(deviceAddress, code, description);
        }
    };

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

    private void notifyOnTapStartConnecting(final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onTapStartConnecting(tapAddress);
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

    private void notifyOnBatteryRead(@NonNull final String tapAddress, final int battery) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onBatteryRead(tapAddress, battery);
            }
        });
    }

    private void notifyOnSerialNumberRead(@NonNull final String tapAddress, @NonNull final String serialNumber) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onSerialNumberRead(tapAddress, serialNumber);
            }
        });
    }

    private void notifyOnHwVerRead(@NonNull final String tapAddress, @NonNull final String hwVer) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onHwVerRead(tapAddress, hwVer);
            }
        });
    }

    private void notifyOnFwVerRead(@NonNull final String tapAddress, @NonNull final String fwVer) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onFwVerRead(tapAddress, fwVer);
            }
        });
    }

    private void notifyOnControllerModeStarted(@NonNull final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onControllerModeStarted(tapAddress);
            }
        });
    }

    private void notifyOnTextModeStarted(@NonNull final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onTextModeStarted(tapAddress);
            }
        });
    }

    private void notifyOnTapInputSubscribed(@NonNull final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onTapInputSubscribed(tapAddress);
            }
        });
    }

    private void notifyOnMouseInputSubscribed(@NonNull final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onMouseInputSubscribed(tapAddress);
            }
        });
    }

    private void notifyOnTapInputReceived(@NonNull final String tapAddress, final int data) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onTapInputReceived(tapAddress, data);
            }
        });
    }

    private void notifyOnMouseInputReceived(@NonNull final String tapAddress, @NonNull final MousePacket data) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onMouseInputReceived(tapAddress, data);
            }
        });
    }

    private void notifyOnError(@NonNull final String tapAddress, final int code, @NonNull final String description) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onError(tapAddress, code, description);
            }
        });
    }

    protected void log(String message) {
        if (debug) {
            Log.d(TAG, message);
        }
    }

    protected void logError(String message) {
        Log.e(TAG, message);
    }
}
