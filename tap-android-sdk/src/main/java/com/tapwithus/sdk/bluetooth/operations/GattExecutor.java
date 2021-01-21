package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import androidx.annotation.NonNull;
import android.util.Log;

import com.tapwithus.sdk.bluetooth.callbacks.OnCompletionListener;
import com.tapwithus.sdk.bluetooth.callbacks.OnErrorListener;
import com.tapwithus.sdk.bluetooth.callbacks.OnNotFoundListener;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

public class GattExecutor implements OnCompletionListener<Object>, OnErrorListener, OnNotFoundListener {

    private static final String TAG = "GattExecutor";

    private final BluetoothGatt gatt;
    private final String deviceAddress;
    private Queue<GattOperation> operations = new LinkedBlockingDeque<>();
    private GattOperation currentOperation;

    private boolean isRunning = false;

    public GattExecutor(@NonNull BluetoothGatt gatt) {
        this.gatt = gatt;
        this.deviceAddress = gatt.getDevice().getAddress();
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public GattExecutor addOperation(GattOperation operation) {
        operation.addOnCompletionListener(this);
        operation.addOnErrorListener(this);
        operation.addOnNotFoundListener(this);

        operations.add(operation);
        run();

        return this;
    }

    public void onCharRead(@NonNull BluetoothGattCharacteristic characteristic, int status) {
        if (currentOperation != null && currentOperation.type() == OperationType.CHAR_READ) {
            currentOperation.gattCallback(characteristic, status);
        }
    }

    public void onCharWrite(@NonNull BluetoothGattCharacteristic characteristic, int status) {
        if (currentOperation != null && currentOperation.type() == OperationType.CHAR_WRITE) {
            currentOperation.gattCallback(characteristic, status);
        }
    }

    public void onCharChange(@NonNull BluetoothGattCharacteristic characteristic) {
        if (currentOperation != null && currentOperation.type() == OperationType.CHAR_CHANGE) {
            currentOperation.gattCallback(characteristic);
        }
    }

    public void onDescRead(@NonNull BluetoothGattDescriptor descriptor, int status) {
        if (currentOperation != null && currentOperation.type() == OperationType.DESC_READ) {
            currentOperation.gattCallback(descriptor, status);
        }
    }

    public void onDescWrite(@NonNull BluetoothGattDescriptor descriptor, int status) {
        if (currentOperation != null && currentOperation.type() == OperationType.DESC_WRITE) {
            currentOperation.gattCallback(descriptor, status);
        }
    }

    public void onServicesDiscovered(int status) {
        if (currentOperation != null && currentOperation.type() == OperationType.DISCOVER_SERVICES) {
            currentOperation.gattCallback(gatt, status);
        }
    }

    @Override
    public void onCompletion(Object data) {
        currentOperation.removeOnCompletionListener(this);
        isRunning = false;
        run();
    }

    @Override
    public void onError(String msg) {
        logError("GattExecutor onError - " + msg);

        if (currentOperation != null) {
            currentOperation.removeOnErrorListener(this);
        }
        isRunning = false;
    }

    @Override
    public void onNotFound(String message) {
        currentOperation.removeOnNotFoundListener(this);
        isRunning = false;
        run();
    }

    public void clear() {
        isRunning = false;
        operations.clear();
    }

    private void run() {
        if (isRunning) {
            return;
        }

        currentOperation = getNextOperation();
        if (currentOperation != null && !currentOperation.isRunning()) {
            isRunning = true;
            currentOperation.execute(gatt);
        }
    }

    private GattOperation getNextOperation() {
        if (currentOperation != null && !currentOperation.isCompleted()) {
            return currentOperation;
        }

        if (!operations.isEmpty()) {
            return operations.poll();
        }

        return null;
    }

    private void logError(String message) {
        Log.e(TAG, message);
    }
}
