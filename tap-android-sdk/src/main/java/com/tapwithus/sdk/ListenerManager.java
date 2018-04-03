package com.tapwithus.sdk;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;


public class ListenerManager<ListenerType> {

    public interface NotifyAction<ListenerType> {
        void onNotify(ListenerType listener);
    }

    private final ArrayList<ListenerType> listeners = new ArrayList<>();

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
    public ArrayList<ListenerType> getAllListeners() {
        return new ArrayList<>(listeners);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    public void notifyListeners(@NonNull NotifyAction<ListenerType> notifyAction) {
        for (Iterator<ListenerType> iterator = listeners.iterator(); iterator.hasNext();) {
            notifyAction.onNotify(iterator.next());
        }
    }
}
