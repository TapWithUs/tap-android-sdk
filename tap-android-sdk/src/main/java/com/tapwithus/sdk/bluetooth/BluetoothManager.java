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
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.tapwithus.sdk.ListenerManager;
import com.tapwithus.sdk.NotifyAction;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BluetoothManager {

    private static final String TAG = "BluetoothManager";
    private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    protected static final UUID GENERIC_ATTRIBUTE = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    protected static final UUID SERVICE_CHANGED = UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb");
    private static final Map<String, BluetoothGatt> gatts = new ConcurrentHashMap<>();
    private static final List<String> establishConnectionSent = new CopyOnWriteArrayList<>();

    public static final int ERR_BLUETOOTH_OFF = 1;
    public static final int ERR_BLUETOOTH_NOT_SUPPORTED = 2;
    public static final int ERR_PAIRED_DEVICES = 3;
    public static final int ERR_DISCOVER_SERVICES = 4;
    public static final int ERR_DEVICE_NOT_CONNECTED = 5;
    public static final int ERR_GATT_READ = 6;
    public static final int ERR_GATT_WRITE = 7;
    public static final int ERR_SERVICE_NOT_FOUND = 8;
    public static final int ERR_CHARACTERISTIC_NOT_FOUND = 9;
    public static final int ERR_DESCRIPTOR_SET = 10;
    public static final int ERR_CHARACTERISTIC_SET = 11;
    public static final int ERR_CHARACTERISTIC_READ = 12;
    public static final int ERR_CHARACTERISTIC_WRITE = 13;
    public static final int ERR_NOTIFICATION_DATA = 14;
    public static final int ERR_TIMEOUT = 15;

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private ListenerManager<BluetoothListener> bluetoothListeners = new ListenerManager<>();
    private List<String> connectionInProgress = new CopyOnWriteArrayList<>();

    private boolean debug = false;
    private boolean bluetoothRestartRequested = false;
    private boolean restartBondRequested = false;

    private boolean serviceDiscoveryRequested = false;
    private int numOfServiceDiscoveryRequests = 0;
    private boolean pairRequested = false;
    private int numOfPairRequests = 0;
    private boolean unpairRequested = false;
    private int numOfUnpairRequests = 0;

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
            notifyOnError("", ERR_BLUETOOTH_OFF, "Bluetooth is turned OFF");
        }
    }

    public void unregisterBluetoothListener(@NonNull BluetoothListener listener) {
        bluetoothListeners.unregisterListener(listener);
    }

    public boolean isConnectionInProgress() {
        return connectionInProgress.size() > 0;
    }

    public boolean isConnectionInProgress(@NonNull String deviceAddress) {
        return connectionInProgress.contains(deviceAddress);
    }

    public void refreshConnections() {
        establishConnections();
    }

    public void close() {
        log("BluetoothManager is closing...");
        bluetoothListeners.removeAllListeners();
        disconnectDevices();
        unregisterBluetoothState();
    }

    private void registerBluetoothState() {
        IntentFilter i = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(broadcastReceiver, i);
    }

    private void unregisterBluetoothState() {
        try {
            context.unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException ignored) { }
    }

    private void establishConnections() {
        log("Establishing connections...");
        if (bluetoothAdapter == null) {
            notifyOnError("", ERR_BLUETOOTH_NOT_SUPPORTED, "Bluetooth is not supported on this hardware platform");
            return;
        }

        for (String deviceAddress: getConnectedDevices()) {
            notifyOnDeviceAlreadyConnected(deviceAddress);
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices == null) {
            notifyOnError("", ERR_PAIRED_DEVICES, "Unable to retrieve paired devices");
            return;
        }

        for (BluetoothDevice bondedDevice : bondedDevices) {
            if (!establishConnectionSent.contains(bondedDevice.getAddress())) {
                establishConnectionSent.add(bondedDevice.getAddress());
                establishConnection(bondedDevice);
            }
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

            if (status == 8) {
                notifyOnError(device.getAddress(), ERR_TIMEOUT, "Timeout");
                return;
            }

            if (status == 22) {
                log("Status " + status);
                handleDeviceDisconnection(gatt);
                return;
            }

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
            gatts.put(deviceAddress, gatt);

            serviceDiscoveryRequested = false;

            if (status != BluetoothGatt.GATT_SUCCESS) {
                notifyOnError(deviceAddress, ERR_DISCOVER_SERVICES, "Unable to discover services");
                return;
            }

            if (gatt.getServices().isEmpty()) {
                log(deviceAddress + " no services discovered");
            } else {
                log(deviceAddress + " services discovered");
                for (BluetoothGattService gattService: gatt.getServices()) {
                    log(gattService.getUuid().toString());
                }
            }

            connectionInProgress.remove(deviceAddress);
            notifyOnDeviceConnected(deviceAddress);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String deviceAddress = gatt.getDevice().getAddress();

            if (status != BluetoothGatt.GATT_SUCCESS) {
                notifyOnError(deviceAddress, ERR_GATT_READ, "Characteristic GATT Read Error - " + characteristic.getUuid().toString());
                return;
            }

            log("On Characteristic Read - " + characteristic.getUuid().toString());

            notifyOnCharacteristicRead(deviceAddress, characteristic.getUuid(), characteristic.getValue());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String deviceAddress = gatt.getDevice().getAddress();

            if (status != BluetoothGatt.GATT_SUCCESS) {
                notifyOnError(deviceAddress, ERR_GATT_WRITE, "Characteristic GATT Write Error - " + characteristic.getUuid().toString());
                return;
            }

            log("On Characteristic Write");

            notifyOnCharacteristicWrite(deviceAddress, characteristic.getUuid(), characteristic.getValue());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            log("On characteristic changed");
            String deviceAddress = gatt.getDevice().getAddress();
            byte[] data = characteristic.getValue();

            if (data == null) {
                notifyOnError(deviceAddress, ERR_NOTIFICATION_DATA, "Unable to read notification data");
                return;
            }

            notifyOnNotificationReceived(deviceAddress, characteristic.getUuid(), data);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            String deviceAddress = gatt.getDevice().getAddress();

            if (status != BluetoothGatt.GATT_SUCCESS) {
                notifyOnError(deviceAddress, ERR_GATT_WRITE, "Descriptor GATT Write Error - " + descriptor.getUuid().toString());
                return;
            }

            log("On Descriptor Write");

            if (descriptor.getUuid().equals(CCCD)) {
                notifyOnNotificationSubscribed(deviceAddress, descriptor.getCharacteristic().getUuid());
            }
        }
    };

    private void handleDeviceConnection(final BluetoothGatt gatt) {
        String deviceAddress = gatt.getDevice().getAddress();

        if (!connectionInProgress.contains(deviceAddress)) {
            connectionInProgress.add(deviceAddress);
        }

        notifyOnDeviceStartConnecting(deviceAddress);

        BluetoothGatt storedGatt = gatts.get(deviceAddress);

        // If there is an already stored gatt for the same device and it's not the same connection,
        // it means that a new gatt connection opened for the same device, so close the old one and
        // save the new one
        if (storedGatt != null && storedGatt != gatt) {
            closeGatt(storedGatt);
        }

        log(deviceAddress + " connected.");

        Boolean isRefreshSucceed = refreshGatt(gatt);
        if (!isRefreshSucceed) {
            logError("Gatt refresh failed " + deviceAddress);
        }

        log("Waiting 1600 ms for a possible Service Changed indication...");
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                boolean success;
                int numOfDiscoverRequests = 0;
                do {
                    numOfDiscoverRequests++;
                    success = gatt.discoverServices();
                    log("Discover services - " + success);
                } while (!success && numOfDiscoverRequests < 3);
            }

        }, 1600);
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
        removeFromLists(deviceAddress);

        log(deviceAddress + " disconnected.");

        notifyOnDeviceDisconnected(deviceAddress);

        // Trying to establish another connection, to make sure the device will auto reconnect when turned back on
        if (autoReconnect && !restartBondRequested) {
            log("Establishing autoReconnect connection");
            establishConnection(device);
        }
    }

    private void closeGatt(BluetoothGatt gatt) {
        gatt.disconnect();
        gatt.close();
    }

    private void removeFromLists(@NonNull String deviceAddress) {
        gatts.remove(deviceAddress);
        establishConnectionSent.remove(deviceAddress);
    }

    public void setupNotification(@NonNull String deviceAddress, @NonNull UUID serviceUUID, @NonNull UUID characteristicUUID) {
        BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            notifyOnError(deviceAddress, ERR_DEVICE_NOT_CONNECTED, "Device is not connected to set notification");
            return;
        }
        setupNotification(gatt, serviceUUID, characteristicUUID);
    }

    private void setupNotification(BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid) {
        String deviceAddress = gatt.getDevice().getAddress();

        BluetoothGattService service = gatt.getService(serviceUuid);
        if (service == null) {
            notifyOnError(deviceAddress, ERR_SERVICE_NOT_FOUND,
                    "Service not found for notification - " + serviceUuid.toString());
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        if (characteristic == null) {
            notifyOnError(deviceAddress, ERR_CHARACTERISTIC_NOT_FOUND,
                    "Characteristic not found for notification - " + characteristicUuid.toString());
            return;
        }

        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            notifyOnError(deviceAddress, ERR_CHARACTERISTIC_SET,
                    "Failed to set characteristic notification - " + characteristicUuid.toString());
            return;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
        if (descriptor == null) {
            notifyOnError(deviceAddress, ERR_DESCRIPTOR_SET,
                    "Failed to get notification descriptor - " + characteristicUuid.toString());
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
            notifyOnError(deviceAddress, ERR_DESCRIPTOR_SET,
                    "Failed to set notification type - " + characteristicUuid.toString());
            return;
        }

        log("Notification Description: " + Arrays.toString(valueToSend));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!gatt.writeDescriptor(descriptor)) {
            notifyOnError(deviceAddress, ERR_DESCRIPTOR_SET,
                    "Failed to write characteristic descriptor - " + characteristicUuid.toString());
        }
    }

    public void readCharacteristic(@NonNull String deviceAddress, @NonNull UUID serviceUUID, @NonNull UUID characteristicUUID) {
        BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            notifyOnError(deviceAddress, ERR_DEVICE_NOT_CONNECTED, "Device is not connected to read characteristic");
            return;
        }
        readCharacteristic(gatt, serviceUUID, characteristicUUID);
    }

    private void readCharacteristic(BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid) {
        String deviceAddress = gatt.getDevice().getAddress();

        BluetoothGattService service = gatt.getService(serviceUuid);
        if (service == null) {
            notifyOnError(deviceAddress, ERR_SERVICE_NOT_FOUND,
                    "Service not found for reading - " + serviceUuid.toString());
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        if (characteristic == null) {
            notifyOnError(deviceAddress, ERR_CHARACTERISTIC_NOT_FOUND,
                    "Characteristic not found for reading - " + characteristicUuid.toString());
            return;
        }

        if (!gatt.readCharacteristic(characteristic)) {
            notifyOnError(deviceAddress, ERR_CHARACTERISTIC_READ,
                    "Failed to read characteristic - " + characteristicUuid.toString());
        }
    }

    public void writeCharacteristic(@NonNull String deviceAddress, @NonNull UUID serviceUUID, @NonNull UUID characteristicUUID, @NonNull byte[] data) {
        BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            notifyOnError(deviceAddress, ERR_DEVICE_NOT_CONNECTED, "Device is not connected to write characteristic");
            return;
        }
        writeCharacteristic(gatt, serviceUUID, characteristicUUID, data);
    }

    private void writeCharacteristic(BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid, byte[] data) {
        String deviceAddress = gatt.getDevice().getAddress();

        BluetoothGattService service = gatt.getService(serviceUuid);
        if (service == null) {
            notifyOnError(deviceAddress, ERR_SERVICE_NOT_FOUND,
                    "Service not found for writing - " + serviceUuid.toString());
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        if (characteristic == null) {
            notifyOnError(deviceAddress, ERR_CHARACTERISTIC_NOT_FOUND,
                    "Characteristic not found for writing - " + characteristicUuid.toString());
            return;
        }

        if (!characteristic.setValue(data)) {
            notifyOnError(deviceAddress, ERR_CHARACTERISTIC_SET,
                    "Failed to set characteristic's value - " + characteristicUuid.toString());
            return;
        }

        if (!gatt.writeCharacteristic(characteristic)) {
            notifyOnError(deviceAddress, ERR_CHARACTERISTIC_WRITE,
                    "Failed to write characteristic - " + characteristicUuid.toString());
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
    public Set<String> getConnectedDevices() {
        return new HashSet<>(gatts.keySet());
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

                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_ON:
                            log("Bluetooth turned ON");
                            establishConnections();
                            notifyOnBluetoothTurnedOn();
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            log("Bluetooth turned OFF");
                            notifyOnBluetoothTurnedOff();
                            establishConnectionSent.clear();
                            disconnectDevices();
                            if (bluetoothRestartRequested) {
                                bluetoothRestartRequested = false;
                                setBluetooth(true);
                            }
                            break;
                    }
                }
            }

            Boolean intentActionIsBondStateChanged = false;
            try {
                intentActionIsBondStateChanged = intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            } catch (NullPointerException ignored) { }

            if (intentActionIsBondStateChanged) {

                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) {
                    logError("Bond state changed - Unable to retrieve bluetooth device");
                    return;
                }

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    pairRequested = false;
                    log("Paired - " + device.toString());
                    establishConnection(device);
                }

                if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    unpairRequested = false;
                    log("Unpaired - " + device.toString());

                    removeFromLists(device.getAddress());

                    if (restartBondRequested) {
                        log("restartBondRequested");
                        restartBondRequested = false;
                        startPairProcess(device);
                    }
                }

            }
        }
    };

    private void notifyOnBluetoothTurnedOn() {
        bluetoothListeners.notifyAll(new NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onBluetoothTurnedOn();
            }
        });
    }

    private void notifyOnBluetoothTurnedOff() {
        bluetoothListeners.notifyAll(new NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onBluetoothTurnedOff();
            }
        });
    }

    private void notifyOnDeviceStartConnecting(@NonNull final String deviceAddress) {
        bluetoothListeners.notifyAll(new NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onDeviceStartConnecting(deviceAddress);
            }
        });
    }

    private void notifyOnDeviceConnected(@NonNull final String deviceAddress) {
        bluetoothListeners.notifyAll(new NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onDeviceConnected(deviceAddress);
            }
        });
    }

    private void notifyOnDeviceAlreadyConnected(@NonNull final String deviceAddress) {
        bluetoothListeners.notifyAll(new NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onDeviceAlreadyConnected(deviceAddress);
            }
        });
    }

    private void notifyOnDeviceDisconnected(@NonNull final String deviceAddress) {
        bluetoothListeners.notifyAll(new NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onDeviceDisconnected(deviceAddress);
            }
        });
    }

    private void notifyOnCharacteristicRead(@NonNull final String deviceAddress, @NonNull final UUID characteristic, @NonNull final byte[] data) {
        bluetoothListeners.notifyAll(new NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onCharacteristicRead(deviceAddress, characteristic, data);
            }
        });
    }

    private void notifyOnCharacteristicWrite(@NonNull final String deviceAddress, @NonNull final UUID characteristic, @NonNull final byte[] data) {
        bluetoothListeners.notifyAll(new NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onCharacteristicWrite(deviceAddress, characteristic, data);
            }
        });
    }

    private void notifyOnNotificationSubscribed(@NonNull final String deviceAddress, @NonNull final UUID characteristic) {
        bluetoothListeners.notifyAll(new NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onNotificationSubscribed(deviceAddress, characteristic);
            }
        });
    }

    private void notifyOnNotificationReceived(@NonNull final String deviceAddress, @NonNull final UUID characteristic, @NonNull final byte[] data) {
        bluetoothListeners.notifyAll(new NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onNotificationReceived(deviceAddress, characteristic, data);
            }
        });
    }

    private void notifyOnError(@NonNull final String deviceAddress, final int code, @NonNull final String description) {
        connectionInProgress.clear();
        bluetoothListeners.notifyAll(new NotifyAction<BluetoothListener>() {
            @Override
            public void onNotify(BluetoothListener listener) {
                listener.onError(deviceAddress, code, description);
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

    public void refreshBond(String deviceAddress) {
        if (!restartBondRequested) {
            restartBondRequested = true;

            BluetoothGatt gatt = gatts.get(deviceAddress);
            if (gatt != null) {
                BluetoothDevice device = gatt.getDevice();
                startUnpairProcess(device);
            }
        }
    }

    private void startDiscoverServicesProcess(final BluetoothGatt gatt) {
        log("Starting discover services process");

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                log("Service discovery timeout");
                if (serviceDiscoveryRequested) {
                    numOfServiceDiscoveryRequests++;
                    if (numOfServiceDiscoveryRequests > 3) {
                        serviceDiscoveryRequested = false;
                        numOfServiceDiscoveryRequests = 0;
                        notifyOnError(gatt.getDevice().getAddress(), ERR_TIMEOUT, "Service Discovery Timeout");
                        return;
                    }
                    startDiscoverServicesProcess(gatt);
                }
            }
        }, 5000);

        discoverServices(gatt);
    }

    private void discoverServices(final BluetoothGatt gatt) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                boolean success;
                int numOfDiscoverRequests = 0;
                do {
                    numOfDiscoverRequests++;
                    serviceDiscoveryRequested = true;
                    success = gatt.discoverServices();
                    log("Discover services - " + success);
                } while (!success && numOfDiscoverRequests < 3);
            }
        });
    }

    private void startPairProcess(final BluetoothDevice device) {
        log("Starting pair process");

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                log("Pairing timeout");
                if (pairRequested) {
                    numOfPairRequests++;
                    if (numOfPairRequests > 3) {
                        pairRequested = false;
                        numOfPairRequests = 0;
                        notifyOnError(device.getAddress(), ERR_TIMEOUT, "Pairing Timeout");
                        return;
                    }
                    startPairProcess(device);
                }
            }
        }, 5000);

        createBond(device);
    }

    private void createBond(final BluetoothDevice device) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                log("Pairing device - " + device.getAddress());
                Method localMethod;
                try {
                    localMethod = device.getClass().getMethod("createBond");
                    if (localMethod != null) {

                        boolean success;
                        int numOfRequests = 0;
                        do {
                            numOfRequests++;
                            pairRequested = true;
                            success = (boolean) localMethod.invoke(device);
                            log("Create bond - " + success);
                        } while (!success && numOfRequests < 3);

                    }
                } catch (Exception e) {
                    String message = e.getMessage() == null ? "Unknown error" : e.getMessage();
                    logError("An exception occurred while creating bond. " + message);
                }

            }
        });
    }

    private void startUnpairProcess(final BluetoothDevice device) {
        log("Starting unpair process");

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                log("Unpairing timeout");
                if (unpairRequested) {
                    numOfUnpairRequests++;
                    if (numOfUnpairRequests > 3) {
                        unpairRequested = false;
                        numOfUnpairRequests = 0;
                        notifyOnError(device.getAddress(), ERR_TIMEOUT, "Unpairing Timeout");
                        return;
                    }
                    startUnpairProcess(device);
                }
            }
        }, 5000);

        removeBond(device);
    }

    private void removeBond(final BluetoothDevice device) {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                log("Unpairing device - " + device.getAddress());
                Method localMethod;
                try {
                    localMethod = device.getClass().getMethod("removeBond");
                    if (localMethod != null) {

                        boolean success;
                        int numOfDiscoverRequests = 0;
                        do {
                            numOfDiscoverRequests++;
                            unpairRequested = true;
                            success = (boolean) localMethod.invoke(device);
                            log("Remove bond - " + success);
                        } while (!success && numOfDiscoverRequests < 3);

                    }
                } catch (Exception e) {
                    logError(e.toString());
                    String message = e.getMessage() == null ? "Unknown error" : e.getMessage();
                    logError("An exception occurred while removing bond. " + message);
                }

            }
        });
    }

    private void subscribeServiceChange(BluetoothGatt gatt) {
        log("Subscribing to service change...");
        setupNotification(gatt, GENERIC_ATTRIBUTE, SERVICE_CHANGED);
    }

    public void restartBluetooth() {
        if (!bluetoothRestartRequested) {
            bluetoothRestartRequested = true;
            setBluetooth(false);
        }
    }

    public boolean setBluetooth(boolean enable) {
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (enable && !isEnabled) {
            return bluetoothAdapter.enable();
        }
        else if(!enable && isEnabled) {
            return bluetoothAdapter.disable();
        }
        // No need to change bluetooth state
        return true;
    }

}
