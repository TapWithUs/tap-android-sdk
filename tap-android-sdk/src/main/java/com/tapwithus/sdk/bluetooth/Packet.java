package com.tapwithus.sdk.bluetooth;

import java.util.Arrays;

public class Packet {

    private static final int BYTE_SIZE = 8;

    private byte[] data;
    private boolean lsb;

    private StringBuilder binaryDataStringBuilder = new StringBuilder();

    public Packet(byte[] data, int size) {
        this(data, size, true);
    }

    public Packet(byte[] data, int size, boolean lsb) {
        initData(data, size);
        this.lsb = lsb;
    }

    private void initData(byte[] data, int size) {
        byte[] d = data.clone();
        if (d.length > size) {
            d = Arrays.copyOfRange(d, 0, size);
        }
        if (d.length < size) {
            d = concat(d, new byte[size - d.length]);
        }
        setData(d);
    }

    public String getBinaryDataString() {
        return binaryDataStringBuilder.toString();
    }

    private void setData(byte[] data) {
        // Set the binaryDataStringBuilder based on the given data
        binaryDataStringBuilder.setLength(0);
        for (byte b: data) {
            String binaryString = String.valueOf(byteToInt(b));
            binaryDataStringBuilder.append(binaryString);
        }
        this.data = data;
    }

    public String getString(int start, int size) {
        if (lsb) {
            return getLsbString(start, size);
        } else {
            return getMsbString(start, size);
        }
    }

    private String getLsbString(int start, int size) {
        int byteNumber = start / BYTE_SIZE;
        String reversedByte = reverse(getByteStringFromBinaryDataStringBuilder(byteNumber));
        int startBit = start - byteNumber * BYTE_SIZE;
        return reverse(reversedByte.substring(startBit, startBit + size));
    }

    private String getMsbString(int start, int size) {
        return binaryDataStringBuilder.substring(start, start + size);
    }

    public void set(int start, int size, String data) {
        if (lsb) {
            updateLsbData(start, size, data);
        } else {
            updateMsbData(start, size, data);
        }
    }

    private void updateLsbData(int start, int size, String data) {
        int byteNumber = start / BYTE_SIZE;
        String reversedByte = reverse(getByteStringFromBinaryDataStringBuilder(byteNumber));
        int startBit = start - byteNumber * BYTE_SIZE;

        String backToNormalByte = reverse(replaceRange(reversedByte, startBit, startBit + size, reverse(data)));
        binaryDataStringBuilder.replace(byteNumber * BYTE_SIZE, byteNumber * BYTE_SIZE + BYTE_SIZE, backToNormalByte);

        this.data = binaryStringToByteArray(binaryDataStringBuilder.toString());
    }

    private void updateMsbData(int startIndex, int size, String data) {
        binaryDataStringBuilder.replace(startIndex, startIndex + size, data);

        this.data = binaryStringToByteArray(binaryDataStringBuilder.toString());
    }

    private byte[] binaryStringToByteArray(String binaryString) {
        String[] binaryByteStrings = chunk(binaryString);
        byte[] byteArray = new byte[binaryByteStrings.length];
        for (int i = 0; i < binaryByteStrings.length; i++) {
            byteArray[i] = binaryByteStringToByte(binaryByteStrings[i]);
        }
        return byteArray;
    }

    private byte binaryByteStringToByte(String binaryByteString) {
        return (byte) Integer.parseInt(binaryByteString, 2);
    }

    private int byteToInt(byte b) {
        return b & 0xFF;
    }

    private byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;

        byte[] c = new byte[aLen + bLen];

        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    private String getByteStringFromBinaryDataStringBuilder(int byteNumber) {
        return chunk(binaryDataStringBuilder.toString())[byteNumber];
    }

    private String[] chunk(String string) {
        return string.split("(?<=\\G.{" + BYTE_SIZE + "})");
    }

    private String reverse(String string) {
        return new StringBuffer(string).reverse().toString();
    }

    private String replaceRange(String source, int startIndex, int endIndex, String replacement) {
        return new StringBuffer(source).replace(startIndex, endIndex, replacement).toString();
    }

    public class PacketValue {

        private int startIndex;
        private int size;

        public PacketValue(int startIndex, int size) {
            this.startIndex = startIndex;
            this.size = size;
        }

        public String getString() {
            return Packet.this.getString(startIndex, size);
        }

        public boolean getBoolean() {
            return Packet.this.getString(startIndex, size).equals("1");
        }

        public int getInt() {
            return Integer.parseInt(Packet.this.getString(startIndex, size));
        }

        public void set(boolean data) {
            Packet.this.set(startIndex, size, toNumericalString(data));
        }

        public void set(int data) {
            Packet.this.set(startIndex, size, toBinaryString(data, size));
        }

        public void set(String data) {
            if (isNumerical(data)) {
                Packet.this.set(startIndex, size, data);
            }
        }

        private String toNumericalString(boolean data) {
            return data ? "1" : "0";
        }

        private String toBinaryString(int num, int size) {
            int intValue = num;
            if (intValue < 0) {
                intValue += 256;
            }
            return String
                    .format("%" + size + "s", Integer.toBinaryString(intValue))
                    .replace(' ', '0');
        }

        private boolean isNumerical(String s) {
            return s != null && s.matches("[-+]?\\d*\\.?\\d+");
        }
    }
}
