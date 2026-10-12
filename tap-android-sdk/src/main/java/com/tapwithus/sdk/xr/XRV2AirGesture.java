package com.tapwithus.sdk.xr;

import androidx.annotation.Nullable;

/**
 * Intermediate V2 gesture produced by {@link GesturePostprocessor}
 * before conversion to {@link XRAirGesture}.
 */
enum XRV2AirGesture {
    NONE(0),
    CLICK_INDEX(1),
    CLICK_MIDDLE(2),
    CLICK_RING(3),
    CLICK_PINKY(4),
    DRAG_INDEX(5),
    DRAG_MIDDLE(6),
    DRAG_RING(7),
    DRAG_PINKY(8),
    SWIPE_LEFT(17),
    SWIPE_RIGHT(18),
    SWIPE_UP(19),
    SWIPE_DOWN(20),
    FIST(30);

    final int code;

    XRV2AirGesture(int code) {
        this.code = code;
    }

    @Nullable
    static XRV2AirGesture fromCode(int code) {
        for (XRV2AirGesture gesture : values()) {
            if (gesture.code == code) {
                return gesture;
            }
        }
        return null;
    }

    boolean isDrag() {
        return this == DRAG_INDEX || this == DRAG_MIDDLE || this == DRAG_RING || this == DRAG_PINKY;
    }
}
