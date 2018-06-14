package com.tapwithus.sdk.bluetooth;

public class MousePacket extends Packet {

    public MousePacket(byte[] data) {
        super(data, 8);
    }

//    public PacketValue dx = new PacketValue(0, 15);
//    public PacketValue dy = new PacketValue(16, 31);
//    public PacketValue dt = new PacketValue(32, 63);

    public PacketValue dx = new PacketValue(0, 16);
    public PacketValue dy = new PacketValue(16, 16);
    public PacketValue dt = new PacketValue(32, 32);
}
