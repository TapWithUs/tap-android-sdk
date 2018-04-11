package com.tapwithus.sdk.bluetooth;

import java.util.UUID;

public interface BluetoothListener {
    void onBluetoothTurnedOn();
    void onBluetoothTurnedOff();
    void onDeviceConnected(String deviceAddress);
    void onDeviceAlreadyConnected(String deviceAddress);
    void onDeviceDisconnected(String deviceAddress);
    void onCharacteristicRead(String deviceAddress, UUID characteristic, byte[] data);
    void onCharacteristicWrite(String deviceAddress, UUID characteristic, byte[] data);
    void onNotificationSubscribed(String deviceAddress, UUID characteristic);
    void onNotificationReceived(String deviceAddress, UUID characteristic, byte[] data);
}
