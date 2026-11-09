package net.openstage.client.cinematic;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.openstage.core.animation.AnimationClip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public final class VideoExportService {
    private static final Logger LOGGER = LoggerFactory.getLogger("OpenStage/VideoExport");
    private static final VideoExportService INSTANCE = new VideoExportService();
    private boolean isExporting = false;
    private int currentFrame = 0;
    private int totalFrames = 0;
    private int fps = 60;
    private int exportWidth = 1920;
    private int exportHeight = 1080;
    private Process ffmpegProcess;
    private OutputStream ffmpegStdin;
    private File outputFolder;
    private byte[] pixelBuffer;

    private VideoExportService() {}

    public static VideoExportService getInstance() {
        return INSTANCE;
    }

    public boolean isExporting() {
        return isExporting;
    }

    public int getFps() {
        return fps;
    }

    public float getProgress() {
        if (totalFrames <= 0) return 0.0f;
        return (float) currentFrame / (float) totalFrames;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public int getExportWidth() {
        return exportWidth;
    }

    public int getExportHeight() {
        return exportHeight;
    }

    public void startExport(AnimationClip clip, int width, int height, int fps, boolean useFfmpegMp4, int bitrateMbps) {
        if (clip == null) return;
        this.isExporting = true;
        this.currentFrame = 0;
        this.fps = fps;
        this.exportWidth = width;
        this.exportHeight = height;
        this.totalFrames = Math.max(1, (int) Math.ceil(clip.getLength() * fps));
        String sanitizedName = clip.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
        this.outputFolder = new File("openstage/exports/" + sanitizedName);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        if (useFfmpegMp4) {
            try {
                File mp4File = new File(outputFolder, sanitizedName + ".mp4");
                ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg",
                        "-y",
                        "-f", "rawvideo",
                        "-vcodec", "rawvideo",
                        "-s", width + "x" + height,
                        "-pix_fmt", "rgba",
                        "-framerate", String.valueOf(fps),
                        "-i", "-",
                        "-c:v", "libx264",
                        "-preset", "fast",
                        "-b:v", bitrateMbps + "M",
                        "-maxrate", bitrateMbps + "M",
                        "-bufsize", (bitrateMbps * 2) + "M",
                        "-pix_fmt", "yuv420p",
                        "-g", String.valueOf(fps),
                        "-movflags", "+faststart",
                        mp4File.getAbsolutePath()
                );
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                this.ffmpegProcess = pb.start();
                this.ffmpegStdin = ffmpegProcess.getOutputStream();
            } catch (Exception e) {
                LOGGER.warn("FFmpeg not found on PATH, falling back to PNG Image Sequence", e);
                this.ffmpegProcess = null;
                this.ffmpegStdin = null;
            }
        } else {
            this.ffmpegProcess = null;
            this.ffmpegStdin = null;
        }
    }

    public void pushFrame(NativeImage image) {
        if (!isExporting || image == null) {
            if (image != null) {
                image.close();
            }
            return;
        }

        try {
            if (ffmpegStdin != null) {
                int w = image.getWidth();
                int h = image.getHeight();
                int totalBytes = w * h * 4;
                if (pixelBuffer == null || pixelBuffer.length != totalBytes) {
                    pixelBuffer = new byte[totalBytes];
                }
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int pixel = image.getPixelRGBA(x, y);
                        int idx = (y * w + x) * 4;
                        pixelBuffer[idx] = (byte) (pixel & 0xFF);
                        pixelBuffer[idx + 1] = (byte) ((pixel >> 8) & 0xFF);
                        pixelBuffer[idx + 2] = (byte) ((pixel >> 16) & 0xFF);
                        pixelBuffer[idx + 3] = (byte) ((pixel >> 24) & 0xFF);
                    }
                }
                ffmpegStdin.write(pixelBuffer);
            } else {
                File frameFile = new File(outputFolder, String.format("frame_%05d.png", currentFrame));
                image.writeToFile(frameFile);
            }
        } catch (Exception e) {
            LOGGER.error("Failed writing export frame " + currentFrame, e);
        } finally {
            image.close();
        }

        currentFrame++;
        if (currentFrame >= totalFrames) {
            finishExport();
        }
    }

    public void finishExport() {
        this.isExporting = false;
        if (ffmpegStdin != null) {
            try {
                ffmpegStdin.flush();
                ffmpegStdin.close();
            } catch (Exception ignored) {}
            ffmpegStdin = null;
        }
        if (ffmpegProcess != null) {
            try {
                ffmpegProcess.waitFor(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
            ffmpegProcess = null;
        }
        if (outputFolder != null && outputFolder.exists()) {
            Util.getPlatform().openFile(outputFolder);
        }
    }

    public void cancelExport() {
        this.isExporting = false;
        if (ffmpegStdin != null) {
            try { ffmpegStdin.close(); } catch (Exception ignored) {}
            ffmpegStdin = null;
        }
        if (ffmpegProcess != null) {
            ffmpegProcess.destroyForcibly();
            ffmpegProcess = null;
        }
    }
}

