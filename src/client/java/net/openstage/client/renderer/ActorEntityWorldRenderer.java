package net.openstage.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.openstage.client.skin.SkinManager;
import net.openstage.core.animation.AnimationClip;
import net.openstage.core.scene.ActorEntity;
import net.openstage.core.scene.SceneManager;
import org.jetbrains.annotations.NotNull;
import java.util.Map;

public class ActorEntityWorldRenderer extends EntityRenderer<ActorEntity> {
    private final PlayerModel<ActorEntity> wideModel;
    private final PlayerModel<ActorEntity> slimModel;

    public ActorEntityWorldRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.wideModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.wideModel.young = false;
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.slimModel.young = false;
    }

    @Override
    public void render(ActorEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        boolean isSlim = entity.isSlim() || SkinManager.getInstance().isSkinSlim(entity.getSkin());
        PlayerModel<ActorEntity> model = isSlim ? this.slimModel : this.wideModel;
        model.young = false;

        poseStack.pushPose();

        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0, -1.501, 0.0);

        resetModelPose(model);

        if (entity.isPlaying()) {
            applyAnimation(entity, model, partialTick, poseStack);
        }

        ResourceLocation texture = getTextureLocation(entity);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    private void applyAnimation(ActorEntity entity, PlayerModel<ActorEntity> model, float partialTick, PoseStack poseStack) {
        AnimationClip clip = SceneManager.getInstance().getClip(entity.getClipId());
        if (clip == null) return;
        long currentTick = entity.level().getGameTime();
        float elapsedSeconds = (float) (currentTick - entity.getStartTick() + partialTick) / 20.0f;
        Map<String, float[]> rotations = clip.evaluateFrame(elapsedSeconds);
        Map<String, float[]> positions = clip.evaluatePositions(elapsedSeconds);

        if (positions != null && positions.containsKey("root")) {
            float[] rp = positions.get("root");
            if (rp != null && rp.length >= 3) {
                poseStack.translate(rp[0], rp[1], rp[2]);
            }
        }
        if (rotations != null && rotations.containsKey("root")) {
            float[] rr = rotations.get("root");
            if (rr != null && rr.length >= 3) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rr[1]));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rr[0]));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rr[2]));
            }
        }

        applyBoneTransform(model.head, rotations.get("head"), positions.get("head"));
        applyBoneTransform(model.body, rotations.get("body"), positions.get("body"));
        applyBoneTransform(model.rightArm, rotations.get("right_arm"), positions.get("right_arm"));
        applyBoneTransform(model.leftArm, rotations.get("left_arm"), positions.get("left_arm"));
        applyBoneTransform(model.rightLeg, rotations.get("right_leg"), positions.get("right_leg"));
        applyBoneTransform(model.leftLeg, rotations.get("left_leg"), positions.get("left_leg"));

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

    private void applyBoneTransform(net.minecraft.client.model.geom.ModelPart part, float[] rot, float[] pos) {
        if (part == null) return;
        if (rot != null) {
            part.xRot = (float) Math.toRadians(rot[0]);
            part.yRot = (float) Math.toRadians(rot[1]);
            part.zRot = (float) Math.toRadians(rot[2]);
        }
        if (pos != null) {
            if (Math.abs(pos[1]) > 50.0f || Math.abs(pos[0]) > 50.0f || Math.abs(pos[2]) > 50.0f) {
                part.visible = false;
                return;
            }
            part.x += pos[0] * 16.0f;
            part.y += pos[1] * 16.0f;
            part.z += pos[2] * 16.0f;
        }
    }

    private void resetModelPose(PlayerModel<ActorEntity> model) {
        model.head.resetPose();
        model.body.resetPose();
        model.rightArm.resetPose();
        model.leftArm.resetPose();
        model.rightLeg.resetPose();
        model.leftLeg.resetPose();
        model.hat.resetPose();
        model.jacket.resetPose();
        model.rightSleeve.resetPose();
        model.leftSleeve.resetPose();
        model.rightPants.resetPose();
        model.leftPants.resetPose();
        model.head.visible = true;
        model.body.visible = true;
        model.rightArm.visible = true;
        model.leftArm.visible = true;
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;
        model.hat.visible = true;
        model.jacket.visible = true;
        model.rightSleeve.visible = true;
        model.leftSleeve.visible = true;
        model.rightPants.visible = true;
        model.leftPants.visible = true;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(ActorEntity entity) {
        return SkinManager.getInstance().getSkinTexture(entity.getSkin(), entity.isSlim());
    }
}

