package net.openstage;

import net.openstage.core.animation.curve.CurvePreset;
import net.openstage.core.animation.curve.CurvePresetRegistry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CurvePresetTest {
    @Test
    void testAllPresetsLoaded() {
        assertFalse(CurvePresetRegistry.getAll().isEmpty());
        assertNotNull(CurvePresetRegistry.findById("bounce_5"));
        assertEquals("Отскок 5", CurvePresetRegistry.findById("bounce_5").displayName());
    }

    @Test
    void testCurveEvaluationBounds() {
        CurvePreset p = CurvePresetRegistry.findById("bounce_5");
        float yStart = CurvePreset.evaluate(p.defaultPoints(), 0.0f);
        float yEnd = CurvePreset.evaluate(p.defaultPoints(), 1.0f);
        assertEquals(0.15f, yStart, 0.001f);
        assertEquals(1.0f, yEnd, 0.001f);
        float yMid = CurvePreset.evaluate(p.defaultPoints(), 0.55f);
        assertTrue(yMid >= 0.0f);
    }
}

