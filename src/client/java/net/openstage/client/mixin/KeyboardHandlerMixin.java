package net.openstage.client.mixin;

import net.minecraft.client.KeyboardHandler;
import net.openstage.client.cinematic.CinematicPlaybackHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void openstage$handleCinematicEscape(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_ESCAPE) {
            CinematicPlaybackHandler handler = CinematicPlaybackHandler.getInstance();
            if (handler.isPlaying()) {
                handler.stop();
                ci.cancel();
            }
        }
    }
}

