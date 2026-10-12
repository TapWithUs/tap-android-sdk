package com.tapwithus.sdk;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ListenerManager<ListenerType> {

    private final List<ListenerType> listeners = new CopyOnWriteArrayList<>();

    public void registerListener(@NonNull ListenerType listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(@NonNull ListenerType listener) {
        listeners.remove(listener);
    }

    public boolean isAlreadyExists(@NonNull ListenerType listener) {
        return listeners.contains(listener);
    }

    public @NonNull List<ListenerType> getAllListeners() {
        return new ArrayList<>(listeners);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    public void notifyAll(@NonNull NotifyAction<ListenerType> notifyAction) {
        for (Iterator<ListenerType> iterator = listeners.iterator(); iterator.hasNext();) {
            notifyAction.onNotify(iterator.next());
        }
    }
}
