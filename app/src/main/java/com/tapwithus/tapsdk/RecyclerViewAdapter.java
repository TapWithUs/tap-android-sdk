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
        }
    }
}
