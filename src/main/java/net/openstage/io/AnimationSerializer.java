package net.openstage.io;

import com.google.gson.*;
import net.openstage.core.animation.*;
import java.util.Locale;
import java.util.Map;

public final class AnimationSerializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AnimationSerializer() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static AnimationClip fromJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String name          = getStringOr(root, "name", "unnamed");
        String formatVersion = getStringOr(root, "format_version", AnimationClip.DEFAULT_FORMAT_VERSION);
        float  length        = getFloatOr(root, "length", 0.0f);
        boolean looping      = getBoolOr(root, "looping", false);
        AnimationClip clip = new AnimationClip(name, formatVersion, length, looping);

        if (root.has("bones") && root.get("bones").isJsonObject()) {
            JsonObject bonesObj = root.getAsJsonObject("bones");

            for (Map.Entry<String, JsonElement> boneEntry : bonesObj.entrySet()) {
                String boneName = boneEntry.getKey();
                BoneTrack track = clip.getOrCreateBone(boneName);

                parseBone(boneEntry.getValue().getAsJsonObject(), track);
            }
        }

        if (root.has("text_track") && root.get("text_track").isJsonArray()) {
            JsonArray textArr = root.getAsJsonArray("text_track");

            for (JsonElement elem : textArr) {
                if (elem.isJsonObject()) {
                    JsonObject cardObj = elem.getAsJsonObject();
                    float start = getFloatOr(cardObj, "start_time", 0.0f);
                    float end = getFloatOr(cardObj, "end_time", start + 2.0f);
                    String text = getStringOr(cardObj, "text", "");
                    String id = getStringOr(cardObj, "id", null);
                    TextCard card = new TextCard(id, start, end, text);

                    if (cardObj.has("style") && cardObj.get("style").isJsonObject()) {
                        JsonObject style = cardObj.getAsJsonObject("style");

                        if (style.has("position")) {
                            try {
                                card.setPosition(TextCard.PositionPreset.valueOf(
                                        style.get("position").getAsString().toUpperCase(Locale.ROOT)));
                            } catch (Exception ignored) {}
                        }

                        if (style.has("effect")) {
                            try {
                                card.setEffect(TextCard.AnimationEffect.valueOf(
                                        style.get("effect").getAsString().toUpperCase(Locale.ROOT)));
                            } catch (Exception ignored) {}
                        }

                        card.setFontSize(getFloatOr(style, "font_size", 1.0f));

                        card.setFadeIn(getFloatOr(style, "fade_in", 0.3f));

                        card.setFadeOut(getFloatOr(style, "fade_out", 0.3f));

                        card.setOffsetX(getFloatOr(style, "offset_x", 0.0f));

                        card.setOffsetY(getFloatOr(style, "offset_y", -24.0f));

                        card.setOffsetZ(getFloatOr(style, "offset_z", 0.0f));

                        card.setRotX(getFloatOr(style, "rot_x", 0.0f));

                        card.setRotY(getFloatOr(style, "rot_y", 0.0f));

                        card.setRotZ(getFloatOr(style, "rot_z", 0.0f));

                        card.setScaleX(getFloatOr(style, "scale_x", 1.0f));

                        card.setScaleY(getFloatOr(style, "scale_y", 1.0f));

                        if (style.has("text_color")) {
                            card.setTextColor(parseColor(style.get("text_color").getAsString(), 0xFFFFFFFF));
                        }

                        if (style.has("background_color")) {
                            card.setBackgroundColor(parseColor(style.get("background_color").getAsString(), 0x88000000));
                        }
                    }

                    clip.getTextTrack().addCard(card);
                }
            }
        }

        JsonObject camObj = null;

        if (root.has("camera_track") && root.get("camera_track").isJsonObject()) {
            camObj = root.getAsJsonObject("camera_track");
        } else if (root.has("cameraTrack") && root.get("cameraTrack").isJsonObject()) {
            camObj = root.getAsJsonObject("cameraTrack");
        }

        if (camObj != null) {
            if (camObj.has("keyframes") && camObj.get("keyframes").isJsonArray()) {
                JsonArray arr = camObj.getAsJsonArray("keyframes");

                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        JsonObject kfObj = el.getAsJsonObject();
                        float time = getFloatOr(kfObj, "time", 0.0f);
                        float x = 0, y = 0, z = 0;

                        if (kfObj.has("pos") && kfObj.get("pos").isJsonArray()) {
                            float[] p = parseArray3(kfObj.getAsJsonArray("pos"));
                            x = p[0]; y = p[1]; z = p[2];
                        } else {
                            x = getFloatOr(kfObj, "x", 0.0f);
                            y = getFloatOr(kfObj, "y", 0.0f);
                            z = getFloatOr(kfObj, "z", 0.0f);
                        }

                        float yaw = 0, pitch = 0, roll = 0;

                        if (kfObj.has("rot") && kfObj.get("rot").isJsonArray()) {
                            float[] r = parseArray3(kfObj.getAsJsonArray("rot"));
                            yaw = r[0]; pitch = r[1]; roll = r[2];
                        } else {
                            yaw = getFloatOr(kfObj, "yaw", 0.0f);
                            pitch = getFloatOr(kfObj, "pitch", 0.0f);
                            roll = getFloatOr(kfObj, "roll", 0.0f);
                        }

                        float fov = getFloatOr(kfObj, "fov", 70.0f);
                        EasingType easing = null;

                        if (kfObj.has("easing")) {
                            easing = EasingType.fromJson(kfObj.get("easing").getAsString());
                        }

                        float[] handles = null;

                        if (kfObj.has("handles") && kfObj.get("handles").isJsonArray()) {
                            com.google.gson.JsonArray hArr = kfObj.getAsJsonArray("handles");
                            handles = new float[hArr.size()];

                            for (int i = 0; i < hArr.size(); i++) {
                                handles[i] = hArr.get(i).getAsFloat();
                            }
                        }

                        clip.getCameraTrack().addKey(new net.openstage.core.scene.CameraTrack.CameraKeyframe(
                                time, x, y, z, yaw, pitch, roll, fov, easing, handles
                        ));
                    }
                }
            } else {
                if (camObj.has("keyframes") && camObj.get("keyframes").isJsonObject()) {
                    camObj = camObj.getAsJsonObject("keyframes");
                }

                for (Map.Entry<String, JsonElement> entry : camObj.entrySet()) {
                    float time;

                    try {
                        time = Float.parseFloat(entry.getKey());
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    if (entry.getValue().isJsonObject()) {
                        JsonObject kfObj = entry.getValue().getAsJsonObject();
                        float x = 0, y = 0, z = 0;

                        if (kfObj.has("pos") && kfObj.get("pos").isJsonArray()) {
                            float[] p = parseArray3(kfObj.getAsJsonArray("pos"));
                            x = p[0]; y = p[1]; z = p[2];
                        } else {
                            x = getFloatOr(kfObj, "x", 0.0f);
                            y = getFloatOr(kfObj, "y", 0.0f);
                            z = getFloatOr(kfObj, "z", 0.0f);
                        }

                        float yaw = 0, pitch = 0, roll = 0;

                        if (kfObj.has("rot") && kfObj.get("rot").isJsonArray()) {
                            float[] r = parseArray3(kfObj.getAsJsonArray("rot"));
                            yaw = r[0]; pitch = r[1]; roll = r[2];
                        } else {
                            yaw = getFloatOr(kfObj, "yaw", 0.0f);
                            pitch = getFloatOr(kfObj, "pitch", 0.0f);
                            roll = getFloatOr(kfObj, "roll", 0.0f);
                        }

                        float fov = getFloatOr(kfObj, "fov", 70.0f);
                        EasingType easing = null;

                        if (kfObj.has("easing")) {
                            easing = EasingType.fromJson(kfObj.get("easing").getAsString());
                        }

                        float[] handles = null;

                        if (kfObj.has("handles") && kfObj.get("handles").isJsonArray()) {
                            com.google.gson.JsonArray hArr = kfObj.getAsJsonArray("handles");
                            handles = new float[hArr.size()];

                            for (int i = 0; i < hArr.size(); i++) {
                                handles[i] = hArr.get(i).getAsFloat();
                            }
                        }

                        clip.getCameraTrack().addKey(new net.openstage.core.scene.CameraTrack.CameraKeyframe(
                                time, x, y, z, yaw, pitch, roll, fov, easing, handles
                        ));
                    }
                }
            }
        }

        if (root.has("environment_track") && root.get("environment_track").isJsonArray()) {
            JsonArray arr = root.getAsJsonArray("environment_track");
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    JsonObject o = el.getAsJsonObject();
                    float time = o.has("time") ? o.get("time").getAsFloat() : 0.0f;
                    net.openstage.core.scene.EnvironmentKeyframe.EnvironmentMode mode = net.openstage.core.scene.EnvironmentKeyframe.EnvironmentMode.WORLD;
                    if (o.has("mode")) {
                        try {
                            mode = net.openstage.core.scene.EnvironmentKeyframe.EnvironmentMode.valueOf(o.get("mode").getAsString());
                        } catch (Exception ignored) {}
                    }
                    int ax = 0, ay = 64, az = 0;
                    if (o.has("anchor") && o.get("anchor").isJsonArray()) {
                        JsonArray a = o.getAsJsonArray("anchor");
                        if (a.size() > 0) ax = a.get(0).getAsInt();
                        if (a.size() > 1) ay = a.get(1).getAsInt();
                        if (a.size() > 2) az = a.get(2).getAsInt();
                    }
                    net.openstage.core.scene.EnvironmentKeyframe.EnvironmentTransition trans = net.openstage.core.scene.EnvironmentKeyframe.EnvironmentTransition.CUT;
                    if (o.has("transition")) {
                        try {
                            trans = net.openstage.core.scene.EnvironmentKeyframe.EnvironmentTransition.valueOf(o.get("transition").getAsString());
                        } catch (Exception ignored) {}
                    }
                    float fadeDuration = o.has("fadeDuration") ? o.get("fadeDuration").getAsFloat() : 0.5f;
                    clip.getEnvironmentTrack().addKey(new net.openstage.core.scene.EnvironmentKeyframe(time, mode, ax, ay, az, trans, fadeDuration));
                }
            }
        }

        if (root.has("particle_track") && root.get("particle_track").isJsonArray()) {
            JsonArray arr = root.getAsJsonArray("particle_track");
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    JsonObject o = el.getAsJsonObject();
                    float time = o.has("time") ? o.get("time").getAsFloat() : 0.0f;
                    String particleId = o.has("particleId") ? o.get("particleId").getAsString() : "minecraft:flame";
                    net.openstage.core.particle.ParticleEmitterType type = net.openstage.core.particle.ParticleEmitterType.BURST_SPHERE;
                    if (o.has("emitterType")) {
                        try {
                            type = net.openstage.core.particle.ParticleEmitterType.valueOf(o.get("emitterType").getAsString());
                        } catch (Exception ignored) {}
                    }
                    float px = 0, py = 0, pz = 0;
                    if (o.has("pos") && o.get("pos").isJsonArray()) {
                        JsonArray p = o.getAsJsonArray("pos");
                        if (p.size() > 0) px = p.get(0).getAsFloat();
                        if (p.size() > 1) py = p.get(1).getAsFloat();
                        if (p.size() > 2) pz = p.get(2).getAsFloat();
                    }
                    float ry = 0, rp = 0, rr = 0;
                    if (o.has("rot") && o.get("rot").isJsonArray()) {
                        JsonArray r = o.getAsJsonArray("rot");
                        if (r.size() > 0) ry = r.get(0).getAsFloat();
                        if (r.size() > 1) rp = r.get(1).getAsFloat();
                        if (r.size() > 2) rr = r.get(2).getAsFloat();
                    }
                    int count = o.has("count") ? o.get("count").getAsInt() : 20;
                    float speed = o.has("speed") ? o.get("speed").getAsFloat() : 0.2f;
                    float spread = o.has("spread") ? o.get("spread").getAsFloat() : 0.15f;
                    float duration = o.has("duration") ? o.get("duration").getAsFloat() : 0.0f;
                    int colorRgb = o.has("colorRgb") ? o.get("colorRgb").getAsInt() : 0xFF5500;
                    float size = o.has("size") ? o.get("size").getAsFloat() : 1.0f;
                    int attachedActor = o.has("attachedActorIndex") ? o.get("attachedActorIndex").getAsInt() : -1;
                    String attachedBone = o.has("attachedBone") ? o.get("attachedBone").getAsString() : "";
                    clip.getParticleTrack().addKeyframe(new net.openstage.core.particle.ParticleKeyframe(
                            time, particleId, type, px, py, pz, ry, rp, rr, count, speed, spread, duration, colorRgb, size, attachedActor, attachedBone
                    ));
                }
            }
        }

        return clip;
    }

    public static String toJson(AnimationClip clip) {
        JsonObject root = new JsonObject();

        root.addProperty("format_version", clip.getFormatVersion());

        root.addProperty("name", clip.getName());

        root.addProperty("length", clip.getLength());

        root.addProperty("looping", clip.isLooping());
        JsonObject bonesObj = new JsonObject();

        for (Map.Entry<String, BoneTrack> entry : clip.getBones().entrySet()) {
            bonesObj.add(entry.getKey(), serializeBone(entry.getValue()));
        }

        root.add("bones", bonesObj);

        if (!clip.getTextTrack().getCards().isEmpty()) {
            JsonArray textArr = new JsonArray();

            for (TextCard card : clip.getTextTrack().getCards()) {
                JsonObject cardObj = new JsonObject();

                cardObj.addProperty("id", card.getId());

                cardObj.addProperty("start_time", card.getStartTime());

                cardObj.addProperty("end_time", card.getEndTime());

                cardObj.addProperty("text", card.getText());
                JsonObject style = new JsonObject();

                style.addProperty("position", card.getPosition().name().toLowerCase(Locale.ROOT));

                style.addProperty("effect", card.getEffect().name().toLowerCase(Locale.ROOT));

                style.addProperty("font_size", card.getFontSize());

                style.addProperty("fade_in", card.getFadeIn());

                style.addProperty("fade_out", card.getFadeOut());

                style.addProperty("offset_x", card.getOffsetX());

                style.addProperty("offset_y", card.getOffsetY());

                style.addProperty("offset_z", card.getOffsetZ());

                style.addProperty("rot_x", card.getRotX());

                style.addProperty("rot_y", card.getRotY());

                style.addProperty("rot_z", card.getRotZ());

                style.addProperty("scale_x", card.getScaleX());

                style.addProperty("scale_y", card.getScaleY());

                style.addProperty("text_color", String.format(Locale.ROOT, "#%08X", card.getTextColor()));

                style.addProperty("background_color", String.format(Locale.ROOT, "#%08X", card.getBackgroundColor()));

                cardObj.add("style", style);

                textArr.add(cardObj);
            }

            root.add("text_track", textArr);
        }

        if (!clip.getCameraTrack().getKeyframes().isEmpty()) {
            JsonObject camObj = new JsonObject();

            for (Map.Entry<Float, net.openstage.core.scene.CameraTrack.CameraKeyframe> entry : clip.getCameraTrack().getKeyframes().entrySet()) {
                net.openstage.core.scene.CameraTrack.CameraKeyframe kf = entry.getValue();
                JsonObject kfObj = new JsonObject();
                JsonArray posArr = new JsonArray();

                posArr.add(kf.x()); posArr.add(kf.y()); posArr.add(kf.z());

                kfObj.add("pos", posArr);
                JsonArray rotArr = new JsonArray();

                rotArr.add(kf.yaw()); rotArr.add(kf.pitch()); rotArr.add(kf.roll());

                kfObj.add("rot", rotArr);

                kfObj.addProperty("fov", kf.fov());

                if (kf.easing() != null) {
                    kfObj.addProperty("easing", kf.easing().getJsonName());
                }

                if (kf.handles() != null && kf.handles().length >= 4) {
                    JsonArray hArr = new JsonArray();

                    for (float h : kf.handles()) {
                        hArr.add(h);
                    }

                    kfObj.add("handles", hArr);
                }

                camObj.add(formatTimeKey(entry.getKey()), kfObj);
            }

            root.add("camera_track", camObj);
        }

        if (!clip.getParticleTrack().getKeyframes().isEmpty()) {
            JsonArray partArr = new JsonArray();
            for (net.openstage.core.particle.ParticleKeyframe pk : clip.getParticleTrack().getKeyframes()) {
                JsonObject pkObj = new JsonObject();
                pkObj.addProperty("time", pk.time());
                pkObj.addProperty("particleId", pk.particleId());
                pkObj.addProperty("emitterType", pk.emitterType().name());
                JsonArray posArr = new JsonArray();
                posArr.add(pk.posX()); posArr.add(pk.posY()); posArr.add(pk.posZ());
                pkObj.add("pos", posArr);
                JsonArray rotArr = new JsonArray();
                rotArr.add(pk.rotYaw()); rotArr.add(pk.rotPitch()); rotArr.add(pk.rotRoll());
                pkObj.add("rot", rotArr);
                pkObj.addProperty("count", pk.count());
                pkObj.addProperty("speed", pk.speed());
                pkObj.addProperty("spread", pk.spread());
                pkObj.addProperty("duration", pk.duration());
                pkObj.addProperty("colorRgb", pk.colorRgb());
                pkObj.addProperty("size", pk.size());
                pkObj.addProperty("attachedActorIndex", pk.attachedActorIndex());
                pkObj.addProperty("attachedBone", pk.attachedBone());
                partArr.add(pkObj);
            }
            root.add("particle_track", partArr);
        }

        if (!clip.getEnvironmentTrack().isEmpty()) {
            JsonArray envArr = new JsonArray();
            for (var entry : clip.getEnvironmentTrack().getKeyframes().entrySet()) {
                net.openstage.core.scene.EnvironmentKeyframe ek = entry.getValue();
                JsonObject ekObj = new JsonObject();
                ekObj.addProperty("time", ek.time());
                ekObj.addProperty("mode", ek.mode().name());
                JsonArray aArr = new JsonArray();
                aArr.add(ek.anchorX()); aArr.add(ek.anchorY()); aArr.add(ek.anchorZ());
                ekObj.add("anchor", aArr);
                ekObj.addProperty("transition", ek.transition().name());
                ekObj.addProperty("fadeDuration", ek.fadeDuration());
                envArr.add(ekObj);
            }
            root.add("environment_track", envArr);
        }

        return GSON.toJson(root);
    }

    private static int parseColor(String hex, int defaultColor) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);

            if (hex.length() == 6) {
                return (int) (0xFF000000L | Long.parseLong(hex, 16));
            } else if (hex.length() == 8) {
                return (int) Long.parseLong(hex, 16);
            }
        } catch (Exception ignored) {}

        return defaultColor;
    }

    private static void parseBone(JsonObject boneObj, BoneTrack track) {
        parseChannel(boneObj, "rotation",  ChannelType.ROTATION, track);

        parseChannel(boneObj, "rotations", ChannelType.ROTATION, track);

        parseChannel(boneObj, "position",  ChannelType.POSITION, track);

        parseChannel(boneObj, "positions", ChannelType.POSITION, track);

        parseChannel(boneObj, "scale",     ChannelType.SCALE,    track);

        parseChannel(boneObj, "scales",    ChannelType.SCALE,    track);
    }

    private static void parseChannel(JsonObject boneObj, String channelName,

                                     ChannelType channel, BoneTrack track) {
        if (!boneObj.has(channelName)) return;
        JsonElement channelElem = boneObj.get(channelName);

        if (channelElem.isJsonObject()) {
            JsonObject keys = channelElem.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : keys.entrySet()) {
                float time = Float.parseFloat(entry.getKey());
                Keyframe kf = parseKeyframe(time, entry.getValue());

                track.addKeyframe(channel, kf);
            }
        }
    }

    private static Keyframe parseKeyframe(float time, JsonElement element) {
        if (element.isJsonArray()) {
            float[] value = parseArray3(element.getAsJsonArray());

            return new Keyframe(time, value, null, null);
        }

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String expr = element.getAsString();

            return new Keyframe(time, new float[]{0, 0, 0}, null, null, expr);
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            float[] value;
            String expressionSource = null;

            if (obj.has("value") || obj.has("values")) {
                JsonElement valElem = obj.has("value") ? obj.get("value") : obj.get("values");

                if (valElem.isJsonArray()) {
                    value = parseArray3(valElem.getAsJsonArray());
                } else if (valElem.isJsonPrimitive() && valElem.getAsJsonPrimitive().isString()) {
                    expressionSource = valElem.getAsString();
                    value = new float[]{0, 0, 0};
                } else {
                    throw new JsonParseException("Invalid 'value' type in keyframe at t=" + time);
                }
            } else {
                value = new float[]{0, 0, 0};
            }

            EasingType easing = null;

            if (obj.has("easing")) {
                easing = EasingType.fromJson(obj.get("easing").getAsString());
            }

            float[] handles = null;

            if (obj.has("handles")) {
                JsonArray hArr = obj.getAsJsonArray("handles");
                handles = new float[hArr.size()];

                for (int i = 0; i < hArr.size(); i++) {
                    handles[i] = hArr.get(i).getAsFloat();
                }
            }

            if (easing != null && handles != null) {
                throw new JsonParseException(
                        "Keyframe at t=" + time + " has BOTH 'easing' and 'handles' set. " +

                        "Only one interpolation method is allowed per keyframe.");
            }

            return new Keyframe(time, value, easing, handles, expressionSource);
        }

        throw new JsonParseException("Unrecognised keyframe format at t=" + time);
    }

    private static JsonObject serializeBone(BoneTrack track) {
        JsonObject obj = new JsonObject();
        JsonObject rot = serializeChannel(track.getRotationKeys());

        if (rot.size() > 0) obj.add("rotation", rot);
        JsonObject pos = serializeChannel(track.getPositionKeys());

        if (pos.size() > 0) obj.add("position", pos);
        JsonObject scl = serializeChannel(track.getScaleKeys());

        if (scl.size() > 0) obj.add("scale", scl);

        return obj;
    }

    private static JsonObject serializeChannel(java.util.NavigableMap<Float, Keyframe> keys) {
        JsonObject obj = new JsonObject();

        for (Map.Entry<Float, Keyframe> entry : keys.entrySet()) {
            String timeKey = formatTimeKey(entry.getKey());

            obj.add(timeKey, serializeKeyframe(entry.getValue()));
        }

        return obj;
    }

    private static JsonElement serializeKeyframe(Keyframe kf) {
        if (kf.isExpression()) {
            if (kf.easing() == null && kf.handles() == null) {
                return new JsonPrimitive(kf.expressionSource());
            }

            JsonObject obj = new JsonObject();

            obj.addProperty("value", kf.expressionSource());

            addInterpolation(obj, kf);

            return obj;
        }

        if (kf.easing() == null && kf.handles() == null) {
            return toJsonArray(kf.value());
        }

        JsonObject obj = new JsonObject();

        obj.add("value", toJsonArray(kf.value()));

        addInterpolation(obj, kf);

        return obj;
    }

    private static void addInterpolation(JsonObject obj, Keyframe kf) {
        if (kf.easing() != null) {
            obj.addProperty("easing", kf.easing().getJsonName());
        }

        if (kf.handles() != null) {
            JsonArray arr = new JsonArray();

            for (float h : kf.handles()) {
                arr.add(h);
            }

            obj.add("handles", arr);
        }
    }

    private static float[] parseArray3(JsonArray arr) {
        if (arr.size() != 3) {
            throw new JsonParseException("Expected 3-component array, got " + arr.size());
        }

        return new float[]{
                arr.get(0).getAsFloat(),

                arr.get(1).getAsFloat(),

                arr.get(2).getAsFloat()
        };
    }

    private static JsonArray toJsonArray(float[] v) {
        JsonArray arr = new JsonArray();

        for (float f : v) arr.add(f);

        return arr;
    }

    private static String formatTimeKey(float time) {
        if (time == (int) time) {
            return String.format(Locale.ROOT, "%.1f", time);
        }

        return Float.toString(time);
    }

    private static String getStringOr(JsonObject obj, String key, String defaultValue) {
        return obj.has(key) ? obj.get(key).getAsString() : defaultValue;
    }

    private static float getFloatOr(JsonObject obj, String key, float defaultValue) {
        return obj.has(key) ? obj.get(key).getAsFloat() : defaultValue;
    }

    private static boolean getBoolOr(JsonObject obj, String key, boolean defaultValue) {
        return obj.has(key) ? obj.get(key).getAsBoolean() : defaultValue;
    }
}

