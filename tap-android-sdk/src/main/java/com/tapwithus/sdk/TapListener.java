package com.tapwithus.sdk;

import com.tapwithus.sdk.bluetooth.MousePacket;

import java.util.UUID;

public interface TapListener {
    void onBluetoothTurnedOn();
    void onBluetoothTurnedOff();
    void onTapConnected(String tapIdentifier);
    void onTapDisconnected(String tapIdentifier);
    void onNameRead(String tapIdentifier, String name);
    void onNameWrite(String tapIdentifier, String name);
    void onCharacteristicRead(String tapIdentifier, UUID characteristic, byte[] data);
    void onCharacteristicWrite(String tapIdentifier, UUID characteristic, byte[] data);
    void onControllerModeStarted(String tapIdentifier);
    void onTextModeStarted(String tapIdentifier);
    void onTapInputReceived(String tapIdentifier, int data);
    void onMouseInputReceived(String tapIdentifier, MousePacket data);
}
