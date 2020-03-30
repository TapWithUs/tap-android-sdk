package com.tapwithus.sdk.mode;


import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import android.util.Log;


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

        while (offset + length - 1 < data.length) {
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
                Log.d("RawSensorData", "Point is Null!");
                return null;
            }
            onGyroSensitivity = false;
            offset = offset + 6;
        }
        if (dataType == DataType.IMU && points.size() != 2) {
            return null;
        } else if (dataType == DataType.Device && points.size() != 5) {
            return null;
        }
        return new RawSensorData(timestamp, dataType, points.toArray(new Point3[0]));
    }

    public String toString() {
        String typeString = this.dataType == DataType.IMU ? "IMU" : "DEVICE";
        String pointsString = "";
        for (int i=0; i < this.points.length; i++) {
            pointsString = pointsString + this.points[i].toString();
            if (i < this.points.length -1) {
                pointsString = pointsString + ", ";
            }
        }
        return "timestamp: " + this.timestamp + ", type: " + typeString + ", points: " + pointsString;
    }

    public String rawString(String delimeter) {
        String typeString = "IMU";
        if (this.dataType == DataType.Device) {
            typeString = "DEVICE";
        }

        String pointsString = "";
        for (int i=0; i<this.points.length; i++) {
            pointsString = pointsString + this.points[i].rawString(delimeter);
            if (i < this.points.length-1) {
                pointsString = pointsString + delimeter;
            }

        }
        return this.timestamp + delimeter + typeString + delimeter + pointsString;
    }

    public Point3 getPoint(int index) {
        if (this.points.length > 0 && index >= 0 && index < this.points.length) {
            return this.points[index];
        } else {
            return null;
        }
    }

}
