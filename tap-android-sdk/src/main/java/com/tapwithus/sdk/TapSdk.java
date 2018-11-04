package com.tapwithus.sdk;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.tapwithus.sdk.bluetooth.MousePacket;
import com.tapwithus.sdk.bluetooth.TapBluetoothListener;
import com.tapwithus.sdk.bluetooth.TapBluetoothManager;
import com.tapwithus.sdk.tap.Tap;
import com.tapwithus.sdk.tap.TapCache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TapSdk {

    private static final int NUM_OF_MODES = 2;
    public static final int MODE_TEXT = 1;
    public static final int MODE_CONTROLLER = 2;

    private static final int RAW_MODE_LOOP_DELAY = 10000;

    private static final String TAG = "TapSdk";

    public static final int ERR_SUBSCRIBE_MODE = 101;
    public static final int ERR_INPUT_RECEIVED = 101;

    protected TapBluetoothManager tapBluetoothManager;
    private ListenerManager<TapListener> tapListeners = new ListenerManager<>();
    private Map<String, Integer> modeSubscribers = new ConcurrentHashMap<>();
    private List<String> startModeNotificationSubscribers = new CopyOnWriteArrayList<>();
    private List<String> notifyOnConnectedAfterControllerModeStarted = new CopyOnWriteArrayList<>();
    private List<String> notifyOnResumedAfterControllerModeStarted = new CopyOnWriteArrayList<>();
    private boolean debug = false;
    private boolean isClosing = false;
    private boolean isClosed = false;
    private boolean isPaused = false;
    private boolean autoSetControllerModeOnConnection = true;

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

    public void enableAutoSetControllerModeOnConnection() {
        autoSetControllerModeOnConnection = true;
    }

    public void disableAutoSetControllerModeOnConnection() {
        autoSetControllerModeOnConnection = false;
    }

    public void enablePauseResumeHandling() {
        pauseResumeHandling = true;
    }

    public void disablePauseResumeHandling() {
        pauseResumeHandling = false;
    }

    public void resume() {
        isPaused = false;

        if (!pauseResumeHandling) {
            return;
        }

        Set<String> actuallyConnectTaps = getConnectedTaps();

        for (String tapIdentifier: getTapsInMode(MODE_CONTROLLER)) {
            if (actuallyConnectTaps.contains(tapIdentifier)) {
                notifyOnResumedAfterControllerModeStarted.add(tapIdentifier);
                startControllerMode(tapIdentifier);
            }
        }

        for (String tapIdentifier: getTapsInMode(MODE_TEXT)) {
            if (actuallyConnectTaps.contains(tapIdentifier)) {
                notifyOnTapResumed(tapIdentifier);
            }
        }

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

        List<String> controllerModeSubscribers = getTapsInMode(MODE_CONTROLLER);
        for (String tapIdentifier: controllerModeSubscribers) {
            tapBluetoothManager.startTextMode(tapIdentifier);
        }
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

    public void startMode(String tapIdentifier, int mode) {
        if (!isModeValid(mode)) {
            notifyOnError(tapIdentifier, ERR_SUBSCRIBE_MODE, "Invalid mode passed");
            return;
        }

        startModeNotificationSubscribers.add(tapIdentifier);
        switch (mode) {
            case MODE_TEXT:
                startTextMode(tapIdentifier);
                break;
            case MODE_CONTROLLER:
                startControllerMode(tapIdentifier);
                break;
        }
    }

    public void restartBluetooth() {
        tapBluetoothManager.restartBluetooth();
    }

    public void refreshBond(@NonNull String tapIdentifier) {
        tapBluetoothManager.refreshBond(tapIdentifier);
    }

    private void startControllerMode(@NonNull String tapIdentifier) {
        log("Starting Controller mode - " + tapIdentifier);
        modeSubscribers.put(tapIdentifier, MODE_CONTROLLER);
        tapBluetoothManager.startControllerMode(tapIdentifier);
    }

    private void startTextMode(@NonNull String tapIdentifier) {
        if (!isFeatureSupported(tapIdentifier, FeatureVersionSupport.FEATURE_ENABLE_TEXT_MODE)) {
            logError("FEATURE_ENABLE_TEXT_MODE not supported - " + tapIdentifier);
            startModeNotificationSubscribers.remove(tapIdentifier);
            return;
        }

        if (!modeSubscribers.containsKey(tapIdentifier) || modeSubscribers.get(tapIdentifier) != MODE_TEXT) {
            log("Starting Text mode - " + tapIdentifier);
            modeSubscribers.put(tapIdentifier, MODE_TEXT);
            tapBluetoothManager.startTextMode(tapIdentifier);
        }
    }

    private boolean isModeValid(int mode) {
        return mode >= 1 && mode >> NUM_OF_MODES <= 0;
    }

    public int getMode(String tapIdentifier) {
        return modeSubscribers.containsKey(tapIdentifier) ? modeSubscribers.get(tapIdentifier) : 0;
    }

    public List<String> getTapsInMode(int mode) {
        List<String> taps = new ArrayList<>();

        if (isModeValid(mode)) {
            for (Map.Entry<String, Integer> entry : modeSubscribers.entrySet()) {
                String tapIdentifier = entry.getKey();
                if (isInMode(tapIdentifier, mode)) {
                    taps.add(tapIdentifier);
                }
            }
        }

        return taps;
    }

    public boolean isInMode(String tapIdentifier, int mode) {
        if (!modeSubscribers.containsKey(tapIdentifier)) {
            return false;
        }
        return (modeSubscribers.get(tapIdentifier) & mode) == mode;
    }

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

    @SuppressWarnings("FieldCanBeLocal")
    private TapBluetoothListener tapBluetoothListener = new TapBluetoothListener() {

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
        public void onControllerModeStarted(@NonNull String tapAddress) {
            if (notifyOnConnectedAfterControllerModeStarted.contains(tapAddress)) {
                notifyOnConnectedAfterControllerModeStarted.remove(tapAddress);
                notifyOnTapConnected(tapAddress);
            } else if (notifyOnResumedAfterControllerModeStarted.contains(tapAddress)) {
                notifyOnResumedAfterControllerModeStarted.remove(tapAddress);
                notifyOnTapResumed(tapAddress);
            } else {
                notifyOnControllerModeStarted(tapAddress);
            }

        }

        @Override
        public void onTextModeStarted(@NonNull String tapAddress) {
            if (notifyOnConnectedAfterControllerModeStarted.contains(tapAddress)) {
                notifyOnConnectedAfterControllerModeStarted.remove(tapAddress);
                notifyOnTapConnected(tapAddress);
            } else {
                notifyOnTextModeStarted(tapAddress);
            }
        }

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
        public void onTapInputReceived(@NonNull String tapAddress, int data) {
            notifyOnTapInputReceived(tapAddress, data);
        }

        @Override
        public void onMouseInputReceived(@NonNull String tapAddress, @NonNull MousePacket data) {
            notifyOnMouseInputReceived(tapAddress, data);
        }

        @Override
        public void onError(@NonNull String tapAddress, int code, @NonNull String description) {
            notifyOnError(tapAddress, code, description);
        }
    };

    private void notifyOnBluetoothTurnedOn() {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onBluetoothTurnedOn();
            }
        });
    }

    private void notifyOnBluetoothTurnedOff() {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onBluetoothTurnedOff();
            }
        });
    }

    private void notifyOnTapStartConnecting(@NonNull final String tapIdentifier) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTapStartConnecting(tapIdentifier);
            }
        });
    }

    private void notifyOnTapConnected(@NonNull final String tapIdentifier) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTapConnected(tapIdentifier);
            }
        });
    }

    private void notifyOnTapDisconnected(@NonNull final String tapIdentifier) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTapDisconnected(tapIdentifier);
            }
        });
    }

    private void notifyOnTapResumed(@NonNull final String tapIdentifier) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTapResumed(tapIdentifier);
            }
        });
    }

    private void notifyOnTapChanged(@NonNull final String tapIdentifier) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTapChanged(tapIdentifier);
            }
        });
    }

    private void notifyOnControllerModeStarted(@NonNull final String tapIdentifier) {
        if (!startModeNotificationSubscribers.contains(tapIdentifier)) {
            return;
        }
        startModeNotificationSubscribers.remove(tapIdentifier);

        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onControllerModeStarted(tapIdentifier);
            }
        });
    }

    private void notifyOnTextModeStarted(@NonNull final String tapIdentifier) {
        if (!startModeNotificationSubscribers.contains(tapIdentifier)) {
            return;
        }
        startModeNotificationSubscribers.remove(tapIdentifier);

        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTextModeStarted(tapIdentifier);
            }
        });
    }

    private void notifyOnTapInputReceived(@NonNull final String tapIdentifier, final int data) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onTapInputReceived(tapIdentifier, data);
            }
        });
    }

    private void notifyOnMouseInputReceived(@NonNull final String tapIdentifier, @NonNull final MousePacket data) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onMouseInputReceived(tapIdentifier, data);
            }
        });
    }

    private void notifyOnError(final String tapIdentifier, final int code, final String description) {
        tapListeners.notifyAll(new NotifyAction<TapListener>() {
            @Override
            public void onNotify(TapListener listener) {
                listener.onError(tapIdentifier, code, description);
            }
        });
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

        List<String> textModeSubscribers = getTapsInMode(MODE_TEXT);
        if (textModeSubscribers.contains(tapIdentifier) || !autoSetControllerModeOnConnection) {
            modeSubscribers.put(tapIdentifier, MODE_TEXT);
            notifyOnTapConnected(tapIdentifier);
        } else {
            notifyOnConnectedAfterControllerModeStarted.add(tapIdentifier);
            startControllerMode(tapIdentifier);
        }
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
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.TapNotification)) {
            tapBluetoothManager.setupTapNotification(tapIdentifier);
        } else if (!cache.has(tapIdentifier, TapCache.DataKey.MouseNotification)) {
            tapBluetoothManager.setupMouseNotification(tapIdentifier);
        } else {
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
                for (String tapIdentifier: getTapsInMode(MODE_CONTROLLER)) {
                    startControllerMode(tapIdentifier);
                }
                rawModeHandler.postDelayed(this, RAW_MODE_LOOP_DELAY);
            }
        };

//        rawModeHandler.postDelayed(rawModeRunnable, 0);
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
