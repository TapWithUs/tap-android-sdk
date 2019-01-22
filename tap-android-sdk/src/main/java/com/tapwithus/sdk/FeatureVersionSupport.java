package com.tapwithus.sdk;

import android.support.annotation.NonNull;

import com.tapwithus.sdk.tap.Tap;

import java.util.HashMap;
import java.util.Map;

public class FeatureVersionSupport {

    public static final int FEATURE_ENABLE_TEXT_MODE = 1;
    public static final int FEATURE_MOUSE_MODE = 2;

    protected static Map<Integer, Integer> featureVersion = new HashMap<Integer, Integer>() {{
        put(FEATURE_ENABLE_TEXT_MODE, 10000);
        put(FEATURE_MOUSE_MODE, 10500);
    }};

    public static boolean isFeatureSupported(@NonNull Tap tap, int feature) {
        int featureMinVersion = Integer.MAX_VALUE;
        if (featureVersion.containsKey(feature)) {
            featureMinVersion = featureVersion.get(feature);
        }
        return semVerToInt(tap.getFwVer()) >= featureMinVersion;
    }

    public static boolean isFeatureSupported(@NonNull String fwVer, int feature) {
        int featureMinVersion = Integer.MAX_VALUE;
        if (featureVersion.containsKey(feature)) {
            featureMinVersion = featureVersion.get(feature);
        }
        return semVerToInt(fwVer) >= featureMinVersion;
    }

    public static int semVerToInt(String semVer) {
        String[] semVerParts = semVer.split("\\.");

        String major;
        String minor;
        String patch;

        switch (semVerParts.length) {
            case 1:
                major = String.format("%2s", semVerParts[0]).replace(' ', '0');
                minor = "00";
                patch = "00";
                break;
            case 2:
                major = String.format("%2s", semVerParts[0]).replace(' ', '0');
                minor = String.format("%2s", semVerParts[1]).replace(' ', '0');
                patch = "00";
                break;
            case 3:
                major = String.format("%2s", semVerParts[0]).replace(' ', '0');
                minor = String.format("%2s", semVerParts[1]).replace(' ', '0');
                patch = String.format("%2s", semVerParts[2]).replace(' ', '0');
                break;
            default:
                major = "00";
                minor = "00";
                patch = "00";
                break;
        }

        String ver = major + minor + patch;

        return Integer.parseInt(ver);
    }

    public static String intToSemVer(int intVer) {
        String patch = String.format("%2s", intVer % 100).trim();
        String minor = String.format("%2s", intVer / 100 % 100).trim();
        String major = String.format("%2s", intVer / 10000).trim();

        return major + "." + minor + "." + patch;
    }
}
