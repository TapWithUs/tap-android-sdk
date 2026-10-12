package com.tapwithus.sdk.xr;

import androidx.annotation.Nullable;

/**
 * Legacy XR air-gesture states consumed by {@link XRGesturesMain}.
 */
public enum XRGestureState {
    NONE(100),
    THUMB_INDEX(101),
    THUMB_MIDDLE(102),
    THUMB_RING(103),
    THUMB_PINKY(104),
    FIST(105);

    public final int code;

    XRGestureState(int code) {
        this.code = code;
    }

    @Nullable
    public static XRGestureState fromCode(int code) {
        for (XRGestureState state : values()) {
            if (state.code == code) {
                return state;
            }
        }
        return null;
    }
}
