package com.tapwithus.tapsdk;

public class TapListItem {

    public String tapIdentifier;
    public String tapName;
    public String tapFwVer;
    public int tapInputInt;
    public boolean[] tapInputFingers;
    public boolean isInControllerMode = true;

    public TapListItemOnClickListener onClickListener;

    TapListItem(String tapIdentifier, TapListItemOnClickListener listener) {
        this.tapIdentifier = tapIdentifier;
        this.onClickListener = listener;
    }
}
