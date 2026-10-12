package com.tapwithus.sdk.xr;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

/**
 * Turns a stream of legacy XR gesture states and cursor samples into
 * press, release, and fist actions.
 */
final class MouseEvents {

    interface Delegate {
        void mouseEventsAction(MouseEventsAction action);
    }

    private XRGestureState prevGesture = XRGestureState.NONE;
    private int gestureDuration;
    private final FixedWindow<double[]> directionAggregator = new FixedWindow<>(20);
    private final FixedWindow<Double> motionAggregator = new FixedWindow<>(10);
    private double vy;
    @Nullable
    Delegate delegate;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    void resetGestures() {
        prevGesture = XRGestureState.NONE;
    }

    void putCursor(int vx, int vy) {
        this.vy = vy;
        directionAggregator.add(new double[] { vx, vy });
        motionAggregator.add(norm(vx, vy));
    }

    void putClick(int gesture) {
        XRGestureState state = XRGestureState.fromCode(gesture);
        if (state == null) {
            return;
        }
        gestureDuration = gestureDuration + 1;
        if (prevGesture != state) {
            if (state == XRGestureState.NONE) {
                dispatchAction(MouseEventsAction.release());
            }
            if (prevGesture == XRGestureState.NONE || state == XRGestureState.NONE) {
                gestureDuration = 0;
                prevGesture = state;
            }
        }
        if (gestureDuration == 1 && prevGesture == XRGestureState.THUMB_INDEX) {
            dispatchAction(MouseEventsAction.press(MouseEventFinger.INDEX));
        }
        if (gestureDuration == 1 && prevGesture == XRGestureState.THUMB_MIDDLE) {
            dispatchAction(MouseEventsAction.press(MouseEventFinger.MIDDLE));
        }
        if (gestureDuration == 1 && prevGesture == XRGestureState.THUMB_RING) {
            dispatchAction(MouseEventsAction.press(MouseEventFinger.RING));
        }
        if (gestureDuration == 1 && prevGesture == XRGestureState.THUMB_PINKY) {
            dispatchAction(MouseEventsAction.press(MouseEventFinger.PINKY));
        }
        if (gestureDuration == 1 && prevGesture == XRGestureState.FIST) {
            dispatchAction(MouseEventsAction.fist());
        }
    }

    private void dispatchAction(final MouseEventsAction action) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (delegate != null) {
                    delegate.mouseEventsAction(action);
                }
            }
        });
    }

    private static double norm(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }
}
