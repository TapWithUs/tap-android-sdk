package com.tapwithus.sdk.bluetooth;

public class MousePacket extends Packet {

    public MousePacket(byte[] data) {
        super(data, 8);
    }

    public PacketValue dx = new PacketValue(0, 7);
    public PacketValue dy = new PacketValue(8, 15);
    public PacketValue dt = new PacketValue(16, 31);
}
