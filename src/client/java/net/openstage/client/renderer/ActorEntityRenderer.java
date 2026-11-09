package net.openstage.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

public final class ActorEntityRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("OpenStage/ActorRenderer");

    private static final ResourceLocation STEVE_SKIN =

            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private static final ResourceLocation ALEX_SKIN =

            ResourceLocation.withDefaultNamespace("textures/entity/player/slim/alex.png");

    private static final ResourceLocation BBS_EYES_STEVE =

            ResourceLocation.fromNamespaceAndPath("openstage", "textures/entity/eyes/steve.png");

    private static final ResourceLocation BBS_EYES_ALEX =

            ResourceLocation.fromNamespaceAndPath("openstage", "textures/entity/eyes/alex.png");

    private static PlayerModel wideModel;
    private static PlayerModel slimModel;
    private static BendableRig bendableWideRig;
    private static BendableRig bendableSlimRig;

    public static final class BendableRig {
        public final ModelPart root;
        public final ModelPart head;
        public final ModelPart hat;
        public final ModelPart body;
        public final ModelPart jacket;
        public final ModelPart rightArm;
        public final ModelPart rightForearm;
        public final ModelPart rightSleeve;
        public final ModelPart rightForearmSleeve;
        public final ModelPart leftArm;
        public final ModelPart leftForearm;
        public final ModelPart leftSleeve;
        public final ModelPart leftForearmSleeve;
        public final ModelPart rightLeg;
        public final ModelPart rightLowerLeg;
        public final ModelPart rightPants;
        public final ModelPart rightLowerPants;
        public final ModelPart leftLeg;
        public final ModelPart leftLowerLeg;
        public final ModelPart leftPants;
        public final ModelPart leftLowerPants;

                        public BendableRig(ModelPart root) {
            this.root = root;
            this.body = root.getChild("body");
            this.jacket = this.body.getChild("jacket");
            this.head = this.body.getChild("head");
            this.hat = this.head.getChild("hat");
            this.rightArm = this.body.getChild("right_arm");
            this.rightForearm = this.rightArm.getChild("right_forearm");
            this.rightSleeve = this.rightArm.getChild("right_sleeve");
            this.rightForearmSleeve = this.rightForearm.getChild("right_forearm_sleeve");
            this.leftArm = this.body.getChild("left_arm");
            this.leftForearm = this.leftArm.getChild("left_forearm");
            this.leftSleeve = this.leftArm.getChild("left_sleeve");
            this.leftForearmSleeve = this.leftForearm.getChild("left_forearm_sleeve");
            this.rightLeg = root.getChild("right_leg");
            this.rightLowerLeg = this.rightLeg.getChild("right_lower_leg");
            this.rightPants = this.rightLeg.getChild("right_pants");
            this.rightLowerPants = this.rightLowerLeg.getChild("right_lower_pants");
            this.leftLeg = root.getChild("left_leg");
            this.leftLowerLeg = this.leftLeg.getChild("left_lower_leg");
            this.leftPants = this.leftLeg.getChild("left_pants");
            this.leftLowerPants = this.leftLowerLeg.getChild("left_lower_pants");
        }

        public void resetAllPoses() {
            head.resetPose(); hat.resetPose();

            body.resetPose(); jacket.resetPose();

            rightArm.resetPose(); rightForearm.resetPose();

            rightSleeve.resetPose(); rightForearmSleeve.resetPose();

            leftArm.resetPose(); leftForearm.resetPose();

            leftSleeve.resetPose(); leftForearmSleeve.resetPose();

            rightLeg.resetPose(); rightLowerLeg.resetPose();

            rightPants.resetPose(); rightLowerPants.resetPose();

            leftLeg.resetPose(); leftLowerLeg.resetPose();

            leftPants.resetPose(); leftLowerPants.resetPose();
            head.visible = true; hat.visible = true;
            body.visible = true; jacket.visible = true;
            rightArm.visible = true; rightForearm.visible = true;
            rightSleeve.visible = true; rightForearmSleeve.visible = true;
            leftArm.visible = true; leftForearm.visible = true;
            leftSleeve.visible = true; leftForearmSleeve.visible = true;
            rightLeg.visible = true; rightLowerLeg.visible = true;
            rightPants.visible = true; rightLowerPants.visible = true;
            leftLeg.visible = true; leftLowerLeg.visible = true;
            leftPants.visible = true; leftLowerPants.visible = true;
        }
    }

    private ActorEntityRenderer() { }

    private static BendableRig getOrCreateBendableRig(boolean slim) {
        if (slim) {
            if (bendableSlimRig == null) {
                bendableSlimRig = new BendableRig(createBendableModelPart(true));
            }

            return bendableSlimRig;
        } else {
            if (bendableWideRig == null) {
                bendableWideRig = new BendableRig(createBendableModelPart(false));
            }

            return bendableWideRig;
        }
    }

    private static ModelPart createBendableModelPart(boolean slim) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

                PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f),
                PartPose.ZERO
        );
        body.addOrReplaceChild("jacket",
                CubeListBuilder.create().texOffs(16, 32).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.25f)),
                PartPose.ZERO
        );

        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f),
                PartPose.ZERO
        );
        head.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.5f)),
                PartPose.ZERO
        );
        float armW = slim ? 3.0f : 4.0f;
        float rArmX = slim ? -2.0f : -3.0f;

        PartDefinition rightArm = body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(rArmX, -2.0f, -2.0f, armW, 6.0f, 4.0f),
                PartPose.offset(-5.0f, 2.0f, 0.0f)
        );
        rightArm.addOrReplaceChild("right_sleeve",
                CubeListBuilder.create().texOffs(40, 32).addBox(rArmX, -2.0f, -2.0f, armW, 6.0f, 4.0f, new CubeDeformation(0.25f)),
                PartPose.ZERO
        );
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm",
                CubeListBuilder.create().texOffs(40, 22).addBox(rArmX, 0.0f, -2.0f, armW, 6.0f, 4.0f),
                PartPose.offset(0.0f, 4.0f, 0.0f)
        );
        rightForearm.addOrReplaceChild("right_forearm_sleeve",
                CubeListBuilder.create().texOffs(40, 38).addBox(rArmX, 0.0f, -2.0f, armW, 6.0f, 4.0f, new CubeDeformation(0.25f)),
                PartPose.ZERO
        );

        PartDefinition leftArm = body.addOrReplaceChild("left_arm",

                CubeListBuilder.create().texOffs(32, 48).addBox(-1.0f, -2.0f, -2.0f, armW, 6.0f, 4.0f),

                PartPose.offset(5.0f, 2.0f, 0.0f)
        );

        leftArm.addOrReplaceChild("left_sleeve",

                CubeListBuilder.create().texOffs(48, 48).addBox(-1.0f, -2.0f, -2.0f, armW, 6.0f, 4.0f, new CubeDeformation(0.25f)),

                PartPose.ZERO
        );

        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm",

                CubeListBuilder.create().texOffs(32, 54).addBox(-1.0f, 0.0f, -2.0f, armW, 6.0f, 4.0f),

                PartPose.offset(0.0f, 4.0f, 0.0f)
        );

        leftForearm.addOrReplaceChild("left_forearm_sleeve",

                CubeListBuilder.create().texOffs(48, 54).addBox(-1.0f, 0.0f, -2.0f, armW, 6.0f, 4.0f, new CubeDeformation(0.25f)),

                PartPose.ZERO
        );

        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",

                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f),

                PartPose.offset(-1.9f, 12.0f, 0.0f)
        );

        rightLeg.addOrReplaceChild("right_pants",

                CubeListBuilder.create().texOffs(0, 32).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.25f)),

                PartPose.ZERO
        );

        PartDefinition rightLowerLeg = rightLeg.addOrReplaceChild("right_lower_leg",

                CubeListBuilder.create().texOffs(0, 22).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f),

                PartPose.offset(0.0f, 6.0f, 0.0f)
        );

        rightLowerLeg.addOrReplaceChild("right_lower_pants",

                CubeListBuilder.create().texOffs(0, 38).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.25f)),

                PartPose.ZERO
        );

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",

                CubeListBuilder.create().texOffs(16, 48).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f),

                PartPose.offset(1.9f, 12.0f, 0.0f)
        );

        leftLeg.addOrReplaceChild("left_pants",

                CubeListBuilder.create().texOffs(0, 48).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.25f)),

                PartPose.ZERO
        );

        PartDefinition leftLowerLeg = leftLeg.addOrReplaceChild("left_lower_leg",

                CubeListBuilder.create().texOffs(16, 54).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f),

                PartPose.offset(0.0f, 6.0f, 0.0f)
        );

        leftLowerLeg.addOrReplaceChild("left_lower_pants",

                CubeListBuilder.create().texOffs(0, 54).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.25f)),

                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    private static PlayerModel getOrCreateModel(boolean slim) {
        if (slim) {
            if (slimModel == null) {
                EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();

                slimModel = new PlayerModel(
                        modelSet.bakeLayer(ModelLayers.PLAYER_SLIM), true
                );
                slimModel.young = false;
            }

            return slimModel;
        } else {
            if (wideModel == null) {
                EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();

                wideModel = new PlayerModel(
                        modelSet.bakeLayer(ModelLayers.PLAYER), false
                );
                wideModel.young = false;
            }

            return wideModel;
        }
    }

    public static void renderGhostModel(
            PoseStack poseStack,

            MultiBufferSource buffers,

            int light,

            Map<String, float[]> rots,

            Map<String, float[]> poses,

            boolean slim,

            ResourceLocation skin,

            net.openstage.client.model.ModelSettings settings,

            int tintColor
    ) {
        PlayerModel model = getOrCreateModel(slim);
        model.young = false;

        model.body.resetPose();

        model.head.resetPose();

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

        if (rots != null) {
            applyRotation(model.body, rots.get("body"));

            applyRotation(model.head, rots.get("head"));

            applyRotation(model.rightArm, rots.get("right_arm"));

            applyRotation(model.leftArm, rots.get("left_arm"));

            applyRotation(model.rightLeg, rots.get("right_leg"));

            applyRotation(model.leftLeg, rots.get("left_leg"));
        }

        if (poses != null) {
            applyPosition(model.body, poses.get("body"));

            applyPosition(model.head, poses.get("head"));

            applyPosition(model.rightArm, poses.get("right_arm"));

            applyPosition(model.leftArm, poses.get("left_arm"));

            applyPosition(model.rightLeg, poses.get("right_leg"));

            applyPosition(model.leftLeg, poses.get("left_leg"));
        }

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

        poseStack.pushPose();

        if (poses != null && poses.containsKey("root")) {
            float[] rp = poses.get("root");
            if (rp != null && rp.length >= 3) {
                poseStack.translate(rp[0], rp[1], rp[2]);
            }
        }

        if (settings != null && settings.getRootYaw() != 0.0f) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(settings.getRootYaw()));
        }

        if (settings != null && settings.getScale() != 1.0f) {
            float s = settings.getScale();
            poseStack.scale(s, s, s);
        }

        if (rots != null && rots.containsKey("root")) {
            float[] rr = rots.get("root");
            if (rr != null && rr.length >= 3) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotation(rr[1]));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotation(rr[0]));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(rr[2]));
            }
        }

        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0f, -1.501f, 0.0f);

        var vertexConsumer = buffers.getBuffer(RenderType.entityTranslucent(skin));

        model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, tintColor);

        poseStack.popPose();
    }

    public static void renderAnimatedPlayerModel(
            PoseStack poseStack,

            MultiBufferSource buffers,

            int light,

            float[] bodyRot,

            float[] headRot,

            float[] rightArmRot,

            float[] leftArmRot,

            float[] rightLegRot,

            float[] leftLegRot,

            boolean slim
    ) {
        ResourceLocation skin = slim ? ALEX_SKIN : STEVE_SKIN;

        renderAnimatedPlayerModel(poseStack, buffers, light, bodyRot, headRot, rightArmRot, leftArmRot, rightLegRot, leftLegRot, slim, skin);
    }

    public static void renderAnimatedPlayerModel(
            PoseStack poseStack,

            MultiBufferSource buffers,

            int light,

            float[] bodyRot,

            float[] headRot,

            float[] rightArmRot,

            float[] leftArmRot,

            float[] rightLegRot,

            float[] leftLegRot,

            boolean slim,

            ResourceLocation skin
    ) {
        renderAnimatedPlayerModel(poseStack, buffers, light, bodyRot, headRot, rightArmRot, leftArmRot, rightLegRot, leftLegRot, slim, skin, null, null);
    }

    public static void renderAnimatedPlayerModel(
            PoseStack poseStack,

            MultiBufferSource buffers,

            int light,

            float[] bodyRot,

            float[] headRot,

            float[] rightArmRot,

            float[] leftArmRot,

            float[] rightLegRot,

            float[] leftLegRot,

            boolean slim,

            ResourceLocation skin,

            net.openstage.client.model.ModelSettings settings
    ) {
        renderAnimatedPlayerModel(poseStack, buffers, light, bodyRot, headRot, rightArmRot, leftArmRot, rightLegRot, leftLegRot, slim, skin, settings, null);
    }

    public static void renderAnimatedPlayerModel(
            PoseStack poseStack,

            MultiBufferSource buffers,

            int light,

            Map<String, float[]> rotations,

            boolean slim,

            ResourceLocation skin,

            net.openstage.client.model.ModelSettings settings,

            String selectedBone
    ) {
        renderAnimatedPlayerModel(poseStack, buffers, light, rotations, null, slim, skin, settings, selectedBone);
    }

    public static void renderAnimatedPlayerModel(
            PoseStack poseStack,

            MultiBufferSource buffers,

            int light,

            Map<String, float[]> rotations,

            Map<String, float[]> positions,

            boolean slim,

            ResourceLocation skin,

            net.openstage.client.model.ModelSettings settings,

            String selectedBone
    ) {
        if (skin == null) {
            skin = slim ? ALEX_SKIN : STEVE_SKIN;
        }

        if (settings != null && settings.isBendableLimbs()) {
            BendableRig rig = getOrCreateBendableRig(slim);

            rig.resetAllPoses();

            if (rotations != null) {
                applyRotation(rig.head, rotations.get("head"));

                applyRotation(rig.body, rotations.get("body"));

                applyRotation(rig.rightArm, rotations.get("right_arm"));

                applyRotation(rig.rightForearm, rotations.get("right_forearm"));

                applyRotation(rig.leftArm, rotations.get("left_arm"));

                applyRotation(rig.leftForearm, rotations.get("left_forearm"));

                applyRotation(rig.rightLeg, rotations.get("right_leg"));

                applyRotation(rig.rightLowerLeg, rotations.get("right_lower_leg"));

                applyRotation(rig.leftLeg, rotations.get("left_leg"));

                applyRotation(rig.leftLowerLeg, rotations.get("left_lower_leg"));

                if (rig.body.xRot != 0.0f) {
                    float hipZ = (float) (Math.sin(rig.body.xRot) * 12.0);
                    float hipY = (float) ((1.0 - Math.cos(rig.body.xRot)) * 12.0);
                    rig.rightLeg.z += hipZ;
                    rig.rightLeg.y -= hipY;
                    rig.leftLeg.z += hipZ;
                    rig.leftLeg.y -= hipY;
                }
            }

            float[] bodyPos = (positions != null) ? positions.get("body") : null;

            if (positions != null) {
                applyPosition(rig.body, bodyPos);

                applyPosition(rig.head, positions.get("head"));

                applyPosition(rig.rightArm, positions.get("right_arm"));

                applyPosition(rig.rightForearm, positions.get("right_forearm"));

                applyPosition(rig.leftArm, positions.get("left_arm"));

                applyPosition(rig.leftForearm, positions.get("left_forearm"));

                applyPosition(rig.rightLeg, positions.get("right_leg"));

                applyPosition(rig.rightLowerLeg, positions.get("right_lower_leg"));

                applyPosition(rig.leftLeg, positions.get("left_leg"));

                applyPosition(rig.leftLowerLeg, positions.get("left_lower_leg"));
            }

            rig.hat.visible = settings.isShowHat() && rig.head.visible;
            rig.jacket.visible = settings.isShowJacket() && rig.body.visible;
            rig.rightSleeve.visible = settings.isShowRightSleeve() && rig.rightArm.visible;
            rig.rightForearmSleeve.visible = settings.isShowRightSleeve() && rig.rightArm.visible;
            rig.rightForearm.visible = rig.rightArm.visible;
            rig.leftSleeve.visible = settings.isShowLeftSleeve() && rig.leftArm.visible;
            rig.leftForearmSleeve.visible = settings.isShowLeftSleeve() && rig.leftArm.visible;
            rig.leftForearm.visible = rig.leftArm.visible;
            rig.rightPants.visible = settings.isShowRightPants() && rig.rightLeg.visible;
            rig.rightLowerPants.visible = settings.isShowRightPants() && rig.rightLeg.visible;
            rig.rightLowerLeg.visible = rig.rightLeg.visible;
            rig.leftPants.visible = settings.isShowLeftPants() && rig.leftLeg.visible;
            rig.leftLowerPants.visible = settings.isShowLeftPants() && rig.leftLeg.visible;
            rig.leftLowerLeg.visible = rig.leftLeg.visible;

            poseStack.pushPose();

            if (positions != null && positions.containsKey("root")) {
                float[] rp = positions.get("root");
                if (rp != null && rp.length >= 3) {
                    poseStack.translate(rp[0], rp[1], rp[2]);
                }
            }

            if (settings != null && settings.getRootYaw() != 0.0f) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(settings.getRootYaw()));
            }

            if (settings != null && settings.getScale() != 1.0f) {
                float s = settings.getScale();
                poseStack.scale(s, s, s);
            }

            if (rotations != null && rotations.containsKey("root")) {
                float[] rr = rotations.get("root");
                if (rr != null && rr.length >= 3) {
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotation(rr[1]));
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotation(rr[0]));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(rr[2]));
                }
            }

            poseStack.scale(-1.0f, -1.0f, 1.0f);
            poseStack.translate(0.0f, -1.501f, 0.0f);
            var vertexConsumer = buffers.getBuffer(RenderType.entityCutoutNoCull(skin));

            rig.root.render(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);

            if (settings.isAnimatedEyes()) {
                poseStack.pushPose();
                rig.body.translateAndRotate(poseStack);
                renderAnimatedEyes(poseStack, buffers, rig.head, settings, slim);
                poseStack.popPose();
            }

            if (selectedBone != null && (settings == null || !settings.isBoneGhosted(selectedBone))) {
                renderBendableBoneHighlight(poseStack, buffers, rig, selectedBone, slim);
            }

            if (settings != null && !settings.getRightHandItem().isEmpty()) {
                poseStack.pushPose();

                rig.rightArm.translateAndRotate(poseStack);

                rig.rightForearm.translateAndRotate(poseStack);

                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0f));

                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f));

                poseStack.translate(1.0f / 16.0f, 0.125f, -0.625f);

                Minecraft.getInstance().getItemRenderer().renderStatic(
                        settings.getRightHandItem(),

                        net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,

                        light,

                        OverlayTexture.NO_OVERLAY,

                        poseStack,

                        buffers,

                        Minecraft.getInstance().level,

                        0
                );

                poseStack.popPose();
            }

            if (settings != null && !settings.getLeftHandItem().isEmpty()) {
                poseStack.pushPose();

                rig.leftArm.translateAndRotate(poseStack);

                rig.leftForearm.translateAndRotate(poseStack);

                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0f));

                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f));

                poseStack.translate(-1.0f / 16.0f, 0.125f, -0.625f);

                Minecraft.getInstance().getItemRenderer().renderStatic(
                        settings.getLeftHandItem(),

                        net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND,

                        light,

                        OverlayTexture.NO_OVERLAY,

                        poseStack,

                        buffers,

                        Minecraft.getInstance().level,

                        0
                );

                poseStack.popPose();
            }

            poseStack.popPose();

            return;
        }

        PlayerModel model = getOrCreateModel(slim);
        model.young = false;

        model.body.resetPose();

        model.head.resetPose();

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

        if (rotations != null) {
            applyRotation(model.body, rotations.get("body"));

            applyRotation(model.head, rotations.get("head"));

            applyRotation(model.rightArm, rotations.get("right_arm"));

            applyRotation(model.leftArm, rotations.get("left_arm"));

            applyRotation(model.rightLeg, rotations.get("right_leg"));

            applyRotation(model.leftLeg, rotations.get("left_leg"));

            if (model.body.xRot != 0.0f) {
                float hipZ = (float) (Math.sin(model.body.xRot) * 12.0);
                float hipY = (float) ((1.0 - Math.cos(model.body.xRot)) * 12.0);
                model.rightLeg.z += hipZ;
                model.rightLeg.y -= hipY;
                model.leftLeg.z += hipZ;
                model.leftLeg.y -= hipY;
            }
        }

        float[] bodyPos = (positions != null) ? positions.get("body") : null;

        if (positions != null) {
            applyPosition(model.body, bodyPos);

            applyPosition(model.head, positions.get("head"));

            applyPosition(model.rightArm, positions.get("right_arm"));

            applyPosition(model.leftArm, positions.get("left_arm"));

            applyPosition(model.rightLeg, positions.get("right_leg"));

            applyPosition(model.leftLeg, positions.get("left_leg"));
        }

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

        if (settings != null) {
            model.hat.visible = settings.isShowHat() && model.head.visible;
            model.jacket.visible = settings.isShowJacket() && model.body.visible;
            model.rightSleeve.visible = settings.isShowRightSleeve() && model.rightArm.visible;
            model.leftSleeve.visible = settings.isShowLeftSleeve() && model.leftArm.visible;
            model.rightPants.visible = settings.isShowRightPants() && model.rightLeg.visible;
            model.leftPants.visible = settings.isShowLeftPants() && model.leftLeg.visible;
        }

        poseStack.pushPose();

        if (positions != null && positions.containsKey("root")) {
            float[] rp = positions.get("root");
            if (rp != null && rp.length >= 3) {
                poseStack.translate(rp[0], rp[1], rp[2]);
            }
        }

        if (settings != null && settings.getRootYaw() != 0.0f) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(settings.getRootYaw()));
        }

        if (settings != null && settings.getScale() != 1.0f) {
            float s = settings.getScale();
            poseStack.scale(s, s, s);
        }

        if (rotations != null && rotations.containsKey("root")) {
            float[] rr = rotations.get("root");
            if (rr != null && rr.length >= 3) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotation(rr[1]));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotation(rr[0]));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(rr[2]));
            }
        }

        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0f, -1.501f, 0.0f);

        boolean headGhost = (settings != null && settings.isBoneGhosted("head"));
        boolean bodyGhost = (settings != null && (settings.isBoneGhosted("body") || settings.isBoneGhosted("root")));
        boolean rightArmGhost = (settings != null && settings.isBoneGhosted("right_arm"));
        boolean leftArmGhost = (settings != null && settings.isBoneGhosted("left_arm"));
        boolean rightLegGhost = (settings != null && settings.isBoneGhosted("right_leg"));
        boolean leftLegGhost = (settings != null && settings.isBoneGhosted("left_leg"));
        boolean hasAnyGhost = headGhost || bodyGhost || rightArmGhost || leftArmGhost || rightLegGhost || leftLegGhost;

        if (hasAnyGhost) {
            boolean origHead = model.head.visible;
            boolean origHat = model.hat.visible;
            boolean origBody = model.body.visible;
            boolean origJacket = model.jacket.visible;
            boolean origRightArm = model.rightArm.visible;
            boolean origRightSleeve = model.rightSleeve.visible;
            boolean origLeftArm = model.leftArm.visible;
            boolean origLeftSleeve = model.leftSleeve.visible;
            boolean origRightLeg = model.rightLeg.visible;
            boolean origRightPants = model.rightPants.visible;
            boolean origLeftLeg = model.leftLeg.visible;
            boolean origLeftPants = model.leftPants.visible;

            if (headGhost) { model.head.visible = false; model.hat.visible = false; }

            if (bodyGhost) { model.body.visible = false; model.jacket.visible = false; }

            if (rightArmGhost) { model.rightArm.visible = false; model.rightSleeve.visible = false; }

            if (leftArmGhost) { model.leftArm.visible = false; model.leftSleeve.visible = false; }

            if (rightLegGhost) { model.rightLeg.visible = false; model.rightPants.visible = false; }

            if (leftLegGhost) { model.leftLeg.visible = false; model.leftPants.visible = false; }

            var vertexConsumer = buffers.getBuffer(RenderType.entityCutoutNoCull(skin));

            renderPlayerHierarchy(poseStack, model, vertexConsumer, light, 0xFFFFFFFF);
            var transConsumer = buffers.getBuffer(RenderType.entityTranslucent(skin));
            int ghostCol = 0x55FFFFFF;

            if (headGhost) {
                if (origHead) model.head.render(poseStack, transConsumer, light, OverlayTexture.NO_OVERLAY, ghostCol);

                if (origHat) model.hat.render(poseStack, transConsumer, light, OverlayTexture.NO_OVERLAY, ghostCol);
            }

            if (bodyGhost) {
                if (origBody) model.body.render(poseStack, transConsumer, light, OverlayTexture.NO_OVERLAY, ghostCol);

                if (origJacket) model.jacket.render(poseStack, transConsumer, light, OverlayTexture.NO_OVERLAY, ghostCol);
            }

            if (rightArmGhost) {
                if (origRightArm) model.rightArm.render(poseStack, transConsumer, light, OverlayTexture.NO_OVERLAY, ghostCol);

                if (origRightSleeve) model.rightSleeve.render(poseStack, transConsumer, light, OverlayTexture.NO_OVERLAY, ghostCol);
            }

            if (leftArmGhost) {
                if (origLeftArm) model.leftArm.render(poseStack, transConsumer, light, OverlayTexture.NO_OVERLAY, ghostCol);

                if (origLeftSleeve) model.leftSleeve.render(poseStack, transConsumer, light, OverlayTexture.NO_OVERLAY, ghostCol);
            }

            if (rightLegGhost) {
                if (origRightLeg) model.rightLeg.render(poseStack, transConsumer, light, OverlayTexture.NO_OVERLAY, ghostCol);

                if (origRightPants) model.rightPants.render(poseStack, transConsumer, light, OverlayTexture.NO_OVERLAY, ghostCol);
            }

            if (leftLegGhost) {
                if (origLeftLeg) model.leftLeg.render(poseStack, transConsumer, light, OverlayTexture.NO_OVERLAY, ghostCol);

                if (origLeftPants) model.leftPants.render(poseStack, transConsumer, light, OverlayTexture.NO_OVERLAY, ghostCol);
            }

            model.head.visible = origHead;
            model.hat.visible = origHat;
            model.body.visible = origBody;
            model.jacket.visible = origJacket;
            model.rightArm.visible = origRightArm;
            model.rightSleeve.visible = origRightSleeve;
            model.leftArm.visible = origLeftArm;
            model.leftSleeve.visible = origLeftSleeve;
            model.rightLeg.visible = origRightLeg;
            model.rightPants.visible = origRightPants;
            model.leftLeg.visible = origLeftLeg;
            model.leftPants.visible = origLeftPants;
        } else {
            var vertexConsumer = buffers.getBuffer(RenderType.entityCutoutNoCull(skin));

            renderPlayerHierarchy(poseStack, model, vertexConsumer, light, 0xFFFFFFFF);
        }

        if (settings != null && !settings.getRightHandItem().isEmpty()) {
            poseStack.pushPose();

            model.rightArm.translateAndRotate(poseStack);

            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0f));

            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f));

            poseStack.translate(1.0f / 16.0f, 0.125f, -0.625f);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    settings.getRightHandItem(),

                    net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,

                    light,

                    OverlayTexture.NO_OVERLAY,

                    poseStack,

                    buffers,

                    Minecraft.getInstance().level,

                    0
            );

            poseStack.popPose();
        }

        if (settings != null && !settings.getLeftHandItem().isEmpty()) {
            poseStack.pushPose();

            model.leftArm.translateAndRotate(poseStack);

            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0f));

            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f));

            poseStack.translate(-1.0f / 16.0f, 0.125f, -0.625f);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    settings.getLeftHandItem(),

                    net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND,

                    light,

                    OverlayTexture.NO_OVERLAY,

                    poseStack,

                    buffers,

                    Minecraft.getInstance().level,

                    0
            );

            poseStack.popPose();
        }

        if (settings != null && settings.isAnimatedEyes()) {
            poseStack.pushPose();
            renderAnimatedEyes(poseStack, buffers, model.head, settings, slim);
            poseStack.popPose();
        }

        if (selectedBone != null && (settings == null || !settings.isBoneGhosted(selectedBone))) {
            renderBoneHighlight(poseStack, buffers, model, selectedBone, slim);
        }

        poseStack.popPose();
    }

    public static void renderAnimatedPlayerModel(
            PoseStack poseStack,

            MultiBufferSource buffers,

            int light,

            float[] bodyRot,

            float[] headRot,

            float[] rightArmRot,

            float[] leftArmRot,

            float[] rightLegRot,

            float[] leftLegRot,

            boolean slim,

            ResourceLocation skin,

            net.openstage.client.model.ModelSettings settings,

            String selectedBone
    ) {
        Map<String, float[]> rots = new HashMap<>();

        if (bodyRot != null) rots.put("body", bodyRot);

        if (headRot != null) rots.put("head", headRot);

        if (rightArmRot != null) rots.put("right_arm", rightArmRot);

        if (leftArmRot != null) rots.put("left_arm", leftArmRot);

        if (rightLegRot != null) rots.put("right_leg", rightLegRot);

        if (leftLegRot != null) rots.put("left_leg", leftLegRot);

        renderAnimatedPlayerModel(poseStack, buffers, light, rots, slim, skin, settings, selectedBone);
    }

    private static void renderPlayerHierarchy(PoseStack poseStack, PlayerModel model, VertexConsumer consumer, int light, int color) {
        if (model.head.visible) model.head.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (model.hat.visible) model.hat.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (model.body.visible) model.body.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (model.jacket.visible) model.jacket.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (model.rightArm.visible) model.rightArm.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (model.rightSleeve.visible) model.rightSleeve.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (model.leftArm.visible) model.leftArm.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (model.leftSleeve.visible) model.leftSleeve.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (model.rightLeg.visible) model.rightLeg.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (model.rightPants.visible) model.rightPants.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (model.leftLeg.visible) model.leftLeg.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (model.leftPants.visible) model.leftPants.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
    }

        private static void renderBoneHighlight(PoseStack poseStack, MultiBufferSource buffers,
                                            PlayerModel model, String selectedBone, boolean slim) {
        switch (selectedBone) {
            case "root" -> {
                renderLimbHighlight(poseStack, buffers, model.body, -4.02f, -0.02f, -2.02f, 4.02f, 12.02f, 2.02f);
                poseStack.pushPose();
                model.body.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, model.head, -4.02f, -8.02f, -4.02f, 4.02f, 0.02f, 4.02f);
                float armMinX = slim ? -2.02f : -3.02f;
                float armMaxX = slim ? 2.02f : 3.02f;
                renderLimbHighlight(poseStack, buffers, model.rightArm, armMinX, -2.02f, -2.02f, 1.02f, 10.02f, 2.02f);
                renderLimbHighlight(poseStack, buffers, model.leftArm, -1.02f, -2.02f, -2.02f, armMaxX, 10.02f, 2.02f);
                poseStack.popPose();
                renderLimbHighlight(poseStack, buffers, model.rightLeg, -2.02f, -0.02f, -2.02f, 2.02f, 12.02f, 2.02f);
                renderLimbHighlight(poseStack, buffers, model.leftLeg, -2.02f, -0.02f, -2.02f, 2.02f, 12.02f, 2.02f);
            }
            case "head" -> {
                poseStack.pushPose();
                model.body.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, model.head, -4.08f, -8.08f, -4.08f, 4.08f, 0.08f, 4.08f);
                poseStack.popPose();
            }
            case "body" -> renderLimbHighlight(poseStack, buffers, model.body, -4.08f, -0.08f, -2.08f, 4.08f, 12.08f, 2.08f);
            case "right_arm" -> {
                float minX = slim ? -2.08f : -3.08f;
                poseStack.pushPose();
                model.body.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, model.rightArm, minX, -2.08f, -2.08f, 1.08f, 10.08f, 2.08f);
                poseStack.popPose();
            }
            case "left_arm" -> {
                float maxX = slim ? 2.08f : 3.08f;
                poseStack.pushPose();
                model.body.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, model.leftArm, -1.08f, -2.08f, -2.08f, maxX, 10.08f, 2.08f);
                poseStack.popPose();
            }
            case "right_leg" -> renderLimbHighlight(poseStack, buffers, model.rightLeg, -2.08f, -0.08f, -2.08f, 2.08f, 12.08f, 2.08f);
            case "left_leg" -> renderLimbHighlight(poseStack, buffers, model.leftLeg, -2.08f, -0.08f, -2.08f, 2.08f, 12.08f, 2.08f);
        }
    }

    private static void renderBendableBoneHighlight(PoseStack poseStack, MultiBufferSource buffers,
                                                    BendableRig rig, String selectedBone, boolean slim) {
        float armMinX = slim ? -2.08f : -3.08f;
        float armMaxX = slim ? 2.08f : 3.08f;

        switch (selectedBone) {
            case "root" -> {
                renderLimbHighlight(poseStack, buffers, rig.body, -4.02f, -0.02f, -2.02f, 4.02f, 12.02f, 2.02f);
                poseStack.pushPose();
                rig.body.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, rig.head, -4.02f, -8.02f, -4.02f, 4.02f, 0.02f, 4.02f);
                renderLimbHighlight(poseStack, buffers, rig.rightArm, armMinX, -2.02f, -2.02f, 1.02f, 4.02f, 2.02f);
                poseStack.pushPose();
                rig.rightArm.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, rig.rightForearm, armMinX, -0.02f, -2.02f, 1.02f, 6.02f, 2.02f);
                poseStack.popPose();
                renderLimbHighlight(poseStack, buffers, rig.leftArm, -1.02f, -2.02f, -2.02f, armMaxX, 4.02f, 2.02f);
                poseStack.pushPose();
                rig.leftArm.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, rig.leftForearm, -1.02f, -0.02f, -2.02f, armMaxX, 6.02f, 2.02f);
                poseStack.popPose();
                poseStack.popPose();

                renderLimbHighlight(poseStack, buffers, rig.rightLeg, -2.02f, -0.02f, -2.02f, 2.02f, 6.02f, 2.02f);
                poseStack.pushPose();
                rig.rightLeg.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, rig.rightLowerLeg, -2.02f, -0.02f, -2.02f, 2.02f, 6.02f, 2.02f);
                poseStack.popPose();
                renderLimbHighlight(poseStack, buffers, rig.leftLeg, -2.02f, -0.02f, -2.02f, 2.02f, 6.02f, 2.02f);
                poseStack.pushPose();
                rig.leftLeg.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, rig.leftLowerLeg, -2.02f, -0.02f, -2.02f, 2.02f, 6.02f, 2.02f);
                poseStack.popPose();
            }
            case "head" -> {
                poseStack.pushPose();
                rig.body.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, rig.head, -4.08f, -8.08f, -4.08f, 4.08f, 0.08f, 4.08f);
                poseStack.popPose();
            }
            case "body" -> renderLimbHighlight(poseStack, buffers, rig.body, -4.08f, -0.08f, -2.08f, 4.08f, 12.08f, 2.08f);
            case "right_arm" -> {
                poseStack.pushPose();
                rig.body.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, rig.rightArm, armMinX, -2.08f, -2.08f, 1.08f, 4.08f, 2.08f);
                poseStack.popPose();
            }
            case "right_forearm" -> {
                poseStack.pushPose();
                rig.body.translateAndRotate(poseStack);
                rig.rightArm.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, rig.rightForearm, armMinX, -0.08f, -2.08f, 1.08f, 6.08f, 2.08f);
                poseStack.popPose();
            }
            case "left_arm" -> {
                poseStack.pushPose();
                rig.body.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, rig.leftArm, -1.08f, -2.08f, -2.08f, armMaxX, 4.08f, 2.08f);
                poseStack.popPose();
            }
            case "left_forearm" -> {
                poseStack.pushPose();
                rig.body.translateAndRotate(poseStack);
                rig.leftArm.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, rig.leftForearm, -1.08f, -0.08f, -2.08f, armMaxX, 6.08f, 2.08f);
                poseStack.popPose();
            }
            case "right_leg" -> renderLimbHighlight(poseStack, buffers, rig.rightLeg, -2.08f, -0.08f, -2.08f, 2.08f, 6.08f, 2.08f);
            case "right_lower_leg" -> {
                poseStack.pushPose();
                rig.rightLeg.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, rig.rightLowerLeg, -2.08f, -0.08f, -2.08f, 2.08f, 6.08f, 2.08f);
                poseStack.popPose();
            }
            case "left_leg" -> renderLimbHighlight(poseStack, buffers, rig.leftLeg, -2.08f, -0.08f, -2.08f, 2.08f, 6.08f, 2.08f);
            case "left_lower_leg" -> {
                poseStack.pushPose();
                rig.leftLeg.translateAndRotate(poseStack);
                renderLimbHighlight(poseStack, buffers, rig.leftLowerLeg, -2.08f, -0.08f, -2.08f, 2.08f, 6.08f, 2.08f);
                poseStack.popPose();
            }
        }
    }

    private static void renderLimbHighlight(PoseStack poseStack, MultiBufferSource buffers,

                                            net.minecraft.client.model.geom.ModelPart part,

                                            float minX, float minY, float minZ,

                                            float maxX, float maxY, float maxZ) {
        poseStack.pushPose();

        part.translateAndRotate(poseStack);

        poseStack.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
        float pad = 0.08f;
        minX -= pad; minY -= pad; minZ -= pad;
        maxX += pad; maxY += pad; maxZ += pad;
        com.mojang.blaze3d.vertex.VertexConsumer consumer = buffers.getBuffer(RenderType.gui());
        org.joml.Matrix4f mat = poseStack.last().pose();
        int tint = 0x33FFCC00;
        int edge = 0xFFFFCC00;

        drawBoxFaces(consumer, mat, minX, minY, minZ, maxX, maxY, maxZ, tint);

        drawBoxEdges(consumer, mat, minX, minY, minZ, maxX, maxY, maxZ, edge, 0.18f);
        if (buffers instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.gui());
        }

        poseStack.popPose();
    }

    private static void drawBoxFaces(com.mojang.blaze3d.vertex.VertexConsumer consumer,

                                     org.joml.Matrix4f mat,

                                     float minX, float minY, float minZ,

                                     float maxX, float maxY, float maxZ, int color) {
        drawTwoSidedQuad(consumer, mat, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, color);

        drawTwoSidedQuad(consumer, mat, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);

        drawTwoSidedQuad(consumer, mat, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, color);

        drawTwoSidedQuad(consumer, mat, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);

        drawTwoSidedQuad(consumer, mat, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, color);

        drawTwoSidedQuad(consumer, mat, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
    }

    private static void drawTwoSidedQuad(com.mojang.blaze3d.vertex.VertexConsumer consumer, org.joml.Matrix4f mat,

                                         float x0, float y0, float z0,

                                         float x1, float y1, float z1,

                                         float x2, float y2, float z2,

                                         float x3, float y3, float z3, int color) {
        consumer.addVertex(mat, x0, y0, z0).setColor(color);

        consumer.addVertex(mat, x1, y1, z1).setColor(color);

        consumer.addVertex(mat, x2, y2, z2).setColor(color);

        consumer.addVertex(mat, x3, y3, z3).setColor(color);

        consumer.addVertex(mat, x3, y3, z3).setColor(color);

        consumer.addVertex(mat, x2, y2, z2).setColor(color);

        consumer.addVertex(mat, x1, y1, z1).setColor(color);

        consumer.addVertex(mat, x0, y0, z0).setColor(color);
    }

    private static void drawBoxEdges(com.mojang.blaze3d.vertex.VertexConsumer consumer,

                                     org.joml.Matrix4f mat,

                                     float minX, float minY, float minZ,

                                     float maxX, float maxY, float maxZ, int color, float t) {
        drawEdgeX(consumer, mat, minX, maxX, minY, minZ, t, color);

        drawEdgeX(consumer, mat, minX, maxX, maxY, minZ, t, color);

        drawEdgeX(consumer, mat, minX, maxX, minY, maxZ, t, color);

        drawEdgeX(consumer, mat, minX, maxX, maxY, maxZ, t, color);

        drawEdgeY(consumer, mat, minX, minY, maxY, minZ, t, color);

        drawEdgeY(consumer, mat, maxX, minY, maxY, minZ, t, color);

        drawEdgeY(consumer, mat, minX, minY, maxY, maxZ, t, color);

        drawEdgeY(consumer, mat, maxX, minY, maxY, maxZ, t, color);

        drawEdgeZ(consumer, mat, minX, minY, minZ, maxZ, t, color);

        drawEdgeZ(consumer, mat, maxX, minY, minZ, maxZ, t, color);

        drawEdgeZ(consumer, mat, minX, maxY, minZ, maxZ, t, color);

        drawEdgeZ(consumer, mat, maxX, maxY, minZ, maxZ, t, color);
    }

    private static void drawEdgeX(com.mojang.blaze3d.vertex.VertexConsumer consumer, org.joml.Matrix4f mat,

                                  float x0, float x1, float y, float z, float t, int color) {
        float xMin = Math.min(x0, x1);
        float xMax = Math.max(x0, x1);

        drawTwoSidedQuad(consumer, mat, xMin, y - t, z - t, xMax, y - t, z - t, xMax, y + t, z - t, xMin, y + t, z - t, color);

        drawTwoSidedQuad(consumer, mat, xMin, y - t, z + t, xMax, y - t, z + t, xMax, y + t, z + t, xMin, y + t, z + t, color);

        drawTwoSidedQuad(consumer, mat, xMin, y - t, z - t, xMax, y - t, z - t, xMax, y - t, z + t, xMin, y - t, z + t, color);

        drawTwoSidedQuad(consumer, mat, xMin, y + t, z - t, xMax, y + t, z - t, xMax, y + t, z + t, xMin, y + t, z + t, color);
    }

    private static void drawEdgeY(com.mojang.blaze3d.vertex.VertexConsumer consumer, org.joml.Matrix4f mat,

                                  float x, float y0, float y1, float z, float t, int color) {
        float yMin = Math.min(y0, y1);
        float yMax = Math.max(y0, y1);

        drawTwoSidedQuad(consumer, mat, x - t, yMin, z - t, x + t, yMin, z - t, x + t, yMax, z - t, x - t, yMax, z - t, color);

        drawTwoSidedQuad(consumer, mat, x - t, yMin, z + t, x + t, yMin, z + t, x + t, yMax, z + t, x - t, yMax, z + t, color);

        drawTwoSidedQuad(consumer, mat, x - t, yMin, z - t, x - t, yMin, z + t, x - t, yMax, z + t, x - t, yMax, z - t, color);

        drawTwoSidedQuad(consumer, mat, x + t, yMin, z - t, x + t, yMin, z + t, x + t, yMax, z + t, x + t, yMax, z - t, color);
    }

    private static void drawEdgeZ(com.mojang.blaze3d.vertex.VertexConsumer consumer, org.joml.Matrix4f mat,

                                  float x, float y, float z0, float z1, float t, int color) {
        float zMin = Math.min(z0, z1);
        float zMax = Math.max(z0, z1);

        drawTwoSidedQuad(consumer, mat, x - t, y - t, zMin, x + t, y - t, zMin, x + t, y - t, zMax, x - t, y - t, zMax, color);

        drawTwoSidedQuad(consumer, mat, x - t, y + t, zMin, x + t, y + t, zMin, x + t, y + t, zMax, x - t, y + t, zMax, color);

        drawTwoSidedQuad(consumer, mat, x - t, y - t, zMin, x - t, y + t, zMin, x - t, y + t, zMax, x - t, y - t, zMax, color);

        drawTwoSidedQuad(consumer, mat, x + t, y - t, zMin, x + t, y + t, zMin, x + t, y + t, zMax, x + t, y - t, zMax, color);
    }

    private static void renderAnimatedEyes(PoseStack poseStack, MultiBufferSource buffers,
                                          net.minecraft.client.model.geom.ModelPart head,
                                          net.openstage.client.model.ModelSettings settings,
                                          boolean slim) {
        poseStack.pushPose();
        head.translateAndRotate(poseStack);
        poseStack.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
        ResourceLocation eyeTex = slim ? BBS_EYES_ALEX : BBS_EYES_STEVE;
        var consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(eyeTex));
        org.joml.Matrix4f mat = poseStack.last().pose();
        int light = 15728880;
        float pupilX = settings.getPupilX();
        float pupilY = settings.getPupilY();
        float leftEyelid = settings.getLeftEyelid();
        float rightEyelid = settings.getRightEyelid();

        drawTexturedQuad(consumer, mat, -3.0f, -4.0f, -1.0f, -2.0f, -4.010f, 1.0f / 16.0f, 14.0f / 16.0f, 3.0f / 16.0f, 16.0f / 16.0f, light);
        drawTexturedQuad(consumer, mat, 1.0f, -4.0f, 3.0f, -2.0f, -4.010f, 7.0f / 16.0f, 14.0f / 16.0f, 9.0f / 16.0f, 16.0f / 16.0f, light);

        drawTexturedQuad(consumer, mat, -3.0f, -4.0f, -1.0f, -2.0f, -4.015f, 1.0f / 16.0f, 4.0f / 16.0f, 3.0f / 16.0f, 6.0f / 16.0f, light);

        drawTexturedQuad(consumer, mat, 1.0f, -4.0f, 3.0f, -2.0f, -4.015f, 7.0f / 16.0f, 4.0f / 16.0f, 9.0f / 16.0f, 6.0f / 16.0f, light);
        float pxOff = Math.max(-0.55f, Math.min(0.55f, pupilX * 0.45f));
        float pyOff = Math.max(-0.45f, Math.min(0.45f, pupilY * 0.35f));
        float prX0 = Math.max(-3.0f, Math.min(-2.0f, -2.0f + pxOff));
        float prX1 = prX0 + 1.0f;
        float prY0 = Math.max(-4.0f, Math.min(-3.0f, -4.0f + pyOff));
        float prY1 = prY0 + 2.0f;
        drawTexturedQuad(consumer, mat, prX0, prY0, prX1, prY1, -4.025f, 1.0f / 16.0f, 1.0f / 16.0f, 2.0f / 16.0f, 3.0f / 16.0f, light);
        float plX0 = Math.max(1.0f, Math.min(2.0f, 1.0f + pxOff));
        float plX1 = plX0 + 1.0f;
        float plY0 = Math.max(-4.0f, Math.min(-3.0f, -4.0f + pyOff));
        float plY1 = plY0 + 2.0f;
        drawTexturedQuad(consumer, mat, plX0, plY0, plX1, plY1, -4.025f, 7.0f / 16.0f, 1.0f / 16.0f, 8.0f / 16.0f, 3.0f / 16.0f, light);

        if (rightEyelid > 0.02f) {
            float rLid = Math.min(1.0f, rightEyelid);
            float topH = 1.3f * rLid;
            drawTexturedQuad(consumer, mat, -3.0f, -4.0f, -1.0f, -4.0f + topH, -4.035f, 1.0f / 16.0f, 11.0f / 16.0f, 3.0f / 16.0f, 12.0f / 16.0f, light);
            float botH = 0.9f * rLid;
            drawTexturedQuad(consumer, mat, -3.0f, -2.0f - botH, -1.0f, -2.0f, -4.035f, 1.0f / 16.0f, 13.0f / 16.0f, 3.0f / 16.0f, 14.0f / 16.0f, light);
        }
        if (leftEyelid > 0.02f) {
            float lLid = Math.min(1.0f, leftEyelid);
            float topH = 1.3f * lLid;
            drawTexturedQuad(consumer, mat, 1.0f, -4.0f, 3.0f, -4.0f + topH, -4.035f, 7.0f / 16.0f, 11.0f / 16.0f, 9.0f / 16.0f, 12.0f / 16.0f, light);
            float botH = 0.9f * lLid;
            drawTexturedQuad(consumer, mat, 1.0f, -2.0f - botH, 3.0f, -2.0f, -4.035f, 7.0f / 16.0f, 13.0f / 16.0f, 9.0f / 16.0f, 14.0f / 16.0f, light);
        }

        drawTexturedQuad(consumer, mat, -3.0f, -5.0f, -1.0f, -4.0f, -4.045f, 1.0f / 16.0f, 7.0f / 16.0f, 3.0f / 16.0f, 8.0f / 16.0f, light);
        drawTexturedQuad(consumer, mat, 1.0f, -5.0f, 3.0f, -4.0f, -4.045f, 7.0f / 16.0f, 7.0f / 16.0f, 9.0f / 16.0f, 8.0f / 16.0f, light);
        drawTexturedQuad(consumer, mat, -4.0f, -4.0f, -3.0f, -3.0f, -4.045f, 7.0f / 16.0f, 9.0f / 16.0f, 8.0f / 16.0f, 10.0f / 16.0f, light);
        drawTexturedQuad(consumer, mat, 3.0f, -4.0f, 4.0f, -3.0f, -4.045f, 1.0f / 16.0f, 9.0f / 16.0f, 2.0f / 16.0f, 10.0f / 16.0f, light);

        poseStack.popPose();
    }

    private static void drawTexturedQuad(com.mojang.blaze3d.vertex.VertexConsumer consumer,

                                         org.joml.Matrix4f mat,

                                         float x0, float y0, float x1, float y1, float z,

                                         float u0, float v0, float u1, float v1, int light) {
        consumer.addVertex(mat, x0, y0, z).setColor(0xFFFFFFFF).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);

        consumer.addVertex(mat, x0, y1, z).setColor(0xFFFFFFFF).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);

        consumer.addVertex(mat, x1, y1, z).setColor(0xFFFFFFFF).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);

        consumer.addVertex(mat, x1, y0, z).setColor(0xFFFFFFFF).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
    }

    private static void applyRotation(net.minecraft.client.model.geom.ModelPart part, float[] rot) {
        if (rot == null || rot.length < 3) return;
        part.xRot = rot[0];
        part.yRot = rot[1];
        part.zRot = rot[2];
    }

    private static void applyPosition(net.minecraft.client.model.geom.ModelPart part, float[] pos) {
        applyPositionWithParent(part, pos, null);
    }

    private static void applyPositionWithParent(net.minecraft.client.model.geom.ModelPart part, float[] pos, float[] parentPos) {
        if (part == null) return;
        float px = (pos != null && pos.length >= 3) ? pos[0] : 0.0f;
        float py = (pos != null && pos.length >= 3) ? pos[1] : 0.0f;
        float pz = (pos != null && pos.length >= 3) ? pos[2] : 0.0f;

        if (parentPos != null && parentPos.length >= 3) {
            px += parentPos[0];
            py += parentPos[1];
            pz += parentPos[2];
        }

        if (px == 0.0f && py == 0.0f && pz == 0.0f) return;

        if (Math.abs(py) > 50.0f || Math.abs(px) > 50.0f || Math.abs(pz) > 50.0f) {
            part.visible = false;

            return;
        }

        part.x -= px * 16.0f;
        part.y -= py * 16.0f;
        part.z += pz * 16.0f;
    }

    public static void renderObjModel(PoseStack poseStack, MultiBufferSource buffers, int light,

                                      net.openstage.client.model.ObjModel model,

                                      Map<String, float[]> rotations, Map<String, float[]> positions,

                                      net.openstage.client.model.ModelSettings settings, String selectedBone) {
        if (model == null) return;

        poseStack.pushPose();
        float yOffset = (model.getMinY() < 0.0f) ? -model.getMinY() : 0.0f;
        if (yOffset != 0.0f) {
            poseStack.translate(0.0f, yOffset, 0.0f);
        }

        float px = 0.0f, py = 0.0f, pz = 0.0f;

        if (positions != null) {
            float[] rp = positions.get("root");

            if (rp != null && rp.length >= 3) {
                px += rp[0]; py += rp[1]; pz += rp[2];
            }

            float[] bp = positions.get("body");

            if (bp != null && bp.length >= 3) {
                px += bp[0]; py += bp[1]; pz += bp[2];
            }

            if (selectedBone != null && !"root".equals(selectedBone) && !"body".equals(selectedBone)) {
                float[] sp = positions.get(selectedBone);

                if (sp != null && sp.length >= 3) {
                    px += sp[0]; py += sp[1]; pz += sp[2];
                }
            }
        }

        if (px != 0.0f || py != 0.0f || pz != 0.0f) {
            poseStack.translate(px, py, pz);
        }

        float rx = 0.0f, ry = 0.0f, rz = 0.0f;

        if (rotations != null) {
            float[] rr = rotations.get("root");

            if (rr != null && rr.length >= 3) {
                rx += rr[0]; ry += rr[1]; rz += rr[2];
            }

            float[] br = rotations.get("body");

            if (br != null && br.length >= 3) {
                rx += br[0]; ry += br[1]; rz += br[2];
            }

            if (selectedBone != null && !"root".equals(selectedBone) && !"body".equals(selectedBone)) {
                float[] sr = rotations.get(selectedBone);

                if (sr != null && sr.length >= 3) {
                    rx += sr[0]; ry += sr[1]; rz += sr[2];
                }
            }
        }

        if (rx != 0.0f || ry != 0.0f || rz != 0.0f) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotation(ry));

            poseStack.mulPose(com.mojang.math.Axis.XP.rotation(rx));

            poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(rz));
        }

        if (settings != null) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(settings.getRootYaw()));
            float s = settings.getScale();

            poseStack.scale(s, s, s);
        }

        model.render(poseStack, buffers, light, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);

                if (selectedBone != null) {
            if (buffers instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource bs) {
                bs.endBatch();
            }
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            com.mojang.blaze3d.vertex.VertexConsumer consumer = buffers.getBuffer(RenderType.gui());
            org.joml.Matrix4f mat = poseStack.last().pose();
            int tint = 0x25FFCC00;
            int edge = 0xFFFFCC00;
            float pad = 0.04f;
            float minX = model.getMinX() - pad;
            float maxX = model.getMaxX() + pad;
            float minY = model.getMinY() - pad;
            float maxY = model.getMaxY() + pad;
            float minZ = model.getMinZ() - pad;
            float maxZ = model.getMaxZ() + pad;

            drawBoxFaces(consumer, mat, minX, minY, minZ, maxX, maxY, maxZ, tint);
            drawBoxEdges(consumer, mat, minX, minY, minZ, maxX, maxY, maxZ, edge, 0.045f);
            if (buffers instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource bs) {
                bs.endBatch(RenderType.gui());
            }
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }

        poseStack.popPose();
    }

    public static void invalidateModels() {
        wideModel = null;
        slimModel = null;
        bendableWideRig = null;
        bendableSlimRig = null;
    }
}

