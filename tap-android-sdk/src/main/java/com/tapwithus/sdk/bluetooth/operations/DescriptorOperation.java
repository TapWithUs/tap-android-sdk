package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import androidx.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.ErrorStrings;

import java.util.UUID;

public abstract class DescriptorOperation extends BaseCharacteristicOperation {

    protected UUID descriptor;

    public DescriptorOperation(UUID service, UUID characteristic, UUID descriptor) {
        super(service, characteristic);
        this.descriptor = descriptor;
    }

    protected void onOperationCompleted(BluetoothGattDescriptor descriptor, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            postOnCompletion(descriptor.getValue());
        } else {
            postOnError(ErrorStrings.DESC_OP_FAIL + status);
        }
    }

    protected BluetoothGattDescriptor extractDescriptor(BluetoothGatt gatt) {

        BluetoothGattCharacteristic c = extractCharacteristic(gatt);
        if (c == null) {
            postOnError(ErrorStrings.NO_CHARACTERISTIC);
            return null;
        }

        return c.getDescriptor(descriptor);
    }

    @Override
    public void gattCallback(@NonNull Object gattOrCharOrDesc, int status) {
        if (gattOrCharOrDesc instanceof BluetoothGattDescriptor) {
            onOperationCompleted((BluetoothGattDescriptor) gattOrCharOrDesc, status);
            return;
        }

        postOnError(ErrorStrings.GATT_CALLBACK_MISMATCH);
    }
}
