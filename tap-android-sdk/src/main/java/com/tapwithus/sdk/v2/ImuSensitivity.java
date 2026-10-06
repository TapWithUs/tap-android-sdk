package com.tapwithus.sdk.v2;

import androidx.annotation.Nullable;

/**
 * IMU sensitivity read back from a V2 Tap, or stored after a set.
 *
 * {@link #getGyro()} and {@link #getAccelerometer()} are the bytes on the wire
 * (gyro first). Named ranges use the same integers as tap-python-sdk:
 * gyro 1 = {@link ImuGyroSensitivity#DPS125} … 5 = {@link ImuGyroSensitivity#DPS2000},
 * accelerometer 1 = {@link ImuAcclSensitivity#G2} … 4 = {@link ImuAcclSensitivity#G16}.
 * {@code 0} is not a named range. {@link #getGyroSensitivity()} is null for it.
 */
public class ImuSensitivity {

    /**
     * Historical clamp used only by the deprecated int {@code setImuSensitivity} overload.
     * Not a sensitivity the device protocol names. Prefer {@link ImuGyroSensitivity}.
     */
    @Deprecated
    public static final int GYRO_MIN = 0;
    /** @deprecated See {@link #GYRO_MIN}. Python's highest gyro value is {@link ImuGyroSensitivity#DPS2000} (5). */
    @Deprecated
    public static final int GYRO_MAX = 5;
    /** @deprecated See {@link #GYRO_MIN}. Prefer {@link ImuAcclSensitivity}. */
    @Deprecated
    public static final int ACCELEROMETER_MIN = 0;
    /** @deprecated See {@link #GYRO_MIN}. Python's highest accelerometer value is {@link ImuAcclSensitivity#G16} (4). */
    @Deprecated
    public static final int ACCELEROMETER_MAX = 4;

    private final int gyro;
    private final int accelerometer;

    /**
     * @param gyro wire byte (Python {@code ImuGyroSensitivity} value, or 0 if unset)
     * @param accelerometer wire byte (Python {@code ImuAcclSensitivity} value, or 0 if unset)
     */
    public ImuSensitivity(int gyro, int accelerometer) {
        this.gyro = gyro;
        this.accelerometer = accelerometer;
    }

    /** Gyroscope wire byte. 1–5 match {@link ImuGyroSensitivity}. */
    public int getGyro() {
        return gyro;
    }

    /** Accelerometer wire byte. 1–4 match {@link ImuAcclSensitivity}. */
    public int getAccelerometer() {
        return accelerometer;
    }

    /** Named gyro range, or null when the wire byte is not one of 1–5. */
    @Nullable
    public ImuGyroSensitivity getGyroSensitivity() {
        return ImuGyroSensitivity.fromValue(gyro);
    }

    /** Named accelerometer range, or null when the wire byte is not one of 1–4. */
    @Nullable
    public ImuAcclSensitivity getAccelerometerSensitivity() {
        return ImuAcclSensitivity.fromValue(accelerometer);
    }

    @Override
    public String toString() {
        return "ImuSensitivity{gyro=" + gyro + ", accelerometer=" + accelerometer + '}';
    }
}
