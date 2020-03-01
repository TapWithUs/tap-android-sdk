package com.tapwithus.sdk.mode;

public class SensorSensitivity {


    private static final double[] imuAccelSF = new double[] { 0.122, 0.061, 0.122, 0.244, 0.488 };
    private static final double[] imuGyroSF = new double[] { 17.5, 4.375, 8.75, 17.5, 35, 70 };
    private static final double[] devAccelSF = new double[] { 31.25, 3.90625, 7.8125, 15.625, 31.25 };

    private SensorSensitivity() {

    }

    private static byte norm(byte sens, int length) {
        return (sens >= 0 && sens < (byte)length) ? sens : 0;
    }

    public static byte normDeviceAccel(byte sens) {
        return norm(sens, devAccelSF.length);
    }

    public static byte normImuAccel(byte sens) {
        return norm(sens, imuAccelSF.length);
    }

    public static byte normImuGyro(byte sens) {
        return norm(sens, imuGyroSF.length);
    }

    public static double getDeviceAccelFactor(byte sens) {
        return devAccelSF[(int)norm(sens, devAccelSF.length)];
    }

    public static double getImuGyroFactor(byte sens) {
        return imuGyroSF[(int)norm(sens, imuGyroSF.length)];
    }

    public static double getImuAccelFactor(byte sens) {
        return imuAccelSF[(int)norm(sens, imuAccelSF.length)];
    }

//
//    public SensorSensitivity(int deviceAccelerometer,)
}
