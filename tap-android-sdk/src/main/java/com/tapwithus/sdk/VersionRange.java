package com.tapwithus.sdk;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class VersionRange {

    static final int max = 999999;
    static final int min = 0;
    private final int _min;
    private final int _max;

    public VersionRange() {
        this._min = VersionRange.min;
        this._max = VersionRange.max;
    }

    public static VersionRange onlyMinValue(int value) {
        return new VersionRange(value, VersionRange.max);
    }

    public static VersionRange onlyMaxValue(int value) {
        return new VersionRange(VersionRange.min, value);
    }

    public VersionRange(int minValue, int maxValue) {
        this._min = max(0,min(minValue, maxValue));
        this._max = max(0,max(minValue, maxValue));
    }

    public boolean inRange(int value) {

        return (value >= this._min && value <= this._max);
    }

}
