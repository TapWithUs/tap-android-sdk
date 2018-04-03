package com.tapwithus.sdk.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.util.Log;

import com.tapwithus.sdk.ListenerManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BluetoothManager {

    private static final String TAG = "BluetoothManager";
    private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final Map<String, BluetoothGatt> gatts = new ConcurrentHashMap<>();

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private ListenerManager<BluetoothListener> bluetoothListeners = new ListenerManager<>();

    private boolean debug = false;

    public BluetoothManager(Context context, BluetoothAdapter bluetoothAdapter) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        registerBluetoothState();
    }

    public void enableDebug() {
        debug = true;
    }

    public void disableDebug() {
        debug = false;
    }

    public void registerBluetoothListener(@NonNull BluetoothListener listener) {
        bluetoothListeners.registerListener(listener);
        if (bluetoothAdapter != null && bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            listener.onBluetoothTurnedOn();
            establishConnections();
        } else {
            logError("Bluetooth is turned OFF");
        }
    }

    public void unregisterBluetoothListener(@NonNull BluetoothListener listener) {
        bluetoothListeners.unregisterListener(listener);
    }

    public void refreshConnections() {
        establishConnections();
    }

    public void close() {
        log("BluetoothManager closing...");
        bluetoothListeners.removeAllListeners();
        disconnectDevices();
        unregisterBluetoothState();
    }

    private void registerBluetoothState() {
        IntentFilter i = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(broadcastReceiver, i);
    }

    private void unregisterBluetoothState() {
        try {
            context.unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException ignored) { }
    }

    private void establishConnections() {
        if (bluetoothAdapter == null) {
            logError("Bluetooth is not supported on this hardware platform");
            return;
        }

        for (String deviceAddress: getConnectedDevices()) {
            notifyOnDeviceAlreadyConnected(deviceAddress);
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices == null) {
            logError("Unable to retrieve paired devices");
            return;
        }

        for (BluetoothDevice bondedDevice : bondedDevices) {
            establishConnection(bondedDevice);
        }
    }

    private void establishConnection(BluetoothDevice device) {
        if (isGattConnectionExists(device.getAddress())) {
            return;
        }

        device.connectGatt(context, true, bluetoothGattCallback);
    }

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice device = gatt.getDevice();
            log(device.getName() + " " + device.getAddress() + " status: " + status + " newState: " + newState);

            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothAdapter.STATE_DISCONNECTED) {
                device.connectGatt(context, false, this);
                return;
            }

            switch (newState) {
                case BluetoothAdapter.STATE_CONNECTED:
                    handleDeviceConnection(gatt);
                    break;
                case BluetoothAdapter.STATE_DISCONNECTED:
                    handleDeviceDisconnectionWithAutoReconnect(gatt);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            String deviceAddress = gatt.getDevice().getAddress();

            if (status != BluetoothGatt.GATT_SUCCESS) {
                logError(deviceAddress + " Unable to discover services");
                return;
            }

            log(deviceAddress + " services discovered");
//            for (BluetoothGattService gattService: gatt.getServices()) {
//                log(gattService.getUuid().toString());
//            }

            notifyOnDeviceConnected(deviceAddress);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status != BluetoothGatt.GATT_SUCCESS) {
                logError("On Characteristic Read Error");
                return;
            }

            log("On Characteristic Read");

            notifyOnCharacteristicRead(gatt.getDevice().getAddress(), characteristic.getUuid(), characteristic.getValue());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logError("On Characteristic Write Error");
                return;
            }

            log("On Characteristic Write");

            notifyOnCharacteristicWrite(gatt.getDevice().getAddress(), characteristic.getUuid(), characteristic.getValue());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            log("On characteristic changed");
            String deviceAddress = gatt.getDevice().getAddress();
            byte[] data = characteristic.getValue();
            if (data != null && data[0] != 0) {
                notifyOnNotificationReceived(deviceAddress, characteristic.getUuid(), data[0]);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            log("On Descriptor Write");
            if (descriptor.getUuid().equals(CCCD)) {
                notifyOnNotificationSubscribed(gatt.getDevice().getAddress(), descriptor.getCharacteristic().getUuid());
            }
        }
    };

    private void handleDeviceConnection(BluetoothGatt gatt) {
        String deviceAddress = gatt.getDevice().getAddress();

        BluetoothGatt storedGatt = gatts.get(deviceAddress);

        // If there is an already stored gatt for the same device and it's not the same connection,
        // it means that a new gatt connection opened for the same device, so close the old one and
        // save the new one
        if (storedGatt != null && storedGatt != gatt) {
            closeGatt(storedGatt);
        }

        gatts.put(deviceAddress, gatt);

        log(deviceAddress + " connected.");

        Boolean isRefreshSucceed = refreshGatt(gatt);
        if (!isRefreshSucceed) {
            logError("Gatt refresh failed to " + deviceAddress);
        }
        gatt.discoverServices();
    }

    private void handleDeviceDisconnection(BluetoothGatt gatt) {
        handleDeviceDisconnectionAndReconnection(gatt, false);
    }

    private void handleDeviceDisconnectionWithAutoReconnect(BluetoothGatt gatt) {
        handleDeviceDisconnectionAndReconnection(gatt, true);
    }

    private void handleDeviceDisconnectionAndReconnection(BluetoothGatt gatt, boolean autoReconnect) {
        BluetoothDevice device = gatt.getDevice();
        String deviceAddress = device.getAddress();

        BluetoothGatt storedGatt = gatts.get(deviceAddress);
        if (storedGatt == null || storedGatt != gatt) {
            closeGatt(gatt);
            return;
        }

        closeGatt(gatt);
        gatts.remove(deviceAddress);

        log(deviceAddress + " disconnected.");

        notifyOnDeviceDisconnected(deviceAddress);

        // Trying to establish another connection, to make sure the device will auto reconnect when turned back on
        if (autoReconnect) {
            establishConnection(device);
        }
    }

    private void closeGatt(BluetoothGatt gatt) {
        gatt.disconnect();
        gatt.close();
    }

    public void setupNotification(@NonNull String deviceAddress, @NonNull UUID serviceUUID, @NonNull UUID characteristicUUID) {
        BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            logError("Setup notification Error. " + deviceAddress + " is not connected");
            return;
        }
        setupNotification(gatt, serviceUUID, characteristicUUID);
    }

    private void setupNotification(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID) {
        BluetoothGattService service = gatt.getService(serviceUUID);
        if (service == null) {
            logError("Error. Service not found for notification");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
        if (characteristic == null) {
            logError("Error. Characteristic not found for notification");
            return;
        }

        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            logError("Error. Failed to set characteristic notification");
            return;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
        if (descriptor == null) {
            logError("Error. Failed to get notification descriptor");
            return;
        }

        int characteristicProperties = characteristic.getProperties();
        byte[] valueToSend = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        if ((characteristicProperties & 0x10) == 16) {
            valueToSend = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        } else if ((characteristicProperties & 0x20) == 32) {
            valueToSend = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
        }
        if (valueToSend.length == 0) {
            logError("Error. Failed to set notification type");
            return;
        }

        log("Notification Description: " + Arrays.toString(valueToSend));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!gatt.writeDescriptor(descriptor)) {
            logError("Error. Failed to write characteristic descriptor");
        }
    }

    public void readCharacteristic(@NonNull String deviceAddress, @NonNull UUID serviceUUID, @NonNull UUID characteristicUUID) {
        BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            logError("Read Characteristic Error. $deviceAddress is not connected");
            return;
        }
        readCharacteristic(gatt, serviceUUID, characteristicUUID);
    }

    private void readCharacteristic(BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid) {
        BluetoothGattService service = gatt.getService(serviceUuid);
        if (service == null) {
            logError("Read Characteristic Error. Service not found for writing.");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        if (characteristic == null) {
            logError("Read Characteristic Error. Characteristic not found for writing");
            return;
        }

        if (!gatt.readCharacteristic(characteristic)) {
            logError("Error. Failed to read characteristic");
        }
    }

    public void writeCharacteristic(@NonNull String deviceAddress, @NonNull UUID serviceUUID, @NonNull UUID characteristicUUID, @NonNull byte[] data) {
        BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            logError("Write Characteristic Error. " + deviceAddress + " is not connected");
            return;
        }
        writeCharacteristic(gatt, serviceUUID, characteristicUUID, data);
    }

    private void writeCharacteristic(BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid, byte[] data) {
        BluetoothGattService service = gatt.getService(serviceUuid);
        if (service == null) {
            logError("Write Characteristic Error. Service not found for writing.");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        if (characteristic == null) {
            logError("Write Characteristic Error. Characteristic not found for writing");
            return;
        }

        if (!characteristic.setValue(data)) {
            logError("Error. Failed to set Characteristic's value");
            return;
        }

        if (!gatt.writeCharacteristic(characteristic)) {
            logError("Error. Failed to write Characteristic");
        }
    }

    private void disconnectDevices() {
        for (String s : gatts.keySet()) {
            disconnectDevice(s);
        }
    }

    private void disconnectDevice(String deviceAddress) {
        BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            return;
        }
        handleDeviceDisconnection(gatt);
    }

    @NonNull
    public ArrayList<String> getConnectedDevices() {
        return new ArrayList<>(gatts.keySet());
    }

    private Boolean isGattConnectionExists(String deviceAddress) {
        return gatts.containsKey(deviceAddress);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("SwitchIntDef")
        @Override
        public void onReceive(Context context, Intent intent) {

            Boolean intentActionIsStateChanged = false;
            try {
                intentActionIsStateChanged = intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED);
            } catch (NullPointerException ignored) { }

            if (intentActionIsStateChanged) {
                if (bluetoothAdapter != null) {

                    switch (bluetoothAdapter.getState()) {
                        case BluetoothAdapter.STATE_ON:
                            log("Bluetooth turned ON");
                            notifyOnBluetoothTurnedOn();
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            log("Bluetooth turned OFF");
                            notifyOnBluetoothTurnedOff();
                            disconnectDevices();
                            break;
                    }
                }
            }
        }
    };

    private void notifyOnBluetoothTurnedOn() {
        bluetoothListeners.notifyListeners(new ListenerManager.NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onBluetoothTurnedOn();
            }
        });
    }

    private void notifyOnBluetoothTurnedOff() {
        bluetoothListeners.notifyListeners(new ListenerManager.NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onBluetoothTurnedOff();
            }
        });
    }

    private void notifyOnDeviceConnected(final String deviceAddress) {
        bluetoothListeners.notifyListeners(new ListenerManager.NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onDeviceConnected(deviceAddress);
            }
        });
    }

    private void notifyOnDeviceAlreadyConnected(final String deviceAddress) {
        bluetoothListeners.notifyListeners(new ListenerManager.NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onDeviceAlreadyConnected(deviceAddress);
            }
        });
    }

    private void notifyOnDeviceDisconnected(final String deviceAddress) {
        bluetoothListeners.notifyListeners(new ListenerManager.NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onDeviceDisconnected(deviceAddress);
            }
        });
    }

    private void notifyOnCharacteristicRead(final String deviceAddress, final UUID characteristic, final byte[] data) {
        bluetoothListeners.notifyListeners(new ListenerManager.NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onCharacteristicRead(deviceAddress, characteristic, data);
            }
        });
    }

    private void notifyOnCharacteristicWrite(final String deviceAddress, final UUID characteristic, final byte[] data) {
        bluetoothListeners.notifyListeners(new ListenerManager.NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onCharacteristicWrite(deviceAddress, characteristic, data);
            }
        });
    }

    private void notifyOnNotificationSubscribed(final String deviceAddress, final UUID characteristic) {
        bluetoothListeners.notifyListeners(new ListenerManager.NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onNotificationSubscribed(deviceAddress, characteristic);
            }
        });
    }

    private void notifyOnNotificationReceived(final String deviceAddress, final UUID characteristic, final int data) {
        bluetoothListeners.notifyListeners(new ListenerManager.NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onNotificationReceived(deviceAddress, characteristic, data);
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

    private Boolean refreshGatt(BluetoothGatt gatt) {
        Method localMethod;
        try {
            localMethod = gatt.getClass().getMethod("refresh");
            if (localMethod != null) {
                return (Boolean) localMethod.invoke(gatt);
            }
        } catch (Exception e) {
            String message = e.getMessage() == null ? "Unknown error" : e.getMessage();
            logError("An exception occurred while refreshing device. " + message);
        }

        return false;
    }
}
