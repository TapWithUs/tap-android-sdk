package com.tapwithus.sdk.v2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * Parser for inbound framed messages of the TAP V2 protocol.
 *
 * Every inbound message is a 4-byte header (cmd, subcmd1, subcmd2, subcmd3)
 * followed by a payload. Matches tap-python-sdk's {@code parsers.tap_inc_msg}.
 */
public class TapV2Parser {

    public static final int METADATA_SIZE_BYTES = 4;

    // Inbound command types
    public static final int CMD_IMU_DATA = 0;
    public static final int CMD_MODEL_DETECTION = 1;
    public static final int CMD_STANDBY_STATE = 2;
    public static final int CMD_CONFIG_STATE = 3;

    // Inbound sub-command 1 for IMU_DATA / MODEL_DETECTION
    public static final int SUBCMD1_IMU_MOTION_DATA = 0;
    public static final int SUBCMD1_IMU_RAW_DATA = 1;
    public static final int SUBCMD1_TAP_GESTURE = 2;
    public static final int SUBCMD1_AIR_GESTURE = 3;

    // Inbound sub-command 1 for CONFIG_STATE
    public static final int CONFIG_FEATURE = 0;
    public static final int CONFIG_VISION_OP_MODE = 1;
    public static final int CONFIG_VISION_MODEL = 2;
    public static final int CONFIG_IMU_SENSITIVITY = 3;
    public static final int CONFIG_HAPTIC_PATTERN = 4;

    private TapV2Parser() { }

    /**
     * @return the parsed message, or null if the frame is unknown or malformed
     */
    @Nullable
    public static TapV2Message parse(@NonNull byte[] data) {
        if (data.length < METADATA_SIZE_BYTES) {
            return null;
        }

        int cmd = data[0] & 0xFF;
        int subcmd1 = data[1] & 0xFF;
        byte[] payload = Arrays.copyOfRange(data, METADATA_SIZE_BYTES, data.length);

        switch (cmd) {
            case CMD_IMU_DATA:
                if (subcmd1 == SUBCMD1_IMU_MOTION_DATA) {
                    return validated(TapV2Message.Type.IMU_MOTION, payload, 10);
                } else if (subcmd1 == SUBCMD1_IMU_RAW_DATA) {
                    return new TapV2Message(TapV2Message.Type.IMU_RAW, payload);
                }
                return null;
            case CMD_MODEL_DETECTION:
                if (subcmd1 == SUBCMD1_TAP_GESTURE) {
                    return validated(TapV2Message.Type.TAP_GESTURE, payload, 1);
                } else if (subcmd1 == SUBCMD1_AIR_GESTURE) {
                    return validated(TapV2Message.Type.AIR_GESTURE, payload, 1);
                }
                return null;
            case CMD_STANDBY_STATE:
                return validated(TapV2Message.Type.STANDBY_STATE, payload, 1);
            case CMD_CONFIG_STATE:
                switch (subcmd1) {
                    case CONFIG_FEATURE:
                        return validated(TapV2Message.Type.CONFIG_FEATURE, payload, 2);
                    case CONFIG_VISION_OP_MODE:
                        return validated(TapV2Message.Type.CONFIG_VISION_OP_MODE, payload, 1);
                    case CONFIG_VISION_MODEL:
                        return validated(TapV2Message.Type.CONFIG_VISION_MODEL, payload, 1);
                    case CONFIG_IMU_SENSITIVITY:
                        return validated(TapV2Message.Type.CONFIG_IMU_SENSITIVITY, payload, 2);
                    case CONFIG_HAPTIC_PATTERN:
                        return new TapV2Message(TapV2Message.Type.CONFIG_HAPTIC_PATTERN, payload);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    @Nullable
    private static TapV2Message validated(@NonNull TapV2Message.Type type, @NonNull byte[] payload, int minLength) {
        if (payload.length < minLength) {
            return null;
        }
        return new TapV2Message(type, payload);
    }
}
