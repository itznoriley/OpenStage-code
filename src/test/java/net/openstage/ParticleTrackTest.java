package net.openstage;

import net.openstage.core.animation.AnimationClip;
import net.openstage.core.animation.BoneTrack;
import net.openstage.core.animation.ChannelType;
import net.openstage.core.animation.EasingType;
import net.openstage.core.animation.Keyframe;
import net.openstage.core.particle.ParticleEmitterType;
import net.openstage.core.particle.ParticleKeyframe;
import net.openstage.core.particle.ParticleTrack;
import net.openstage.io.AnimationSerializer;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class ParticleTrackTest {
    @Test
    public void testAddAndRetrieveParticleKeyframes() {
        ParticleTrack track = new ParticleTrack();
        ParticleKeyframe kf1 = new ParticleKeyframe(1.0f, "minecraft:flame", ParticleEmitterType.STREAM_CONE, 1.0f, 2.0f, 3.0f, 45.0f, 10.0f, 0.0f, 50, 0.5f, 0.2f, 2.0f, 0xFF5500, 1.2f, -1, "");
        ParticleKeyframe kf0 = new ParticleKeyframe(0.0f, "minecraft:crit", ParticleEmitterType.BURST_SPHERE, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 20, 0.2f, 0.1f, 0.0f, 0x00FFCC, 1.0f, -1, "");

        track.addKeyframe(kf1);
        track.addKeyframe(kf0);
        List<ParticleKeyframe> keys = track.getKeyframes();
        assertEquals(2, keys.size());
        assertEquals(0.0f, keys.get(0).time(), 0.001f);
        assertEquals(1.0f, keys.get(1).time(), 0.001f);

        assertNotNull(track.getKeyframeAt(1.0f));
        assertEquals("minecraft:flame", track.getKeyframeAt(1.0f).particleId());
        assertEquals(45.0f, track.getKeyframeAt(1.0f).rotYaw(), 0.001f);

        track.removeKeyframeAt(0.0f);
        assertEquals(1, track.getKeyframes().size());
        assertNull(track.getKeyframeAt(0.0f));
    }

    @Test
    public void testContinuousEmitterActiveRange() {
        ParticleTrack track = new ParticleTrack();
        ParticleKeyframe continuous = new ParticleKeyframe(1.0f, "minecraft:smoke", ParticleEmitterType.STREAM_CONE, 0, 0, 0, 0, 0, 0, 10, 0.1f, 0.1f, 3.0f, 0, 1.0f, -1, "");
        ParticleKeyframe burst = new ParticleKeyframe(2.0f, "minecraft:explosion", ParticleEmitterType.BURST_SPHERE, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0.0f, 0, 1.0f, -1, "");

        track.addKeyframe(continuous);
        track.addKeyframe(burst);

        assertEquals(0, track.getActiveEmittersAt(0.5f).size());
        assertEquals(1, track.getActiveEmittersAt(1.5f).size());
        assertEquals("minecraft:smoke", track.getActiveEmittersAt(1.5f).get(0).particleId());

        assertEquals(2, track.getActiveEmittersAt(2.0f).size());
        assertEquals(1, track.getActiveEmittersAt(3.5f).size());
        assertEquals(0, track.getActiveEmittersAt(4.5f).size());
    }

    @Test
    public void testSerializationAndDeserialization() {
        AnimationClip clip = new AnimationClip("VFX_Scene", 5.0f);
        ParticleKeyframe pk = new ParticleKeyframe(
                1.5f,
                "minecraft:sonic_boom",
                ParticleEmitterType.STREAM_SPIRAL,
                12.5f, 64.0f, -8.2f,
                90.0f, -30.0f, 15.0f,
                42,
                1.75f,
                0.35f,
                2.5f,
                0x00D9FF,
                1.5f,
                0,
                "body"
        );
        clip.getParticleTrack().addKeyframe(pk);
        String json = AnimationSerializer.toJson(clip);
        assertTrue(json.contains("particle_track"));
        assertTrue(json.contains("sonic_boom"));
        assertTrue(json.contains("STREAM_SPIRAL"));
        AnimationClip loaded = AnimationSerializer.fromJson(json);
        assertEquals(1, loaded.getParticleTrack().getKeyframes().size());
        ParticleKeyframe loadedKey = loaded.getParticleTrack().getKeyframes().get(0);
        assertEquals(1.5f, loadedKey.time(), 0.001f);
        assertEquals("minecraft:sonic_boom", loadedKey.particleId());
        assertEquals(ParticleEmitterType.STREAM_SPIRAL, loadedKey.emitterType());
        assertEquals(12.5f, loadedKey.posX(), 0.001f);
        assertEquals(64.0f, loadedKey.posY(), 0.001f);
        assertEquals(-8.2f, loadedKey.posZ(), 0.001f);
        assertEquals(90.0f, loadedKey.rotYaw(), 0.001f);
        assertEquals(-30.0f, loadedKey.rotPitch(), 0.001f);
        assertEquals(15.0f, loadedKey.rotRoll(), 0.001f);
        assertEquals(42, loadedKey.count());
        assertEquals(1.75f, loadedKey.speed(), 0.001f);
        assertEquals(0.35f, loadedKey.spread(), 0.001f);
        assertEquals(2.5f, loadedKey.duration(), 0.001f);
        assertEquals(0x00D9FF, loadedKey.colorRgb());
        assertEquals(1.5f, loadedKey.size(), 0.001f);
        assertEquals(0, loadedKey.attachedActorIndex());
        assertEquals("body", loadedKey.attachedBone());
    }

    @Test
    public void testMigrateToMultiActor() {
        AnimationClip clip = new AnimationClip("Walk", 2.0f);
        BoneTrack headTrack = clip.getOrCreateBone("head");
        headTrack.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{10, 0, 0}, EasingType.LINEAR, null));
        BoneTrack bodyTrack = clip.getOrCreateBone("body");
        bodyTrack.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{0, 20, 0}, EasingType.LINEAR, null));

        assertFalse(clip.hasActorPrefixes());
        clip.migrateToMultiActor();
        assertTrue(clip.hasActorPrefixes());

        assertNotNull(clip.getBones().get("actor0:head"));
        assertNotNull(clip.getBones().get("actor0:body"));
        assertNull(clip.getBones().get("head"));

        clip.getOrCreateBone("actor1:head").addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{-15, 0, 0}, EasingType.LINEAR, null));
        var actor0Rots = clip.evaluateFrameForActor(0.0f, 0);
        var actor1Rots = clip.evaluateFrameForActor(0.0f, 1);

        assertEquals(10.0f, actor0Rots.get("head")[0], 0.001f);
        assertEquals(-15.0f, actor1Rots.get("head")[0], 0.001f);
    }
}

