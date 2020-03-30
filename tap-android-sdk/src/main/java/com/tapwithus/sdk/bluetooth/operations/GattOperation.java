package com.tapwithus.sdk.bluetooth.operations;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.NonNull;

import com.tapwithus.sdk.bluetooth.callbacks.OnCompletionListener;
import com.tapwithus.sdk.bluetooth.callbacks.OnErrorListener;
import com.tapwithus.sdk.bluetooth.callbacks.OnNotFoundListener;

import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class GattOperation<T> {

    public static final int GATT_SUCCESS = BluetoothGatt.GATT_SUCCESS;
    public static final int OP_TIMEOUT = 5000;

    private boolean isRunning = false;
    private boolean isCompleted = false;
    private boolean isTimedout = false;

    private long preDelay = 0;
    private long postDelay = 0;

    private final List<OnCompletionListener<T>> cCallbacks = new CopyOnWriteArrayList<>();
    private final List<OnErrorListener> eCallbacks = new CopyOnWriteArrayList<>();
    private final List<OnNotFoundListener> nfCallbacks = new CopyOnWriteArrayList<>();

    public abstract OperationType type();
    public abstract void onExecute(@NonNull BluetoothGatt gatt);
    public abstract void gattCallback(Object gattOrCharOrDesc, int status);

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    /**
     * @param preDelay must be greater than zero
     * @return this
     */
    public GattOperation<T> setPreDelay(long preDelay) {
        if (preDelay > 0) {
            this.preDelay = preDelay;
        }
        return this;
    }

    /**
     * @param postDelay must be greater than zero
     * @return this
     */
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

    public GattOperation<T> addOnNotFoundListener(@NonNull OnNotFoundListener<T> listener) {
        if (!nfCallbacks.contains(listener)) {
            nfCallbacks.add(listener);
        }
        return this;
    }

    public GattOperation<T> removeOnNotFoundListener(@NonNull OnNotFoundListener<T> listener) {
        nfCallbacks.remove(listener);
        return this;
    }

    public void execute(@NonNull final BluetoothGatt gatt) {
        isRunning = true;
        isCompleted = false;
        isTimedout = false;

        if (preDelay == 0) {
            setTimeout();
            onExecute(gatt);
            return;
        }

        delay(preDelay, new OnCompletionListener<Void>() {
            @Override
            public void onCompletion(Void data) {
                setTimeout();
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
        if (isTimedout) {
            return;
        }

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

    protected void postOnNotFound(@NonNull final String message) {
        isRunning = false;
        isCompleted = true;

        for (Iterator<OnNotFoundListener> iterator = nfCallbacks.iterator(); iterator.hasNext();) {
            iterator.next().onNotFound(message);
        }
    }

    private void doPostOnCompletion(T data) {
        isRunning = false;
        isCompleted = true;

        for (Iterator<OnCompletionListener<T>> iterator = cCallbacks.iterator(); iterator.hasNext();) {
            iterator.next().onCompletion(data);
        }
    }

    private void setTimeout() {
        delay(OP_TIMEOUT, new OnCompletionListener<Void>() {
            @Override
            public void onCompletion(Void data) {
                if (!isCompleted) {
                    isTimedout = true;
                    postOnError("Operation timeout");
                }
            }
        });
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
