package net.openstage.core.scene;

import net.openstage.core.animation.AnimationClip;
import net.openstage.core.animation.BoneTrack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SceneManager {
    private static final SceneManager INSTANCE = new SceneManager();

    public static SceneManager getInstance() {
        return INSTANCE;
    }

    private SceneManager() {
    }

    public record ActorState(UUID entityId, String clipId, long startTick, boolean looping) {
        public ActorState {
            Objects.requireNonNull(entityId, "entityId must not be null");
            Objects.requireNonNull(clipId, "clipId must not be null");
        }
    }

    private final Map<String, AnimationClip> loadedClips = new ConcurrentHashMap<>();
    private final List<ActorState> actors = new CopyOnWriteArrayList<>();

    public void loadClip(String id, AnimationClip clip) {
        Objects.requireNonNull(id, "clip id must not be null");
        Objects.requireNonNull(clip, "clip must not be null");
        loadedClips.put(id, clip);
    }

    public void unloadClip(String id) {
        loadedClips.remove(id);
    }

    public AnimationClip getClip(String id) {
        return loadedClips.get(id);
    }

    public Map<String, AnimationClip> getLoadedClips() {
        return Collections.unmodifiableMap(loadedClips);
    }

    public void playOnActor(UUID entityId, String clipId, long currentTick, boolean loop) {
        Objects.requireNonNull(entityId, "entityId must not be null");
        Objects.requireNonNull(clipId, "clipId must not be null");
        if (!loadedClips.containsKey(clipId)) {
            throw new IllegalArgumentException("Clip '" + clipId + "' is not loaded");
        }

        actors.removeIf(a -> a.entityId().equals(entityId));
        actors.add(new ActorState(entityId, clipId, currentTick, loop));
    }

    public void stopActor(UUID entityId) {
        actors.removeIf(a -> a.entityId().equals(entityId));
    }

    public List<ActorState> getActors() {
        return Collections.unmodifiableList(new ArrayList<>(actors));
    }

    public float[] getActorBoneRotation(UUID entityId, String boneName,
                                        long currentTick, float ticksPerSecond) {
        ActorState state = findActor(entityId);
        if (state == null) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }

        AnimationClip clip = loadedClips.get(state.clipId());
        if (clip == null) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }

        float elapsed = (currentTick - state.startTick()) / ticksPerSecond;
        BoneTrack track = clip.getBones().get(boneName);
        if (track == null) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }

        float time;
        if (clip.isLooping() && clip.getLength() > 0) {
            time = elapsed % clip.getLength();
            if (time < 0) time += clip.getLength();
        } else {
            time = Math.clamp(elapsed, 0.0f, clip.getLength());
        }

        return track.getRotationAt(time);
    }

    private ActorState findActor(UUID entityId) {
        for (ActorState s : actors) {
            if (s.entityId().equals(entityId)) {
                return s;
            }
        }
        return null;
    }
}

