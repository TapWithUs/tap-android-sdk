package com.tapwithus.tapsdk;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.tapwithus.sdk.TapListener;
import com.tapwithus.sdk.TapSdk;
import com.tapwithus.sdk.TapSdkFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private TapSdk tapSdk;
    private RecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        log("onCreate");

        tapSdk = TapSdkFactory.getDefault(this);
        tapSdk.enableDebug();

        initRecyclerView();

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tapSdk.refreshConnections();
            }
        });
    }

    private TapListItemOnClickListener itemOnClickListener = new TapListItemOnClickListener() {
        @Override
        public void onClick(TapListItem item) {
            if (item.isInControllerMode) {
                tapSdk.startMode(item.tapIdentifier, TapSdk.MODE_TEXT);
            } else {
                tapSdk.startMode(item.tapIdentifier, TapSdk.MODE_CONTROLLER);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        List<String> connectedTaps = tapSdk.getConnectedTaps();
        List<TapListItem> listItems = new ArrayList<>();
        for (String tapIdentifier: connectedTaps) {
            TapListItem tapListItem = new TapListItem(tapIdentifier, itemOnClickListener);
            tapListItem.isInControllerMode = tapSdk.isInMode(tapIdentifier, TapSdk.MODE_CONTROLLER);
            listItems.add(tapListItem);
        }
        adapter.updateList(listItems);

        tapSdk.registerTapListener(tapListener);
        tapSdk.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        tapSdk.pause();
        tapSdk.unregisterTapListener(tapListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        log("onDestroy");
        tapSdk.unregisterTapListener(tapListener);
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
        public void onTapConnected(String tapIdentifier) {
            log("TAP connected " + tapIdentifier);
            tapSdk.readName(tapIdentifier);
        }

        @Override
        public void onTapDisconnected(String tapIdentifier) {
            log("TAP disconnected " + tapIdentifier);
            adapter.removeItem(tapIdentifier);
        }

        @Override
        public void onNameRead(String tapIdentifier, String name) {
            log(tapIdentifier + " Name read " + name);

            TapListItem newItem = new TapListItem(tapIdentifier, itemOnClickListener);
            newItem.tapName = name;

            int mode = tapSdk.getMode(tapIdentifier);
            switch (mode) {
                case TapSdk.MODE_TEXT:
                    newItem.isInControllerMode = false;
                    break;
                case TapSdk.MODE_CONTROLLER:
                    newItem.isInControllerMode = true;
                    break;
            }

            adapter.addItem(newItem);
        }

        @Override
        public void onNameWrite(String tapIdentifier, String name) {
            log(tapIdentifier + " Name write " + name);
        }

        @Override
        public void onCharacteristicRead(String tapIdentifier, UUID characteristic, byte[] data) {
            log(tapIdentifier + " " + characteristic.toString() + " Characteristic read " + Arrays.toString(data));
        }

        @Override
        public void onCharacteristicWrite(String tapIdentifier, UUID characteristic, byte[] data) {
            log(tapIdentifier + " " + characteristic.toString() + " Characteristic write " + Arrays.toString(data));
        }

        @Override
        public void onControllerModeStarted(String tapIdentifier) {
            log("Controller mode started " + tapIdentifier);
            adapter.onControllerModeStarted(tapIdentifier);
        }

        @Override
        public void onTextModeStarted(String tapIdentifier) {
            log("Text mode started " + tapIdentifier);
            adapter.onTextModeStarted(tapIdentifier);
        }

        @Override
        public void onTapInputReceived(String tapIdentifier, int data) {
            log(tapIdentifier + " TAP input received " + String.valueOf(data));
            adapter.updateTapInput(tapIdentifier, data);
        }
    };

    private void log(String message) {
        Log.e(this.getClass().getSimpleName(), message);
    }
}
