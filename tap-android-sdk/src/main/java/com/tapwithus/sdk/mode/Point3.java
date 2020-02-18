package com.tapwithus.sdk.mode;

import android.support.annotation.Nullable;

public class Point3 {
    public double x;
    public double y;
    public double z;

    private Point3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Nullable
    public static Point3 make(byte[] data, double sensitivity) {
        if (data.length == 6) {
            short x_i = (short)((short)data[1] << 8 | (short)data[0]);
            short y_i = (short)((short)data[3] << 8 | (short)data[2]);
            short z_i = (short)((short)data[5] << 8 | (short)data[4]);
            return new Point3((double)x_i * sensitivity, (double)y_i * sensitivity, (double)z_i * sensitivity);
        }

        return null;
    }
}
