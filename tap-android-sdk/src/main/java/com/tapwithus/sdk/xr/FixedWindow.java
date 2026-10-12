package com.tapwithus.sdk.xr;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed-length history. Adding past {@code len} drops the oldest item.
 */
final class FixedWindow<T> {

    private final List<T> items = new ArrayList<>();
    private final int len;

    FixedWindow(int len) {
        this.len = len;
    }

    void add(T item) {
        items.add(item);
        if (items.size() > len) {
            items.subList(0, items.size() - len).clear();
        }
    }

    List<T> all() {
        return items;
    }
}
