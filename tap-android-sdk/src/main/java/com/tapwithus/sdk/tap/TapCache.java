package com.tapwithus.sdk.tap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tapwithus.sdk.FeatureVersionSupport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TapCache {

    private static final int INT_NULL_VALUE = -1;
    private static final boolean BOOLEAN_NULL_VALUE = false;
    private static final String UNAVAILABLE = "Unavailable";
    private static final int UNAVAILABLE_INT = -2;
    private static final int TRUE_INT = 1;
    private static final int FALSE_INT = 0;

    private Map<String, TapCh> tapChs = new ConcurrentHashMap<>();

    public void onNameRead(@NonNull String identifier, @NonNull String name) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.name = name;
        saveToCache(tapCh);
    }

    public void onNameWrite(@NonNull String identifier, @NonNull String name) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.name = name;
        saveToCache(tapCh);
    }

    public void onBatteryRead(@NonNull String identifier, int battery) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.battery = battery;
        saveToCache(tapCh);
    }

    public void onBatteryUnavailable(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.battery = UNAVAILABLE_INT;
        saveToCache(tapCh);
    }

    public void onSerialNumberRead(@NonNull String identifier, @NonNull String serialNumber) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.serialNumber = serialNumber;
        saveToCache(tapCh);
    }

    public void onHwVerRead(@NonNull String identifier, @NonNull String hwVer) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.hwVer = hwVer;
        saveToCache(tapCh);
    }

    public void onFwVerRead(@NonNull String identifier, @NonNull String fwVer) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.fwVer = fwVer;
        saveToCache(tapCh);
    }

    public void onBootloaderVerRead(@NonNull String identifier, @NonNull String bootloaderVer) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.bootloaderVer = bootloaderVer;
        saveToCache(tapCh);
    }

    public void onBootloaderUnavailable(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.bootloaderVer = UNAVAILABLE;
        saveToCache(tapCh);
    }

    public void onTapInputSubscribed(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.tapNotification = TRUE_INT;
        saveToCache(tapCh);
    }

    public void onMouseInputSubscribed(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.mouseNotification = TRUE_INT;
        saveToCache(tapCh);
    }

    public void onAirMouseInputSubscribed(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.airMouseNotification = TRUE_INT;
        saveToCache(tapCh);
    }

    public void onAirMouseUnavailable(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.airMouseNotification = UNAVAILABLE_INT;
        saveToCache(tapCh);
    }

    public void onRawSensorInputSubscribed(@NonNull String identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.rawSensorNotification = TRUE_INT;
        saveToCache(tapCh);
    }

    public void onDataRequestSubscribed(@NonNull String  identifier) {
        TapCh tapCh = getFromCache(identifier);
        tapCh.dataRequestNotification = TRUE_INT;
        saveToCache(tapCh);
    }

    public @Nullable Tap getCached(@NonNull String identifier) {
        if (isCached(identifier)) {
            TapCh tapCh = getFromCache(identifier);
            return new Tap(identifier, tapCh.name, tapCh.battery, tapCh.serialNumber, tapCh.hwVer, tapCh.fwVer, tapCh.bootloaderVer);
        }
        return null;
    }

    public boolean has(@NonNull String identifier, @NonNull DataKey dataKey) {
        TapCh tapCh = getFromCache(identifier);
        switch (dataKey) {
            case Name: return tapCh.name != null;
            case Battery: return tapCh.battery != INT_NULL_VALUE;
            case SerialNumber: return tapCh.serialNumber != null;
            case HwVer: return tapCh.hwVer != null;
            case FwVer: return tapCh.fwVer != null;
            case BootloaderVer: return tapCh.bootloaderVer != null;
            case TapNotification: return tapCh.tapNotification != FALSE_INT;
            case MouseNotification: return tapCh.mouseNotification != FALSE_INT;
            case AirMouseNotification: return tapCh.airMouseNotification != FALSE_INT;
            case RawSensorNotification: return tapCh.rawSensorNotification != FALSE_INT;
            case DataRequestNotification: return tapCh.dataRequestNotification != FALSE_INT;
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
        tapCh.tapNotification = FALSE_INT;
        saveToCache(tapCh);
    }

    public boolean isCached(@NonNull String identifier) {
        return isCached(getFromCache(identifier));
    }

    private boolean isCached(TapCh tapCh) {
        if (tapCh.name == null || tapCh.battery == INT_NULL_VALUE || tapCh.serialNumber == null ||
                tapCh.hwVer == null || tapCh.fwVer == null ||  tapCh.bootloaderVer == null || tapCh.tapNotification == FALSE_INT) {
            return false;
        }

        if (FeatureVersionSupport.isFeatureSupported(tapCh.hwVer, tapCh.fwVer, FeatureVersionSupport.FEATURE_MOUSE_MODE)) {
            if (tapCh.mouseNotification == FALSE_INT) {
                return false;
            }
        }

        if (FeatureVersionSupport.isFeatureSupported(tapCh.hwVer, tapCh.fwVer, FeatureVersionSupport.FEATURE_AIR_MOUSE)) {
            if (tapCh.airMouseNotification == FALSE_INT) {
                return false;
            }
        }

        if (FeatureVersionSupport.isFeatureSupported(tapCh.hwVer, tapCh.fwVer, FeatureVersionSupport.FEATURE_RAW_SENSOR)) {
            if (tapCh.rawSensorNotification == FALSE_INT) {
                return false;
            }
        }

        if (FeatureVersionSupport.isFeatureSupported(tapCh.hwVer, tapCh.fwVer, FeatureVersionSupport.FEATURE_CONTROLLER_WITH_FULLHID)) {
            if (tapCh.dataRequestNotification == FALSE_INT) {
                return false;
            }
        }

        return true;
    }

    private @NonNull TapCh getFromCache(@NonNull String identifier) {
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
    
    private class TapCh {
        
        String identifier;
        String name = null;
        int battery = INT_NULL_VALUE;
        String serialNumber = null;
        String hwVer = null;
        String fwVer = null;
        String bootloaderVer = null;
        int tapNotification = FALSE_INT;
        int mouseNotification = FALSE_INT;
        int airMouseNotification = FALSE_INT;
        int rawSensorNotification = FALSE_INT;
        int dataRequestNotification = FALSE_INT;

        TapCh(@NonNull String identifier) {
            this.identifier = identifier;
        }

        TapCh(@NonNull TapCh other) {
            this.identifier = other.identifier;
            this.name = other.name;
            this.battery = other.battery;
            this.serialNumber = other.serialNumber;
            this.hwVer = other.hwVer;
            this.fwVer = other.fwVer;
            this.bootloaderVer = other.bootloaderVer;
            this.tapNotification = other.tapNotification;
            this.mouseNotification = other.mouseNotification;
            this.airMouseNotification = other.airMouseNotification;
            this.rawSensorNotification = other.rawSensorNotification;
            this.dataRequestNotification = other.dataRequestNotification;
        }
    }

    public enum DataKey {
        Name,
        Battery,
        SerialNumber,
        HwVer,
        FwVer,
        TapNotification,
        MouseNotification,
        AirMouseNotification,
        RawSensorNotification,
        DataRequestNotification,
        BootloaderVer
    }
}
