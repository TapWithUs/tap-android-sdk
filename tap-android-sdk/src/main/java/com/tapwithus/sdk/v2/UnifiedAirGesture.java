package com.tapwithus.sdk.v2;

import androidx.annotation.Nullable;

/**
 * Air gesture codes reported by V2 Tap devices through model detection.
 *
 * These arrive via {@code TapListener.onAirMouseInputReceived} - the gesture
 * value of the packet holds one of these codes when the connected device is a
 * V2 device. Matches {@code UnifiedAirGestures} in tap-python-sdk.
 */
public enum UnifiedAirGesture {

    NONE(100),
    LEFT(101),
    RIGHT(102),
    UP(103),
    DOWN(104),
    AB(105),
    AC(106),
    AD(107),
    AE(108),
    FIST(109),
    AB_HOLD(110),
    AC_HOLD(111),
    AD_HOLD(112),
    AE_HOLD(113),
    FIST_HOLD(114);

    private final int code;

    UnifiedAirGesture(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Nullable
    public static UnifiedAirGesture fromCode(int code) {
        for (UnifiedAirGesture gesture : values()) {
            if (gesture.code == code) {
                return gesture;
            }
        }
        return null;
    }
}
