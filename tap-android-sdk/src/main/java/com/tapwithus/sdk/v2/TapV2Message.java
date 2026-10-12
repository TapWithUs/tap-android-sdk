package com.tapwithus.sdk.v2;

import androidx.annotation.NonNull;

/**
 * A parsed inbound frame from a V2 Tap device.
 *
 * {@link #payload} holds the frame bytes after the 4-byte header.
 */
public class TapV2Message {

    public enum Type {
        IMU_MOTION,
        IMU_RAW,
        TAP_GESTURE,
        AIR_GESTURE,
        STANDBY_STATE,
        CONFIG_FEATURE,
        CONFIG_VISION_OP_MODE,
        CONFIG_VISION_MODEL,
        CONFIG_IMU_SENSITIVITY,
        CONFIG_HAPTIC_PATTERN
    }

    @NonNull
    public final Type type;

    @NonNull
    public final byte[] payload;

    TapV2Message(@NonNull Type type, @NonNull byte[] payload) {
        this.type = type;
        this.payload = payload;
    }
}
