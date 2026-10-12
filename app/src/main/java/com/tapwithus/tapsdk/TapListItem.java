package com.tapwithus.tapsdk;

public class TapListItem {

    public String tapIdentifier;
    public String tapName;
    public String tapFwVer;
    public int tapInputInt;
    public int tapShiftSwitchInt;
    public int tapRepeatInt;
    public boolean[] tapInputFingers;
    public int[] tapShiftAndSwitch;
    public boolean isInControllerMode = true;
    public boolean isV2 = false;
    public int airGestureInt = -1;
    public boolean isAirMouseState = false;

    public TapListItemOnClickListener onClickListener;

    TapListItem(String tapIdentifier, TapListItemOnClickListener listener) {
        this.tapIdentifier = tapIdentifier;
        this.onClickListener = listener;
    }
}
