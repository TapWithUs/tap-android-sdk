package com.tapwithus.tapsdk;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tapwithus.sdk.TapSdk;
import com.tapwithus.sdk.airmouse.AirMousePacket;
import com.tapwithus.sdk.v2.UnifiedAirGesture;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private List<TapListItem> dataSet;

    private boolean onBind;

    public RecyclerViewAdapter(List<TapListItem> dataSet) {
        this.dataSet = dataSet;
    }

    public void updateList(List<TapListItem> items) {
        if (!onBind) {
            dataSet.clear();
            dataSet.addAll(items);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_row, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        onBind = true;
        try {
            holder.bindTapListItem(dataSet.get(position));
        } catch (IndexOutOfBoundsException e) {
            Log.e("RecyclerViewAdapter", "Mmm... " + e.getMessage());
        } finally {
            onBind = false;
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public void addItem(TapListItem item) {
        for (TapListItem i : dataSet) {
            if (i.tapIdentifier.equals(item.tapIdentifier)) {
                return;
            }
        }
        if (!onBind) {
            dataSet.add(item);
            notifyItemInserted(dataSet.size());
        }
    }

    public void removeItem(String tapIdentifier) {
        int position;
        for (position = 0; position < dataSet.size(); position++) {
            if (dataSet.get(position).tapIdentifier.equals(tapIdentifier)) {
                if (!onBind) {
                    dataSet.remove(position);
                    notifyItemRemoved(position);
                }
                break;
            }
        }
    }

    public void updateTapInput(String tapIdentifier, int tapInputInt, int repeatDataInt) {
        if (tapInputInt == 0) {
            return;
        }

        for (int position = 0; position < dataSet.size(); position++) {
            TapListItem item = dataSet.get(position);
            if (item.tapIdentifier.equals(tapIdentifier)) {
                if (!onBind) {
                    item.tapInputInt = tapInputInt;
                    item.tapRepeatInt = repeatDataInt;
                    item.tapInputFingers = TapSdk.toFingers(tapInputInt);
                    notifyItemChanged(position);
                }
            }
        }
    }

    public void updateAirGesture(String tapIdentifier, int gestureInt) {
        for (int position = 0; position < dataSet.size(); position++) {
            TapListItem item = dataSet.get(position);
            if (item.tapIdentifier.equals(tapIdentifier)) {
                if (!onBind) {
                    item.airGestureInt = gestureInt;
                    notifyItemChanged(position);
                }
            }
        }
    }

    public void updateXRState(String tapIdentifier, boolean isAirMouseState) {
        for (int position = 0; position < dataSet.size(); position++) {
            TapListItem item = dataSet.get(position);
            if (item.tapIdentifier.equals(tapIdentifier)) {
                if (!onBind) {
                    item.isAirMouseState = isAirMouseState;
                    notifyItemChanged(position);
                }
                break;
            }
        }
    }

    public void updateTapSwitchShift(String tapIdentifier, int tapSwitchShiftInt) {
        // I think even a value of zero is meaningful

        for (int position = 0; position < dataSet.size(); position++) {
            TapListItem item = dataSet.get(position);
            if (item.tapIdentifier.equals(tapIdentifier)) {
                if (!onBind) {
                    item.tapShiftSwitchInt = tapSwitchShiftInt;
                    item.tapShiftAndSwitch = TapSdk.toShiftAndSwitch(tapSwitchShiftInt);
                    notifyItemChanged(position);
                }
            }
        }
    }

    public void updateName(String tapIdentifier, String name) {
        for (int position = 0; position < dataSet.size(); position++) {
            TapListItem item = dataSet.get(position);
            if (item.tapIdentifier.equals(tapIdentifier)) {
                if (!onBind) {
                    item.tapName = name;
                    notifyItemChanged(position);
                }
                break;
            }
        }
    }

    public void updateFwVer(String tapIdentifier, String fwVer) {
        for (int position = 0; position < dataSet.size(); position++) {
            TapListItem item = dataSet.get(position);
            if (item.tapIdentifier.equals(tapIdentifier)) {
                if (!onBind) {
                    item.tapFwVer = fwVer;
                    notifyItemChanged(position);
                }
                break;
            }
        }
    }

    public void onTextModeStarted(String tapIdentifier) {
        changeMode(tapIdentifier, false);    }

    public void onControllerModeStarted(String tapIdentifier) {
        changeMode(tapIdentifier, true);
    }

    private void changeMode(String tapIdentifier, boolean isInControllerMode) {
        for (int position = 0; position < dataSet.size(); position++) {
            TapListItem item = dataSet.get(position);
            if (item.tapIdentifier.equals(tapIdentifier)) {
                if (!onBind) {
                    item.isInControllerMode = isInControllerMode;
                    notifyItemChanged(position);
                }
                break;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout itemView;
        public TextView tapName;
        public TextView tapIdentifier;
        public TextView tapInputInt;
        public View finger1;
        public View finger2;
        public View finger3;
        public View finger4;
        public View finger5;
        public TextView mode;
        public TextView fwVer;
        public TextView shiftState;
        public TextView switchState;
        public TextView specialChar;
        public TextView protocol;
        public TextView airGesture;
        public TextView xrState;

        public ViewHolder(ConstraintLayout itemView) {
            super(itemView);
            this.itemView = itemView;

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            tapName = itemView.findViewById(R.id.tapName);
            tapIdentifier = itemView.findViewById(R.id.tapAddress);
            tapInputInt = itemView.findViewById(R.id.tapInputInt);
            finger1 = itemView.findViewById(R.id.finger1);
            finger2 = itemView.findViewById(R.id.finger2);
            finger3 = itemView.findViewById(R.id.finger3);
            finger4 = itemView.findViewById(R.id.finger4);
            finger5 = itemView.findViewById(R.id.finger5);
            mode = itemView.findViewById(R.id.tapMode);
            fwVer = itemView.findViewById(R.id.tapFwVer);
            shiftState = itemView.findViewById(R.id.shiftState);
            switchState = itemView.findViewById(R.id.switchState);
            specialChar = itemView.findViewById(R.id.specialChar);
            protocol = itemView.findViewById(R.id.tapProtocol);
            airGesture = itemView.findViewById(R.id.airGesture);
            xrState = itemView.findViewById(R.id.xrState);
        }

        public void bindTapListItem(final TapListItem listItem) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listItem.onClickListener.onClick(listItem);
                }
            });
            tapName.setText(listItem.tapName);
            tapIdentifier.setText(listItem.tapIdentifier);
            tapInputInt.setText(String.valueOf(listItem.tapInputInt));
            if (listItem.tapInputFingers != null) {
                finger1.setBackgroundResource(listItem.tapInputFingers[0] ? R.drawable.circle_filled : R.drawable.circle_empty);
                finger2.setBackgroundResource(listItem.tapInputFingers[1] ? R.drawable.circle_filled : R.drawable.circle_empty);
                finger3.setBackgroundResource(listItem.tapInputFingers[2] ? R.drawable.circle_filled : R.drawable.circle_empty);
                finger4.setBackgroundResource(listItem.tapInputFingers[3] ? R.drawable.circle_filled : R.drawable.circle_empty);
                finger5.setBackgroundResource(listItem.tapInputFingers[4] ? R.drawable.circle_filled : R.drawable.circle_empty);
            }
            if (listItem.tapShiftAndSwitch != null) {
                switch (listItem.tapShiftAndSwitch[0]) {
                    case 0:
                        shiftState.setText("Shift OFF");
                        break;
                    case 1:
                        shiftState.setText("Shift ON");
                        break;
                    case 2:
                        shiftState.setText("Shift LOCK");
                        break;
                    default:
                        shiftState.setText("Shift ERROR!!!");
                }
                if (listItem.tapShiftAndSwitch[1] > 0) {
                    switchState.setText("Switch ON");
                } else {
                    switchState.setText("Switch OFF");
                }
            }
            mode.setText(listItem.isInControllerMode ? "Controller Mode" : "Text Mode");
            fwVer.setText(listItem.tapFwVer);
            specialChar.setText("Repeat = " + listItem.tapRepeatInt);
            protocol.setText(listItem.isV2 ? "V2" : "Legacy");
            airGesture.setText(listItem.airGestureInt == -1
                    ? "" : "Air Gesture: " + gestureName(listItem));
            xrState.setText(listItem.isAirMouseState ? "AirMouse Mode" : "Tapping Mode");
            xrState.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listItem.onClickListener.onXRStateClick(listItem);
                }
            });
        }

        private static String gestureName(TapListItem listItem) {
            int code = listItem.airGestureInt;
            if (listItem.isV2) {
                UnifiedAirGesture gesture = UnifiedAirGesture.fromCode(code);
                return gesture != null ? gesture.name() : String.valueOf(code);
            }
            switch (code) {
                case AirMousePacket.AIR_MOUSE_GESTURE_NONE: return "NONE";
                case AirMousePacket.AIR_MOUSE_GESTURE_GENERAL: return "GENERAL";
                case AirMousePacket.AIR_MOUSE_GESTURE_UP: return "UP";
                case AirMousePacket.AIR_MOUSE_GESTURE_UP_TWO_FINGERS: return "UP_TWO_FINGERS";
                case AirMousePacket.AIR_MOUSE_GESTURE_DOWN: return "DOWN";
                case AirMousePacket.AIR_MOUSE_GESTURE_DOWN_TWO_FINGERS: return "DOWN_TWO_FINGERS";
                case AirMousePacket.AIR_MOUSE_GESTURE_LEFT: return "LEFT";
                case AirMousePacket.AIR_MOUSE_GESTURE_LEFT_TWO_FINGERS: return "LEFT_TWO_FINGERS";
                case AirMousePacket.AIR_MOUSE_GESTURE_RIGHT: return "RIGHT";
                case AirMousePacket.AIR_MOUSE_GESTURE_RIGHT_TWO_FINGERS: return "RIGHT_TWO_FINGERS";
                case AirMousePacket.AIR_MOUSE_GESTURE_INDEX_TO_THUMB_TOUCH: return "INDEX_TO_THUMB_TOUCH";
                case AirMousePacket.AIR_MOUSE_GESTURE_MIDDLE_TO_THUMB_TOUCH: return "MIDDLE_TO_THUMB_TOUCH";
                case AirMousePacket.XR_AIR_GESTURE_NONE: return "XR_NONE";
                case AirMousePacket.XR_AIR_GESTURE_THUMB_INDEX: return "XR_THUMB_INDEX";
                case AirMousePacket.XR_AIR_GESTURE_THUMB_MIDDLE: return "XR_THUMB_MIDDLE";
                default: return String.valueOf(code);
            }
        }
    }
}
