package com.tapwithus.sdk.v2;

/**
 * IMU sensitivity configuration of a V2 Tap device.
 */
public class ImuSensitivity {

    public static final int GYRO_MIN = 0;
    public static final int GYRO_MAX = 5;
    public static final int ACCELEROMETER_MIN = 0;
    public static final int ACCELEROMETER_MAX = 4;

    private final int gyro;
    private final int accelerometer;

    public ImuSensitivity(int gyro, int accelerometer) {
        this.gyro = clamp(gyro, GYRO_MIN, GYRO_MAX);
        this.accelerometer = clamp(accelerometer, ACCELEROMETER_MIN, ACCELEROMETER_MAX);
    }

    public int getGyro() {
        return gyro;
    }

    public int getAccelerometer() {
        return accelerometer;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return "ImuSensitivity{gyro=" + gyro + ", accelerometer=" + accelerometer + '}';
    }
}
