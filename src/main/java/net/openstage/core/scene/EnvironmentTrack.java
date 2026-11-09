package net.openstage.core.scene;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class EnvironmentTrack {
    private final NavigableMap<Float, EnvironmentKeyframe> keyframes = new TreeMap<>();

    public void addKey(EnvironmentKeyframe kf) {
        if (kf != null) {
            keyframes.put(kf.time(), kf);
        }
    }

    public void removeKey(float time) {
        keyframes.remove(time);
    }

    public void clear() {
        keyframes.clear();
    }

    public boolean isEmpty() {
        return keyframes.isEmpty();
    }

    public NavigableMap<Float, EnvironmentKeyframe> getKeyframes() {
        return Collections.unmodifiableNavigableMap(keyframes);
    }

    public EnvironmentKeyframe getActiveKeyframeAt(float time) {
        if (keyframes.isEmpty()) return null;
        Map.Entry<Float, EnvironmentKeyframe> floor = keyframes.floorEntry(time);
        if (floor != null) {
            return floor.getValue();
        }
        return keyframes.firstEntry().getValue();
    }

    public EnvironmentKeyframe.EnvironmentMode getModeAt(float time, EnvironmentKeyframe.EnvironmentMode fallback) {
        EnvironmentKeyframe kf = getActiveKeyframeAt(time);
        return kf != null ? kf.mode() : fallback;
    }

    public BlockPos getAnchorAt(float time, BlockPos fallback) {
        EnvironmentKeyframe kf = getActiveKeyframeAt(time);
        if (kf != null && kf.mode() == EnvironmentKeyframe.EnvironmentMode.WORLD) {
            return kf.getAnchorPos();
        }
        return fallback;
    }

    public float getFadeAlphaAt(float time) {
        if (keyframes.isEmpty()) return 0.0f;
        Map.Entry<Float, EnvironmentKeyframe> floor = keyframes.floorEntry(time);
        if (floor == null) return 0.0f;
        EnvironmentKeyframe kf = floor.getValue();
        if (kf.transition() != EnvironmentKeyframe.EnvironmentTransition.FADE) return 0.0f;

        float duration = Math.max(0.1f, kf.fadeDuration());
        float elapsed = time - kf.time();
        if (elapsed < 0.0f || elapsed > duration) return 0.0f;

        float half = duration * 0.5f;
        if (elapsed <= half) {
            return 1.0f - (elapsed / half);
        } else {
            return 0.0f;
        }
    }
}

