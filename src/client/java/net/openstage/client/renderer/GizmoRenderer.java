package net.openstage.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class GizmoRenderer {
    private static final int COLOR_X = 0xFFEF4444;
    private static final int COLOR_Y = 0xFF22C55E;
    private static final int COLOR_Z = 0xFF3B82F6;
    private static final int COLOR_X_HOV = 0xFFFF7777;
    private static final int COLOR_Y_HOV = 0xFF66FF88;
    private static final int COLOR_Z_HOV = 0xFF66B2FF;
    private static final int PLANE_X_FILL = 0x55EF4444;
    private static final int PLANE_Y_FILL = 0x5522C55E;
    private static final int PLANE_Z_FILL = 0x553B82F6;

    public static void renderGizmo(PoseStack poseStack, MultiBufferSource buffers, float radius, int hoveredAxis) {
        renderGizmo(poseStack, buffers, radius, hoveredAxis, net.openstage.client.model.ModelSettings.GizmoMode.ROTATE);
    }

    public static void renderGizmo(PoseStack poseStack, MultiBufferSource buffers, float radius, int hoveredAxis, net.openstage.client.model.ModelSettings.GizmoMode mode) {
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        VertexConsumer consumer = buffers.getBuffer(RenderType.gui());
        Matrix4f mat = poseStack.last().pose();
        int segments = 48;
        float ringThickness = 0.018f;
        int colX = (hoveredAxis == 0) ? COLOR_X_HOV : COLOR_X;
        int colY = (hoveredAxis == 1) ? COLOR_Y_HOV : COLOR_Y;
        int colZ = (hoveredAxis == 2) ? COLOR_Z_HOV : COLOR_Z;

        if (mode == net.openstage.client.model.ModelSettings.GizmoMode.ROTATE) {
            if (hoveredAxis == 0) drawFilledCircleYZ(consumer, mat, radius, segments, PLANE_X_FILL);
            if (hoveredAxis == 1) drawFilledCircleXZ(consumer, mat, radius, segments, PLANE_Y_FILL);
            if (hoveredAxis == 2) drawFilledCircleXY(consumer, mat, radius, segments, PLANE_Z_FILL);

            drawVolumetricRingYZ(consumer, mat, radius, ringThickness, segments, colX);
            drawVolumetricRingXZ(consumer, mat, radius, ringThickness, segments, colY);
            drawVolumetricRingXY(consumer, mat, radius, ringThickness, segments, colZ);
        } else {
            float arrowLen = Math.max(0.85f, radius * 2.4f);
            float shaftW = 0.024f;
            float headL = 0.22f;
            float headW = 0.075f;

            drawArrowX(consumer, mat, arrowLen, shaftW, headL, headW, colX);
            drawArrowY(consumer, mat, arrowLen, shaftW, headL, headW, colY);
            drawArrowZ(consumer, mat, arrowLen, shaftW, headL, headW, colZ);
            float pOff = radius * 0.35f;
            float pSize = radius * 0.30f;
            drawPlaneYZ(consumer, mat, pOff, pOff + pSize, pOff, pOff + pSize, (hoveredAxis == 0) ? 0x88EF4444 : 0x55EF4444, colX);
            drawPlaneXZ(consumer, mat, pOff, pOff + pSize, pOff, pOff + pSize, (hoveredAxis == 1) ? 0x8822C55E : 0x5522C55E, colY);
            drawPlaneXY(consumer, mat, pOff, pOff + pSize, pOff, pOff + pSize, (hoveredAxis == 2) ? 0x883B82F6 : 0x553B82F6, colZ);
            int centerCol = (hoveredAxis == 3) ? 0xFFFCD34D : 0xCCE2E8F0;
            drawCenterCube(consumer, mat, 0.045f, centerCol);
        }

        com.mojang.blaze3d.systems.RenderSystem.enableCull();
        if (buffers instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.gui());
        }
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }

    private static void drawArrowX(VertexConsumer consumer, Matrix4f mat, float len, float sw, float hl, float hw, int color) {
        float bLen = len - hl;

        consumer.addVertex(mat, 0, -sw, -sw).setColor(color);
        consumer.addVertex(mat, bLen, -sw, -sw).setColor(color);
        consumer.addVertex(mat, bLen, sw, -sw).setColor(color);
        consumer.addVertex(mat, 0, sw, -sw).setColor(color);

        consumer.addVertex(mat, 0, sw, sw).setColor(color);
        consumer.addVertex(mat, bLen, sw, sw).setColor(color);
        consumer.addVertex(mat, bLen, -sw, sw).setColor(color);
        consumer.addVertex(mat, 0, -sw, sw).setColor(color);

        consumer.addVertex(mat, 0, sw, -sw).setColor(color);
        consumer.addVertex(mat, bLen, sw, -sw).setColor(color);
        consumer.addVertex(mat, bLen, sw, sw).setColor(color);
        consumer.addVertex(mat, 0, sw, sw).setColor(color);

        consumer.addVertex(mat, 0, -sw, sw).setColor(color);
        consumer.addVertex(mat, bLen, -sw, sw).setColor(color);
        consumer.addVertex(mat, bLen, -sw, -sw).setColor(color);
        consumer.addVertex(mat, 0, -sw, -sw).setColor(color);

        consumer.addVertex(mat, bLen, -hw, -hw).setColor(color);
        consumer.addVertex(mat, bLen, hw, -hw).setColor(color);
        consumer.addVertex(mat, bLen, hw, hw).setColor(color);
        consumer.addVertex(mat, bLen, -hw, hw).setColor(color);

        consumer.addVertex(mat, bLen, -hw, -hw).setColor(color);
        consumer.addVertex(mat, bLen, hw, -hw).setColor(color);
        consumer.addVertex(mat, len, 0, 0).setColor(color);
        consumer.addVertex(mat, len, 0, 0).setColor(color);

        consumer.addVertex(mat, bLen, hw, -hw).setColor(color);
        consumer.addVertex(mat, bLen, hw, hw).setColor(color);
        consumer.addVertex(mat, len, 0, 0).setColor(color);
        consumer.addVertex(mat, len, 0, 0).setColor(color);

        consumer.addVertex(mat, bLen, hw, hw).setColor(color);
        consumer.addVertex(mat, bLen, -hw, hw).setColor(color);
        consumer.addVertex(mat, len, 0, 0).setColor(color);
        consumer.addVertex(mat, len, 0, 0).setColor(color);

        consumer.addVertex(mat, bLen, -hw, hw).setColor(color);
        consumer.addVertex(mat, bLen, -hw, -hw).setColor(color);
        consumer.addVertex(mat, len, 0, 0).setColor(color);
        consumer.addVertex(mat, len, 0, 0).setColor(color);
    }

    private static void drawArrowY(VertexConsumer consumer, Matrix4f mat, float len, float sw, float hl, float hw, int color) {
        float bLen = len - hl;

        consumer.addVertex(mat, -sw, 0, -sw).setColor(color);
        consumer.addVertex(mat, -sw, bLen, -sw).setColor(color);
        consumer.addVertex(mat, sw, bLen, -sw).setColor(color);
        consumer.addVertex(mat, sw, 0, -sw).setColor(color);

        consumer.addVertex(mat, sw, 0, sw).setColor(color);
        consumer.addVertex(mat, sw, bLen, sw).setColor(color);
        consumer.addVertex(mat, -sw, bLen, sw).setColor(color);
        consumer.addVertex(mat, -sw, 0, sw).setColor(color);

        consumer.addVertex(mat, sw, 0, -sw).setColor(color);
        consumer.addVertex(mat, sw, bLen, -sw).setColor(color);
        consumer.addVertex(mat, sw, bLen, sw).setColor(color);
        consumer.addVertex(mat, sw, 0, sw).setColor(color);

        consumer.addVertex(mat, -sw, 0, sw).setColor(color);
        consumer.addVertex(mat, -sw, bLen, sw).setColor(color);
        consumer.addVertex(mat, -sw, bLen, -sw).setColor(color);
        consumer.addVertex(mat, -sw, 0, -sw).setColor(color);

        consumer.addVertex(mat, -hw, bLen, -hw).setColor(color);
        consumer.addVertex(mat, hw, bLen, -hw).setColor(color);
        consumer.addVertex(mat, hw, bLen, hw).setColor(color);
        consumer.addVertex(mat, -hw, bLen, hw).setColor(color);

        consumer.addVertex(mat, -hw, bLen, -hw).setColor(color);
        consumer.addVertex(mat, hw, bLen, -hw).setColor(color);
        consumer.addVertex(mat, 0, len, 0).setColor(color);
        consumer.addVertex(mat, 0, len, 0).setColor(color);

        consumer.addVertex(mat, hw, bLen, -hw).setColor(color);
        consumer.addVertex(mat, hw, bLen, hw).setColor(color);
        consumer.addVertex(mat, 0, len, 0).setColor(color);
        consumer.addVertex(mat, 0, len, 0).setColor(color);

        consumer.addVertex(mat, hw, bLen, hw).setColor(color);
        consumer.addVertex(mat, -hw, bLen, hw).setColor(color);
        consumer.addVertex(mat, 0, len, 0).setColor(color);
        consumer.addVertex(mat, 0, len, 0).setColor(color);

        consumer.addVertex(mat, -hw, bLen, hw).setColor(color);
        consumer.addVertex(mat, -hw, bLen, -hw).setColor(color);
        consumer.addVertex(mat, 0, len, 0).setColor(color);
        consumer.addVertex(mat, 0, len, 0).setColor(color);
    }

    private static void drawArrowZ(VertexConsumer consumer, Matrix4f mat, float len, float sw, float hl, float hw, int color) {
        float bLen = len - hl;

        consumer.addVertex(mat, -sw, -sw, 0).setColor(color);
        consumer.addVertex(mat, -sw, -sw, bLen).setColor(color);
        consumer.addVertex(mat, sw, -sw, bLen).setColor(color);
        consumer.addVertex(mat, sw, -sw, 0).setColor(color);

        consumer.addVertex(mat, sw, sw, 0).setColor(color);
        consumer.addVertex(mat, sw, sw, bLen).setColor(color);
        consumer.addVertex(mat, -sw, sw, bLen).setColor(color);
        consumer.addVertex(mat, -sw, sw, 0).setColor(color);

        consumer.addVertex(mat, -sw, sw, 0).setColor(color);
        consumer.addVertex(mat, -sw, sw, bLen).setColor(color);
        consumer.addVertex(mat, -sw, -sw, bLen).setColor(color);
        consumer.addVertex(mat, -sw, -sw, 0).setColor(color);

        consumer.addVertex(mat, sw, -sw, 0).setColor(color);
        consumer.addVertex(mat, sw, -sw, bLen).setColor(color);
        consumer.addVertex(mat, sw, sw, bLen).setColor(color);
        consumer.addVertex(mat, sw, sw, 0).setColor(color);

        consumer.addVertex(mat, -hw, -hw, bLen).setColor(color);
        consumer.addVertex(mat, hw, -hw, bLen).setColor(color);
        consumer.addVertex(mat, hw, hw, bLen).setColor(color);
        consumer.addVertex(mat, -hw, hw, bLen).setColor(color);

        consumer.addVertex(mat, -hw, -hw, bLen).setColor(color);
        consumer.addVertex(mat, hw, -hw, bLen).setColor(color);
        consumer.addVertex(mat, 0, 0, len).setColor(color);
        consumer.addVertex(mat, 0, 0, len).setColor(color);

        consumer.addVertex(mat, hw, -hw, bLen).setColor(color);
        consumer.addVertex(mat, hw, hw, bLen).setColor(color);
        consumer.addVertex(mat, 0, 0, len).setColor(color);
        consumer.addVertex(mat, 0, 0, len).setColor(color);

        consumer.addVertex(mat, hw, hw, bLen).setColor(color);
        consumer.addVertex(mat, -hw, hw, bLen).setColor(color);
        consumer.addVertex(mat, 0, 0, len).setColor(color);
        consumer.addVertex(mat, 0, 0, len).setColor(color);

        consumer.addVertex(mat, -hw, hw, bLen).setColor(color);
        consumer.addVertex(mat, -hw, -hw, bLen).setColor(color);
        consumer.addVertex(mat, 0, 0, len).setColor(color);
        consumer.addVertex(mat, 0, 0, len).setColor(color);
    }

    private static void drawPlaneYZ(VertexConsumer consumer, Matrix4f mat, float y0, float y1, float z0, float z1, int fill, int border) {
        consumer.addVertex(mat, 0, y0, z0).setColor(fill);
        consumer.addVertex(mat, 0, y1, z0).setColor(fill);
        consumer.addVertex(mat, 0, y1, z1).setColor(fill);
        consumer.addVertex(mat, 0, y0, z1).setColor(fill);

        consumer.addVertex(mat, 0, y0, z1).setColor(fill);
        consumer.addVertex(mat, 0, y1, z1).setColor(fill);
        consumer.addVertex(mat, 0, y1, z0).setColor(fill);
        consumer.addVertex(mat, 0, y0, z0).setColor(fill);
        float t = 0.012f;
        consumer.addVertex(mat, 0, y0, z0).setColor(border);
        consumer.addVertex(mat, 0, y1, z0).setColor(border);
        consumer.addVertex(mat, 0, y1, z0 + t).setColor(border);
        consumer.addVertex(mat, 0, y0, z0 + t).setColor(border);

        consumer.addVertex(mat, 0, y0, z0).setColor(border);
        consumer.addVertex(mat, 0, y0 + t, z0).setColor(border);
        consumer.addVertex(mat, 0, y0 + t, z1).setColor(border);
        consumer.addVertex(mat, 0, y0, z1).setColor(border);

        consumer.addVertex(mat, 0, y1 - t, z0).setColor(border);
        consumer.addVertex(mat, 0, y1, z0).setColor(border);
        consumer.addVertex(mat, 0, y1, z1).setColor(border);
        consumer.addVertex(mat, 0, y1 - t, z1).setColor(border);

        consumer.addVertex(mat, 0, y0, z1 - t).setColor(border);
        consumer.addVertex(mat, 0, y1, z1 - t).setColor(border);
        consumer.addVertex(mat, 0, y1, z1).setColor(border);
        consumer.addVertex(mat, 0, y0, z1).setColor(border);
    }

    private static void drawPlaneXZ(VertexConsumer consumer, Matrix4f mat, float x0, float x1, float z0, float z1, int fill, int border) {
        consumer.addVertex(mat, x0, 0, z0).setColor(fill);
        consumer.addVertex(mat, x1, 0, z0).setColor(fill);
        consumer.addVertex(mat, x1, 0, z1).setColor(fill);
        consumer.addVertex(mat, x0, 0, z1).setColor(fill);

        consumer.addVertex(mat, x0, 0, z1).setColor(fill);
        consumer.addVertex(mat, x1, 0, z1).setColor(fill);
        consumer.addVertex(mat, x1, 0, z0).setColor(fill);
        consumer.addVertex(mat, x0, 0, z0).setColor(fill);
        float t = 0.012f;
        consumer.addVertex(mat, x0, 0, z0).setColor(border);
        consumer.addVertex(mat, x1, 0, z0).setColor(border);
        consumer.addVertex(mat, x1, 0, z0 + t).setColor(border);
        consumer.addVertex(mat, x0, 0, z0 + t).setColor(border);

        consumer.addVertex(mat, x0, 0, z0).setColor(border);
        consumer.addVertex(mat, x0 + t, 0, z0).setColor(border);
        consumer.addVertex(mat, x0 + t, 0, z1).setColor(border);
        consumer.addVertex(mat, x0, 0, z1).setColor(border);

        consumer.addVertex(mat, x0, 0, z1 - t).setColor(border);
        consumer.addVertex(mat, x1, 0, z1 - t).setColor(border);
        consumer.addVertex(mat, x1, 0, z1).setColor(border);
        consumer.addVertex(mat, x0, 0, z1).setColor(border);

        consumer.addVertex(mat, x1 - t, 0, z0).setColor(border);
        consumer.addVertex(mat, x1, 0, z0).setColor(border);
        consumer.addVertex(mat, x1, 0, z1).setColor(border);
        consumer.addVertex(mat, x1 - t, 0, z1).setColor(border);
    }

    private static void drawPlaneXY(VertexConsumer consumer, Matrix4f mat, float x0, float x1, float y0, float y1, int fill, int border) {
        consumer.addVertex(mat, x0, y0, 0).setColor(fill);
        consumer.addVertex(mat, x1, y0, 0).setColor(fill);
        consumer.addVertex(mat, x1, y1, 0).setColor(fill);
        consumer.addVertex(mat, x0, y1, 0).setColor(fill);

        consumer.addVertex(mat, x0, y1, 0).setColor(fill);
        consumer.addVertex(mat, x1, y1, 0).setColor(fill);
        consumer.addVertex(mat, x1, y0, 0).setColor(fill);
        consumer.addVertex(mat, x0, y0, 0).setColor(fill);
        float t = 0.012f;
        consumer.addVertex(mat, x0, y0, 0).setColor(border);
        consumer.addVertex(mat, x1, y0, 0).setColor(border);
        consumer.addVertex(mat, x1, y0 + t, 0).setColor(border);
        consumer.addVertex(mat, x0, y0 + t, 0).setColor(border);

        consumer.addVertex(mat, x0, y0, 0).setColor(border);
        consumer.addVertex(mat, x0 + t, y0, 0).setColor(border);
        consumer.addVertex(mat, x0 + t, y1, 0).setColor(border);
        consumer.addVertex(mat, x0, y1, 0).setColor(border);

        consumer.addVertex(mat, x0, y1 - t, 0).setColor(border);
        consumer.addVertex(mat, x1, y1 - t, 0).setColor(border);
        consumer.addVertex(mat, x1, y1, 0).setColor(border);
        consumer.addVertex(mat, x0, y1, 0).setColor(border);

        consumer.addVertex(mat, x1 - t, y0, 0).setColor(border);
        consumer.addVertex(mat, x1, y0, 0).setColor(border);
        consumer.addVertex(mat, x1, y1, 0).setColor(border);
        consumer.addVertex(mat, x1 - t, y1, 0).setColor(border);
    }

    private static void drawFilledCircleYZ(VertexConsumer consumer, Matrix4f mat, float r, int seg, int color) {
        float step = (float) (Math.PI * 2.0 / seg);
        for (int i = 0; i < seg; i++) {
            float a0 = i * step;
            float a1 = (i + 1) * step;
            consumer.addVertex(mat, 0, 0, 0).setColor(color);
            consumer.addVertex(mat, 0, (float) Math.cos(a0) * r, (float) Math.sin(a0) * r).setColor(color);
            consumer.addVertex(mat, 0, (float) Math.cos(a1) * r, (float) Math.sin(a1) * r).setColor(color);
            consumer.addVertex(mat, 0, 0, 0).setColor(color);
        }
    }

    private static void drawFilledCircleXZ(VertexConsumer consumer, Matrix4f mat, float r, int seg, int color) {
        float step = (float) (Math.PI * 2.0 / seg);
        for (int i = 0; i < seg; i++) {
            float a0 = i * step;
            float a1 = (i + 1) * step;
            consumer.addVertex(mat, 0, 0, 0).setColor(color);
            consumer.addVertex(mat, (float) Math.cos(a0) * r, 0, (float) Math.sin(a0) * r).setColor(color);
            consumer.addVertex(mat, (float) Math.cos(a1) * r, 0, (float) Math.sin(a1) * r).setColor(color);
            consumer.addVertex(mat, 0, 0, 0).setColor(color);
        }
    }

    private static void drawFilledCircleXY(VertexConsumer consumer, Matrix4f mat, float r, int seg, int color) {
        float step = (float) (Math.PI * 2.0 / seg);
        for (int i = 0; i < seg; i++) {
            float a0 = i * step;
            float a1 = (i + 1) * step;
            consumer.addVertex(mat, 0, 0, 0).setColor(color);
            consumer.addVertex(mat, (float) Math.cos(a0) * r, (float) Math.sin(a0) * r, 0).setColor(color);
            consumer.addVertex(mat, (float) Math.cos(a1) * r, (float) Math.sin(a1) * r, 0).setColor(color);
            consumer.addVertex(mat, 0, 0, 0).setColor(color);
        }
    }

    private static int shadeColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void drawVolumetricRingXZ(VertexConsumer consumer, Matrix4f mat, float r, float t, int seg, int color) {
        float step = (float) (Math.PI * 2.0 / seg);
        float rOut = r + t;
        float rIn = r - t;
        int colTop = color;
        int colOuter = shadeColor(color, 0.85f);
        int colInner = shadeColor(color, 0.70f);
        int colBot = shadeColor(color, 0.55f);

        for (int i = 0; i < seg; i++) {
            float a0 = i * step;
            float a1 = (i + 1) * step;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);

            consumer.addVertex(mat, rIn * c0, t, rIn * s0).setColor(colTop);
            consumer.addVertex(mat, rOut * c0, t, rOut * s0).setColor(colTop);
            consumer.addVertex(mat, rOut * c1, t, rOut * s1).setColor(colTop);
            consumer.addVertex(mat, rIn * c1, t, rIn * s1).setColor(colTop);

            consumer.addVertex(mat, rIn * c0, -t, rIn * s0).setColor(colBot);
            consumer.addVertex(mat, rIn * c1, -t, rIn * s1).setColor(colBot);
            consumer.addVertex(mat, rOut * c1, -t, rOut * s1).setColor(colBot);
            consumer.addVertex(mat, rOut * c0, -t, rOut * s0).setColor(colBot);

            consumer.addVertex(mat, rOut * c0, -t, rOut * s0).setColor(colOuter);
            consumer.addVertex(mat, rOut * c1, -t, rOut * s1).setColor(colOuter);
            consumer.addVertex(mat, rOut * c1, t, rOut * s1).setColor(colOuter);
            consumer.addVertex(mat, rOut * c0, t, rOut * s0).setColor(colOuter);

            consumer.addVertex(mat, rIn * c0, -t, rIn * s0).setColor(colInner);
            consumer.addVertex(mat, rIn * c0, t, rIn * s0).setColor(colInner);
            consumer.addVertex(mat, rIn * c1, t, rIn * s1).setColor(colInner);
            consumer.addVertex(mat, rIn * c1, -t, rIn * s1).setColor(colInner);
        }
    }

    private static void drawVolumetricRingYZ(VertexConsumer consumer, Matrix4f mat, float r, float t, int seg, int color) {
        float step = (float) (Math.PI * 2.0 / seg);
        float rOut = r + t;
        float rIn = r - t;
        int colRight = color;
        int colOuter = shadeColor(color, 0.85f);
        int colInner = shadeColor(color, 0.70f);
        int colLeft = shadeColor(color, 0.55f);

        for (int i = 0; i < seg; i++) {
            float a0 = i * step;
            float a1 = (i + 1) * step;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);

            consumer.addVertex(mat, t, rIn * c0, rIn * s0).setColor(colRight);
            consumer.addVertex(mat, t, rOut * c0, rOut * s0).setColor(colRight);
            consumer.addVertex(mat, t, rOut * c1, rOut * s1).setColor(colRight);
            consumer.addVertex(mat, t, rIn * c1, rIn * s1).setColor(colRight);

            consumer.addVertex(mat, -t, rIn * c0, rIn * s0).setColor(colLeft);
            consumer.addVertex(mat, -t, rIn * c1, rIn * s1).setColor(colLeft);
            consumer.addVertex(mat, -t, rOut * c1, rOut * s1).setColor(colLeft);
            consumer.addVertex(mat, -t, rOut * c0, rOut * s0).setColor(colLeft);

            consumer.addVertex(mat, -t, rOut * c0, rOut * s0).setColor(colOuter);
            consumer.addVertex(mat, t, rOut * c0, rOut * s0).setColor(colOuter);
            consumer.addVertex(mat, t, rOut * c1, rOut * s1).setColor(colOuter);
            consumer.addVertex(mat, -t, rOut * c1, rOut * s1).setColor(colOuter);

            consumer.addVertex(mat, -t, rIn * c0, rIn * s0).setColor(colInner);
            consumer.addVertex(mat, -t, rIn * c1, rIn * s1).setColor(colInner);
            consumer.addVertex(mat, t, rIn * c1, rIn * s1).setColor(colInner);
            consumer.addVertex(mat, t, rIn * c0, rIn * s0).setColor(colInner);
        }
    }

    private static void drawVolumetricRingXY(VertexConsumer consumer, Matrix4f mat, float r, float t, int seg, int color) {
        float step = (float) (Math.PI * 2.0 / seg);
        float rOut = r + t;
        float rIn = r - t;
        int colFront = color;
        int colOuter = shadeColor(color, 0.85f);
        int colInner = shadeColor(color, 0.70f);
        int colBack = shadeColor(color, 0.55f);

        for (int i = 0; i < seg; i++) {
            float a0 = i * step;
            float a1 = (i + 1) * step;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);

            consumer.addVertex(mat, rIn * c0, rIn * s0, t).setColor(colFront);
            consumer.addVertex(mat, rOut * c0, rOut * s0, t).setColor(colFront);
            consumer.addVertex(mat, rOut * c1, rOut * s1, t).setColor(colFront);
            consumer.addVertex(mat, rIn * c1, rIn * s1, t).setColor(colFront);

            consumer.addVertex(mat, rIn * c0, rIn * s0, -t).setColor(colBack);
            consumer.addVertex(mat, rIn * c1, rIn * s1, -t).setColor(colBack);
            consumer.addVertex(mat, rOut * c1, rOut * s1, -t).setColor(colBack);
            consumer.addVertex(mat, rOut * c0, rOut * s0, -t).setColor(colBack);

            consumer.addVertex(mat, rOut * c0, rOut * s0, -t).setColor(colOuter);
            consumer.addVertex(mat, rOut * c0, rOut * s0, t).setColor(colOuter);
            consumer.addVertex(mat, rOut * c1, rOut * s1, t).setColor(colOuter);
            consumer.addVertex(mat, rOut * c1, rOut * s1, -t).setColor(colOuter);

            consumer.addVertex(mat, rIn * c0, rIn * s0, -t).setColor(colInner);
            consumer.addVertex(mat, rIn * c1, rIn * s1, -t).setColor(colInner);
            consumer.addVertex(mat, rIn * c1, rIn * s1, t).setColor(colInner);
            consumer.addVertex(mat, rIn * c0, rIn * s0, t).setColor(colInner);
        }
    }

    public static int testHitAxis(double mouseX, double mouseY, int vpX, int vpY, int vpW, int vpH,
                                  float cameraPitch, float cameraYaw, float cameraZoom,
                                  float cameraPanX, float cameraPanY, Vector3f jointOffset, float radius) {
        return testHitAxis(mouseX, mouseY, vpX, vpY, vpW, vpH, cameraPitch, cameraYaw, cameraZoom, cameraPanX, cameraPanY, jointOffset, radius, 0.0f, 1.0f);
    }

    public static int testHitAxis(double mouseX, double mouseY, int vpX, int vpY, int vpW, int vpH,
                                  float cameraPitch, float cameraYaw, float cameraZoom,
                                  float cameraPanX, float cameraPanY, Vector3f jointOffset, float radius,
                                  float rootYaw, float modelScale) {
        return testHitAxis(mouseX, mouseY, vpX, vpY, vpW, vpH, cameraPitch, cameraYaw, cameraZoom, cameraPanX, cameraPanY, jointOffset, radius, rootYaw, modelScale, net.openstage.client.model.ModelSettings.GizmoMode.ROTATE);
    }

    public static int testHitAxis(double mouseX, double mouseY, int vpX, int vpY, int vpW, int vpH,
                                  float cameraPitch, float cameraYaw, float cameraZoom,
                                  float cameraPanX, float cameraPanY, Vector3f jointOffset, float radius,
                                  float rootYaw, float modelScale, net.openstage.client.model.ModelSettings.GizmoMode mode) {
        float s = 120.0f * (2.0f / Math.max(0.4f, cameraZoom));
        float cx = vpX + vpW * 0.5f + cameraPanX * s;
        float cy = vpY + vpH * 0.5f + 55.0f + cameraPanY * s;
        float radRoot = (float) Math.toRadians(rootYaw);
        float cosR = (float) Math.cos(radRoot);
        float sinR = (float) Math.sin(radRoot);
        float radYaw = (float) Math.toRadians(cameraYaw);
        float radPitch = (float) Math.toRadians(cameraPitch);
        float cosY = (float) Math.cos(radYaw);
        float sinY = (float) Math.sin(radYaw);
        float cosP = (float) Math.cos(radPitch);
        float sinP = (float) Math.sin(radPitch);
        float c_rx = (jointOffset.x * cosR + jointOffset.z * sinR) * modelScale;
        float c_ry = jointOffset.y * modelScale;
        float c_rz = (-jointOffset.x * sinR + jointOffset.z * cosR) * modelScale;
        float c_x1 = c_rx * cosY + c_rz * sinY;
        float c_y1 = c_ry;
        float c_z1 = -c_rx * sinY + c_rz * cosY;
        float c_x2 = c_x1;
        float c_y2 = c_y1 * cosP - c_z1 * sinP;
        float c_sx = cx + c_x2 * s;
        float c_sy = cy - c_y2 * s;

        if (mode == net.openstage.client.model.ModelSettings.GizmoMode.ROTATE) {
            int numSamples = 96;
            float hitThreshold = 9.0f;
            int bestAxis = -1;
            float bestDist = Float.MAX_VALUE;
            float bestZ = -Float.MAX_VALUE;

            for (int axis = 0; axis < 3; axis++) {
                for (int i = 0; i < numSamples; i++) {
                    float theta = (float) (i * 2.0 * Math.PI / numSamples);
                    float c = (float) Math.cos(theta) * radius;
                    float sn = (float) Math.sin(theta) * radius;
                    float lx = (axis == 0) ? 0.0f : ((axis == 1) ? c : c);
                    float ly = (axis == 0) ? c : ((axis == 1) ? 0.0f : sn);
                    float lz = (axis == 0) ? sn : ((axis == 1) ? sn : 0.0f);
                    float wx = jointOffset.x + lx;
                    float wy = jointOffset.y + ly;
                    float wz = jointOffset.z + lz;
                    float rx = (wx * cosR + wz * sinR) * modelScale;
                    float ry = wy * modelScale;
                    float rz = (-wx * sinR + wz * cosR) * modelScale;
                    float x1 = rx * cosY + rz * sinY;
                    float y1 = ry;
                    float z1 = -rx * sinY + rz * cosY;
                    float x2 = x1;
                    float y2 = y1 * cosP - z1 * sinP;
                    float z2 = y1 * sinP + z1 * cosP;
                    float sx = cx + x2 * s;
                    float sy = cy - y2 * s;
                    float dist = (float) Math.hypot(mouseX - sx, mouseY - sy);
                    if (dist <= hitThreshold) {
                        if (z2 > bestZ || (Math.abs(z2 - bestZ) < 0.05f && dist < bestDist)) {
                            bestZ = z2;
                            bestDist = dist;
                            bestAxis = axis;
                        }
                    }
                }
            }
            return bestAxis;
        } else {
            if (Math.hypot(mouseX - c_sx, mouseY - c_sy) <= 10.0) {
                return 3;
            }
            float arrowLen = Math.max(0.85f, radius * 2.4f);
            float hitThreshold = 8.5f;
            int numSamples = 16;
            int bestAxis = -1;
            float bestDist = Float.MAX_VALUE;
            float bestZ = -Float.MAX_VALUE;

            for (int axis = 0; axis < 3; axis++) {
                for (int i = 0; i <= numSamples; i++) {
                    float frac = (float) i / (float) numSamples;
                    float len = frac * arrowLen;
                    float lx = (axis == 0) ? len : 0.0f;
                    float ly = (axis == 1) ? len : 0.0f;
                    float lz = (axis == 2) ? len : 0.0f;
                    float wx = jointOffset.x + lx;
                    float wy = jointOffset.y + ly;
                    float wz = jointOffset.z + lz;
                    float rx = (wx * cosR + wz * sinR) * modelScale;
                    float ry = wy * modelScale;
                    float rz = (-wx * sinR + wz * cosR) * modelScale;
                    float x1 = rx * cosY + rz * sinY;
                    float y1 = ry;
                    float z1 = -rx * sinY + rz * cosY;
                    float x2 = x1;
                    float y2 = y1 * cosP - z1 * sinP;
                    float z2 = y1 * sinP + z1 * cosP;
                    float sx = cx + x2 * s;
                    float sy = cy - y2 * s;
                    float dist = (float) Math.hypot(mouseX - sx, mouseY - sy);
                    if (dist <= hitThreshold) {
                        if (z2 > bestZ || (Math.abs(z2 - bestZ) < 0.05f && dist < bestDist)) {
                            bestZ = z2;
                            bestDist = dist;
                            bestAxis = axis;
                        }
                    }
                }
            }
            return bestAxis;
        }
    }

    private static void drawCenterCube(VertexConsumer consumer, Matrix4f mat, float size, int color) {
        consumer.addVertex(mat, -size, -size, -size).setColor(color);
        consumer.addVertex(mat, -size, size, -size).setColor(color);
        consumer.addVertex(mat, size, size, -size).setColor(color);
        consumer.addVertex(mat, size, -size, -size).setColor(color);

        consumer.addVertex(mat, -size, -size, size).setColor(color);
        consumer.addVertex(mat, size, -size, size).setColor(color);
        consumer.addVertex(mat, size, size, size).setColor(color);
        consumer.addVertex(mat, -size, size, size).setColor(color);

        consumer.addVertex(mat, -size, -size, -size).setColor(color);
        consumer.addVertex(mat, size, -size, -size).setColor(color);
        consumer.addVertex(mat, size, -size, size).setColor(color);
        consumer.addVertex(mat, -size, -size, size).setColor(color);

        consumer.addVertex(mat, -size, size, -size).setColor(color);
        consumer.addVertex(mat, -size, size, size).setColor(color);
        consumer.addVertex(mat, size, size, size).setColor(color);
        consumer.addVertex(mat, size, size, -size).setColor(color);
    }

    public static Vector3f getJointWorldOffset(String bone, boolean isSlim) {
        float armX = isSlim ? 0.28125f : 0.3125f;
        return switch (bone) {
            case "root" -> new Vector3f(0.0f, 0.75f, 0.0f);
            case "head" -> new Vector3f(0.0f, 1.5f, 0.0f);
            case "body" -> new Vector3f(0.0f, 0.75f, 0.0f);
            case "right_arm" -> new Vector3f(armX, 1.375f, 0.0f);
            case "left_arm" -> new Vector3f(-armX, 1.375f, 0.0f);
            case "right_forearm" -> new Vector3f(armX, 1.125f, 0.0f);
            case "left_forearm" -> new Vector3f(-armX, 1.125f, 0.0f);
            case "right_leg" -> new Vector3f(0.11875f, 0.75f, 0.0f);
            case "left_leg" -> new Vector3f(-0.11875f, 0.75f, 0.0f);
            case "right_lower_leg" -> new Vector3f(0.11875f, 0.375f, 0.0f);
            case "left_lower_leg" -> new Vector3f(-0.11875f, 0.375f, 0.0f);
            default -> new Vector3f(0.0f, 0.75f, 0.0f);
        };
    }

    public static Vector3f getBonePivot(String bone, boolean isSlim, boolean isBendable) {
        return getBonePivot(bone, isSlim, isBendable, null, null, null);
    }

    public static Vector3f getBonePivot(String bone, boolean isSlim, boolean isBendable,
                                        java.util.Map<String, float[]> rotations,
                                        java.util.Map<String, float[]> positions,
                                        net.openstage.client.model.ModelSettings settings) {
        if (bone == null || bone.isEmpty()) {
            bone = "root";
        }
        float armX = isSlim ? 0.28125f : 0.3125f;
        Vector3f p = switch (bone) {
            case "root" -> new Vector3f(0.0f, 0.75f, 0.0f);
            case "head" -> new Vector3f(0.0f, 1.5f, 0.0f);
            case "body" -> new Vector3f(0.0f, 0.75f, 0.0f);
            case "right_arm" -> new Vector3f(armX, 1.375f, 0.0f);
            case "left_arm" -> new Vector3f(-armX, 1.375f, 0.0f);
            case "right_forearm" -> new Vector3f(armX, 1.125f, 0.0f);
            case "left_forearm" -> new Vector3f(-armX, 1.125f, 0.0f);
            case "right_leg" -> new Vector3f(0.11875f, 0.75f, 0.0f);
            case "left_leg" -> new Vector3f(-0.11875f, 0.75f, 0.0f);
            case "right_lower_leg" -> new Vector3f(0.11875f, 0.375f, 0.0f);
            case "left_lower_leg" -> new Vector3f(-0.11875f, 0.375f, 0.0f);
            default -> new Vector3f(0.0f, 0.75f, 0.0f);
        };

        if (rotations != null && (bone.equals("head") || bone.equals("right_arm") || bone.equals("left_arm") || bone.equals("right_forearm") || bone.equals("left_forearm"))) {
            float[] bRot = rotations.get("body");
            if (bRot != null && bRot.length >= 3) {
                float dy = p.y - 0.75f;
                float dx = p.x;
                float dz = p.z;
                float radX = bRot[0];
                float radY = bRot[1];
                float radZ = bRot[2];
                float cosX = (float) Math.cos(radX), sinX = (float) Math.sin(radX);
                float cosY = (float) Math.cos(radY), sinY = (float) Math.sin(radY);
                float ny = dy * cosX - dz * sinX;
                float nz = dy * sinX + dz * cosX;
                float nx = dx * cosY + nz * sinY;
                nz = -dx * sinY + nz * cosY;
                p.x = nx;
                p.y = 0.75f + ny;
                p.z = nz;
            }
        }

        if (positions != null) {
            if (!"root".equals(bone) && positions.containsKey(bone)) {
                float[] bp = positions.get(bone);
                if (bp != null && bp.length >= 3) {
                    p.add(bp[0], bp[1], bp[2]);
                }
            }
            if (bone.equals("head") || bone.equals("right_arm") || bone.equals("left_arm") || bone.equals("right_forearm") || bone.equals("left_forearm")) {
                float[] parentP = positions.get("body");
                if (parentP != null && parentP.length >= 3) {
                    p.add(parentP[0], parentP[1], parentP[2]);
                }
            }
        }

        if (rotations != null && rotations.containsKey("root")) {
            float[] rr = rotations.get("root");
            if (rr != null && rr.length >= 3) {
                float radX = rr[0];
                float radY = rr[1];
                float radZ = rr[2];
                float cosZ = (float) Math.cos(radZ), sinZ = (float) Math.sin(radZ);
                float cosX = (float) Math.cos(radX), sinX = (float) Math.sin(radX);
                float cosY = (float) Math.cos(radY), sinY = (float) Math.sin(radY);

                float x1 = p.x * cosZ - p.y * sinZ;
                float y1 = p.x * sinZ + p.y * cosZ;
                float z1 = p.z;

                float x2 = x1;
                float y2 = y1 * cosX - z1 * sinX;
                float z2 = y1 * sinX + z1 * cosX;

                float x3 = x2 * cosY + z2 * sinY;
                float y3 = y2;
                float z3 = -x2 * sinY + z2 * cosY;

                p.x = x3;
                p.y = y3;
                p.z = z3;
            }
        }

        float scale = (settings != null) ? settings.getScale() : 1.0f;
        if (scale != 1.0f) {
            p.mul(scale);
        }

        float rootYaw = (settings != null) ? settings.getRootYaw() : 0.0f;
        if (rootYaw != 0.0f) {
            float rad = (float) Math.toRadians(rootYaw);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            float rx = p.x * cos + p.z * sin;
            float rz = -p.x * sin + p.z * cos;
            p.x = rx;
            p.z = rz;
        }

        if (positions != null && positions.containsKey("root")) {
            float[] rp = positions.get("root");
            if (rp != null && rp.length >= 3) {
                p.add(rp[0], rp[1], rp[2]);
            }
        }

        return p;
    }
}

