package com.tapwithus.sdk.v2;

import androidx.annotation.Nullable;

/**
 * Independent feature toggles of V2 Tap devices (TapSDK2).
 *
 * V2 devices don't use "input modes" - instead, each data stream is enabled or
 * disabled independently. Matches {@code DeviceFeatures} in tap-python-sdk.
 */
public enum DeviceFeature {

    RAW_IMU_DATA(0),
    MODEL_DETECTION(1),
    IMU_MOTION_DATA(2),
    /** Reserved - not implemented by current firmware. */
    TRIGGER_DETECTIONS(3),
    STANDBY_GESTURE_DETECTION(4);

    private final int value;

    DeviceFeature(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Nullable
    public static DeviceFeature fromValue(int value) {
        for (DeviceFeature feature : values()) {
            if (feature.value == value) {
                return feature;
            }
        }
        return null;
    }
}
