package com.tapwithus.sdk;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.tapwithus.sdk.bluetooth.BluetoothManager;
import com.tapwithus.sdk.bluetooth.TapBluetoothManager;

public class TapSdkFactory {

    private static TapSdk sdk;

    public static TapSdk getDefault(Context context) {
        if (sdk == null) {
            BluetoothManager bluetoothManager = new BluetoothManager(context.getApplicationContext(), BluetoothAdapter.getDefaultAdapter());
            TapBluetoothManager tapBluetoothManager = new TapBluetoothManager(bluetoothManager);
            sdk = new TapSdk(tapBluetoothManager);
        }
        return sdk;
    }
}
