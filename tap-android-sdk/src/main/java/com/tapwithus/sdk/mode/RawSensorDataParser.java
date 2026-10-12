package com.tapwithus.sdk.mode;

import java.util.Arrays;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RawSensorDataParser {
//    private RawSensorDataParserListener listener;

//    public RawSensorDataParser(@NonNull  RawSensorDataParserListener listener) {
//        this.listener = listener;
//    }

    private RawSensorDataParser() {

    }

    private static int parseAsLittleEndianByteArray(byte[] bytes) {
        if (bytes.length != 4) {
            return 0;
        }
        return ((bytes[3] & 0xff) << 24) | ((bytes[2] & 0xff) << 16) |
                ((bytes[1] & 0xff) << 8)  | (bytes[0] & 0xff);
    }

    public static ArrayList<RawSensorData> parseWhole(@NonNull String tapIdentifier, byte[] data, byte devAccelSens, byte imuGyroSens, byte imuAccelSens) {
        int metaLength = 4;
        int metaOffset = 0;
        int timestamp = 1;
        ArrayList<RawSensorData> result = new ArrayList<>();

        while (metaOffset + metaLength - 1< data.length && timestamp != 0) {
            int messageOffset;
            int messageLength;

            int meta= 0;

//            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
//            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
//
//
//            ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(data, metaOffset, metaOffset + metaLength));
//            meta = bb.getInt();
            meta = parseAsLittleEndianByteArray(Arrays.copyOfRange(data, metaOffset, metaOffset + metaLength));

            if (meta != 0) {
                byte packetType = (byte)(((meta & (0x80000000)) >> 31) & (byte)0x01);
                timestamp = (meta & 0x7fffffff);

                RawSensorData.DataType type = RawSensorData.DataType.IMU;
                messageOffset = metaOffset + metaLength;
                messageLength = 12;
                if (packetType == 1) {
                    type = RawSensorData.DataType.Device;
                    messageLength = 30;
                } else if (packetType != 0) {
                    return result;
                }
                if (messageOffset + messageLength < data.length) {
                    RawSensorData rsData =  parseSingle(tapIdentifier, type, timestamp, Arrays.copyOfRange(data,messageOffset, messageOffset + messageLength), devAccelSens, imuGyroSens, imuAccelSens);
                    if (rsData != null) {
                        result.add(rsData);
                    }
                }
                if (timestamp == 0) {

                    return result;
                }
            } else {
                return result;
            }
            metaOffset = metaOffset + metaLength + messageLength;
        }
        return result;
    }

    private static @Nullable RawSensorData parseSingle(@NonNull String tapIdentifier, RawSensorData.DataType type, int timestamp, byte[] data, byte devAccelSens, byte imuGyroSens, byte imuAccelSens) {
        return RawSensorData.make(type, timestamp, data, devAccelSens, imuGyroSens, imuAccelSens);

    }
}
