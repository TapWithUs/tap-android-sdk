package com.tapwithus.sdk.airmouse;

import com.tapwithus.sdk.bluetooth.Packet;

public class AirMousePacket extends Packet {

    public static final int AIR_MOUSE_GESTURE_NONE = 0;
    public static final int AIR_MOUSE_GESTURE_GENERAL = 1;
    public static final int AIR_MOUSE_GESTURE_UP = 2;
    public static final int AIR_MOUSE_GESTURE_UP_TWO_FINGERS = 3;
    public static final int AIR_MOUSE_GESTURE_DOWN = 4;
    public static final int AIR_MOUSE_GESTURE_DOWN_TWO_FINGERS = 5;
    public static final int AIR_MOUSE_GESTURE_LEFT = 6;
    public static final int AIR_MOUSE_GESTURE_LEFT_TWO_FINGERS = 7;
    public static final int AIR_MOUSE_GESTURE_RIGHT = 8;
    public static final int AIR_MOUSE_GESTURE_RIGHT_TWO_FINGERS = 9;

    public AirMousePacket(byte[] data) {
        super(data, 1);
    }

    public PacketValue gesture = new PacketValue(0, 8);
}
