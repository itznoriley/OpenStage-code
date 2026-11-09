package net.openstage;

import com.google.gson.JsonParseException;
import net.openstage.core.animation.*;
import net.openstage.io.AnimationSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AnimationSerializer Parsing, Strict Validation, and Roundtrip Tests")
class AnimationSerializerTest {
    @Test
    @DisplayName("Strict rejection: keyframe with BOTH easing and handles throws JsonParseException")
    void testRejectsBothEasingAndHandles() {
        String jsonWithBoth = """
            {
              "format_version": "1.0",
              "name": "illegal_clip",
              "length": 1.0,
              "bones": {
                "right_arm": {
                  "rotation": {
                    "0.5": {
                      "value": [45.0, 0.0, 0.0],
                      "easing": "easeInOut",
                      "handles": [0.42, 0.0, 0.58, 1.0]
                    }
                  }
                }
              }
            }
            """;

        JsonParseException ex = assertThrows(JsonParseException.class, () ->
                AnimationSerializer.fromJson(jsonWithBoth));
        assertTrue(ex.getMessage().contains("BOTH"),
                "Exception message must mention that both easing and handles were set: " + ex.getMessage());
    }

    @Test
    @DisplayName("Keyframe with handles only parses correctly")
    void testKeyframeWithHandlesOnly() {
        String json = """
            {
              "format_version": "1.0",
              "name": "handles_only_clip",
              "length": 2.0,
              "bones": {
                "body": {
                  "position": {
                    "0.5": {
                      "value": [1.0, 2.0, 3.0],
                      "handles": [0.25, 0.1, 0.25, 1.0]
                    }
                  }
                }
              }
            }
            """;

        AnimationClip clip = AnimationSerializer.fromJson(json);
        assertNotNull(clip);
        BoneTrack bodyTrack = clip.getBones().get("body");
        assertNotNull(bodyTrack);
        Keyframe kf = bodyTrack.getPositionKeys().get(0.5f);
        assertNotNull(kf);
        assertArrayEquals(new float[]{1.0f, 2.0f, 3.0f}, kf.value(), 1e-6f);
        assertNull(kf.easing(), "Easing should be null when only handles are provided");
        assertNotNull(kf.handles());
        assertArrayEquals(new float[]{0.25f, 0.1f, 0.25f, 1.0f}, kf.handles(), 1e-6f);
    }

    @Test
    @DisplayName("Keyframe with easing only parses correctly")
    void testKeyframeWithEasingOnly() {
        String json = """
            {
              "format_version": "1.0",
              "name": "easing_only_clip",
              "length": 2.0,
              "bones": {
                "head": {
                  "rotation": {
                    "0.75": {
                      "value": [10.0, 20.0, 30.0],
                      "easing": "bounce"
                    }
                  }
                }
              }
            }
            """;

        AnimationClip clip = AnimationSerializer.fromJson(json);
        assertNotNull(clip);
        BoneTrack headTrack = clip.getBones().get("head");
        assertNotNull(headTrack);
        Keyframe kf = headTrack.getRotationKeys().get(0.75f);
        assertNotNull(kf);
        assertArrayEquals(new float[]{10.0f, 20.0f, 30.0f}, kf.value(), 1e-6f);
        assertEquals(EasingType.BOUNCE, kf.easing());
        assertNull(kf.handles(), "Handles should be null when only easing is provided");
    }

    @Test
    @DisplayName("Plain array and expression keyframes parse correctly")
    void testPlainArrayAndExpressionKeyframes() {
        String json = """
            {
              "format_version": "1.0",
              "name": "mixed_clip",
              "length": 3.0,
              "bones": {
                "right_leg": {
                  "rotation": {
                    "0.0": [0.0, 15.0, 0.0],
                    "1.0": "math.sin(t * 90) * 20"
                  }
                }
              }
            }
            """;

        AnimationClip clip = AnimationSerializer.fromJson(json);
        BoneTrack legTrack = clip.getBones().get("right_leg");
        assertNotNull(legTrack);
        Keyframe kf0 = legTrack.getRotationKeys().get(0.0f);
        assertNotNull(kf0);
        assertArrayEquals(new float[]{0.0f, 15.0f, 0.0f}, kf0.value(), 1e-6f);
        assertFalse(kf0.isExpression());
        Keyframe kf1 = legTrack.getRotationKeys().get(1.0f);
        assertNotNull(kf1);
        assertTrue(kf1.isExpression());
        assertEquals("math.sin(t * 90) * 20", kf1.expressionSource());
    }

    @Test
    @DisplayName("Roundtrip serialization to JSON and back preserves all animation data")
    void testRoundtripSerialization() {
        Locale prevLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            AnimationClip original = new AnimationClip("walk_cycle", "1.0", 2.5f, true);
            BoneTrack rightArm = original.getOrCreateBone("right_arm");
            rightArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{0, 0, 0}, null, null));
            rightArm.addKeyframe(ChannelType.ROTATION, new Keyframe(0.5f, new float[]{30, 0, 0}, EasingType.EASE_IN_OUT, null));
            rightArm.addKeyframe(ChannelType.ROTATION, new Keyframe(1.0f, new float[]{60, 0, 0}, null, new float[]{0.2f, 0.8f}));
            rightArm.addKeyframe(ChannelType.ROTATION, new Keyframe(2.0f, new float[]{0, 0, 0}, null, null, "math.sin(t * 180)"));
            BoneTrack body = original.getOrCreateBone("body");
            body.addKeyframe(ChannelType.POSITION, new Keyframe(0.0f, new float[]{0, 1, 0}, null, null));
            body.addKeyframe(ChannelType.POSITION, new Keyframe(1.25f, new float[]{0, 1.2f, 0}, EasingType.BOUNCE, null));
            body.addKeyframe(ChannelType.SCALE, new Keyframe(0.0f, new float[]{1, 1, 1}, null, null));
            String json = AnimationSerializer.toJson(original);
            assertNotNull(json);
            AnimationClip restored = AnimationSerializer.fromJson(json);
            assertNotNull(restored);

            assertEquals(original.getName(), restored.getName());
            assertEquals(original.getFormatVersion(), restored.getFormatVersion());
            assertEquals(original.getLength(), restored.getLength(), 1e-6f);
            assertEquals(original.isLooping(), restored.isLooping());

            assertEquals(2, restored.getBones().size());
            BoneTrack restoredArm = restored.getBones().get("right_arm");
            assertNotNull(restoredArm);
            assertEquals(4, restoredArm.getRotationKeys().size());
            Keyframe armKf0 = restoredArm.getRotationKeys().get(0.0f);
            assertEquals(new Keyframe(0.0f, new float[]{0, 0, 0}, null, null), armKf0);
            Keyframe armKf05 = restoredArm.getRotationKeys().get(0.5f);
            assertEquals(EasingType.EASE_IN_OUT, armKf05.easing());
            assertArrayEquals(new float[]{30, 0, 0}, armKf05.value(), 1e-6f);
            Keyframe armKf10 = restoredArm.getRotationKeys().get(1.0f);
            assertNull(armKf10.easing());
            assertArrayEquals(new float[]{0.2f, 0.8f}, armKf10.handles(), 1e-6f);
            Keyframe armKf20 = restoredArm.getRotationKeys().get(2.0f);
            assertTrue(armKf20.isExpression());
            assertEquals("math.sin(t * 180)", armKf20.expressionSource());
            String reJson = AnimationSerializer.toJson(restored);
            assertEquals(json, reJson);
        } finally {
            Locale.setDefault(prevLocale);
        }
    }

    @Test
    @DisplayName("TextTrack parsing and saving with all properties (position, effect, duration, colors)")
    void testTextTrackParsingAndSavingAllProperties() {
        AnimationClip clip = new AnimationClip("cinematic", 5.0f);
        TextCard card1 = new TextCard("card-intro", 0.5f, 3.5f, "Chapter 1: The Beginning");
        card1.setPosition(TextCard.PositionPreset.TOP_CENTER);
        card1.setEffect(TextCard.AnimationEffect.FADE);
        card1.setFontSize(2.5f);
        card1.setFadeIn(0.4f);
        card1.setFadeOut(0.6f);
        card1.setOffsetX(12.5f);
        card1.setOffsetY(-48.0f);
        card1.setTextColor(0xFFEEDDCC);
        card1.setBackgroundColor(0x99112233);
        TextCard card2 = new TextCard("card-lower-third", 3.0f, 5.0f, "Speaker: Alex");
        card2.setPosition(TextCard.PositionPreset.LOWER_THIRD_LEFT);
        card2.setEffect(TextCard.AnimationEffect.SLIDE_UP);
        card2.setFontSize(1.2f);
        card2.setFadeIn(0.2f);
        card2.setFadeOut(0.3f);
        card2.setOffsetX(5.0f);
        card2.setOffsetY(-10.0f);
        card2.setTextColor(0xFF00FF00);
        card2.setBackgroundColor(0x80000000);

        clip.getTextTrack().addCard(card1);
        clip.getTextTrack().addCard(card2);

        assertEquals(3.0f, card1.getDuration(), 1e-6f);
        assertEquals(2.0f, card2.getDuration(), 1e-6f);
        String json = AnimationSerializer.toJson(clip);
        assertNotNull(json);
        assertTrue(json.contains("text_track"));
        AnimationClip restored = AnimationSerializer.fromJson(json);
        assertNotNull(restored);
        List<TextCard> cards = restored.getTextTrack().getCards();
        assertEquals(2, cards.size());
        TextCard r1 = cards.get(0);
        assertEquals("card-intro", r1.getId());
        assertEquals(0.5f, r1.getStartTime(), 1e-6f);
        assertEquals(3.5f, r1.getEndTime(), 1e-6f);
        assertEquals(3.0f, r1.getDuration(), 1e-6f, "Duration must equal endTime - startTime");
        assertEquals("Chapter 1: The Beginning", r1.getText());
        assertEquals(TextCard.PositionPreset.TOP_CENTER, r1.getPosition());
        assertEquals(TextCard.AnimationEffect.FADE, r1.getEffect());
        assertEquals(2.5f, r1.getFontSize(), 1e-6f);
        assertEquals(0.4f, r1.getFadeIn(), 1e-6f);
        assertEquals(0.6f, r1.getFadeOut(), 1e-6f);
        assertEquals(12.5f, r1.getOffsetX(), 1e-6f);
        assertEquals(-48.0f, r1.getOffsetY(), 1e-6f);
        assertEquals(0xFFEEDDCC, r1.getTextColor());
        assertEquals(0x99112233, r1.getBackgroundColor());
        TextCard r2 = cards.get(1);
        assertEquals("card-lower-third", r2.getId());
        assertEquals(3.0f, r2.getStartTime(), 1e-6f);
        assertEquals(5.0f, r2.getEndTime(), 1e-6f);
        assertEquals(2.0f, r2.getDuration(), 1e-6f);
        assertEquals("Speaker: Alex", r2.getText());
        assertEquals(TextCard.PositionPreset.LOWER_THIRD_LEFT, r2.getPosition());
        assertEquals(TextCard.AnimationEffect.SLIDE_UP, r2.getEffect());
        assertEquals(1.2f, r2.getFontSize(), 1e-6f);
        assertEquals(0.2f, r2.getFadeIn(), 1e-6f);
        assertEquals(0.3f, r2.getFadeOut(), 1e-6f);
        assertEquals(5.0f, r2.getOffsetX(), 1e-6f);
        assertEquals(-10.0f, r2.getOffsetY(), 1e-6f);
        assertEquals(0xFF00FF00, r2.getTextColor());
        assertEquals(0x80000000, r2.getBackgroundColor());
    }

    @Test
    @DisplayName("Invalid keyframe value array size throws JsonParseException")
    void testInvalidValueArrayThrows() {
        String jsonBadArray = """
            {
              "bones": {
                "body": {
                  "position": {
                    "0.0": [1.0, 2.0]
                  }
                }
              }
            }
            """;
        assertThrows(JsonParseException.class, () -> AnimationSerializer.fromJson(jsonBadArray));

        String json4Array = """
            {
              "bones": {
                "body": {
                  "position": {
                    "0.0": [1.0, 2.0, 3.0, 4.0]
                  }
                }
              }
            }
            """;
        assertThrows(JsonParseException.class, () -> AnimationSerializer.fromJson(json4Array));
    }

    @Test
    @DisplayName("3D Text Card (After Effects style) transforms and 3D effects roundtrip")
    void test3DTextCardRoundtrip() {
        AnimationClip clip = new AnimationClip("3d_card_clip", 4.0f);
        TextCard card = new TextCard("c-3d", 0.5f, 3.5f, "3D Motion Graphics Title");
        card.setEffect(TextCard.AnimationEffect.FLIP_Y);
        card.setRotX(15.0f);
        card.setRotY(45.0f);
        card.setRotZ(-10.0f);
        card.setOffsetZ(-50.0f);
        card.setScaleX(1.5f);
        card.setScaleY(1.2f);
        clip.getTextTrack().addCard(card);
        String json = AnimationSerializer.toJson(clip);
        AnimationClip deserialized = AnimationSerializer.fromJson(json);
        TextCard loaded = deserialized.getTextTrack().getCards().getFirst();
        assertEquals("3D Motion Graphics Title", loaded.getText());
        assertEquals(TextCard.AnimationEffect.FLIP_Y, loaded.getEffect());
        assertEquals(15.0f, loaded.getRotX(), 1e-4f);
        assertEquals(45.0f, loaded.getRotY(), 1e-4f);
        assertEquals(-10.0f, loaded.getRotZ(), 1e-4f);
        assertEquals(-50.0f, loaded.getOffsetZ(), 1e-4f);
        assertEquals(1.5f, loaded.getScaleX(), 1e-4f);
        assertEquals(1.2f, loaded.getScaleY(), 1e-4f);
    }

    @Test
    @DisplayName("Camera track serialization and interpolation roundtrip")
    void testCameraTrackRoundtrip() {
        AnimationClip clip = new AnimationClip("cinematic_cam", 5.0f);
        clip.getCameraTrack().addKey(new net.openstage.core.scene.CameraTrack.CameraKeyframe(
                0.0f, 0.0f, 1.8f, -4.0f, 0.0f, 15.0f, 0.0f, 70.0f, EasingType.EASE_IN_OUT
        ));
        clip.getCameraTrack().addKey(new net.openstage.core.scene.CameraTrack.CameraKeyframe(
                5.0f, 3.0f, 2.5f, -1.0f, 45.0f, 5.0f, -5.0f, 45.0f, null
        ));
        String json = AnimationSerializer.toJson(clip);
        AnimationClip deserialized = AnimationSerializer.fromJson(json);

        assertEquals(2, deserialized.getCameraTrack().getKeyframes().size());
        net.openstage.core.scene.CameraTrack.CameraKeyframe mid = deserialized.getCameraTrack().interpolateAt(2.5f);
        assertTrue(mid.x() > 0.0f && mid.x() < 3.0f);
        assertTrue(mid.fov() < 70.0f && mid.fov() > 45.0f);
    }

    @Test
    @DisplayName("Parse master cutest kitty scene with multi-actor bones")
    void testParseMasterScene() throws Exception {
        java.io.File f = new java.io.File("run/openstage/animations/cutest_kitty_scene.osanim.json");
        if (f.exists()) {
            String json = java.nio.file.Files.readString(f.toPath());
            AnimationClip clip = AnimationSerializer.fromJson(json);
            assertNotNull(clip);
            assertTrue(clip.hasActorPrefixes());
            assertEquals(224, clip.getCameraTrack().getKeyframes().size());
            assertNotNull(clip.getBones().get("actor0:right_arm"));
            assertEquals(224, clip.getBones().get("actor0:right_arm").getRotationKeys().size());
            assertEquals(224, clip.getBones().get("actor1:head").getRotationKeys().size());
            assertEquals(4, clip.getTextTrack().getCards().size());
            java.util.Map<String, float[]> a0IntroPos = clip.evaluatePositionsForActor(1.0f, 0);
            assertEquals(0.52f, a0IntroPos.get("root")[0], 1e-3f);
            assertEquals(0.0f, a0IntroPos.get("body")[1], 1e-3f);
            assertEquals(0.0f, a0IntroPos.get("head")[1], 1e-3f);
            java.util.Map<String, float[]> a0PetRot = clip.evaluateFrameForActor(5.0f, 0);
            assertEquals(0.0f, a0PetRot.get("body")[0], 1e-3f);
            assertTrue(a0PetRot.get("right_arm")[0] < -50.0f);
            java.util.Map<String, float[]> a1IntroPos = clip.evaluatePositionsForActor(1.0f, 1);
            assertEquals(-0.58f, a1IntroPos.get("root")[1], 1e-3f);
            assertEquals(0.0f, a1IntroPos.get("body")[1], 1e-3f);
            assertEquals(0.0f, a1IntroPos.get("head")[1], 1e-3f);
            java.util.Map<String, float[]> a1ScatterPos = clip.evaluatePositionsForActor(8.2f, 1);
            assertTrue(a1ScatterPos.get("head")[1] > 0.0f);
            assertTrue(a1ScatterPos.get("body")[2] > 1.0f);
        }
    }
}

