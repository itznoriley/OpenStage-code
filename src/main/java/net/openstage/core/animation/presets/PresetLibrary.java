package net.openstage.core.animation.presets;

import net.openstage.core.animation.*;

public final class PresetLibrary {
    private PresetLibrary() {}

    public static AnimationClip createTPose() {
        AnimationClip clip = new AnimationClip("t_pose", 1.0f);
        clip.setLooping(false);

        clip.getOrCreateBone("right_arm").addKeyframe(ChannelType.ROTATION,
                new Keyframe(0.0f, new float[]{0.0f, 0.0f, 90.0f}, EasingType.LINEAR, null));

        clip.getOrCreateBone("left_arm").addKeyframe(ChannelType.ROTATION,
                new Keyframe(0.0f, new float[]{0.0f, 0.0f, -90.0f}, EasingType.LINEAR, null));

        clip.getOrCreateBone("body").addKeyframe(ChannelType.ROTATION,
                new Keyframe(0.0f, new float[]{0.0f, 0.0f, 0.0f}, EasingType.LINEAR, null));

        return clip;
    }

    public static AnimationClip createIdle() {
        AnimationClip clip = new AnimationClip("idle", 2.0f);
        clip.setLooping(true);
        BoneTrack body = clip.getOrCreateBone("body");
        body.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{0.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        body.addKeyframe(ChannelType.ROTATION, new Keyframe(1.0f, new float[]{1.5f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        body.addKeyframe(ChannelType.ROTATION, new Keyframe(2.0f, new float[]{0.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        BoneTrack head = clip.getOrCreateBone("head");
        head.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{0.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        head.addKeyframe(ChannelType.ROTATION, new Keyframe(1.0f, new float[]{-1.5f, 1.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        head.addKeyframe(ChannelType.ROTATION, new Keyframe(2.0f, new float[]{0.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        BoneTrack rArm = clip.getOrCreateBone("right_arm");
        rArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{0.0f, 0.0f, 2.0f}, EasingType.EASE_IN_OUT, null));
        rArm.addKeyframe(ChannelType.ROTATION, new Keyframe(1.0f, new float[]{0.0f, 0.0f, 6.0f}, EasingType.EASE_IN_OUT, null));
        rArm.addKeyframe(ChannelType.ROTATION, new Keyframe(2.0f, new float[]{0.0f, 0.0f, 2.0f}, EasingType.EASE_IN_OUT, null));
        BoneTrack lArm = clip.getOrCreateBone("left_arm");
        lArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{0.0f, 0.0f, -2.0f}, EasingType.EASE_IN_OUT, null));
        lArm.addKeyframe(ChannelType.ROTATION, new Keyframe(1.0f, new float[]{0.0f, 0.0f, -6.0f}, EasingType.EASE_IN_OUT, null));
        lArm.addKeyframe(ChannelType.ROTATION, new Keyframe(2.0f, new float[]{0.0f, 0.0f, -2.0f}, EasingType.EASE_IN_OUT, null));

        return clip;
    }

    public static AnimationClip createWalk() {
        AnimationClip clip = new AnimationClip("walk", 1.0f);
        clip.setLooping(true);
        BoneTrack rLeg = clip.getOrCreateBone("right_leg");
        rLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{25.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        rLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(0.5f, new float[]{-25.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        rLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(1.0f, new float[]{25.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        BoneTrack lLeg = clip.getOrCreateBone("left_leg");
        lLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{-25.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        lLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(0.5f, new float[]{25.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        lLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(1.0f, new float[]{-25.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        BoneTrack rArm = clip.getOrCreateBone("right_arm");
        rArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{-20.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        rArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.5f, new float[]{20.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        rArm.addKeyframe(ChannelType.ROTATION, new Keyframe(1.0f, new float[]{-20.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        BoneTrack lArm = clip.getOrCreateBone("left_arm");
        lArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{20.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        lArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.5f, new float[]{-20.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        lArm.addKeyframe(ChannelType.ROTATION, new Keyframe(1.0f, new float[]{20.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));

        return clip;
    }

    public static AnimationClip createRun() {
        AnimationClip clip = new AnimationClip("run", 0.6f);
        clip.setLooping(true);
        BoneTrack body = clip.getOrCreateBone("body");
        body.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{0.0f, 0.0f, 0.0f}, EasingType.LINEAR, null));
        BoneTrack rLeg = clip.getOrCreateBone("right_leg");
        rLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{45.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        rLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(0.3f, new float[]{-45.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        rLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(0.6f, new float[]{45.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        BoneTrack lLeg = clip.getOrCreateBone("left_leg");
        lLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{-45.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        lLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(0.3f, new float[]{45.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        lLeg.addKeyframe(ChannelType.ROTATION, new Keyframe(0.6f, new float[]{-45.0f, 0.0f, 0.0f}, EasingType.EASE_IN_OUT, null));
        BoneTrack rArm = clip.getOrCreateBone("right_arm");
        rArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{-45.0f, 0.0f, 5.0f}, EasingType.EASE_IN_OUT, null));
        rArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.3f, new float[]{45.0f, 0.0f, 5.0f}, EasingType.EASE_IN_OUT, null));
        rArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.6f, new float[]{-45.0f, 0.0f, 5.0f}, EasingType.EASE_IN_OUT, null));
        BoneTrack lArm = clip.getOrCreateBone("left_arm");
        lArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{45.0f, 0.0f, -5.0f}, EasingType.EASE_IN_OUT, null));
        lArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.3f, new float[]{-45.0f, 0.0f, -5.0f}, EasingType.EASE_IN_OUT, null));
        lArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.6f, new float[]{45.0f, 0.0f, -5.0f}, EasingType.EASE_IN_OUT, null));

        return clip;
    }

    public static AnimationClip createWave() {
        AnimationClip clip = new AnimationClip("wave", 1.6f);
        clip.setLooping(true);
        BoneTrack arm = clip.getOrCreateBone("right_arm");
        arm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{0.0f, 0.0f, 145.0f}, EasingType.EASE_IN_OUT, null));
        arm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.4f, new float[]{0.0f, 0.0f, 165.0f}, EasingType.EASE_IN_OUT, null));
        arm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.8f, new float[]{0.0f, 0.0f, 135.0f}, EasingType.EASE_IN_OUT, null));
        arm.addKeyframe(ChannelType.ROTATION, new Keyframe(1.2f, new float[]{0.0f, 0.0f, 165.0f}, EasingType.EASE_IN_OUT, null));
        arm.addKeyframe(ChannelType.ROTATION, new Keyframe(1.6f, new float[]{0.0f, 0.0f, 145.0f}, EasingType.EASE_IN_OUT, null));

        return clip;
    }

    public static AnimationClip createSitting() {
        AnimationClip clip = new AnimationClip("sitting", 1.0f);
        clip.setLooping(false);

        clip.getOrCreateBone("root").addKeyframe(ChannelType.POSITION,
                new Keyframe(0.0f, new float[]{0.0f, -0.55f, 0.0f}, EasingType.LINEAR, null));

        clip.getOrCreateBone("body").addKeyframe(ChannelType.ROTATION,
                new Keyframe(0.0f, new float[]{4.0f, 0.0f, 0.0f}, EasingType.LINEAR, null));

        clip.getOrCreateBone("head").addKeyframe(ChannelType.ROTATION,
                new Keyframe(0.0f, new float[]{-2.0f, 0.0f, 0.0f}, EasingType.LINEAR, null));

        clip.getOrCreateBone("right_leg").addKeyframe(ChannelType.ROTATION,
                new Keyframe(0.0f, new float[]{-80.0f, 15.0f, 5.0f}, EasingType.LINEAR, null));
        clip.getOrCreateBone("left_leg").addKeyframe(ChannelType.ROTATION,
                new Keyframe(0.0f, new float[]{-80.0f, -15.0f, -5.0f}, EasingType.LINEAR, null));

        clip.getOrCreateBone("right_arm").addKeyframe(ChannelType.ROTATION,
                new Keyframe(0.0f, new float[]{-25.0f, 0.0f, 12.0f}, EasingType.LINEAR, null));
        clip.getOrCreateBone("left_arm").addKeyframe(ChannelType.ROTATION,
                new Keyframe(0.0f, new float[]{-25.0f, 0.0f, -12.0f}, EasingType.LINEAR, null));

        return clip;
    }
}
