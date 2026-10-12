package com.tapwithus.sdk.v2;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.tapwithus.sdk.mode.TapInputMode;
import com.tapwithus.sdk.mode.TapXRState;

import org.junit.Test;

import java.util.List;

/**
 * Mode-to-feature mapping tests against tap-ios-sdk's
 * {@code TAPV2InputModeMapper.swift}.
 */
public class TapV2InputModeMapperTest {

    @Test
    public void textModeDisablesAllFeatures() {
        List<byte[]> commands = TapV2InputModeMapper.commands(TapInputMode.text(), null);
        assertEquals(3, commands.size());
        assertArrayEquals(TapV2Encoder.encodeSetFeature(DeviceFeature.MODEL_DETECTION, false), commands.get(0));
        assertArrayEquals(TapV2Encoder.encodeSetFeature(DeviceFeature.IMU_MOTION_DATA, false), commands.get(1));
        assertArrayEquals(TapV2Encoder.encodeSetFeature(DeviceFeature.RAW_IMU_DATA, false), commands.get(2));
    }

    @Test
    public void controllerModeEnablesDetectionAndMotion() {
        List<byte[]> commands = TapV2InputModeMapper.commands(TapInputMode.controller(), null);
        assertEquals(3, commands.size());
        assertArrayEquals(TapV2Encoder.encodeSetFeature(DeviceFeature.MODEL_DETECTION, true), commands.get(0));
        assertArrayEquals(TapV2Encoder.encodeSetFeature(DeviceFeature.IMU_MOTION_DATA, true), commands.get(1));
        assertArrayEquals(TapV2Encoder.encodeSetFeature(DeviceFeature.RAW_IMU_DATA, false), commands.get(2));
    }

    @Test
    public void controllerModeWithAirMouseStateConfiguresVision() {
        List<byte[]> commands = TapV2InputModeMapper.commands(TapInputMode.controller(), TapXRState.airMouse());
        assertEquals(5, commands.size());
        assertArrayEquals(TapV2Encoder.encodeSetVisionSensorModel(VisionSensorModel.AIR_GESTURE), commands.get(3));
        assertArrayEquals(TapV2Encoder.encodeSetVisionSensorOpMode(VisionSensorOpMode.STREAM), commands.get(4));
    }

    @Test
    public void controllerModeWithTappingStateConfiguresVision() {
        List<byte[]> commands = TapV2InputModeMapper.commands(TapInputMode.controller(), TapXRState.tapping());
        assertEquals(5, commands.size());
        assertArrayEquals(TapV2Encoder.encodeSetVisionSensorModel(VisionSensorModel.TAPPING), commands.get(3));
        assertArrayEquals(TapV2Encoder.encodeSetVisionSensorOpMode(VisionSensorOpMode.TRIGGER), commands.get(4));
    }

    @Test
    public void rawSensorModeEnablesRawImuWithSensitivity() {
        List<byte[]> commands = TapV2InputModeMapper.commands(
                TapInputMode.rawSensorData((byte) 0, (byte) 3, (byte) 2), null);
        assertEquals(4, commands.size());
        assertArrayEquals(TapV2Encoder.encodeSetFeature(DeviceFeature.RAW_IMU_DATA, true), commands.get(0));
        assertArrayEquals(TapV2Encoder.encodeSetFeature(DeviceFeature.MODEL_DETECTION, false), commands.get(1));
        assertArrayEquals(TapV2Encoder.encodeSetFeature(DeviceFeature.IMU_MOTION_DATA, false), commands.get(2));
        assertArrayEquals(TapV2Encoder.encodeSetImuSensitivity(3, 2), commands.get(3));
    }
}
