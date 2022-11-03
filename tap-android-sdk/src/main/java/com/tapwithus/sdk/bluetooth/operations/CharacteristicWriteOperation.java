package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import androidx.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.ErrorStrings;

import java.util.UUID;

public class CharacteristicWriteOperation extends CharacteristicOperation {

    protected byte[] data;

    public CharacteristicWriteOperation(UUID service, UUID characteristic, byte[] data) {
        super(service, characteristic);
        this.data = data;
    }

    @Override
    public OperationType type() {
        return OperationType.CHAR_WRITE;
    }

    @Override
    public void onExecute(@NonNull BluetoothGatt gatt) {
        BluetoothGattCharacteristic c = extractCharacteristic(gatt);
        if (c == null) {
            postOnError(ErrorStrings.NO_CHARACTERISTIC);
            return;
        }

        if (!c.setValue(data)) {
            postOnError(ErrorStrings.VALUE_STORE_FAIL);
            return;
        }

        try {
            if (!gatt.writeCharacteristic(c)) {
                postOnError(ErrorStrings.WRITE_OP_INIT_FAIL);
            }
        } catch (SecurityException se) {
            postOnError(ErrorStrings.WRITE_OP_INIT_FAIL);
            postOnError(ErrorStrings.LACKING_PERMISSION_FAIL);
        }
    }
}
