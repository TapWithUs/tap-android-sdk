package com.tapwithus.sdk.xr;

import java.util.LinkedHashMap;
import java.util.Map;

final class MajorityVoting<T> {

    private final T defaultValue;
    private final int len;
    private FixedWindow<T> window;

    MajorityVoting(int len, T defaultValue) {
        this.defaultValue = defaultValue;
        this.len = len;
        this.window = new FixedWindow<>(len);
        reset();
    }

    private void reset() {
        window = new FixedWindow<>(len);
        for (int i = 0; i < len; i++) {
            window.add(defaultValue);
        }
    }

    T call(T item) {
        window.add(item);
        Map<T, Integer> counts = new LinkedHashMap<>();
        for (T value : window.all()) {
            Integer count = counts.get(value);
            counts.put(value, count == null ? 1 : count + 1);
        }
        T best = null;
        int bestCount = -1;
        for (Map.Entry<T, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }
}
