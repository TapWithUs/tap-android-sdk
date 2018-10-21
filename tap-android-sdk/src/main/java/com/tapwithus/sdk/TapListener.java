package com.tapwithus.sdk;

import android.support.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.MousePacket;
import com.tapwithus.sdk.tap.Tap;

import java.util.UUID;

public interface TapListener {
    void onBluetoothTurnedOn();
    void onBluetoothTurnedOff();
    void onTapStartConnecting(@NonNull String tapIdentifier);
    void onTapConnected(@NonNull String tapIdentifier);
    void onTapDisconnected(@NonNull String tapIdentifier);
    void onTapResumed(@NonNull String tapIdentifier);
    void onTapChanged(@NonNull String tapIdentifier);
    void onControllerModeStarted(@NonNull String tapIdentifier);
    void onTextModeStarted(@NonNull String tapIdentifier);
    void onTapInputReceived(@NonNull String tapIdentifier, int data);
    void onMouseInputReceived(@NonNull String tapIdentifier, @NonNull MousePacket data);
    void onError(@NonNull String tapIdentifier, int code, @NonNull String description);
}
