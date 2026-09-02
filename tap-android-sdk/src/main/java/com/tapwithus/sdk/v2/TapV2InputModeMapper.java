package com.tapwithus.sdk.v2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tapwithus.sdk.mode.TapInputMode;
import com.tapwithus.sdk.mode.TapXRState;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates the classic v1 input mode / XR state API into V2 framed commands,
 * so the existing {@code TapSdk} mode API works transparently on V2 devices.
 *
 * Mirrors tap-ios-sdk's {@code TAPV2InputModeMapper.swift}.
 */
public class TapV2InputModeMapper {

    private TapV2InputModeMapper() { }

    /**
     * Builds the full V2 command sequence from scratch for the given input mode
     * and (optional) XR state.
     */
    @NonNull
    public static List<byte[]> commands(@NonNull TapInputMode mode, @Nullable TapXRState xrState) {
        switch (mode.type) {
            case TapInputMode.TEXT:
                return textModeCommands();
            case TapInputMode.RAW_SENSOR:
                return rawSensorModeCommands(mode);
            default:
                return controllerModeCommands(xrState);
        }
    }

    @NonNull
    private static List<byte[]> defaultFeatureCommands() {
        List<byte[]> commands = new ArrayList<>();
        commands.add(TapV2Encoder.encodeSetFeature(DeviceFeature.MODEL_DETECTION, true));
        commands.add(TapV2Encoder.encodeSetFeature(DeviceFeature.IMU_MOTION_DATA, true));
        commands.add(TapV2Encoder.encodeSetFeature(DeviceFeature.RAW_IMU_DATA, false));
        return commands;
    }

    @NonNull
    private static List<byte[]> textModeCommands() {
        List<byte[]> commands = new ArrayList<>();
        commands.add(TapV2Encoder.encodeSetFeature(DeviceFeature.MODEL_DETECTION, false));
        commands.add(TapV2Encoder.encodeSetFeature(DeviceFeature.IMU_MOTION_DATA, false));
        commands.add(TapV2Encoder.encodeSetFeature(DeviceFeature.RAW_IMU_DATA, false));
        return commands;
    }

    @NonNull
    private static List<byte[]> rawSensorModeCommands(@NonNull TapInputMode mode) {
        List<byte[]> commands = new ArrayList<>();
        commands.add(TapV2Encoder.encodeSetFeature(DeviceFeature.RAW_IMU_DATA, true));
        commands.add(TapV2Encoder.encodeSetFeature(DeviceFeature.MODEL_DETECTION, false));
        commands.add(TapV2Encoder.encodeSetFeature(DeviceFeature.IMU_MOTION_DATA, false));
        ImuSensitivity sensitivity = new ImuSensitivity(
                mode.getImuGyroSensitivity(),
                mode.getImuAccelerometerSensitivity());
        commands.add(TapV2Encoder.encodeSetImuSensitivity(
                sensitivity.getGyro(),
                sensitivity.getAccelerometer()));
        return commands;
    }

    @NonNull
    private static List<byte[]> controllerModeCommands(@Nullable TapXRState xrState) {
        List<byte[]> commands = defaultFeatureCommands();

        if (xrState == null) {
            return commands;
        }

        switch (xrState.type) {
            case TapXRState.AIR_MOUSE:
                commands.add(TapV2Encoder.encodeSetVisionSensorModel(VisionSensorModel.AIR_GESTURE));
                commands.add(TapV2Encoder.encodeSetVisionSensorOpMode(VisionSensorOpMode.STREAM));
                break;
            case TapXRState.TAPPING:
                commands.add(TapV2Encoder.encodeSetVisionSensorModel(VisionSensorModel.TAPPING));
                commands.add(TapV2Encoder.encodeSetVisionSensorOpMode(VisionSensorOpMode.TRIGGER));
                break;
            default:
                break;
        }

        return commands;
    }
}
