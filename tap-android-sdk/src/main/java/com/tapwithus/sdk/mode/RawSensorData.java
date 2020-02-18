package com.tapwithus.sdk.mode;


import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

public class RawSensorData {

    public enum DataType  {
        IMU,
        Device
    }


    public static final int iIMU_GYRO = 0;
    public static final int iIMU_ACCELEROMETER = 1;
    public static final int iDEV_THUMB = 0;
    public static final int iDEV_INDEX = 1;
    public static final int iDEV_MIDDLE = 2;
    public static final int iDEV_RING = 3;
    public static final int iDEV_PINKY = 4;

    public int timestamp;
    public DataType dataType;
    public Point3[] points;

    private RawSensorData(int timestamp, DataType dataType, Point3[] points) {
        this.timestamp = timestamp;
        this.dataType = dataType;
        this.points = Arrays.copyOf(points, points.length);
    }

    @Nullable
    public static RawSensorData make(DataType dataType, int timestamp, byte[] data, byte deviceAccelSens, byte imuGyroSens, byte imuAccelSens) {
        ArrayList<Point3> points = new ArrayList<>();
        int offset = 0;
        int length = 6;
        boolean onGyroSensitivity = dataType == DataType.IMU;

        while (offset < data.length) {
            if (offset + length < data.length) {
                double sens = 0;
                if (dataType == DataType.IMU) {
                    if (onGyroSensitivity) {
                        sens = SensorSensitivity.getImuGyroFactor(imuGyroSens);
                    } else {
                        sens = SensorSensitivity.getImuAccelFactor(imuAccelSens);
                    }
                } else {
                    sens = SensorSensitivity.getDeviceAccelFactor(deviceAccelSens);
                }

                if (sens == 0) {
                    return null;
                }

                Point3 point = Point3.make(Arrays.copyOfRange(data, offset, offset + length), sens);
                if (point != null) {
                    points.add(point);
                } else {
                    return null;
                }
                onGyroSensitivity = false;
            } else {
                return null;
            }
        }
        return new RawSensorData(timestamp, dataType, points.toArray(new Point3[0]));
    }




}
