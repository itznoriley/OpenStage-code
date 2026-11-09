package net.openstage.io;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.openstage.core.animation.AnimationClip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AnimationFileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("OpenStage/FileManager");
    private static final AnimationFileManager INSTANCE = new AnimationFileManager();
    private final Path animationsDirectory;
    private final Path posesDirectory;

    private AnimationFileManager() {
        Path baseDir = FabricLoader.getInstance().getGameDir().resolve("openstage");
        this.animationsDirectory = baseDir.resolve("animations");
        this.posesDirectory = baseDir.resolve("poses");
        ensureDirectoryExists();
    }

    public static AnimationFileManager getInstance() {
        return INSTANCE;
    }

    public void ensureDirectoryExists() {
        try {
            if (!Files.exists(animationsDirectory)) {
                Files.createDirectories(animationsDirectory);
                LOGGER.info("[OpenStage] Created animations folder at: {}", animationsDirectory.toAbsolutePath());
            }
            if (!Files.exists(posesDirectory)) {
                Files.createDirectories(posesDirectory);
                LOGGER.info("[OpenStage] Created poses folder at: {}", posesDirectory.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("[OpenStage] Failed to create openstage folders", e);
        }
    }

    public Path getAnimationsDirectory() {
        return animationsDirectory;
    }

    public Path getPosesDirectory() {
        return posesDirectory;
    }

    public void openAnimationsFolder() {
        try {
            ensureDirectoryExists();
            Util.getPlatform().openPath(animationsDirectory);
        } catch (Throwable t) {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(animationsDirectory.toFile());
                }
            } catch (Throwable ignored) {}
        }
    }

    public void openPosesFolder() {
        try {
            ensureDirectoryExists();
            Util.getPlatform().openPath(posesDirectory);
        } catch (Throwable t) {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(posesDirectory.toFile());
                }
            } catch (Throwable ignored) {}
        }
    }

    public List<String> listAnimationFiles() {
        ensureDirectoryExists();
        List<String> list = new ArrayList<>();
        File folder = animationsDirectory.toFile();
        File[] files = folder.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".osanim.json") || (!lower.endsWith(".ospose.json") && lower.endsWith(".json"));
        });

        if (files != null) {
            for (File file : files) {
                list.add(file.getName());
            }
        }
        return list;
    }

    public List<String> listPoseFiles() {
        ensureDirectoryExists();
        List<String> list = new ArrayList<>();
        File folder = posesDirectory.toFile();
        File[] files = folder.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".ospose.json") || lower.endsWith(".json");
        });

        if (files != null) {
            for (File file : files) {
                list.add(file.getName());
            }
        }
        return list;
    }

    public boolean saveAnimation(AnimationClip clip, String fileName) {
        if (clip == null || fileName == null || fileName.isBlank()) return false;
        ensureDirectoryExists();
        String lower = fileName.toLowerCase(Locale.ROOT);
        boolean isPose = lower.endsWith(".ospose.json");
        if (!lower.endsWith(".osanim.json") && !isPose) {
            fileName += ".osanim.json";
        }

        Path target = (isPose ? posesDirectory : animationsDirectory).resolve(fileName);
        try {
            String json = AnimationSerializer.toJson(clip);
            Files.writeString(target, json);
            LOGGER.info("[OpenStage] Saved {} '{}' to {}", isPose ? "pose" : "animation", clip.getName(), target.toAbsolutePath());
            return true;
        } catch (IOException e) {
            LOGGER.error("[OpenStage] Failed to save to " + target.toAbsolutePath(), e);
            return false;
        }
    }

    public boolean savePose(AnimationClip clip, String fileName) {
        if (clip == null || fileName == null || fileName.isBlank()) return false;
        ensureDirectoryExists();
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".ospose.json")) {
            fileName += ".ospose.json";
        }
        return saveAnimation(clip, fileName);
    }

    public AnimationClip loadAnimation(String fileName) {
        ensureDirectoryExists();
        String lower = fileName.toLowerCase(Locale.ROOT);
        Path primary = lower.endsWith(".ospose.json") ? posesDirectory.resolve(fileName) : animationsDirectory.resolve(fileName);
        Path secondary = lower.endsWith(".ospose.json") ? animationsDirectory.resolve(fileName) : posesDirectory.resolve(fileName);

        Path source = Files.exists(primary) ? primary : (Files.exists(secondary) ? secondary : null);

        if (source == null) {
            Path animWithExt = animationsDirectory.resolve(fileName + ".osanim.json");
            Path poseWithExt = posesDirectory.resolve(fileName + ".ospose.json");
            if (Files.exists(animWithExt)) {
                source = animWithExt;
            } else if (Files.exists(poseWithExt)) {
                source = poseWithExt;
            } else {
                LOGGER.warn("[OpenStage] File not found: {}", fileName);
                return null;
            }
        }

        try {
            String json = Files.readString(source);
            AnimationClip clip = AnimationSerializer.fromJson(json);
            LOGGER.info("[OpenStage] Loaded animation '{}' from {}", clip.getName(), source.toAbsolutePath());
            return clip;
        } catch (Exception e) {
            LOGGER.error("[OpenStage] Failed to load animation from " + source.toAbsolutePath(), e);
            return null;
        }
    }

    public AnimationClip loadPose(String fileName) {
        return loadAnimation(fileName);
    }

    public boolean deleteFile(String fileName) {
        if (fileName == null || fileName.isBlank()) return false;
        ensureDirectoryExists();
        String lower = fileName.toLowerCase(Locale.ROOT);
        Path target = lower.endsWith(".ospose.json") ? posesDirectory.resolve(fileName) : animationsDirectory.resolve(fileName);
        if (!Files.exists(target)) {
            target = lower.endsWith(".ospose.json") ? animationsDirectory.resolve(fileName) : posesDirectory.resolve(fileName);
        }
        if (Files.exists(target)) {
            try {
                Files.delete(target);
                LOGGER.info("[OpenStage] Deleted file: {}", target.toAbsolutePath());
                return true;
            } catch (IOException e) {
                LOGGER.error("[OpenStage] Failed to delete file: " + target.toAbsolutePath(), e);
            }
        }
        return false;
    }

    public boolean duplicateFile(String fileName) {
        if (fileName == null || fileName.isBlank()) return false;
        AnimationClip clip = loadAnimation(fileName);
        if (clip == null) return false;
        String lower = fileName.toLowerCase(Locale.ROOT);
        boolean isPose = lower.endsWith(".ospose.json");
        String baseName = fileName.replace(".osanim.json", "").replace(".ospose.json", "");
        String newName = baseName + "_copy";
        int counter = 2;
        Path dir = isPose ? posesDirectory : animationsDirectory;
        String ext = isPose ? ".ospose.json" : ".osanim.json";
        while (Files.exists(dir.resolve(newName + ext))) {
            newName = baseName + "_copy" + counter;
            counter++;
        }
        clip.setName(newName);
        return isPose ? savePose(clip, newName + ext) : saveAnimation(clip, newName + ext);
    }

    public boolean renameProjectFile(String oldName, String newName) {
        if (oldName == null || newName == null || oldName.isBlank() || newName.isBlank()) return false;
        ensureDirectoryExists();
        String lowerOld = oldName.toLowerCase(Locale.ROOT);
        boolean isPose = lowerOld.endsWith(".ospose.json");
        Path source = isPose ? posesDirectory.resolve(oldName) : animationsDirectory.resolve(oldName);
        if (!Files.exists(source)) {
            source = isPose ? animationsDirectory.resolve(oldName) : posesDirectory.resolve(oldName);
        }
        if (!Files.exists(source)) return false;

        String ext = isPose ? ".ospose.json" : ".osanim.json";
        String safeNew = newName.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        if (!safeNew.toLowerCase(Locale.ROOT).endsWith(ext)) {
            safeNew += ext;
        }
        Path target = (isPose ? posesDirectory : animationsDirectory).resolve(safeNew);
        try {
            Files.move(source, target);
            LOGGER.info("[OpenStage] Renamed '{}' to '{}'", oldName, safeNew);
            return true;
        } catch (IOException e) {
            LOGGER.error("[OpenStage] Failed to rename file from " + source + " to " + target, e);
            return false;
        }
    }

    public void openFileFolder(String fileName) {
        if (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".ospose.json")) {
            openPosesFolder();
        } else {
            openAnimationsFolder();
        }
    }
}

