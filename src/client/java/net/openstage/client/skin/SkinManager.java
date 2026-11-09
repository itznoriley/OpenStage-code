package net.openstage.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.openstage.OpenStageMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class SkinManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("OpenStage/SkinManager");
    private static final SkinManager INSTANCE = new SkinManager();

    public static final ResourceLocation STEVE_SKIN =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    public static final ResourceLocation ALEX_SKIN =
            ResourceLocation.withDefaultNamespace("textures/entity/player/slim/alex.png");

    private final Path skinsDirectory;
    private final Map<String, SkinEntry> loadedSkins = new LinkedHashMap<>();

    public record SkinEntry(String name, ResourceLocation location, boolean isSlim, int width, int height) {
        public SkinEntry(String name, ResourceLocation location, boolean isSlim) {
            this(name, location, isSlim, 64, 64);
        }
    }

    private SkinManager() {
        this.skinsDirectory = FabricLoader.getInstance().getGameDir()
                .resolve("openstage")
                .resolve("skins");
        ensureDirectoryExists();
    }

    public static SkinManager getInstance() {
        return INSTANCE;
    }

    public void ensureDirectoryExists() {
        try {
            if (!Files.exists(skinsDirectory)) {
                Files.createDirectories(skinsDirectory);
                LOGGER.info("[OpenStage] Created skins directory at: {}", skinsDirectory.toAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("[OpenStage] Failed to create skins directory", e);
        }
    }

    public Path getSkinsDirectory() {
        return skinsDirectory;
    }

    public void openSkinsFolder() {
        try {
            ensureDirectoryExists();
            Util.getPlatform().openPath(skinsDirectory);
        } catch (Throwable t) {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(skinsDirectory.toFile());
                }
            } catch (Throwable ignored) {}
        }
    }

    public void refreshSkins() {
        ensureDirectoryExists();
        File folder = skinsDirectory.toFile();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".png"));

        if (files == null || files.length == 0) {
            LOGGER.info("[OpenStage] No custom skins found in {}", skinsDirectory.toAbsolutePath());
            return;
        }

        for (File file : files) {
            String fileName = file.getName();
            String skinKey = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);

            if (loadedSkins.containsKey(skinKey)) {
                continue;
            }

            try (InputStream in = new FileInputStream(file)) {
                NativeImage image = NativeImage.read(in);
                int width = image.getWidth();
                int height = image.getHeight();

                if (width != 64 || (height != 64 && height != 32)) {
                    LOGGER.warn("[OpenStage] Skin {} has unsupported dimensions: {}x{}. Expected 64x64 or 64x32.",
                            fileName, width, height);
                    image.close();
                    continue;
                }

                boolean isSlim = false;
                if (height == 64) {
                    int pixel = image.getPixelRGBA(54, 20);
                    int alpha = (pixel >> 24) & 0xFF;
                    if (alpha == 0) {
                        isSlim = true;
                    }
                }

                String safeId = skinKey.replaceAll("[^a-z0-9_.-]", "_");
                ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(
                        OpenStageMod.MOD_ID, "skin_" + safeId
                );
                DynamicTexture dynTex = new DynamicTexture(image);
                Minecraft.getInstance().getTextureManager().register(texLoc, dynTex);
                SkinEntry entry = new SkinEntry(skinKey, texLoc, isSlim, width, height);
                loadedSkins.put(skinKey, entry);
                LOGGER.info("[OpenStage] Loaded custom skin '{}' (slim={}) from {}", skinKey, isSlim, fileName);
            } catch (Exception e) {
                LOGGER.error("[OpenStage] Failed to load skin from " + file.getAbsolutePath(), e);
            }
        }
    }

    public String getSkinDimensions(String skinName) {
        if (skinName != null) {
            SkinEntry entry = loadedSkins.get(skinName);
            if (entry != null && entry.width() > 0 && entry.height() > 0) {
                return entry.width() + "x" + entry.height() + " px";
            }
        }
        return "64x64 px";
    }

    public ResourceLocation getSkinTexture(String skinName, boolean preferSlim) {
        if (skinName == null || skinName.isBlank() || skinName.equalsIgnoreCase("steve")) {
            return preferSlim ? ALEX_SKIN : STEVE_SKIN;
        }
        if (skinName.equalsIgnoreCase("alex")) {
            return ALEX_SKIN;
        }

        String key = skinName.toLowerCase(Locale.ROOT);
        SkinEntry entry = loadedSkins.get(key);
        if (entry != null) {
            return entry.location();
        }

        return preferSlim ? ALEX_SKIN : STEVE_SKIN;
    }

    public boolean isSkinSlim(String skinName) {
        if (skinName == null) return false;
        if (skinName.equalsIgnoreCase("alex")) return true;
        SkinEntry entry = loadedSkins.get(skinName.toLowerCase(Locale.ROOT));
        return entry != null && entry.isSlim();
    }

    public List<String> getAvailableSkinNames() {
        refreshSkins();
        List<String> list = new ArrayList<>();
        list.add("steve");
        list.add("alex");
        list.addAll(loadedSkins.keySet());
        return list;
    }
}

