package com.tapwithus.sdk.mode;

import android.support.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.text.DecimalFormat;
import java.util.Arrays;

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
//            int x_i = ((short)(data[1]) << 8 | data[0]);
//            int y_i = ((short)(data[3]) << 8 | data[2]);
//            int z_i = ((short)(data[5]) << 8 | data[4]);
            ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(data,0,6));
            bb.order( ByteOrder.LITTLE_ENDIAN);
            short[] points = new short[] { 0, 0, 0};
            int counter = 0;
            while (bb.hasRemaining() && counter < points.length) {
                points[counter] = bb.getShort();
                counter ++;
            }
            if ( counter < 2) {
                return null;
            }

//            int x_i = ((data[1] & 0xff) << 8) | (data[0] & 0xff);
//            int y_i = ((data[3] & 0xff) << 8) | (data[2] & 0xff);
//            int z_i = ((data[5] & 0xff) << 8) | (data[4] & 0xff);
//            short x_i = (short)((short)data[1] << 8 | (short)data[0]);
//            short y_i = (short)((short)data[3] << 8 | (short)data[2]);
//            short z_i = (short)((short)data[5] << 8 | (short)data[4]);
            return new Point3((double)points[0] * sensitivity, (double)points[1] * sensitivity, (double)points[2] * sensitivity);
        }

        return null;
    }


    public String toString() {
        DecimalFormat df = new DecimalFormat("#.###");
        return "(" + df.format(this.x) + ", " + df.format(this.y) + ", " + df.format(this.z) + ")";
    }

    public String rawString(String delimeter) {
        return this.x + delimeter + this.y + delimeter + this.z;
    }
}
