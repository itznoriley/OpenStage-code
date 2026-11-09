package net.openstage.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.openstage.core.scene.CameraTrack;
import org.joml.Matrix4f;
import java.util.Map;
import java.util.NavigableMap;

public final class CameraFrustumRenderer {
    private static final int COLOR_FRUSTUM = 0xFFFFCC00;
    private static final int COLOR_CAMERA_BODY = 0xFF38BDF8;
    private static final int COLOR_UP_ARROW = 0xFFFF4444;
    private static final int COLOR_PATH = 0xFF38BDF8;
    private static final int COLOR_KEY = 0xFFFFCC00;

    public static void renderFrustum(PoseStack poseStack, MultiBufferSource buffers, float fov, float aspect, float nearDist, float farDist) {
        renderFrustum(poseStack, buffers, fov, aspect, nearDist, farDist, COLOR_FRUSTUM);
    }

    public static void renderFrustum(PoseStack poseStack, MultiBufferSource buffers, float fov, float aspect, float nearDist, float farDist, int frustumColor) {
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        VertexConsumer consumer = buffers.getBuffer(RenderType.gui());
        Matrix4f mat = poseStack.last().pose();
        float halfFovRad = (float) Math.toRadians(fov * 0.5f);
        float hNear = (float) Math.tan(halfFovRad) * nearDist;
        float wNear = hNear * aspect;
        float hFar = (float) Math.tan(halfFovRad) * farDist;
        float wFar = hFar * aspect;
        float lensZ = 0.035f;

        drawBox3D(consumer, mat, -0.08f, 0.08f, -0.05f, 0.05f, -0.14f, 0.0f, COLOR_CAMERA_BODY, 0.005f);
        drawBox3D(consumer, mat, -0.035f, 0.035f, 0.05f, 0.085f, -0.11f, -0.03f, COLOR_CAMERA_BODY, 0.004f);
        drawBox3D(consumer, mat, -0.045f, 0.045f, -0.045f, 0.045f, 0.0f, lensZ, COLOR_CAMERA_BODY, 0.004f);

        drawLine3D(consumer, mat, 0, 0, lensZ, -wFar, -hFar, farDist, frustumColor, 0.006f);
        drawLine3D(consumer, mat, 0, 0, lensZ, wFar, -hFar, farDist, frustumColor, 0.006f);
        drawLine3D(consumer, mat, 0, 0, lensZ, wFar, hFar, farDist, frustumColor, 0.006f);
        drawLine3D(consumer, mat, 0, 0, lensZ, -wFar, hFar, farDist, frustumColor, 0.006f);

        drawRectFrame3D(consumer, mat, -wFar, wFar, -hFar, hFar, farDist, frustumColor, 0.006f);
        drawRectFrame3D(consumer, mat, -wNear, wNear, -hNear, hNear, nearDist, frustumColor, 0.005f);
        float upPeakY = -hFar - hFar * 0.22f;
        drawLine3D(consumer, mat, -wFar * 0.25f, -hFar, farDist, 0, upPeakY, farDist, COLOR_UP_ARROW, 0.006f);
        drawLine3D(consumer, mat, 0, upPeakY, farDist, wFar * 0.25f, -hFar, farDist, COLOR_UP_ARROW, 0.006f);
        float crossSize = 0.03f;
        drawLine3D(consumer, mat, -crossSize, 0, farDist, crossSize, 0, farDist, frustumColor, 0.004f);
        drawLine3D(consumer, mat, 0, -crossSize, farDist, 0, crossSize, farDist, frustumColor, 0.004f);
        if (buffers instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.gui());
        }
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }

    public static void renderCameraPath(PoseStack poseStack, MultiBufferSource buffers, CameraTrack track, float totalTime) {
        if (track == null) return;
        NavigableMap<Float, CameraTrack.CameraKeyframe> keys = track.getKeyframes();
        if (keys.size() < 2) return;
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        VertexConsumer consumer = buffers.getBuffer(RenderType.gui());
        Matrix4f mat = poseStack.last().pose();
        int steps = 120;
        float stepTime = totalTime / steps;
        float prevX = 0, prevY = 0, prevZ = 0;
        boolean hasPrev = false;

        for (int i = 0; i <= steps; i++) {
            float t = i * stepTime;
            try {
                CameraTrack.CameraKeyframe kf = track.interpolateAt(t);
                if (hasPrev) {
                    drawLine3D(consumer, mat, prevX, prevY, prevZ, kf.x(), kf.y(), kf.z(), COLOR_PATH, 0.008f);
                }
                prevX = kf.x();
                prevY = kf.y();
                prevZ = kf.z();
                hasPrev = true;
            } catch (Exception ignored) {}
        }

        for (Map.Entry<Float, CameraTrack.CameraKeyframe> entry : keys.entrySet()) {
            CameraTrack.CameraKeyframe kf = entry.getValue();
            drawDiamond3D(consumer, mat, kf.x(), kf.y(), kf.z(), 0.08f, COLOR_KEY);
        }
        if (buffers instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.gui());
        }
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }

    public static void drawLine3D(VertexConsumer consumer, Matrix4f mat, float x0, float y0, float z0, float x1, float y1, float z1, int color, float r) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-5f) return;
        float dirX = dx / len;
        float dirY = dy / len;
        float dirZ = dz / len;
        float ax = 0.0f;
        float ay = 1.0f;
        float az = 0.0f;
        if (Math.abs(dirY) > 0.95f) {
            ax = 1.0f;
            ay = 0.0f;
            az = 0.0f;
        }

        float ux = dirY * az - dirZ * ay;
        float uy = dirZ * ax - dirX * az;
        float uz = dirX * ay - dirY * ax;
        float uLen = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (uLen < 1e-5f) return;
        ux /= uLen;
        uy /= uLen;
        uz /= uLen;
        float vx = dirY * uz - dirZ * uy;
        float vy = dirZ * ux - dirX * uz;
        float vz = dirX * uy - dirY * ux;
        float c0x = (ux + vx) * r;
        float c0y = (uy + vy) * r;
        float c0z = (uz + vz) * r;
        float c1x = (-ux + vx) * r;
        float c1y = (-uy + vy) * r;
        float c1z = (-uz + vz) * r;
        float c2x = (-ux - vx) * r;
        float c2y = (-uy - vy) * r;
        float c2z = (-uz - vz) * r;
        float c3x = (ux - vx) * r;
        float c3y = (uy - vy) * r;
        float c3z = (uz - vz) * r;

        drawQuadDoubleSided(consumer, mat,
                x0 + c0x, y0 + c0y, z0 + c0z,
                x1 + c0x, y1 + c0y, z1 + c0z,
                x1 + c1x, y1 + c1y, z1 + c1z,
                x0 + c1x, y0 + c1y, z0 + c1z,
                color);

        drawQuadDoubleSided(consumer, mat,
                x0 + c1x, y0 + c1y, z0 + c1z,
                x1 + c1x, y1 + c1y, z1 + c1z,
                x1 + c2x, y1 + c2y, z1 + c2z,
                x0 + c2x, y0 + c2y, z0 + c2z,
                color);

        drawQuadDoubleSided(consumer, mat,
                x0 + c2x, y0 + c2y, z0 + c2z,
                x1 + c2x, y1 + c2y, z1 + c2z,
                x1 + c3x, y1 + c3y, z1 + c3z,
                x0 + c3x, y0 + c3y, z0 + c3z,
                color);

        drawQuadDoubleSided(consumer, mat,
                x0 + c3x, y0 + c3y, z0 + c3z,
                x1 + c3x, y1 + c3y, z1 + c3z,
                x1 + c0x, y1 + c0y, z1 + c0z,
                x0 + c0x, y0 + c0y, z0 + c0z,
                color);
    }

    private static void drawQuadDoubleSided(VertexConsumer consumer, Matrix4f mat,
                                           float x0, float y0, float z0,
                                           float x1, float y1, float z1,
                                           float x2, float y2, float z2,
                                           float x3, float y3, float z3,
                                           int color) {
        consumer.addVertex(mat, x0, y0, z0).setColor(color);
        consumer.addVertex(mat, x1, y1, z1).setColor(color);
        consumer.addVertex(mat, x2, y2, z2).setColor(color);
        consumer.addVertex(mat, x3, y3, z3).setColor(color);

        consumer.addVertex(mat, x3, y3, z3).setColor(color);
        consumer.addVertex(mat, x2, y2, z2).setColor(color);
        consumer.addVertex(mat, x1, y1, z1).setColor(color);
        consumer.addVertex(mat, x0, y0, z0).setColor(color);
    }

    public static void drawBox3D(VertexConsumer consumer, Matrix4f mat,
                                 float minX, float maxX, float minY, float maxY, float minZ, float maxZ,
                                 int color, float r) {
        drawLine3D(consumer, mat, minX, minY, minZ, maxX, minY, minZ, color, r);
        drawLine3D(consumer, mat, minX, maxY, minZ, maxX, maxY, minZ, color, r);
        drawLine3D(consumer, mat, minX, minY, maxZ, maxX, minY, maxZ, color, r);
        drawLine3D(consumer, mat, minX, maxY, maxZ, maxX, maxY, maxZ, color, r);

        drawLine3D(consumer, mat, minX, minY, minZ, minX, maxY, minZ, color, r);
        drawLine3D(consumer, mat, maxX, minY, minZ, maxX, maxY, minZ, color, r);
        drawLine3D(consumer, mat, minX, minY, maxZ, minX, maxY, maxZ, color, r);
        drawLine3D(consumer, mat, maxX, minY, maxZ, maxX, maxY, maxZ, color, r);

        drawLine3D(consumer, mat, minX, minY, minZ, minX, minY, maxZ, color, r);
        drawLine3D(consumer, mat, maxX, minY, minZ, maxX, minY, maxZ, color, r);
        drawLine3D(consumer, mat, minX, maxY, minZ, minX, maxY, maxZ, color, r);
        drawLine3D(consumer, mat, maxX, maxY, minZ, maxX, maxY, maxZ, color, r);
    }

    private static void drawRectFrame3D(VertexConsumer consumer, Matrix4f mat, float minX, float maxX, float minY, float maxY, float z, int color, float r) {
        drawLine3D(consumer, mat, minX, minY, z, maxX, minY, z, color, r);
        drawLine3D(consumer, mat, maxX, minY, z, maxX, maxY, z, color, r);
        drawLine3D(consumer, mat, maxX, maxY, z, minX, maxY, z, color, r);
        drawLine3D(consumer, mat, minX, maxY, z, minX, minY, z, color, r);
    }

    private static void drawDiamond3D(VertexConsumer consumer, Matrix4f mat, float x, float y, float z, float s, int color) {
        float r = 0.005f;
        drawLine3D(consumer, mat, x, y - s, z, x + s, y, z, color, r);
        drawLine3D(consumer, mat, x + s, y, z, x, y + s, z, color, r);
        drawLine3D(consumer, mat, x, y + s, z, x - s, y, z, color, r);
        drawLine3D(consumer, mat, x - s, y, z, x, y - s, z, color, r);

        drawLine3D(consumer, mat, x, y, z - s, x + s, y, z, color, r);
        drawLine3D(consumer, mat, x + s, y, z, x, y, z + s, color, r);
        drawLine3D(consumer, mat, x, y, z + s, x - s, y, z, color, r);
        drawLine3D(consumer, mat, x - s, y, z, x, y, z - s, color, r);

        drawLine3D(consumer, mat, x, y - s, z, x, y, z - s, color, r);
        drawLine3D(consumer, mat, x, y - s, z, x, y, z + s, color, r);
        drawLine3D(consumer, mat, x, y + s, z, x, y, z - s, color, r);
        drawLine3D(consumer, mat, x, y + s, z, x, y, z + s, color, r);
    }
}

