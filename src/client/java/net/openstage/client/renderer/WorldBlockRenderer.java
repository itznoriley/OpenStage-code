package net.openstage.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.openstage.OpenStageMod;

import java.util.ArrayList;
import java.util.List;

public final class WorldBlockRenderer {

    public record CachedBlock(
            int dx, int dy, int dz,
            BlockState state,
            BakedModel model,
            float rCol, float gCol, float bCol,
            int light
    ) {}

    private static BlockPos lastCenter = null;
    private static int lastRadius = -1;
    private static long lastScanTimeMs = 0L;
    private static final List<CachedBlock> cache = new ArrayList<>();

    public static void invalidateCache() {
        lastCenter = null;
        lastRadius = -1;
        cache.clear();
    }

    public static void renderSurroundingWorld(PoseStack poseStack, MultiBufferSource buffers, BlockPos centerPos, int radius) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || centerPos == null) return;
        if (!level.hasChunkAt(centerPos)) return;

        int r = Math.min(32, Math.max(8, radius));
        long now = System.currentTimeMillis();

        if (lastCenter == null || !lastCenter.equals(centerPos) || lastRadius != r || (now - lastScanTimeMs > 4000L)) {
            rebuildCache(mc, level, centerPos, r);
            lastCenter = centerPos;
            lastRadius = r;
            lastScanTimeMs = now;
        }

        if (cache.isEmpty()) return;

        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());

        for (int i = 0; i < cache.size(); i++) {
            CachedBlock cb = cache.get(i);
            poseStack.pushPose();
            poseStack.translate(cb.dx() - 0.5f, cb.dy(), cb.dz() - 0.5f);
            try {
                dispatcher.getModelRenderer().renderModel(
                        poseStack.last(), consumer, cb.state(), cb.model(),
                        cb.rCol(), cb.gCol(), cb.bCol(), cb.light(),
                        OverlayTexture.NO_OVERLAY
                );
            } catch (Exception ignored) {
                try {
                    dispatcher.renderSingleBlock(cb.state(), poseStack, buffers, cb.light(), OverlayTexture.NO_OVERLAY);
                } catch (Exception ignored2) {}
            }
            poseStack.popPose();
        }
    }

    private static void rebuildCache(Minecraft mc, Level level, BlockPos centerPos, int r) {
        cache.clear();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        int rSq = r * r;
        int minY = Math.max(-24, level.getMinBuildHeight() - centerPos.getY());
        int maxY = Math.min(32, level.getMaxBuildHeight() - 1 - centerPos.getY());
        int maxBlocksToRender = 8000;

        int lastChunkX = Integer.MIN_VALUE;
        int lastChunkZ = Integer.MIN_VALUE;
        boolean lastChunkLoaded = false;

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > rSq) continue;

                int blockX = centerPos.getX() + dx;
                int blockZ = centerPos.getZ() + dz;
                int chunkX = blockX >> 4;
                int chunkZ = blockZ >> 4;
                if (chunkX != lastChunkX || chunkZ != lastChunkZ) {
                    lastChunkX = chunkX;
                    lastChunkZ = chunkZ;
                    lastChunkLoaded = level.hasChunk(chunkX, chunkZ);
                }
                if (!lastChunkLoaded) continue;

                for (int dy = minY; dy <= maxY; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos pos = centerPos.offset(dx, dy, dz);
                    if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) continue;

                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.is(OpenStageMod.MODEL_BLOCK)) continue;

                    if (!isExposed(level, pos)) continue;

                    int light = 0;
                    for (Direction dir : Direction.values()) {
                        BlockPos n = pos.relative(dir);
                        if (n.getY() >= level.getMinBuildHeight() && n.getY() < level.getMaxBuildHeight()) {
                            if (!level.getBlockState(n).isSolidRender(level, n)) {
                                light = Math.max(light, LevelRenderer.getLightColor(level, n));
                            }
                        }
                    }
                    if (light == 0) {
                        light = LevelRenderer.getLightColor(level, state, pos);
                    }
                    int bLight = Math.max(4, LightTexture.block(light));
                    int sLight = Math.max(10, LightTexture.sky(light));
                    light = LightTexture.pack(bLight, sLight);

                    int color = mc.getBlockColors().getColor(state, level, pos, 0);
                    float rCol = (color != -1) ? ((color >> 16) & 0xFF) / 255.0f : 1.0f;
                    float gCol = (color != -1) ? ((color >> 8) & 0xFF) / 255.0f : 1.0f;
                    float bCol = (color != -1) ? (color & 0xFF) / 255.0f : 1.0f;

                    BakedModel model = dispatcher.getBlockModel(state);
                    cache.add(new CachedBlock(dx, dy, dz, state, model, rCol, gCol, bCol, light));

                    if (cache.size() >= maxBlocksToRender) {
                        return;
                    }
                }
            }
        }
    }

    private static boolean isExposed(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (neighbor.getY() >= level.getMaxBuildHeight()) {
                return true;
            }
            if (neighbor.getY() < level.getMinBuildHeight()) {
                continue;
            }
            if (!level.hasChunkAt(neighbor)) {
                continue;
            }
            BlockState nState = level.getBlockState(neighbor);
            if (!nState.isSolidRender(level, neighbor)) {
                return true;
            }
        }
        return false;
    }
}

