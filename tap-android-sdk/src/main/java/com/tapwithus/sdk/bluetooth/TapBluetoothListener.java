package com.tapwithus.sdk.bluetooth;

import android.support.annotation.NonNull;

import com.tapwithus.sdk.airmouse.AirMousePacket;
import com.tapwithus.sdk.mouse.MousePacket;

public interface TapBluetoothListener {
    void onBluetoothTurnedOn();
    void onBluetoothTurnedOff();
    void onTapStartConnecting(@NonNull String tapAddress);
    void onTapConnected(@NonNull String tapAddress);
    void onTapAlreadyConnected(@NonNull String tapAddress);
    void onTapDisconnected(@NonNull String tapAddress);
    void onNameRead(@NonNull String tapAddress, @NonNull String name);
    void onNameWrite(@NonNull String tapAddress, @NonNull String name);
    void onBatteryRead(@NonNull String tapAddress, int battery);
    void onSerialNumberRead(@NonNull String tapAddress, @NonNull String serialNumber);
    void onHwVerRead(@NonNull String tapAddress, @NonNull String hwVer);
    void onFwVerRead(@NonNull String tapAddress, @NonNull String fwVer);
    void onControllerModeStarted(@NonNull String tapAddress);
    void onTextModeStarted(@NonNull String tapAddress);
    void onTapInputSubscribed(@NonNull String tapAddress);
    void onMouseInputSubscribed(@NonNull String tapAddress);
    void onAirMouseInputSubscribed(@NonNull String tapAddress);
    void onTapInputReceived(@NonNull String tapAddress, int data);
    void onMouseInputReceived(@NonNull String tapAddress, @NonNull MousePacket data);
    void onAirMouseInputReceived(@NonNull String tapAddress, @NonNull AirMousePacket data);
    void onError(@NonNull String tapAddress, int code, @NonNull String description);
}
