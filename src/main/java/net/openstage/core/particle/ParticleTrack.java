package net.openstage.core.particle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ParticleTrack {
    private final List<ParticleKeyframe> keyframes = new ArrayList<>();

    public void addKeyframe(ParticleKeyframe keyframe) {
        if (keyframe == null) return;
        removeKeyframeAt(keyframe.time());
        keyframes.add(keyframe);
        keyframes.sort(Comparator.comparingDouble(ParticleKeyframe::time));
    }

    public boolean removeKeyframe(ParticleKeyframe keyframe) {
        return keyframes.remove(keyframe);
    }

    public void removeKeyframeAt(float time) {
        keyframes.removeIf(k -> Math.abs(k.time() - time) < 0.001f);
    }

    public List<ParticleKeyframe> getKeyframes() {
        return Collections.unmodifiableList(keyframes);
    }

    public void clear() {
        keyframes.clear();
    }

    public boolean isEmpty() {
        return keyframes.isEmpty();
    }

    public ParticleKeyframe getKeyframeAt(float time) {
        for (ParticleKeyframe k : keyframes) {
            if (Math.abs(k.time() - time) < 0.001f) {
                return k;
            }
        }
        return null;
    }

    public ParticleKeyframe getNearestKeyframe(float time, float tolerance) {
        ParticleKeyframe nearest = null;
        float minDist = tolerance;
        for (ParticleKeyframe k : keyframes) {
            float dist = Math.abs(k.time() - time);
            if (dist <= minDist) {
                minDist = dist;
                nearest = k;
            }
        }
        return nearest;
    }

    public List<ParticleKeyframe> getActiveEmittersAt(float time) {
        List<ParticleKeyframe> active = new ArrayList<>();
        for (ParticleKeyframe k : keyframes) {
            if (k.isContinuous()) {
                if (time >= k.time() && time <= k.time() + k.duration()) {
                    active.add(k);
                }
            } else {
                if (Math.abs(time - k.time()) < 0.05f) {
                    active.add(k);
                }
            }
        }
        return active;
    }
}

