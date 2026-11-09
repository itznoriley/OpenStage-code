package net.openstage.core.animation.curve;

import java.util.Arrays;

public record CurvePreset(String id, String displayName, float[] defaultPoints) {
    public CurvePreset {
        if (defaultPoints == null || defaultPoints.length < 4 || defaultPoints.length % 2 != 0) {
            throw new IllegalArgumentException("Points array must have at least 2 (x,y) pairs");
        }
    }

    public float[] copyDefaultPoints() {
        return Arrays.copyOf(defaultPoints, defaultPoints.length);
    }

    public static float evaluate(float[] points, float t) {
        if (points == null || points.length < 4) {
            return t;
        }

        int count = points.length / 2;
        if (t <= points[0]) {
            return points[1];
        }
        if (t >= points[(count - 1) * 2]) {
            return points[(count - 1) * 2 + 1];
        }

        float[] xs = new float[count];
        float[] ys = new float[count];
        for (int i = 0; i < count; i++) {
            xs[i] = points[i * 2];
            ys[i] = points[i * 2 + 1];
        }

        float[] dxs = new float[count - 1];
        float[] dys = new float[count - 1];
        float[] ms = new float[count - 1];
        for (int i = 0; i < count - 1; i++) {
            dxs[i] = xs[i + 1] - xs[i];
            dys[i] = ys[i + 1] - ys[i];
            ms[i] = (dxs[i] <= 0.00001f) ? 0.0f : dys[i] / dxs[i];
        }

        float[] tangents = new float[count];
        tangents[0] = ms[0];
        tangents[count - 1] = ms[count - 2];
        for (int i = 1; i < count - 1; i++) {
            if (ms[i - 1] * ms[i] <= 0.0f) {
                tangents[i] = 0.0f;
            } else {
                tangents[i] = (ms[i - 1] + ms[i]) * 0.5f;
            }
        }

        for (int i = 0; i < count - 1; i++) {
            if (Math.abs(ms[i]) < 0.00001f) {
                tangents[i] = 0.0f;
                tangents[i + 1] = 0.0f;
            } else {
                float alpha = tangents[i] / ms[i];
                float beta = tangents[i + 1] / ms[i];
                float s = alpha * alpha + beta * beta;
                if (s > 9.0f) {
                    float tau = 3.0f / (float) Math.sqrt(s);
                    tangents[i] = tau * alpha * ms[i];
                    tangents[i + 1] = tau * beta * ms[i];
                }
            }
        }

        int seg = 0;
        for (int i = 0; i < count - 1; i++) {
            if (t >= xs[i] && t <= xs[i + 1]) {
                seg = i;
                break;
            }
        }

        float x1 = xs[seg];
        float x2 = xs[seg + 1];
        float y1 = ys[seg];
        float y2 = ys[seg + 1];
        float h = Math.max(0.00001f, x2 - x1);
        float m1 = tangents[seg];
        float m2 = tangents[seg + 1];
        float u = Math.clamp((t - x1) / h, 0.0f, 1.0f);
        float u2 = u * u;
        float u3 = u2 * u;
        float h00 = 2.0f * u3 - 3.0f * u2 + 1.0f;
        float h10 = u3 - 2.0f * u2 + u;
        float h01 = -2.0f * u3 + 3.0f * u2;
        float h11 = u3 - u2;

        return h00 * y1 + h10 * h * m1 + h01 * y2 + h11 * h * m2;
    }
}

