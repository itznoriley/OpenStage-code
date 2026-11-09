package net.openstage.core.animation;

public enum EasingType {
    LINEAR("linear") {
        @Override
        public float apply(float t) {
            return t;
        }
    },

    STEP("step") {
        @Override
        public float apply(float t) {
            return (t >= 1.0f) ? 1.0f : 0.0f;
        }
    },

    EASE_IN("easeIn") {
        @Override
        public float apply(float t) {
            return t * t;
        }
    },

    EASE_OUT("easeOut") {
        @Override
        public float apply(float t) {
            return t * (2.0f - t);
        }
    },

    EASE_IN_OUT("easeInOut") {
        @Override
        public float apply(float t) {
            return t * t * (3.0f - 2.0f * t);
        }
    },

    BOUNCE("bounce") {
        @Override
        public float apply(float t) {
            if (t < 1.0f / 2.75f) {
                return 7.5625f * t * t;
            } else if (t < 2.0f / 2.75f) {
                float t2 = t - 1.5f / 2.75f;
                return 7.5625f * t2 * t2 + 0.75f;
            } else if (t < 2.5f / 2.75f) {
                float t2 = t - 2.25f / 2.75f;
                return 7.5625f * t2 * t2 + 0.9375f;
            } else {
                float t2 = t - 2.625f / 2.75f;
                return 7.5625f * t2 * t2 + 0.984375f;
            }
        }
    };
    private final String jsonName;

    EasingType(String jsonName) {
        this.jsonName = jsonName;
    }

    public String getJsonName() {
        return jsonName;
    }

    public abstract float apply(float t);

    public static EasingType fromJson(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Easing type name must not be null");
        }
        if (name.equalsIgnoreCase("constant") || name.equalsIgnoreCase("hold")) {
            return STEP;
        }
        for (EasingType type : values()) {
            if (type.jsonName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown easing type: " + name);
    }
}

