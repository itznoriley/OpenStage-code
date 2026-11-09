package net.openstage.client.audio;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.sounds.JOrbisAudioStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public record AudioTrackData(
        String filePath,
        String fileName,
        float duration,
        float[] waveformPeaks,
        byte[] pcmData,
        AudioFormat format,
        AudioTrackData original
) {
    public static final float MAX_DURATION_SECONDS = 180.0f;

    public AudioTrackData(String filePath, String fileName, float duration, float[] waveformPeaks, byte[] pcmData, AudioFormat format) {
        this(filePath, fileName, duration, waveformPeaks, pcmData, format, null);
    }

    public static AudioTrackData loadAudio(File file) throws IOException {
        String lower = file.getName().toLowerCase();
        if (lower.endsWith(".wav")) {
            return loadFromWav(file);
        }
        return loadFromOgg(file);
    }

    public static AudioTrackData loadFromWav(File file) throws IOException {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            AudioFormat baseFormat = ais.getFormat();
            AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );
            AudioInputStream pcmStream = AudioSystem.getAudioInputStream(format, ais);
            int sampleRate = (int) format.getSampleRate();
            int channels = format.getChannels();
            int maxSamples = (int) (MAX_DURATION_SECONDS * sampleRate * channels);
            FloatArrayList allSamples = new FloatArrayList();
            byte[] buf = new byte[4096];
            int read;
            while ((read = pcmStream.read(buf)) != -1) {
                int samplesInBuf = read / 2;
                for (int i = 0; i < samplesInBuf; i++) {
                    short s = (short) ((buf[i * 2] & 0xFF) | (buf[i * 2 + 1] << 8));
                    allSamples.add(s / 32768.0f);
                    if (allSamples.size() >= maxSamples) break;
                }
                if (allSamples.size() >= maxSamples) break;
            }
            pcmStream.close();
            ais.close();

            return buildTrackData(file, format, sampleRate, channels, allSamples);
        } catch (Exception e) {
            throw new IOException("Failed to load WAV audio", e);
        }
    }

    public static AudioTrackData loadFromOgg(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            JOrbisAudioStream stream = new JOrbisAudioStream(fis);
            AudioFormat format = stream.getFormat();
            int sampleRate = (int) format.getSampleRate();
            int channels = format.getChannels();
            int maxSamples = (int) (MAX_DURATION_SECONDS * sampleRate * channels);
            FloatArrayList allSamples = new FloatArrayList();
            FloatConsumer consumer = s -> {
                if (allSamples.size() < maxSamples) {
                    allSamples.add(s);
                }
            };
            while (stream.readChunk(consumer) && allSamples.size() < maxSamples) {}

            return buildTrackData(file, format, sampleRate, channels, allSamples);
        }
    }

    private static AudioTrackData buildTrackData(File file, AudioFormat format, int sampleRate, int channels, FloatArrayList allSamples) {
        int totalSamples = allSamples.size();
        float duration = Math.min(MAX_DURATION_SECONDS, (float) totalSamples / (sampleRate * channels));
        int slices = Math.max(100, (int) (duration * 100));
        float[] peaks = new float[slices];
        int samplesPerSlice = Math.max(1, totalSamples / slices);

        for (int i = 0; i < slices; i++) {
            int start = i * samplesPerSlice;
            int end = Math.min(totalSamples, start + samplesPerSlice);
            float max = 0.0f;
            for (int s = start; s < end; s++) {
                float abs = Math.abs(allSamples.getFloat(s));
                if (abs > max) max = abs;
            }
            peaks[i] = Math.min(1.0f, max);
        }

        byte[] pcm = new byte[totalSamples * 2];
        int pcmIdx = 0;
        for (int s = 0; s < totalSamples; s++) {
            short val = (short) Math.clamp(allSamples.getFloat(s) * 32767.0f, -32768.0f, 32767.0f);
            pcm[pcmIdx++] = (byte) (val & 0xFF);
            pcm[pcmIdx++] = (byte) ((val >> 8) & 0xFF);
        }

        return new AudioTrackData(file.getAbsolutePath(), file.getName(), duration, peaks, pcm, format, null);
    }

    public float getPeakAt(float seconds) {
        if (waveformPeaks == null || waveformPeaks.length == 0 || duration <= 0.0f) return 0.0f;
        int idx = (int) ((seconds / duration) * waveformPeaks.length);
        if (idx < 0) idx = 0;
        if (idx >= waveformPeaks.length) idx = waveformPeaks.length - 1;
        return waveformPeaks[idx];
    }

    public AudioTrackData trimStart(float fromSeconds) {
        return trim(fromSeconds, this.duration);
    }

    public AudioTrackData trimEnd(float toSeconds) {
        return trim(0.0f, toSeconds);
    }

    public AudioTrackData trim(float startSeconds, float endSeconds) {
        if (pcmData == null || pcmData.length == 0 || format == null) return this;
        startSeconds = Math.max(0.0f, Math.min(duration, startSeconds));
        endSeconds = Math.max(startSeconds + 0.1f, Math.min(duration, endSeconds));

        int bytesPerSec = (int) (format.getSampleRate() * format.getChannels() * 2);
        int startByte = Math.max(0, (int) (startSeconds * bytesPerSec));
        startByte = (startByte / 2) * 2;
        int endByte = Math.min(pcmData.length, (int) (endSeconds * bytesPerSec));
        endByte = (endByte / 2) * 2;

        if (endByte <= startByte) return this;

        int newLen = endByte - startByte;
        byte[] newPcm = new byte[newLen];
        System.arraycopy(pcmData, startByte, newPcm, 0, newLen);

        float newDuration = (float) (newLen / 2) / (format.getSampleRate() * format.getChannels());
        int slices = Math.max(100, (int) (newDuration * 100));
        float[] newPeaks = new float[slices];
        int totalSamples = newLen / 2;
        int samplesPerSlice = Math.max(1, totalSamples / slices);

        for (int i = 0; i < slices; i++) {
            int sStart = i * samplesPerSlice;
            int sEnd = Math.min(totalSamples, sStart + samplesPerSlice);
            float max = 0.0f;
            for (int s = sStart; s < sEnd; s++) {
                short val = (short) ((newPcm[s * 2] & 0xFF) | (newPcm[s * 2 + 1] << 8));
                float abs = Math.abs(val / 32768.0f);
                if (abs > max) max = abs;
            }
            newPeaks[i] = Math.min(1.0f, max);
        }

        AudioTrackData orig = (this.original != null) ? this.original : this;
        return new AudioTrackData(filePath, fileName + " (trimmed)", newDuration, newPeaks, newPcm, format, orig);
    }

    public AudioTrackData getRootOriginal() {
        return (original != null) ? original : this;
    }
}
