package com.tapwithus.sdk;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListenerManager<ListenerType> {

    private final List<ListenerType> listeners = new ArrayList<>();

    public void registerListener(@NonNull ListenerType listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(@NonNull ListenerType listener) {
        for (Iterator<ListenerType> iterator = listeners.iterator(); iterator.hasNext();) {
            ListenerType l = iterator.next();
            if (l == listener) {
                iterator.remove();
            }
        }
    }

    @NonNull
    public List<ListenerType> getAllListeners() {
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
