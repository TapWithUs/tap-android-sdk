package com.tapwithus.sdk.bluetooth;

public class ErrorStrings {
    public static final String NO_SERVICE = "Requested service is not offered by the remote device";
    public static final String NO_CHARACTERISTIC = "No characteristic with the given UUID was found";
    public static final String NO_DESCRIPTOR = "No descriptor with the given UUID was found";
    public static final String DISCOVER_SERVICES_OP_INIT_FAIL = "The remote service discovery has not been started";
    public static final String CHAR_OP_FAIL = "A characteristic operation failed";
    public static final String DESC_OP_FAIL = "A descriptor operation failed";
    public static final String READ_OP_INIT_FAIL = "The read operation was not initiated successfully";
    public static final String WRITE_OP_INIT_FAIL = "The write operation was not initiated successfully";
    public static final String NOTIFY_OP_INIT_FAIL = "The requested notification status was not set successfully";
    public static final String VALUE_STORE_FAIL = "The requested value could not be stored locally";
    public static final String NOTIFY_TYPE_FAIL = "Failed to set notification type";
    public static final String GATT_CALLBACK_MISMATCH = "GATT callback characteristic or descriptor mismatch";
    public static final String OP_BUNDLE_EMPTY_EXEC = "Cannot execute empty operation in bundle";
    public static final String UNPAIR_OP_INIT_FAIL = "Remove bond (pairing) with the remote device failed";
    public static final String REFRESH_OP_INIT_FAIL = "Failed clearing the internal cache of the remote device";
    public static final String LACKING_PERMISSION_FAIL = "Operation prevented by not having permissions (Android 12)";
    public static final String GATT_FAILURE = "GATT operation failure";

    public static final String BLUETOOTH_OFF = "Bluetooth is turned OFF";
    public static final String BLUETOOTH_NOT_SUPPORTED = "Bluetooth is not supported on this hardware platform";
    public static final String PAIRED_DEVICES = "Unable to retrieve paired devices";
    public static final String SET_NOTIFICATION = "Device is not connected to set notification";
    public static final String READ_CHAR = "Device is not connected to read characteristic";
    public static final String WRITE_CHAR = "Device is not connected to write characteristic";
}
