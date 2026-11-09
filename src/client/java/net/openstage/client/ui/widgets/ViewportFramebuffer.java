package net.openstage.client.ui.widgets;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.openstage.client.renderer.ActorEntityRenderer;
import net.openstage.client.skin.SkinManager;
import net.openstage.core.animation.AnimationClip;
import net.openstage.core.animation.TextCard;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

public class ViewportFramebuffer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("OpenStage/Viewport");
    private RenderTarget renderTarget;
    private int fbWidth, fbHeight;
    private boolean initialised;

    public void init(int width, int height) {
        if (width <= 0 || height <= 0) {
            LOGGER.warn("Ignoring init() with non-positive size ({}×{}).", width, height);
            return;
        }
        destroyIfNeeded();
        this.fbWidth = width;
        this.fbHeight = height;
        this.renderTarget = new TextureTarget(width, height, true, false);
        this.renderTarget.setClearColor(0.06f, 0.06f, 0.10f, 1.0f);
        this.initialised = true;

        LOGGER.debug("Viewport framebuffer created ({}×{}).", width, height);
    }

    public void resize(int width, int height) {
        if (width == fbWidth && height == fbHeight && initialised) {
            return;
        }
        init(width, height);
    }

    public void beginRender() {
        if (!initialised) {
            throw new IllegalStateException("ViewportFramebuffer has not been initialised.");
        }
        renderTarget.bindWrite(true);
        renderTarget.clear(false);
    }

    public void endRender() {
        if (initialised) {
            renderTarget.unbindWrite();

            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }

    public void renderScene(AnimationClip clip, float currentTime, String skinName, boolean isSlim,
                            float cameraPitch, float cameraYaw, float cameraZoom,
                            net.openstage.client.model.ModelSettings settings) {
        if (!initialised || renderTarget == null) return;

        if (settings != null) {
            net.openstage.client.model.ModelSettings.BackgroundMode bg = settings.getBackgroundMode();
            renderTarget.setClearColor(bg.getR(), bg.getG(), bg.getB(), 1.0f);
        }

        beginRender();
        try {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.disableCull();

            com.mojang.blaze3d.platform.Lighting.setupForEntityInInventory();

            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            RenderSystem.applyModelViewMatrix();

            RenderSystem.backupProjectionMatrix();
            try {
                float fov = 55.0f;
                boolean cinematic = (settings != null) && settings.isCinematicCamera() && (clip != null) && !clip.getCameraTrack().getKeyframes().isEmpty();
                net.openstage.core.scene.CameraTrack.CameraKeyframe camKey = null;
                if (cinematic) {
                    camKey = clip.getCameraTrack().interpolateAt(currentTime);
                    fov = camKey.fov();
                }

                Matrix4f proj = new Matrix4f().perspective(
                        (float) Math.toRadians(fov),
                        (float) fbWidth / (float) fbHeight,
                        0.05f, 100.0f
                );
                RenderSystem.setProjectionMatrix(proj, VertexSorting.DISTANCE_TO_ORIGIN);
                PoseStack poseStack = new PoseStack();
                poseStack.pushPose();

                if (cinematic && camKey != null) {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-camKey.roll()));
                    poseStack.mulPose(Axis.XP.rotationDegrees(camKey.pitch()));
                    poseStack.mulPose(Axis.YP.rotationDegrees(camKey.yaw()));
                    poseStack.translate(-camKey.x(), -camKey.y(), -camKey.z());
                } else {
                    poseStack.translate(0.0f, 0.0f, -cameraZoom);
                    poseStack.mulPose(Axis.XP.rotationDegrees(cameraPitch));
                    poseStack.mulPose(Axis.YP.rotationDegrees(cameraYaw));
                    poseStack.translate(0.0f, -0.9f, 0.0f);
                }

                MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

                if (settings == null || settings.isShowGrid()) {
                    renderGrid(poseStack, bufferSource, 6, 0.5f);
                    bufferSource.endBatch(RenderType.lines());
                }

                float[] bodyRot = new float[3];
                float[] headRot = new float[3];
                float[] rArmRot = new float[3];
                float[] lArmRot = new float[3];
                float[] rLegRot = new float[3];
                float[] lLegRot = new float[3];

                if (clip != null) {
                    Map<String, float[]> rots = clip.evaluateFrame(currentTime);
                    if (rots.containsKey("body")) toRadians(rots.get("body"), bodyRot);
                    if (rots.containsKey("head")) toRadians(rots.get("head"), headRot);
                    if (rots.containsKey("right_arm")) toRadians(rots.get("right_arm"), rArmRot);
                    if (rots.containsKey("left_arm")) toRadians(rots.get("left_arm"), lArmRot);
                    if (rots.containsKey("right_leg")) toRadians(rots.get("right_leg"), rLegRot);
                    if (rots.containsKey("left_leg")) toRadians(rots.get("left_leg"), lLegRot);
                }

                ResourceLocation skinTex = SkinManager.getInstance().getSkinTexture(skinName, isSlim);

                ActorEntityRenderer.renderAnimatedPlayerModel(
                        poseStack, bufferSource, LightTexture.FULL_BRIGHT,
                        bodyRot, headRot, rArmRot, lArmRot, rLegRot, lLegRot,
                        isSlim, skinTex, settings
                );

                bufferSource.endBatch();

                poseStack.popPose();
            } finally {
                RenderSystem.restoreProjectionMatrix();
                RenderSystem.getModelViewStack().popMatrix();
                RenderSystem.applyModelViewMatrix();
                com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
                RenderSystem.disableDepthTest();
            }
        } finally {
            endRender();
        }
    }

    private void renderGrid(PoseStack poseStack, MultiBufferSource buffers, int radius, float spacing) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        Matrix4f pose = poseStack.last().pose();
        PoseStack.Pose normalPose = poseStack.last();

        for (int i = -radius; i <= radius; i++) {
            float coord = i * spacing;
            int color = (i == 0) ? 0xFF5C9DFF : 0x88A0A0B0;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int a = (color >> 24) & 0xFF;

            consumer.addVertex(pose, -radius * spacing, 0.0f, coord)
                    .setColor(r, g, b, a)
                    .setNormal(normalPose, 0.0f, 1.0f, 0.0f);
            consumer.addVertex(pose, radius * spacing, 0.0f, coord)
                    .setColor(r, g, b, a)
                    .setNormal(normalPose, 0.0f, 1.0f, 0.0f);

            consumer.addVertex(pose, coord, 0.0f, -radius * spacing)
                    .setColor(r, g, b, a)
                    .setNormal(normalPose, 0.0f, 1.0f, 0.0f);
            consumer.addVertex(pose, coord, 0.0f, radius * spacing)
                    .setColor(r, g, b, a)
                    .setNormal(normalPose, 0.0f, 1.0f, 0.0f);
        }
    }

    private static void toRadians(float[] deg, float[] rad) {
        if (deg == null || rad == null) return;
        rad[0] = (float) Math.toRadians(deg[0]);
        rad[1] = (float) Math.toRadians(deg[1]);
        rad[2] = (float) Math.toRadians(deg[2]);
    }

    public void renderToScreen(GuiGraphics graphics, int x, int y, int width, int height,
                               AnimationClip clip, float currentTime) {
        if (!initialised || renderTarget == null) return;

        graphics.flush();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, renderTarget.getColorTextureId());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix, (float) x, (float) (y + height), 0.0f).setUv(0.0f, 0.0f);
        bufferBuilder.addVertex(matrix, (float) (x + width), (float) (y + height), 0.0f).setUv(1.0f, 0.0f);
        bufferBuilder.addVertex(matrix, (float) (x + width), (float) y, 0.0f).setUv(1.0f, 1.0f);
        bufferBuilder.addVertex(matrix, (float) x, (float) y, 0.0f).setUv(0.0f, 1.0f);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        graphics.flush();

        if (clip != null) {
            List<TextCard> activeCards = clip.getTextTrack().getActiveCardsAt(currentTime);
            for (TextCard card : activeCards) {
                TextOverlayRenderer.render(graphics, card, currentTime, x, y, width, height);
            }
        }
    }

    @Override
    public void close() {
        destroyIfNeeded();
        LOGGER.debug("Viewport framebuffer closed.");
    }

    private void destroyIfNeeded() {
        if (renderTarget != null) {
            renderTarget.destroyBuffers();
            renderTarget = null;
            initialised  = false;
        }
    }

    public boolean isInitialised() {
        return initialised;
    }

    public int getWidth() {
        return fbWidth;
    }

    public int getHeight() {
        return fbHeight;
    }
}

