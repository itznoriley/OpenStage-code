package net.openstage.client.audio;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AudioPlaybackService {
    private static final AudioPlaybackService INSTANCE = new AudioPlaybackService();

    public static AudioPlaybackService getInstance() {
        return INSTANCE;
    }

    public static final class AudioSegment {
        private AudioTrackData sourceData;
        private float inPoint;
        private float duration;
        private float startOffset;
        private boolean selected;

        public AudioSegment(AudioTrackData sourceData, float inPoint, float duration, float startOffset) {
            this.sourceData = sourceData;
            this.inPoint = Math.max(0.0f, inPoint);
            float maxAvail = (sourceData != null) ? Math.max(0.0f, sourceData.duration() - this.inPoint) : 0.0f;
            this.duration = Math.max(0.05f, Math.min(maxAvail, duration));
            this.startOffset = Math.max(0.0f, startOffset);
            this.selected = false;
        }

        public AudioSegment(AudioTrackData data, float startOffset) {
            this(data, 0.0f, (data != null) ? data.duration() : 0.0f, startOffset);
        }

        public AudioTrackData getSourceData() { return sourceData; }
        public AudioTrackData getData() { return sourceData; }
        public void setData(AudioTrackData data) {
            this.sourceData = data;
            this.inPoint = 0.0f;
            this.duration = (data != null) ? data.duration() : 0.0f;
        }

        public float getInPoint() { return inPoint; }
        public void setInPoint(float inPoint) { this.inPoint = Math.max(0.0f, inPoint); }

        public float getDuration() { return duration; }
        public void setDuration(float duration) {
            float maxAvail = (sourceData != null) ? Math.max(0.05f, sourceData.duration() - this.inPoint) : 0.05f;
            this.duration = Math.max(0.05f, Math.min(maxAvail, duration));
        }

        public float getStartOffset() { return startOffset; }
        public void setStartOffset(float startOffset) { this.startOffset = Math.max(0.0f, startOffset); }

        public float getEndOffset() { return startOffset + duration; }

        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
    }

    private final List<AudioSegment> segments = new CopyOnWriteArrayList<>();
    private AudioSegment selectedSegment = null;
    private AudioTrackData currentTrack = null;
    private SourceDataLine line = null;
    private Thread playThread = null;
    private final AtomicBoolean playing = new AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicInteger playGeneration = new java.util.concurrent.atomic.AtomicInteger(0);
    private float volume = 1.0f;
    private float audioStartOffset = 0.0f;

    private AudioPlaybackService() {}

    public List<AudioSegment> getSegments() {
        return segments;
    }

    public AudioSegment getSelectedSegment() {
        return selectedSegment;
    }

    public void selectSegment(AudioSegment seg) {
        for (AudioSegment s : segments) {
            s.setSelected(s == seg);
        }
        this.selectedSegment = seg;
    }

    public void clearSegmentSelection() {
        for (AudioSegment s : segments) {
            s.setSelected(false);
        }
        this.selectedSegment = null;
    }

    public boolean deleteSelectedSegment() {
        if (selectedSegment != null) {
            stop();
            segments.remove(selectedSegment);
            selectedSegment = null;
            if (segments.isEmpty()) {
                currentTrack = null;
                audioStartOffset = 0.0f;
            } else {
                currentTrack = segments.get(0).getSourceData();
                audioStartOffset = segments.get(0).getStartOffset();
            }
            return true;
        }
        return false;
    }

    public AudioSegment getSegmentAt(float timelineTimeSeconds) {
        for (AudioSegment seg : segments) {
            if (timelineTimeSeconds >= seg.getStartOffset() && timelineTimeSeconds < seg.getEndOffset()) {
                return seg;
            }
        }
        return null;
    }

    public boolean splitAt(float timelineTimeSeconds) {
        for (int i = 0; i < segments.size(); i++) {
            AudioSegment seg = segments.get(i);
            float segStart = seg.getStartOffset();
            float segEnd = seg.getEndOffset();
            if (timelineTimeSeconds > segStart + 0.05f && timelineTimeSeconds < segEnd - 0.05f) {
                stop();
                float splitRel = timelineTimeSeconds - segStart;
                AudioSegment leftSeg = new AudioSegment(seg.getSourceData(), seg.getInPoint(), splitRel, segStart);
                AudioSegment rightSeg = new AudioSegment(seg.getSourceData(), seg.getInPoint() + splitRel, seg.getDuration() - splitRel, timelineTimeSeconds);
                rightSeg.setSelected(true);

                segments.set(i, leftSeg);
                segments.add(i + 1, rightSeg);
                this.selectedSegment = rightSeg;
                return true;
            }
        }
        return false;
    }

    public float getPrevSegmentEnd(AudioSegment seg) {
        float maxEnd = 0.0f;
        for (AudioSegment s : segments) {
            if (s != seg && s.getEndOffset() <= seg.getStartOffset() + 0.05f) {
                if (s.getEndOffset() > maxEnd) {
                    maxEnd = s.getEndOffset();
                }
            }
        }
        return maxEnd;
    }

    public float getNextSegmentStart(AudioSegment seg) {
        float minStart = Float.MAX_VALUE;
        for (AudioSegment s : segments) {
            if (s != seg && s.getStartOffset() >= seg.getEndOffset() - 0.05f) {
                if (s.getStartOffset() < minStart) {
                    minStart = s.getStartOffset();
                }
            }
        }
        return minStart;
    }

    public void moveSegment(AudioSegment seg, float newStartOffset) {
        if (seg == null) return;
        float prevEnd = getPrevSegmentEnd(seg);
        float nextStart = getNextSegmentStart(seg);
        float maxStart = (nextStart < Float.MAX_VALUE) ? (nextStart - seg.getDuration()) : Float.MAX_VALUE;
        newStartOffset = Math.max(prevEnd, Math.min(maxStart, newStartOffset));
        seg.setStartOffset(newStartOffset);
    }

    public void trimSegmentStart(AudioSegment seg, float newStartOffset) {
        if (seg == null || seg.getSourceData() == null) return;
        stop();
        float currentStart = seg.getStartOffset();
        float currentEnd = seg.getEndOffset();
        float minStart = getPrevSegmentEnd(seg);
        newStartOffset = Math.max(minStart, Math.min(currentEnd - 0.05f, newStartOffset));
        float delta = newStartOffset - currentStart;
        float newInPoint = Math.max(0.0f, seg.getInPoint() + delta);
        float newDuration = currentEnd - newStartOffset;
        seg.setInPoint(newInPoint);
        seg.setDuration(newDuration);
        seg.setStartOffset(newStartOffset);
    }

    public void trimSegmentEnd(AudioSegment seg, float newEndOffset) {
        if (seg == null || seg.getSourceData() == null) return;
        stop();
        float currentStart = seg.getStartOffset();
        float maxEnd = getNextSegmentStart(seg);
        float maxDuration = seg.getSourceData().duration() - seg.getInPoint();
        float ceilingEnd = Math.min(maxEnd, currentStart + maxDuration);
        newEndOffset = Math.max(currentStart + 0.05f, Math.min(ceilingEnd, newEndOffset));
        float newDuration = newEndOffset - currentStart;
        seg.setDuration(newDuration);
    }

    public float getTotalDuration() {
        float maxEnd = 0.0f;
        for (AudioSegment seg : segments) {
            maxEnd = Math.max(maxEnd, seg.getEndOffset());
        }
        return maxEnd;
    }

    public AudioTrackData getCurrentTrack() {
        if (selectedSegment != null) return selectedSegment.getSourceData();
        if (!segments.isEmpty()) return segments.get(0).getSourceData();
        return currentTrack;
    }

    public void setTrack(AudioTrackData track) {
        stop();
        this.currentTrack = track;
        this.audioStartOffset = 0.0f;
        this.segments.clear();
        this.selectedSegment = null;
        if (track != null) {
            AudioSegment seg = new AudioSegment(track, 0.0f);
            this.segments.add(seg);
            this.selectedSegment = seg;
        }
    }

    public float getAudioStartOffset() {
        if (selectedSegment != null) return selectedSegment.getStartOffset();
        if (!segments.isEmpty()) return segments.get(0).getStartOffset();
        return audioStartOffset;
    }

    public void setAudioStartOffset(float audioStartOffset) {
        this.audioStartOffset = Math.max(0.0f, audioStartOffset);
        if (selectedSegment != null) {
            selectedSegment.setStartOffset(this.audioStartOffset);
        } else if (!segments.isEmpty()) {
            segments.get(0).setStartOffset(this.audioStartOffset);
        }
    }

    public void trimStartAt(float timelineTimeSeconds) {
        splitAt(timelineTimeSeconds);
    }

    public void trimEndAt(float timelineTimeSeconds) {
        splitAt(timelineTimeSeconds);
    }

    public void resetTrim() {
        if (selectedSegment != null && selectedSegment.getSourceData() != null) {
            stop();
            selectedSegment.setInPoint(0.0f);
            selectedSegment.setDuration(selectedSegment.getSourceData().duration());
        }
    }

    public void removeTrack() {
        stop();
        this.currentTrack = null;
        this.segments.clear();
        this.selectedSegment = null;
        this.audioStartOffset = 0.0f;
    }

    public void openFileDialog() {
        openFileDialog(null);
    }

    public void openFileDialog(java.util.function.Consumer<AudioTrackData> onLoaded) {
        Thread thread = new Thread(() -> {
            String selectedPath = null;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(4);
                filters.put(stack.UTF8("*.ogg"));
                filters.put(stack.UTF8("*.OGG"));
                filters.put(stack.UTF8("*.wav"));
                filters.put(stack.UTF8("*.WAV"));
                filters.flip();
                selectedPath = TinyFileDialogs.tinyfd_openFileDialog(
                        "Open Audio File", "", filters, "Audio Files (*.ogg, *.wav)", false
                );
            } catch (Throwable ignored) {}

            if (selectedPath == null || selectedPath.isBlank()) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Select Audio Track");
                    chooser.setFileFilter(new FileNameExtensionFilter("Audio Files (*.ogg, *.wav)", "ogg", "wav"));
                    int result = chooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                        selectedPath = chooser.getSelectedFile().getAbsolutePath();
                    }
                } catch (Throwable ignored) {}
            }

            if (selectedPath != null && !selectedPath.isBlank()) {
                try {
                    File file = new File(selectedPath);
                    if (file.exists()) {
                        AudioTrackData data = AudioTrackData.loadAudio(file);
                        setTrack(data);
                        if (onLoaded != null) {
                            onLoaded.accept(data);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }, "OpenStage-AudioFileLoader");
        thread.setDaemon(true);
        thread.start();
    }

    public void playFrom(float timelineTimeSeconds) {
        stop();
        final int currentGen = playGeneration.incrementAndGet();
        AudioSegment activeSeg = getSegmentAt(timelineTimeSeconds);
        if (activeSeg == null) {
            return;
        }

        AudioTrackData track = activeSeg.getSourceData();
        if (track == null || track.pcmData() == null || track.pcmData().length == 0) {
            return;
        }

        float relTime = timelineTimeSeconds - activeSeg.getStartOffset();
        float sourceTime = activeSeg.getInPoint() + relTime;
        float remainingDuration = activeSeg.getDuration() - relTime;
        if (remainingDuration <= 0.01f || sourceTime >= track.duration()) {
            return;
        }

        playing.set(true);

        playThread = new Thread(() -> {
            SourceDataLine currentLine = null;
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, track.format());
                currentLine = (SourceDataLine) AudioSystem.getLine(info);
                currentLine.open(track.format());
                currentLine.start();
                this.line = currentLine;

                int bytesPerSec = (int) (track.format().getSampleRate() * track.format().getChannels() * 2);
                int startOffset = Math.max(0, (int) (sourceTime * bytesPerSec));
                startOffset = (startOffset / 4) * 4;
                int maxBytesToPlay = (int) (remainingDuration * bytesPerSec);
                maxBytesToPlay = (maxBytesToPlay / 4) * 4;

                byte[] pcm = track.pcmData();
                int offset = startOffset;
                int endLimit = Math.min(pcm.length, startOffset + maxBytesToPlay);
                int bufferSize = 4096;

                while (playing.get() && playGeneration.get() == currentGen && offset < endLimit) {
                    int toWrite = Math.min(bufferSize, endLimit - offset);
                    currentLine.write(pcm, offset, toWrite);
                    offset += toWrite;
                }

                if (playing.get() && playGeneration.get() == currentGen) {
                    currentLine.drain();
                }
            } catch (Exception ignored) {
            } finally {
                if (currentLine != null) {
                    try {
                        currentLine.stop();
                        currentLine.close();
                    } catch (Exception ignored) {}
                }
                if (playGeneration.get() == currentGen) {
                    this.line = null;
                    playing.set(false);
                }
            }
        }, "OpenStage-AudioPlayer");
        playThread.setDaemon(true);
        playThread.start();
    }

    public void stop() {
        playGeneration.incrementAndGet();
        playing.set(false);
        stopLine();
        if (playThread != null) {
            try {
                playThread.interrupt();
            } catch (Exception ignored) {}
            playThread = null;
        }
    }

    private void stopLine() {
        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (Exception ignored) {}
            line = null;
        }
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = Math.clamp(volume, 0.0f, 1.0f);
    }
}
