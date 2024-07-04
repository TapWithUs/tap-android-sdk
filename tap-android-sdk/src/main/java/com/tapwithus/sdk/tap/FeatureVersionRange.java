package com.tapwithus.sdk.tap;

import com.tapwithus.sdk.VersionRange;

public class FeatureVersionRange {
    private VersionRange _hw;
    private VersionRange _fw;

    public FeatureVersionRange(VersionRange hw, VersionRange fw) {
        this._hw = hw;
        this._fw = fw;
    }

    public boolean isCompatible(int hw, int fw) {
        return (this._hw.inRange(hw) && this._fw.inRange(fw));
    }
}