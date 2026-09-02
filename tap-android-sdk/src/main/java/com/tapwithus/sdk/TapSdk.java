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
import com.tapwithus.sdk.mode.TapXRState;
import com.tapwithus.sdk.mouse.MousePacket;
import com.tapwithus.sdk.tap.Tap;
import com.tapwithus.sdk.tap.TapCache;
import com.tapwithus.sdk.v2.DeviceFeature;
import com.tapwithus.sdk.v2.ImuMotionPacket;
import com.tapwithus.sdk.v2.ImuSensitivity;
import com.tapwithus.sdk.v2.TapV2Callback;
import com.tapwithus.sdk.v2.TapV2Encoder;
import com.tapwithus.sdk.v2.TapV2InputModeMapper;
import com.tapwithus.sdk.v2.TapV2Message;
import com.tapwithus.sdk.v2.VisionSensorModel;
import com.tapwithus.sdk.v2.VisionSensorOpMode;

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
    public static final int ERR_V2_NOT_SUPPORTED = 103;

    private static final int V2_GET_TIMEOUT_MS = 2000;

    protected TapBluetoothManager tapBluetoothManager;
    private final ListenerManager<TapListener> tapListeners = new ListenerManager<>();



    private final Map<String, TapInputMode> modeSubscribers = new ConcurrentHashMap<>();
    private final Map<String, TapXRState> stateSubscribers = new ConcurrentHashMap<>();
    private final Map<String, ImuSensitivity> v2ImuSensitivities = new ConcurrentHashMap<>();
    private final Map<String, PendingV2Request> pendingV2Requests = new ConcurrentHashMap<>();
    private final Handler v2RequestHandler = new Handler(Looper.getMainLooper());

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
    private TapXRState defaultXRState = TapXRState.none();
//    private boolean autoSetControllerModeOnConnection = true;

    private Handler rawModeHandler;
    private Runnable rawModeRunnable;

    protected TapCache cache;
    private boolean clearCacheOnTapDisconnection = true;
    private boolean pauseResumeHandling = true;

    public TapSdk(TapBluetoothManager tapBluetoothManager) {
        this.cache = this.getTapCache();
        this.tapBluetoothManager = tapBluetoothManager;
        this.tapBluetoothManager.registerTapBluetoothListener(tapBluetoothListener);
        startRawModeLoop();
    }

    public TapCache getTapCache() {
        return new TapCache();
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
            if (isV2Tap(tapIdentifier)) {
                // Text mode on V2 devices means disabling all data-stream features
                tapBluetoothManager.startV2Mode(tapIdentifier, TapV2InputModeMapper.commands(textMode, null));
                continue;
            }
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
        if (isV2Tap(tapIdentifier)) {
            tapBluetoothManager.sendV2HapticPacket(tapIdentifier, generateDurations(durations));
            return;
        }
        if (!isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_HAPTIC)) {
            notifyOnError(tapIdentifier, ERR_HAPTIC, "FEATURE_HAPTIC not supported");
            return;
        }
        tapBluetoothManager.sendHapticPacket(tapIdentifier, generateDurations(durations));
    }

    // ============================== V2 (framed protocol) API ==============================

    /**
     * @return true if the connected Tap speaks the V2 framed protocol
     *         (e.g. TapBand, or TapXR with V2 firmware)
     */
    public boolean isV2Tap(@NonNull String tapIdentifier) {
        return cache.isV2(tapIdentifier);
    }

    /**
     * Enables or disables a single device feature on a V2 Tap. Note that setting
     * an input mode also updates features - prefer one control style per session.
     */
    public void setFeature(@NonNull String tapIdentifier, @NonNull DeviceFeature feature, boolean enable) {
        if (!verifyV2(tapIdentifier, "setFeature")) {
            return;
        }
        tapBluetoothManager.sendV2Frame(tapIdentifier, TapV2Encoder.encodeSetFeature(feature, enable));
    }

    public void getFeature(@NonNull String tapIdentifier, @NonNull DeviceFeature feature, @NonNull TapV2Callback<Boolean> callback) {
        if (!verifyV2(tapIdentifier, "getFeature")) {
            callback.onResponse(tapIdentifier, null);
            return;
        }
        requestV2(tapIdentifier, "feature:" + feature.getValue(), TapV2Encoder.encodeGetFeature(feature), callback);
    }

    public void setVisionSensorOpMode(@NonNull String tapIdentifier, @NonNull VisionSensorOpMode mode) {
        if (!verifyV2(tapIdentifier, "setVisionSensorOpMode")) {
            return;
        }
        tapBluetoothManager.sendV2Frame(tapIdentifier, TapV2Encoder.encodeSetVisionSensorOpMode(mode));
    }

    public void getVisionSensorOpMode(@NonNull String tapIdentifier, @NonNull TapV2Callback<VisionSensorOpMode> callback) {
        if (!verifyV2(tapIdentifier, "getVisionSensorOpMode")) {
            callback.onResponse(tapIdentifier, null);
            return;
        }
        requestV2(tapIdentifier, "vision_op_mode", TapV2Encoder.encodeGetVisionSensorOpMode(), callback);
    }

    public void setVisionSensorModel(@NonNull String tapIdentifier, @NonNull VisionSensorModel model) {
        if (!verifyV2(tapIdentifier, "setVisionSensorModel")) {
            return;
        }
        tapBluetoothManager.sendV2Frame(tapIdentifier, TapV2Encoder.encodeSetVisionSensorModel(model));
    }

    public void getVisionSensorModel(@NonNull String tapIdentifier, @NonNull TapV2Callback<VisionSensorModel> callback) {
        if (!verifyV2(tapIdentifier, "getVisionSensorModel")) {
            callback.onResponse(tapIdentifier, null);
            return;
        }
        requestV2(tapIdentifier, "vision_model", TapV2Encoder.encodeGetVisionSensorModel(), callback);
    }

    /**
     * @param gyroSensitivity gyroscope sensitivity index (0-5)
     * @param accelerometerSensitivity IMU accelerometer sensitivity index (0-4)
     */
    public void setImuSensitivity(@NonNull String tapIdentifier, int gyroSensitivity, int accelerometerSensitivity) {
        if (!verifyV2(tapIdentifier, "setImuSensitivity")) {
            return;
        }
        ImuSensitivity sensitivity = new ImuSensitivity(gyroSensitivity, accelerometerSensitivity);
        v2ImuSensitivities.put(tapIdentifier, sensitivity);
        tapBluetoothManager.sendV2Frame(tapIdentifier,
                TapV2Encoder.encodeSetImuSensitivity(sensitivity.getGyro(), sensitivity.getAccelerometer()));
    }

    public void getImuSensitivity(@NonNull String tapIdentifier, @NonNull TapV2Callback<ImuSensitivity> callback) {
        if (!verifyV2(tapIdentifier, "getImuSensitivity")) {
            callback.onResponse(tapIdentifier, null);
            return;
        }
        requestV2(tapIdentifier, "imu_sensitivity", TapV2Encoder.encodeGetImuSensitivity(), callback);
    }

    public void setStandbyState(@NonNull String tapIdentifier, boolean standby) {
        if (!verifyV2(tapIdentifier, "setStandbyState")) {
            return;
        }
        tapBluetoothManager.sendV2Frame(tapIdentifier, TapV2Encoder.encodeStandbyStateSet(standby));
    }

    public void getStandbyState(@NonNull String tapIdentifier, @NonNull TapV2Callback<Boolean> callback) {
        if (!verifyV2(tapIdentifier, "getStandbyState")) {
            callback.onResponse(tapIdentifier, null);
            return;
        }
        requestV2(tapIdentifier, "standby", TapV2Encoder.encodeStandbyStateGet(), callback);
    }

    public void sendKeepAlive(@NonNull String tapIdentifier) {
        if (!verifyV2(tapIdentifier, "sendKeepAlive")) {
            return;
        }
        tapBluetoothManager.sendV2KeepAlive(tapIdentifier);
    }

    private boolean verifyV2(@NonNull String tapIdentifier, @NonNull String action) {
        if (!isV2Tap(tapIdentifier)) {
            notifyOnError(tapIdentifier, ERR_V2_NOT_SUPPORTED, action + " requires a V2 Tap device");
            return false;
        }
        return true;
    }

    private class PendingV2Request {

        final String tapIdentifier;
        final String fullKey;
        final TapV2Callback callback;
        final Runnable timeoutRunnable;

        PendingV2Request(String tapIdentifier, String fullKey, TapV2Callback callback) {
            this.tapIdentifier = tapIdentifier;
            this.fullKey = fullKey;
            this.callback = callback;
            this.timeoutRunnable = () -> {
                if (pendingV2Requests.remove(fullKey, this)) {
                    invoke(null);
                }
            };
        }

        @SuppressWarnings("unchecked")
        void invoke(@Nullable Object value) {
            v2RequestHandler.removeCallbacks(timeoutRunnable);
            callback.onResponse(tapIdentifier, value);
        }
    }

    @SuppressWarnings("rawtypes")
    private void requestV2(@NonNull String tapIdentifier, @NonNull String key, @NonNull byte[] frame, @NonNull TapV2Callback callback) {
        String fullKey = tapIdentifier + "|" + key;
        PendingV2Request request = new PendingV2Request(tapIdentifier, fullKey, callback);
        PendingV2Request prior = pendingV2Requests.put(fullKey, request);
        if (prior != null) {
            // A newer request supersedes the pending one
            prior.invoke(null);
        }
        tapBluetoothManager.sendV2Frame(tapIdentifier, frame);
        v2RequestHandler.postDelayed(request.timeoutRunnable, V2_GET_TIMEOUT_MS);
    }

    private void resolveV2Request(@NonNull String tapIdentifier, @NonNull String key, @Nullable Object value) {
        PendingV2Request request = pendingV2Requests.remove(tapIdentifier + "|" + key);
        if (request != null) {
            request.invoke(value);
        }
    }

    private void cancelV2Requests(@NonNull String tapIdentifier) {
        for (Map.Entry<String, PendingV2Request> entry : pendingV2Requests.entrySet()) {
            if (entry.getKey().startsWith(tapIdentifier + "|")) {
                if (pendingV2Requests.remove(entry.getKey(), entry.getValue())) {
                    entry.getValue().invoke(null);
                }
            }
        }
    }

    // =======================================================================================

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

    public void setDefaultXRState(TapXRState state, Boolean applyImmediate) {
        if (!state.isValid()) {
            return;
        }
        this.defaultXRState = state;
        if (applyImmediate) {
            Set<String> taps = getConnectedTaps();
            for (String tapIdentifier : taps) {
                startXRSTate(tapIdentifier, state);

            }
        }
    }

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

    private void startXRSTate(String tapIdentifier, TapXRState state) {
        stateSubscribers.put(tapIdentifier, state);

        if (isV2Tap(tapIdentifier)) {
            if (state.type == TapXRState.NONE) {
                return;
            }
            TapInputMode mode = modeSubscribers.containsKey(tapIdentifier)
                    ? modeSubscribers.get(tapIdentifier)
                    : TapInputMode.controller();
            if (mode.type != TapInputMode.TEXT) {
                tapBluetoothManager.startV2Mode(tapIdentifier, TapV2InputModeMapper.commands(mode, state));
                if (state.type == TapXRState.USER_CONTROL) {
                    stateSubscribers.put(tapIdentifier, TapXRState.none());
                }
            }
            return;
        }

        if (state.getBytes().length > 0) {
            if (isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_XR_STATE)) {
                if (modeSubscribers.containsKey(tapIdentifier) && modeSubscribers.get(tapIdentifier).type != TapInputMode.TEXT) {
                    tapBluetoothManager.startXRState(tapIdentifier, state.getBytes());
                    if (state.type == TapXRState.USER_CONTROL) {
                        stateSubscribers.put(tapIdentifier, TapXRState.none());
                    }
                }
            }
        }
    }

    private void startMode(String tapIdentifier, TapInputMode mode) {
        if (!mode.isValid()) {
            notifyOnError(tapIdentifier, ERR_SUBSCRIBE_MODE, "Invalid mode passed");
            return;
        }

        modeSubscribers.put(tapIdentifier, mode);

        if (isV2Tap(tapIdentifier)) {
            if (mode.type == TapInputMode.RAW_SENSOR) {
                v2ImuSensitivities.put(tapIdentifier,
                        new ImuSensitivity(mode.getImuGyroSensitivity(), mode.getImuAccelerometerSensitivity()));
            }
            tapBluetoothManager.startV2Mode(tapIdentifier,
                    TapV2InputModeMapper.commands(mode, stateSubscribers.get(tapIdentifier)));
            return;
        }

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

    public void startWithNoModes() {
        tapBluetoothManager.disableModes();
    }

    public void startControllerMode(@NonNull String tapIdentifier) {
        log("Starting Controller mode - " + tapIdentifier);
        startMode(tapIdentifier, TapInputMode.controller());

//        modeSubscribers.put(tapIdentifier, MODE_CONTROLLER);
//        tapBluetoothManager.startControllerMode(tapIdentifier);
    }

    public  void startTextMode(@NonNull String tapIdentifier) {
        if (!isV2Tap(tapIdentifier) && !isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_ENABLE_TEXT_MODE)) {
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

        if (!isV2Tap(tapIdentifier) && !isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_CONTROLLER_WITH_MOUSEHID)) {
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

        if (!isV2Tap(tapIdentifier) && !isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_CONTROLLER_WITH_FULLHID)) {
            logError("FEATURE_CONTROLLER_WITH_FULLHID not supported - " + tapIdentifier + ", Falling back to Controller mode");
            startControllerMode(tapIdentifier);
            return;
        }
        log("Starting Controller with Keyboard HID mode - " + tapIdentifier);
        startMode(tapIdentifier, TapInputMode.controllerWithFullHID());
    }

    public void startXRUserControlState(@NonNull String tapIdentifier) {
        this.startXRSTate(tapIdentifier, TapXRState.userControl());
    }

    public void startXRAirMouseState(@NonNull String tapIdentifier) {
        this.startXRSTate(tapIdentifier, TapXRState.airMouse());
    }

    public void startXRTappingState(@NonNull String tapIdentifier) {
        this.startXRSTate(tapIdentifier, TapXRState.tapping());
    }
    public void requestShiftSwitchState(@NonNull String tapIdentifier) {
        if (isV2Tap(tapIdentifier)) {
            logError("Shift/Switch state is not available on V2 Tap devices - " + tapIdentifier);
            return;
        }
        if (!isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_CONTROLLER_WITH_FULLHID)) {
            logError("FEATURE_CONTROLLER_WITH_FULLHID not supported - " + tapIdentifier + ", Can't request SwitchShift state");
            startControllerMode(tapIdentifier);
            return;
        }
        log("Requesting Shift/Switch state - " + tapIdentifier);
        tapBluetoothManager.requestShiftSwitchState(tapIdentifier);
    }

    public void requestTap(@NonNull String tapIdentifier, byte combination) {
        if (isV2Tap(tapIdentifier)) {
            logError("requestTap is not available on V2 Tap devices - " + tapIdentifier);
            return;
        }
        if (!isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_CONTROLLER_WITH_FULLHID)) {
            logError("FEATURE_CONTROLLER_WITH_FULLHID not supported - " + tapIdentifier + ", Can't request setTap");
            startControllerMode(tapIdentifier);
            return;
        }
        log("Requesting Tap - " + combination + ", on tap: " + tapIdentifier);
        tapBluetoothManager.requestTap(tapIdentifier, combination);
    }

    public void startRawSensorMode(@NonNull String tapIdentifier, byte deviceAccelerometerSensitivity, byte imuGyroSensitivity, byte imuAccelerometerSensitivity) {

        if (!isV2Tap(tapIdentifier) && !isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_RAW_SENSOR)) {
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
        Log.i("TAPSDK", "CLOSE!!!");
        isClosing = true;
        stopRawModeLoop();
        tapBluetoothManager.close();
        modeSubscribers.clear();

        handleCloseReset();
    }


    public void enableModes() {
        tapBluetoothManager.enableModes();
    }

    public void disableModes() {
        tapBluetoothManager.disableModes();
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
            detectProtocol(tapAddress);
            handleEmission(tapAddress);
        }

        @Override
        public void onTapAlreadyConnected(@NonNull String tapAddress) {
            detectProtocol(tapAddress);
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

            byte deviceAccelerometerSensitivity;
            byte imuGyroSensitivity;
            byte imuAccelerometerSensitivity;

            if (isV2Tap(tapAddress)) {
                // V2 devices stream raw IMU data only; sensitivity is configured through
                // setImuSensitivity (or raw sensor mode) and tracked per device
                ImuSensitivity sensitivity = v2ImuSensitivities.get(tapAddress);
                if (sensitivity == null) {
                    sensitivity = new ImuSensitivity(0, 0);
                }
                deviceAccelerometerSensitivity = 0;
                imuGyroSensitivity = (byte) sensitivity.getGyro();
                imuAccelerometerSensitivity = (byte) sensitivity.getAccelerometer();
            } else if (modeSubscribers.containsKey(tapAddress)) {
                TapInputMode mode = modeSubscribers.get(tapAddress);
                deviceAccelerometerSensitivity = mode.getDeviceAccelerometerSensitivity();
                imuGyroSensitivity = mode.getImuGyroSensitivity();
                imuAccelerometerSensitivity = mode.getImuAccelerometerSensitivity();
            } else {
                return;
            }

            ArrayList<RawSensorData> rsData = RawSensorDataParser.parseWhole(tapAddress, data,
                    deviceAccelerometerSensitivity, imuGyroSensitivity, imuAccelerometerSensitivity);

            for (RawSensorData rsDatum : rsData) {
                notifyOnRawSensorDataReceieved(tapAddress, rsDatum);
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
        public void onV2InputSubscribed(@NonNull String tapAddress) {
            cache.onV2InputSubscribed(tapAddress);
            handleEmission(tapAddress);
        }

        @Override
        public void onImuMotionInputReceived(@NonNull String tapAddress, @NonNull ImuMotionPacket packet) {
            if (isPaused || isClosing) {
                return;
            }
            notifyOnImuMotionInputReceived(tapAddress, packet);
        }

        @Override
        public void onStandbyStateReceived(@NonNull String tapAddress, boolean standby) {
            resolveV2Request(tapAddress, "standby", standby);
            notifyOnTapStandbyStateChanged(tapAddress, standby);
        }

        @Override
        public void onV2ConfigStateReceived(@NonNull String tapAddress, @NonNull TapV2Message message) {
            switch (message.type) {
                case CONFIG_FEATURE:
                    resolveV2Request(tapAddress, "feature:" + (message.payload[0] & 0xFF),
                            message.payload[1] == 1);
                    break;
                case CONFIG_VISION_OP_MODE:
                    resolveV2Request(tapAddress, "vision_op_mode",
                            VisionSensorOpMode.fromValue(message.payload[0] & 0xFF));
                    break;
                case CONFIG_VISION_MODEL:
                    resolveV2Request(tapAddress, "vision_model",
                            VisionSensorModel.fromValue(message.payload[0] & 0xFF));
                    break;
                case CONFIG_IMU_SENSITIVITY:
                    // Reply payload is [gyro, accelerometer], same order as the set command
                    resolveV2Request(tapAddress, "imu_sensitivity",
                            new ImuSensitivity(message.payload[0] & 0xFF, message.payload[1] & 0xFF));
                    break;
                default:
                    log("Unhandled V2 config state - " + message.type);
                    break;
            }
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

    private void notifyOnImuMotionInputReceived(@NonNull final String tapIdentifier, @NonNull final ImuMotionPacket packet) {
        tapListeners.notifyAll(listener -> listener.onImuMotionInputReceived(tapIdentifier, packet));
    }

    private void notifyOnTapStandbyStateChanged(@NonNull final String tapIdentifier, final boolean standby) {
        tapListeners.notifyAll(listener -> listener.onTapStandbyStateChanged(tapIdentifier, standby));
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

    private void detectProtocol(@NonNull String tapIdentifier) {
        // Only decide once GATT service discovery completed, so the V2
        // characteristics are visible if the device has them
        if (tapBluetoothManager.isProtocolDetectionReady(tapIdentifier)) {
            String protocol = tapBluetoothManager.isV2Tap(tapIdentifier) ? TapCache.PROTOCOL_V2 : TapCache.PROTOCOL_V1;
            cache.onProtocolDetected(tapIdentifier, protocol);
            log("Detected protocol " + protocol + " - " + tapIdentifier);
        }
    }

    private void handleTapConnection(@NonNull String tapIdentifier) {

        if (isPaused || isClosing) {
            return;
        }

        startMode(tapIdentifier, autoSetModeOnConnection);
        startXRSTate(tapIdentifier, defaultXRState);
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
        stateSubscribers.remove(tapIdentifier);
        v2ImuSensitivities.remove(tapIdentifier);
        cancelV2Requests(tapIdentifier);
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

    protected void handleEmission(@NonNull String tapIdentifier) {
        Log.i("TAPSDK", "HANDLE EMISSION CALL " + tapIdentifier);
        if (!cache.isCached(tapIdentifier)) {
            handleCacheDependencies(tapIdentifier);
            return;
        }
        Log.i("TAPSDK", "HANDLE EMISSION COMPLETED " + tapIdentifier);
        if (isTapConnected(tapIdentifier)) {
            handleTapConnection(tapIdentifier);
        } else {
            handleTapDisconnection(tapIdentifier);
        }
    }


    public boolean handleCacheDependencies(@NonNull String tapIdentifier) {

        if (!cache.has(tapIdentifier, TapCache.DataKey.Name) && cache.shouldHave(tapIdentifier, TapCache.DataKey.Name)) {
            tapBluetoothManager.readName(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.Battery) && cache.shouldHave(tapIdentifier, TapCache.DataKey.Battery)) {

            tapBluetoothManager.readBattery(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.SerialNumber) && cache.shouldHave(tapIdentifier, TapCache.DataKey.SerialNumber)) {

            tapBluetoothManager.readSerialNumber(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.HwVer) && cache.shouldHave(tapIdentifier, TapCache.DataKey.HwVer)) {
            tapBluetoothManager.readHwVer(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.FwVer) && cache.shouldHave(tapIdentifier, TapCache.DataKey.FwVer)) {
            tapBluetoothManager.readFwVer(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.BootloaderVer) && cache.shouldHave(tapIdentifier, TapCache.DataKey.BootloaderVer)) {

            tapBluetoothManager.readBootloaderVer(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.TapNotification) && cache.shouldHave(tapIdentifier, TapCache.DataKey.TapNotification)) {
            tapBluetoothManager.setupTapNotification(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.MouseNotification) && cache.shouldHave(tapIdentifier, TapCache.DataKey.MouseNotification)) {
            tapBluetoothManager.setupMouseNotification(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.AirMouseNotification) && cache.shouldHave(tapIdentifier, TapCache.DataKey.AirMouseNotification)) {

            tapBluetoothManager.setupAirMouseNotification(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.RawSensorNotification) && cache.shouldHave(tapIdentifier, TapCache.DataKey.RawSensorNotification)) {

            tapBluetoothManager.setupRawSensorNotification(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.DataRequestNotification) && cache.shouldHave(tapIdentifier, TapCache.DataKey.DataRequestNotification)) {
            tapBluetoothManager.setupDataNotification(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.V2Notification) && cache.shouldHave(tapIdentifier, TapCache.DataKey.V2Notification)) {
            tapBluetoothManager.setupV2Notification(tapIdentifier);
        } else {
            return true;
        }
        return false;
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

                // V2 devices don't need periodic mode refresh - they need a keepalive
                // message on the framed pipe instead, sent to every connected V2 device
                // whether or not a mode was requested (matches tap-ios-sdk's global
                // keepalive timer and tap-python-sdk's KeepAliveManager)
                for (String tapIdentifier : getConnectedTaps()) {
                    if (isV2Tap(tapIdentifier)) {
                        tapBluetoothManager.sendV2KeepAlive(tapIdentifier);
                    }
                }

                for (String tapIdentifier: modeSubscribers.keySet()) {
                    if (isV2Tap(tapIdentifier)) {
                        continue;
                    }
                    tapBluetoothManager.startMode(tapIdentifier, modeSubscribers.get(tapIdentifier).getBytes());
//                    startControllerMode(tapIdentifier);
                }

                for (String tapIdentifier : stateSubscribers.keySet()) {
                    if (isV2Tap(tapIdentifier)) {
                        continue;
                    }
                    TapXRState state = stateSubscribers.get(tapIdentifier);
                    tapBluetoothManager.startXRState(tapIdentifier, stateSubscribers.get(tapIdentifier).getBytes());
                    if (state.type == TapXRState.USER_CONTROL) {
                        stateSubscribers.put(tapIdentifier, TapXRState.none());
                    }
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
