package net.openstage.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.openstage.OpenStageMod;
import net.openstage.block.ModelBlock;
import net.openstage.client.cinematic.CinematicPlaybackHandler;
import net.openstage.client.network.OpenStageClientNetwork;
import net.openstage.client.renderer.ActorEntityWorldRenderer;
import net.openstage.client.renderer.ModelBlockEntityRenderer;
import net.openstage.client.skin.SkinManager;
import net.openstage.client.ui.OpenStageScreen;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenStageClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("OpenStage/Client");
    private static KeyMapping openEditorKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[OpenStage] Initialising client module …");
        ModelBlock.CLIENT_SCREEN_OPENER = OpenStageScreen::openForBlock;

        openEditorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.openstage.open_editor",
                GLFW.GLFW_KEY_F6,
                "category.openstage"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openEditorKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new OpenStageScreen());
                }
            }

            if (CinematicPlaybackHandler.getInstance().isPlaying()) {
                if (!CinematicPlaybackHandler.getInstance().isExporting()) {
                    CinematicPlaybackHandler.getInstance().tick(0.05f);
                }
                long window = client.getWindow().getWindow();
                if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
                    if (CinematicPlaybackHandler.getInstance().isExporting()) {
                        net.openstage.client.cinematic.VideoExportService.getInstance().cancelExport();
                    }
                    CinematicPlaybackHandler.getInstance().stop();
                }
            }
        });

        OpenStageClientNetwork.registerReceivers();

        EntityRendererRegistry.register(OpenStageMod.ACTOR_ENTITY_TYPE, ActorEntityWorldRenderer::new);
        BlockEntityRendererRegistry.register(OpenStageMod.MODEL_BLOCK_ENTITY_TYPE, ModelBlockEntityRenderer::new);

        LOGGER.info("[OpenStage] Client module initialised successfully.");
    }

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }
}

