package com.tapwithus.sdk.bluetooth;

import java.util.UUID;

public interface TapBluetoothListener {
    void onBluetoothTurnedOn();
    void onBluetoothTurnedOff();
    void onTapConnected(String tapAddress);
    void onTapAlreadyConnected(String tapAddress);
    void onTapDisconnected(String tapAddress);
    void onNameRead(String tapAddress, String name);
    void onNameWrite(String tapAddress, String name);
    void onCharacteristicRead(String tapAddress, UUID characteristic, byte[] data);
    void onCharacteristicWrite(String tapAddress, UUID characteristic, byte[] data);
    void onNotificationSubscribed(String tapAddress, UUID characteristic);
    void onControllerModeStarted(String tapAddress);
    void onTextModeStarted(String tapAddress);
    void onTapInputReceived(String tapAddress, int data);
    void onMouseInputReceived(String tapAddress, MousePacket data);
}
