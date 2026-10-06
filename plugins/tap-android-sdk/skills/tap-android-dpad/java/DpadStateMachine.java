package com.tapwithus.tapsdk.sample;

import java.util.ArrayList;
import java.util.List;

/**
 * Air-gesture codes + IMU motion → D-Pad events. No Android or Tap SDK imports.
 * Pass nowNanos from System.nanoTime().
 *
 * Event.kind is one of:
 *   direction, pinch, hold, mode, rotate, drag, release, hide, show
 */
public final class DpadStateMachine {

    public static final class Event {
        public final String kind;
        public final String name;
        public final int index;
        public final int dx;
        public final int dy;
        public final double degrees;

        Event(String kind, String name, int index, int dx, int dy, double degrees) {
            this.kind = kind;
            this.name = name;
            this.index = index;
            this.dx = dx;
            this.dy = dy;
            this.degrees = degrees;
        }

        static Event named(String kind, String name) {
            return new Event(kind, name, -1, 0, 0, 0);
        }

        static Event indexed(String kind, int index) {
            return new Event(kind, null, index, 0, 0, 0);
        }
    }

    private final double rotateLockDeg;
    private final long lockWindowNs;
    private final int releaseAfter;
    private final int showAfter;
    private final long debounceNs;

    /** Pinch index 0–3 while held, else null. */
    public Integer held;
    /** "pending", "rotate", "drag", or null. */
    public String mode;
    public boolean hidden;

    private long holdStartNs;
    private Double refRoll;
    private Double lastRoll;
    private double rollTravel;
    private int noneCount;
    private Integer lastCode;
    private long lastCodeNs = Long.MIN_VALUE;

    public DpadStateMachine(double rotateLockDeg, long lockWindowNs, int releaseAfter,
                            int showAfter, long debounceNs) {
        this.rotateLockDeg = rotateLockDeg;
        this.lockWindowNs = lockWindowNs;
        this.releaseAfter = releaseAfter;
        this.showAfter = showAfter;
        this.debounceNs = debounceNs;
    }

    public DpadStateMachine() {
        this(25.0, 1_000_000_000L, 4, 2, 40_000_000L);
    }

    public List<Event> tick(long nowNanos) {
        List<Event> events = new ArrayList<Event>();
        if ("pending".equals(mode) && nowNanos - holdStartNs >= lockWindowNs) {
            events.add(lock(rollTravel >= rotateLockDeg ? "rotate" : "drag"));
        }
        return events;
    }

    public List<Event> onGesture(int code, long nowNanos) {
        List<Event> events = tick(nowNanos);
        if (code == 100) {
            noneCount++;
            if (hidden && noneCount >= showAfter) {
                hidden = false;
                noneCount = 0;
                events.add(Event.named("show", null));
            } else if (held != null && noneCount >= releaseAfter) {
                events.addAll(release());
            }
            return events;
        }
        if (hidden) {
            return events;
        }
        noneCount = 0;

        if (code == 114) {
            if (!debounced(code, nowNanos)) {
                events.addAll(release());
                hidden = true;
                events.add(Event.named("hide", null));
            }
        } else if (code >= 101 && code <= 104) {
            if (!debounced(code, nowNanos)) {
                events.add(Event.named("direction", swipeName(code)));
            }
        } else if (code >= 110 && code <= 113) {
            int index = code - 110;
            if ((held == null || index != held) && !debounced(code, nowNanos)) {
                events.addAll(release());
                held = index;
                mode = "pending";
                holdStartNs = nowNanos;
                rollTravel = 0;
                events.add(Event.indexed("hold", index));
            }
        } else if (code >= 105 && code <= 108) {
            if (held == null && !debounced(code, nowNanos)) {
                events.add(Event.indexed("pinch", code - 105));
            }
        }
        return events;
    }

    public List<Event> onMotion(int dx, int dy, double roll, long nowNanos) {
        List<Event> events = tick(nowNanos);
        if (held == null) {
            return events;
        }
        if ("drag".equals(mode)) {
            if (dx != 0 || dy != 0) {
                events.add(new Event("drag", null, held, dx, dy, 0));
            }
            return events;
        }
        if (lastRoll != null) {
            rollTravel += Math.abs(roll - lastRoll);
        }
        lastRoll = roll;
        if (refRoll == null) {
            refRoll = roll;
        }
        if ("pending".equals(mode) && rollTravel >= rotateLockDeg) {
            events.add(lock("rotate"));
        }
        events.add(new Event("rotate", null, held, 0, 0, roll - refRoll));
        return events;
    }

    private boolean debounced(int code, long nowNanos) {
        if (lastCode != null && lastCode == code && nowNanos - lastCodeNs < debounceNs) {
            return true;
        }
        lastCode = code;
        lastCodeNs = nowNanos;
        return false;
    }

    private List<Event> release() {
        List<Event> events = new ArrayList<Event>();
        if (held != null) {
            events.add(Event.indexed("release", held));
        }
        held = null;
        mode = null;
        refRoll = null;
        lastRoll = null;
        noneCount = 0;
        return events;
    }

    private Event lock(String nextMode) {
        mode = nextMode;
        return Event.named("mode", nextMode);
    }

    private static String swipeName(int code) {
        switch (code) {
            case 101: return "left";
            case 102: return "right";
            case 103: return "up";
            case 104: return "down";
            default: return "";
        }
    }
}
