package net.openstage;

import net.minecraft.core.BlockPos;
import net.openstage.core.animation.AnimationClip;
import net.openstage.core.scene.CameraTrack;
import net.openstage.core.scene.EnvironmentKeyframe;
import net.openstage.core.scene.EnvironmentTrack;
import net.openstage.io.AnimationSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EnvironmentTrack and Scene Transition Tests")
class EnvironmentTrackTest {

    @Test
    @DisplayName("Empty environment track returns null or fallbacks")
    void testEmptyTrack() {
        EnvironmentTrack track = new EnvironmentTrack();
        assertTrue(track.isEmpty());
        assertNull(track.getActiveKeyframeAt(0.0f));

        BlockPos fallbackPos = new BlockPos(10, 64, 20);
        assertEquals(fallbackPos, track.getAnchorAt(0.0f, fallbackPos));
        assertEquals(EnvironmentKeyframe.EnvironmentMode.DARK_VOID,
                track.getModeAt(0.0f, EnvironmentKeyframe.EnvironmentMode.DARK_VOID));
        assertEquals(0.0f, track.getFadeAlphaAt(0.0f));
    }

    @Test
    @DisplayName("Scene anchors and modes evaluate accurately across cut transitions")
    void testSceneTransitions() {
        EnvironmentTrack track = new EnvironmentTrack();
        BlockPos housePos = new BlockPos(100, 65, 200);
        BlockPos plainsPos = new BlockPos(500, 70, 800);

        track.addKey(new EnvironmentKeyframe(0.0f, EnvironmentKeyframe.EnvironmentMode.DARK_VOID, BlockPos.ZERO,
                EnvironmentKeyframe.EnvironmentTransition.CUT, 0.0f));

        track.addKey(new EnvironmentKeyframe(2.0f, EnvironmentKeyframe.EnvironmentMode.WORLD, housePos,
                EnvironmentKeyframe.EnvironmentTransition.CUT, 0.0f));

        track.addKey(new EnvironmentKeyframe(5.0f, EnvironmentKeyframe.EnvironmentMode.WORLD, plainsPos,
                EnvironmentKeyframe.EnvironmentTransition.CUT, 0.0f));

        track.addKey(new EnvironmentKeyframe(8.0f, EnvironmentKeyframe.EnvironmentMode.CHROMA_GREEN, BlockPos.ZERO,
                EnvironmentKeyframe.EnvironmentTransition.CUT, 0.0f));

        assertEquals(EnvironmentKeyframe.EnvironmentMode.DARK_VOID,
                track.getModeAt(0.5f, EnvironmentKeyframe.EnvironmentMode.DAYLIGHT));

        assertEquals(EnvironmentKeyframe.EnvironmentMode.WORLD,
                track.getModeAt(2.5f, EnvironmentKeyframe.EnvironmentMode.DARK_VOID));
        assertEquals(housePos, track.getAnchorAt(2.5f, BlockPos.ZERO));

        assertEquals(EnvironmentKeyframe.EnvironmentMode.WORLD,
                track.getModeAt(6.0f, EnvironmentKeyframe.EnvironmentMode.DARK_VOID));
        assertEquals(plainsPos, track.getAnchorAt(6.0f, BlockPos.ZERO));

        assertEquals(EnvironmentKeyframe.EnvironmentMode.CHROMA_GREEN,
                track.getModeAt(9.0f, EnvironmentKeyframe.EnvironmentMode.DARK_VOID));
    }

    @Test
    @DisplayName("Fade transition alpha computes smooth fade dip")
    void testFadeTransition() {
        EnvironmentTrack track = new EnvironmentTrack();
        track.addKey(new EnvironmentKeyframe(0.0f, EnvironmentKeyframe.EnvironmentMode.DARK_VOID, BlockPos.ZERO,
                EnvironmentKeyframe.EnvironmentTransition.CUT, 0.0f));

        track.addKey(new EnvironmentKeyframe(3.0f, EnvironmentKeyframe.EnvironmentMode.WORLD, new BlockPos(10, 64, 10),
                EnvironmentKeyframe.EnvironmentTransition.FADE, 1.0f));

        assertEquals(0.0f, track.getFadeAlphaAt(2.9f));

        assertEquals(1.0f, track.getFadeAlphaAt(3.0f), 0.01f);

        assertEquals(0.5f, track.getFadeAlphaAt(3.25f), 0.05f);

        assertEquals(0.0f, track.getFadeAlphaAt(3.5f), 0.01f);
        assertEquals(0.0f, track.getFadeAlphaAt(4.0f));
    }

    @Test
    @DisplayName("Multi-scene animation file with 3 background changes parses and validates")
    void testWalkScenesAnimationFile() throws Exception {
        String json = "{\n" +
                "  \"format_version\": \"1.0\",\n" +
                "  \"name\": \"WalkScenes\",\n" +
                "  \"length\": 5.0,\n" +
                "  \"looping\": true,\n" +
                "  \"camera_track\": {\n" +
                "    \"0.0\": { \"pos\": [-2.5, 1.3, -4.2], \"rot\": [0.0, 0.0, 0.0], \"fov\": 70.0 },\n" +
                "    \"5.0\": { \"pos\": [2.5, 1.3, -4.2], \"rot\": [0.0, 0.0, 0.0], \"fov\": 70.0 }\n" +
                "  },\n" +
                "  \"environment_track\": [\n" +
                "    { \"time\": 0.0, \"mode\": \"WORLD\", \"anchor\": [93, 64, 328], \"transition\": \"CUT\" },\n" +
                "    { \"time\": 1.7, \"mode\": \"WORLD\", \"anchor\": [54, 64, 462], \"transition\": \"CUT\" },\n" +
                "    { \"time\": 3.4, \"mode\": \"WORLD\", \"anchor\": [277, -39, 571], \"transition\": \"CUT\" }\n" +
                "  ]\n" +
                "}";
        AnimationClip clip = AnimationSerializer.fromJson(json);
        assertNotNull(clip);
        assertEquals("WalkScenes", clip.getName());
        assertEquals(5.0f, clip.getLength(), 1e-4f);
        assertEquals(3, clip.getEnvironmentTrack().getKeyframes().size());

        assertEquals(new BlockPos(93, 64, 328), clip.getEnvironmentTrack().getAnchorAt(0.5f, BlockPos.ZERO));

        assertEquals(new BlockPos(54, 64, 462), clip.getEnvironmentTrack().getAnchorAt(2.5f, BlockPos.ZERO));

        assertEquals(new BlockPos(277, -39, 571), clip.getEnvironmentTrack().getAnchorAt(4.0f, BlockPos.ZERO));

        CameraTrack.CameraKeyframe camAtZero = clip.getCameraTrack().interpolateAt(0.0f);
        assertEquals(-2.5f, camAtZero.x(), 0.05f);
        CameraTrack.CameraKeyframe camAtEnd = clip.getCameraTrack().interpolateAt(5.0f);
        assertEquals(2.5f, camAtEnd.x(), 0.05f);
    }

    @Test
    @DisplayName("Serialization and deserialization roundtrip preserves environment track")
    void testSerializationRoundtrip() {
        AnimationClip clip = new AnimationClip("scene_test", 10.0f);
        BlockPos castlePos = new BlockPos(350, 80, -120);

        clip.getEnvironmentTrack().addKey(new EnvironmentKeyframe(0.0f,
                EnvironmentKeyframe.EnvironmentMode.DARK_VOID, BlockPos.ZERO,
                EnvironmentKeyframe.EnvironmentTransition.CUT, 0.0f));
        clip.getEnvironmentTrack().addKey(new EnvironmentKeyframe(4.5f,
                EnvironmentKeyframe.EnvironmentMode.WORLD, castlePos,
                EnvironmentKeyframe.EnvironmentTransition.FADE, 0.8f));

        String json = AnimationSerializer.toJson(clip);
        assertNotNull(json);
        assertTrue(json.contains("environment_track"));
        assertTrue(json.contains("DARK_VOID"));
        assertTrue(json.contains("WORLD"));

        AnimationClip loaded = AnimationSerializer.fromJson(json);
        assertNotNull(loaded);
        assertEquals(2, loaded.getEnvironmentTrack().getKeyframes().size());

        EnvironmentKeyframe kf2 = loaded.getEnvironmentTrack().getActiveKeyframeAt(5.0f);
        assertNotNull(kf2);
        assertEquals(EnvironmentKeyframe.EnvironmentMode.WORLD, kf2.mode());
        assertEquals(350, kf2.anchorX());
        assertEquals(80, kf2.anchorY());
        assertEquals(-120, kf2.anchorZ());
        assertEquals(EnvironmentKeyframe.EnvironmentTransition.FADE, kf2.transition());
        assertEquals(0.8f, kf2.fadeDuration(), 0.001f);
    }
}

