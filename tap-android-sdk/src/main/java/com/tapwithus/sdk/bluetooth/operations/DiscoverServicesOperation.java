package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import androidx.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.ErrorStrings;

import java.util.List;

public class DiscoverServicesOperation extends GattOperation<List<BluetoothGattService>> {

    @Override
    public OperationType type() {
        return OperationType.DISCOVER_SERVICES;
    }

    @Override
    public void onExecute(@NonNull BluetoothGatt gatt) {
        try {
            if (!gatt.discoverServices()) {
                postOnError(ErrorStrings.DISCOVER_SERVICES_OP_INIT_FAIL);
            }
        } catch (SecurityException se) {
            postOnError(ErrorStrings.DISCOVER_SERVICES_OP_INIT_FAIL);
            postOnError(ErrorStrings.LACKING_PERMISSION_FAIL);
        }
    }

    @Override
    public void gattCallback(Object gattOrCharOrDesc, int status) {
        if (gattOrCharOrDesc instanceof BluetoothGatt) {
            postOnCompletion(((BluetoothGatt) gattOrCharOrDesc).getServices());
            return;
        }

        postOnError(ErrorStrings.GATT_CALLBACK_MISMATCH);
    }
}
