package net.openstage.core.particle;

public record ParticleKeyframe(
        float time,
        String particleId,
        ParticleEmitterType emitterType,
        float posX,
        float posY,
        float posZ,
        float rotYaw,
        float rotPitch,
        float rotRoll,
        int count,
        float speed,
        float spread,
        float duration,
        int colorRgb,
        float size,
        int attachedActorIndex,
        String attachedBone,
        float[] curveHandles
) {
    public ParticleKeyframe {
        particleId = particleId != null ? particleId : "minecraft:flame";
        emitterType = emitterType != null ? emitterType : ParticleEmitterType.BURST_SPHERE;
        count = Math.clamp(count, 1, 500);
        speed = Math.clamp(speed, 0.0f, 20.0f);
        spread = Math.clamp(spread, 0.0f, 5.0f);
        duration = Math.max(0.0f, duration);
        size = Math.clamp(size, 0.1f, 10.0f);
        attachedBone = attachedBone != null ? attachedBone : "";
    }

    public ParticleKeyframe(float time, String particleId, ParticleEmitterType emitterType,
                            float posX, float posY, float posZ,
                            float rotYaw, float rotPitch, float rotRoll,
                            int count, float speed, float spread, float duration,
                            int colorRgb, float size, int attachedActorIndex, String attachedBone) {
        this(time, particleId, emitterType, posX, posY, posZ, rotYaw, rotPitch, rotRoll,
             count, speed, spread, duration, colorRgb, size, attachedActorIndex, attachedBone, null);
    }

    public static ParticleKeyframe createDefault(float time, float x, float y, float z) {
        return new ParticleKeyframe(
                time,
                "minecraft:flame",
                ParticleEmitterType.BURST_SPHERE,
                x, y, z,
                0.0f, 0.0f, 0.0f,
                20,
                0.2f,
                0.15f,
                0.0f,
                0xFF5500,
                1.0f,
                -1,
                "",
                null
        );
    }

    public boolean isContinuous() {
        return duration > 0.001f;
    }

    public ParticleKeyframe withTime(float newTime) {
        return new ParticleKeyframe(newTime, particleId, emitterType, posX, posY, posZ, rotYaw, rotPitch, rotRoll, count, speed, spread, duration, colorRgb, size, attachedActorIndex, attachedBone, curveHandles);
    }

    public ParticleKeyframe withPosition(float x, float y, float z) {
        return new ParticleKeyframe(time, particleId, emitterType, x, y, z, rotYaw, rotPitch, rotRoll, count, speed, spread, duration, colorRgb, size, attachedActorIndex, attachedBone, curveHandles);
    }

    public ParticleKeyframe withRotation(float yaw, float pitch, float roll) {
        return new ParticleKeyframe(time, particleId, emitterType, posX, posY, posZ, yaw, pitch, roll, count, speed, spread, duration, colorRgb, size, attachedActorIndex, attachedBone, curveHandles);
    }

    public ParticleKeyframe withParticleId(String id) {
        return new ParticleKeyframe(time, id, emitterType, posX, posY, posZ, rotYaw, rotPitch, rotRoll, count, speed, spread, duration, colorRgb, size, attachedActorIndex, attachedBone, curveHandles);
    }

    public ParticleKeyframe withEmitterType(ParticleEmitterType type) {
        return new ParticleKeyframe(time, particleId, type, posX, posY, posZ, rotYaw, rotPitch, rotRoll, count, speed, spread, duration, colorRgb, size, attachedActorIndex, attachedBone, curveHandles);
    }

    public ParticleKeyframe withParams(int newCount, float newSpeed, float newSpread, float newDuration, int newColorRgb, float newSize) {
        return new ParticleKeyframe(time, particleId, emitterType, posX, posY, posZ, rotYaw, rotPitch, rotRoll, newCount, newSpeed, newSpread, newDuration, newColorRgb, newSize, attachedActorIndex, attachedBone, curveHandles);
    }

    public ParticleKeyframe withAttachment(int actorIndex, String bone) {
        return new ParticleKeyframe(time, particleId, emitterType, posX, posY, posZ, rotYaw, rotPitch, rotRoll, count, speed, spread, duration, colorRgb, size, actorIndex, bone, curveHandles);
    }

    public ParticleKeyframe withCurveHandles(float[] newHandles) {
        return new ParticleKeyframe(time, particleId, emitterType, posX, posY, posZ, rotYaw, rotPitch, rotRoll, count, speed, spread, duration, colorRgb, size, attachedActorIndex, attachedBone, newHandles);
    }
}

