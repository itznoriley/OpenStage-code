package net.openstage.client.model;

import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ObjModelManager {
    private static final ObjModelManager INSTANCE = new ObjModelManager();
    private final Map<String, ObjModel> cache = new HashMap<>();
    private final Path modelsDirectory;

    private ObjModelManager() {
        this.modelsDirectory = FabricLoader.getInstance().getGameDir()
                .resolve("openstage")
                .resolve("models");
        ensureDirectoryExists();
    }

    public static ObjModelManager getInstance() {
        return INSTANCE;
    }

    private String stripPrefix(String name) {
        if (name == null) return "";
        String s = name.trim();
        if (s.startsWith("custom:")) s = s.substring(7);
        else if (s.startsWith("player:")) s = s.substring(7);
        else if (s.startsWith("skin:")) s = s.substring(5);
        else if (s.startsWith("model:")) s = s.substring(6);
        return s;
    }

    private void ensureDirectoryExists() {
        File dir = modelsDirectory.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public boolean hasModel(String name) {
        if (name == null || name.isEmpty()) return false;
        String key = normalizeKey(name);
        if (cache.containsKey(key)) return true;
        File file = findModelFile(name);
        return file != null && file.exists();
    }

    public ObjModel getOrLoadModel(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = normalizeKey(name);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        File file = findModelFile(name);
        if (file == null || !file.exists()) {
            return null;
        }

        ObjModel model = ObjModel.load(file);
        if (model != null) {
            cache.put(key, model);
            cache.put(file.getName().toLowerCase(Locale.ROOT), model);
            if (file.getName().toLowerCase(Locale.ROOT).endsWith(".obj")) {
                String noExt = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                cache.put(noExt, model);
            }
        }
        return model;
    }

    private File findModelFile(String name) {
        File dir = modelsDirectory.toFile();
        if (!dir.exists()) return null;
        String clean = stripPrefix(name);
        File direct = new File(dir, clean);
        if (direct.exists() && direct.isFile() && direct.getName().toLowerCase(Locale.ROOT).endsWith(".obj")) {
            return direct;
        }

        if (!clean.toLowerCase(Locale.ROOT).endsWith(".obj")) {
            File withObj = new File(dir, clean + ".obj");
            if (withObj.exists() && withObj.isFile()) {
                return withObj;
            }
        }

        File[] files = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".obj"));
        if (files != null) {
            String lower = clean.toLowerCase(Locale.ROOT);
            for (File f : files) {
                if (f.getName().toLowerCase(Locale.ROOT).equalsIgnoreCase(lower)
                        || f.getName().toLowerCase(Locale.ROOT).equalsIgnoreCase(lower + ".obj")) {
                    return f;
                }
            }
            for (File f : files) {
                String base = f.getName().substring(0, f.getName().length() - 4).toLowerCase(Locale.ROOT);
                if (base.equalsIgnoreCase(lower) || lower.contains(base) || base.contains(lower)) {
                    return f;
                }
            }
        }

        return null;
    }

    private String normalizeKey(String name) {
        String k = stripPrefix(name).toLowerCase(Locale.ROOT).trim();
        if (k.endsWith(".obj")) {
            k = k.substring(0, k.length() - 4);
        }
        return k;
    }

    public void clearCache() {
        cache.clear();
    }
}

