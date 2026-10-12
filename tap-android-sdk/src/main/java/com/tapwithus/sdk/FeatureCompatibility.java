package com.tapwithus.sdk;


import static java.lang.Math.max;
import static java.lang.Math.min;

import com.tapwithus.sdk.tap.FeatureVersionRange;

import java.util.ArrayList;
import java.util.List;


public class FeatureCompatibility {



    private List<FeatureVersionRange> _ranges;

    public FeatureCompatibility() {
        _ranges = new ArrayList<FeatureVersionRange>();
    }

    public FeatureCompatibility withAddedVersionRange(VersionRange hw, VersionRange fw) {
        _ranges.add(new FeatureVersionRange(hw, fw));
        return this;
    }

    public boolean isCompatible(int hw, int fw) {

        for (int i=0; i<_ranges.size(); i++) {
            if (this._ranges.get(i).isCompatible(hw, fw)) {
                return true;
            }
        }
        return false;
    }
}
