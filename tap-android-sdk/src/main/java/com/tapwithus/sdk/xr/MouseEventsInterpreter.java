package com.tapwithus.sdk.xr;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

/**
 * Turns press and release actions into click, drag, drop, and fist gestures.
 */
final class MouseEventsInterpreter {

    interface Callbacks {
        void onClick(MouseEventFinger finger);
        void onDrag(MouseEventFinger finger);
        void onDrop();
        void onPotentialDragOrClick(MouseEventFinger finger);
        void onFistBegin();
        void onFistEnd();
    }

    private static final long CLICK_TIME_MS = 200;

    private MouseEventsAction prev = MouseEventsAction.release();
    @Nullable
    private Runnable dragTimer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    Callbacks callbacks;

    void action(final MouseEventsAction next) {
        if (dragTimer != null) {
            mainHandler.removeCallbacks(dragTimer);
            dragTimer = null;
        }
        MouseEventsAction.Kind previousKind = prev.kind;
        if (previousKind == MouseEventsAction.Kind.RELEASE && next.kind == MouseEventsAction.Kind.PRESS && next.finger != null) {
            final MouseEventFinger finger = next.finger;
            dragTimer = new Runnable() {
                @Override
                public void run() {
                    prev = MouseEventsAction.drag();
                    if (callbacks != null) {
                        callbacks.onDrag(finger);
                    }
                }
            };
            mainHandler.postDelayed(dragTimer, CLICK_TIME_MS);
            if (callbacks != null) {
                callbacks.onPotentialDragOrClick(finger);
            }
        } else if (previousKind == MouseEventsAction.Kind.PRESS && next.kind == MouseEventsAction.Kind.RELEASE && prev.finger != null) {
            if (callbacks != null) {
                callbacks.onClick(prev.finger);
            }
        } else if (previousKind == MouseEventsAction.Kind.DRAG && next.kind == MouseEventsAction.Kind.RELEASE) {
            if (callbacks != null) {
                callbacks.onDrop();
            }
        } else if (previousKind == MouseEventsAction.Kind.RELEASE && next.kind == MouseEventsAction.Kind.FIST) {
            if (callbacks != null) {
                callbacks.onFistBegin();
            }
        } else if (previousKind == MouseEventsAction.Kind.FIST && next.kind == MouseEventsAction.Kind.RELEASE) {
            if (callbacks != null) {
                callbacks.onFistEnd();
            }
        }
        prev = next;
    }
}
