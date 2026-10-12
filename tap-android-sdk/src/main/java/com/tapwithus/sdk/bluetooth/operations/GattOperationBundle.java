package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.ErrorStrings;
import com.tapwithus.sdk.bluetooth.callbacks.OnCompletionListener;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

public class GattOperationBundle extends GattOperation<Void> implements OnCompletionListener<Object> {

    private Queue<GattOperation> operations;
    private GattOperation currentOperation;

    public GattOperationBundle() {
        operations = new LinkedBlockingDeque<>();
    }

    public void addOperation(@NonNull GattOperation operation) {
        if (operations.isEmpty()) {
            currentOperation = operation;
        }
        operations.add(operation);
    }

    @Override
    public OperationType type() {
        if (currentOperation != null) {
            return currentOperation.type();
        }
        return null;
    }

    @Override
    public void onExecute(@NonNull BluetoothGatt gatt) {
        if (currentOperation != null) {
            currentOperation.execute(gatt);
            return;
        }

        postOnError(ErrorStrings.OP_BUNDLE_EMPTY_EXEC);
    }

    @Override
    public void gattCallback(@NonNull Object gattOrCharOrDesc, int status) {
        currentOperation.gattCallback(gattOrCharOrDesc, status);

        GattOperation nextOperation = getNextOperation();
        if (nextOperation == null) {
            postOnCompletion(null);
        } else {
            currentOperation = nextOperation;
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

    @Override
    public void onCompletion(Object data) {
        if (currentOperation != null) {
            currentOperation.removeOnCompletionListener(this);
        }

        // TODO Need to finish this method and this class
    }
}
