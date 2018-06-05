package com.tapwithus.sdk;

public interface NotifyAction<ListenerType> {
    void onNotify(ListenerType listener);
}