package net.openstage.core.particle;

public enum ParticleEmitterType {
    BURST_SPHERE("Взрывная сфера"),
    BURST_RING("Кольцо ударной волны"),
    STREAM_CONE("Направленный конус"),
    STREAM_SPIRAL("Спиральный вихрь"),
    AMBIENT_BOX("Объемная зона (Бокс)"),
    POINT("Точечный импульс");
    private final String displayName;

    ParticleEmitterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

