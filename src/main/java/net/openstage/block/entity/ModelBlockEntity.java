package net.openstage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.openstage.OpenStageMod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ModelBlockEntity extends BlockEntity {
    private String modelId = "player:steve";
    private String skin = "steve";
    private boolean isSlim = false;
    private String clipId = "idle";
    private long startTick = 0L;
    private boolean looping = true;
    private boolean staticPose = false;
    private final Map<String, float[]> poseRotations = new HashMap<>();
    private final Map<String, float[]> posePositions = new HashMap<>();
    private float yaw = 0.0f;
    private float scale = 1.0f;
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;
    private float offsetZ = 0.0f;
    private ItemStack rightHandItem = ItemStack.EMPTY;
    private ItemStack leftHandItem = ItemStack.EMPTY;

    public ModelBlockEntity(BlockPos pos, BlockState state) {
        super(OpenStageMod.MODEL_BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("ModelId", modelId);
        tag.putString("Skin", skin);
        tag.putBoolean("IsSlim", isSlim);
        tag.putString("ClipId", clipId);
        tag.putLong("StartTick", startTick);
        tag.putBoolean("Looping", looping);
        tag.putBoolean("StaticPose", staticPose);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Scale", scale);
        tag.putFloat("OffsetX", offsetX);
        tag.putFloat("OffsetY", offsetY);
        tag.putFloat("OffsetZ", offsetZ);

        if (!rightHandItem.isEmpty()) {
            tag.put("RightHandItem", rightHandItem.saveOptional(registries));
        }
        if (!leftHandItem.isEmpty()) {
            tag.put("LeftHandItem", leftHandItem.saveOptional(registries));
        }

        if (!poseRotations.isEmpty()) {
            CompoundTag poseTag = new CompoundTag();
            for (Map.Entry<String, float[]> entry : poseRotations.entrySet()) {
                float[] r = entry.getValue();
                if (r != null && r.length >= 3) {
                    CompoundTag rotTag = new CompoundTag();
                    rotTag.putFloat("x", r[0]);
                    rotTag.putFloat("y", r[1]);
                    rotTag.putFloat("z", r[2]);
                    poseTag.put(entry.getKey(), rotTag);
                }
            }
            tag.put("PoseRotations", poseTag);
        }

        if (!posePositions.isEmpty()) {
            CompoundTag posTag = new CompoundTag();
            for (Map.Entry<String, float[]> entry : posePositions.entrySet()) {
                float[] p = entry.getValue();
                if (p != null && p.length >= 3) {
                    CompoundTag pTag = new CompoundTag();
                    pTag.putFloat("x", p[0]);
                    pTag.putFloat("y", p[1]);
                    pTag.putFloat("z", p[2]);
                    posTag.put(entry.getKey(), pTag);
                }
            }
            tag.put("PosePositions", posTag);
        }
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ModelId")) modelId = tag.getString("ModelId");
        if (tag.contains("Skin")) skin = tag.getString("Skin");
        if (tag.contains("IsSlim")) isSlim = tag.getBoolean("IsSlim");
        if (tag.contains("ClipId")) clipId = tag.getString("ClipId");
        if (tag.contains("StartTick")) startTick = tag.getLong("StartTick");
        if (tag.contains("Looping")) looping = tag.getBoolean("Looping");
        if (tag.contains("StaticPose")) staticPose = tag.getBoolean("StaticPose");
        if (tag.contains("Yaw")) yaw = tag.getFloat("Yaw");
        if (tag.contains("Scale")) scale = tag.getFloat("Scale");
        if (tag.contains("OffsetX")) offsetX = tag.getFloat("OffsetX");
        if (tag.contains("OffsetY")) offsetY = tag.getFloat("OffsetY");
        if (tag.contains("OffsetZ")) offsetZ = tag.getFloat("OffsetZ");

        if (tag.contains("RightHandItem")) {
            rightHandItem = ItemStack.parseOptional(registries, tag.getCompound("RightHandItem"));
        } else {
            rightHandItem = ItemStack.EMPTY;
        }
        if (tag.contains("LeftHandItem")) {
            leftHandItem = ItemStack.parseOptional(registries, tag.getCompound("LeftHandItem"));
        } else {
            leftHandItem = ItemStack.EMPTY;
        }

        poseRotations.clear();
        if (tag.contains("PoseRotations")) {
            CompoundTag poseTag = tag.getCompound("PoseRotations");
            for (String key : poseTag.getAllKeys()) {
                CompoundTag rotTag = poseTag.getCompound(key);
                poseRotations.put(key, new float[]{
                        rotTag.getFloat("x"),
                        rotTag.getFloat("y"),
                        rotTag.getFloat("z")
                });
            }
        }

        posePositions.clear();
        if (tag.contains("PosePositions")) {
            CompoundTag posTag = tag.getCompound("PosePositions");
            for (String key : posTag.getAllKeys()) {
                CompoundTag pTag = posTag.getCompound(key);
                posePositions.put(key, new float[]{
                        pTag.getFloat("x"),
                        pTag.getFloat("y"),
                        pTag.getFloat("z")
                });
            }
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean isStaticPose() { return staticPose; }
    public void setStaticPose(boolean staticPose) {
        this.staticPose = staticPose;
        setChanged();
    }

    public Map<String, float[]> getPoseRotations() { return poseRotations; }
    public void setPoseRotations(Map<String, float[]> newRotations) {
        poseRotations.clear();
        if (newRotations != null) {
            for (Map.Entry<String, float[]> e : newRotations.entrySet()) {
                float[] r = e.getValue();
                if (r != null && r.length >= 3) {
                    poseRotations.put(e.getKey(), new float[]{r[0], r[1], r[2]});
                }
            }
        }
        setChanged();
    }

    public Map<String, float[]> getPosePositions() { return posePositions; }
    public void setPosePositions(Map<String, float[]> newPositions) {
        posePositions.clear();
        if (newPositions != null) {
            for (Map.Entry<String, float[]> e : newPositions.entrySet()) {
                float[] p = e.getValue();
                if (p != null && p.length >= 3) {
                    posePositions.put(e.getKey(), new float[]{p[0], p[1], p[2]});
                }
            }
        }
        setChanged();
    }

    public String getModelId() { return modelId != null ? modelId : "player:steve"; }
    public void setModelId(String modelId) {
        this.modelId = (modelId != null) ? modelId : "player:steve";
        setChanged();
    }

    public String getSkin() { return skin; }
    public void setSkin(String skin) {
        this.skin = (skin != null) ? skin : "steve";
        setChanged();
    }

    public boolean isSlim() { return isSlim; }
    public void setSlim(boolean slim) {
        this.isSlim = slim;
        setChanged();
    }

    public String getClipId() { return clipId; }
    public void setClipId(String clipId) {
        this.clipId = (clipId != null) ? clipId : "idle";
        setChanged();
    }

    public long getStartTick() { return startTick; }
    public void setStartTick(long startTick) {
        this.startTick = startTick;
        setChanged();
    }

    public boolean isLooping() { return looping; }
    public void setLooping(boolean looping) {
        this.looping = looping;
        setChanged();
    }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) {
        this.yaw = yaw;
        setChanged();
    }

    public float getScale() { return scale; }
    public void setScale(float scale) {
        this.scale = scale;
        setChanged();
    }

    public float getOffsetX() { return offsetX; }
    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
        setChanged();
    }

    public float getOffsetY() { return offsetY; }
    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
        setChanged();
    }

    public float getOffsetZ() { return offsetZ; }
    public void setOffsetZ(float offsetZ) {
        this.offsetZ = offsetZ;
        setChanged();
    }

    public ItemStack getRightHandItem() {
        return rightHandItem;
    }

    public void setRightHandItem(ItemStack rightHandItem) {
        this.rightHandItem = (rightHandItem != null) ? rightHandItem : ItemStack.EMPTY;
        setChanged();
    }

    public ItemStack getLeftHandItem() {
        return leftHandItem;
    }

    public void setLeftHandItem(ItemStack leftHandItem) {
        this.leftHandItem = (leftHandItem != null) ? leftHandItem : ItemStack.EMPTY;
        setChanged();
    }
}

