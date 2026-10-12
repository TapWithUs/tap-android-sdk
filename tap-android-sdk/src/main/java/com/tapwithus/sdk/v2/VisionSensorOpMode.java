package com.tapwithus.sdk.v2;

import androidx.annotation.Nullable;

/**
 * Operation mode of the vision sensor on V2 Tap devices.
 * Matches {@code VisionSensorOpModes} in tap-python-sdk.
 */
public enum VisionSensorOpMode {

    TRIGGER(0),
    STREAM_ON_TRIGGER(1),
    STREAM(2);

    private final int value;

    VisionSensorOpMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Nullable
    public static VisionSensorOpMode fromValue(int value) {
        for (VisionSensorOpMode mode : values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        return null;
    }
}
