package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import androidx.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.ErrorStrings;

import java.util.UUID;

public class DescriptorWriteOperation extends DescriptorOperation {

    protected byte[] data;

    public DescriptorWriteOperation(UUID service, UUID characteristic, UUID descriptor, byte[] data) {
        super(service, characteristic, descriptor);
        this.data = data;
    }

    @Override
    public OperationType type() {
        return OperationType.DESC_WRITE;
    }

    @Override
    public void onExecute(@NonNull BluetoothGatt gatt) {
        BluetoothGattDescriptor d = extractDescriptor(gatt);
        if (d == null) {
            postOnError(ErrorStrings.NO_DESCRIPTOR);
            return;
        }

        if (!d.setValue(data)) {
            postOnError(ErrorStrings.VALUE_STORE_FAIL);
            return;
        }

        try {
            if (!gatt.writeDescriptor(d)) {
                postOnError(ErrorStrings.WRITE_OP_INIT_FAIL);
            }
        } catch (SecurityException se) {
            postOnError(ErrorStrings.WRITE_OP_INIT_FAIL);
            postOnError(ErrorStrings.LACKING_PERMISSION_FAIL);
        }

    }
}
