package net.openstage.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.openstage.OpenStageMod;
import net.openstage.block.entity.ModelBlockEntity;
import net.openstage.core.scene.ActorEntity;
import net.openstage.core.scene.SceneManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OpenStageNetwork {
    private OpenStageNetwork() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final ResourceLocation PLAY_ANIMATION_ID =
            ResourceLocation.fromNamespaceAndPath(OpenStageMod.MOD_ID, "play_animation");
    private static final ResourceLocation STOP_ANIMATION_ID =
            ResourceLocation.fromNamespaceAndPath(OpenStageMod.MOD_ID, "stop_animation");
    private static final ResourceLocation SYNC_ANIMATION_ID =
            ResourceLocation.fromNamespaceAndPath(OpenStageMod.MOD_ID, "sync_animation");
    private static final ResourceLocation SPAWN_ACTOR_ID =
            ResourceLocation.fromNamespaceAndPath(OpenStageMod.MOD_ID, "spawn_actor");
    private static final ResourceLocation ACTOR_SPAWNED_ID =
            ResourceLocation.fromNamespaceAndPath(OpenStageMod.MOD_ID, "actor_spawned");
    private static final ResourceLocation UPDATE_MODEL_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath(OpenStageMod.MOD_ID, "update_model_block");
    private static final ResourceLocation APPLY_POSE_TO_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath(OpenStageMod.MOD_ID, "apply_pose_to_block");

    public record PlayAnimationPayload(
            UUID entityId, String clipId, long startTick, boolean loop
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PlayAnimationPayload> TYPE =
                new CustomPacketPayload.Type<>(PLAY_ANIMATION_ID);

        public static final StreamCodec<FriendlyByteBuf, PlayAnimationPayload> CODEC =
                StreamCodec.of(PlayAnimationPayload::write, PlayAnimationPayload::read);

        private static PlayAnimationPayload read(FriendlyByteBuf buf) {
            return new PlayAnimationPayload(buf.readUUID(), buf.readUtf(), buf.readLong(), buf.readBoolean());
        }

        private static void write(FriendlyByteBuf buf, PlayAnimationPayload payload) {
            buf.writeUUID(payload.entityId);
            buf.writeUtf(payload.clipId);
            buf.writeLong(payload.startTick);
            buf.writeBoolean(payload.loop);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StopAnimationPayload(UUID entityId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StopAnimationPayload> TYPE =
                new CustomPacketPayload.Type<>(STOP_ANIMATION_ID);

        public static final StreamCodec<FriendlyByteBuf, StopAnimationPayload> CODEC =
                StreamCodec.of(StopAnimationPayload::write, StopAnimationPayload::read);

        private static StopAnimationPayload read(FriendlyByteBuf buf) {
            return new StopAnimationPayload(buf.readUUID());
        }

        private static void write(FriendlyByteBuf buf, StopAnimationPayload payload) {
            buf.writeUUID(payload.entityId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SyncAnimationPayload(
            UUID entityId, String clipId, long startTick, boolean loop
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncAnimationPayload> TYPE =
                new CustomPacketPayload.Type<>(SYNC_ANIMATION_ID);

        public static final StreamCodec<FriendlyByteBuf, SyncAnimationPayload> CODEC =
                StreamCodec.of(SyncAnimationPayload::write, SyncAnimationPayload::read);

        private static SyncAnimationPayload read(FriendlyByteBuf buf) {
            return new SyncAnimationPayload(buf.readUUID(), buf.readUtf(), buf.readLong(), buf.readBoolean());
        }

        private static void write(FriendlyByteBuf buf, SyncAnimationPayload payload) {
            buf.writeUUID(payload.entityId);
            buf.writeUtf(payload.clipId);
            buf.writeLong(payload.startTick);
            buf.writeBoolean(payload.loop);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SpawnActorPayload(
            UUID entityId, double x, double y, double z, String skinSource
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SpawnActorPayload> TYPE =
                new CustomPacketPayload.Type<>(SPAWN_ACTOR_ID);

        public static final StreamCodec<FriendlyByteBuf, SpawnActorPayload> CODEC =
                StreamCodec.of(SpawnActorPayload::write, SpawnActorPayload::read);

        private static SpawnActorPayload read(FriendlyByteBuf buf) {
            return new SpawnActorPayload(
                    buf.readUUID(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readUtf());
        }

        private static void write(FriendlyByteBuf buf, SpawnActorPayload payload) {
            buf.writeUUID(payload.entityId);
            buf.writeDouble(payload.x);
            buf.writeDouble(payload.y);
            buf.writeDouble(payload.z);
            buf.writeUtf(payload.skinSource);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ActorSpawnedPayload(
            UUID entityId, double x, double y, double z, String skinSource
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ActorSpawnedPayload> TYPE =
                new CustomPacketPayload.Type<>(ACTOR_SPAWNED_ID);

        public static final StreamCodec<FriendlyByteBuf, ActorSpawnedPayload> CODEC =
                StreamCodec.of(ActorSpawnedPayload::write, ActorSpawnedPayload::read);

        private static ActorSpawnedPayload read(FriendlyByteBuf buf) {
            return new ActorSpawnedPayload(
                    buf.readUUID(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readUtf());
        }

        private static void write(FriendlyByteBuf buf, ActorSpawnedPayload payload) {
            buf.writeUUID(payload.entityId);
            buf.writeDouble(payload.x);
            buf.writeDouble(payload.y);
            buf.writeDouble(payload.z);
            buf.writeUtf(payload.skinSource);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UpdateModelBlockPayload(
            BlockPos pos, String modelId, String skin, boolean isSlim, String clipId, float yaw, float scale
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<UpdateModelBlockPayload> TYPE =
                new CustomPacketPayload.Type<>(UPDATE_MODEL_BLOCK_ID);

        public static final StreamCodec<FriendlyByteBuf, UpdateModelBlockPayload> CODEC =
                StreamCodec.of(UpdateModelBlockPayload::write, UpdateModelBlockPayload::read);

        private static UpdateModelBlockPayload read(FriendlyByteBuf buf) {
            return new UpdateModelBlockPayload(
                    buf.readBlockPos(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readBoolean(),
                    buf.readUtf(),
                    buf.readFloat(),
                    buf.readFloat()
            );
        }

        private static void write(FriendlyByteBuf buf, UpdateModelBlockPayload payload) {
            buf.writeBlockPos(payload.pos());
            buf.writeUtf(payload.modelId());
            buf.writeUtf(payload.skin());
            buf.writeBoolean(payload.isSlim());
            buf.writeUtf(payload.clipId());
            buf.writeFloat(payload.yaw());
            buf.writeFloat(payload.scale());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ApplyPoseToBlockPayload(
            BlockPos pos, String modelId, String skin, boolean isSlim, float yaw, float scale, String poseData, String posData
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ApplyPoseToBlockPayload> TYPE =
                new CustomPacketPayload.Type<>(APPLY_POSE_TO_BLOCK_ID);

        public static final StreamCodec<FriendlyByteBuf, ApplyPoseToBlockPayload> CODEC =
                StreamCodec.of(ApplyPoseToBlockPayload::write, ApplyPoseToBlockPayload::read);

        private static ApplyPoseToBlockPayload read(FriendlyByteBuf buf) {
            return new ApplyPoseToBlockPayload(
                    buf.readBlockPos(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readBoolean(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readUtf(),
                    buf.readUtf()
            );
        }

        private static void write(FriendlyByteBuf buf, ApplyPoseToBlockPayload payload) {
            buf.writeBlockPos(payload.pos());
            buf.writeUtf(payload.modelId());
            buf.writeUtf(payload.skin());
            buf.writeBoolean(payload.isSlim());
            buf.writeFloat(payload.yaw());
            buf.writeFloat(payload.scale());
            buf.writeUtf(payload.poseData());
            buf.writeUtf(payload.posData());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerServerHandlers() {
        PayloadTypeRegistry.playC2S().register(PlayAnimationPayload.TYPE, PlayAnimationPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StopAnimationPayload.TYPE, StopAnimationPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SpawnActorPayload.TYPE,    SpawnActorPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateModelBlockPayload.TYPE, UpdateModelBlockPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ApplyPoseToBlockPayload.TYPE, ApplyPoseToBlockPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(SyncAnimationPayload.TYPE, SyncAnimationPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ActorSpawnedPayload.TYPE,  ActorSpawnedPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PlayAnimationPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer sender = context.player();
                    if (!hasPermission(sender)) return;
                    context.server().execute(() -> {
                        SceneManager.getInstance().playOnActor(
                                payload.entityId(), payload.clipId(),
                                payload.startTick(), payload.loop());

                        SyncAnimationPayload sync = new SyncAnimationPayload(
                                payload.entityId(), payload.clipId(),
                                payload.startTick(), payload.loop());
                        for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
                            ServerPlayNetworking.send(player, sync);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(StopAnimationPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer sender = context.player();
                    if (!hasPermission(sender)) return;
                    context.server().execute(() -> {
                        SceneManager.getInstance().stopActor(payload.entityId());
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(SpawnActorPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer sender = context.player();
                    if (!hasPermission(sender)) return;
                    context.server().execute(() -> {
                        ServerLevel level = sender.serverLevel();
                        ChunkPos chunkPos = new ChunkPos(
                                (int) payload.x() >> 4,
                                (int) payload.z() >> 4);
                        if (!level.isLoaded(chunkPos.getWorldPosition())) {
                            return;
                        }

                        ActorEntity actor = new ActorEntity(OpenStageMod.ACTOR_ENTITY_TYPE, level);
                        actor.setUUID(payload.entityId());
                        actor.setPos(payload.x(), payload.y(), payload.z());
                        actor.setSkin(payload.skinSource());
                        level.addFreshEntity(actor);

                        ActorSpawnedPayload spawned = new ActorSpawnedPayload(
                                payload.entityId(), payload.x(), payload.y(),
                                payload.z(), payload.skinSource());
                        for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
                            ServerPlayNetworking.send(player, spawned);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(UpdateModelBlockPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer sender = context.player();
                    if (!hasPermission(sender)) return;
                    context.server().execute(() -> {
                        ServerLevel level = sender.serverLevel();
                        BlockEntity be = level.getBlockEntity(payload.pos());
                        if (be instanceof ModelBlockEntity mbe) {
                            mbe.setModelId(payload.modelId());
                            mbe.setSkin(payload.skin());
                            mbe.setSlim(payload.isSlim());
                            mbe.setClipId(payload.clipId());
                            mbe.setYaw(payload.yaw());
                            mbe.setScale(payload.scale());
                            mbe.setStaticPose(false);
                            mbe.setStartTick(level.getGameTime());
                            mbe.setChanged();
                            level.sendBlockUpdated(payload.pos(), mbe.getBlockState(), mbe.getBlockState(), 3);
                            OpenStageMod.LOGGER.info("[OpenStage] {} updated ModelBlock at {}", sender.getName().getString(), payload.pos());
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(ApplyPoseToBlockPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer sender = context.player();
                    if (!hasPermission(sender)) return;
                    context.server().execute(() -> {
                        ServerLevel level = sender.serverLevel();
                        BlockEntity be = level.getBlockEntity(payload.pos());
                        if (be instanceof ModelBlockEntity mbe) {
                            mbe.setModelId(payload.modelId());
                            mbe.setSkin(payload.skin());
                            mbe.setSlim(payload.isSlim());
                            mbe.setYaw(payload.yaw());
                            mbe.setScale(payload.scale());
                            mbe.setStaticPose(true);
                            mbe.setPoseRotations(parsePoseData(payload.poseData()));
                            mbe.setPosePositions(parsePoseData(payload.posData()));
                            mbe.setChanged();
                            level.sendBlockUpdated(payload.pos(), mbe.getBlockState(), mbe.getBlockState(), 3);
                            OpenStageMod.LOGGER.info("[OpenStage] {} applied static pose to ModelBlock at {}", sender.getName().getString(), payload.pos());
                        }
                    });
                });

        OpenStageMod.LOGGER.info("[OpenStage] Server network handlers registered");
    }

    public static Map<String, float[]> parsePoseData(String data) {
        Map<String, float[]> map = new HashMap<>();
        if (data == null || data.isEmpty()) return map;
        String[] parts = data.split(";");
        for (String p : parts) {
            String[] kv = p.split("=");
            if (kv.length == 2) {
                String bone = kv[0].trim();
                if (bone.isEmpty()) continue;
                String[] vals = kv[1].split(",");
                if (vals.length == 3) {
                    try {
                        float v0 = Float.parseFloat(vals[0].trim());
                        float v1 = Float.parseFloat(vals[1].trim());
                        float v2 = Float.parseFloat(vals[2].trim());
                        if (Float.isFinite(v0) && Float.isFinite(v1) && Float.isFinite(v2)) {
                            map.put(bone, new float[]{v0, v1, v2});
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return map;
    }

    public static String serializePoseData(Map<String, float[]> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, float[]> e : map.entrySet()) {
            if (e.getKey() == null || e.getKey().isBlank() || e.getValue() == null || e.getValue().length < 3) continue;
            float[] r = e.getValue();
            if (!Float.isFinite(r[0]) || !Float.isFinite(r[1]) || !Float.isFinite(r[2])) continue;
            if (sb.length() > 0) sb.append(";");
            sb.append(e.getKey().trim()).append("=").append(r[0]).append(",").append(r[1]).append(",").append(r[2]);
        }
        return sb.toString();
    }

    public static boolean hasPermission(ServerPlayer player) {
        if (player.server.isSingleplayer()) return true;
        if (player.hasPermissions(2)) return true;
        return player.isCreative();
    }
}

