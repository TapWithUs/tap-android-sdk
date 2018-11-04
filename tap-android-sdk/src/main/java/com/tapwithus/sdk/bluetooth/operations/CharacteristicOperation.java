package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.tapwithus.sdk.bluetooth.ErrorStrings;

import java.util.UUID;

public abstract class CharacteristicOperation extends BaseCharacteristicOperation {

    public CharacteristicOperation(UUID service, UUID characteristic) {
        super(service, characteristic);
    }

    protected void onOperationCompleted(BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            postOnCompletion(characteristic.getValue());
        } else {
            postOnError(ErrorStrings.CHAR_OP_FAIL + " " + status);
        }
    }

    @Override
    public void gattCallback(Object gattOrCharOrDesc, int status) {
        if (gattOrCharOrDesc instanceof BluetoothGattCharacteristic) {
            onOperationCompleted((BluetoothGattCharacteristic) gattOrCharOrDesc, status);
            return;
        }

        postOnError(ErrorStrings.GATT_CALLBACK_MISMATCH);
    }
}
