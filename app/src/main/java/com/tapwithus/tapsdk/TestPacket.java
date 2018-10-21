package com.tapwithus.tapsdk;

import com.tapwithus.sdk.bluetooth.Packet;

public class TestPacket extends Packet {

    public TestPacket(byte[] data) {
        super(data, 8);
    }

//    public PacketValue dx = new PacketValue(0, 15);
//    public PacketValue dy = new PacketValue(16, 31);
//    public PacketValue dt = new PacketValue(32, 63);

    public PacketValue string4B = new PacketValue(0, 64);
//    public PacketValue int2B= new PacketValue(32, 16);
//    public PacketValue int4B= new PacketValue(48, 32);
}
