package com.tapwithus.sdk.v2;

import androidx.annotation.Nullable;

/**
 * Detection model running on the vision sensor of V2 Tap devices.
 * Matches {@code ModelTypes} in tap-python-sdk.
 */
public enum VisionSensorModel {

    TAPPING(0),
    AIR_GESTURE(1);

    private final int value;

    VisionSensorModel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Nullable
    public static VisionSensorModel fromValue(int value) {
        for (VisionSensorModel model : values()) {
            if (model.value == value) {
                return model;
            }
        }
        return null;
    }
}
