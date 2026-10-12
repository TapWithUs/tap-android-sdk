package com.tapwithus.sdk.v2;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Inbound frame parsing tests against the tap-python-sdk reference
 * implementation ({@code parsers.tap_inc_msg}).
 */
public class TapV2ParserTest {

    @Test
    public void tapGesture() {
        TapV2Message message = TapV2Parser.parse(new byte[] { 1, 2, 0, 0, 10 });
        assertNotNull(message);
        assertEquals(TapV2Message.Type.TAP_GESTURE, message.type);
        assertEquals(10, message.payload[0]);
    }

    @Test
    public void airGesture() {
        TapV2Message message = TapV2Parser.parse(new byte[] { 1, 3, 0, 0, 105 });
        assertNotNull(message);
        assertEquals(TapV2Message.Type.AIR_GESTURE, message.type);
        assertEquals(UnifiedAirGesture.AB, UnifiedAirGesture.fromCode(message.payload[0]));
    }

    @Test
    public void imuMotion() {
        byte[] frame = new byte[4 + 16];
        frame[0] = 0; // IMU_DATA
        frame[1] = 0; // IMU_MOTION_DATA
        // payload byte 1-2: dx = -5 (0xFFFB LE)
        frame[4 + 1] = (byte) 0xFB;
        frame[4 + 2] = (byte) 0xFF;
        // payload byte 3-4: dy = 300 (0x012C LE)
        frame[4 + 3] = (byte) 0x2C;
        frame[4 + 4] = 0x01;
        // payload byte 9: isMouse
        frame[4 + 9] = 1;
        // payload bytes 10-15: roll = 100, pitch = -200, yaw = 42
        frame[4 + 10] = 100;
        frame[4 + 12] = (byte) 0x38;
        frame[4 + 13] = (byte) 0xFF;
        frame[4 + 14] = 42;

        TapV2Message message = TapV2Parser.parse(frame);
        assertNotNull(message);
        assertEquals(TapV2Message.Type.IMU_MOTION, message.type);

        ImuMotionPacket packet = new ImuMotionPacket(message.payload);
        assertEquals(-5, packet.dx);
        assertEquals(300, packet.dy);
        assertEquals(true, packet.isMouse);
        assertEquals(100, packet.roll);
        assertEquals(-200, packet.pitch);
        assertEquals(42, packet.yaw);
    }

    @Test
    public void imuRaw() {
        byte[] frame = new byte[] { 0, 1, 0, 0, 1, 2, 3, 4 };
        TapV2Message message = TapV2Parser.parse(frame);
        assertNotNull(message);
        assertEquals(TapV2Message.Type.IMU_RAW, message.type);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, message.payload);
    }

    @Test
    public void standbyState() {
        TapV2Message message = TapV2Parser.parse(new byte[] { 2, 0, 0, 0, 1 });
        assertNotNull(message);
        assertEquals(TapV2Message.Type.STANDBY_STATE, message.type);
        assertEquals(1, message.payload[0]);
    }

    @Test
    public void configStates() {
        TapV2Message feature = TapV2Parser.parse(new byte[] { 3, 0, 0, 0, 2, 1 });
        assertNotNull(feature);
        assertEquals(TapV2Message.Type.CONFIG_FEATURE, feature.type);
        assertEquals(2, feature.payload[0]);
        assertEquals(1, feature.payload[1]);

        TapV2Message opMode = TapV2Parser.parse(new byte[] { 3, 1, 0, 0, 2 });
        assertNotNull(opMode);
        assertEquals(TapV2Message.Type.CONFIG_VISION_OP_MODE, opMode.type);
        assertEquals(VisionSensorOpMode.STREAM, VisionSensorOpMode.fromValue(opMode.payload[0]));

        TapV2Message model = TapV2Parser.parse(new byte[] { 3, 2, 0, 0, 1 });
        assertNotNull(model);
        assertEquals(TapV2Message.Type.CONFIG_VISION_MODEL, model.type);
        assertEquals(VisionSensorModel.AIR_GESTURE, VisionSensorModel.fromValue(model.payload[0]));

        TapV2Message sensitivity = TapV2Parser.parse(new byte[] { 3, 3, 0, 0, 4, 3 });
        assertNotNull(sensitivity);
        assertEquals(TapV2Message.Type.CONFIG_IMU_SENSITIVITY, sensitivity.type);
        assertEquals(4, sensitivity.payload[0]);
        assertEquals(3, sensitivity.payload[1]);

        TapV2Message haptic = TapV2Parser.parse(new byte[] { 3, 4, 0, 0 });
        assertNotNull(haptic);
        assertEquals(TapV2Message.Type.CONFIG_HAPTIC_PATTERN, haptic.type);
    }

    @Test
    public void malformedFrames() {
        assertNull(TapV2Parser.parse(new byte[] { }));
        assertNull(TapV2Parser.parse(new byte[] { 1, 2 }));                // too short
        assertNull(TapV2Parser.parse(new byte[] { 9, 0, 0, 0 }));         // unknown command
        assertNull(TapV2Parser.parse(new byte[] { 1, 7, 0, 0, 1 }));      // unknown subcommand
        assertNull(TapV2Parser.parse(new byte[] { 1, 2, 0, 0 }));         // tap gesture without payload
        assertNull(TapV2Parser.parse(new byte[] { 3, 0, 0, 0, 1 }));      // config feature payload too short
        assertNull(TapV2Parser.parse(new byte[] { 2, 0, 0, 0 }));         // standby without payload
    }
}
