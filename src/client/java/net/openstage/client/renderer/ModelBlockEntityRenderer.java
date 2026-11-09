package net.openstage.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.openstage.block.entity.ModelBlockEntity;
import net.openstage.client.skin.SkinManager;
import net.openstage.core.animation.AnimationClip;
import net.openstage.core.scene.SceneManager;
import org.jetbrains.annotations.NotNull;
import java.util.Map;

public class ModelBlockEntityRenderer implements BlockEntityRenderer<ModelBlockEntity> {
    @SuppressWarnings("rawtypes")
    private final PlayerModel wideModel;
    @SuppressWarnings("rawtypes")
    private final PlayerModel slimModel;

    public ModelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.wideModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.wideModel.young = false;
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.slimModel.young = false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(@NotNull ModelBlockEntity entity, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (entity.getLevel() == null) return;
        String modelId = entity.getModelId();
        net.openstage.client.model.ObjModel obj = null;
        if (modelId != null && !modelId.startsWith("player:")) {
            obj = net.openstage.client.model.ObjModelManager.getInstance().getOrLoadModel(modelId);
        }
        if (obj == null && entity.getSkin() != null && !entity.getSkin().equals("steve") && !entity.getSkin().equals("alex")) {
            obj = net.openstage.client.model.ObjModelManager.getInstance().getOrLoadModel(entity.getSkin());
        }

        if (obj != null) {
            poseStack.pushPose();
            poseStack.translate(0.5 + entity.getOffsetX(), 0.0 + entity.getOffsetY(), 0.5 + entity.getOffsetZ());
            poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYaw()));
            float scale = entity.getScale();
            poseStack.scale(scale, scale, scale);
            Map<String, float[]> rots = null;
            Map<String, float[]> poses = null;
            if (entity.isStaticPose()) {
                rots = entity.getPoseRotations();
                poses = entity.getPosePositions();
            } else {
                String clipId = entity.getClipId();
                AnimationClip clip = SceneManager.getInstance().getClip(clipId);
                if (clip != null) {
                    long currentTick = entity.getLevel().getGameTime();
                    long elapsedTicks = currentTick - entity.getStartTick();
                    float timeSeconds = (elapsedTicks + partialTick) / 20.0f;
                    rots = clip.evaluateFrame(timeSeconds);
                    poses = clip.evaluatePositions(timeSeconds);
                }
            }

            net.openstage.client.model.ModelSettings settings = new net.openstage.client.model.ModelSettings();
            settings.setRootYaw(0.0f);
            settings.setScale(1.0f);

            ActorEntityRenderer.renderObjModel(poseStack, buffer, packedLight, obj, rots, poses, settings, null);
            poseStack.popPose();
            return;
        }

        poseStack.pushPose();

        poseStack.translate(0.5 + entity.getOffsetX(), 0.0 + entity.getOffsetY(), 0.5 + entity.getOffsetZ());

        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYaw()));
        float scale = entity.getScale();
        poseStack.scale(scale, scale, scale);

        boolean isSlim = entity.isSlim() || SkinManager.getInstance().isSkinSlim(entity.getSkin());
        @SuppressWarnings("rawtypes")
        PlayerModel model = isSlim ? this.slimModel : this.wideModel;
        model.young = false;

        resetModelPose(model);
        Map<String, float[]> rots = null;
        Map<String, float[]> poses = null;
        if (entity.isStaticPose()) {
            rots = entity.getPoseRotations();
            poses = entity.getPosePositions();
        } else {
            String clipId = entity.getClipId();
            AnimationClip clip = SceneManager.getInstance().getClip(clipId);
            if (clip != null) {
                long currentTick = entity.getLevel().getGameTime();
                long elapsedTicks = currentTick - entity.getStartTick();
                float timeSeconds = (elapsedTicks + partialTick) / 20.0f;
                rots = clip.evaluateFrame(timeSeconds);
                poses = clip.evaluatePositions(timeSeconds);
            }
        }

        if (poses != null && poses.containsKey("root")) {
            float[] rp = poses.get("root");
            if (rp != null && rp.length >= 3) {
                poseStack.translate(rp[0], rp[1], rp[2]);
            }
        }
        if (rots != null && rots.containsKey("root")) {
            float[] rr = rots.get("root");
            if (rr != null && rr.length >= 3) {
                poseStack.mulPose(Axis.YP.rotation(rr[1]));
                poseStack.mulPose(Axis.XP.rotation(rr[0]));
                poseStack.mulPose(Axis.ZP.rotation(rr[2]));
            }
        }

        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0, -1.501, 0.0);

        if (rots != null) {
            applyRotations(model, rots);
        }
        if (poses != null) {
            applyPositions(model, poses);
        }

        ResourceLocation skinTexture = SkinManager.getInstance().getSkinTexture(entity.getSkin(), isSlim);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(skinTexture));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        if (!entity.getRightHandItem().isEmpty()) {
            poseStack.pushPose();
            model.rightArm.translateAndRotate(poseStack);
            poseStack.translate(-0.0625f, 0.4375f, 0.0625f);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    entity.getRightHandItem(),
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    entity.getLevel(),
                    0
            );
            poseStack.popPose();
        }

        if (!entity.getLeftHandItem().isEmpty()) {
            poseStack.pushPose();
            model.leftArm.translateAndRotate(poseStack);
            poseStack.translate(0.0625f, 0.4375f, 0.0625f);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    entity.getLeftHandItem(),
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    entity.getLevel(),
                    0
            );
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    @SuppressWarnings("rawtypes")
    private void resetModelPose(PlayerModel model) {
        model.head.resetPose();
        model.hat.resetPose();
        model.body.resetPose();
        model.jacket.resetPose();
        model.rightArm.resetPose();
        model.rightSleeve.resetPose();
        model.leftArm.resetPose();
        model.leftSleeve.resetPose();
        model.rightLeg.resetPose();
        model.rightPants.resetPose();
        model.leftLeg.resetPose();
        model.leftPants.resetPose();
        model.head.visible = true;
        model.hat.visible = true;
        model.body.visible = true;
        model.jacket.visible = true;
        model.rightArm.visible = true;
        model.rightSleeve.visible = true;
        model.leftArm.visible = true;
        model.leftSleeve.visible = true;
        model.rightLeg.visible = true;
        model.rightPants.visible = true;
        model.leftLeg.visible = true;
        model.leftPants.visible = true;
    }

    @SuppressWarnings("rawtypes")
    private void applyRotations(PlayerModel model, Map<String, float[]> rots) {
        if (rots.containsKey("head")) applyToPart(model.head, rots.get("head"));
        if (rots.containsKey("body")) applyToPart(model.body, rots.get("body"));
        if (rots.containsKey("right_arm")) applyToPart(model.rightArm, rots.get("right_arm"));
        if (rots.containsKey("left_arm")) applyToPart(model.leftArm, rots.get("left_arm"));
        if (rots.containsKey("right_leg")) applyToPart(model.rightLeg, rots.get("right_leg"));
        if (rots.containsKey("left_leg")) applyToPart(model.leftLeg, rots.get("left_leg"));

        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    @SuppressWarnings("rawtypes")
    private void applyPositions(PlayerModel model, Map<String, float[]> poses) {
        if (poses == null || poses.isEmpty()) return;
        if (poses.containsKey("head")) applyPosToPart(model.head, poses.get("head"));
        if (poses.containsKey("body")) applyPosToPart(model.body, poses.get("body"));
        if (poses.containsKey("right_arm")) applyPosToPart(model.rightArm, poses.get("right_arm"));
        if (poses.containsKey("left_arm")) applyPosToPart(model.leftArm, poses.get("left_arm"));
        if (poses.containsKey("right_leg")) applyPosToPart(model.rightLeg, poses.get("right_leg"));
        if (poses.containsKey("left_leg")) applyPosToPart(model.leftLeg, poses.get("left_leg"));

        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
        model.hat.visible = model.head.visible;
        model.jacket.visible = model.body.visible;
        model.rightSleeve.visible = model.rightArm.visible;
        model.leftSleeve.visible = model.leftArm.visible;
        model.rightPants.visible = model.rightLeg.visible;
        model.leftPants.visible = model.leftLeg.visible;
    }

    private void applyPosToPart(net.minecraft.client.model.geom.ModelPart part, float[] pos) {
        if (pos != null && pos.length >= 3) {
            if (Math.abs(pos[1]) > 50.0f || Math.abs(pos[0]) > 50.0f || Math.abs(pos[2]) > 50.0f) {
                part.visible = false;
                return;
            }
            part.x -= pos[0] * 16.0f;
            part.y -= pos[1] * 16.0f;
            part.z += pos[2] * 16.0f;
        }
    }

    private void applyToPart(net.minecraft.client.model.geom.ModelPart part, float[] rotDeg) {
        if (rotDeg != null && rotDeg.length >= 3) {
            part.xRot = (float) Math.toRadians(rotDeg[0]);
            part.yRot = (float) Math.toRadians(rotDeg[1]);
            part.zRot = (float) Math.toRadians(rotDeg[2]);
        }
    }
}

