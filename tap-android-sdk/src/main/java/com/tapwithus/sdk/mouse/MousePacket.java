package com.tapwithus.sdk.mouse;

import com.tapwithus.sdk.bluetooth.Packet;

public class MousePacket extends Packet {

    public MousePacket(byte[] data) {
        super(data, 9);
    }

    public PacketValue dx = new PacketValue(0, 16);
    public PacketValue dy = new PacketValue(16, 16);
    public PacketValue dt = new PacketValue(32, 32);
    public PacketValue proximity = new PacketValue(64, 8);
}
