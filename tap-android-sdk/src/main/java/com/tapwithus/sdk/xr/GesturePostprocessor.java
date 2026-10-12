package com.tapwithus.sdk.xr;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Debounces a stream of V2 air-gesture class indexes.
 * TAPKit constructs this with hold debounce on and aggregated doubles off.
 */
public final class GesturePostprocessor {

    public static final class Step {
        public final int current;
        public final int previous;

        Step(int current, int previous) {
            this.current = current;
            this.previous = previous;
        }
    }

    private static final Map<String, Integer> LBL_IDX = new HashMap<>();
    private static final Map<Integer, String> IDX_LBL = new HashMap<>();
    private static final Set<String> PINCHES = new HashSet<>(Arrays.asList("AB", "AC", "AD", "AE", "FIST"));
    private static final Set<String> HOLDS = new HashSet<>(Arrays.asList(
            "AB_HOLD", "AC_HOLD", "AD_HOLD", "AE_HOLD", "FIST_HOLD"));
    private static final double WINDOW_MS = 300.0;
    private static final double COOLDOWN_MS = 200.0;
    private static final int NONE = 100;

    static {
        LBL_IDX.put("NONE", 100);
        LBL_IDX.put("LEFT", 101);
        LBL_IDX.put("RIGHT", 102);
        LBL_IDX.put("UP", 103);
        LBL_IDX.put("DOWN", 104);
        LBL_IDX.put("AB", 105);
        LBL_IDX.put("AC", 106);
        LBL_IDX.put("AD", 107);
        LBL_IDX.put("AE", 108);
        LBL_IDX.put("FIST", 109);
        LBL_IDX.put("AB_HOLD", 110);
        LBL_IDX.put("AC_HOLD", 111);
        LBL_IDX.put("AD_HOLD", 112);
        LBL_IDX.put("AE_HOLD", 113);
        LBL_IDX.put("FIST_HOLD", 114);
        LBL_IDX.put("DOUBLE_AB", 115);
        LBL_IDX.put("DOUBLE_AC", 116);
        LBL_IDX.put("DOUBLE_AD", 117);
        LBL_IDX.put("DOUBLE_AE", 118);
        for (Map.Entry<String, Integer> entry : LBL_IDX.entrySet()) {
            IDX_LBL.put(entry.getValue(), entry.getKey());
        }
    }

    public final boolean holdDebounce;
    public final boolean aggregatedDoubles;

    private final List<Integer> history;
    @Nullable
    private String pending;
    private double pendingAt;
    private boolean pendingReleased;
    private boolean holdActive;
    @Nullable
    private Integer holdLabel;
    private double lastFiredAt;
    private int previousOutputIndex;

    public GesturePostprocessor(boolean holdDebounce, boolean aggregatedDoubles) {
        this.holdDebounce = holdDebounce;
        this.aggregatedDoubles = aggregatedDoubles;
        this.history = new java.util.ArrayList<>(Collections.nCopies(10, NONE));
        this.previousOutputIndex = NONE;
    }

    @androidx.annotation.NonNull
    public java.util.List<XRAirGesture> toXRAirGestures(int classIndex) {
        Step step = call(classIndex);
        XRV2AirGesture current = toXRV2AirGesture(step.current);
        XRV2AirGesture previous = toXRV2AirGesture(step.previous);
        if (current == null || previous == null) {
            return java.util.Collections.emptyList();
        }
        return XRV2AirGestureConverter.convert(current, previous);
    }

    @androidx.annotation.NonNull
    public Step call(int classIndex) {
        int previous = previousOutputIndex;
        int current = process(classIndex);
        previousOutputIndex = current;
        return new Step(current, previous);
    }

    @Nullable
    public static XRV2AirGesture toXRV2AirGesture(int classIndex) {
        String name = IDX_LBL.get(classIndex);
        if (name == null) {
            return null;
        }
        switch (name) {
            case "NONE": return XRV2AirGesture.NONE;
            case "LEFT": return XRV2AirGesture.SWIPE_LEFT;
            case "RIGHT": return XRV2AirGesture.SWIPE_RIGHT;
            case "UP": return XRV2AirGesture.SWIPE_UP;
            case "DOWN": return XRV2AirGesture.SWIPE_DOWN;
            case "AB": return XRV2AirGesture.CLICK_INDEX;
            case "AC": return XRV2AirGesture.CLICK_MIDDLE;
            case "AD": return XRV2AirGesture.CLICK_RING;
            case "AE": return XRV2AirGesture.CLICK_PINKY;
            case "AB_HOLD": return XRV2AirGesture.DRAG_INDEX;
            case "AC_HOLD": return XRV2AirGesture.DRAG_MIDDLE;
            case "AD_HOLD": return XRV2AirGesture.DRAG_RING;
            case "AE_HOLD": return XRV2AirGesture.DRAG_PINKY;
            case "FIST_HOLD": return XRV2AirGesture.FIST;
            default: return null;
        }
    }

    private int process(int classIndex) {
        double now = monotonicTime();
        String knownName = IDX_LBL.get(classIndex);
        String name = knownName != null ? knownName : "NONE";
        int prev = history.get(history.size() - 1);
        appendHistory(classIndex);

        final int out;
        if (HOLDS.contains(name) && (!holdActive || (holdLabel != null && classIndex == holdLabel))) {
            holdActive = true;
            holdLabel = classIndex;
            out = classIndex;
        } else if (holdActive) {
            if (classIndex == NONE) {
                holdActive = false;
                lastFiredAt = now;
                out = NONE;
            } else {
                out = holdLabel != null ? holdLabel : NONE;
            }
        } else if (prev != NONE && classIndex != NONE) {
            out = NONE;
        } else {
            out = classIndex;
        }

        int fin = out;

        if (aggregatedDoubles && pending != null && (now - pendingAt) * 1000 >= WINDOW_MS) {
            Integer emitted = LBL_IDX.get(pending);
            fin = emitted != null ? emitted : NONE;
            pending = null;
            pendingReleased = false;
            if (PINCHES.contains(name)) {
                pending = name;
                pendingAt = now;
            }
        } else if (aggregatedDoubles && PINCHES.contains(name)) {
            if (name.equals(pending) && pendingReleased) {
                pending = null;
                pendingReleased = false;
                Integer doubled = LBL_IDX.get("DOUBLE_" + name);
                fin = doubled != null ? doubled : NONE;
            } else if (name.equals(pending)) {
                fin = NONE;
            } else {
                pending = name;
                pendingAt = now;
                pendingReleased = false;
                fin = NONE;
            }
        } else if (aggregatedDoubles && "NONE".equals(name) && pending != null) {
            pendingReleased = true;
        }

        if ((now - lastFiredAt) * 1000 < COOLDOWN_MS) {
            return NONE;
        }
        String outName = IDX_LBL.containsKey(fin) ? IDX_LBL.get(fin) : "NONE";
        if (!HOLDS.contains(outName) && !"NONE".equals(outName)) {
            lastFiredAt = now;
        }
        return fin;
    }

    private void appendHistory(int classIndex) {
        history.add(classIndex);
        if (history.size() > 10) {
            history.remove(0);
        }
    }

    private static double monotonicTime() {
        return System.nanoTime() / 1_000_000_000.0;
    }
}
