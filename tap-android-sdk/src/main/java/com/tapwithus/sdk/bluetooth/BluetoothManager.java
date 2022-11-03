package com.tapwithus.sdk.bluetooth;

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
import androidx.annotation.NonNull;
import android.util.Log;

import com.tapwithus.sdk.ListenerManager;
import com.tapwithus.sdk.NotifyAction;
import com.tapwithus.sdk.bluetooth.callbacks.OnCompletionListener;
import com.tapwithus.sdk.bluetooth.callbacks.OnErrorListener;
import com.tapwithus.sdk.bluetooth.callbacks.OnNotFoundListener;
import com.tapwithus.sdk.bluetooth.operations.CharacteristicReadOperation;
import com.tapwithus.sdk.bluetooth.operations.CharacteristicWriteOperation;
import com.tapwithus.sdk.bluetooth.operations.DiscoverServicesOperation;
import com.tapwithus.sdk.bluetooth.operations.GattExecutor;
import com.tapwithus.sdk.bluetooth.operations.GattOperation;
import com.tapwithus.sdk.bluetooth.operations.RefreshOperation;
import com.tapwithus.sdk.bluetooth.operations.SetNotificationOperation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

@SuppressWarnings({"WeakerAccess", "unused"})
public class BluetoothManager {

    private static final String TAG = "BluetoothManager";

    public static final String EMPTY_DEVICE_ADDRESS = "";

    private static final Map<String, BluetoothGatt> gatts = new ConcurrentHashMap<>();
    private static final Map<String, GattExecutor> executors = new ConcurrentHashMap<>();
    private static final List<String> establishConnectionSent = new CopyOnWriteArrayList<>();
    private final List<String> connectionInProgress = new CopyOnWriteArrayList<>();
    private static final Set<String> connectedDevices = new CopyOnWriteArraySet<>();
    private static final Set<String> ignoredDevices = new CopyOnWriteArraySet<>();
    private static final Set<String> ignoreInProgress = new CopyOnWriteArraySet<>();

    public static final int ERR_C_BLUETOOTH_OFF = 1;
    public static final int ERR_C_BLUETOOTH_NOT_SUPPORTED = 2;
    public static final int ERR_C_PAIRED_DEVICES = 3;
    public static final int ERR_C_DEVICE_NOT_CONNECTED = 4;
    public static final int ERR_C_GATT_OP = 5;

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final ListenerManager<BluetoothListener> bluetoothListeners = new ListenerManager<>();

    private boolean debug = false;
    private boolean restartBondRequested = false;
    private boolean isClosing = false;
    private boolean isClosed = false;
    private boolean isBluetoothTurnedOff = false;

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

    public boolean isClosing() {
        return isClosing;
    }

    public void registerBluetoothListener(@NonNull BluetoothListener listener) {
        isClosed = false;

        bluetoothListeners.registerListener(listener);
        if (bluetoothAdapter != null && bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            listener.onBluetoothTurnedOn();
            establishConnections();
        } else {
            notifyOnError(EMPTY_DEVICE_ADDRESS, ERR_C_BLUETOOTH_OFF, ErrorStrings.BLUETOOTH_OFF);
        }
    }

    public void unregisterBluetoothListener(@NonNull BluetoothListener listener) {
        bluetoothListeners.unregisterListener(listener);
    }

    public void ignoreDevice(@NonNull String deviceAddress) {
        ignoreInProgress.add(deviceAddress);
        disconnectDevice(deviceAddress);
    }

    public void unignoreDevice(@NonNull String deviceAddress) {
        ignoredDevices.remove(deviceAddress);
        establishConnection(deviceAddress);
    }

    public Set<String> getIgnoredDevices() {
        return ignoredDevices;
    }

    public boolean isDeviceIgnored(@NonNull String deviceAddress) {
        return ignoreInProgress.contains(deviceAddress) || ignoredDevices.contains(deviceAddress);
    }

    public boolean isConnectionInProgress() {
        return connectionInProgress.size() > 0;
    }

    public boolean isConnectionInProgress(@NonNull String deviceAddress) {
        return connectionInProgress.contains(deviceAddress);
    }

    public void refreshConnections() {
        isClosed = false;
        establishConnectionSent.clear();
        gatts.clear();
        establishConnections();
    }

    public void close() {
        log("BluetoothManager is closing...");

        isClosing = true;

        if (numOfConnectedDevices() != 0) {
            disconnectAllDevices();
        } else {
            handleCloseAfterAllDevicesDisconnected();
        }
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
            notifyOnError(EMPTY_DEVICE_ADDRESS, ERR_C_BLUETOOTH_NOT_SUPPORTED, ErrorStrings.BLUETOOTH_NOT_SUPPORTED);
            return;
        }

        for (String deviceAddress: getConnectedDevices()) {
            notifyOnDeviceAlreadyConnected(deviceAddress);
        }

        Set<BluetoothDevice> bondedDevices = null;
        try {
            bondedDevices = bluetoothAdapter.getBondedDevices();
        } catch (SecurityException se) {
            logError("Security Exception - no permission (Android 12) - " + se.getMessage());
        }

        if (bondedDevices == null) {
            notifyOnError(EMPTY_DEVICE_ADDRESS, ERR_C_PAIRED_DEVICES, ErrorStrings.PAIRED_DEVICES);
            return;
        }
        for (BluetoothDevice bondedDevice : bondedDevices) {
            String deviceAddress = bondedDevice.getAddress();
//            log("We have a bonded device: " + deviceAddress);
            if (!ignoredDevices.contains(deviceAddress) && !establishConnectionSent.contains(deviceAddress)) {
                establishConnectionSent.add(deviceAddress);
//                log("Attempting connection to it!");
                establishConnection(bondedDevice);
            }
//            else {
//                log("But we will NOT bother connecting!!");
//                if (ignoredDevices.contains(deviceAddress)) {
//                    log("Because it is in the ignored devices");
//                }
//                if(establishConnectionSent.contains(deviceAddress)) {
//                    log("Because it is already in the establishConnections list");
//                }
//            }
        }
    }

    private void establishConnection(BluetoothDevice device) {
        if (ignoredDevices.contains(device.getAddress())) {
            return;
        }

        // check if gatt connection already exists
        if (gatts.containsKey(device.getAddress())) {
//            log("Not connecting because we have it in gatts already");
            return;
        }

        log("Trying to connect to gatt");
        try {
            device.connectGatt(context, true, bluetoothGattCallback);
        } catch (SecurityException se) {
            log("No permission granted (Android 12)");
        }
    }

    private void establishConnection(String deviceAddress) {

        log("Establishing connection with " + deviceAddress);

        if (bluetoothAdapter == null) {
            notifyOnError(EMPTY_DEVICE_ADDRESS, ERR_C_BLUETOOTH_NOT_SUPPORTED, ErrorStrings.BLUETOOTH_NOT_SUPPORTED);
            return;
        }

        for (String connectedDeviceAddress: getConnectedDevices()) {
            notifyOnDeviceAlreadyConnected(connectedDeviceAddress);
        }

        Set<BluetoothDevice> bondedDevices = null;
        try {
            bondedDevices = bluetoothAdapter.getBondedDevices();
        } catch (SecurityException se) {
            log("No permission granted for getBondedDevices (Android 12)");
            notifyOnError(EMPTY_DEVICE_ADDRESS, ERR_C_PAIRED_DEVICES, ErrorStrings.PAIRED_DEVICES);
            return;
        }
        if (bondedDevices == null) {
            notifyOnError(EMPTY_DEVICE_ADDRESS, ERR_C_PAIRED_DEVICES, ErrorStrings.PAIRED_DEVICES);
            return;
        }

        for (BluetoothDevice bondedDevice : bondedDevices) {
            String bondedDeviceAddress = bondedDevice.getAddress();
            log(bondedDeviceAddress + " " + deviceAddress);
            if (bondedDeviceAddress.equals(deviceAddress)) {
                if (!establishConnectionSent.contains(bondedDeviceAddress)) {
                    establishConnectionSent.add(bondedDeviceAddress);
                    establishConnection(bondedDevice);
                }
            }
        }
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            /*
             * status -
             *
             * 8  (0x08) - connection timeout / device went out of range
             * 19 (0x13) - connection terminated by peer user
             * 22 (0x16) - connection terminated by local host
             *
             * For more information, please refer to:
             * https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/refs/tags/android-cts-5.1_r28/stack/include/gatt_api.h
             */

            BluetoothDevice device = gatt.getDevice();
            try {
                log("Connection state changed - " + device.getName() + " " + device.getAddress() + " status: " + status + " newState: " + newState);
            } catch (SecurityException se) {
                log("Connection state changed - No permission granted for device.getName() (Android 12)");
            }

            if (ignoredDevices.contains(device.getAddress())) {
                return;
            }

            if (status == 22) {
                // will get here after calling 'removeBond'
                handleDeviceUnpaired(gatt);
                return;
            }

            switch (newState) {
                case BluetoothAdapter.STATE_CONNECTED:
                    handleDeviceConnection(gatt);
                    break;
                case BluetoothAdapter.STATE_DISCONNECTED:
                    handleDeviceDisconnection(gatt);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            log("Services discovered - " + status);
            GattExecutor executor = getExecutor(gatt);
            if (executor != null) {
                executor.onServicesDiscovered(status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            GattExecutor executor = getExecutor(gatt);
            if (executor != null) {
                executor.onCharRead(characteristic, status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            GattExecutor executor = getExecutor(gatt);
            if (executor != null) {
                executor.onCharWrite(characteristic, status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (ignoredDevices.contains(gatt.getDevice().getAddress())) {
                return;
            }

            GattExecutor executor = getExecutor(gatt);
            if (executor != null) {
                executor.onCharChange(characteristic);
            }

            notifyOnNotificationReceived(gatt.getDevice().getAddress(),
                    characteristic.getUuid(), characteristic.getValue());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            GattExecutor executor = getExecutor(gatt);
            if (executor != null) {
                executor.onDescWrite(descriptor, status);
            }
        }
    };

    private void onDiscoverServicesCompleted(BluetoothGatt gatt, List<BluetoothGattService> services) {
            String deviceAddress = gatt.getDevice().getAddress();
            gatts.put(deviceAddress, gatt);

            if (services.isEmpty()) {
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

    private void handleDeviceConnection(final BluetoothGatt gatt) {
        final String deviceAddress = gatt.getDevice().getAddress();

        if (ignoredDevices.contains(deviceAddress)) {
            return;
        }

        if (!connectionInProgress.contains(deviceAddress)) {
            connectionInProgress.add(deviceAddress);
        }

        notifyOnDeviceStartConnecting(deviceAddress);

        BluetoothGatt storedGatt = gatts.get(deviceAddress);

        // If there is an already stored gatt for the same device and it's not the same connection,
        // it means that a new gatt connection opened for the same device, so clear the old one and
        // save the new one
        if (storedGatt != null && storedGatt != gatt) {
            closeGatt(storedGatt);
        }

        connectedDevices.add(deviceAddress);

        log(deviceAddress + " connected.");

        final GattOperation<?> refreshOp = new RefreshOperation()
                .addOnCompletionListener(data -> log("Refresh finished successfully"))
                .addOnErrorListener(msg -> {
                    logError("refreshOperation - " + msg);
                    notifyOnError(deviceAddress, ERR_C_GATT_OP, msg);
                });

        final GattOperation<?> discoverServicesOp = new DiscoverServicesOperation()
                .addOnCompletionListener(data -> {
                    log("Discover services finished successfully");
                    onDiscoverServicesCompleted(gatt, data);
                })
                .addOnErrorListener(msg -> {
                    logError("discoverServicesOperation - " + msg);
                    notifyOnError(deviceAddress, ERR_C_GATT_OP, msg);
                });

        GattExecutor executor = getExecutor(gatt);
        addExecutor(executor);
        executor.addOperation(refreshOp);
        executor.addOperation(discoverServicesOp);
    }

    private void handleDeviceDisconnection(BluetoothGatt gatt) {
        BluetoothDevice device = gatt.getDevice();
        String deviceAddress = device.getAddress();

        if (ignoredDevices.contains(deviceAddress)) {
            return;
        }

        BluetoothGatt storedGatt = gatts.get(deviceAddress);

        if (storedGatt == null) {
            log("while device disconnecting, unable to retrieve stored gatt, using the given gatt instead");
            storedGatt = gatt;
        }

        if (storedGatt != gatt) {
            log("while device disconnecting, stored gatt is different than the given gatt, get ready for another device disconnection handling call");
            handleDeviceDisconnection(storedGatt);
            storedGatt = gatt;
        }

        GattExecutor executor = getExecutor(storedGatt);
        if (executor == null) {
            log("While device disconnecting, unable to retrieve executor. Skipping executor clear");
        } else {
            executor.clear();
        }

        connectedDevices.remove(deviceAddress);

        log(deviceAddress + " disconnected.");

        if (ignoreInProgress.contains(deviceAddress) || isClosing || isBluetoothTurnedOff) {
            closeGatt(gatt);
            removeFromLists(deviceAddress);

            if (isClosing && gatts.isEmpty()) {
                handleCloseAfterAllDevicesDisconnected();
            }

            if (ignoreInProgress.contains(deviceAddress)) {
                ignoredDevices.add(deviceAddress);
                ignoreInProgress.remove(deviceAddress);
            }
        }

        notifyOnDeviceDisconnected(deviceAddress);
    }

    private void handleDeviceUnpaired(BluetoothGatt gatt) {
        BluetoothDevice device = gatt.getDevice();
        String deviceAddress = device.getAddress();

        BluetoothGatt storedGatt = gatts.get(deviceAddress);
        if (storedGatt == null) {
            log("while trying to handle device unpaired, unable to retrieve stored gatt. Using the given gatt instead");
            storedGatt = gatt;
        }

        GattExecutor executor = getExecutor(storedGatt);
        if (executor == null) {
            log("while trying to handle device unpaired, unable to retrieve executor. Skipping executor clear");
        } else {
            executor.clear();
        }

        connectedDevices.remove(deviceAddress);

        closeGatt(storedGatt);
        removeFromLists(deviceAddress);

        notifyOnDeviceDisconnected(deviceAddress);
    }

    private void handleCloseAfterAllDevicesDisconnected() {
        establishConnectionSent.clear();
        isClosing = false;
        isClosed = true;
    }

    private void closeGatt(BluetoothGatt gatt) {
        removeExecutor(getExecutor(gatt));
        try {
            gatt.close();
        } catch (SecurityException se) {
            log("Failed to call gatt.close() - No permission granted (Android 12)");
        }
    }

    private void removeFromLists(@NonNull String deviceAddress) {
        log("Removing " + deviceAddress + " from lists");
        gatts.remove(deviceAddress);
        establishConnectionSent.remove(deviceAddress);
    }

    public void setupNotification(@NonNull final String deviceAddress, @NonNull UUID serviceUUID, @NonNull final UUID characteristicUUID) {
        if (ignoredDevices.contains(deviceAddress)) {
            return;
        }

        final BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            notifyOnError(deviceAddress, ERR_C_DEVICE_NOT_CONNECTED, ErrorStrings.SET_NOTIFICATION);
            return;
        }

        GattOperation<?> setNotificationOp = new SetNotificationOperation(serviceUUID, characteristicUUID)
                .addOnCompletionListener(data -> notifyOnNotificationSubscribed(deviceAddress, characteristicUUID))
                .addOnErrorListener(msg -> notifyOnError(deviceAddress, ERR_C_DEVICE_NOT_CONNECTED, msg));


        getExecutor(gatt).addOperation(setNotificationOp);
    }

    public void readCharacteristic(@NonNull final String deviceAddress, @NonNull UUID serviceUUID, @NonNull final UUID characteristicUUID) {
        if (ignoredDevices.contains(deviceAddress)) {
            return;
        }

        final BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            notifyOnError(deviceAddress, ERR_C_DEVICE_NOT_CONNECTED, ErrorStrings.READ_CHAR);
            return;
        }

        GattOperation<?> characteristicReadOp = new CharacteristicReadOperation(serviceUUID, characteristicUUID)
                .addOnCompletionListener(data -> notifyOnCharacteristicRead(deviceAddress, characteristicUUID, data))
                .addOnErrorListener(msg -> notifyOnError(deviceAddress, ERR_C_DEVICE_NOT_CONNECTED, msg))
                .addOnNotFoundListener(message -> notifyOnCharacteristicNotFound(deviceAddress, characteristicUUID));

        getExecutor(gatt).addOperation(characteristicReadOp);
    }

    public void writeCharacteristic(@NonNull final String deviceAddress, @NonNull UUID serviceUUID, @NonNull final UUID characteristicUUID, @NonNull byte[] data) {
        if (ignoredDevices.contains(deviceAddress)) {
            return;
        }

        final BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            notifyOnError(deviceAddress, ERR_C_DEVICE_NOT_CONNECTED, ErrorStrings.WRITE_CHAR);
            return;
        }

        GattOperation<?> characteristicWriteOp = new CharacteristicWriteOperation(serviceUUID, characteristicUUID, data)
                .addOnCompletionListener(data1 -> notifyOnCharacteristicWrite(deviceAddress, characteristicUUID, data1))
                .addOnErrorListener(msg -> notifyOnError(deviceAddress, ERR_C_DEVICE_NOT_CONNECTED, msg));

        getExecutor(gatt).addOperation(characteristicWriteOp);
    }

    private void disconnectAllDevices() {
        for (String s : gatts.keySet()) {
            disconnectDevice(s);
        }
    }

    private void disconnectDevice(String deviceAddress) {
        BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            return;
        }
        try {
            gatt.disconnect();
        } catch (SecurityException se) {
            log("Failed to call gatt.disconnect() - No permission granted (Android 12)");
        }
    }

    @NonNull
    public Set<String> getConnectedDevices() {
        return connectedDevices;
    }

    public int numOfConnectedDevices() {
        return gatts.size();
    }

    public boolean setBluetooth(boolean enable) {
        boolean isEnabled = bluetoothAdapter.isEnabled();
        try {
            if (enable && !isEnabled) {
                return bluetoothAdapter.enable();
            } else if(!enable && isEnabled) {
                return bluetoothAdapter.disable();
            }
        } catch (SecurityException se) {
            log("Failed to call bluetoothAdapter.enable()/bluetoothAdapter.disable() - No permission granted (Android 12)");
            return false;
        }
        // No need to change bluetooth state
        return true;
    }

    public void refreshBond(String deviceAddress) {
        if (ignoredDevices.contains(deviceAddress)) {
            return;
        }

        BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            return;
        }

        if (!restartBondRequested) {
            restartBondRequested = true;

            removeBond(gatt.getDevice());
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isClosed) {
                return;
            }

            boolean intentActionIsStateChanged = false;
            try {
                intentActionIsStateChanged = intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED);
            } catch (NullPointerException ignored) { }

            if (intentActionIsStateChanged) {
                if (bluetoothAdapter != null) {

                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_ON:
                            log("Bluetooth turned ON");
                            isBluetoothTurnedOff = false;
                            notifyOnBluetoothTurnedOn();
                            establishConnections();
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            log("Bluetooth turned OFF");
                            isBluetoothTurnedOff = true;
                            notifyOnBluetoothTurnedOff();
                            handleBluetoothOff();
                            break;
                    }
                }
            }

            boolean intentActionIsBondStateChanged = false;
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
                    if (restartBondRequested) {
                        restartBondRequested = false;
                    }
                    log("Paired - " + device.toString());
                    establishConnection(device);
                }

                if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    log("Unpaired - " + device.toString());

                    BluetoothGatt storedGatt = gatts.get(device.toString());
                    if (storedGatt == null) {
                        log("while trying to handle device unpaired, unable to retrieve stored gatt.");
                    } else {
                        GattExecutor executor = getExecutor(storedGatt);
                        if (executor == null) {
                            log("while trying to handle device unpaired, unable to retrieve executor. Skipping executor clear");
                        } else {
                            executor.clear();
                        }
                        closeGatt(storedGatt);
                    }
                    connectedDevices.remove(device.toString());
                    removeFromLists(device.toString());

                    if (restartBondRequested) {
                        createBond(device);
                    }
                }
            }
        }
    };

    private void handleBluetoothOff() {
        establishConnectionSent.clear();
        for (BluetoothGatt gatt: gatts.values()) {
            handleDeviceDisconnection(gatt);
        }
    }

    private void createBond(final BluetoothDevice device) {
        if (ignoredDevices.contains(device.getAddress())) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {

            log("Pairing device - " + device.getAddress());
            Method localMethod;
            try {
                localMethod = device.getClass().getMethod("createBond");

                boolean success;
                int numOfRequests = 0;
                do {
                    numOfRequests++;
                    success = (boolean) localMethod.invoke(device);
                    log("Create bond - " + success);
                } while (!success && numOfRequests < 3);

            } catch (Exception e) {
                String message = e.getMessage() == null ? "Unknown error" : e.getMessage();
                logError("An exception occurred while creating bond. " + message);
            }

        });
    }

    private void removeBond(final BluetoothDevice device) {
        if (ignoredDevices.contains(device.getAddress())) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {

            log("Unpairing device - " + device.getAddress());
            Method localMethod;
            try {
                // TODO - this probably doesn't work anymore on version of Android over 24
                localMethod = device.getClass().getMethod("removeBond");

                boolean success;
                int numOfDiscoverRequests = 0;
                do {
                    numOfDiscoverRequests++;
                    success = (boolean) localMethod.invoke(device);
                    log("Remove bond - " + success);
                } while (!success && numOfDiscoverRequests < 3);

            } catch (Exception e) {
                logError(e.toString());
                String message = e.getMessage() == null ? "Unknown error" : e.getMessage();
                logError("An exception occurred while removing bond. " + message);
            }

        });
    }

    private GattExecutor getExecutor(BluetoothGatt gatt) {
        String deviceAddress = gatt.getDevice().getAddress();
        if (executors.containsKey(deviceAddress)) {
            return executors.get(deviceAddress);
        }
        log("Creating new executor");
        return new GattExecutor(gatt);
    }

    private void addExecutor(GattExecutor executor) {
        String deviceAddress = executor.getDeviceAddress();
        if (executors.containsKey(deviceAddress)) {
            log("Executor already exists...");
            if (executors.get(deviceAddress) != executor) {
                logError("Did you just create a different executor?");
            }
        } else {
            executors.put(deviceAddress, executor);
        }
    }

    private void removeExecutor(GattExecutor executor) {
        if (executor != null) {
            executor.clear();
            executors.remove(executor.getDeviceAddress());
        }
    }

    private void notifyOnBluetoothTurnedOn() {
        bluetoothListeners.notifyAll(BluetoothListener::onBluetoothTurnedOn);
    }

    private void notifyOnBluetoothTurnedOff() {
        bluetoothListeners.notifyAll(BluetoothListener::onBluetoothTurnedOff);
    }

    private void notifyOnDeviceStartConnecting(@NonNull final String deviceAddress) {
        bluetoothListeners.notifyAll(listener -> listener.onDeviceStartConnecting(deviceAddress));
    }

    private void notifyOnDeviceConnected(@NonNull final String deviceAddress) {
        bluetoothListeners.notifyAll(listener -> listener.onDeviceConnected(deviceAddress));
    }

    private void notifyOnDeviceAlreadyConnected(@NonNull final String deviceAddress) {
        bluetoothListeners.notifyAll(listener -> listener.onDeviceAlreadyConnected(deviceAddress));
    }

    private void notifyOnDeviceDisconnected(@NonNull final String deviceAddress) {
        bluetoothListeners.notifyAll(listener -> listener.onDeviceDisconnected(deviceAddress));
    }

    private void notifyOnCharacteristicRead(@NonNull final String deviceAddress, @NonNull final UUID characteristic, @NonNull final byte[] data) {
        bluetoothListeners.notifyAll(listener -> listener.onCharacteristicRead(deviceAddress, characteristic, data));
    }

    private void notifyOnCharacteristicNotFound(@NonNull final String deviceAddress, @NonNull final UUID characteristic) {
        bluetoothListeners.notifyAll(listener -> listener.onCharacteristicNotFound(deviceAddress, characteristic));
    }

    private void notifyOnCharacteristicWrite(@NonNull final String deviceAddress, @NonNull final UUID characteristic, @NonNull final byte[] data) {
        bluetoothListeners.notifyAll(listener -> listener.onCharacteristicWrite(deviceAddress, characteristic, data));
    }

    private void notifyOnNotificationSubscribed(@NonNull final String deviceAddress, @NonNull final UUID characteristic) {
        bluetoothListeners.notifyAll(listener -> listener.onNotificationSubscribed(deviceAddress, characteristic));
    }

    private void notifyOnNotificationReceived(@NonNull final String deviceAddress, @NonNull final UUID characteristic, @NonNull final byte[] data) {
        bluetoothListeners.notifyAll(listener -> listener.onNotificationReceived(deviceAddress, characteristic, data));
    }

    private void notifyOnError(@NonNull final String deviceAddress, final int code, @NonNull final String description) {
        connectionInProgress.clear();
        bluetoothListeners.notifyAll(listener -> listener.onError(deviceAddress, code, description));
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
