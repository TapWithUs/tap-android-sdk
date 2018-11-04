package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.callbacks.OnCompletionListener;
import com.tapwithus.sdk.bluetooth.callbacks.OnErrorListener;

import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class GattOperation<T> {

    public static final int GATT_SUCCESS = BluetoothGatt.GATT_SUCCESS;

    private boolean isRunning = false;
    private boolean isCompleted = false;

    private long preDelay = 0;
    private long postDelay = 0;

    private final List<OnCompletionListener<T>> cCallbacks = new CopyOnWriteArrayList<>();
    private final List<OnErrorListener> eCallbacks = new CopyOnWriteArrayList<>();

    public abstract OperationType type();
    public abstract void onExecute(@NonNull BluetoothGatt gatt);
    public abstract void gattCallback(Object gattOrCharOrDesc, int status);

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public GattOperation<T> setPreDelay(long preDelay) {
        if (preDelay > 0) {
            this.preDelay = preDelay;
        }
        return this;
    }

    public GattOperation<T> setPostDelay(long postDelay) {
        if (postDelay > 0) {
            this.postDelay = postDelay;
        }
        return this;
    }

    public GattOperation<T> addOnCompletionListener(@NonNull OnCompletionListener<T> listener) {
        if (!cCallbacks.contains(listener)) {
            cCallbacks.add(listener);
        }
        return this;
    }

    public GattOperation<T> removeOnCompletionListener(@NonNull OnCompletionListener<T> listener) {
        cCallbacks.remove(listener);
        return this;
    }

    public GattOperation<T> addOnErrorListener(@NonNull OnErrorListener listener) {
        if (!eCallbacks.contains(listener)) {
            eCallbacks.add(listener);
        }
        return this;
    }

    public GattOperation<T> removeOnErrorListener(@NonNull OnErrorListener listener) {
        eCallbacks.remove(listener);
        return this;
    }

    public void execute(@NonNull final BluetoothGatt gatt) {
        isRunning = true;
        isCompleted = false;

        if (preDelay == 0) {
            onExecute(gatt);
            return;
        }

        delay(preDelay, new OnCompletionListener<Void>() {
            @Override
            public void onCompletion(Void data) {
                onExecute(gatt);
            }
        });
    }

    public void gattCallback(@NonNull Object gattOrCharOrDesc) {
        gattCallback(gattOrCharOrDesc, GATT_SUCCESS);
    }

    public void gattCallback(int status) {
        gattCallback(null, status);
    }

    protected void postOnCompletion(final T data) {
        if (postDelay == 0) {
            doPostOnCompletion(data);
            return;
        }

        delay(postDelay, new OnCompletionListener<Void>() {
            @Override
            public void onCompletion(Void d) {
                doPostOnCompletion(data);
            }
        });
    }

    protected void postOnError(@NonNull final String msg) {
        isRunning = false;
        isCompleted = true;

        for (Iterator<OnErrorListener> iterator = eCallbacks.iterator(); iterator.hasNext();) {
            iterator.next().onError(msg);
        }
    }

    private void doPostOnCompletion(T data) {
        isRunning = false;
        isCompleted = true;

        for (Iterator<OnCompletionListener<T>> iterator = cCallbacks.iterator(); iterator.hasNext();) {
            iterator.next().onCompletion(data);
        }
    }

    private void delay(long delay, final OnCompletionListener<Void> onFinish) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                onFinish.onCompletion(null);
            }
        }, delay);
    }
}
