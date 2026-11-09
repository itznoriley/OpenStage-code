package net.openstage.core.math;

import org.joml.Vector3f;

public final class InverseKinematicsSolver {
    public record LimbResult(
            float rootPitch, float rootYaw, float rootRoll,
            float childPitch, float childYaw, float childRoll
    ) {}

    private InverseKinematicsSolver() {}

    public static LimbResult solveTwoBone(
            Vector3f root, Vector3f target, float l1, float l2, boolean bendForward
    ) {
        float dx = target.x - root.x;
        float dy = target.y - root.y;
        float dz = target.z - root.z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float maxReach = l1 + l2 - 0.001f;
        float minReach = Math.abs(l1 - l2) + 0.001f;
        float clampedDist = Math.clamp(dist, minReach, maxReach);
        float cosAlpha = (l1 * l1 + clampedDist * clampedDist - l2 * l2) / (2.0f * l1 * clampedDist);
        cosAlpha = Math.clamp(cosAlpha, -1.0f, 1.0f);
        float alpha = (float) Math.acos(cosAlpha);
        float cosBeta = (l1 * l1 + l2 * l2 - clampedDist * clampedDist) / (2.0f * l1 * l2);
        cosBeta = Math.clamp(cosBeta, -1.0f, 1.0f);
        float beta = (float) Math.acos(cosBeta);
        float dirPitch = (float) Math.atan2(Math.sqrt(dx * dx + dz * dz), -dy);
        float dirYaw = (float) Math.atan2(dx, dz);
        float rootPitch = (float) Math.toDegrees(bendForward ? (dirPitch - alpha) : (dirPitch + alpha));
        float childPitch = (float) Math.toDegrees(bendForward ? (Math.PI - beta) : -(Math.PI - beta));
        float rootYaw = (float) Math.toDegrees(dirYaw);

        return new LimbResult(rootPitch, rootYaw, 0.0f, childPitch, 0.0f, 0.0f);
    }

    public static float[] solveSingleBone(Vector3f root, Vector3f target) {
        float dx = target.x - root.x;
        float dy = target.y - root.y;
        float dz = target.z - root.z;
        float pitch = (float) Math.toDegrees(Math.atan2(dz, -dy));
        float yaw = (float) Math.toDegrees(Math.atan2(dx, Math.sqrt(dy * dy + dz * dz)));
        return new float[]{pitch, yaw, 0.0f};
    }
}

