package net.openstage.core.animation;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AnimationClip {
    public static final String DEFAULT_FORMAT_VERSION = "1.0";
    private String name;
    private String formatVersion;
    private float length;
    private final Map<String, BoneTrack> bones = new LinkedHashMap<>();
    private final TextTrack textTrack = new TextTrack();
    private final net.openstage.core.scene.CameraTrack cameraTrack = new net.openstage.core.scene.CameraTrack();
    private final net.openstage.core.particle.ParticleTrack particleTrack = new net.openstage.core.particle.ParticleTrack();
    private final net.openstage.core.scene.EnvironmentTrack environmentTrack = new net.openstage.core.scene.EnvironmentTrack();
    private boolean looping;

    public AnimationClip(String name, float length) {
        this(name, DEFAULT_FORMAT_VERSION, length, false);
    }

    public AnimationClip(String name, String formatVersion, float length, boolean looping) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.formatVersion = formatVersion != null ? formatVersion : DEFAULT_FORMAT_VERSION;
        this.length = length;
        this.looping = looping;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = Objects.requireNonNull(name); }

    public String getFormatVersion() { return formatVersion; }

    public void setFormatVersion(String formatVersion) { this.formatVersion = formatVersion; }

    public float getLength() { return length; }

    public void setLength(float length) { this.length = Math.max(0.0f, length); }

    public boolean isLooping() { return looping; }

    public void setLooping(boolean looping) { this.looping = looping; }

    public Map<String, BoneTrack> getBones() {
        return Collections.unmodifiableMap(bones);
    }

    public TextTrack getTextTrack() {
        return textTrack;
    }

    public net.openstage.core.scene.CameraTrack getCameraTrack() {
        return cameraTrack;
    }

    public net.openstage.core.particle.ParticleTrack getParticleTrack() {
        return particleTrack;
    }

    public net.openstage.core.scene.EnvironmentTrack getEnvironmentTrack() {
        return environmentTrack;
    }

    public BoneTrack getOrCreateBone(String name) {
        Objects.requireNonNull(name, "bone name must not be null");
        return bones.computeIfAbsent(name, BoneTrack::new);
    }

    public boolean removeBone(String name) {
        if (name == null) return false;
        return bones.remove(name) != null;
    }

    public Map<String, float[]> evaluateFrame(float time) {
        float t = resolveTime(time);
        Map<String, float[]> result = new HashMap<>();
        for (var entry : bones.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getRotationAt(t));
        }
        return result;
    }

    public Map<String, float[]> evaluatePositions(float time) {
        float t = resolveTime(time);
        Map<String, float[]> result = new HashMap<>();
        for (var entry : bones.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getPositionAt(t));
        }
        return result;
    }

    public Map<String, float[]> evaluateScales(float time) {
        float t = resolveTime(time);
        Map<String, float[]> result = new HashMap<>();
        for (var entry : bones.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getScaleAt(t));
        }
        return result;
    }

    public boolean hasActorPrefixes() {
        for (String k : bones.keySet()) {
            if (k.startsWith("actor") && k.contains(":")) return true;
            if (k.startsWith("noob:") || k.startsWith("cat:")) return true;
        }
        return false;
    }

    public void migrateToMultiActor() {
        if (hasActorPrefixes()) return;
        Map<String, BoneTrack> migrated = new LinkedHashMap<>();
        for (var entry : bones.entrySet()) {
            String k = entry.getKey();
            if (!k.contains(":")) {
                migrated.put("actor0:" + k, entry.getValue());
            } else {
                migrated.put(k, entry.getValue());
            }
        }
        bones.clear();
        bones.putAll(migrated);
    }

    public Map<String, float[]> evaluateFrameForActor(float time, int actorIndex) {
        float t = resolveTime(time);
        Map<String, float[]> result = new HashMap<>();
        String prefix = "actor" + actorIndex + ":";
        String altPrefix = actorIndex == 0 ? "noob:" : (actorIndex == 1 ? "cat:" : null);
        boolean hasPref = hasActorPrefixes();
        for (var entry : bones.entrySet()) {
            String k = entry.getKey();
            if (k.startsWith(prefix)) {
                result.put(k.substring(prefix.length()), entry.getValue().getRotationAt(t));
            } else if (altPrefix != null && k.startsWith(altPrefix)) {
                result.put(k.substring(altPrefix.length()), entry.getValue().getRotationAt(t));
            } else if (!hasPref && actorIndex == 0 && !k.contains(":")) {
                result.put(k, entry.getValue().getRotationAt(t));
            }
        }
        return result;
    }

    public Map<String, float[]> evaluatePositionsForActor(float time, int actorIndex) {
        float t = resolveTime(time);
        Map<String, float[]> result = new HashMap<>();
        String prefix = "actor" + actorIndex + ":";
        String altPrefix = actorIndex == 0 ? "noob:" : (actorIndex == 1 ? "cat:" : null);
        boolean hasPref = hasActorPrefixes();
        for (var entry : bones.entrySet()) {
            String k = entry.getKey();
            if (k.startsWith(prefix)) {
                result.put(k.substring(prefix.length()), entry.getValue().getPositionAt(t));
            } else if (altPrefix != null && k.startsWith(altPrefix)) {
                result.put(k.substring(altPrefix.length()), entry.getValue().getPositionAt(t));
            } else if (!hasPref && actorIndex == 0 && !k.contains(":")) {
                result.put(k, entry.getValue().getPositionAt(t));
            }
        }
        return result;
    }

    public Map<String, float[]> evaluateScalesForActor(float time, int actorIndex) {
        float t = resolveTime(time);
        Map<String, float[]> result = new HashMap<>();
        String prefix = "actor" + actorIndex + ":";
        String altPrefix = actorIndex == 0 ? "noob:" : (actorIndex == 1 ? "cat:" : null);
        boolean hasPref = hasActorPrefixes();
        for (var entry : bones.entrySet()) {
            String k = entry.getKey();
            if (k.startsWith(prefix)) {
                result.put(k.substring(prefix.length()), entry.getValue().getScaleAt(t));
            } else if (altPrefix != null && k.startsWith(altPrefix)) {
                result.put(k.substring(altPrefix.length()), entry.getValue().getScaleAt(t));
            } else if (!hasPref && actorIndex == 0 && !k.contains(":")) {
                result.put(k, entry.getValue().getScaleAt(t));
            }
        }
        return result;
    }

    private float resolveTime(float time) {
        if (!Float.isFinite(length) || length <= 0.0f) return 0.0f;
        if (Float.isNaN(time)) return 0.0f;
        if (looping) {
            time = time % length;
            if (time < 0.0f) time += length;
        } else {
            time = Math.clamp(time, 0.0f, length);
        }
        return time;
    }

    @Override
    public String toString() {
        return "AnimationClip{name='" + name + '\'' +
               ", length=" + length +
               ", bones=" + bones.size() +
               ", looping=" + looping +
               '}';
    }
}

