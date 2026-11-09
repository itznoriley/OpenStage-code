package net.openstage.core.animation;

import java.util.Arrays;
import java.util.Objects;

public final class Keyframe {
    private final float time;
    private final float[] value;
    private final EasingType easing;
    private final float[] handles;
    private final String expressionSource;

    public Keyframe(float time, float[] value, EasingType easing, float[] handles) {
        this(time, value, easing, handles, null);
    }

    public Keyframe(float time, float[] value, EasingType easing, float[] handles, String expressionSource) {
        Objects.requireNonNull(value, "Keyframe value must not be null");
        if (value.length != 3) {
            throw new IllegalArgumentException(
                    "Keyframe value must have exactly 3 components, got " + value.length);
        }
        if (easing != null && handles != null) {
            throw new IllegalArgumentException(
                    "A keyframe cannot have both an easing function and bezier handles set. " +
                    "Use one or the other (or neither for linear interpolation).");
        }
        if (time < 0) {
            throw new IllegalArgumentException("Keyframe time must be >= 0, got " + time);
        }

        this.time = time;
        this.value = Arrays.copyOf(value, 3);
        this.easing = easing;
        this.handles = handles != null ? Arrays.copyOf(handles, handles.length) : null;
        this.expressionSource = expressionSource;
    }

    public float time() {
        return time;
    }

    public float[] value() {
        return Arrays.copyOf(value, 3);
    }

    public EasingType easing() {
        return easing;
    }

    public float[] handles() {
        return handles != null ? Arrays.copyOf(handles, handles.length) : null;
    }

    public String expressionSource() {
        return expressionSource;
    }

    public boolean isExpression() {
        return expressionSource != null;
    }

    @Override
    public String toString() {
        return "Keyframe{time=" + time +
               ", value=" + Arrays.toString(value) +
               (easing != null ? ", easing=" + easing.getJsonName() : "") +
               (handles != null ? ", handles=" + Arrays.toString(handles) : "") +
               (expressionSource != null ? ", expr=\"" + expressionSource + "\"" : "") +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Keyframe other)) return false;
        return Float.compare(other.time, time) == 0 &&
               Arrays.equals(other.value, value) &&
               easing == other.easing &&
               Arrays.equals(other.handles, handles) &&
               Objects.equals(expressionSource, other.expressionSource);
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(time);
        result = 31 * result + Arrays.hashCode(value);
        result = 31 * result + Objects.hashCode(easing);
        result = 31 * result + Arrays.hashCode(handles);
        result = 31 * result + Objects.hashCode(expressionSource);
        return result;
    }
}

