package net.openstage.core.animation;

import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public final class BoneTrack {
    private final String boneName;
    private final NavigableMap<Float, Keyframe> rotationKeys = new TreeMap<>();
    private final NavigableMap<Float, Keyframe> positionKeys = new TreeMap<>();
    private final NavigableMap<Float, Keyframe> scaleKeys = new TreeMap<>();

    public BoneTrack(String boneName) {
        this.boneName = Objects.requireNonNull(boneName, "boneName must not be null");
    }

    public String getBoneName() {
        return boneName;
    }

    public NavigableMap<Float, Keyframe> getRotationKeys() {
        return rotationKeys;
    }

    public NavigableMap<Float, Keyframe> getPositionKeys() {
        return positionKeys;
    }

    public NavigableMap<Float, Keyframe> getScaleKeys() {
        return scaleKeys;
    }

    public float[] getRotationAt(float time) {
        return evaluateChannel(rotationKeys, time, new float[]{0.0f, 0.0f, 0.0f});
    }

    public float[] getPositionAt(float time) {
        return evaluateChannel(positionKeys, time, new float[]{0.0f, 0.0f, 0.0f});
    }

    public float[] getScaleAt(float time) {
        return evaluateChannel(scaleKeys, time, new float[]{1.0f, 1.0f, 1.0f});
    }

    public void addKeyframe(ChannelType channel, Keyframe kf) {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(kf, "keyframe must not be null");
        getMap(channel).put(kf.time(), kf);
    }

    public void removeKeyframe(ChannelType channel, float time) {
        Objects.requireNonNull(channel, "channel must not be null");
        getMap(channel).remove(time);
    }

    public Keyframe getKeyframe(ChannelType channel, float time) {
        Objects.requireNonNull(channel, "channel must not be null");
        return getMap(channel).get(time);
    }

    public void clearChannel(ChannelType channel) {
        Objects.requireNonNull(channel, "channel must not be null");
        getMap(channel).clear();
    }

    private NavigableMap<Float, Keyframe> getMap(ChannelType channel) {
        return switch (channel) {
            case ROTATION -> rotationKeys;
            case POSITION -> positionKeys;
            case SCALE    -> scaleKeys;
            default       -> rotationKeys;
        };
    }

    private static float[] evaluateChannel(NavigableMap<Float, Keyframe> keys,
                                           float time, float[] defaultValue) {
        if (keys.isEmpty()) {
            return defaultValue;
        }

        Keyframe exact = keys.get(time);
        if (exact != null) {
            return exact.value();
        }

        var floorEntry = keys.floorEntry(time);
        var ceilEntry  = keys.ceilingEntry(time);

        if (floorEntry == null) {
            return ceilEntry.getValue().value();
        }
        if (ceilEntry == null) {
            return floorEntry.getValue().value();
        }

        return Interpolator.interpolate(floorEntry.getValue(), ceilEntry.getValue(), time);
    }

    @Override
    public String toString() {
        return "BoneTrack{bone='" + boneName + '\'' +
               ", rotKeys=" + rotationKeys.size() +
               ", posKeys=" + positionKeys.size() +
               ", scaleKeys=" + scaleKeys.size() +
               '}';
    }
}

