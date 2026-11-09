package net.openstage.client.model;

import net.minecraft.world.item.ItemStack;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class RecentStateStore {
    public static final class StateSnapshot {
        public final String modelId;
        public final String name;
        public final String format;
        public final File file;
        public final String skin;
        public final boolean isSlim;
        public final boolean isBendable;
        public final Map<String, float[]> rotations = new HashMap<>();
        public final Map<String, float[]> positions = new HashMap<>();
        public float rootYaw = 0.0f;
        public float scale = 1.0f;
        public ItemStack rightHandItem = ItemStack.EMPTY;
        public ItemStack leftHandItem = ItemStack.EMPTY;
        public boolean animatedEyes = true;
        public float pupilX = 0.0f;
        public float pupilY = 0.0f;
        public float leftEyelid = 0.0f;
        public float rightEyelid = 0.0f;

        public StateSnapshot(String modelId, String name, String format, File file, String skin, boolean isSlim, boolean isBendable) {
            this.modelId = modelId;
            this.name = name;
            this.format = format;
            this.file = file;
            this.skin = skin;
            this.isSlim = isSlim;
            this.isBendable = isBendable;
        }
    }

    private static final Map<String, StateSnapshot> SNAPSHOTS = new HashMap<>();

    public static void saveSnapshot(String modelId,
                                    String name,
                                    String format,
                                    File file,
                                    String skin,
                                    boolean isSlim,
                                    boolean isBendable,
                                    Map<String, float[]> rots,
                                    Map<String, float[]> poses,
                                    ModelSettings settings) {
        if (modelId == null || modelId.isEmpty()) return;
        StateSnapshot snapshot = new StateSnapshot(modelId, name, format, file, skin, isSlim, isBendable);
        if (rots != null) {
            for (Map.Entry<String, float[]> e : rots.entrySet()) {
                if (e.getValue() != null) {
                    snapshot.rotations.put(e.getKey(), e.getValue().clone());
                }
            }
        }
        if (poses != null) {
            for (Map.Entry<String, float[]> e : poses.entrySet()) {
                if (e.getValue() != null) {
                    snapshot.positions.put(e.getKey(), e.getValue().clone());
                }
            }
        }
        if (settings != null) {
            snapshot.rootYaw = settings.getRootYaw();
            snapshot.scale = settings.getScale();
            snapshot.rightHandItem = settings.getRightHandItem() != null ? settings.getRightHandItem().copy() : ItemStack.EMPTY;
            snapshot.leftHandItem = settings.getLeftHandItem() != null ? settings.getLeftHandItem().copy() : ItemStack.EMPTY;
            snapshot.animatedEyes = settings.isAnimatedEyes();
            snapshot.pupilX = settings.getPupilX();
            snapshot.pupilY = settings.getPupilY();
            snapshot.leftEyelid = settings.getLeftEyelid();
            snapshot.rightEyelid = settings.getRightEyelid();
        }

        SNAPSHOTS.put(modelId, snapshot);
    }

    public static StateSnapshot getSnapshot(String modelId) {
        if (modelId == null) return null;
        return SNAPSHOTS.get(modelId);
    }

    public static boolean hasSnapshot(String modelId) {
        return modelId != null && SNAPSHOTS.containsKey(modelId);
    }

    public static void clear() {
        SNAPSHOTS.clear();
    }
}

