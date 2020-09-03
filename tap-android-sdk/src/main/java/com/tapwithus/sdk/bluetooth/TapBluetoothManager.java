package com.tapwithus.sdk.bluetooth;

import android.util.Log;

import androidx.annotation.NonNull;

import com.tapwithus.sdk.ListenerManager;
import com.tapwithus.sdk.NotifyAction;
import com.tapwithus.sdk.airmouse.AirMousePacket;
import com.tapwithus.sdk.haptic.HapticPacket;
import com.tapwithus.sdk.mouse.MousePacket;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings({"WeakerAccess"})
public class TapBluetoothManager {

    private static final String TAG = "TapBluetoothManager";

    private static final byte[] READ_TAP_STATE_DATA = new byte[] { 0xd };

    // the value '-15' given here is equivalent to byte value of 241 in ones' complement
    private static final byte[] REQUEST_SHIFT_SWITCH_STATE = new byte[] { -15, 0 };

    protected static final UUID TAP = UUID.fromString("C3FF0001-1D8B-40FD-A56F-C7BD5D0F3370");
    protected static final UUID DEVICE_INFORMATION = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    protected static final UUID BATTERY = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    protected static final UUID BATTERY_LEVEL = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB");
    protected static final UUID SERIAL_NAME_STRING = UUID.fromString("00002A25-0000-1000-8000-00805F9B34FB");
    protected static final UUID HARDWARE_REVISION_STRING = UUID.fromString("00002A27-0000-1000-8000-00805F9B34FB");
    protected static final UUID FIRMWARE_REVISION_STRING = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB");
    protected static final UUID SOFTWARE_REVISION_STRING = UUID.fromString("00002A28-0000-1000-8000-00805F9B34FB");
    protected static final UUID TAP_DATA = UUID.fromString("C3FF0005-1D8B-40FD-A56F-C7BD5D0F3370");
    protected static final UUID NUS = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    protected static final UUID RX = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    protected static final UUID TX = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    protected static final UUID NAME = UUID.fromString("C3FF0003-1D8B-40FD-A56F-C7BD5D0F3370");
    protected static final UUID MOUSE_DATA = UUID.fromString("C3FF0006-1D8B-40FD-A56F-C7BD5D0F3370");
    protected static final UUID AIR_MOUSE_DATA = UUID.fromString("C3FF000A-1D8B-40FD-A56F-C7BD5D0F3370");
    protected static final UUID HAPTIC = UUID.fromString("C3FF0009-1D8B-40FD-A56F-C7BD5D0F3370");
    protected static final UUID DATA_REQUEST = UUID.fromString("C3FF000B-1D8B-40FD-A56F-C7BD5D0F3370");

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

    public void ignoreTap(@NonNull String tapAddress) {
        bluetoothManager.ignoreDevice(tapAddress);
    }

    public void unignoreTap(@NonNull String tapAddress) {
        bluetoothManager.unignoreDevice(tapAddress);
    }

    public Set<String> getIgnoredTaps() {
        return bluetoothManager.getIgnoredDevices();
    }

    public boolean isTapIgnored(@NonNull String tapAddress) {
        return bluetoothManager.isDeviceIgnored(tapAddress);
    }



    @NonNull
    public Set<String> getConnectedTaps() {
        return bluetoothManager.getConnectedDevices();
    }

    public void refreshConnections() {
        bluetoothManager.refreshConnections();
    }

    public void startMode(@NonNull String tapAddress, byte[] data) {
        bluetoothManager.writeCharacteristic(tapAddress, NUS, RX, data);
    }

    public void sendHapticPacket(String tapAddress, int[] durations) {
        HapticPacket p = new HapticPacket(new byte[] { });
        p.vOn1.set(durations[0]);
        p.vOff1.set(durations[1]);
        p.vOn2.set(durations[2]);
        p.vOff2.set(durations[3]);
        p.vOn3.set(durations[4]);
        p.vOff3.set(durations[5]);
        p.vOn4.set(durations[6]);
        p.vOff4.set(durations[7]);
        p.vOn5.set(durations[8]);
        p.vOff5.set(durations[9]);
        p.vOn6.set(durations[10]);
        p.vOff6.set(durations[11]);
        p.vOn7.set(durations[12]);
        p.vOff7.set(durations[13]);
        p.vOn8.set(durations[14]);
        p.vOff8.set(durations[15]);
        p.vOn9.set(durations[16]);
        p.vOff9.set(durations[17]);
        log("Sending Haptic packet - " + Arrays.toString(p.getData()));
        bluetoothManager.writeCharacteristic(tapAddress, TAP, HAPTIC, p.getData());
    }

//    public void startControllerMode(@NonNull String tapAddress) {
//        bluetoothManager.writeCharacteristic(tapAddress, NUS, RX, CONTROLLER_MODE_DATA);
//    }
//
//    public void startTextMode(@NonNull String tapAddress) {
//        bluetoothManager.writeCharacteristic(tapAddress, NUS, RX, TEXT_MODE_DATA);
//    }
//
//    public void startControllerModeWithMouseHID(@NonNull String tapAddress) {
//        bluetoothManager.writeCharacteristic(tapAddress, NUS, RX, CONTROLLER_MODE_WITH_MOUSEHID_DATA);
//    }

    // Deprecated
//    public void readTapState(@NonNull String tapAddress) {
//        log("Reading tap state");
//        bluetoothManager.readCharacteristic(tapAddress, TAP, AIR_MOUSE_DATA);
//    }

    public void requestReadTapState(@NonNull String tapAddress) {
        log("request read tap state");
        bluetoothManager.writeCharacteristic(tapAddress, TAP, AIR_MOUSE_DATA, READ_TAP_STATE_DATA);
    }

    public void requestShiftSwitchState(@NonNull String tapAddress) {
        log("request shift/switch state");
        bluetoothManager.writeCharacteristic(tapAddress, TAP, DATA_REQUEST, REQUEST_SHIFT_SWITCH_STATE);
    }

    public void requestTap(@NonNull String tapAddress, byte combination) {
        log("request tap with " + combination);
        byte[] request_array = new byte[] { combination, 0 };
        bluetoothManager.writeCharacteristic(tapAddress, TAP, DATA_REQUEST, request_array);
    }

    public void readName(@NonNull String tapAddress) {
        log("Reading name");
        bluetoothManager.readCharacteristic(tapAddress, TAP, NAME);
    }

    public void writeName(@NonNull String tapAddress, @NonNull String name) {
        log("Writing name");
        bluetoothManager.writeCharacteristic(tapAddress, TAP, NAME, name.getBytes(StandardCharsets.UTF_8));
    }

    public void readBattery(@NonNull String tapAddress) {
        log("Reading battery");
        bluetoothManager.readCharacteristic(tapAddress, BATTERY, BATTERY_LEVEL);
    }

    public void readSerialNumber(@NonNull String tapAddress) {
        log("Reading serial number");
        bluetoothManager.readCharacteristic(tapAddress, DEVICE_INFORMATION, SERIAL_NAME_STRING);
    }

    public void readHwVer(@NonNull String tapAddress) {
        log("Reading hw ver");
        bluetoothManager.readCharacteristic(tapAddress, DEVICE_INFORMATION, HARDWARE_REVISION_STRING);
    }

    public void readFwVer(@NonNull String tapAddress) {
        log("Reading fw ver");
        bluetoothManager.readCharacteristic(tapAddress, DEVICE_INFORMATION, FIRMWARE_REVISION_STRING);
    }

    public void readBootloaderVer(@NonNull String tapAddress) {
        log("Reading bootloader ver");
        bluetoothManager.readCharacteristic(tapAddress, DEVICE_INFORMATION, SOFTWARE_REVISION_STRING);
    }

    public void setupTapNotification(@NonNull String tapAddress) {
        log("Setting up tap notifications");
        bluetoothManager.setupNotification(tapAddress, TAP, TAP_DATA);
    }

    public void setupMouseNotification(@NonNull String tapAddress) {
        log("Setting up mouse notifications");
        bluetoothManager.setupNotification(tapAddress, TAP, MOUSE_DATA);
    }

    public void setupAirMouseNotification(@NonNull String tapAddress) {
        log("Setting up air mouse notifications");
        bluetoothManager.setupNotification(tapAddress, TAP, AIR_MOUSE_DATA);

    }

    public void setupRawSensorNotification(@NonNull String tapAddress) {
        log("Settings up raw sensor notifications");
        bluetoothManager.setupNotification(tapAddress, NUS, TX);
    }

    public void setupDataNotification(@NonNull String tapAddress) {
        log("setting up data notifications");
        bluetoothManager.setupNotification(tapAddress, TAP, DATA_REQUEST);
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
                notifyOnNameRead(deviceAddress, new String(data, StandardCharsets.UTF_8));
            } else if (characteristic.equals(BATTERY_LEVEL)) {
                notifyOnBatteryRead(deviceAddress, data[0] & 0xFF);
            } else if (characteristic.equals(SERIAL_NAME_STRING)) {
                notifyOnSerialNumberRead(deviceAddress, new String(data));
            } else if (characteristic.equals(HARDWARE_REVISION_STRING)) {
                notifyOnHwVerRead(deviceAddress, new String(data));
            } else if (characteristic.equals(FIRMWARE_REVISION_STRING)) {
                notifyOnFwVerRead(deviceAddress, new String((data)));
            } else if (characteristic.equals(SOFTWARE_REVISION_STRING)) {
                notifyOnBootloaderVerRead(deviceAddress, new String((data)));
            } else if (characteristic.equals(AIR_MOUSE_DATA)) {
                onNotificationReceived(deviceAddress, characteristic, data);
            } else if (characteristic.equals(TX)) {
                onNotificationReceived(deviceAddress, characteristic, data);
            } else if (characteristic.equals(DATA_REQUEST)) {
                onNotificationReceived(deviceAddress, characteristic, data);
            }
        }

        @Override
        public void onCharacteristicNotFound(@NonNull String deviceAddress, @NonNull UUID characteristic) {
            log("Characteristic Not Found");
            if (characteristic.equals(NAME)) {
                notifyOnNameRead(deviceAddress, "Unavailable");
            } else if (characteristic.equals(BATTERY_LEVEL)) {
                notifyOnBatteryRead(deviceAddress, -2);
            } else if (characteristic.equals(SERIAL_NAME_STRING)) {
                notifyOnSerialNumberRead(deviceAddress, "Unavailable");
            } else if (characteristic.equals(HARDWARE_REVISION_STRING)) {
                notifyOnHwVerRead(deviceAddress, "Unavailable");
            } else if (characteristic.equals(FIRMWARE_REVISION_STRING)) {
                notifyOnFwVerRead(deviceAddress,"Unavailable");
            } else if (characteristic.equals(SOFTWARE_REVISION_STRING)) {
                notifyOnBootloaderVerRead(deviceAddress, "Unavailable");
            }
        }

        @Override
        public void onCharacteristicWrite(@NonNull String deviceAddress, @NonNull UUID characteristic, @NonNull byte[] data) {
//            if (characteristic.equals(RX)) {
//                if (Arrays.equals(data, CONTROLLER_MODE_DATA)) {
//                    log("Controller Mode Started: " + deviceAddress);
////                    notifyOnControllerModeStarted(deviceAddress);
//                } else if (Arrays.equals(data, TEXT_MODE_DATA)) {
//                    log("Text Mode Started: " + deviceAddress);
////                    notifyOnTextModeStarted(deviceAddress);
//                } else if (Arrays.equals(data, CONTROLLER_MODE_WITH_MOUSEHID_DATA)) {
//                    log("Controller with Mouse HID Mode Started: " + deviceAddress);
////                    notifyOnControllerWithMouseHIDModeStarted(deviceAddress);
//                }
//            } else
            if (characteristic.equals(NAME)) {
                log("Name Changed");
                notifyOnNameWrite(deviceAddress, new String(data, StandardCharsets.UTF_8));
            }
        }

        @Override
        public void onNotificationSubscribed(@NonNull String deviceAddress, @NonNull UUID characteristic) {
            if (characteristic.equals(TAP_DATA)) {
                log("Tap notification subscribed");
                notifyOnTapInputSubscribed(deviceAddress);
            } else if (characteristic.equals(MOUSE_DATA)) {
                log("Mouse notification subscribed");
                notifyOnMouseInputSubscribed(deviceAddress);
            } else if (characteristic.equals(AIR_MOUSE_DATA)) {
                log("Air mouse notification subscribed");
//                requestReadTapState(deviceAddress);
                notifyOnAirMouseDataSubscribed(deviceAddress);
            } else if (characteristic.equals(TX)) {
                log("Raw sensor notification subscribed");
                notifyOnRawSensorDataSubscribed(deviceAddress);
            } else if (characteristic.equals(DATA_REQUEST)) {
                log("DataRequest notification subscribed");
                notifyOnDataRequestSubscribed(deviceAddress);
            }
        }


        @Override
        public void onNotificationReceived(@NonNull String deviceAddress, @NonNull UUID characteristic, @NonNull byte[] data) {
//            log("Notification Received " + Arrays.toString(data));

            if (characteristic.equals(TAP_DATA)) {
                int byte3 = 0;
                if (data.length > 3) {
                    byte3 = data[3];
                }
                if (data[0] != 0) {
                    // we are now going to send byte [3] into the same function to decide single/double/triple
                    notifyOnTapInputReceived(deviceAddress, data[0], byte3);
                }
                notifyOnTapShiftSWitchReceived(deviceAddress, byte3);
            } else if (characteristic.equals(MOUSE_DATA)) {
                data = Arrays.copyOfRange(data, 1, data.length);
                MousePacket mousePacket = new MousePacket(data);
                notifyOnMouseInputReceived(deviceAddress, mousePacket);
            } else if (characteristic.equals(AIR_MOUSE_DATA)) {
                AirMousePacket airMousePacket = new AirMousePacket(data);
                if (airMousePacket.gesture.getInt() == 20) {
                    notifyOnTapChangedState(deviceAddress, airMousePacket.state.getInt());
                } else {
                    notifyOnAirMouseInputReceived(deviceAddress, airMousePacket);
                }
            } else if (characteristic.equals(TX)) {
                notifyOnRawSensorInputReceived(deviceAddress, data);
            } else if (characteristic.equals(DATA_REQUEST)) {
                // should deal with this like a character which has come in - nothing should come here
                // so if something comes in let's give an error
                logError("!!! Notification Received  on DATA REQUEST channel" + Arrays.toString(data));
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

    private void notifyOnBootloaderVerRead(@NonNull final String tapAddress, @NonNull final String bootloaderVer) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onBootloaderVerRead(tapAddress, bootloaderVer);
            }
        });
    }

//    private void notifyOnControllerModeStarted(@NonNull final String tapAddress) {
//        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
//            @Override
//            public void onNotify(TapBluetoothListener listener) {
//                listener.onControllerModeStarted(tapAddress);
//            }
//        });
//    }
//
//    private void notifyOnControllerWithMouseHIDModeStarted(@NonNull final String tapAddress) {
//        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
//            @Override
//            public void onNotify(TapBluetoothListener listener) {
//                listener.onControllerWithMouseHIDModeStarted(tapAddress);
//            }
//        });
//    }
//
//    private void notifyOnTextModeStarted(@NonNull final String tapAddress) {
//        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
//            @Override
//            public void onNotify(TapBluetoothListener listener) {
//                listener.onTextModeStarted(tapAddress);
//            }
//        });
//    }

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

    private void notifyOnAirMouseDataSubscribed(@NonNull final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onAirMouseInputSubscribed(tapAddress);
            }
        });
    }

    private void notifyOnDataRequestSubscribed(@NonNull final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onDataRequestSubscribed(tapAddress);
            }
        });
    }

    private void notifyOnRawSensorDataSubscribed(@NonNull final String tapAddress) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onRawSensorInputSubscribed(tapAddress);
            }
        });
    }

    private void notifyOnTapInputReceived(@NonNull final String tapAddress, final int data, final int repeatData) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onTapInputReceived(tapAddress, data, repeatData);
            }
        });
    }

    private void notifyOnTapShiftSWitchReceived(@NonNull final String tapAddress, final int data) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onTapShiftSwitchReceived(tapAddress, data);
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

    private void notifyOnAirMouseInputReceived(@NonNull final String tapAddress, @NonNull final AirMousePacket data) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onAirMouseInputReceived(tapAddress, data);
            }
        });
    }

    private void notifyOnRawSensorInputReceived(@NonNull final String tapAddress, @NonNull final byte[] data) {
        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onRawSensorDataReceieved(tapAddress, data);
            }
        });
    }

    private void notifyOnTapChangedState(@NonNull final String tapAddress, final int state) {

        tapBluetoothListeners.notifyAll(new NotifyAction<TapBluetoothListener>() {
            @Override
            public void onNotify(TapBluetoothListener listener) {
                listener.onTapChangedState(tapAddress, state);
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
