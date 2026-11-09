package net.openstage.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.Vec3;
import net.openstage.OpenStageMod;
import net.openstage.core.scene.ActorEntity;

public final class OpenStageCommands {
    private OpenStageCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register(OpenStageCommands::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                         CommandBuildContext registryAccess,
                                         Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("openstage")
                .requires(source -> source.hasPermission(2) || (source.getPlayer() != null && source.getPlayer().isCreative()))
                .then(Commands.literal("spawn")
                        .executes(ctx -> spawnActor(ctx.getSource(), "steve"))
                        .then(Commands.argument("skin", StringArgumentType.word())
                                .executes(ctx -> spawnActor(ctx.getSource(), StringArgumentType.getString(ctx, "skin")))))
                .then(Commands.literal("stop")
                        .executes(ctx -> stopNearestActor(ctx.getSource())))
                .then(Commands.literal("play")
                        .then(Commands.argument("clip", StringArgumentType.word())
                                .executes(ctx -> playNearestActor(ctx.getSource(), StringArgumentType.getString(ctx, "clip")))))
        );
    }

    private static int spawnActor(CommandSourceStack source, String skin) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can spawn actors"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position().add(look.x * 2.0, 0.0, look.z * 2.0);
        ActorEntity actor = new ActorEntity(OpenStageMod.ACTOR_ENTITY_TYPE, level);
        actor.setPos(pos.x, pos.y, pos.z);
        actor.setYRot(player.getYRot() + 180.0f);
        actor.setSkin(skin);
        level.addFreshEntity(actor);

        source.sendSuccess(() -> Component.literal("§a[OpenStage] Spawned Actor (" + skin + ") at " +
                String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z)), true);
        return 1;
    }

    private static int stopNearestActor(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can stop actors"));
            return 0;
        }

        ActorEntity nearest = player.level().getNearestEntity(
                ActorEntity.class,
                TargetingConditions.forNonCombat(),
                player, player.getX(), player.getY(), player.getZ(),
                player.getBoundingBox().inflate(16.0)
        );

        if (nearest == null) {
            source.sendFailure(Component.literal("§c[OpenStage] No ActorEntity found nearby (16 blocks)"));
            return 0;
        }

        nearest.stopClip();
        source.sendSuccess(() -> Component.literal("§a[OpenStage] Stopped animation on actor " + nearest.getUUID()), true);
        return 1;
    }

    private static int playNearestActor(CommandSourceStack source, String clipId) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can play animations on actors"));
            return 0;
        }

        ActorEntity nearest = player.level().getNearestEntity(
                ActorEntity.class,
                TargetingConditions.forNonCombat(),
                player, player.getX(), player.getY(), player.getZ(),
                player.getBoundingBox().inflate(16.0)
        );

        if (nearest == null) {
            source.sendFailure(Component.literal("§c[OpenStage] No ActorEntity found nearby (16 blocks)"));
            return 0;
        }

        long startTick = player.level().getGameTime();
        nearest.playClip(clipId, startTick, true);
        source.sendSuccess(() -> Component.literal("§a[OpenStage] Playing clip '" + clipId + "' on actor " + nearest.getUUID()), true);
        return 1;
    }
}

