package com.tapwithus.sdk.bluetooth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public byte[] getData() {
        return data;
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
            String binaryString = byteToBinaryString(b);
            binaryDataStringBuilder.append(binaryString);
        }
        this.data = data;
    }

    private String byteToBinaryString(byte b) {
        return String
                .format("%8s", Integer.toBinaryString(byteToInt(b)))
                .replace(' ', '0');
    }

    public String getString(int start, int size) {
        if (lsb) {
            return getLsbString(start, size);
        } else {
            return getMsbString(start, size);
        }
    }

    private String getLsbString(int startIndex, int size) {
        String binaryString = getBinaryDataString();

        String s = getL(binaryString, startIndex, size);
        return reverseBinaryString(s);
    }

//    private String getLsbString(int index, int size) {
//        StringBuilder sb = new StringBuilder();
//
//        String[] binaryByteStrings = chunk(getBinaryDataString());
//
//        int byteNumber = index / BYTE_SIZE;
//        int startIndex = index - (BYTE_SIZE * byteNumber);
//        while (size > 0) {
//            int nSize = Math.min(BYTE_SIZE - startIndex, size);
//            String sub = subBinaryByteString(binaryByteStrings[byteNumber], startIndex, nSize);
//            sb.append(sub);
//
//            byteNumber++;
//            startIndex = 0;
//            size -= nSize;
//        }
//
//        return reverseBinaryString(sb.toString());
//    }

    private String getMsbString(int start, int size) {
        return binaryDataStringBuilder.substring(start, start + size);
    }

    public void set(int startIndex, int size, String data) {
        if (lsb) {
            updateLsbData(startIndex, size, data);
        } else {
            updateMsbData(startIndex, size, data);
        }
    }

    private void updateLsbData(int startIndex, int size, String data) {

        data = reverseBinaryString(data);

        String binaryString = getBinaryDataString();
        binaryString = setL(binaryString, startIndex, size, data);

        this.binaryDataStringBuilder = new StringBuilder(binaryString);
        this.data = binaryStringToByteArray(binaryString);
    }

//    private void updateLsbData(int startIndex, int size, String data) {
//
//        StringBuilder sb = new StringBuilder();
//
//        String[] dataChunks = chunk(data);
//        for (String byteString: dataChunks) {
//            sb.insert(0, byteString);
//        }
//
//        int byteNumber = startIndex / BYTE_SIZE;
//        int bitNumber = startIndex % BYTE_SIZE;
//        int lsbBitOffset = BYTE_SIZE - bitNumber;
//        int eIndex = byteNumber * BYTE_SIZE + lsbBitOffset;
//        int sIndex = eIndex - size;
//
////        7654 3210 5432 1098
////        0123 4567 8901 2345
////        0000 0000 0000 0010
//
//        binaryDataStringBuilder.replace(sIndex, eIndex, sb.toString());
//        this.data = binaryStringToByteArray(binaryDataStringBuilder.toString());
//    }

    private void updateMsbData(int startIndex, int size, String data) {
        binaryDataStringBuilder.replace(startIndex, startIndex + size, data);
        this.data = binaryStringToByteArray(binaryDataStringBuilder.toString());
    }

    private String subBinaryByteString(String binaryByteString, int startIndex, int size) {

        String reversedBinaryByteString = reverse(binaryByteString);
        String subReveredBinaryByteString = substring(reversedBinaryByteString, startIndex, size);

        return reverse(subReveredBinaryByteString);
    }

    private String substring(String s, int startIndex, int size) {
        int byteBinaryStringSize = s.length();
        if (byteBinaryStringSize < startIndex || byteBinaryStringSize < startIndex + size) {
            throw new IndexOutOfBoundsException("Index or Size not corresponding actual given string size");
        }
        return s.substring(startIndex, startIndex + size);
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

    public String reverseBinaryString(String binaryString) {
        String[] chunks = chunk(binaryString);
        String[] reversedChunks = reverseBytes(chunks);

        StringBuilder reversedBinaryString = new StringBuilder();
        for (String chunk: reversedChunks) {
            reversedBinaryString.append(chunk);
        }
        return reversedBinaryString.toString();
    }

    private String[] reverseBytes(String[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            String temp = bytes[i];
            bytes[i] = bytes[bytes.length - i - 1];
            bytes[bytes.length - i - 1] = temp;
        }
        return bytes;
    }

    private String[] chunk(String string) {

        List<String> parts = new ArrayList<>();
        int index = 0;
        while (index < string.length()) {
            parts.add(string.substring(index, Math.min(index + BYTE_SIZE, string.length())));
            index += BYTE_SIZE;
        }

        return parts.toArray(new String[0]);
    }

    private String reverse(String string) {
        return new StringBuffer(string).reverse().toString();
    }

    private String replaceRange(String source, int startIndex, int endIndex, String replacement) {
        return new StringBuffer(source).replace(startIndex, endIndex, replacement).toString();
    }


//    public String b(int bitIndex, int size, String lsbBinaryString) {
//        int binaryStringSize = lsbBinaryString.length();
//        int bitsToCopy = bitsToCopy(bitIndex, size);
//
//        String remainingBits = "";
//        if (binaryStringSize > BYTE_SIZE) {
//            String remainingBinaryString = lsbBinaryString.substring(BYTE_SIZE, binaryStringSize);
//            int remainingBitIndex = bitIndex + bitsToCopy;
//            int remainingSize = size - bitsToCopy;
//            remainingBits = b(remainingBitIndex, remainingSize, remainingBinaryString);
//        }
//
//        subBitsOfLsbByte(lsbBinaryString, bitIndex, bitsToCopy);
//
//
//        int byteNumber = byteNumber(bitIndex);
//        int bitNumber = bitNumber(bitIndex);
//        int lsbBitOffset = BYTE_SIZE - bitNumber;
//
//
//        StringBuilder sb = new StringBuilder(binaryStringToSaveTo);
//
//        int eIndex = lsbBitOffset;
//        int sIndex = eIndex - size;
//
//        sb.replace(sIndex, eIndex, binaryStringToSave);
//
//
//
//    }

    public String setL(String lsbBinaryString, int bitIndex, int size, String value) {

        String binaryString = lsbBinaryString;

        int binaryStringSize = binaryString.length();

        if (binaryStringSize == 0) {
            return "";
        }

        if (value.length() > size) {
            value = value.substring(value.length() - size, value.length());
        }

        if (value.length() < size) {
            value = addLeadingZeros(value);
            value = value.substring(value.length() - size, value.length());
        }

        int bitsToCopy = bitsToCopy(bitIndex, size);
        lsbBinaryString = replaceInLsbByte(lsbBinaryString, bitIndex, bitsToCopy, value);

        if (bitsToCopy == size) {
            return lsbBinaryString;
        }

        int newBitIndex = bitIndex + bitsToCopy - BYTE_SIZE;
        int newSize = size - bitsToCopy;
        String newLsbBinaryString = lsbBinaryString.substring(BYTE_SIZE, binaryStringSize);
        String newValue = value.substring(bitsToCopy, value.length());

        String remainingBits = setL(newLsbBinaryString, newBitIndex, newSize, newValue);

        lsbBinaryString = lsbBinaryString.substring(0, BYTE_SIZE);
        return lsbBinaryString + remainingBits;
    }

    public String replaceInLsbByte(String lsbBinaryString, int bitIndex, int size, String value) {

        if (value.length() > size) {
            value = value.substring(0, size);
        }

        if (value.length() < size) {
            value = addLeadingZeros(value);
            value = value.substring(value.length() - size, value.length());
        }

        int sIndex = bitIndex;
        int eIndex = sIndex + size;
        int binaryStringSize = lsbBinaryString.length();

        if (sIndex >= BYTE_SIZE || sIndex >= binaryStringSize) {
            return lsbBinaryString;
        }

        if (eIndex > BYTE_SIZE) {
            eIndex = Math.min(BYTE_SIZE, binaryStringSize);
        } else if (eIndex > binaryStringSize) {
            eIndex = binaryStringSize;
        }

        // Notice that when converting from LSB to MSB, naturally the start and end indexes will be switched
        int msbSIndex = lsbBitToMsbBit(eIndex - 1);
        int msbEIndex = lsbBitToMsbBit(sIndex) + 1;

        int replacementSize = msbEIndex - msbSIndex;
        if (value.length() > replacementSize) {
            value = value.substring(value.length() - replacementSize, value.length());
        }

        return new StringBuilder(lsbBinaryString).replace(msbSIndex, msbEIndex, value).toString();
    }

    public String getL(String lsbBinaryString, int bitIndex, int size) {

        int binaryStringSize = lsbBinaryString.length();

        if (binaryStringSize == 0) {
            return "";
        }

        String subBits = subBitsOfLsbByte(lsbBinaryString, bitIndex, size);
        int subBitsSize = subBits.length();

        if (subBitsSize == size) {
            return subBits;
        }

        int newBitIndex = bitIndex + subBitsSize - BYTE_SIZE;
        int newSize = size - subBitsSize;
        String newLsbBinaryString = lsbBinaryString.substring(BYTE_SIZE, binaryStringSize);

        String remainingBits = getL(newLsbBinaryString, newBitIndex, newSize);

        String lsbValue = addLeadingZeros(subBits + remainingBits);
//        String s = new StringBuilder(lsbValue).reverse().toString();

        return lsbValue;
    }

    public String addLeadingZeros(String binaryString) {
        int binaryStringSize = binaryString.length();
        int zerosToAdd = BYTE_SIZE - binaryStringSize % BYTE_SIZE;
        if (zerosToAdd == BYTE_SIZE && binaryStringSize != 0) {
            zerosToAdd = 0;
        }
        for (; zerosToAdd > 0; zerosToAdd--) {
            binaryString = '0' + binaryString;
        }
        return binaryString;
    }

    public int bitsToCopy(int bitIndex, int size) {
        if (byteNumber(bitIndex) != 0) {
            return 0;
        }
        return Math.min(BYTE_SIZE - bitNumber(bitIndex), size);
    }

    public String subBitsOfLsbByte(String lsbBinaryString, int bitIndex, int size) {

        int sIndex = bitIndex;
        int eIndex = sIndex + size;
        int binaryStringSize = lsbBinaryString.length();

        if (sIndex >= BYTE_SIZE || sIndex >= binaryStringSize) {
            return "";
        }

        if (eIndex > BYTE_SIZE) {
            eIndex = Math.min(BYTE_SIZE, binaryStringSize);
        } else if (eIndex > binaryStringSize) {
            eIndex = binaryStringSize;
        }

        // Notice that when converting from LSB to MSB, naturally the start and end indexes will be switched
        int msbSIndex = lsbBitToMsbBit(eIndex - 1);
        int msbEIndex = lsbBitToMsbBit(sIndex) + 1;

        return lsbBinaryString.substring(msbSIndex, msbEIndex);
    }

    public String getFirstNBitsLsb(String lsbBinaryString, int n) {
        int sizeToReturn = n;
        int size = lsbBinaryString.length();

        if (sizeToReturn > BYTE_SIZE) {
            sizeToReturn = Math.min(BYTE_SIZE, size);
        } else if (sizeToReturn > size) {
            sizeToReturn = size;
        }

        int startIndex = BYTE_SIZE - sizeToReturn;
        return lsbBinaryString.substring(startIndex, BYTE_SIZE);
    }

    private int lsbBitToMsbBit(int bitIndex) {
        int lsbBitOffset = BYTE_SIZE - bitNumber(bitIndex) - 1;
        return byteNumber(bitIndex) * BYTE_SIZE + lsbBitOffset;
    }

    private int byteNumber(int bitIndex) {
        return bitIndex / BYTE_SIZE;
    }

    private int bitNumber(int bitIndex) {
        return bitIndex % BYTE_SIZE;
    }

    public class PacketValue {

        private int startIndex;
        private int size;
        private boolean unsigned;

        public PacketValue(int startBit, int size) {
            this.startIndex = startBit;
            this.size = size;
        }

        public String getString() {
            String binaryString = Packet.this.getString(startIndex, size);
//            binaryString = reverseBinaryString(binaryString);
            String reversedData = new String(binaryStringToByteArray(binaryString)).trim();
            return reverse(reversedData);
        }

        public boolean getBoolean() {
            String binaryString = Packet.this.getString(startIndex, size);
            return Integer.parseInt(binaryString, 2) == 1;
        }

        public int getInt() {
            String binaryString = Packet.this.getString(startIndex, size);
            return (int) (byte) Integer.parseInt(binaryString, 2);
        }

        public int getUnsignedInt() {
            String binaryString = Packet.this.getString(startIndex, size);
            return Integer.parseInt(binaryString, 2);
        }

        public long getLong() {
            String binaryString = Packet.this.getString(startIndex, size);
            return (long) (byte) Long.parseLong(binaryString, 2);
        }

        public long getUnsignedLong() {
            String binaryString = Packet.this.getString(startIndex, size);
            return Long.parseLong(binaryString, 2);
        }

        public void set(boolean data) {
            Packet.this.set(startIndex, size, toNumericalString(data));
        }

        public void set(int data) {
            String binaryString = toBinaryString(data, size);

            Packet.this.set(startIndex, size, binaryString);
        }

        public void set(String data) {
            byte[] dataBytes = reverse(data).getBytes();

            StringBuilder sb = new StringBuilder();
            for (byte b: dataBytes) {
                sb.append(byteToBinaryString(b));
//                sb.insert(0, byteToBinaryString(b));
            }

            // Add leading zeros
            for (int i = dataBytes.length; i < (size / BYTE_SIZE); ++i) {
                sb.insert(0, byteToBinaryString((byte) 0x0));
            }

            Packet.this.set(startIndex, size, sb.toString());
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

//        private boolean isNumerical(String s) {
//            return s != null && s.matches("[-+]?\\d*\\.?\\d+");
//        }

//        private String reverseBinaryString(String binaryString) {
//
//            String[] chunks = chunk(binaryString);
//
//            StringBuilder sb = new StringBuilder();
//            for (String byteString: chunks) {
//
//                sb.insert(0, reverse(byteString));
//            }
//
//            return sb.toString();
//        }

//        private String reverseBinaryString(String binaryString) {
//
//            String[] chunks = chunk(binaryString);
//
//            StringBuilder sb = new StringBuilder();
//            for (String byteString: chunks) {
//                sb.insert(0, byteString);
//            }
//
//            return sb.toString();
//        }
    }
}
