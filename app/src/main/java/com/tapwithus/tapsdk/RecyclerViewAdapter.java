package com.tapwithus.tapsdk;

import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tapwithus.sdk.TapSdk;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private List<TapListItem> dataSet;

    public RecyclerViewAdapter(List<TapListItem> dataSet) {
        this.dataSet = dataSet;
    }

    public void updateList(List<TapListItem> items) {
        dataSet.clear();
        dataSet.addAll(items);
        notifyDataSetChanged();
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
        holder.bindTapListItem(dataSet.get(position));
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public void addItem(TapListItem item) {
        for (TapListItem i: dataSet) {
            if (i.tapIdentifier.equals(item.tapIdentifier)) {
                return;
            }
        }
        dataSet.add(item);
        notifyItemInserted(dataSet.size());
    }

    public void removeItem(String tapIdentifier) {
        int position;
        for (position = 0; position < dataSet.size(); position++) {
            if (dataSet.get(position).tapIdentifier.equals(tapIdentifier)) {
                dataSet.remove(position);
                notifyItemRemoved(position);
                break;
            }
        }
    }

    public void updateTapInput(String tapIdentifier, int tapInputInt) {
        if (tapInputInt == 0) {
            return;
        }

        for (int position = 0; position < dataSet.size(); position++) {
            TapListItem item = dataSet.get(position);
            if (item.tapIdentifier.equals(tapIdentifier)) {
                item.tapInputInt = tapInputInt;
                item.tapInputFingers = TapSdk.toFingers(tapInputInt);
                notifyItemChanged(position);
            }
        }
    }

    public void updateName(String tapIdentifier, String name) {
        for (int position = 0; position < dataSet.size(); position++) {
            TapListItem item = dataSet.get(position);
            if (item.tapIdentifier.equals(tapIdentifier)) {
                item.tapName = name;
                notifyItemChanged(position);
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
                item.isInControllerMode = isInControllerMode;
                notifyItemChanged(position);
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
            mode.setText(listItem.isInControllerMode ? "Controller Mode" : "Text Mode");
        }
    }
}
