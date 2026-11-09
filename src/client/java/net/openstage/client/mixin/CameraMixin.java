package net.openstage.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.openstage.client.cinematic.CinematicPlaybackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    private boolean detached;

    @Inject(method = "setup", at = @At("TAIL"))
    private void openstage$overrideCutsceneCamera(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        CinematicPlaybackHandler handler = CinematicPlaybackHandler.getInstance();
        if (handler.isPlaying()) {
            handler.applyCameraOverride((x, y, z, yaw, pitch) -> {
                this.setPosition(x, y, z);
                this.setRotation(yaw, pitch);
                this.detached = false;
            });
        }
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    private void openstage$overrideIsDetached(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (CinematicPlaybackHandler.getInstance().isPlaying()) {
            cir.setReturnValue(false);
        }
    }
}

