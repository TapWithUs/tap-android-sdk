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
import com.tapwithus.sdk.bluetooth.callbacks.OnCompletionListener;
import com.tapwithus.sdk.bluetooth.callbacks.OnErrorListener;
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

public class BluetoothManager {

    private static final String TAG = "BluetoothManager";
    private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    protected static final UUID GENERIC_ATTRIBUTE = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    protected static final UUID SERVICE_CHANGED = UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb");
    private static final Map<String, BluetoothGatt> gatts = new ConcurrentHashMap<>();
    private static final Map<String, GattExecutor> executors = new ConcurrentHashMap<>();
    private static final List<String> establishConnectionSent = new CopyOnWriteArrayList<>();
    private static final Set<String> isConnected = new CopyOnWriteArraySet<>();

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

    private boolean isClosing = false;
    private boolean isClosed = false;
    private boolean isBluetoothTurnedOff = false;

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
        isClosed = false;
        establishConnections();
    }

    public void close() {
        log("BluetoothManager is closing...");
        isClosing = true;

        if (numOfConnectedDevices() != 0) {
            disconnectDevices();
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

            if (status == 22) {
                log("Status " + status);
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
            GattExecutor executor = getExecutor(gatt);
            if (executor != null) {
                executor.onServicesDiscovered(status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            log("On Characteristic Read - " + characteristic.getUuid().toString());
            GattExecutor executor = getExecutor(gatt);
            if (executor != null) {
                executor.onCharRead(characteristic, status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            log("On Characteristic Write");
            GattExecutor executor = getExecutor(gatt);
            if (executor != null) {
                executor.onCharWrite(characteristic, status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            log("On characteristic changed");
            GattExecutor executor = getExecutor(gatt);
            if (executor != null) {
                executor.onCharChange(characteristic);
            }

            String deviceAddress = gatt.getDevice().getAddress();

            notifyOnNotificationReceived(deviceAddress, characteristic.getUuid(), characteristic.getValue());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//            log("On Descriptor Write");
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
        String deviceAddress = gatt.getDevice().getAddress();

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

        if (!isConnected.contains(deviceAddress)) {
            isConnected.add(deviceAddress);
        }

        log(deviceAddress + " connected.");

        final GattOperation refreshOp = new RefreshOperation()
                .addOnCompletionListener(new OnCompletionListener<Void>() {
                    @Override
                    public void onCompletion(Void data) {
                        log("Refresh finished successfully");
                    }
                })
                .addOnErrorListener(new OnErrorListener() {
                    @Override
                    public void onError(String msg) {
                        logError("refreshOperation - " + msg);
                    }
                });

        final GattOperation discoverServicesOp = new DiscoverServicesOperation()
                .addOnCompletionListener(new OnCompletionListener<List<BluetoothGattService>>() {
                    @Override
                    public void onCompletion(List<BluetoothGattService> data) {
                        onDiscoverServicesCompleted(gatt, data);
                    }
                })
                .addOnErrorListener(new OnErrorListener() {
                    @Override
                    public void onError(String msg) {
                        logError("discoverServicesOperation - " + msg);
                    }
                });

        GattExecutor executor = getExecutor(gatt);
        executor.addOperation(refreshOp);
        executor.addOperation(discoverServicesOp);
        addExecutor(executor);

//        log("Waiting 1600 ms for a possible Service Changed indication...");
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                boolean success;
//                int numOfDiscoverRequests = 0;
//                do {
//                    numOfDiscoverRequests++;
//                    success = gatt.discoverServices();
//                    log("Discover services - " + success);
//                } while (!success && numOfDiscoverRequests < 3);
//            }
//
//        }, 1600);
    }

    private void handleDeviceDisconnection(BluetoothGatt gatt) {
        BluetoothDevice device = gatt.getDevice();
        String deviceAddress = device.getAddress();

        BluetoothGatt storedGatt = gatts.get(deviceAddress);
        if (storedGatt == null || storedGatt != gatt) {
            closeGatt2(gatt);
            return;
        }

        GattExecutor executor = getExecutor(storedGatt);
        if (executor != null) {
            executor.clear();
        }

        log(deviceAddress + " disconnected.");

        if (isConnected.contains(deviceAddress)) {
            isConnected.remove(deviceAddress);
        }

        if (isClosing || isBluetoothTurnedOff) {
            closeGatt2(gatt);
            removeFromLists(deviceAddress);

            if (isClosing && gatts.isEmpty()) {
                handleCloseAfterAllDevicesDisconnected();
            }
        }

        notifyOnDeviceDisconnected(deviceAddress);
    }

    private void handleDeviceUnpaired(BluetoothGatt gatt) {
        BluetoothDevice device = gatt.getDevice();
        String deviceAddress = device.getAddress();

        BluetoothGatt storedGatt = gatts.get(deviceAddress);
        if (storedGatt == null) {
            return;
        }

        GattExecutor executor = getExecutor(storedGatt);
        if (executor != null) {
            executor.clear();
        }

        if (isConnected.contains(deviceAddress)) {
            isConnected.remove(deviceAddress);
        }

        closeGatt2(storedGatt);
        removeFromLists(deviceAddress);

        notifyOnDeviceDisconnected(deviceAddress);
    }

    private void handleCloseAfterAllDevicesDisconnected() {
        establishConnectionSent.clear();
        isClosing = false;
        isClosed = true;
    }

    private void closeGatt(BluetoothGatt gatt) {
        gatt.disconnect();
        gatt.close();
    }

    private void closeGatt2(BluetoothGatt gatt) {
        removeExecutor(getExecutor(gatt));
        gatt.close();
    }

    private void removeFromLists(@NonNull String deviceAddress) {
        gatts.remove(deviceAddress);
        establishConnectionSent.remove(deviceAddress);
    }

    public void setupNotification(@NonNull final String deviceAddress, @NonNull UUID serviceUUID, @NonNull final UUID characteristicUUID) {
        final BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            notifyOnError(deviceAddress, ERR_DEVICE_NOT_CONNECTED, "Device is not connected to set notification");
            return;
        }

        GattOperation cccdSetNotificationOp = new SetNotificationOperation(serviceUUID, characteristicUUID)
                .addOnCompletionListener(new OnCompletionListener<byte[]>() {
                    @Override
                    public void onCompletion(byte[] data) {
                        notifyOnNotificationSubscribed(deviceAddress, characteristicUUID);
                    }
                })
                .addOnErrorListener(new OnErrorListener() {
                    @Override
                    public void onError(String msg) {
                        notifyOnError(deviceAddress, ERR_DEVICE_NOT_CONNECTED, msg);
                    }
                });


        getExecutor(gatt)
                .addOperation(cccdSetNotificationOp);
    }

    public void readCharacteristic(@NonNull final String deviceAddress, @NonNull UUID serviceUUID, @NonNull final UUID characteristicUUID) {
        final BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            notifyOnError(deviceAddress, ERR_DEVICE_NOT_CONNECTED, "Device is not connected to read characteristic");
            return;
        }

        GattOperation characteristicReadOp = new CharacteristicReadOperation(serviceUUID, characteristicUUID)
                .addOnCompletionListener(new OnCompletionListener<byte[]>() {
                    @Override
                    public void onCompletion(byte[] data) {
                        notifyOnCharacteristicRead(deviceAddress, characteristicUUID, data);
                    }
                })
                .addOnErrorListener(new OnErrorListener() {
                    @Override
                    public void onError(String msg) {
                        notifyOnError(deviceAddress, ERR_DEVICE_NOT_CONNECTED, msg);
                    }
                });

        getExecutor(gatt)
                .addOperation(characteristicReadOp);
    }

    public void writeCharacteristic(@NonNull final String deviceAddress, @NonNull UUID serviceUUID, @NonNull final UUID characteristicUUID, @NonNull byte[] data) {
        final BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            notifyOnError(deviceAddress, ERR_DEVICE_NOT_CONNECTED, "Device is not connected to read characteristic");
            return;
        }

        GattOperation characteristicWriteOp = new CharacteristicWriteOperation(serviceUUID, characteristicUUID, data)
                .addOnCompletionListener(new OnCompletionListener<byte[]>() {
                    @Override
                    public void onCompletion(byte[] data) {
                        notifyOnCharacteristicWrite(deviceAddress, characteristicUUID, data);
                    }
                })
                .addOnErrorListener(new OnErrorListener() {
                    @Override
                    public void onError(String msg) {
                        notifyOnError(deviceAddress, ERR_DEVICE_NOT_CONNECTED, msg);
                    }
                });

        getExecutor(gatt)
                .addOperation(characteristicWriteOp);
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
        gatt.disconnect();
    }

    @NonNull
    public Set<String> getConnectedDevices() {
        return isConnected;
//        return new HashSet<>(gatts.keySet());
    }

    public int numOfConnectedDevices() {
        return gatts.size();
    }

    private Boolean isGattConnectionExists(String deviceAddress) {
        return gatts.containsKey(deviceAddress);
    }

    private void handleBluetoothOff() {
        establishConnectionSent.clear();
        for (BluetoothGatt gatt: gatts.values()) {
            handleDeviceDisconnection(gatt);
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("SwitchIntDef")
        @Override
        public void onReceive(Context context, Intent intent) {

            if (isClosed) {
                return;
            }

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
                            isBluetoothTurnedOff = false;
                            notifyOnBluetoothTurnedOn();
                            establishConnections();
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            log("Bluetooth turned OFF");
                            isBluetoothTurnedOff = true;
                            notifyOnBluetoothTurnedOff();
                            handleBluetoothOff();
//                            if (bluetoothRestartRequested) {
//                                bluetoothRestartRequested = false;
//                                setBluetooth(true);
//                            }
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
                    if (restartBondRequested) {
                        restartBondRequested = false;
                    }
                    pairRequested = false;
                    log("Paired - " + device.toString());
                    establishConnection(device);
                }

                if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    unpairRequested = false;
                    log("Unpaired - " + device.toString());

                    if (restartBondRequested) {
                        createBond(device);
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

        BluetoothGatt gatt = gatts.get(deviceAddress);
        if (gatt == null) {
            return;
        }

        if (!restartBondRequested) {
            restartBondRequested = true;

            removeBond(gatt.getDevice());
        }
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

//    private void subscribeServiceChange(BluetoothGatt gatt) {
//        log("Subscribing to service change...");
//        setupNotification(gatt, GENERIC_ATTRIBUTE, SERVICE_CHANGED);
//    }

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

    private String getDeviceAddress(@NonNull BluetoothGatt gatt) {
        return gatt.getDevice().getAddress();
    }

    private void addExecutor(GattExecutor executor) {
        String deviceAddress = executor.getDeviceAddress();
        if (executors.containsKey(deviceAddress)) {
            log("Executor already exists...");
            if (executors.get(deviceAddress) != executor) {
                logError("Did you just create different executor?");
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

    private GattExecutor getExecutor(BluetoothGatt gatt) {
        String deviceAddress = getDeviceAddress(gatt);
        if (executors.containsKey(deviceAddress)) {
            return executors.get(deviceAddress);
        }
        log("Creating new executor");
        return new GattExecutor(gatt);
    }

}
