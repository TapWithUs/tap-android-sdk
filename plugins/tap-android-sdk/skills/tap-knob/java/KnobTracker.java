/**
 * Pinch-hold + wrist roll → knob steps. No Android or Tap SDK imports.
 * Drop this class into the app. Feed UnifiedAirGesture codes and ImuMotionPacket.roll.
 *
 * onGesture(code) → "start", "release", or null
 * onRoll(roll)    → signed steps (0 when idle)
 * active          → knob index 0–3 while a pinch is held, else null
 */
public final class KnobTracker {

    private static final int NONE_CODE = 100;

    private final double stepDeg;
    private final int releaseAfter;

    /** Knob index 0–3 (AB, AC, AD, AE), or null when idle. */
    public Integer active;

    private Double anchor;
    private int noneCount;

    public KnobTracker(double stepDeg, int releaseAfter) {
        this.stepDeg = stepDeg;
        this.releaseAfter = releaseAfter;
    }

    public KnobTracker() {
        this(1.0, 4);
    }

    /** Shortest signed angle, so crossing +/-180 does not jump. */
    public static double wrapDegrees(double delta) {
        double wrapped = (delta + 180.0) % 360.0;
        if (wrapped < 0) {
            wrapped += 360.0;
        }
        return wrapped - 180.0;
    }

    public String onGesture(int code) {
        if (code == NONE_CODE) {
            if (active == null) {
                return null;
            }
            noneCount++;
            if (noneCount >= releaseAfter) {
                active = null;
                anchor = null;
                return "release";
            }
            return null;
        }
        noneCount = 0;
        Integer knob = holdIndex(code);
        if (knob == null || knob.equals(active)) {
            return null;
        }
        active = knob;
        anchor = null;
        return "start";
    }

    public int onRoll(double roll) {
        if (active == null) {
            return 0;
        }
        if (anchor == null) {
            anchor = roll;
            return 0;
        }
        int steps = (int) (wrapDegrees(roll - anchor) / stepDeg);
        if (steps != 0) {
            anchor = wrapDegrees(anchor + steps * stepDeg);
        }
        return steps;
    }

    /** AB_HOLD..AE_HOLD (110–113) → 0–3. */
    private static Integer holdIndex(int code) {
        if (code >= 110 && code <= 113) {
            return code - 110;
        }
        return null;
    }
}
