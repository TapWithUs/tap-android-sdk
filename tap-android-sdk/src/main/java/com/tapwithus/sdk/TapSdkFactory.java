package com.tapwithus.sdk;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

public class TapSdkFactory {

    private static TapSdk sdk;

    public static TapSdk getDefault(Context context) {
        if (sdk == null) {
            Log.e("AAA", "Created new TAP SDK instance");
            sdk = new TapSdk(context.getApplicationContext(), BluetoothAdapter.getDefaultAdapter());
        }
        return sdk;
    }
}
