package net.openstage.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.openstage.client.cinematic.CinematicPlaybackHandler;
import net.openstage.client.cinematic.VideoExportService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void openstage$overrideCutsceneFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        CinematicPlaybackHandler handler = CinematicPlaybackHandler.getInstance();
        if (handler.isPlaying()) {
            float fov = handler.getCurrentFov();
            if (fov > 0.0f) {
                cir.setReturnValue((double) fov);
            }
        }
    }

    @Inject(method = "bobHurt", at = @At("TAIL"))
    private void openstage$applyCameraRoll(PoseStack poseStack, float tickDelta, CallbackInfo ci) {
        CinematicPlaybackHandler handler = CinematicPlaybackHandler.getInstance();
        if (handler.isPlaying()) {
            float roll = handler.getCurrentRoll();
            if (Math.abs(roll) > 0.001f) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
            }
        }
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void openstage$captureExportFrame(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci) {
        VideoExportService svc = VideoExportService.getInstance();
        if (svc.isExporting()) {
            CinematicPlaybackHandler handler = CinematicPlaybackHandler.getInstance();
            if (handler.isExporting()) {
                handler.captureAndPushFrame();
            }
        }
    }
}

