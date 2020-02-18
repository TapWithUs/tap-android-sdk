package com.tapwithus.sdk.mode;

import java.util.Arrays;



public class TapInputMode {

    public static final int TEXT = 1;
    public static final int CONTROLLER = 2;
    public static final int CONTROLLER_WITH_MOUSEHID = 3;
    public static final int RAW_SENSOR = 4;
    public static final int[] ALL_MODES = new int[] { TEXT, CONTROLLER, CONTROLLER_WITH_MOUSEHID, RAW_SENSOR};


    private static final byte[] CONTROLLER_MODE_DATA = new byte[] { 0x3, 0xc, 0x0, 0x1 };
    private static final byte[] CONTROLLER_MODE_WITH_MOUSEHID_DATA = new byte[] { 0x3, 0xc, 0x0, 0x4};
    private static final byte[] TEXT_MODE_DATA = new byte[] { 0x3, 0xc, 0x0, 0x0 };
    private static final byte[] RAW_SENSOR_MODE_DATA = new byte[] { 0x3, 0xc, 0x0, 0xa};

    public int type;

    private byte deviceAccelerometer =0 ;
    private byte imuGyro = 0;
    private byte imuAccelerometer = 0;

    private byte[] modeData;

    private TapInputMode (int type) {
        this.type = type;
        switch (type) {
            case TEXT : this.modeData = TEXT_MODE_DATA;
                        break;
            case CONTROLLER : this.modeData = CONTROLLER_MODE_DATA;
                        break;
            case CONTROLLER_WITH_MOUSEHID : this.modeData = CONTROLLER_MODE_WITH_MOUSEHID_DATA;
                        break;
            default : this.modeData = CONTROLLER_MODE_DATA;
        }
    }

    private TapInputMode(byte deviceAccelerometer, byte imuGyro, byte imuAccelerometer) {
        this.type = RAW_SENSOR;
        this.deviceAccelerometer = deviceAccelerometer;
        this.imuGyro = imuGyro;
        this.imuAccelerometer = imuAccelerometer;
        byte[] data1 = RAW_SENSOR_MODE_DATA;
        byte[] data2 = new byte[] { this.deviceAccelerometer, this.imuGyro, this.imuAccelerometer};
        this.modeData = Arrays.copyOf(data1, data1.length + data2.length);
        System.arraycopy(data2, 0, this.modeData, data1.length, data2.length);

    }

    public boolean isValid() {
        return Arrays.binarySearch(TapInputMode.ALL_MODES, this.type) >= 0;


    }

    public static TapInputMode controller() {
        return new TapInputMode(CONTROLLER);
    }

    public static TapInputMode text() {
        return new TapInputMode(TEXT);
    }

    public static TapInputMode controllerWithMouseHID() {
        return new TapInputMode(CONTROLLER_WITH_MOUSEHID);
    }

    public static TapInputMode rawSensorData(byte deviceAccelerometer, byte imuAccelerometer, byte imuGyro) {
        return new TapInputMode(
                SensorSensitivity.normDeviceAccel(deviceAccelerometer),
                SensorSensitivity.normImuAccel(imuAccelerometer),
                SensorSensitivity.normImuGyro(imuGyro));
    }

    public byte[] getBytes() {
        return this.modeData;
    }

}

