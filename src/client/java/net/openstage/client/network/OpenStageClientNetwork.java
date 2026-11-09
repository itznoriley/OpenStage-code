package net.openstage.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.Entity;
import net.openstage.OpenStageMod;
import net.openstage.core.scene.ActorEntity;
import net.openstage.core.scene.SceneManager;
import net.openstage.network.OpenStageNetwork;

public final class OpenStageClientNetwork {
    private OpenStageClientNetwork() {}

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(OpenStageNetwork.SyncAnimationPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    SceneManager.getInstance().playOnActor(
                            payload.entityId(), payload.clipId(),
                            payload.startTick(), payload.loop()
                    );
                    if (context.client().level != null) {
                        for (Entity entity : context.client().level.entitiesForRendering()) {
                            if (entity.getUUID().equals(payload.entityId()) && entity instanceof ActorEntity actor) {
                                actor.playClip(payload.clipId(), payload.startTick(), payload.loop());
                                break;
                            }
                        }
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(OpenStageNetwork.ActorSpawnedPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    OpenStageMod.LOGGER.info("[OpenStage] Received ActorSpawnedPayload for entity {}", payload.entityId());
                }));

        OpenStageMod.LOGGER.info("[OpenStage] Client network handlers registered.");
    }
}

