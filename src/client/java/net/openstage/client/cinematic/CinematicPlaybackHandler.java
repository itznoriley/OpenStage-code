package net.openstage.client.cinematic;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.openstage.block.entity.ModelBlockEntity;
import net.openstage.client.ui.widgets.TextOverlayRenderer;
import net.openstage.core.animation.AnimationClip;
import net.openstage.core.animation.TextCard;
import net.openstage.core.scene.CameraTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CinematicPlaybackHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("OpenStage/Cinematic");

    @FunctionalInterface
    public interface CameraSetter {
        void set(double x, double y, double z, float yaw, float pitch);
    }

    private static final CinematicPlaybackHandler INSTANCE = new CinematicPlaybackHandler();

    public static CinematicPlaybackHandler getInstance() {
        return INSTANCE;
    }

    private boolean playing = false;
    private boolean isExporting = false;
    private boolean isStaticMode = false;
    private net.minecraft.client.gui.screens.Screen returnScreen = null;
    private AnimationClip currentClip = null;
    private BlockPos originPos = null;
    private final List<BlockPos> sceneActors = new ArrayList<>();
    private float currentTime = 0.0f;
    private float currentFov = 70.0f;
    private float currentRoll = 0.0f;
    private boolean originalHideGui = false;
    private double originalPlayerX, originalPlayerY, originalPlayerZ;
    private float originalYaw, originalPitch;

    private CinematicPlaybackHandler() {
        HudRenderCallback.EVENT.register(this::onRenderHud);
    }

    public void start(AnimationClip clip, BlockPos origin) {
        start(clip, origin, Collections.emptyList());
    }

    public void setReturnScreen(net.minecraft.client.gui.screens.Screen screen) {
        this.returnScreen = screen;
    }

    public void startStatic(AnimationClip clip, BlockPos origin, List<BlockPos> actors, net.minecraft.client.gui.screens.Screen screen, float evalTime) {
        this.returnScreen = screen;
        if (clip == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        this.currentClip = clip;
        this.originPos = origin;
        this.currentTime = evalTime;
        this.playing = true;
        this.isStaticMode = true;
        this.isExporting = false;
        this.sceneActors.clear();

        if (actors != null && !actors.isEmpty()) {
            this.sceneActors.addAll(actors);
        } else if (origin != null && mc.level != null) {
            this.sceneActors.add(origin);
            int r = 24;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -6; dy <= 6; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos p = origin.offset(dx, dy, dz);
                        if (mc.level.getBlockEntity(p) instanceof ModelBlockEntity) {
                            this.sceneActors.add(p);
                        }
                    }
                }
            }
        } else if (mc.level != null) {
            BlockPos closest = findNearestActor(mc.player.blockPosition());
            if (closest != null) {
                this.sceneActors.add(closest);
                if (this.originPos == null) this.originPos = closest;
            }
        }

        this.originalHideGui = mc.options.hideGui;
        this.originalPlayerX = mc.player.getX();
        this.originalPlayerY = mc.player.getY();
        this.originalPlayerZ = mc.player.getZ();
        this.originalYaw = mc.player.getYRot();
        this.originalPitch = mc.player.getXRot();
        mc.options.hideGui = true;
        mc.gameRenderer.setRenderHand(false);
        mc.player.setInvisible(true);

        evaluateSceneActors(evalTime);
        evaluateCamera(evalTime);
    }

    public void start(AnimationClip clip, BlockPos origin, List<BlockPos> actors, net.minecraft.client.gui.screens.Screen screen) {
        if (clip == null || clip.getLength() <= 0.05f) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§e[OpenStage] Animation is empty (0s duration)"), true);
            }
            return;
        }
        this.returnScreen = screen;
        start(clip, origin, actors);
    }

    public void start(AnimationClip clip, BlockPos origin, List<BlockPos> actors) {
        if (clip == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        this.currentClip = clip;
        this.originPos = origin;
        this.currentTime = 0.0f;
        this.playing = true;
        this.isExporting = false;
        this.sceneActors.clear();

        if (actors != null && !actors.isEmpty()) {
            this.sceneActors.addAll(actors);
        } else if (origin != null && mc.level != null) {
            this.sceneActors.add(origin);
            int r = 24;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -6; dy <= 6; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos p = origin.offset(dx, dy, dz);
                        if (mc.level.getBlockEntity(p) instanceof ModelBlockEntity) {
                            this.sceneActors.add(p);
                        }
                    }
                }
            }
        } else if (mc.level != null) {
            BlockPos closest = findNearestActor(mc.player.blockPosition());
            if (closest != null) {
                this.sceneActors.add(closest);
                if (this.originPos == null) this.originPos = closest;
            }
        }

        this.originalHideGui = mc.options.hideGui;
        this.originalPlayerX = mc.player.getX();
        this.originalPlayerY = mc.player.getY();
        this.originalPlayerZ = mc.player.getZ();
        this.originalYaw = mc.player.getYRot();
        this.originalPitch = mc.player.getXRot();
        mc.options.hideGui = true;
        mc.gameRenderer.setRenderHand(false);
        mc.player.setInvisible(true);

        mc.player.displayClientMessage(
                Component.literal("В§6[OpenStage] В§fPlaying cinematic: В§b" + clip.getName() + " В§7(Press ESC to exit)"),
                true
        );

        evaluateSceneActors(0.0f);
        evaluateCamera(0.0f);

        if (net.openstage.client.audio.AudioPlaybackService.getInstance().getCurrentTrack() == null) {
            java.io.File audioFile = new java.io.File("openstage/audio/cutest_kitty.ogg");
            if (audioFile.exists()) {
                try {
                    net.openstage.client.audio.AudioPlaybackService.getInstance().setTrack(net.openstage.client.audio.AudioTrackData.loadAudio(audioFile));
                } catch (Exception ignored) {}
            }
        }
        if (net.openstage.client.audio.AudioPlaybackService.getInstance().getCurrentTrack() != null) {
            net.openstage.client.audio.AudioPlaybackService.getInstance().playFrom(0.0f);
        }
    }

    public void startExport(AnimationClip clip, BlockPos origin, List<BlockPos> actors, int fps) {
        if (clip == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        this.currentClip = clip;
        this.originPos = origin;
        this.currentTime = 0.0f;
        this.playing = true;
        this.isExporting = true;
        this.sceneActors.clear();

        if (actors != null && !actors.isEmpty()) {
            this.sceneActors.addAll(actors);
        } else if (origin != null && mc.level != null) {
            this.sceneActors.add(origin);
        } else if (mc.level != null) {
            BlockPos closest = findNearestActor(mc.player.blockPosition());
            if (closest != null) {
                this.sceneActors.add(closest);
                if (this.originPos == null) this.originPos = closest;
            }
        }

        this.originalHideGui = mc.options.hideGui;
        this.originalPlayerX = mc.player.getX();
        this.originalPlayerY = mc.player.getY();
        this.originalPlayerZ = mc.player.getZ();
        this.originalYaw = mc.player.getYRot();
        this.originalPitch = mc.player.getXRot();
        mc.options.hideGui = true;
        mc.gameRenderer.setRenderHand(false);
        mc.player.setInvisible(true);

        evaluateSceneActors(0.0f);
        evaluateCamera(0.0f);
    }

    private BlockPos findNearestActor(BlockPos pPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        BlockPos closest = null;
        double bestDist = Double.MAX_VALUE;
        int r = 24;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = pPos.offset(dx, dy, dz);
                    if (mc.level.getBlockEntity(p) instanceof ModelBlockEntity) {
                        double d = p.distSqr(pPos);
                        if (d < bestDist) {
                            bestDist = d;
                            closest = p;
                        }
                    }
                }
            }
        }
        return closest;
    }

    public void stop() {
        if (!playing) return;
        this.playing = false;
        this.isStaticMode = false;
        this.isExporting = false;
        net.openstage.client.audio.AudioPlaybackService.getInstance().stop();
        Minecraft mc = Minecraft.getInstance();
        mc.options.hideGui = originalHideGui;
        mc.gameRenderer.setRenderHand(true);
        if (mc.player != null) {
            mc.player.setInvisible(false);
        }

        if (mc.level != null) {
            for (BlockPos pos : sceneActors) {
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be instanceof ModelBlockEntity mbe) {
                    if (returnScreen == null) {
                        mbe.setStaticPose(false);
                    }
                }
            }
        }

        this.sceneActors.clear();
        this.currentClip = null;

        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("В§7[OpenStage] Playback stopped."), true);
        }

        if (returnScreen != null) {
            net.minecraft.client.gui.screens.Screen screenToRestore = returnScreen;
            returnScreen = null;
            mc.setScreen(screenToRestore);
        }
    }

    public boolean isStaticMode() {
        return isStaticMode;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isExporting() {
        return isExporting;
    }

    public float getCurrentFov() {
        return currentFov;
    }

    public float getCurrentRoll() {
        return currentRoll;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public void setExportTime(float time) {
        this.currentTime = time;
        evaluateSceneActors(time);
        evaluateCamera(time);
    }

    public void tick(float deltaSeconds) {
        if (!playing || currentClip == null || isExporting) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            stop();
            return;
        }

        if (isStaticMode) {
            evaluateSceneActors(currentTime);
            evaluateCamera(currentTime);
            return;
        }

        currentTime += deltaSeconds;

        if (currentTime >= currentClip.getLength()) {
            if (currentClip.isLooping()) {
                currentTime = 0.0f;
            } else {
                stop();
                return;
            }
        }

        evaluateSceneActors(currentTime);
        evaluateCamera(currentTime);
    }

    public void evaluateSceneActors(float evalTime) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || currentClip == null) return;

        if (sceneActors.isEmpty() && originPos != null) {
            sceneActors.add(originPos);
        } else if (sceneActors.isEmpty() && mc.player != null) {
            BlockPos closest = findNearestActor(mc.player.blockPosition());
            if (closest != null) {
                sceneActors.add(closest);
                if (originPos == null) originPos = closest;
            }
        }

        for (int i = 0; i < sceneActors.size(); i++) {
            BlockPos pos = sceneActors.get(i);
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be instanceof ModelBlockEntity mbe) {
                Map<String, float[]> actorRots = currentClip.evaluateFrameForActor(evalTime, i);
                Map<String, float[]> actorPos = currentClip.evaluatePositionsForActor(evalTime, i);
                mbe.setPoseRotations(actorRots);
                mbe.setPosePositions(actorPos);
                mbe.setStaticPose(true);
            }
        }
    }

    public void evaluateCamera(float evalTime) {
        if (currentClip == null) return;
        CameraTrack camTrack = currentClip.getCameraTrack();
        if (camTrack != null) {
            camTrack.ensureDefaultKeyframe();
            try {
                CameraTrack.CameraKeyframe kf = camTrack.interpolateAt(evalTime, currentClip.getLength(), currentClip.isLooping());
                this.currentFov = kf.fov();
                this.currentRoll = kf.roll();
            } catch (Exception ignored) {}
        }
    }

    public void applyCameraOverride(CameraSetter setter) {
        if (!playing || currentClip == null || setter == null) return;
        CameraTrack camTrack = currentClip.getCameraTrack();

        float camX = 0.0f, camY = 1.4f, camZ = -3.2f;
        float camYaw = 0.0f, camPitch = 0.0f, camRoll = 0.0f, camFov = 70.0f;

        if (camTrack != null && !camTrack.getKeyframes().isEmpty()) {
            try {
                float evalTime = currentTime;
                CameraTrack.CameraKeyframe kf = camTrack.interpolateAt(evalTime, currentClip.getLength(), currentClip.isLooping());
                camX = kf.x();
                camY = kf.y();
                camZ = kf.z();
                camYaw = kf.yaw();
                camPitch = kf.pitch();
                camRoll = kf.roll();
                camFov = kf.fov();
            } catch (Exception ignored) {}
        }

        this.currentFov = camFov;
        this.currentRoll = camRoll;

        Minecraft mc = Minecraft.getInstance();
        if (originPos == null) {
            if (!sceneActors.isEmpty()) {
                originPos = sceneActors.get(0);
            } else if (mc.player != null) {
                originPos = findNearestActor(mc.player.blockPosition());
            }
        }

        double baseX, baseY, baseZ;
        float actorYaw = 0.0f;

        BlockPos activeOrigin = originPos;
        if (currentClip != null) {
            activeOrigin = currentClip.getEnvironmentTrack().getAnchorAt(currentTime, originPos);
        }

        if (activeOrigin != null) {
            baseX = activeOrigin.getX() + 0.5;
            baseY = activeOrigin.getY();
            baseZ = activeOrigin.getZ() + 0.5;
            if (mc.level != null && mc.level.getBlockEntity(activeOrigin) instanceof ModelBlockEntity mbe) {
                actorYaw = mbe.getYaw();
                baseX += mbe.getOffsetX();
                baseY += mbe.getOffsetY();
                baseZ += mbe.getOffsetZ();
            }
        } else {
            baseX = originalPlayerX;
            baseY = originalPlayerY;
            baseZ = originalPlayerZ;
        }

        double finalX, finalY, finalZ;
        float finalYaw = camYaw;
        if (Math.abs(actorYaw) > 0.001f) {
            double rad = Math.toRadians(-actorYaw);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double rotX = camX * cos - camZ * sin;
            double rotZ = camX * sin + camZ * cos;
            finalX = baseX + rotX;
            finalY = baseY + camY;
            finalZ = baseZ + rotZ;
            finalYaw = (camYaw - actorYaw) % 360.0f;
        } else {
            finalX = baseX + camX;
            finalY = baseY + camY;
            finalZ = baseZ + camZ;
        }

        setter.set(finalX, finalY, finalZ, finalYaw, camPitch);
    }

    public void captureAndPushFrame() {
        VideoExportService svc = VideoExportService.getInstance();
        if (!svc.isExporting() || !isExporting) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getMainRenderTarget() == null) return;

        try {
            NativeImage raw = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            int targetW = svc.getExportWidth();
            int targetH = svc.getExportHeight();
            NativeImage out;
            if (raw.getWidth() == targetW && raw.getHeight() == targetH) {
                out = raw;
            } else {
                out = new NativeImage(targetW, targetH, false);
                raw.resizeSubRectTo(0, 0, raw.getWidth(), raw.getHeight(), out);
                raw.close();
            }
            svc.pushFrame(out);
            stepExportFrame();
        } catch (Exception e) {
            LOGGER.error("Failed to capture export frame", e);
        }
    }

    public void stepExportFrame() {
        VideoExportService svc = VideoExportService.getInstance();
        if (!svc.isExporting() || !isExporting) return;

        if (svc.getCurrentFrame() >= svc.getTotalFrames()) {
            svc.finishExport();
            stop();
            return;
        }

        float nextTime = (float) svc.getCurrentFrame() / (float) svc.getFps();
        this.currentTime = nextTime;
        evaluateSceneActors(nextTime);
        evaluateCamera(nextTime);
    }

    private void onRenderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!playing || currentClip == null) return;
        Minecraft mc = Minecraft.getInstance();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        if (isStaticMode) {
            return;
        }
        List<TextCard> activeCards = currentClip.getTextTrack().getActiveCardsAt(currentTime);
        for (TextCard card : activeCards) {
            TextOverlayRenderer.render(graphics, card, currentTime, 0, 0, screenW, screenH);
        }

        float fadeAlpha = (currentClip != null) ? currentClip.getEnvironmentTrack().getFadeAlphaAt(currentTime) : 0.0f;
        if (fadeAlpha > 0.005f) {
            int alphaInt = Math.min(255, (int) (fadeAlpha * 255.0f));
            graphics.fill(0, 0, screenW, screenH, (alphaInt << 24));
        }
    }
}

