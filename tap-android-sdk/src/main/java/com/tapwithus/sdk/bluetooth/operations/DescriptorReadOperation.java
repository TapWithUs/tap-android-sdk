package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import androidx.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.ErrorStrings;

import java.util.UUID;

public class DescriptorReadOperation extends DescriptorOperation {

    public DescriptorReadOperation(UUID service, UUID characteristic, UUID descriptor) {
        super(service, characteristic, descriptor);
    }

    @Override
    public OperationType type() {
        return OperationType.DESC_READ;
    }

    @Override
    public void onExecute(@NonNull BluetoothGatt gatt) {
        BluetoothGattDescriptor d = extractDescriptor(gatt);
        if (d == null) {
            postOnError(ErrorStrings.NO_DESCRIPTOR);
            return;
        }

        try {
            if (!gatt.readDescriptor(d)) {
                postOnError(ErrorStrings.READ_OP_INIT_FAIL);
            }
        } catch (SecurityException se) {
                postOnError(ErrorStrings.READ_OP_INIT_FAIL);
                postOnError(ErrorStrings.LACKING_PERMISSION_FAIL);
        }
    }
}
