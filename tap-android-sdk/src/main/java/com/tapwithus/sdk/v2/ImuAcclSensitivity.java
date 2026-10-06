package com.tapwithus.sdk.v2;

import androidx.annotation.Nullable;

/**
 * Thumb IMU accelerometer full-scale range for V2 devices.
 *
 * Wire values match {@code ImuAcclSensitivity} in tap-python-sdk (1-based).
 * {@code setImuSensitivity} sends {@link #getValue()} as the second sensitivity byte.
 */
public enum ImuAcclSensitivity {

    G2(1),
    G4(2),
    G8(3),
    G16(4);

    private final int value;

    ImuAcclSensitivity(int value) {
        this.value = value;
    }

    /** Value written to the device. Same integer as the Python enum. */
    public int getValue() {
        return value;
    }

    @Nullable
    public static ImuAcclSensitivity fromValue(int value) {
        for (ImuAcclSensitivity sensitivity : values()) {
            if (sensitivity.value == value) {
                return sensitivity;
            }
        }
        return null;
    }
}
