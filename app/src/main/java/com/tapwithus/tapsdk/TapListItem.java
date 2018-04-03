package com.tapwithus.tapsdk;

public class TapListItem {

    public String tapIdentifier;
    public String tapName;
    public int tapInputInt;
    public boolean[] tapInputFingers;
    public boolean isInControllerMode;

    public TapListItemOnClickListener onClickListener;

//    TapListItem(String tapName, String tapIdentifier, int tapInputInt, boolean[] tapInputFingers, String mode) {
    TapListItem(String tapIdentifier, TapListItemOnClickListener listener) {
//        this.tapName = tapName;
        this.tapIdentifier = tapIdentifier;
//        this.tapInputInt = tapInputInt;
//        this.tapInputFingers = tapInputFingers;
//        this.mode = mode;
        this.onClickListener = listener;
    }
}
