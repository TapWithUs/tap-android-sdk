package com.tapwithus.sdk.haptic;

import com.tapwithus.sdk.bluetooth.Packet;

public class HapticPacket extends Packet {

    public static final int TYPE_ONCE = 0;
    public static final int TYPE_SEQUENCE = 2;

    public HapticPacket(byte[] data) {
        super(data, 20);

        peripheralType.set(0); // HAPTIC
        actionType.set(TYPE_SEQUENCE);
    }

    public PacketValue peripheralType = new PacketValue(0, 8);
    public PacketValue actionType = new PacketValue(8, 8);
//    public PacketValue duration = new PacketValue(16, 16);
//    public PacketValue power = new PacketValue(32, 8);

    public PacketValue vOn1 = new PacketValue(16, 8);
    public PacketValue vOff1 = new PacketValue(24, 8);
    public PacketValue vOn2 = new PacketValue(32, 8);
    public PacketValue vOff2 = new PacketValue(40, 8);
    public PacketValue vOn3 = new PacketValue(48, 8);
    public PacketValue vOff3 = new PacketValue(56, 8);
    public PacketValue vOn4 = new PacketValue(64, 8);
    public PacketValue vOff4 = new PacketValue(72, 8);
    public PacketValue vOn5 = new PacketValue(80, 8);
    public PacketValue vOff5 = new PacketValue(88, 8);
    public PacketValue vOn6 = new PacketValue(96, 8);
    public PacketValue vOff6 = new PacketValue(104, 8);
    public PacketValue vOn7 = new PacketValue(112, 8);
    public PacketValue vOff7 = new PacketValue(120, 8);
    public PacketValue vOn8 = new PacketValue(128, 8);
    public PacketValue vOff8 = new PacketValue(136, 8);
    public PacketValue vOn9 = new PacketValue(144, 8);
    public PacketValue vOff9 = new PacketValue(152, 8);
}
