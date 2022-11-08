package com.tapwithus.sdk;

import android.util.Log;
import androidx.annotation.NonNull;

import com.tapwithus.sdk.tap.Tap;

import java.util.HashMap;
import java.util.Map;

public class FeatureVersionSupport {

    public static final int FEATURE_ENABLE_TEXT_MODE = 1;
    public static final int FEATURE_MOUSE_MODE = 2;
    public static final int FEATURE_AIR_MOUSE = 3;
    public static final int FEATURE_RAW_SENSOR = 4;
    public static final int FEATURE_CONTROLLER_WITH_MOUSEHID = 5;
    public static final int FEATURE_HAPTIC = 6;
    public static final int FEATURE_DEVELOPER_MODE = 7;
    public static final int FEATURE_CONTROLLER_WITH_FULLHID = 8;

    public static final int FIRST_TAP_XR_HW_VERSION = 40000; // as far as we know the TapXR will start at HW 4.0.0
    public static final int FIRST_TAP_XR_FW_VERSION = 30000; // really no idea what version this is

    public static final int MAX_VER_NUMBER = 99999;

    protected static Map<Integer, Integer> featureFwVer = new HashMap<Integer, Integer>() {{
        put(FEATURE_ENABLE_TEXT_MODE, 10000);
        put(FEATURE_MOUSE_MODE, 10500);
        put(FEATURE_AIR_MOUSE, 20000);
        put(FEATURE_RAW_SENSOR, 20325);
        put(FEATURE_CONTROLLER_WITH_MOUSEHID, 20303);
        put(FEATURE_HAPTIC, 20325);
        // just for a test put this up to ver5
//        put(FEATURE_AIR_MOUSE, 50000);
        put(FEATURE_DEVELOPER_MODE, 20324);
        put(FEATURE_CONTROLLER_WITH_FULLHID, 20405);
    }};

    protected static Map<Integer, Integer> featureFwMaxVersion = new HashMap<Integer, Integer>() {{
        put(FEATURE_ENABLE_TEXT_MODE, MAX_VER_NUMBER);
        put(FEATURE_MOUSE_MODE, FIRST_TAP_XR_FW_VERSION - 1);
        put(FEATURE_AIR_MOUSE, FIRST_TAP_XR_FW_VERSION - 1);
        put(FEATURE_RAW_SENSOR, MAX_VER_NUMBER);
        put(FEATURE_CONTROLLER_WITH_MOUSEHID, FIRST_TAP_XR_FW_VERSION - 1);
        put(FEATURE_HAPTIC, MAX_VER_NUMBER);
        put(FEATURE_DEVELOPER_MODE, MAX_VER_NUMBER);
        put(FEATURE_CONTROLLER_WITH_FULLHID, FIRST_TAP_XR_FW_VERSION - 1);
    }};

    protected static Map<Integer, Integer> featureHwVer = new HashMap<Integer, Integer>() {{
        put(FEATURE_ENABLE_TEXT_MODE, 30200);
        put(FEATURE_MOUSE_MODE, 30200);
        put(FEATURE_AIR_MOUSE, 30300);
        put(FEATURE_RAW_SENSOR, 30200);
        put(FEATURE_CONTROLLER_WITH_MOUSEHID, 30200);
        put(FEATURE_HAPTIC, 30200);
        put(FEATURE_DEVELOPER_MODE, 30200);
        put(FEATURE_CONTROLLER_WITH_FULLHID, 30200);
    }};

    protected static Map<Integer, Integer> featureHwMaxVersion = new HashMap<Integer, Integer>() {{
        put(FEATURE_ENABLE_TEXT_MODE, MAX_VER_NUMBER);
        put(FEATURE_MOUSE_MODE, FIRST_TAP_XR_HW_VERSION - 1);
        put(FEATURE_AIR_MOUSE, FIRST_TAP_XR_HW_VERSION - 1);
        put(FEATURE_RAW_SENSOR, MAX_VER_NUMBER);
        put(FEATURE_CONTROLLER_WITH_MOUSEHID, FIRST_TAP_XR_HW_VERSION - 1);
        put(FEATURE_HAPTIC, MAX_VER_NUMBER);
        put(FEATURE_DEVELOPER_MODE, MAX_VER_NUMBER);
        put(FEATURE_CONTROLLER_WITH_FULLHID, FIRST_TAP_XR_HW_VERSION - 1);
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

        // new part because we now have max values too
        int featureMaxHwVer = 0;
        if (featureHwMaxVersion.containsKey(feature)) {
            featureMaxHwVer = featureHwMaxVersion.get(feature);
        }

        int featureMaxFwVer = 0;
        if (featureFwMaxVersion.containsKey(feature)) {
            featureMaxFwVer = featureFwMaxVersion.get(feature);
        }

//        return semVerToInt(hwVer) >= featureMinHwVer && semVerToInt(fwVer) >= featureMinFwVer;
        return semVerToInt(hwVer) >= featureMinHwVer && semVerToInt(fwVer) >= featureMinFwVer &&
                semVerToInt(hwVer) <= featureMaxHwVer && semVerToInt(fwVer) <= featureMaxFwVer;
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

        int retVal = -1;
        try {
            retVal = Integer.parseInt(ver);
        } catch (NumberFormatException nfe) {
            Log.e("TapSDK Error", "Could not format \"ver\" error = " + nfe.getMessage());
        }
        return retVal;
    }

    public static String intToSemVer(int intVer) {
        String patch = String.format("%2s", intVer % 100).trim();
        String minor = String.format("%2s", intVer / 100 % 100).trim();
        String major = String.format("%2s", intVer / 10000).trim();

        return major + "." + minor + "." + patch;
    }
}
