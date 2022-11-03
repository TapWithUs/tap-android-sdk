package com.tapwithus.sdk;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tapwithus.sdk.airmouse.AirMousePacket;
import com.tapwithus.sdk.bluetooth.TapBluetoothListener;
import com.tapwithus.sdk.bluetooth.TapBluetoothManager;
import com.tapwithus.sdk.mode.RawSensorData;
import com.tapwithus.sdk.mode.RawSensorDataParser;
import com.tapwithus.sdk.mode.TapInputMode;
import com.tapwithus.sdk.mouse.MousePacket;
import com.tapwithus.sdk.tap.Tap;
import com.tapwithus.sdk.tap.TapCache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"WeakerAccess", "ConstantConditions"})
public class TapSdk {

    private static final int RAW_MODE_LOOP_DELAY = 10000;

    private static final String TAG = "TapSdk";

    public static final int ERR_SUBSCRIBE_MODE = 101;
    public static final int ERR_HAPTIC = 102;

    protected TapBluetoothManager tapBluetoothManager;
    private final ListenerManager<TapListener> tapListeners = new ListenerManager<>();
    private final Map<String, TapInputMode> modeSubscribers = new ConcurrentHashMap<>();
//    private Set<String> HIDMouseInRawModeSubscribers = new HashSet<>();
    private final Set<String> tapsInAirMouseState = new HashSet<>();
//    private List<String> startModeNotificationSubscribers = new CopyOnWriteArrayList<>();
//    private List<String> notifyOnConnectedAfterControllerModeStarted = new CopyOnWriteArrayList<>();
//    private List<String> notifyOnResumedAfterControllerModeStarted = new CopyOnWriteArrayList<>();
    private boolean debug = false;
    private boolean isClosing = false;
    private boolean isClosed = false;
    private boolean isPaused = false;
    private TapInputMode autoSetModeOnConnection = TapInputMode.controller();
//    private boolean autoSetControllerModeOnConnection = true;

    private Handler rawModeHandler;
    private Runnable rawModeRunnable;

    protected TapCache cache = new TapCache();
    private boolean clearCacheOnTapDisconnection = true;
    private boolean pauseResumeHandling = true;

    public TapSdk(TapBluetoothManager tapBluetoothManager) {
        this.tapBluetoothManager = tapBluetoothManager;
        this.tapBluetoothManager.registerTapBluetoothListener(tapBluetoothListener);
        startRawModeLoop();
    }

    public void enableDebug() {
        debug = true;
        tapBluetoothManager.enableDebug();
    }

    public void disableDebug() {
        debug = false;
        tapBluetoothManager.disableDebug();
    }

    public void clearCacheOnTapDisconnection(boolean clearCacheOnTapDisconnection) {
        this.clearCacheOnTapDisconnection = clearCacheOnTapDisconnection;
    }

//    public void enableAutoSetControllerModeOnConnection() {
//
//        autoSetControllerModeOnConnection = true;
//    }
//
//    public void disableAutoSetControllerModeOnConnection() {
//        autoSetControllerModeOnConnection = false;
//    }

    public void enablePauseResumeHandling() {
        pauseResumeHandling = true;
    }

    public void disablePauseResumeHandling() {
        pauseResumeHandling = false;
    }

    public void ignoreTap(@NonNull String tapIdentifier) {
        tapBluetoothManager.ignoreTap(tapIdentifier);
    }

    public void unignoreTap(@NonNull String tapIdentifier) {
        tapBluetoothManager.unignoreTap(tapIdentifier);
    }

    public Set<String> getIgnoredTaps() {
        return tapBluetoothManager.getIgnoredTaps();
    }

    public boolean isTapIgnored(@NonNull String tapIdentifier) {
        return tapBluetoothManager.isTapIgnored(tapIdentifier);
    }

    public void resume() {
        isPaused = false;

        if (!pauseResumeHandling) {
            return;
        }

        Set<String> actuallyConnectTaps = getConnectedTaps();


        for (Map.Entry<String, TapInputMode> entry : modeSubscribers.entrySet()) {
            if (actuallyConnectTaps.contains(entry.getKey())) {
                startMode(entry.getKey(), entry.getValue());
            }
        }

//        for (String tapIdentifier: getTapsInMode(MODE_CONTROLLER)) {
//            if (actuallyConnectTaps.contains(tapIdentifier)) {
////                notifyOnResumedAfterControllerModeStarted.add(tapIdentifier);
//                startControllerMode(tapIdentifier);
//            }
//        }
//
//        for (String tapIdentifier: getTapsInMode(MODE_TEXT)) {
//            if (actuallyConnectTaps.contains(tapIdentifier)) {
//                startTextMode(tapIdentifier);
////                if (!isConnectionInProgress(tapIdentifier)) {
////                    notifyOnTapResumed(tapIdentifier);
////                }
//            }
//        }



        // Check if a TAP was disconnected while the app was in background
        Set<String> mSubscribers = new HashSet<>(modeSubscribers.keySet());
        for (String tapIdentifier: mSubscribers) {
            if (!actuallyConnectTaps.contains(tapIdentifier)) {
                handleTapDisconnection(tapIdentifier);
            }
        }

        // Check if a new TAP was connected while the app was in background
        for (String tapIdentifier: actuallyConnectTaps) {
            if (!mSubscribers.contains(tapIdentifier)) {
                handleTapConnection(tapIdentifier);
            }
        }

        startRawModeLoop();

        if (isClosed) {
            isClosed = false;
            tapBluetoothManager.refreshConnections();
        }
    }

    public void pause() {
        isPaused = true;

        if (!pauseResumeHandling) {
            return;
        }

        stopRawModeLoop();

        Set<String> connectedTaps = getConnectedTaps();
        TapInputMode textMode = TapInputMode.text();
        for (String tapIdentifier : connectedTaps) {

            tapBluetoothManager.startMode(tapIdentifier, textMode.getBytes());
        }
//        List<String> controllerModeSubscribers = getTapsInMode(MODE_CONTROLLER);
//        for (String tapIdentifier: controllerModeSubscribers) {
//            tapBluetoothManager.startTextMode(tapIdentifier);
//        }
    }

    @NonNull
    public Set<String> getConnectedTaps() {
        return tapBluetoothManager.getConnectedTaps();
    }

    @Nullable
    public Tap getCachedTap(@NonNull String tapIdentifier) {
        return cache.getCached(tapIdentifier);
    }

    public void registerTapListener(@NonNull TapListener listener) {
        isClosed = false;

        tapListeners.registerListener(listener);
    }

    public void unregisterTapListener(@NonNull TapListener listener) {
        tapListeners.unregisterListener(listener);
    }

//    public void setMouseHIDEnabledInRawModeForAllTaps(boolean enable) {
//        Set<String> taps = getConnectedTaps();
//        for (String tapIdentifier : taps) {
//            setMouseHIDEnabledInRawMode(tapIdentifier, enable);
//        }
//    }
//
//    public void setMouseHIDEnabledInRawMode(String tapIdentifier, boolean enable) {
//        if (enable && !HIDMouseInRawModeSubscribers.contains(tapIdentifier)) {
//            HIDMouseInRawModeSubscribers.add(tapIdentifier);
//        } else if (!enable && HIDMouseInRawModeSubscribers.contains(tapIdentifier)) {
//            HIDMouseInRawModeSubscribers.remove(tapIdentifier);
//        }
//        if (getTapsInMode(MODE_CONTROLLER).contains(tapIdentifier)) {
//            startControllerMode(tapIdentifier);
//        }
//    }

    public boolean isAnyTapInAirGestureState() {
        Set<String> taps = getConnectedTaps();
        boolean result = false;
        for (String tapIdentifier : taps) {
            result = result || isTapInAirMouseState(tapIdentifier);
        }
        return result;
    }

    private int[] generateDurations(int[] durations) {
        int[] newDurations = new int[18];

        int minLength = Math.min(durations.length, newDurations.length);

        for (int i = 0; i < minLength; i++) {
            int v = durations[i];
            if (v < 10) {
                v = 10;
            }
            if (v > 2500) {
                v = 2500;
            }
            v /= 10;
            newDurations[i] = v;
        }

        return newDurations;
    }

    public void vibrate(@NonNull String tapIdentifier, int[] durations) {
        if (!isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_HAPTIC)) {
            notifyOnError(tapIdentifier, ERR_HAPTIC, "FEATURE_HAPTIC not supported");
            return;
        }
        tapBluetoothManager.sendHapticPacket(tapIdentifier, generateDurations(durations));
    }

    public boolean isTapInAirMouseState(String tapIdentifier)
    {
        return tapsInAirMouseState.contains(tapIdentifier);
    }


    public boolean isAnyTapSupportsAirGesture() {
        boolean supported = false;
        Set<String> taps = getConnectedTaps();
        for (String tapIdentifier : taps) {
            supported = supported || isAirMouseSupported(tapIdentifier);
        }

        return supported;
    }

    public boolean isAirMouseSupported(String tapIdentifier) {
        return (isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_AIR_MOUSE));
    }

//    public boolean isMouseHIDEnabledInRawMode(String tapIdentifier) {
//        return HIDMouseInRawModeSubscribers.contains(tapIdentifier);
//    }

    public void setDefaultMode(TapInputMode mode, Boolean applyImmediate) {
        if (!mode.isValid()) {
            return;
        }
        autoSetModeOnConnection = mode;
        if (applyImmediate) {
            Set<String> taps = getConnectedTaps();
            for (String tapIdentifier : taps) {
                startMode(tapIdentifier, mode);

            }
        }
    }

    private void startMode(String tapIdentifier, TapInputMode mode) {
        if (!mode.isValid()) {
            notifyOnError(tapIdentifier, ERR_SUBSCRIBE_MODE, "Invalid mode passed");
            return;
        }

        modeSubscribers.put(tapIdentifier, mode);
        tapBluetoothManager.startMode(tapIdentifier, mode.getBytes());
//        startModeNotificationSubscribers.add(tapIdentifier);
//        switch (mode) {
//            case MODE_TEXT:
//                startTextMode(tapIdentifier);
//                break;
//            case MODE_CONTROLLER:
//                startControllerMode(tapIdentifier);
//                break;
//            case MODE_CONTROLLER_WITH_MOUSEHID:
//                startControllerWithMouseHIDMode(tapIdentifier);
//        }
    }

    public void refreshBond(@NonNull String tapIdentifier) {
        tapBluetoothManager.refreshBond(tapIdentifier);
    }

    public void startControllerMode(@NonNull String tapIdentifier) {
        log("Starting Controller mode - " + tapIdentifier);
        startMode(tapIdentifier, TapInputMode.controller());

//        modeSubscribers.put(tapIdentifier, MODE_CONTROLLER);
//        tapBluetoothManager.startControllerMode(tapIdentifier);
    }

    public  void startTextMode(@NonNull String tapIdentifier) {
        if (!isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_ENABLE_TEXT_MODE)) {
            logError("FEATURE_ENABLE_TEXT_MODE not supported - " + tapIdentifier);
//            startModeNotificationSubscribers.remove(tapIdentifier);
            return;
        }

        log("Starting Text mode - " + tapIdentifier);
        startMode(tapIdentifier, TapInputMode.text());
//        modeSubscribers.put(tapIdentifier, MODE_TEXT);
//        tapBluetoothManager.startTextMod e(tapIdentifier);

//        if (!modeSubscribers.containsKey(tapIdentifier) || modeSubscribers.get(tapIdentifier) != MODE_TEXT) {
//            log("Starting Text mode - " + tapIdentifier);
//            modeSubscribers.put(tapIdentifier, MODE_TEXT);
//            tapBluetoothManager.startTextMode(tapIdentifier);
//        }
    }

    public void startControllerWithMouseHIDMode(@NonNull String tapIdentifier) {

        if (!isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_CONTROLLER_WITH_MOUSEHID)) {
            logError("FEATURE_CONTROLLER_WITH_MOUSEHID not supported - " + tapIdentifier + ", Falling back to Controller mode");
            startControllerMode(tapIdentifier);
            return;
        }
        log("Starting Controller with Mouse HID mode - " + tapIdentifier);
        startMode(tapIdentifier, TapInputMode.controllerWithMouseHID());
//        modeSubscribers.put(tapIdentifier, MODE_CONTROLLER_WITH_MOUSEHID);
//        tapBluetoothManager.startControllerModeWithMouseHID(tapIdentifier);
    }

    public void startControllerWithFullHIDMode(@NonNull String tapIdentifier) {

        if (!isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_CONTROLLER_WITH_FULLHID)) {
            logError("FEATURE_CONTROLLER_WITH_FULLHID not supported - " + tapIdentifier + ", Falling back to Controller mode");
            startControllerMode(tapIdentifier);
            return;
        }
        log("Starting Controller with Keyboard HID mode - " + tapIdentifier);
        startMode(tapIdentifier, TapInputMode.controllerWithFullHID());
    }

    public void requestShiftSwitchState(@NonNull String tapIdentifier) {
        if (!isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_CONTROLLER_WITH_FULLHID)) {
            logError("FEATURE_CONTROLLER_WITH_FULLHID not supported - " + tapIdentifier + ", Can't request SwitchShift state");
            startControllerMode(tapIdentifier);
            return;
        }
        log("Requesting Shift/Switch state - " + tapIdentifier);
        tapBluetoothManager.requestShiftSwitchState(tapIdentifier);
    }

    public void requestTap(@NonNull String tapIdentifier, byte combination) {
        if (!isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_CONTROLLER_WITH_FULLHID)) {
            logError("FEATURE_CONTROLLER_WITH_FULLHID not supported - " + tapIdentifier + ", Can't request setTap");
            startControllerMode(tapIdentifier);
            return;
        }
        log("Requesting Tap - " + combination + ", on tap: " + tapIdentifier);
        tapBluetoothManager.requestTap(tapIdentifier, combination);
    }

    public void startRawSensorMode(@NonNull String tapIdentifier, byte deviceAccelerometerSensitivity, byte imuGyroSensitivity, byte imuAccelerometerSensitivity) {

        if (!isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_RAW_SENSOR)) {
            logError("FEATURE_RAW_SENSOR not supported - " + tapIdentifier);
            return;
        }
        log("Starting Raw Sensor mode - " + tapIdentifier);
        startMode(tapIdentifier, TapInputMode.rawSensorData(deviceAccelerometerSensitivity, imuGyroSensitivity, imuAccelerometerSensitivity));

    }
//    private boolean isModeValid(int mode) {
//        return mode >= 1 && mode >> NUM_OF_MODES <= 0;
//    }

//    public int getMode(String tapIdentifier) {
//        return modeSubscribers.containsKey(tapIdentifier) ? modeSubscribers.get(tapIdentifier) : 0;
//    }

//    public List<String> getTapsInMode(int mode) {
//        List<String> taps = new ArrayList<>();
//
//        if (isModeValid(mode)) {
//            for (Map.Entry<String, Integer> entry : modeSubscribers.entrySet()) {
//                String tapIdentifier = entry.getKey();
//                if (isInMode(tapIdentifier, mode)) {
//                    taps.add(tapIdentifier);
//                }
//            }
//        }
//
//        return taps;
//    }

//    public boolean isInMode(String tapIdentifier, int mode) {
//        if (!modeSubscribers.containsKey(tapIdentifier)) {
//            return false;
//        }
//        return (modeSubscribers.get(tapIdentifier) & mode) == mode;
//    }

    public void writeName(@NonNull String tapIdentifier, @NonNull String name) {
        tapBluetoothManager.writeName(tapIdentifier, name);
    }

    public static boolean[] toFingers(int tapInput) {
        final boolean[] fingers = new boolean[5];
        for (int i = 0; i < 5; i++) {
            fingers[i] = (1 << i & tapInput) != 0;
        }
        return fingers;
    }

    public static int[] toShiftAndSwitch(int tapShiftAndSwitchInt) {
        final int[] shiftSwitch = new int[2];
        for (int i = 0; i < 2; i++) {
            shiftSwitch[i] = (3 << (i * 2) & tapShiftAndSwitchInt) >> (i * 2);
        }
        return shiftSwitch;
    }

    public void refreshConnections() {
        tapBluetoothManager.refreshConnections();
    }

    public boolean isConnectionInProgress() {
        return tapBluetoothManager.isConnectionInProgress();
    }

    public boolean isConnectionInProgress(@NonNull String deviceAddress) {
        return tapBluetoothManager.isConnectionInProgress(deviceAddress);
    }

    public void close() {
        isClosing = true;
        stopRawModeLoop();
        tapBluetoothManager.close();
        modeSubscribers.clear();

        handleCloseReset();
    }

//    private RawSensorDataParserListener rawSensorListener = new RawSensorDataParserListener() {
//        @Override
//        public void onRawSensorDataReceived(@NonNull String tapIdentifier, RawSensorData rsData) {
//            notifyOnRawSensorDataReceieved(tapIdentifier, rsData);
//        }
//    };

    @SuppressWarnings("FieldCanBeLocal")
    private final TapBluetoothListener tapBluetoothListener = new TapBluetoothListener() {

        @Override
        public void onBluetoothTurnedOn() {
            if (isPaused || isClosing) {
                return;
            }
            notifyOnBluetoothTurnedOn();
//            tapBluetoothManager.refreshConnections();
        }

        @Override
        public void onBluetoothTurnedOff() {
            if (isPaused || isClosing) {
                return;
            }
            notifyOnBluetoothTurnedOff();
        }

        @Override
        public void onTapStartConnecting(@NonNull String tapAddress) {
            if (isPaused || isClosing) {
                return;
            }
            notifyOnTapStartConnecting(tapAddress);
        }

        @Override
        public void onTapConnected(@NonNull String tapAddress) {
            handleEmission(tapAddress);
        }

        @Override
        public void onTapAlreadyConnected(@NonNull String tapAddress) {
            handleEmission(tapAddress);
        }

        @Override
        public void onTapDisconnected(@NonNull String tapAddress) {
            handleTapDisconnection(tapAddress);
        }

        @Override
        public void onNameRead(@NonNull String tapAddress, @NonNull String name) {
            cache.onNameRead(tapAddress, name);
            handleEmission(tapAddress);
        }

        @Override
        public void onNameWrite(@NonNull String tapAddress, @NonNull String name) {
            cache.onNameWrite(tapAddress, name);
            notifyOnTapChanged(tapAddress);
        }

        @Override
        public void onBatteryRead(@NonNull String tapAddress, int battery) {
            cache.onBatteryRead(tapAddress, battery);
            handleEmission(tapAddress);
        }

        @Override
        public void onSerialNumberRead(@NonNull String tapAddress, @NonNull String serialNumber) {
            cache.onSerialNumberRead(tapAddress, serialNumber);
            handleEmission(tapAddress);
        }

        @Override
        public void onHwVerRead(@NonNull String tapAddress, @NonNull String hwVer) {
            cache.onHwVerRead(tapAddress, hwVer);
            handleEmission(tapAddress);
        }

        @Override
        public void onFwVerRead(@NonNull String tapAddress, @NonNull String fwVer) {
            cache.onFwVerRead(tapAddress, fwVer);
            handleEmission(tapAddress);
        }

        @Override
        public void onBootloaderVerRead(@NonNull String tapAddress, @NonNull String bootloaderVer) {
            cache.onBootloaderVerRead(tapAddress, bootloaderVer);
            handleEmission(tapAddress);
        }

        @Override
        public void onRawSensorDataReceieved(@NonNull String tapAddress, byte[] data) {
            if (isPaused || isClosing) {
                return;
            }
            if (modeSubscribers.containsKey(tapAddress)) {
                TapInputMode mode = modeSubscribers.get(tapAddress);
                ArrayList<RawSensorData> rsData = RawSensorDataParser.parseWhole(tapAddress, data, mode.getDeviceAccelerometerSensitivity(), mode.getImuGyroSensitivity(), mode.getImuAccelerometerSensitivity());


                for (RawSensorData rsDatum : rsData) {
                    notifyOnRawSensorDataReceieved(tapAddress, rsDatum);
                }

            }
        }

//        @Override
//        public void onControllerModeStarted(@NonNull String tapAddress) {
////            if (notifyOnConnectedAfterControllerModeStarted.contains(tapAddress)) {
////                notifyOnConnectedAfterControllerModeStarted.remove(tapAddress);
////                notifyOnTapConnected(tapAddress);
////            } else if (notifyOnResumedAfterControllerModeStarted.contains(tapAddress)) {
////                notifyOnResumedAfterControllerModeStarted.remove(tapAddress);
////                notifyOnTapResumed(tapAddress);
////            } else {
////                notifyOnControllerModeStarted(tapAddress);
////            }
//
//        }

//        @Override
//        public void onTextModeStarted(@NonNull String tapAddress) {
////            if (notifyOnConnectedAfterControllerModeStarted.contains(tapAddress)) {
////                notifyOnConnectedAfterControllerModeStarted.remove(tapAddress);
////                notifyOnTapConnected(tapAddress);
////            } else {
////                notifyOnTextModeStarted(tapAddress);
////            }
//        }

//        @Override
//        public void onControllerWithMouseHIDModeStarted(@NonNull String tapAddress) {
//
//        }

        @Override
        public void onTapInputSubscribed(@NonNull String tapAddress) {
            cache.onTapInputSubscribed(tapAddress);
            handleEmission(tapAddress);
        }

        @Override
        public void onMouseInputSubscribed(@NonNull String tapAddress) {
            cache.onMouseInputSubscribed(tapAddress);
            handleEmission(tapAddress);
        }


        @Override
        public void onAirMouseInputSubscribed(@NonNull String tapAddress) {
            cache.onAirMouseInputSubscribed(tapAddress);
            tapBluetoothManager.requestReadTapState(tapAddress);
            handleEmission(tapAddress);
        }

        @Override
        public void onDataRequestSubscribed(@NonNull String tapAddress) {
            cache.onDataRequestSubscribed(tapAddress);
            tapBluetoothManager.requestShiftSwitchState(tapAddress);
            handleEmission(tapAddress);
        }

        @Override
        public void onRawSensorInputSubscribed(@NonNull String tapAddress) {
            cache.onRawSensorInputSubscribed(tapAddress);
            handleEmission(tapAddress);
        }

        @Override
        public void onTapInputReceived(@NonNull String tapAddress, int data, int repeatData) {
            if (isTapInAirMouseState(tapAddress)) {
                if (data == 2) {
                    AirMousePacket packet = new AirMousePacket(new byte[] { AirMousePacket.AIR_MOUSE_GESTURE_INDEX_TO_THUMB_TOUCH, 0});
                    notifyOnAirMouseInputReceived(tapAddress, packet);
                    return;
                } else if (data == 4) {
                    AirMousePacket packet = new AirMousePacket(new byte[] { AirMousePacket.AIR_MOUSE_GESTURE_MIDDLE_TO_THUMB_TOUCH, 0});
                    notifyOnAirMouseInputReceived(tapAddress, packet);
                    return;
                }
                return;
            }
            // I think here is the last place where we are about to send the information on repeats back to
            // everyone, so we need to normalize the repeat info. The Tap sends 0 for one time, 1 for two and 3 for three
            // theoretically sending 2 is an error - but the info is encoded in the 5th and 6th bit of the byte i.e. 16 and 32

//            log("The byte we are using for repeat is " + repeatData);
            int convertedRepeatData =  (3 << 4 & repeatData) >> (4);

//            log("The repeat int is " + convertedRepeatData);
            switch(convertedRepeatData) {
                case 0:
                    convertedRepeatData = 1;
                    break;
                case 1:
                    convertedRepeatData = 2;
                    break;
                case 2:
                    logError("Something weird happened, got a repeat value of 2 from TAP");
                    break;
                case 3:
                    convertedRepeatData = 3;
                    break;
                default:
                    //this shouldn't be possible at all unless I coded wrong
                    logError("Something super weird, got a value for repeatData of " + repeatData);
            }
            notifyOnTapInputReceived(tapAddress, data, convertedRepeatData);
        }

        @Override
        public void onTapShiftSwitchReceived(@NonNull String tapAddress, int data) {
            notifyOnTapShiftSwitchReceived(tapAddress, data);
        }

        @Override
        public void onMouseInputReceived(@NonNull String tapAddress, @NonNull MousePacket data) {
            notifyOnMouseInputReceived(tapAddress, data);
        }

        @Override
        public void onAirMouseInputReceived(@NonNull String tapAddress, @NonNull AirMousePacket data) {
            notifyOnAirMouseInputReceived(tapAddress, data);
        }

        @Override
        public void onTapChangedState(@NonNull String tapIdentifier, int state) {
            if (state == 1) {
                tapsInAirMouseState.add(tapIdentifier);
            } else {
                tapsInAirMouseState.remove(tapIdentifier);
            }
            notifyOnTapChangedState(tapIdentifier, state);
        }

        @Override
        public void onError(@NonNull String tapAddress, int code, @NonNull String description) {
            notifyOnError(tapAddress, code, description);
        }
    };



    private void notifyOnBluetoothTurnedOn() {
        tapListeners.notifyAll(TapListener::onBluetoothTurnedOn);
    }

    private void notifyOnBluetoothTurnedOff() {
        tapListeners.notifyAll(TapListener::onBluetoothTurnedOff);
    }

    private void notifyOnTapStartConnecting(@NonNull final String tapIdentifier) {
        tapListeners.notifyAll(listener -> listener.onTapStartConnecting(tapIdentifier));
    }

    private void notifyOnTapConnected(@NonNull final String tapIdentifier) {
        tapListeners.notifyAll(listener -> listener.onTapConnected(tapIdentifier));
    }

    private void notifyOnTapDisconnected(@NonNull final String tapIdentifier) {
        tapListeners.notifyAll(listener -> listener.onTapDisconnected(tapIdentifier));
    }

    private void notifyOnTapResumed(@NonNull final String tapIdentifier) {
        tapListeners.notifyAll(listener -> listener.onTapResumed(tapIdentifier));
    }

    private void notifyOnTapChanged(@NonNull final String tapIdentifier) {
        tapListeners.notifyAll(listener -> listener.onTapChanged(tapIdentifier));
    }

    private void notifyOnControllerModeStarted(@NonNull final String tapIdentifier) {
//        if (!startModeNotificationSubscribers.contains(tapIdentifier)) {
//            return;
//        }
//        startModeNotificationSubscribers.remove(tapIdentifier);
//
//        tapListeners.notifyAll(new NotifyAction<TapListener>() {
//            @Override
//            public void onNotify(TapListener listener) {
//                listener.onControllerModeStarted(tapIdentifier);
//            }
//        });
    }

    private void notifyOnTextModeStarted(@NonNull final String tapIdentifier) {
//        if (!startModeNotificationSubscribers.contains(tapIdentifier)) {
//            return;
//        }
//        startModeNotificationSubscribers.remove(tapIdentifier);
//
//        tapListeners.notifyAll(new NotifyAction<TapListener>() {
//            @Override
//            public void onNotify(TapListener listener) {
//                listener.onTextModeStarted(tapIdentifier);
//            }
//        });
    }

    private void notifyOnTapInputReceived(@NonNull final String tapIdentifier, final int data, final int repeatData) {
        tapListeners.notifyAll(listener -> listener.onTapInputReceived(tapIdentifier, data, repeatData));
    }

    private void notifyOnTapShiftSwitchReceived(@NonNull final String tapIdentifier, final int data) {
        tapListeners.notifyAll(listener -> listener.onTapShiftSwitchReceived(tapIdentifier, data));
    }

    private void notifyOnRawSensorDataReceieved(@NonNull final String tapIdentifier, final RawSensorData rsData) {
        tapListeners.notifyAll(listener -> listener.onRawSensorInputReceived(tapIdentifier, rsData));
    }

    private void notifyOnMouseInputReceived(@NonNull final String tapIdentifier, @NonNull final MousePacket data) {
        tapListeners.notifyAll(listener -> listener.onMouseInputReceived(tapIdentifier, data));
    }

    private void notifyOnAirMouseInputReceived(@NonNull final String tapIdentifier, @NonNull final AirMousePacket data) {
        tapListeners.notifyAll(listener -> listener.onAirMouseInputReceived(tapIdentifier, data));
    }

//    private void notifyOnRawSensorInputReceived(@NonNull final String tapIdentifier, @NonNull final int data) {
//        tapListeners.notifyAll(new NotifyAction<TapListener>() {
//            @Override
//            public void onNotify(TapListener listener) {
//                listener.onRawSensorInputReceived(tapIdentifier, data);
//            }
//        });
//    }

    private void notifyOnTapChangedState(@NonNull final String tapIdentifier, final int state) {
        tapListeners.notifyAll(listener -> listener.onTapChangedState(tapIdentifier, state));
    }

    private void notifyOnError(final String tapIdentifier, final int code, final String description) {
        tapListeners.notifyAll(listener -> listener.onError(tapIdentifier, code, description));
    }

    protected void log(String message) {
        if (debug) {
            Log.d(TAG, message);
        }
    }

    protected void logError(String message) {
        Log.e(TAG, message);
    }

    private void handleTapConnection(@NonNull String tapIdentifier) {
        if (isPaused || isClosing) {
            return;
        }

        startMode(tapIdentifier, autoSetModeOnConnection);
        notifyOnTapConnected(tapIdentifier);
//            List<String> textModeSubscribers = getTapsInMode(MODE_TEXT);
//            if (textModeSubscribers.contains(tapIdentifier) || !autoSetControllerModeOnConnection) {
//                modeSubscribers.put(tapIdentifier, MODE_TEXT);
//                if (!isConnectionInProgress(tapIdentifier)) {
//                    notifyOnTapConnected(tapIdentifier);
//                }
//            } else {
//                notifyOnConnectedAfterControllerModeStarted.add(tapIdentifier);
//                startControllerMode(tapIdentifier);
//            }
//        }
    }

    private void handleTapDisconnection(@NonNull String tapIdentifier) {
        if (isPaused) {
            cache.softClear(tapIdentifier);
            return;
        }

        if (clearCacheOnTapDisconnection) {
            cache.clear(tapIdentifier);
        }
        modeSubscribers.remove(tapIdentifier);
//        HIDMouseInRawModeSubscribers.remove(tapIdentifier);
        if (!isClosing) {
            notifyOnTapDisconnected(tapIdentifier);
        }

        handleCloseReset();
    }

    private void handleCloseReset() {
        if (isClosing && tapBluetoothManager.numOfConnectedTaps() == 0) {
            isClosing = false;
            isClosed = true;
        }
    }

    private void handleEmission(@NonNull String tapIdentifier) {
        if (!cache.isCached(tapIdentifier)) {
            handleCacheDependencies(tapIdentifier);
            return;
        }

        if (isTapConnected(tapIdentifier)) {
            handleTapConnection(tapIdentifier);
        } else {
            handleTapDisconnection(tapIdentifier);
        }
    }

    private void handleCacheDependencies(@NonNull String tapIdentifier) {
        if (!cache.has(tapIdentifier, TapCache.DataKey.Name)) {
            tapBluetoothManager.readName(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.Battery)) {
            tapBluetoothManager.readBattery(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.SerialNumber)) {
            tapBluetoothManager.readSerialNumber(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.HwVer)) {
            tapBluetoothManager.readHwVer(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.FwVer)) {
            tapBluetoothManager.readFwVer(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.BootloaderVer)) {
            tapBluetoothManager.readBootloaderVer(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.TapNotification)) {
            tapBluetoothManager.setupTapNotification(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.MouseNotification)) {
            tapBluetoothManager.setupMouseNotification(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.AirMouseNotification)) {
            tapBluetoothManager.setupAirMouseNotification(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.RawSensorNotification)) {
            tapBluetoothManager.setupRawSensorNotification(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.DataRequestNotification)) {
            tapBluetoothManager.setupDataNotification(tapIdentifier);
        } else{
            logError("Cache already has all required fields");
        }
    }

    private boolean isTapConnected(@NonNull String tapIdentifier) {
        return tapBluetoothManager.getConnectedTaps().contains(tapIdentifier);
    }

    private void startRawModeLoop() {
        if (rawModeHandler != null && rawModeRunnable != null) {
            log("Raw Mode Loop already exists");
            return;
        }

        log("startRawModeLoop");

        rawModeHandler = new Handler(Looper.getMainLooper());
        rawModeRunnable = new Runnable() {
            @Override
            public void run() {
                log("In raw mode loop");
                for (String tapIdentifier: modeSubscribers.keySet()) {
                    tapBluetoothManager.startMode(tapIdentifier, modeSubscribers.get(tapIdentifier).getBytes());
//                    startControllerMode(tapIdentifier);
                }
                rawModeHandler.postDelayed(this, RAW_MODE_LOOP_DELAY);
            }
        };

        rawModeHandler.postDelayed(rawModeRunnable, 0);
    }

    private void stopRawModeLoop() {
        log("Stopping raw mode");

        if (rawModeHandler != null && rawModeRunnable != null) {
            rawModeHandler.removeCallbacks(rawModeRunnable);
        }
        rawModeHandler = null;
        rawModeRunnable = null;
    }

    private boolean isFeatureSupported(@NonNull String tapIdentifier, int feature) {
        Tap tap = cache.getCached(tapIdentifier);
        if (tap == null) {
            return false;
        }
        return FeatureVersionSupport.isFeatureSupported(tap, feature);
    }

    private boolean isFeatureSupported(@NonNull Tap tap, int feature) {
        return FeatureVersionSupport.isFeatureSupported(tap, feature);
    }


}
