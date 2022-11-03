package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.ErrorStrings;

public class DisconnectOperation extends GattOperation<Void> {

    @Override
    public OperationType type() {
        return OperationType.DISCONNECT;
    }

    @Override
    public void onExecute(@NonNull BluetoothGatt gatt) {
        try {
            gatt.disconnect();
        } catch (SecurityException se) {
            postOnError(ErrorStrings.GATT_FAILURE);
            postOnError(ErrorStrings.LACKING_PERMISSION_FAIL);
        }

    }

    @Override
    public void gattCallback(Object gattOrCharOrDesc, int status) {
        if (gattOrCharOrDesc == null || gattOrCharOrDesc instanceof BluetoothGatt) {
            postOnCompletion(null);
            return;
        }

        postOnError(ErrorStrings.GATT_CALLBACK_MISMATCH);
    }
}
