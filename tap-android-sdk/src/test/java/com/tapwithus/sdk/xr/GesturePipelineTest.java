package com.tapwithus.sdk.xr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Collections;

public class GesturePipelineTest {

    @Test
    public void hardware4ClassIndexesMapToLegacyXrStates() {
        assertEquals(XRGestureState.NONE.code, LegacyXRAirGesture.fromV2Class(100));
        assertEquals(XRGestureState.THUMB_INDEX.code, LegacyXRAirGesture.fromV2Class(105));
        assertEquals(XRGestureState.THUMB_MIDDLE.code, LegacyXRAirGesture.fromV2Class(106));
        assertEquals(XRGestureState.THUMB_RING.code, LegacyXRAirGesture.fromV2Class(107));
        assertEquals(XRGestureState.THUMB_PINKY.code, LegacyXRAirGesture.fromV2Class(108));
        assertEquals(XRGestureState.FIST.code, LegacyXRAirGesture.fromV2Class(109));
        assertEquals(101, LegacyXRAirGesture.fromV2Class(101));
    }

    @Test
    public void v2IndexPinchBecomesClickIndex() {
        GesturePostprocessor post = new GesturePostprocessor(true, false);
        GesturePostprocessor.Step step = post.call(105);
        XRV2AirGesture current = GesturePostprocessor.toXRV2AirGesture(step.current);
        XRV2AirGesture previous = GesturePostprocessor.toXRV2AirGesture(step.previous);
        assertEquals(XRV2AirGesture.CLICK_INDEX, current);
        assertEquals(XRV2AirGesture.NONE, previous);
        assertEquals(Collections.singletonList(XRAirGesture.CLICK_INDEX),
                XRV2AirGestureConverter.convert(current, previous));
    }
}
