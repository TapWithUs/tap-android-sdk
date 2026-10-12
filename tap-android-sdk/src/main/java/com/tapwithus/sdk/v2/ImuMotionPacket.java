package com.tapwithus.sdk.v2;

import androidx.annotation.NonNull;

/**
 * IMU motion data reported by V2 Tap devices when the
 * {@link DeviceFeature#IMU_MOTION_DATA} feature is enabled.
 *
 * Payload layout (little-endian, same as the v1 mouse packet plus Euler angles):
 * <pre>
 *     byte 0      : reserved
 *     bytes 1-2   : dx (int16)
 *     bytes 3-4   : dy (int16)
 *     byte 9      : isMouse flag (1 = pointer movement)
 *     bytes 10-15 : roll, pitch, yaw (int16 each)
 * </pre>
 */
public class ImuMotionPacket {

    public final int dx;
    public final int dy;
    public final boolean isMouse;
    public final int roll;
    public final int pitch;
    public final int yaw;

    public ImuMotionPacket(@NonNull byte[] payload) {
        dx = int16At(payload, 1);
        dy = int16At(payload, 3);
        isMouse = payload.length > 9 && payload[9] == 1;
        roll = int16At(payload, 10);
        pitch = int16At(payload, 12);
        yaw = int16At(payload, 14);
    }

    private static int int16At(byte[] data, int offset) {
        if (offset + 1 >= data.length) {
            return 0;
        }
        return (short) ((data[offset] & 0xFF) | (data[offset + 1] << 8));
    }

    @Override
    public String toString() {
        return "ImuMotionPacket{dx=" + dx + ", dy=" + dy + ", isMouse=" + isMouse +
                ", roll=" + roll + ", pitch=" + pitch + ", yaw=" + yaw + '}';
    }
}
