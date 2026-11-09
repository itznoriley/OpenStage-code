package net.openstage.core.scene;

import net.openstage.core.animation.EasingType;
import net.openstage.core.animation.curve.CurvePreset;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public final class CameraTrack {
    public static final float MIN_FOV = 10.0f;
    public static final float MAX_FOV = 120.0f;

    public record CameraKeyframe(
            float time,
            float x, float y, float z,
            float yaw, float pitch, float roll,
            float fov,
            EasingType easing,
            float[] handles
    ) {
        public CameraKeyframe(float time, float x, float y, float z, float yaw, float pitch, float roll, float fov, EasingType easing) {
            this(time, x, y, z, yaw, pitch, roll, fov, easing, null);
        }

        public CameraKeyframe {
            fov = Math.clamp(fov, MIN_FOV, MAX_FOV);
            handles = (handles != null) ? Arrays.copyOf(handles, handles.length) : null;
        }

        public boolean isSharp() {
            return easing == EasingType.STEP;
        }
    }

    private final NavigableMap<Float, CameraKeyframe> keyframes = new TreeMap<>();

    public void addKey(CameraKeyframe key) {
        Objects.requireNonNull(key, "CameraKeyframe must not be null");
        keyframes.put(key.time(), key);
    }

    public void removeKey(float time) {
        keyframes.remove(time);
    }

    public NavigableMap<Float, CameraKeyframe> getKeyframes() {
        return java.util.Collections.unmodifiableNavigableMap(keyframes);
    }

    public void ensureDefaultKeyframe() {
        if (keyframes.isEmpty()) {
            addKey(new CameraKeyframe(0.0f, 0.0f, 1.4f, -3.2f, 0.0f, 0.0f, 0.0f, 70.0f, null));
        }
    }

    public CameraKeyframe interpolateOrDefault(float time) {
        if (keyframes.isEmpty()) {
            return new CameraKeyframe(time, 0.0f, 1.4f, -3.2f, 0.0f, 0.0f, 0.0f, 70.0f, null);
        }
        return interpolateAt(time);
    }

    public CameraKeyframe interpolateAt(float time) {
        if (keyframes.isEmpty()) {
            throw new IllegalStateException("No camera keyframes defined");
        }

        if (Float.isNaN(time)) {
            return keyframes.firstEntry().getValue();
        }

        CameraKeyframe exact = keyframes.get(time);
        if (exact != null) {
            return exact;
        }

        Map.Entry<Float, CameraKeyframe> floorEntry = keyframes.floorEntry(time);
        Map.Entry<Float, CameraKeyframe> ceilEntry  = keyframes.ceilingEntry(time);

        if (floorEntry == null) {
            return ceilEntry.getValue();
        }
        if (ceilEntry == null) {
            return floorEntry.getValue();
        }

        CameraKeyframe a = floorEntry.getValue();
        CameraKeyframe b = ceilEntry.getValue();
        float duration = b.time() - a.time();
        float t = (duration > 0.0f) ? (time - a.time()) / duration : 0.0f;
        t = Math.clamp(t, 0.0f, 1.0f);
        boolean isSharpTransition = b.isSharp() || (a.isSharp() && b.easing() != EasingType.LINEAR && b.handles() == null);
        float easedT;
        if (isSharpTransition) {
            easedT = (t >= 1.0f) ? 1.0f : 0.0f;
        } else if (a.handles() != null && a.handles().length >= 4) {
            easedT = CurvePreset.evaluate(a.handles(), t);
        } else if (b.handles() != null && b.handles().length >= 4) {
            easedT = CurvePreset.evaluate(b.handles(), t);
        } else {
            EasingType ease = (a.easing() != null && a.easing() != EasingType.STEP) ? a.easing() :
                    ((b.easing() != null && b.easing() != EasingType.STEP) ? b.easing() : EasingType.LINEAR);
            easedT = ease.apply(t);
        }

        EasingType retEasing = (t >= 1.0f) ? b.easing() : a.easing();
        float[] retHandles = (t >= 1.0f) ? b.handles() : a.handles();

        return new CameraKeyframe(
                time,
                lerp(a.x(),     b.x(),     easedT),
                lerp(a.y(),     b.y(),     easedT),
                lerp(a.z(),     b.z(),     easedT),
                lerp(a.yaw(),   b.yaw(),   easedT),
                lerp(a.pitch(), b.pitch(), easedT),
                lerp(a.roll(),  b.roll(),  easedT),
                lerp(a.fov(),   b.fov(),   easedT),
                retEasing,
                retHandles
        );
    }

    public CameraKeyframe interpolateAt(float time, float loopDuration, boolean looping) {
        if (looping && loopDuration > 0.0f) {
            time = time % loopDuration;
            if (time < 0.0f) time += loopDuration;
        }
        return interpolateAt(time);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Override
    public String toString() {
        return "CameraTrack{keyframes=" + keyframes.size() + '}';
    }
}

