package com.tapwithus.sdk.bluetooth;

public class ExamplePacket extends Packet {

    public ExamplePacket(byte[] data) {
        super(data, 20);
    }

    public PacketValue cmdDescription = new PacketValue(0, 8);
    public PacketValue cmdInfo = new PacketValue(8, 8);
    public PacketValue layoutVersion = new PacketValue(16, 16);
    public PacketValue layoutId = new PacketValue(32, 128);

//    public PacketValue scrollDirectionFlip = new PacketValue(5, 1);
//    public PacketValue keyboardOnly = new PacketValue(6, 1);
//    public PacketValue mouseSensitivity = new PacketValue(8, 4);
//    public PacketValue scrollSensitivity = new PacketValue(12, 4);
//    public PacketValue doubleTapTimeout = new PacketValue(16, 4);
}
