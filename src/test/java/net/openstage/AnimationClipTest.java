package net.openstage;

import net.openstage.core.animation.AnimationClip;
import net.openstage.core.animation.BoneTrack;
import net.openstage.core.animation.ChannelType;
import net.openstage.core.animation.EasingType;
import net.openstage.core.animation.Keyframe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AnimationClip Adversarial QA and Evaluation Tests")
class AnimationClipTest {
    @Test
    @DisplayName("evaluateFrame with empty tracks returns empty map or default transforms")
    void testEvaluateFrameWithEmptyTracks() {
        AnimationClip emptyClip = new AnimationClip("empty_clip", 2.0f);
        Map<String, float[]> frame = emptyClip.evaluateFrame(0.5f);
        assertNotNull(frame);
        assertTrue(frame.isEmpty());
        Map<String, float[]> pos = emptyClip.evaluatePositions(0.5f);
        assertNotNull(pos);
        assertTrue(pos.isEmpty());
        Map<String, float[]> scl = emptyClip.evaluateScales(0.5f);
        assertNotNull(scl);
        assertTrue(scl.isEmpty());

        emptyClip.getOrCreateBone("head");
        Map<String, float[]> frameWithEmptyBone = emptyClip.evaluateFrame(0.5f);
        assertEquals(1, frameWithEmptyBone.size());
        assertArrayEquals(new float[]{0.0f, 0.0f, 0.0f}, frameWithEmptyBone.get("head"), 1e-4f);
        Map<String, float[]> posWithEmptyBone = emptyClip.evaluatePositions(0.5f);
        assertArrayEquals(new float[]{0.0f, 0.0f, 0.0f}, posWithEmptyBone.get("head"), 1e-4f);
        Map<String, float[]> sclWithEmptyBone = emptyClip.evaluateScales(0.5f);
        assertArrayEquals(new float[]{1.0f, 1.0f, 1.0f}, sclWithEmptyBone.get("head"), 1e-4f);
    }

    @Test
    @DisplayName("evaluateFrame with time < 0 and time > clip.length clamps to boundaries when not looping")
    void testEvaluateFrameClampingWhenNotLooping() {
        AnimationClip clip = new AnimationClip("clamp_clip", 2.0f);
        clip.setLooping(false);
        BoneTrack arm = clip.getOrCreateBone("right_arm");
        arm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{0.0f, 0.0f, 0.0f}, EasingType.LINEAR, null));
        arm.addKeyframe(ChannelType.ROTATION, new Keyframe(2.0f, new float[]{90.0f, 45.0f, -30.0f}, EasingType.LINEAR, null));
        Map<String, float[]> beforeZero = clip.evaluateFrame(-1.0f);
        assertArrayEquals(new float[]{0.0f, 0.0f, 0.0f}, beforeZero.get("right_arm"), 1e-4f);
        Map<String, float[]> deepNegative = clip.evaluateFrame(-999.0f);
        assertArrayEquals(new float[]{0.0f, 0.0f, 0.0f}, deepNegative.get("right_arm"), 1e-4f);
        Map<String, float[]> pastLength = clip.evaluateFrame(3.5f);
        assertArrayEquals(new float[]{90.0f, 45.0f, -30.0f}, pastLength.get("right_arm"), 1e-4f);
        Map<String, float[]> extremePast = clip.evaluateFrame(Float.MAX_VALUE);
        assertArrayEquals(new float[]{90.0f, 45.0f, -30.0f}, extremePast.get("right_arm"), 1e-4f);
        Map<String, float[]> atMid = clip.evaluateFrame(1.0f);
        assertArrayEquals(new float[]{45.0f, 22.5f, -15.0f}, atMid.get("right_arm"), 1e-4f);
        Map<String, float[]> nanTime = clip.evaluateFrame(Float.NaN);
        assertArrayEquals(new float[]{0.0f, 0.0f, 0.0f}, nanTime.get("right_arm"), 1e-4f);
    }

    @Test
    @DisplayName("evaluateFrame looping behavior wraps time modulo length for positive and negative values")
    void testEvaluateFrameLoopingBehavior() {
        AnimationClip clip = new AnimationClip("loop_clip", 4.0f);
        clip.setLooping(true);
        BoneTrack spine = clip.getOrCreateBone("spine");
        spine.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{0.0f, 0.0f, 0.0f}, EasingType.LINEAR, null));
        spine.addKeyframe(ChannelType.ROTATION, new Keyframe(4.0f, new float[]{100.0f, 0.0f, 0.0f}, EasingType.LINEAR, null));
        Map<String, float[]> atMid = clip.evaluateFrame(2.0f);
        assertEquals(50.0f, atMid.get("spine")[0], 1e-4f);
        Map<String, float[]> loopOnce = clip.evaluateFrame(6.0f);
        assertEquals(50.0f, loopOnce.get("spine")[0], 1e-4f);
        Map<String, float[]> loopTwice = clip.evaluateFrame(10.0f);
        assertEquals(50.0f, loopTwice.get("spine")[0], 1e-4f);
        Map<String, float[]> loopBoundary = clip.evaluateFrame(8.0f);
        assertEquals(0.0f, loopBoundary.get("spine")[0], 1e-4f);
        Map<String, float[]> negativeLoop = clip.evaluateFrame(-1.0f);
        assertEquals(75.0f, negativeLoop.get("spine")[0], 1e-4f);
        Map<String, float[]> negativeLoopMultiple = clip.evaluateFrame(-5.0f);
        assertEquals(75.0f, negativeLoopMultiple.get("spine")[0], 1e-4f);
    }

    @Test
    @DisplayName("Dynamically adding and removing bone tracks and channels")
    void testDynamicBoneTracksAndChannels() {
        AnimationClip clip = new AnimationClip("dynamic_clip", 5.0f);
        BoneTrack leftLeg = clip.getOrCreateBone("left_leg");
        assertNotNull(leftLeg);
        assertEquals("left_leg", leftLeg.getBoneName());
        assertTrue(clip.getBones().containsKey("left_leg"));

        leftLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{10.0f, 0.0f, 0.0f}, EasingType.LINEAR, null));
        leftLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(2.0f, new float[]{20.0f, 0.0f, 0.0f}, EasingType.LINEAR, null));
        leftLeg.addKeyframe(ChannelType.POSITION, new Keyframe(0.0f, new float[]{0.0f, 1.0f, 0.0f}, EasingType.LINEAR, null));
        leftLeg.addKeyframe(ChannelType.SCALE, new Keyframe(0.0f, new float[]{1.5f, 1.5f, 1.5f}, EasingType.LINEAR, null));

        assertEquals(2, leftLeg.getRotationKeys().size());
        assertEquals(1, leftLeg.getPositionKeys().size());
        assertEquals(1, leftLeg.getScaleKeys().size());

        leftLeg.removeKeyframe(ChannelType.ROTATION, 2.0f);
        assertEquals(1, leftLeg.getRotationKeys().size());

        leftLeg.clearChannel(ChannelType.ROTATION);
        assertTrue(leftLeg.getRotationKeys().isEmpty());
        assertFalse(leftLeg.getPositionKeys().isEmpty());
        boolean removed = clip.removeBone("left_leg");
        assertTrue(removed);
        assertFalse(clip.getBones().containsKey("left_leg"));
        assertTrue(clip.evaluateFrame(1.0f).isEmpty());

        assertFalse(clip.removeBone("non_existent"));
        assertFalse(clip.removeBone(null));

        assertThrows(NullPointerException.class, () -> clip.getOrCreateBone(null));
        assertThrows(UnsupportedOperationException.class, () -> clip.getBones().remove("left_leg"));
    }

    @Test
    @DisplayName("Zero and negative clip length handled safely without division by zero")
    void testZeroAndNegativeLengthSafety() {
        AnimationClip zeroClip = new AnimationClip("zero", 0.0f);
        zeroClip.setLooping(true);
        BoneTrack bone = zeroClip.getOrCreateBone("root");
        bone.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{10.0f, 20.0f, 30.0f}, EasingType.LINEAR, null));

        assertDoesNotThrow(() -> zeroClip.evaluateFrame(5.0f));
        assertDoesNotThrow(() -> zeroClip.evaluateFrame(-5.0f));

        zeroClip.setLength(-10.0f);
        assertEquals(0.0f, zeroClip.getLength(), 1e-4f);
        assertDoesNotThrow(() -> zeroClip.evaluateFrame(2.0f));
    }
}

