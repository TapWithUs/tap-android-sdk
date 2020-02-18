package com.tapwithus.tapsdk;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.tapwithus.sdk.TapListener;
import com.tapwithus.sdk.TapSdk;
import com.tapwithus.sdk.TapSdkFactory;
import com.tapwithus.sdk.airmouse.AirMousePacket;
import com.tapwithus.sdk.mode.TapInputMode;
import com.tapwithus.sdk.mouse.MousePacket;
import com.tapwithus.sdk.tap.Tap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TapSdk sdk;
    private RecyclerViewAdapter adapter;
    private boolean startWithControllerMode = false;
    private String lastConnectedTapAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        log("onCreate");

        sdk = TapSdkFactory.getDefault(this);
        sdk.enableDebug();
        if (!startWithControllerMode) {
            sdk.setDefaultMode(TapInputMode.text(), true);
        }
        sdk.registerTapListener(tapListener);
        if (sdk.isConnectionInProgress()) {
            log("A Tap is connecting");
        }

        initRecyclerView();

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                sdk.setMouseNotification("D7:A9:E0:8C:17:6E");
//                String tapIdentifier = "D7:A9:E0:8C:17:6E";
//                sdk.writeName(tapIdentifier, "YanivWithCase");

//                sdk.restartBluetooth();
//                sdk.refreshBond(lastConnectedTapAddress);

                if (sdk.isTapIgnored(lastConnectedTapAddress)) {
                    sdk.unignoreTap(lastConnectedTapAddress);
                } else {
                    sdk.ignoreTap(lastConnectedTapAddress);
                }
            }
        });
    }

    private TapListItemOnClickListener itemOnClickListener = new TapListItemOnClickListener() {
        @Override
        public void onClick(TapListItem item) {
            final String tapIdentifier = item.tapIdentifier;
//            askModeDialog(new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    // do something like...
//                    log("Switching to TEXT mode");
//                    sdk.startMode(tapIdentifier, TapSdk.MODE_TEXT);
//                }
//            },
//                    new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            // do something like...
//                            log("Switching to CONTROLLER mode with MOUSEHID");
//                            sdk.setMouseHIDEnabledInRawMode(tapIdentifier, true);
//                            sdk.startMode(tapIdentifier, TapSdk.MODE_CONTROLLER);
//                        }
//                    },
//
//                    new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            // do something like...
//                            log("Switching to CONTROLLER mode without MOUSEHID");
//                            sdk.setMouseHIDEnabledInRawMode(tapIdentifier, false);
//                            sdk.startMode(tapIdentifier, TapSdk.MODE_CONTROLLER);
//                        }
//                    }
//            );
        askModeDialog(item.tapIdentifier);


//            if (item.isInControllerMode) {
//                log("Switching to TEXT mode");
//                sdk.startMode(item.tapIdentifier, TapSdk.MODE_TEXT);
//            } else {
//                log("Switching to CONTROLLER mode");
//                sdk.setMouseHIDEnabledInRawMode(item.tapIdentifier, true);
//                sdk.startMode(item.tapIdentifier, TapSdk.MODE_CONTROLLER);
//            }
        }
    };

    private void askModeDialog(final String tapIdentifier) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change TAP Mode");

        String[] options = {"Text Mode", "Controller", "Controller with Mouse HID"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // TextMode
                        log("Switching to TEXT mode");
                        sdk.startTextMode(tapIdentifier);
//                        sdk.startMode(tapIdentifier, TapSdk.MODE_TEXT);
                        break;
                        
                    case 1: // Controller Mode With Mouse HID
                        log("Switching to CONTROLLER mode with MOUSEHID");
//                        sdk.setMouseHIDEnabledInRawMode(tapIdentifier, true);
//                        sdk.startMode(tapIdentifier, TapSdk.MODE_CONTROLLER_WITH_MOUSEHID);
                        sdk.startControllerWithMouseHIDMode(tapIdentifier);
                        break;
                    case 2: // Controller Mode Without Mouse HID
                        log("Switching to CONTROLLER mode without MOUSEHID");
//                        sdk.setMouseHIDEnabledInRawMode(tapIdentifier, false);
//                        sdk.startMode(tapIdentifier, TapSdk.MODE_CONTROLLER);
                        sdk.startControllerMode(tapIdentifier);
                        break;
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void askModeDialog(DialogInterface.OnClickListener textModeListener, DialogInterface.OnClickListener controllerWithMouseHIDListener, DialogInterface.OnClickListener controllerWithoutMouseHIDListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change TAP Mode");
//        builder.setMessage("Would you like to continue learning how to use Android alerts?");
        // add the buttons
        if (textModeListener != null) {
            builder.setNeutralButton("Text", textModeListener);
        }
        if (controllerWithMouseHIDListener != null) {
            builder.setNeutralButton("Con w/", controllerWithMouseHIDListener);
        }
        if (controllerWithoutMouseHIDListener != null) {
            builder.setNeutralButton("Con w/o", controllerWithoutMouseHIDListener);
        }
        builder.setNegativeButton("Cancel", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @Override
    protected void onResume() {
        super.onResume();

        log("onResume");

        Set<String> connectedTaps = sdk.getConnectedTaps();
        List<TapListItem> listItems = new ArrayList<>();
        for (String tapIdentifier: connectedTaps) {
            TapListItem tapListItem = new TapListItem(tapIdentifier, itemOnClickListener);
//            tapListItem.isInControllerMode = sdk.isInMode(tapIdentifier, TapSdk.MODE_CONTROLLER);
            listItems.add(tapListItem);
        }
        adapter.updateList(listItems);

        sdk.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        log("onPause");
        sdk.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        log("onDestroy");
        sdk.unregisterTapListener(tapListener);
    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sdk.close();
                        int pid = android.os.Process.myPid();
                        android.os.Process.killProcess(pid);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        List<TapListItem> listItems = new ArrayList<>();

        adapter = new RecyclerViewAdapter(listItems);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(adapter);
    }

    private TapListener tapListener = new TapListener() {

        @Override
        public void onBluetoothTurnedOn() {
            log("Bluetooth turned ON");
        }

        @Override
        public void onBluetoothTurnedOff() {
            log("Bluetooth turned OFF");
        }

        @Override
        public void onTapStartConnecting(@NonNull String tapIdentifier) {
            log("Tap started connecting - " + tapIdentifier);
        }

        @Override
        public void onTapConnected(@NonNull String tapIdentifier) {
            log("TAP connected " + tapIdentifier);
            Tap tap = sdk.getCachedTap(tapIdentifier);
            if (tap == null) {
                log("Unable to get cached Tap");
                return;
            }

            lastConnectedTapAddress = tapIdentifier;
            log(tap.toString());

            adapter.removeItem(tapIdentifier);

            TapListItem newItem = new TapListItem(tapIdentifier, itemOnClickListener);
            newItem.tapName = tap.getName();
            newItem.tapFwVer = tap.getFwVer();
//            newItem.isInControllerMode = sdk.isInMode(tapIdentifier, TapSdk.MODE_CONTROLLER);
            adapter.addItem(newItem);
        }

        @Override
        public void onTapDisconnected(@NonNull String tapIdentifier) {
            log("TAP disconnected " + tapIdentifier);
            adapter.removeItem(tapIdentifier);
        }

        @Override
        public void onTapResumed(@NonNull String tapIdentifier) {
            log("TAP resumed " + tapIdentifier);
            Tap tap = sdk.getCachedTap(tapIdentifier);
            if (tap == null) {
                log("Unable to get cached Tap");
                return;
            }

            log(tap.toString());
            adapter.updateFwVer(tapIdentifier, tap.getFwVer());
        }

        @Override
        public void onTapChanged(@NonNull String tapIdentifier) {
            log("TAP changed " + tapIdentifier);
            Tap tap = sdk.getCachedTap(tapIdentifier);
            if (tap == null) {
                log("Unable to get cached Tap");
                return;
            }

            log("TAP changed " + tap);
            adapter.updateFwVer(tapIdentifier, tap.getFwVer());
        }

//        @Override
//        public void onControllerModeStarted(@NonNull String tapIdentifier) {
//            log("Controller mode started " + tapIdentifier);
//            adapter.onControllerModeStarted(tapIdentifier);
//        }
//
//        @Override
//        public void onTextModeStarted(@NonNull String tapIdentifier) {
//            log("Text mode started " + tapIdentifier);
//            adapter.onTextModeStarted(tapIdentifier);
//        }

        @Override
        public void onTapInputReceived(@NonNull String tapIdentifier, int data) {
            adapter.updateTapInput(tapIdentifier, data);
        }

        @Override
        public void onMouseInputReceived(@NonNull String tapIdentifier, @NonNull MousePacket data) {
//            log(tapIdentifier + " mouse input received " + data.dx.getInt() + " " + data.dy.getInt() + " " + data.dt.getUnsignedLong() + " " + data.proximity.getInt());
        }

        @Override
        public void onAirMouseInputReceived(@NonNull String tapIdentifier, @NonNull AirMousePacket data) {
            log(tapIdentifier + " air mouse input received " + data.gesture.getInt());
        }

        @Override
        public void onRawSensorInputReceived(@NonNull String tapIdentifier, @NonNull int data) {
            log(tapIdentifier + " raw sensor input received");
        }

        @Override
        public void onTapChangedState(@NonNull String tapIdentifier, @NonNull int state) {
            log(tapIdentifier + " changed state: " + state);
        }

        @Override
        public void onError(@NonNull String tapIdentifier, int code, @NonNull String description) {
            log("Error - " + tapIdentifier + " - " + code + " - " + description);
        }
    };

    private void log(String message) {
        Log.e(this.getClass().getSimpleName(), message);
    }
}
