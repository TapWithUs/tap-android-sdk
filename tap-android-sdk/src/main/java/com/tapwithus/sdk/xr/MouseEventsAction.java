package com.tapwithus.sdk.xr;

import androidx.annotation.Nullable;

final class MouseEventsAction {

    enum Kind {
        RELEASE,
        FLICK,
        PRESS,
        DRAG,
        SCROLL,
        FIST,
        SWIPE
    }

    final Kind kind;
    @Nullable
    final MouseEventFinger finger;

    private MouseEventsAction(Kind kind, @Nullable MouseEventFinger finger) {
        this.kind = kind;
        this.finger = finger;
    }

    static MouseEventsAction release() {
        return new MouseEventsAction(Kind.RELEASE, null);
    }

    static MouseEventsAction press(MouseEventFinger finger) {
        return new MouseEventsAction(Kind.PRESS, finger);
    }

    static MouseEventsAction drag() {
        return new MouseEventsAction(Kind.DRAG, null);
    }

    static MouseEventsAction fist() {
        return new MouseEventsAction(Kind.FIST, null);
    }

    boolean isRelease() {
        return kind == Kind.RELEASE;
    }
}
