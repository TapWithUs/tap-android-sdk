package com.tapwithus.sdk.xr;

import androidx.annotation.NonNull;

/**
 * Processed XR air gesture emitted after {@link GesturePostprocessor}
 * or {@link XRGesturesMain}.
 */
public enum XRAirGesture {
    NONE(0),
    CLICK_INDEX(1),
    CLICK_MIDDLE(2),
    CLICK_RING(3),
    CLICK_PINKY(4),
    DRAG_INDEX(5),
    DRAG_MIDDLE(6),
    DRAG_RING(7),
    DRAG_PINKY(8),
    DROP(9),
    POTENTIAL_DRAG_OR_CLICK_INDEX(10),
    POTENTIAL_DRAG_OR_CLICK_MIDDLE(11),
    POTENTIAL_DRAG_OR_CLICK_RING(12),
    POTENTIAL_DRAG_OR_CLICK_PINKY(13),
    FIST_BEGIN(14),
    FIST_END(15),
    SWIPE_LEFT(17),
    SWIPE_RIGHT(18),
    SWIPE_UP(19),
    SWIPE_DOWN(20);

    public final int code;

    XRAirGesture(int code) {
        this.code = code;
    }

    @NonNull
    public String descriptionString() {
        switch (this) {
            case NONE: return "None";
            case CLICK_INDEX: return "ClickIndex";
            case CLICK_MIDDLE: return "ClickMiddle";
            case CLICK_RING: return "ClickRing";
            case CLICK_PINKY: return "ClickPinky";
            case DRAG_INDEX: return "DragIndex";
            case DRAG_MIDDLE: return "DragMiddle";
            case DRAG_RING: return "DragRing";
            case DRAG_PINKY: return "DragPinky";
            case DROP: return "Drop";
            case POTENTIAL_DRAG_OR_CLICK_INDEX: return "PotentialDragOrClickIndex";
            case POTENTIAL_DRAG_OR_CLICK_MIDDLE: return "PotentialDragOrClickMiddle";
            case POTENTIAL_DRAG_OR_CLICK_RING: return "PotentialDragOrClickRing";
            case POTENTIAL_DRAG_OR_CLICK_PINKY: return "PotentialDragOrClickPinky";
            case FIST_BEGIN: return "FistBegin";
            case FIST_END: return "FistEnd";
            case SWIPE_LEFT: return "SwipeLeft";
            case SWIPE_RIGHT: return "SwipeRight";
            case SWIPE_UP: return "SwipeUp";
            case SWIPE_DOWN: return "SwipeDown";
            default: return name();
        }
    }
}
