package net.openstage.io;

import net.openstage.OpenStageMod;
import net.openstage.core.animation.AnimationClip;
import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FileWatcherService {
    private static final long DEBOUNCE_MS = 150;

    @FunctionalInterface
    public interface AnimationReloadCallback {
        void onAnimationChanged(String fileName, AnimationClip clip);
    }

    private volatile AnimationReloadCallback callback;
    private volatile Path watchDir;
    private volatile Thread watchThread;
    private volatile WatchService watchService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void setCallback(AnimationReloadCallback callback) {
        this.callback = callback;
    }

    public void start(Path animationsDir) throws IOException {
        Objects.requireNonNull(animationsDir, "animationsDir must not be null");
        if (running.getAndSet(true)) {
            OpenStageMod.LOGGER.warn("[OpenStage] FileWatcherService is already running");
            return;
        }

        if (!Files.isDirectory(animationsDir)) {
            Files.createDirectories(animationsDir);
            OpenStageMod.LOGGER.info("[OpenStage] Created animations directory: {}", animationsDir);
        }

        this.watchDir = animationsDir;
        this.watchService = animationsDir.getFileSystem().newWatchService();

        animationsDir.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        watchThread = new Thread(this::watchLoop, "OpenStage-FileWatcher");
        watchThread.setDaemon(true);
        watchThread.start();

        OpenStageMod.LOGGER.info("[OpenStage] File watcher started on {}", animationsDir);
    }

    public void stop() {
        if (!running.getAndSet(false)) return;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            OpenStageMod.LOGGER.warn("[OpenStage] Error closing watch service", e);
        }
        if (watchThread != null) {
            watchThread.interrupt();
        }
        OpenStageMod.LOGGER.info("[OpenStage] File watcher stopped");
    }

    public void forceReload() {
        Path dir = watchDir;
        if (dir == null || !Files.isDirectory(dir)) {
            OpenStageMod.LOGGER.warn("[OpenStage] Cannot force reload – no directory configured");
            return;
        }
        OpenStageMod.LOGGER.info("[OpenStage] Force-reloading all animations in {}", dir);
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".osanim.json"))
                  .forEach(p -> reloadFile(p.getFileName().toString()));
        } catch (IOException e) {
            OpenStageMod.LOGGER.error("[OpenStage] Error during force reload", e);
        }
    }

    private void watchLoop() {
        long lastEventTime = 0;
        String lastFileName = null;

        while (running.get()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                String fileName = pathEvent.context().toString();

                if (!fileName.endsWith(".osanim.json")) continue;
                long now = System.currentTimeMillis();

                if (fileName.equals(lastFileName) && (now - lastEventTime) < DEBOUNCE_MS) {
                    continue;
                }
                lastFileName = fileName;
                lastEventTime = now;

                reloadFile(fileName);
            }

            boolean valid = key.reset();
            if (!valid) {
                OpenStageMod.LOGGER.warn("[OpenStage] Watch key no longer valid – stopping watcher");
                break;
            }
        }
        running.set(false);
    }

    private void reloadFile(String fileName) {
        Path filePath = watchDir.resolve(fileName);
        try {
            String json = Files.readString(filePath);
            AnimationClip clip = AnimationSerializer.fromJson(json);
            OpenStageMod.LOGGER.info("[OpenStage] Reloaded animation: {}", fileName);
            AnimationReloadCallback cb = this.callback;
            if (cb != null) {
                cb.onAnimationChanged(fileName, clip);
            }
        } catch (Exception e) {
            OpenStageMod.LOGGER.error("[OpenStage] Failed to reload '{}': {}", fileName, e.getMessage());
        }
    }
}

