package com.tapwithus.sdk.xr;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

/**
 * Legacy XR air-gesture processor. Gesture states become clicks, drags, and fists.
 * Mouse deltas keep the cursor sample stream alive between reports.
 */
public final class XRGesturesMain implements MouseEvents.Delegate, MouseEventsInterpreter.Callbacks {

    public interface Listener {
        void onXRAirGesture(XRAirGesture gesture);
    }

    private static final long CURSOR_FILL_MS = 100;

    private final MouseEvents mouseEvents = new MouseEvents();
    private final MouseEventsInterpreter mouseEventsInterpreter = new MouseEventsInterpreter();
    private final MajorityVoting<Integer> clickMajorityVoting =
            new MajorityVoting<>(3, XRGestureState.NONE.code);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean useMajority;
    public boolean ignoreEventsUntilRelease;
    private boolean stopped;
    @Nullable
    private Runnable cursorFill;
    @Nullable
    private Listener listener;

    public XRGesturesMain() {
        mouseEvents.delegate = this;
        mouseEventsInterpreter.callbacks = this;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void resetEvents() {
        mouseEvents.resetGestures();
    }

    public void stop() {
        stopped = true;
        if (cursorFill != null) {
            mainHandler.removeCallbacks(cursorFill);
            cursorFill = null;
        }
    }

    public void onMouse(final int vx, final int vy) {
        if (stopped) {
            return;
        }
        if (cursorFill != null) {
            mainHandler.removeCallbacks(cursorFill);
        }
        mouseEvents.putCursor(vx, vy);
        cursorFill = new Runnable() {
            @Override
            public void run() {
                onMouse(0, 0);
            }
        };
        if (!stopped) {
            mainHandler.postDelayed(cursorFill, CURSOR_FILL_MS);
        }
    }

    public void onGestureState(int gesture) {
        Integer voted = clickMajorityVoting.call(gesture);
        if (useMajority) {
            if (voted != null) {
                mouseEvents.putClick(voted);
            }
        } else {
            mouseEvents.putClick(gesture);
        }
    }

    @Override
    public void mouseEventsAction(MouseEventsAction action) {
        if (!ignoreEventsUntilRelease) {
            mouseEventsInterpreter.action(action);
        }
        if (action.isRelease()) {
            ignoreEventsUntilRelease = false;
        }
    }

    @Override
    public void onClick(MouseEventFinger finger) {
        useMajority = false;
        emit(click(finger));
    }

    @Override
    public void onDrag(MouseEventFinger finger) {
        useMajority = true;
        emit(drag(finger));
    }

    @Override
    public void onDrop() {
        useMajority = false;
        emit(XRAirGesture.DROP);
    }

    @Override
    public void onPotentialDragOrClick(MouseEventFinger finger) {
        emit(potential(finger));
    }

    @Override
    public void onFistBegin() {
        emit(XRAirGesture.FIST_BEGIN);
    }

    @Override
    public void onFistEnd() {
        emit(XRAirGesture.FIST_END);
    }

    private void emit(XRAirGesture gesture) {
        if (listener != null) {
            listener.onXRAirGesture(gesture);
        }
    }

    private static XRAirGesture click(MouseEventFinger finger) {
        switch (finger) {
            case INDEX: return XRAirGesture.CLICK_INDEX;
            case MIDDLE: return XRAirGesture.CLICK_MIDDLE;
            case RING: return XRAirGesture.CLICK_RING;
            case PINKY: return XRAirGesture.CLICK_PINKY;
            default: return XRAirGesture.NONE;
        }
    }

    private static XRAirGesture drag(MouseEventFinger finger) {
        switch (finger) {
            case INDEX: return XRAirGesture.DRAG_INDEX;
            case MIDDLE: return XRAirGesture.DRAG_MIDDLE;
            case RING: return XRAirGesture.DRAG_RING;
            case PINKY: return XRAirGesture.DRAG_PINKY;
            default: return XRAirGesture.NONE;
        }
    }

    private static XRAirGesture potential(MouseEventFinger finger) {
        switch (finger) {
            case INDEX: return XRAirGesture.POTENTIAL_DRAG_OR_CLICK_INDEX;
            case MIDDLE: return XRAirGesture.POTENTIAL_DRAG_OR_CLICK_MIDDLE;
            case RING: return XRAirGesture.POTENTIAL_DRAG_OR_CLICK_RING;
            case PINKY: return XRAirGesture.POTENTIAL_DRAG_OR_CLICK_PINKY;
            default: return XRAirGesture.NONE;
        }
    }
}
