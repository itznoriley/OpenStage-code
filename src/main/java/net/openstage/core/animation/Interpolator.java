package net.openstage.core.animation;

public final class Interpolator {
    private Interpolator() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static float[] interpolate(Keyframe from, Keyframe to, float currentTime) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Neither 'from' nor 'to' keyframe may be null");
        }

        float duration = to.time() - from.time();
        if (duration <= 0.0f) {
            return from.value();
        }

        float t = Math.clamp((currentTime - from.time()) / duration, 0.0f, 1.0f);
        float[] a = from.value();
        float[] b = to.value();

        if (from.handles() != null) {
            if (from.handles().length >= 4) {
                float y = net.openstage.core.animation.curve.CurvePreset.evaluate(from.handles(), t);
                return lerpComponents(a, b, y);
            }
            return cubicBezier(a, b, from.handles(), t);
        }

        if (from.easing() != null) {
            float easedT = from.easing().apply(t);
            return lerpComponents(a, b, easedT);
        }

        return lerpComponents(a, b, t);
    }

    private static float[] lerpComponents(float[] a, float[] b, float t) {
        return new float[]{
                a[0] + (b[0] - a[0]) * t,
                a[1] + (b[1] - a[1]) * t,
                a[2] + (b[2] - a[2]) * t
        };
    }

    private static float[] cubicBezier(float[] start, float[] end, float[] handles, float t) {
        float dx = handles.length > 0 ? handles[0] : 0.0f;
        float dy = handles.length > 1 ? handles[1] : 0.0f;
        float oneMinusT = 1.0f - t;
        float oneMinusT2 = oneMinusT * oneMinusT;
        float t2 = t * t;
        float t3 = t2 * t;

        float bezierY = 3.0f * oneMinusT2 * t * dy
                       + 3.0f * oneMinusT * t2 * (1.0f - dy)
                       + t3;

        return lerpComponents(start, end, bezierY);
    }
}

