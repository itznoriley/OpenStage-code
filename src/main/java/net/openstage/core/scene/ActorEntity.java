package net.openstage.core.scene;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;

public class ActorEntity extends LivingEntity {
    public static final int MAX_ACTORS_PER_CHUNK = 32;

    private static final EntityDataAccessor<String> DATA_SKIN =
            SynchedEntityData.defineId(ActorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_IS_SLIM =
            SynchedEntityData.defineId(ActorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_CLIP_ID =
            SynchedEntityData.defineId(ActorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> DATA_START_TICK =
            SynchedEntityData.defineId(ActorEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_IS_LOOPING =
            SynchedEntityData.defineId(ActorEntity.class, EntityDataSerializers.BOOLEAN);

    public ActorEntity(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false;
    }

    public static AttributeSupplier.Builder createActorAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN, "");
        builder.define(DATA_IS_SLIM, false);
        builder.define(DATA_CLIP_ID, "");
        builder.define(DATA_START_TICK, 0L);
        builder.define(DATA_IS_LOOPING, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("SkinSource", getSkin());
        tag.putBoolean("IsSlim", isSlim());
        tag.putString("ClipId", getClipId());
        tag.putLong("StartTick", getStartTick());
        tag.putBoolean("IsLooping", isLooping());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SkinSource")) {
            setSkin(tag.getString("SkinSource"));
        }
        if (tag.contains("IsSlim")) {
            setSlim(tag.getBoolean("IsSlim"));
        }
        if (tag.contains("ClipId")) {
            String clipId = tag.getString("ClipId");
            long startTick = tag.getLong("StartTick");
            boolean looping = tag.getBoolean("IsLooping");
            playClip(clipId, startTick, looping);
        }
    }

    public void playClip(String clipId, long startTick, boolean looping) {
        this.entityData.set(DATA_CLIP_ID, clipId != null ? clipId : "");
        this.entityData.set(DATA_START_TICK, startTick);
        this.entityData.set(DATA_IS_LOOPING, looping);

        SceneManager.getInstance().playOnActor(this.getUUID(), clipId, startTick, looping);
    }

    public void stopClip() {
        this.entityData.set(DATA_CLIP_ID, "");
        this.entityData.set(DATA_START_TICK, 0L);
        this.entityData.set(DATA_IS_LOOPING, false);

        SceneManager.getInstance().stopActor(this.getUUID());
    }

    public String getClipId() {
        return this.entityData.get(DATA_CLIP_ID);
    }

    public long getStartTick() {
        return this.entityData.get(DATA_START_TICK);
    }

    public boolean isLooping() {
        return this.entityData.get(DATA_IS_LOOPING);
    }

    public boolean isPlaying() {
        String clip = getClipId();
        return clip != null && !clip.isEmpty();
    }

    public String getSkin() {
        return this.entityData.get(DATA_SKIN);
    }

    public void setSkin(String skin) {
        this.entityData.set(DATA_SKIN, skin != null ? skin : "");
    }

    public boolean isSlim() {
        return this.entityData.get(DATA_IS_SLIM);
    }

    public void setSlim(boolean slim) {
        this.entityData.set(DATA_IS_SLIM, slim);
    }

    @Override
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public @NotNull HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    public static boolean canSpawnInChunk(Level level, ChunkPos chunkPos) {
        if (level.isClientSide) return true;
        AABB box = new AABB(
                chunkPos.getMinBlockX(), level.getMinBuildHeight(), chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX() + 1, level.getMaxBuildHeight(), chunkPos.getMaxBlockZ() + 1
        );
        List<ActorEntity> actors = level.getEntitiesOfClass(ActorEntity.class, box);
        return actors.size() < MAX_ACTORS_PER_CHUNK;
    }
}

