package com.tapwithus.sdk.xr;

/**
 * Hardware 4.x.x V2 taps report the new class indexes. {@link XRGesturesMain}
 * expects the legacy {@link XRGestureState} codes.
 */
public final class LegacyXRAirGesture {

    private LegacyXRAirGesture() {
    }

    public static int fromV2Class(int value) {
        switch (value) {
            case 100: return XRGestureState.NONE.code;
            case 105: return XRGestureState.THUMB_INDEX.code;
            case 106: return XRGestureState.THUMB_MIDDLE.code;
            case 107: return XRGestureState.THUMB_RING.code;
            case 108: return XRGestureState.THUMB_PINKY.code;
            case 109: return XRGestureState.FIST.code;
            default: return value;
        }
    }
}
