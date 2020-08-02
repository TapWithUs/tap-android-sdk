package com.tapwithus.sdk;

import androidx.annotation.NonNull;

import com.tapwithus.sdk.airmouse.AirMousePacket;
import com.tapwithus.sdk.mouse.MousePacket;
import com.tapwithus.sdk.mode.RawSensorData;
public interface TapListener {
    void onBluetoothTurnedOn();
    void onBluetoothTurnedOff();
    void onTapStartConnecting(@NonNull String tapIdentifier);
    void onTapConnected(@NonNull String tapIdentifier);
    void onTapDisconnected(@NonNull String tapIdentifier);
    void onTapResumed(@NonNull String tapIdentifier);
    void onTapChanged(@NonNull String tapIdentifier);
    void onTapInputReceived(@NonNull String tapIdentifier, int data, int repeatData);
    void onTapShiftSwitchReceived(@NonNull String tapIdentifier, int data);
    void onMouseInputReceived(@NonNull String tapIdentifier, @NonNull MousePacket data);
    void onAirMouseInputReceived(@NonNull String tapIdentifier, @NonNull AirMousePacket data);
    void onRawSensorInputReceived(@NonNull String tapIdentifier, @NonNull RawSensorData rsData);
    void onTapChangedState(@NonNull String tapIdentifier, int state);
    void onError(@NonNull String tapIdentifier, int code, @NonNull String description);
}
