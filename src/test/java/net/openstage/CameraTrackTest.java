package net.openstage;

import net.openstage.core.animation.EasingType;
import net.openstage.core.scene.CameraTrack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CameraTrack Adversarial QA and Edge-Case Tests")

class CameraTrackTest {
    @Test

    @DisplayName("interpolateAt on empty track throws IllegalStateException")

    void testEmptyTrackThrowsException() {
        CameraTrack track = new CameraTrack();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> track.interpolateAt(0.0f));

        assertEquals("No camera keyframes defined", ex.getMessage());

        assertThrows(IllegalStateException.class, () -> track.interpolateAt(-1.0f));

        assertThrows(IllegalStateException.class, () -> track.interpolateAt(100.0f));

        assertThrows(IllegalStateException.class, () -> track.interpolateAt(Float.NaN));
    }

    @Test

    @DisplayName("Single keyframe track evaluates to identical keyframe before, at, and after")

    void testSingleKeyframeTrackBeforeAtAfter() {
        CameraTrack track = new CameraTrack();

        CameraTrack.CameraKeyframe single = new CameraTrack.CameraKeyframe(
                5.0f,

                10.0f, 20.0f, 30.0f,

                45.0f, -15.0f, 5.0f,

                75.0f,

                EasingType.EASE_IN_OUT
        );

        track.addKey(single);
        CameraTrack.CameraKeyframe before = track.interpolateAt(1.0f);

        assertEquals(single.x(), before.x(), 1e-4f);

        assertEquals(single.y(), before.y(), 1e-4f);

        assertEquals(single.z(), before.z(), 1e-4f);

        assertEquals(single.yaw(), before.yaw(), 1e-4f);

        assertEquals(single.pitch(), before.pitch(), 1e-4f);

        assertEquals(single.roll(), before.roll(), 1e-4f);

        assertEquals(single.fov(), before.fov(), 1e-4f);
        CameraTrack.CameraKeyframe at = track.interpolateAt(5.0f);

        assertEquals(single.x(), at.x(), 1e-4f);

        assertEquals(single.y(), at.y(), 1e-4f);

        assertEquals(single.z(), at.z(), 1e-4f);
        CameraTrack.CameraKeyframe after = track.interpolateAt(12.0f);

        assertEquals(single.x(), after.x(), 1e-4f);

        assertEquals(single.y(), after.y(), 1e-4f);

        assertEquals(single.z(), after.z(), 1e-4f);
    }

    @Test

    @DisplayName("FOV clamping: values under 10 and over 120 are strictly clamped")

    void testFovClamping() {
        CameraTrack.CameraKeyframe under = new CameraTrack.CameraKeyframe(
                0.0f, 0f, 0f, 0f, 0f, 0f, 0f, -50.0f, EasingType.LINEAR
        );

        assertEquals(CameraTrack.MIN_FOV, under.fov(), 1e-4f);

        CameraTrack.CameraKeyframe atMin = new CameraTrack.CameraKeyframe(
                0.0f, 0f, 0f, 0f, 0f, 0f, 0f, 10.0f, EasingType.LINEAR
        );

        assertEquals(10.0f, atMin.fov(), 1e-4f);

        CameraTrack.CameraKeyframe over = new CameraTrack.CameraKeyframe(
                0.0f, 0f, 0f, 0f, 0f, 0f, 0f, 999.0f, EasingType.LINEAR
        );

        assertEquals(CameraTrack.MAX_FOV, over.fov(), 1e-4f);

        CameraTrack.CameraKeyframe atMax = new CameraTrack.CameraKeyframe(
                0.0f, 0f, 0f, 0f, 0f, 0f, 0f, 120.0f, EasingType.LINEAR
        );

        assertEquals(120.0f, atMax.fov(), 1e-4f);
        CameraTrack track = new CameraTrack();

        track.addKey(new CameraTrack.CameraKeyframe(0.0f, 0f, 0f, 0f, 0f, 0f, 0f, 5.0f, EasingType.LINEAR));

        track.addKey(new CameraTrack.CameraKeyframe(10.0f, 0f, 0f, 0f, 0f, 0f, 0f, 150.0f, EasingType.LINEAR));
        CameraTrack.CameraKeyframe mid = track.interpolateAt(5.0f);

        assertEquals(65.0f, mid.fov(), 1e-4f);

        assertTrue(mid.fov() >= CameraTrack.MIN_FOV && mid.fov() <= CameraTrack.MAX_FOV);
    }

    @Test

    @DisplayName("Interpolating between identical timestamps handles smoothly without error")

    void testInterpolatingBetweenIdenticalTimestamps() {
        CameraTrack track = new CameraTrack();

        CameraTrack.CameraKeyframe first = new CameraTrack.CameraKeyframe(
                2.0f, 1.0f, 2.0f, 3.0f, 10.0f, 20.0f, 0.0f, 60.0f, EasingType.LINEAR
        );

        CameraTrack.CameraKeyframe replacement = new CameraTrack.CameraKeyframe(
                2.0f, 10.0f, 20.0f, 30.0f, 100.0f, 50.0f, 0.0f, 80.0f, EasingType.LINEAR
        );

        track.addKey(first);

        track.addKey(replacement);

        assertEquals(1, track.getKeyframes().size());
        CameraTrack.CameraKeyframe result = track.interpolateAt(2.0f);

        assertEquals(10.0f, result.x(), 1e-4f);

        assertEquals(20.0f, result.y(), 1e-4f);

        assertEquals(30.0f, result.z(), 1e-4f);

        assertEquals(80.0f, result.fov(), 1e-4f);
    }

    @Test

    @DisplayName("Negative time interpolation correctly lerps or clamps")

    void testNegativeTimeInterpolation() {
        CameraTrack track = new CameraTrack();

        track.addKey(new CameraTrack.CameraKeyframe(-10.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 50.0f, EasingType.LINEAR));

        track.addKey(new CameraTrack.CameraKeyframe(-2.0f, 80.0f, 16.0f, -40.0f, 40.0f, 20.0f, 0.0f, 90.0f, EasingType.LINEAR));
        CameraTrack.CameraKeyframe mid = track.interpolateAt(-6.0f);

        assertEquals(40.0f, mid.x(), 1e-4f);

        assertEquals(8.0f, mid.y(), 1e-4f);

        assertEquals(-20.0f, mid.z(), 1e-4f);

        assertEquals(70.0f, mid.fov(), 1e-4f);
        CameraTrack.CameraKeyframe beforeStart = track.interpolateAt(-25.0f);

        assertEquals(0.0f, beforeStart.x(), 1e-4f);
        CameraTrack.CameraKeyframe afterEnd = track.interpolateAt(5.0f);

        assertEquals(80.0f, afterEnd.x(), 1e-4f);
        CameraTrack normalTrack = new CameraTrack();

        normalTrack.addKey(new CameraTrack.CameraKeyframe(0.0f, 10.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, EasingType.LINEAR));

        normalTrack.addKey(new CameraTrack.CameraKeyframe(5.0f, 20.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, EasingType.LINEAR));
        CameraTrack.CameraKeyframe negativeAtNormal = normalTrack.interpolateAt(-10.0f);

        assertEquals(10.0f, negativeAtNormal.x(), 1e-4f);
    }

    @Test

    @DisplayName("Looping and overflow times evaluate correctly without crash or infinite recursion")

    void testLoopingAndOverflowTimes() {
        CameraTrack track = new CameraTrack();

        track.addKey(new CameraTrack.CameraKeyframe(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, EasingType.LINEAR));

        track.addKey(new CameraTrack.CameraKeyframe(10.0f, 100.0f, 50.0f, 20.0f, 0.0f, 0.0f, 0.0f, 70.0f, EasingType.LINEAR));
        CameraTrack.CameraKeyframe overflowPositive = track.interpolateAt(1000.0f);

        assertEquals(100.0f, overflowPositive.x(), 1e-4f);

        assertEquals(50.0f, overflowPositive.y(), 1e-4f);
        CameraTrack.CameraKeyframe extremeMax = track.interpolateAt(Float.MAX_VALUE);

        assertEquals(100.0f, extremeMax.x(), 1e-4f);
        CameraTrack.CameraKeyframe extremeMin = track.interpolateAt(-Float.MAX_VALUE);

        assertEquals(0.0f, extremeMin.x(), 1e-4f);
        CameraTrack.CameraKeyframe nanEval = track.interpolateAt(Float.NaN);

        assertNotNull(nanEval);
        CameraTrack.CameraKeyframe loopMid = track.interpolateAt(15.0f, 10.0f, true);

        assertEquals(50.0f, loopMid.x(), 1e-4f);
        CameraTrack.CameraKeyframe loopWrap = track.interpolateAt(22.5f, 10.0f, true);

        assertEquals(25.0f, loopWrap.x(), 1e-4f);
        CameraTrack.CameraKeyframe loopNegative = track.interpolateAt(-2.5f, 10.0f, true);

        assertEquals(75.0f, loopNegative.x(), 1e-4f);
    }

    @Test

    @DisplayName("Removing keyframe removes it from interpolation")

    void testRemoveKeyframe() {
        CameraTrack track = new CameraTrack();

        track.addKey(new CameraTrack.CameraKeyframe(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, EasingType.LINEAR));

        track.addKey(new CameraTrack.CameraKeyframe(5.0f, 50.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, EasingType.LINEAR));

        track.addKey(new CameraTrack.CameraKeyframe(10.0f, 100.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, EasingType.LINEAR));

        assertEquals(50.0f, track.interpolateAt(5.0f).x(), 1e-4f);

        track.removeKey(5.0f);

        assertEquals(2, track.getKeyframes().size());

        assertEquals(50.0f, track.interpolateAt(5.0f).x(), 1e-4f);
    }

    @Test

    @DisplayName("Sharp cut instantly teleports to keyframe without drifting")

    void testSharpCutInstantTeleport() {
        CameraTrack track = new CameraTrack();

        track.addKey(new CameraTrack.CameraKeyframe(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, EasingType.LINEAR));

        track.addKey(new CameraTrack.CameraKeyframe(2.0f, 10.0f, 20.0f, 30.0f, 90.0f, 15.0f, 0.0f, 80.0f, EasingType.STEP));

        track.addKey(new CameraTrack.CameraKeyframe(4.0f, 50.0f, 60.0f, 70.0f, 180.0f, 0.0f, 0.0f, 90.0f, EasingType.LINEAR));
        CameraTrack.CameraKeyframe beforeCut = track.interpolateAt(1.0f);

        assertEquals(0.0f, beforeCut.x(), 1e-4f);

        assertEquals(0.0f, beforeCut.y(), 1e-4f);

        assertEquals(0.0f, beforeCut.z(), 1e-4f);
        CameraTrack.CameraKeyframe justBeforeCut = track.interpolateAt(1.999f);

        assertEquals(0.0f, justBeforeCut.x(), 1e-4f);
        CameraTrack.CameraKeyframe atCut = track.interpolateAt(2.0f);

        assertEquals(10.0f, atCut.x(), 1e-4f);

        assertEquals(20.0f, atCut.y(), 1e-4f);

        assertEquals(30.0f, atCut.z(), 1e-4f);

        assertEquals(90.0f, atCut.yaw(), 1e-4f);
        CameraTrack.CameraKeyframe smoothMid = track.interpolateAt(3.0f);

        assertEquals(30.0f, smoothMid.x(), 1e-4f);

        assertEquals(40.0f, smoothMid.y(), 1e-4f);

        assertEquals(50.0f, smoothMid.z(), 1e-4f);

        assertEquals(135.0f, smoothMid.yaw(), 1e-4f);
        CameraTrack.CameraKeyframe atEnd = track.interpolateAt(4.0f);

        assertEquals(50.0f, atEnd.x(), 1e-4f);
    }

    @Test

    @DisplayName("Combining sharp and smooth modes across multiple camera keyframes")

    void testCombineSharpAndSmooth() {
        CameraTrack track = new CameraTrack();

        track.addKey(new CameraTrack.CameraKeyframe(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, EasingType.STEP));

        track.addKey(new CameraTrack.CameraKeyframe(2.0f, 100.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, EasingType.STEP));

        track.addKey(new CameraTrack.CameraKeyframe(4.0f, 200.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, EasingType.STEP));

        assertEquals(0.0f, track.interpolateAt(1.0f).x(), 1e-4f);

        assertEquals(100.0f, track.interpolateAt(2.0f).x(), 1e-4f);

        assertEquals(100.0f, track.interpolateAt(3.0f).x(), 1e-4f);

        assertEquals(200.0f, track.interpolateAt(4.0f).x(), 1e-4f);
    }

    @Test
    @DisplayName("Camera uses graph curve handles identical to bone animation curves")
    void testCameraUsesGraphCurveHandles() {
        CameraTrack track = new CameraTrack();
        float[] easeCurve = net.openstage.core.animation.curve.CurvePresetRegistry.findById("ease_1").copyDefaultPoints();
        track.addKey(new CameraTrack.CameraKeyframe(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, null, easeCurve));
        track.addKey(new CameraTrack.CameraKeyframe(2.0f, 100.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 70.0f, null, easeCurve));
        float expectedCurveY = net.openstage.core.animation.curve.CurvePreset.evaluate(easeCurve, 0.5f);
        float expectedX = 100.0f * expectedCurveY;
        CameraTrack.CameraKeyframe mid = track.interpolateAt(1.0f);
        assertEquals(expectedX, mid.x(), 1e-3f);
    }
}

