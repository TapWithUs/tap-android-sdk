package com.tapwithus.tapsdk;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tapwithus.sdk.TapListener;
import com.tapwithus.sdk.TapSdk;
import com.tapwithus.sdk.TapSdkFactory;
import com.tapwithus.sdk.airmouse.AirMousePacket;
import com.tapwithus.sdk.mode.RawSensorData;
import com.tapwithus.sdk.mouse.MousePacket;
import com.tapwithus.sdk.v2.DeviceFeature;
import com.tapwithus.sdk.v2.ImuMotionPacket;
import com.tapwithus.sdk.v2.UnifiedAirGesture;
import com.tapwithus.tapsdk.sample.DpadStateMachine;
import com.tapwithus.tapsdk.sample.KnobTracker;

import android.widget.TextView;

import java.util.ArrayDeque;

/**
 * V2 knob and D-Pad sample. Uses {@link KnobTracker} and {@link DpadStateMachine}
 * from the tap-android-knob and tap-android-dpad skills.
 */
public class GestureSampleActivity extends AppCompatActivity {

    private static final String[] KNOB_NAMES = {"AB", "AC", "AD", "AE"};

    private TapSdk sdk;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final KnobTracker knob = new KnobTracker(2.0, 4);
    private final DpadStateMachine dpad = new DpadStateMachine();
    private final int[] knobValues = {50, 50, 50, 50};
    private final ArrayDeque<String> dpadLog = new ArrayDeque<>();

    private TextView statusView;
    private TextView knobView;
    private TextView dpadLiveView;
    private TextView dpadLogView;
    private String activeId;

    private final TapListener listener = new TapListener() {
        @Override public void onBluetoothTurnedOn() { }
        @Override public void onBluetoothTurnedOff() {
            main.post(() -> statusView.setText("Bluetooth is off"));
        }
        @Override public void onTapStartConnecting(@NonNull String tapIdentifier) { }
        @Override public void onTapConnected(@NonNull String tapIdentifier) {
            main.post(() -> arm(tapIdentifier));
        }
        @Override public void onTapDisconnected(@NonNull String tapIdentifier) {
            main.post(() -> {
                if (tapIdentifier.equals(activeId)) {
                    activeId = null;
                    statusView.setText("Disconnected");
                }
            });
        }
        @Override public void onTapResumed(@NonNull String tapIdentifier) { }
        @Override public void onTapChanged(@NonNull String tapIdentifier) { }
        @Override public void onTapInputReceived(@NonNull String tapIdentifier, int data, int repeatData) { }
        @Override public void onTapShiftSwitchReceived(@NonNull String tapIdentifier, int data) { }
        @Override public void onMouseInputReceived(@NonNull String tapIdentifier, @NonNull MousePacket data) { }
        @Override public void onRawSensorInputReceived(@NonNull String tapIdentifier, @NonNull RawSensorData rsData) { }
        @Override public void onTapChangedState(@NonNull String tapIdentifier, int state) { }
        @Override public void onError(@NonNull String tapIdentifier, int code, @NonNull String description) {
            main.post(() -> statusView.setText("Error " + code + ": " + description));
        }

        @Override
        public void onAirMouseInputReceived(@NonNull final String tapIdentifier, @NonNull AirMousePacket data) {
            if (!sdk.isV2Tap(tapIdentifier)) {
                return;
            }
            UnifiedAirGesture gesture = UnifiedAirGesture.fromCode(data.gesture.getInt());
            if (gesture == null) {
                return;
            }
            final int code = gesture.getCode();
            final long now = System.nanoTime();
            main.post(() -> {
                if (!tapIdentifier.equals(activeId)) {
                    return;
                }
                String knobEvent = knob.onGesture(code);
                if ("start".equals(knobEvent)) {
                    sdk.vibrate(tapIdentifier, new int[]{40});
                }
                for (DpadStateMachine.Event event : dpad.onGesture(code, now)) {
                    onDpad(tapIdentifier, event);
                }
                render();
            });
        }

        @Override
        public void onImuMotionInputReceived(@NonNull final String tapIdentifier, @NonNull ImuMotionPacket packet) {
            final int dx = packet.dx;
            final int dy = packet.dy;
            final int roll = packet.roll;
            final long now = System.nanoTime();
            main.post(() -> {
                if (!tapIdentifier.equals(activeId)) {
                    return;
                }
                int steps = knob.onRoll(roll);
                if (steps != 0 && knob.active != null) {
                    int index = knob.active;
                    knobValues[index] = Math.max(0, Math.min(100, knobValues[index] + steps));
                }
                for (DpadStateMachine.Event event : dpad.onMotion(dx, dy, roll, now)) {
                    onDpad(tapIdentifier, event);
                }
                render();
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture_sample);
        statusView = findViewById(R.id.gestureStatus);
        knobView = findViewById(R.id.knobValues);
        dpadLiveView = findViewById(R.id.dpadLive);
        dpadLogView = findViewById(R.id.dpadLog);
        sdk = TapSdkFactory.getDefault(this);
        sdk.registerTapListener(listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sdk.resume();
        String found = null;
        for (String id : sdk.getConnectedTaps()) {
            if (sdk.isV2Tap(id)) {
                found = id;
                break;
            }
        }
        if (found != null) {
            arm(found);
        } else if (sdk.getConnectedTaps().isEmpty()) {
            statusView.setText("Waiting for a V2 Tap… Pair it in Bluetooth settings.");
        } else {
            statusView.setText("Connected Tap is not V2. Knob and D-Pad need a TapBand or a V2 TapXR.");
        }
        render();
    }

    @Override
    protected void onPause() {
        sdk.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        sdk.unregisterTapListener(listener);
        super.onDestroy();
    }

    private void arm(String tapIdentifier) {
        if (!sdk.isV2Tap(tapIdentifier)) {
            statusView.setText("Connected Tap is not V2. Knob and D-Pad need a TapBand or a V2 TapXR.");
            return;
        }
        activeId = tapIdentifier;
        sdk.startControllerMode(tapIdentifier);
        sdk.startXRAirMouseState(tapIdentifier);
        sdk.setFeature(tapIdentifier, DeviceFeature.MODEL_DETECTION, true);
        sdk.setFeature(tapIdentifier, DeviceFeature.IMU_MOTION_DATA, true);
        statusView.setText("V2 " + tapIdentifier + " — air gestures + IMU");
    }

    private void onDpad(String tapIdentifier, DpadStateMachine.Event event) {
        if ("hold".equals(event.kind) || "mode".equals(event.kind) || "pinch".equals(event.kind)) {
            sdk.vibrate(tapIdentifier, new int[]{40});
        }
        if ("rotate".equals(event.kind) || "drag".equals(event.kind)) {
            dpadLiveView.setText(describe(event));
            return;
        }
        if ("release".equals(event.kind) || "show".equals(event.kind)) {
            dpadLiveView.setText("idle");
        }
        dpadLog.addLast(describe(event));
        while (dpadLog.size() > 12) {
            dpadLog.removeFirst();
        }
    }

    private void render() {
        StringBuilder knobs = new StringBuilder();
        for (int i = 0; i < knobValues.length; i++) {
            if (i > 0) {
                knobs.append('\n');
            }
            String marker = knob.active != null && knob.active == i ? " *" : "";
            knobs.append(KNOB_NAMES[i]).append(' ').append(knobValues[i]).append(marker);
        }
        knobView.setText(knobs.toString());
        StringBuilder log = new StringBuilder();
        for (String line : dpadLog) {
            if (log.length() > 0) {
                log.append('\n');
            }
            log.append(line);
        }
        dpadLogView.setText(log.toString());
        if (dpad.hidden) {
            dpadLiveView.setText("hidden");
        } else if (dpad.held == null && "idle".equals(dpadLiveView.getText().toString())) {
            dpadLiveView.setText("idle");
        }
    }

    private static String describe(DpadStateMachine.Event event) {
        if ("direction".equals(event.kind) || "mode".equals(event.kind)) {
            return event.kind + " " + event.name;
        }
        if ("pinch".equals(event.kind) || "hold".equals(event.kind) || "release".equals(event.kind)) {
            return event.kind + " " + event.index;
        }
        if ("drag".equals(event.kind)) {
            return "drag " + event.dx + "," + event.dy;
        }
        if ("rotate".equals(event.kind)) {
            return "rotate " + Math.round(event.degrees);
        }
        return event.kind;
    }
}
