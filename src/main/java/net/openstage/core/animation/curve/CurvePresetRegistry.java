package net.openstage.core.animation.curve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CurvePresetRegistry {
    private static final List<CurvePreset> PRESETS = new ArrayList<>();

    static {
        register(new CurvePreset("linear", "Линейный", new float[]{
                0.0f, 0.0f, 1.0f, 1.0f
        }));

        register(new CurvePreset("bounce_1", "Отскок 1", new float[]{
                0.0f, 0.0f, 0.7f, 1.0f, 0.85f, 0.88f, 1.0f, 1.0f
        }));

        register(new CurvePreset("bounce_2", "Отскок 2", new float[]{
                0.0f, 0.0f, 0.5f, 1.0f, 0.65f, 0.75f, 0.8f, 1.0f, 0.9f, 0.92f, 1.0f, 1.0f
        }));

        register(new CurvePreset("bounce_3", "Отскок 3", new float[]{
                0.0f, 0.0f, 0.35f, 1.0f, 0.5f, 0.65f, 0.65f, 1.0f, 0.8f, 0.82f, 0.9f, 1.0f, 0.95f, 0.95f, 1.0f, 1.0f
        }));

        register(new CurvePreset("bounce_4", "Отскок 4", new float[]{
                0.0f, 0.0f, 0.15f, 0.2f, 0.25f, 0.05f, 0.35f, 0.35f, 0.45f, 0.1f, 0.75f, 0.95f, 1.0f, 1.0f
        }));

        register(new CurvePreset("bounce_5", "Отскок 5", new float[]{
                0.00f, 0.15f, 0.08f, 0.04f, 0.12f, 0.22f, 0.18f, 0.04f, 0.23f, 0.38f, 0.31f, 0.04f,

                0.55f, 1.00f, 0.62f, 0.72f, 0.67f, 1.00f, 0.72f, 0.82f, 0.76f, 1.00f, 0.81f, 0.88f,

                0.86f, 1.00f, 1.00f, 1.00f
        }));

        register(new CurvePreset("wave_1", "Волнист 1", new float[]{
                0.0f, 0.0f, 0.25f, 0.15f, 0.5f, 0.5f, 0.75f, 0.85f, 1.0f, 1.0f
        }));

        register(new CurvePreset("wave_2", "Волнист 2", new float[]{
                0.0f, 0.0f, 0.2f, 0.35f, 0.4f, 0.35f, 0.6f, 0.75f, 0.8f, 0.75f, 1.0f, 1.0f
        }));

        register(new CurvePreset("wave_3", "Волнист 3", new float[]{
                0.0f, 0.0f, 0.15f, 0.25f, 0.3f, 0.25f, 0.5f, 0.55f, 0.7f, 0.55f, 0.85f, 0.85f, 1.0f, 1.0f
        }));

        register(new CurvePreset("wave_4", "Волнист 4", new float[]{
                0.0f, 0.0f, 0.12f, 0.2f, 0.25f, 0.2f, 0.38f, 0.45f, 0.5f, 0.45f, 0.65f, 0.72f, 0.78f, 0.72f, 0.9f, 0.9f, 1.0f, 1.0f
        }));

        register(new CurvePreset("wave_5", "Волнист 5", new float[]{
                0.0f, 0.0f, 0.1f, 0.18f, 0.2f, 0.18f, 0.3f, 0.38f, 0.4f, 0.38f, 0.5f, 0.58f, 0.6f, 0.58f, 0.7f, 0.78f, 0.8f, 0.78f, 1.0f, 1.0f
        }));

        register(new CurvePreset("out", "Наружу", new float[]{
                0.0f, 0.0f, 0.3f, 0.7f, 0.65f, 0.93f, 1.0f, 1.0f
        }));

        register(new CurvePreset("quad", "Квадрат", new float[]{
                0.0f, 0.0f, 0.5f, 0.85f, 1.0f, 1.0f
        }));

        register(new CurvePreset("cube_out", "Вылет-куб", new float[]{
                0.0f, 0.0f, 0.4f, 0.9f, 1.0f, 1.0f
        }));

        register(new CurvePreset("ease_1", "Замедле 1", new float[]{
                0.0f, 0.0f, 0.35f, 0.15f, 0.65f, 0.85f, 1.0f, 1.0f
        }));

        register(new CurvePreset("ease_2", "Замедле 2", new float[]{
                0.0f, 0.0f, 0.45f, 0.08f, 0.55f, 0.92f, 1.0f, 1.0f
        }));

        register(new CurvePreset("ease_3", "Замедле 3", new float[]{
                0.0f, 0.0f, 0.45f, 0.02f, 0.55f, 0.98f, 1.0f, 1.0f
        }));

        register(new CurvePreset("back_1", "Обратно 1", new float[]{
                0.0f, 0.0f, 0.3f, -0.15f, 0.7f, 1.15f, 1.0f, 1.0f
        }));

        register(new CurvePreset("back_2", "Обратно 2", new float[]{
                0.0f, 0.0f, 0.35f, -0.3f, 0.65f, 1.3f, 1.0f, 1.0f
        }));

        register(new CurvePreset("back_3", "Обратно 3", new float[]{
                0.0f, 0.0f, 0.4f, -0.45f, 0.6f, 1.45f, 1.0f, 1.0f
        }));

        register(new CurvePreset("rise", "Всплытие", new float[]{
                0.0f, 0.0f, 0.15f, 0.65f, 0.5f, 1.05f, 0.85f, 0.98f, 1.0f, 1.0f
        }));
    }

    private static void register(CurvePreset preset) {
        PRESETS.add(preset);
    }

    public static List<CurvePreset> getAll() {
        return Collections.unmodifiableList(PRESETS);
    }

    public static CurvePreset findById(String id) {
        for (CurvePreset p : PRESETS) {
            if (p.id().equals(id)) return p;
        }

        return PRESETS.getFirst();
    }
}

