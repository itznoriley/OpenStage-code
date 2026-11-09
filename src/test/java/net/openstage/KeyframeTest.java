package net.openstage;

import net.openstage.core.animation.EasingType;
import net.openstage.core.animation.Keyframe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Keyframe Invariants and Validation Tests")
class KeyframeTest {
    @Test
    @DisplayName("Value array must have exactly 3 components; other lengths throw IllegalArgumentException")
    void testValueLengthValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new Keyframe(0.0f, new float[]{}, null, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Keyframe(0.0f, new float[]{1.0f}, null, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Keyframe(0.0f, new float[]{1.0f, 2.0f}, null, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Keyframe(0.0f, new float[]{1.0f, 2.0f, 3.0f, 4.0f}, null, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Keyframe(0.0f, new float[10], null, null));

        assertThrows(NullPointerException.class, () ->
                new Keyframe(0.0f, null, null, null));

        assertDoesNotThrow(() ->
                new Keyframe(0.0f, new float[]{1.0f, 2.0f, 3.0f}, null, null));
    }

    @Test
    @DisplayName("Mutual exclusivity: setting both easing and handles throws IllegalArgumentException")
    void testMutualExclusivityOfEasingAndHandles() {
        float[] val = new float[]{0.0f, 1.0f, 0.0f};
        float[] handles = new float[]{0.25f, 0.1f};

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new Keyframe(0.0f, val, EasingType.EASE_IN_OUT, handles));
        assertTrue(ex.getMessage().contains("both"), "Exception message should explain mutual exclusivity");

        assertThrows(IllegalArgumentException.class, () ->
                new Keyframe(0.0f, val, EasingType.BOUNCE, handles, "t * 2"));

        Keyframe easingKf = assertDoesNotThrow(() ->
                new Keyframe(0.0f, val, EasingType.EASE_IN, null));
        assertEquals(EasingType.EASE_IN, easingKf.easing());
        assertNull(easingKf.handles());

        Keyframe handlesKf = assertDoesNotThrow(() ->
                new Keyframe(0.0f, val, null, handles));
        assertNull(handlesKf.easing());
        assertArrayEquals(handles, handlesKf.handles(), 1e-6f);

        Keyframe linearKf = assertDoesNotThrow(() ->
                new Keyframe(0.0f, val, null, null));
        assertNull(linearKf.easing());
        assertNull(linearKf.handles());
    }

    @ParameterizedTest
    @ValueSource(floats = {-0.0001f, -0.5f, -100.0f, Float.NEGATIVE_INFINITY})
    @DisplayName("Negative keyframe time throws IllegalArgumentException")
    void testNegativeTimeThrows(float negativeTime) {
        float[] val = new float[]{0.0f, 0.0f, 0.0f};
        assertThrows(IllegalArgumentException.class, () ->
                new Keyframe(negativeTime, val, null, null));
    }

    @Test
    @DisplayName("Zero and positive keyframe times are valid")
    void testValidKeyframeTimes() {
        float[] val = new float[]{1.0f, 2.0f, 3.0f};
        Keyframe kf0 = assertDoesNotThrow(() -> new Keyframe(0.0f, val, null, null));
        assertEquals(0.0f, kf0.time());
        Keyframe kfPositive = assertDoesNotThrow(() -> new Keyframe(12.34f, val, null, null));
        assertEquals(12.34f, kfPositive.time());
    }

    @Test
    @DisplayName("Defensive copying protects internal value and handles arrays from external mutation")
    void testDefensiveCopying() {
        float[] val = new float[]{1.0f, 2.0f, 3.0f};
        float[] handles = new float[]{0.1f, 0.2f};
        Keyframe kf = new Keyframe(1.0f, val, null, handles);
        val[0] = 999.0f;
        handles[0] = 999.0f;
        assertEquals(1.0f, kf.value()[0], 1e-6f, "Internal value must be isolated from input mutation");
        assertEquals(0.1f, kf.handles()[0], 1e-6f, "Internal handles must be isolated from input mutation");
        float[] returnedVal = kf.value();
        returnedVal[1] = 888.0f;
        assertEquals(2.0f, kf.value()[1], 1e-6f, "Internal value must be isolated from getter mutation");
        float[] returnedHandles = kf.handles();
        returnedHandles[1] = 777.0f;
        assertEquals(0.2f, kf.handles()[1], 1e-6f, "Internal handles must be isolated from getter mutation");
    }

    @Test
    @DisplayName("Expression keyframe properties are preserved")
    void testExpressionKeyframe() {
        float[] val = new float[]{0.0f, 0.0f, 0.0f};
        String expr = "math.sin(t * 90)";
        Keyframe kf = new Keyframe(0.5f, val, null, null, expr);
        assertTrue(kf.isExpression());
        assertEquals(expr, kf.expressionSource());
        Keyframe numericKf = new Keyframe(0.5f, val, null, null);
        assertFalse(numericKf.isExpression());
        assertNull(numericKf.expressionSource());
    }

    @Test
    @DisplayName("equals and hashCode contract")
    void testEqualsAndHashCode() {
        float[] val1 = new float[]{1.0f, 2.0f, 3.0f};
        float[] val2 = new float[]{1.0f, 2.0f, 3.0f};
        float[] handles1 = new float[]{0.3f, 0.4f};
        float[] handles2 = new float[]{0.3f, 0.4f};
        Keyframe kfA = new Keyframe(1.0f, val1, null, handles1, "expr");
        Keyframe kfB = new Keyframe(1.0f, val2, null, handles2, "expr");
        Keyframe kfDiffTime = new Keyframe(2.0f, val1, null, handles1, "expr");
        Keyframe kfDiffVal = new Keyframe(1.0f, new float[]{4, 5, 6}, null, handles1, "expr");
        Keyframe kfDiffEasing = new Keyframe(1.0f, val1, EasingType.LINEAR, null, "expr");

        assertEquals(kfA, kfB);
        assertEquals(kfA.hashCode(), kfB.hashCode());
        assertNotEquals(kfA, kfDiffTime);
        assertNotEquals(kfA, kfDiffVal);
        assertNotEquals(kfA, kfDiffEasing);
        assertNotEquals(kfA, null);
        assertNotEquals(kfA, "string");
    }
}

