package com.tapwithus.sdk;

import android.support.annotation.NonNull;

import com.tapwithus.sdk.tap.Tap;

import java.util.HashMap;
import java.util.Map;

public class FeatureVersionSupport {

    public static final int FEATURE_ENABLE_TEXT_MODE = 1;
    public static final int FEATURE_MOUSE_MODE = 2;
    public static final int FEATURE_AIR_MOUSE = 3;
    public static final int FEATURE_RAW_SENSOR = 4;
    public static final int FEATURE_CONTROLLER_WITH_MOUSEHID = 5;

    protected static Map<Integer, Integer> featureFwVer = new HashMap<Integer, Integer>() {{
        put(FEATURE_ENABLE_TEXT_MODE, 10000);
        put(FEATURE_MOUSE_MODE, 10500);
        put(FEATURE_AIR_MOUSE, 20000);
        put(FEATURE_RAW_SENSOR, 20303);
        put(FEATURE_CONTROLLER_WITH_MOUSEHID, 20303);
        // just for a test put this up to ver5
//        put(FEATURE_AIR_MOUSE, 50000);
    }};

    protected static Map<Integer, Integer> featureHwVer = new HashMap<Integer, Integer>() {{
        put(FEATURE_ENABLE_TEXT_MODE, 30200);
        put(FEATURE_MOUSE_MODE, 30200);
        put(FEATURE_AIR_MOUSE, 30300);
        put(FEATURE_RAW_SENSOR, 30200);
        put(FEATURE_CONTROLLER_WITH_MOUSEHID, 30200);
    }};

    public static boolean isFeatureSupported(@NonNull Tap tap, int feature) {
        return isFeatureSupported(tap.getHwVer(), tap.getFwVer(), feature);
    }

    public static boolean isFeatureSupported(@NonNull String hwVer, @NonNull String fwVer, int feature) {
        int featureMinHwVer = Integer.MAX_VALUE;
        if (featureHwVer.containsKey(feature)) {
            featureMinHwVer = featureHwVer.get(feature);
        }
        int featureMinFwVer = Integer.MAX_VALUE;
        if (featureFwVer.containsKey(feature)) {
            featureMinFwVer = featureFwVer.get(feature);
        }
        return semVerToInt(hwVer) >= featureMinHwVer && semVerToInt(fwVer) >= featureMinFwVer;
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
