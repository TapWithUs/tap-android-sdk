package com.tapwithus.sdk.v2;

import androidx.annotation.Nullable;

/**
 * Thumb IMU gyroscope full-scale range for V2 devices.
 *
 * Wire values match {@code ImuGyroSensitivity} in tap-python-sdk (1-based).
 * {@code setImuSensitivity} sends {@link #getValue()} as the first sensitivity byte.
 */
public enum ImuGyroSensitivity {

    DPS125(1),
    DPS250(2),
    DPS500(3),
    DPS1000(4),
    DPS2000(5);

    private final int value;

    ImuGyroSensitivity(int value) {
        this.value = value;
    }

    /** Value written to the device. Same integer as the Python enum. */
    public int getValue() {
        return value;
    }

    @Nullable
    public static ImuGyroSensitivity fromValue(int value) {
        for (ImuGyroSensitivity sensitivity : values()) {
            if (sensitivity.value == value) {
                return sensitivity;
            }
        }
        return null;
    }
}
