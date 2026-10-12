package com.tapwithus.sdk.xr;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Converts one step of a V2 gesture stream into the public {@link XRAirGesture} values.
 * Pass {@link XRV2AirGesture#NONE} as {@code previous} on the first frame.
 */
final class XRV2AirGestureConverter {

    private XRV2AirGestureConverter() {
    }

    @NonNull
    static List<XRAirGesture> convert(@NonNull XRV2AirGesture current, @NonNull XRV2AirGesture previous) {
        switch (current) {
            case NONE:
                if (previous == XRV2AirGesture.FIST) {
                    return Collections.singletonList(XRAirGesture.FIST_END);
                }
                if (previous.isDrag()) {
                    return Collections.singletonList(XRAirGesture.DROP);
                }
                return Collections.emptyList();
            case FIST:
                if (previous != XRV2AirGesture.FIST) {
                    return Collections.singletonList(XRAirGesture.FIST_BEGIN);
                }
                return Collections.emptyList();
            case CLICK_INDEX:
                return Collections.singletonList(XRAirGesture.CLICK_INDEX);
            case CLICK_MIDDLE:
                return Collections.singletonList(XRAirGesture.CLICK_MIDDLE);
            case CLICK_RING:
                return Collections.singletonList(XRAirGesture.CLICK_RING);
            case CLICK_PINKY:
                return Collections.singletonList(XRAirGesture.CLICK_PINKY);
            case DRAG_INDEX:
                if (previous != XRV2AirGesture.DRAG_INDEX) {
                    return Collections.singletonList(XRAirGesture.DRAG_INDEX);
                }
                return Collections.emptyList();
            case DRAG_MIDDLE:
                if (previous != XRV2AirGesture.DRAG_MIDDLE) {
                    return Collections.singletonList(XRAirGesture.DRAG_MIDDLE);
                }
                return Collections.emptyList();
            case DRAG_RING:
                if (previous != XRV2AirGesture.DRAG_RING) {
                    return Collections.singletonList(XRAirGesture.DRAG_RING);
                }
                return Collections.emptyList();
            case DRAG_PINKY:
                if (previous != XRV2AirGesture.DRAG_PINKY) {
                    return Collections.singletonList(XRAirGesture.DRAG_PINKY);
                }
                return Collections.emptyList();
            case SWIPE_LEFT:
            case SWIPE_RIGHT:
            case SWIPE_UP:
            case SWIPE_DOWN:
                XRAirGesture swipe = XRAirGestureFromCode(current.code);
                if (swipe != null) {
                    return Collections.singletonList(swipe);
                }
                return Collections.emptyList();
            default:
                return Collections.emptyList();
        }
    }

    @androidx.annotation.Nullable
    private static XRAirGesture XRAirGestureFromCode(int code) {
        for (XRAirGesture gesture : XRAirGesture.values()) {
            if (gesture.code == code) {
                return gesture;
            }
        }
        return null;
    }
}
