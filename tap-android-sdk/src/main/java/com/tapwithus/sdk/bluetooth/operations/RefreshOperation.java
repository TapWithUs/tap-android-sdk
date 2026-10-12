package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.ErrorStrings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RefreshOperation extends GattOperation<Void> {

    public RefreshOperation() {
        setPostDelay(1600);
    }

    @Override
    public OperationType type() {
        return OperationType.REFRESH;
    }

    @Override
    public void onExecute(@NonNull BluetoothGatt gatt) {
        try {
            //noinspection JavaReflectionMemberAccess
            Method localMethod = gatt.getClass().getMethod("refresh");
            boolean success = (boolean) localMethod.invoke(gatt);
            if (!success) {
                postOnError(ErrorStrings.REFRESH_OP_INIT_FAIL);
            }
            gattCallback(GATT_SUCCESS);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            String message = e.getMessage() == null ? "Unknown error" : e.getMessage();
            postOnError(ErrorStrings.REFRESH_OP_INIT_FAIL + ". " + message);
        }
    }

    @Override
    public void gattCallback(Object gattOrCharOrDesc, int status) {
        postOnCompletion(null);
    }
}
