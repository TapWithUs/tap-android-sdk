package com.tapwithus.sdk.mode;

import java.util.Arrays;

public class TapXRState {

    public static final int NONE = 0;
    public static final int USER_CONTROL = 1;
    public static final int TAPPING = 2;
    public static final int AIR_MOUSE = 4;

    public static final int[] ALL_STATES = new int[] { NONE, USER_CONTROL, TAPPING, AIR_MOUSE };

    private static final byte[] STATE_NONE_DATA = new byte[] { };
    private static final byte[] STATE_USER_CONTROL_DATA = new byte[] { 0x3, 0xd, 0x0, 0x3};
    private static final byte[] STATE_TAPPING_DATA = new byte[] { 0x3, 0xd, 0x0, 0x2 };
    private static final byte[] STATE_AIR_MOUSE_DATA = new byte[] { 0x3, 0xd, 0x0, 0x1};

    public int type;

    private byte[] modeData;

    private TapXRState (int type) {
        this.type = type;
        switch (type) {
            case NONE: this.modeData = STATE_NONE_DATA;
                break;
            case USER_CONTROL: this.modeData = STATE_USER_CONTROL_DATA;
                break;
            case TAPPING : this.modeData = STATE_TAPPING_DATA;
                break;
            case AIR_MOUSE: this.modeData = STATE_AIR_MOUSE_DATA;
                break;
            default : this.modeData = STATE_NONE_DATA;
        }
    }

    public boolean isValid() {
        return Arrays.binarySearch(TapXRState.ALL_STATES, this.type) >= 0;
    }

    public static TapXRState none() {
        return new TapXRState(NONE);
    }
    public static TapXRState userControl() {
        return new TapXRState(USER_CONTROL);
    }
    public static TapXRState tapping() {
        return new TapXRState(TAPPING);
    }
    public static TapXRState airMouse() {
        return new TapXRState(AIR_MOUSE);
    }

    public byte[] getBytes() {
        return this.modeData;
    }

}
