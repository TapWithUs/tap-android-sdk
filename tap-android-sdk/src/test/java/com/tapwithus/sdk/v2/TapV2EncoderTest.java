package com.tapwithus.sdk.v2;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/**
 * Wire-format tests against the tap-python-sdk reference implementation
 * ({@code encoder.py}).
 */
public class TapV2EncoderTest {

    @Test
    public void setFeature() {
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 1, 1 },
                TapV2Encoder.encodeSetFeature(DeviceFeature.MODEL_DETECTION, true));
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0 },
                TapV2Encoder.encodeSetFeature(DeviceFeature.RAW_IMU_DATA, false));
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 4, 1 },
                TapV2Encoder.encodeSetFeature(DeviceFeature.STANDBY_GESTURE_DETECTION, true));
    }

    @Test
    public void getFeature() {
        assertArrayEquals(new byte[] { 0, 1, 0, 0, 2 },
                TapV2Encoder.encodeGetFeature(DeviceFeature.IMU_MOTION_DATA));
    }

    @Test
    public void visionSensorOpMode() {
        assertArrayEquals(new byte[] { 1, 0, 0, 0, 2 },
                TapV2Encoder.encodeSetVisionSensorOpMode(VisionSensorOpMode.STREAM));
        assertArrayEquals(new byte[] { 1, 0, 0, 0, 0 },
                TapV2Encoder.encodeSetVisionSensorOpMode(VisionSensorOpMode.TRIGGER));
        assertArrayEquals(new byte[] { 1, 0, 10, 0 },
                TapV2Encoder.encodeGetVisionSensorOpMode());
    }

    @Test
    public void visionSensorModel() {
        assertArrayEquals(new byte[] { 1, 0, 1, 0, 1 },
                TapV2Encoder.encodeSetVisionSensorModel(VisionSensorModel.AIR_GESTURE));
        assertArrayEquals(new byte[] { 1, 0, 1, 0, 0 },
                TapV2Encoder.encodeSetVisionSensorModel(VisionSensorModel.TAPPING));
        assertArrayEquals(new byte[] { 1, 0, 11, 0 },
                TapV2Encoder.encodeGetVisionSensorModel());
    }

    @Test
    public void imuSensitivity() {
        // Wire order is [gyro, accelerometer]
        assertArrayEquals(new byte[] { 1, 1, 2, 0, 3, 2 },
                TapV2Encoder.encodeSetImuSensitivity(3, 2));
        assertArrayEquals(new byte[] { 1, 1, 12, 0 },
                TapV2Encoder.encodeGetImuSensitivity());
    }

    @Test
    public void hapticPattern() {
        byte[] expected = new byte[4 + 2 + 18];
        expected[0] = 1;  // PERIPHERAL_COMMAND
        expected[1] = 2;  // HAPTIC
        expected[2] = 3;  // SET_HAPTIC_PATTERN
        expected[3] = 0;
        expected[4] = 0;  // HAPTIC_UI_PERIPHERAL_TYPE
        expected[5] = 2;  // HAPTIC_UI_ACTION_CONSTANT_POWER_SEQUENCE
        expected[6] = 100;
        expected[7] = 50;
        assertArrayEquals(expected, TapV2Encoder.encodeSetHapticPattern(new int[] { 100, 50 }));
    }

    @Test
    public void hapticPatternClampsAndTruncates() {
        int[] durations = new int[20];
        for (int i = 0; i < durations.length; i++) {
            durations[i] = 300; // clamped to 255
        }
        byte[] frame = TapV2Encoder.encodeSetHapticPattern(durations);
        // 4-byte header + 2-byte UI header + 18 slots, extra durations dropped
        org.junit.Assert.assertEquals(24, frame.length);
        for (int i = 6; i < 24; i++) {
            org.junit.Assert.assertEquals((byte) 255, frame[i]);
        }
    }

    @Test
    public void keepAlive() {
        assertArrayEquals(new byte[] { 2, 0, 0, 0 }, TapV2Encoder.encodeKeepAlive());
    }

    @Test
    public void standbyState() {
        assertArrayEquals(new byte[] { 3, 4, 0, 0, 1 }, TapV2Encoder.encodeStandbyStateSet(true));
        assertArrayEquals(new byte[] { 3, 4, 0, 0, 0 }, TapV2Encoder.encodeStandbyStateSet(false));
        assertArrayEquals(new byte[] { 3, 3, 0, 0 }, TapV2Encoder.encodeStandbyStateGet());
    }
}
