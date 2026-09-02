package com.tapwithus.sdk.v2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Callback for V2 configuration "get" requests.
 */
public interface TapV2Callback<T> {

    /**
     * @param value the device's reply, or null if the request timed out,
     *              was superseded by a newer request, or isn't supported
     *              by the connected device
     */
    void onResponse(@NonNull String tapIdentifier, @Nullable T value);
}
