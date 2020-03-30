package com.tapwithus.sdk.bluetooth;

import androidx.annotation.NonNull;

import java.util.UUID;

public interface BluetoothListener {
    void onBluetoothTurnedOn();
    void onBluetoothTurnedOff();
    void onDeviceStartConnecting(@NonNull String deviceAddress);
    void onDeviceConnected(@NonNull String deviceAddress);
    void onDeviceAlreadyConnected(@NonNull String deviceAddress);
    void onDeviceDisconnected(@NonNull String deviceAddress);
    void onCharacteristicRead(@NonNull String deviceAddress, @NonNull UUID characteristic, @NonNull byte[] data);
    void onCharacteristicNotFound(@NonNull String deviceAddress, @NonNull UUID characteristic);
    void onCharacteristicWrite(@NonNull String deviceAddress, @NonNull UUID characteristic, @NonNull byte[] data);
    void onNotificationSubscribed(@NonNull String deviceAddress, @NonNull UUID characteristic);
    void onNotificationReceived(@NonNull String deviceAddress, @NonNull UUID characteristic, @NonNull byte[] data);
    void onError(@NonNull String deviceAddress, int code, @NonNull String description);
}
