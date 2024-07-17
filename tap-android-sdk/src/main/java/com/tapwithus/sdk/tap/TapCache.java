package com.tapwithus.sdk.tap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import com.tapwithus.sdk.FeatureVersionSupport;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TapCache {

    private static final int INT_NULL_VALUE = -1;
    private static final boolean BOOLEAN_NULL_VALUE = false;
    private static final String UNAVAILABLE = "Unavailable";
    private static final int UNAVAILABLE_INT = -2;
    private static final int TRUE_INT = 1;
    private static final int FALSE_INT = 0;

    private static final String FALSE_STRING = "FALSE";
    private static final String TRUE_STRING = "TRUE";
    private static final String UNAVAILABLE_STRING = "N.A";

    protected Map<String, TapCh> tapChs = new ConcurrentHashMap<>();

    public void onNameRead(@NonNull String identifier, @NonNull String name) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(DataKey.Name, name);
//        tapCh.name = name;
        saveToCache(tapCh);
    }

    public void onNameWrite(@NonNull String identifier, @NonNull String name) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(DataKey.Name, name);
//        tapCh.name = name;
        saveToCache(tapCh);
    }

    public void onBatteryRead(@NonNull String identifier, int battery) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(DataKey.Battery, String.valueOf(battery));
//        tapCh.battery = battery;
        saveToCache(tapCh);
    }

    public void onBatteryUnavailable(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
//        tapCh.set(DataKey.Battery, UNAVAILABLE_STRING);
//        tapCh.battery = UNAVAILABLE_INT;
        saveToCache(tapCh);
    }

    public void onSerialNumberRead(@NonNull String identifier, @NonNull String serialNumber) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(DataKey.SerialNumber, serialNumber);
//        tapCh.serialNumber = serialNumber;
        saveToCache(tapCh);
    }

    public void onCustomDataRead(@NonNull String identifier, String dataKey, String value) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(dataKey, value);
        saveToCache(tapCh);
    }

    public void onHwVerRead(@NonNull String identifier, @NonNull String hwVer) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(DataKey.HwVer, hwVer);
//        tapCh.hwVer = hwVer;
        saveToCache(tapCh);
    }

    public void onFwVerRead(@NonNull String identifier, @NonNull String fwVer) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(DataKey.FwVer, fwVer);
//        tapCh.fwVer = fwVer;
        saveToCache(tapCh);
    }

    public void onBootloaderVerRead(@NonNull String identifier, @NonNull String bootloaderVer) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(DataKey.BootloaderVer, bootloaderVer);
//        tapCh.bootloaderVer = bootloaderVer;
        saveToCache(tapCh);
    }

    public void onBootloaderUnavailable(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
//        tapCh.set();
//        tapCh.bootloaderVer = UNAVAILABLE;
        saveToCache(tapCh);
    }

    public void onTapInputSubscribed(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(DataKey.TapNotification, TRUE_STRING);
//        tapCh.tapNotification = TRUE_INT;
        saveToCache(tapCh);
    }

    public void onMouseInputSubscribed(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(DataKey.MouseNotification, TRUE_STRING);
//        tapCh.mouseNotification = TRUE_INT;
        saveToCache(tapCh);
    }

    public void onAirMouseInputSubscribed(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(DataKey.AirMouseNotification, TRUE_STRING);
//        tapCh.airMouseNotification = TRUE_INT;
        saveToCache(tapCh);
    }

    public void onAirMouseUnavailable(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
//        tapCh.airMouseNotification = UNAVAILABLE_INT;
        saveToCache(tapCh);
    }

    public void onRawSensorInputSubscribed(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(DataKey.RawSensorNotification, TRUE_STRING);
//        tapCh.rawSensorNotification = TRUE_INT;
        saveToCache(tapCh);
    }

    public void onDataRequestSubscribed(@NonNull String  identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.set(DataKey.DataRequestNotification, TRUE_STRING);
//        tapCh.dataRequestNotification = TRUE_INT;
        saveToCache(tapCh);
    }

    public @Nullable Tap getCached(@NonNull String identifier) {
        if (isCached(identifier)) {
            TapCh tapCh = getFromCache(identifier);
            int battery = 0;
            try {
                battery = Integer.parseInt(tapCh.get(DataKey.Battery));
            } catch(NumberFormatException e) {

            }

            return new Tap(identifier, tapCh.get(DataKey.Name), battery, tapCh.get(DataKey.SerialNumber), tapCh.get(DataKey.HwVer), tapCh.get(DataKey.FwVer), tapCh.get(DataKey.BootloaderVer));
        }
        return null;
    }

    public boolean has(@NonNull String identifier, @NonNull String dataKey) {
        TapCh tapCh = getFromCache(identifier);
        return tapCh.has(dataKey);
//        switch (dataKey) {
//            case Name: return tapCh.name != null;
//            case Battery: return tapCh.battery != INT_NULL_VALUE;
//            case SerialNumber: return tapCh.serialNumber != null;
//            case HwVer: return tapCh.hwVer != null;
//            case FwVer: return tapCh.fwVer != null;
//            case BootloaderVer: return tapCh.bootloaderVer != null;
//            case TapNotification: return tapCh.tapNotification != FALSE_INT;
//            case MouseNotification: return tapCh.mouseNotification != FALSE_INT;
//            case AirMouseNotification: return tapCh.airMouseNotification != FALSE_INT;
//            case RawSensorNotification: return tapCh.rawSensorNotification != FALSE_INT;
//            case DataRequestNotification: return tapCh.dataRequestNotification != FALSE_INT;
//        }
//        return false;
    }

    public boolean shouldHave(@NonNull String identifier, @NonNull String dataKey) {
        TapCh tapCh = getFromCache(identifier);
        String hwVer = tapCh.has(DataKey.HwVer) ? tapCh.get(DataKey.HwVer) : "0";
        String fwVer = tapCh.has(DataKey.FwVer) ? tapCh.get(DataKey.FwVer) : "0";
        switch (dataKey) {
            case DataKey.Name: return true;
            case DataKey.Battery: return true;
            case DataKey.SerialNumber: return true;
            case DataKey.HwVer: return true;
            case DataKey.FwVer: return true;
            case DataKey.BootloaderVer: return true;
            case DataKey.TapNotification: return true;
            case DataKey.MouseNotification: return FeatureVersionSupport.isFeatureSupported(hwVer, fwVer, FeatureVersionSupport.FEATURE_MOUSE_MODE);
            case DataKey.AirMouseNotification: return FeatureVersionSupport.isFeatureSupported(hwVer, fwVer, FeatureVersionSupport.FEATURE_AIR_MOUSE);
            case DataKey.RawSensorNotification: return FeatureVersionSupport.isFeatureSupported(hwVer, fwVer, FeatureVersionSupport.FEATURE_RAW_SENSOR);
            case DataKey.DataRequestNotification: return !FeatureVersionSupport.isXr(hwVer);
        }
        return false;
    }

    public void clear() {
        tapChs.clear();
    }

    public void clear(@NonNull String identifier) {
        tapChs.remove(identifier);
    }

    public void softClear(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.remove(DataKey.TapNotification);
//        tapCh.set(DataKey.TapNotification, FALSE_STRING);
//        tapCh.tapNotification = FALSE_INT;
        saveToCache(tapCh);
    }

    public boolean isCached(@NonNull String identifier) {
        return isCached(getFromCache(identifier));
    }

    protected boolean isCached(TapCh tapCh) {
        if (!tapCh.has(DataKey.Name) || !tapCh.has(DataKey.Battery) || !tapCh.has(DataKey.SerialNumber) || !tapCh.has(DataKey.HwVer) || !tapCh.has(DataKey.FwVer) || !tapCh.has(DataKey.BootloaderVer) || !tapCh.has(DataKey.TapNotification)) {
            return false;
        }
//        if (tapCh.name == null || tapCh.battery == INT_NULL_VALUE || tapCh.serialNumber == null ||
//                tapCh.hwVer == null || tapCh.fwVer == null ||  tapCh.bootloaderVer == null || tapCh.tapNotification == FALSE_INT) {
//
//            return false;
//        }
        String hwVer = tapCh.get(DataKey.HwVer);
        String fwVer = tapCh.get(DataKey.FwVer);

        if (FeatureVersionSupport.isFeatureSupported(hwVer, fwVer, FeatureVersionSupport.FEATURE_MOUSE_MODE)) {
            if (!tapCh.has(DataKey.MouseNotification)) {
                return false;
            }
//            if (tapCh.mouseNotification == FALSE_INT) {
//                return false;
//            }
        }

        if (FeatureVersionSupport.isFeatureSupported(hwVer, fwVer, FeatureVersionSupport.FEATURE_AIR_MOUSE)) {
            if (!tapCh.has(DataKey.AirMouseNotification)) {
                return false;
            }
//            if (tapCh.airMouseNotification == FALSE_INT) {
//                return false;
//            }
        }

        if (FeatureVersionSupport.isFeatureSupported(hwVer, fwVer, FeatureVersionSupport.FEATURE_RAW_SENSOR)) {
            if (!tapCh.has(DataKey.RawSensorNotification))  {
                return false;
            }
//
//
//            if (tapCh.rawSensorNotification == FALSE_INT) {
//                return false;
//            }
        }

        if (FeatureVersionSupport.isFeatureSupported(hwVer, fwVer, FeatureVersionSupport.FEATURE_CONTROLLER_WITH_FULLHID)) {
            if (!tapCh.has(DataKey.DataRequestNotification)) {
                 return false;
            }
//            if (tapCh.dataRequestNotification == FALSE_INT) {
//                return false;
//            }
        }

        return true;
    }

    public @NonNull TapCh getFromCache(@NonNull String identifier) {
        TapCh tapCh = tapChs.get(identifier);
        if (tapCh == null) {
            return new TapCh(identifier);
        }
        return new TapCh(tapCh);
    }

    private void saveToCache(TapCh tapCh) {
        if (tapCh != null) {
            tapChs.put(tapCh.identifier, tapCh);
        }
    }
    
    public class TapCh {

        Hashtable<String, String> data;

        String identifier;
//        String name = null;
//        int battery = INT_NULL_VALUE;
//        String serialNumber = null;
//        String hwVer = null;
//        String fwVer = null;
//        String bootloaderVer = null;
//        int tapNotification = FALSE_INT;
//        int mouseNotification = FALSE_INT;
//        int airMouseNotification = FALSE_INT;
//        int rawSensorNotification = FALSE_INT;
//        int dataRequestNotification = FALSE_INT;

        TapCh(@NonNull String identifier) {
            this.identifier = identifier;
            this.data = new Hashtable<>();
        }

        TapCh(@NonNull TapCh other) {

            this.identifier = other.identifier;
            this.data = new Hashtable<>();

            Enumeration<String> k = other.data.keys();
            while (k.hasMoreElements()) {
                String key = k.nextElement();
                this.data.put(key, other.data.get(key));
            }
//
//            this.data = other.data.
//            this.name = other.name;
//            this.battery = other.battery;
//            this.serialNumber = other.serialNumber;
//            this.hwVer = other.hwVer;
//            this.fwVer = other.fwVer;
//            this.bootloaderVer = other.bootloaderVer;
//            this.tapNotification = other.tapNotification;
//            this.mouseNotification = other.mouseNotification;
//            this.airMouseNotification = other.airMouseNotification;
//            this.rawSensorNotification = other.rawSensorNotification;
//            this.dataRequestNotification = other.dataRequestNotification;
        }

        public void set(String key, String value) {
            this.data.put(key, value);
        }

        public String get(String key) {
            if (this.data.containsKey(key)) {
                return this.data.get(key);
            } else {
                return null;
            }
        }

        public boolean has(String key) {
            return this.data.containsKey(key);
        }

        public void remove(String key) {
            if (this.has(key)) {
                this.data.remove(key);
            }
        }
    }

    public class DataKey {
        public static final String Name = "Name";
        public static final String Battery = "Battery";
        public static final String SerialNumber = "SerialNumber";
        public static final String HwVer = "HwVer";
        public static final String FwVer = "FwVer";
        public static final String TapNotification = "TapNotification";
        public static final String MouseNotification = "MouseNotification";
        public static final String AirMouseNotification = "AirMouseNotification";
        public static final String RawSensorNotification = "RawSensorNotification";
        public static final String DataRequestNotification = "DataRequestNotification";
        public static final String BootloaderVer = "BootloaderVer";
    }
}
