package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.tapwithus.sdk.bluetooth.ErrorStrings;

import java.util.UUID;

public abstract class BaseCharacteristicOperation extends GattOperation<byte[]> {

    protected UUID service;
    protected UUID characteristic;

    public BaseCharacteristicOperation(UUID service, UUID characteristic) {
        this.service = service;
        this.characteristic = characteristic;
    }

    protected BluetoothGattCharacteristic extractCharacteristic(BluetoothGatt gatt) {
        BluetoothGattService s = gatt.getService(service);
        if (service == null) {
            postOnError(ErrorStrings.NO_SERVICE);
            return null;
        }

        try {
            return s.getCharacteristic(characteristic);
        } catch (NullPointerException e) {
            return null;
        }
    }
}
