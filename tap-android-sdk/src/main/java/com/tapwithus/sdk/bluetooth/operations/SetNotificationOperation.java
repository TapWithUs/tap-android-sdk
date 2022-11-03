package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import androidx.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.ErrorStrings;

import java.util.UUID;

public class SetNotificationOperation extends DescriptorWriteOperation {

    private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public SetNotificationOperation(UUID service, UUID characteristic) {
        super(service, characteristic, CCCD, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    }

    @Override
    public void execute(@NonNull BluetoothGatt gatt) {

        BluetoothGattCharacteristic c = extractCharacteristic(gatt);
        if (c == null) {
            postOnError(ErrorStrings.NO_CHARACTERISTIC + ": " + this.characteristic.toString());
            return;
        }

        BluetoothGattDescriptor d = c.getDescriptor(descriptor);
        if (d == null) {
            postOnError(ErrorStrings.NO_DESCRIPTOR);
            return;
        }

        int characteristicProperties = c.getProperties();
        byte[] notificationValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        if ((characteristicProperties & 0x10) == 16) {
            notificationValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        } else if ((characteristicProperties & 0x20) == 32) {
            notificationValue = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
        }

        if (notificationValue.length == 0) {
            postOnError(ErrorStrings.NOTIFY_TYPE_FAIL);
            return;
        }

        if (!d.setValue(notificationValue)) {
            postOnError(ErrorStrings.VALUE_STORE_FAIL);
            return;
        }

        try {
            if (!gatt.setCharacteristicNotification(c, true)) {
                postOnError(ErrorStrings.NOTIFY_OP_INIT_FAIL);
                return;
            }
        } catch (SecurityException se) {
            postOnError(ErrorStrings.NOTIFY_OP_INIT_FAIL);
            postOnError(ErrorStrings.LACKING_PERMISSION_FAIL);
            return;
        }

        try {
            if (!gatt.writeDescriptor(d)) {
                postOnError(ErrorStrings.WRITE_OP_INIT_FAIL);
            }
        } catch (SecurityException se) {
            postOnError(ErrorStrings.NOTIFY_OP_INIT_FAIL);
            postOnError(ErrorStrings.LACKING_PERMISSION_FAIL);
        }
    }
}
