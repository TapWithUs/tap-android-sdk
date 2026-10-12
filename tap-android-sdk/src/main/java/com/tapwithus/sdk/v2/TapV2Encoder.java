package com.tapwithus.sdk.v2;

import androidx.annotation.NonNull;

/**
 * Encoder for the framed TAP V2 protocol (TapSDK2).
 *
 * Every outbound message is a 4-byte header followed by an optional payload:
 * <pre>
 *     byte 0: command
 *     byte 1: sub-command 1
 *     byte 2: sub-command 2
 *     byte 3: sub-command 3
 *     byte 4+: payload
 * </pre>
 *
 * Wire format is identical to tap-python-sdk's {@code encoder.py} and
 * tap-ios-sdk's {@code TAPV2Encoder.swift}.
 */
public class TapV2Encoder {

    public static final int METADATA_SIZE_BYTES = 4;

    // Outbound command types
    public static final int CMD_FEATURE = 0;
    public static final int CMD_PERIPHERAL = 1;
    public static final int CMD_KEEPALIVE = 2;
    public static final int CMD_STANDBY_STATE = 3;

    // Sub-command 1
    public static final int SUBCMD1_SET_FEATURE = 0;
    public static final int SUBCMD1_GET_FEATURE = 1;
    public static final int SUBCMD1_PERIPHERAL_VISION_SENSOR = 0;
    public static final int SUBCMD1_PERIPHERAL_IMU = 1;
    public static final int SUBCMD1_PERIPHERAL_HAPTIC = 2;
    public static final int SUBCMD1_STANDBY_STATE_GET = 3;
    public static final int SUBCMD1_STANDBY_STATE_SET = 4;

    // Sub-command 2
    public static final int SUBCMD2_SET_VISION_SENSOR_OP_MODE = 0;
    public static final int SUBCMD2_SET_VISION_SENSOR_MODEL = 1;
    public static final int SUBCMD2_SET_IMU_SENSITIVITY = 2;
    public static final int SUBCMD2_SET_HAPTIC_PATTERN = 3;
    public static final int SUBCMD2_GET_VISION_SENSOR_OP_MODE = 10;
    public static final int SUBCMD2_GET_VISION_SENSOR_MODEL = 11;
    public static final int SUBCMD2_GET_IMU_SENSITIVITY = 12;

    // UI-command body embedded in the SET haptic payload
    public static final int HAPTIC_UI_PERIPHERAL_TYPE = 0;
    public static final int HAPTIC_UI_ACTION_CONSTANT_POWER_SEQUENCE = 2;
    public static final int HAPTIC_UI_DURATION_SLOT_COUNT = 18;

    private TapV2Encoder() { }

    @NonNull
    public static byte[] encodeMessage(int cmd, int subcmd1, int subcmd2, int subcmd3, @NonNull byte[] payload) {
        byte[] msg = new byte[METADATA_SIZE_BYTES + payload.length];
        msg[0] = (byte) cmd;
        msg[1] = (byte) subcmd1;
        msg[2] = (byte) subcmd2;
        msg[3] = (byte) subcmd3;
        System.arraycopy(payload, 0, msg, METADATA_SIZE_BYTES, payload.length);
        return msg;
    }

    @NonNull
    public static byte[] encodeSetFeature(@NonNull DeviceFeature feature, boolean enable) {
        byte[] payload = new byte[] { (byte) feature.getValue(), (byte) (enable ? 1 : 0) };
        return encodeMessage(CMD_FEATURE, SUBCMD1_SET_FEATURE, 0, 0, payload);
    }

    @NonNull
    public static byte[] encodeGetFeature(@NonNull DeviceFeature feature) {
        byte[] payload = new byte[] { (byte) feature.getValue() };
        return encodeMessage(CMD_FEATURE, SUBCMD1_GET_FEATURE, 0, 0, payload);
    }

    @NonNull
    public static byte[] encodeSetVisionSensorOpMode(@NonNull VisionSensorOpMode mode) {
        byte[] payload = new byte[] { (byte) mode.getValue() };
        return encodeMessage(CMD_PERIPHERAL, SUBCMD1_PERIPHERAL_VISION_SENSOR, SUBCMD2_SET_VISION_SENSOR_OP_MODE, 0, payload);
    }

    @NonNull
    public static byte[] encodeGetVisionSensorOpMode() {
        return encodeMessage(CMD_PERIPHERAL, SUBCMD1_PERIPHERAL_VISION_SENSOR, SUBCMD2_GET_VISION_SENSOR_OP_MODE, 0, new byte[0]);
    }

    @NonNull
    public static byte[] encodeSetVisionSensorModel(@NonNull VisionSensorModel model) {
        byte[] payload = new byte[] { (byte) model.getValue() };
        return encodeMessage(CMD_PERIPHERAL, SUBCMD1_PERIPHERAL_VISION_SENSOR, SUBCMD2_SET_VISION_SENSOR_MODEL, 0, payload);
    }

    @NonNull
    public static byte[] encodeGetVisionSensorModel() {
        return encodeMessage(CMD_PERIPHERAL, SUBCMD1_PERIPHERAL_VISION_SENSOR, SUBCMD2_GET_VISION_SENSOR_MODEL, 0, new byte[0]);
    }

    /**
     * @param gyroSensitivity gyroscope sensitivity index (0-5)
     * @param accelerometerSensitivity IMU accelerometer sensitivity index (0-4)
     */
    @NonNull
    public static byte[] encodeSetImuSensitivity(int gyroSensitivity, int accelerometerSensitivity) {
        // Wire order is [gyro, accelerometer], same as the python reference
        byte[] payload = new byte[] { (byte) gyroSensitivity, (byte) accelerometerSensitivity };
        return encodeMessage(CMD_PERIPHERAL, SUBCMD1_PERIPHERAL_IMU, SUBCMD2_SET_IMU_SENSITIVITY, 0, payload);
    }

    @NonNull
    public static byte[] encodeGetImuSensitivity() {
        return encodeMessage(CMD_PERIPHERAL, SUBCMD1_PERIPHERAL_IMU, SUBCMD2_GET_IMU_SENSITIVITY, 0, new byte[0]);
    }

    /**
     * @param scaledDurations vibration durations in 10ms units (host ms / 10), up to 18 slots
     */
    @NonNull
    public static byte[] encodeSetHapticPattern(@NonNull int[] scaledDurations) {
        byte[] payload = new byte[2 + HAPTIC_UI_DURATION_SLOT_COUNT];
        payload[0] = (byte) HAPTIC_UI_PERIPHERAL_TYPE;
        payload[1] = (byte) HAPTIC_UI_ACTION_CONSTANT_POWER_SEQUENCE;
        int count = Math.min(scaledDurations.length, HAPTIC_UI_DURATION_SLOT_COUNT);
        for (int i = 0; i < count; i++) {
            int v = Math.max(0, Math.min(255, scaledDurations[i]));
            payload[2 + i] = (byte) v;
        }
        return encodeMessage(CMD_PERIPHERAL, SUBCMD1_PERIPHERAL_HAPTIC, SUBCMD2_SET_HAPTIC_PATTERN, 0, payload);
    }

    @NonNull
    public static byte[] encodeKeepAlive() {
        return encodeMessage(CMD_KEEPALIVE, 0, 0, 0, new byte[0]);
    }

    @NonNull
    public static byte[] encodeStandbyStateSet(boolean standby) {
        byte[] payload = new byte[] { (byte) (standby ? 1 : 0) };
        return encodeMessage(CMD_STANDBY_STATE, SUBCMD1_STANDBY_STATE_SET, 0, 0, payload);
    }

    @NonNull
    public static byte[] encodeStandbyStateGet() {
        return encodeMessage(CMD_STANDBY_STATE, SUBCMD1_STANDBY_STATE_GET, 0, 0, new byte[0]);
    }
}
