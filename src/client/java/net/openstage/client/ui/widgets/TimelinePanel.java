package net.openstage.client.ui.widgets;

import net.minecraft.client.Minecraft;
import net.openstage.client.model.ModelSettings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.openstage.client.audio.AudioPlaybackService;
import net.openstage.client.ui.I18n;
import net.openstage.client.audio.AudioTrackData;
import net.openstage.core.animation.AnimationClip;
import net.openstage.core.animation.BoneTrack;
import net.openstage.core.animation.ChannelType;
import net.openstage.core.animation.Keyframe;
import net.openstage.core.animation.TextCard;
import net.openstage.core.scene.CameraTrack;
import java.util.*;

public class TimelinePanel {
    private static final int COLOR_TIMELINE_BG  = 0xFF141923;
    private static final int COLOR_TOOLBAR_BG   = 0xFF1C2230;
    private static final int COLOR_TOOLBAR_BORDER = 0xFF2A3448;
    private static final int COLOR_RULER_BG     = 0xFF181E2B;
    private static final int COLOR_RULER_TEXT   = 0xFF7D8FA9;
    private static final int COLOR_PLAYHEAD     = 0xFFFF3B30;
    private static final int COLOR_KEYFRAME     = 0xFFFFCC00;
    private static final int COLOR_KEYFRAME_SEL  = 0xFF00FFCC;
    private static final int COLOR_TRACK_LINE   = 0xFF181D28;
    private static final int COLOR_TRACK_LABEL  = 0xFF99AABB;
    private static final int COLOR_TEXT_CARD_BG  = 0xCC2B5C8F;
    private static final int COLOR_TEXT_CARD_SEL = 0xFF4A90E2;
    public static final int TOOLBAR_HEIGHT = 22;
    public static final int RULER_HEIGHT = 18;
    public static final int TRACK_HEIGHT = 16;
    private static final int DIAMOND_SIZE = 4;
    public static final net.minecraft.resources.ResourceLocation ICONS_TEXTURE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("openstage", "textures/gui/icons.png");
    private static final int TRACK_HEADER_WIDTH = 120;
    private net.openstage.client.model.ModelSettings settings;

    public void setSettings(net.openstage.client.model.ModelSettings settings) {
        this.settings = settings;

        updateActiveBones();
    }

    public net.openstage.client.model.ModelSettings getSettings() {
        return settings;
    }

    private int x, y, width, height;
    private int fps;
    private int currentFrame;
    private boolean playing;
    private long lastTimeNano;
    private float pixelsPerFrame = 6.0f;
    private float scrollOffset = 0.0f;
    private AudioPlaybackService.AudioSegment draggingAudioSegment = null;
    private AudioTrackData dragAudioBaseData = null;
    private int draggingAudioHandle = 0;
    private long lastAudioClickTime = 0L;
    private AudioPlaybackService.AudioSegment lastAudioClickedSeg = null;
    private double dragAudioStartX;
    private float dragAudioOriginalOffset;
    private float dragAudioOriginalDuration;
    private int timelineCapacityFrames = 1000;

    public void checkAutoExtendTimeline() {
        int highest = Math.max(currentFrame, (clip != null) ? (int) (clip.getLength() * fps) : 0);
        while (highest >= timelineCapacityFrames - 50) {
            timelineCapacityFrames += 1000;
            if (clip != null && clip.getLength() < (float) timelineCapacityFrames / fps) {
                clip.setLength((float) timelineCapacityFrames / fps);
            }
        }
    }
    private boolean draggingPlayhead;
    private boolean draggingZoomSlider;
    private AnimationClip clip;

    public record KeyframeRef(String bone, ChannelType channel, float time, Keyframe keyframe) {}

    private final Set<KeyframeRef> selectedKeyframes = new HashSet<>();
    private boolean isDraggingKeyframes = false;
    private double dragKeyframesStartX;
    private float currentDragDeltaTime = 0.0f;

    private record DraggedKeyRecord(String bone, ChannelType channel, float originalTime, Keyframe keyframe, CameraTrack.CameraKeyframe camKey, net.openstage.core.particle.ParticleKeyframe particleKey) {}

    private final List<DraggedKeyRecord> activeDraggedKeys = new ArrayList<>();
    private boolean isBoxSelecting = false;
    private double boxStartX, boxStartY, boxCurrentX, boxCurrentY;

    public static final String[] STANDARD_BONES = {
            "root", "body", "head", "right_arm", "left_arm", "right_leg", "left_leg"
    };

    public static final String[] BENDABLE_BONES = {
            "root", "body", "head",

            "right_arm", "right_forearm",

            "left_arm", "left_forearm",

            "right_leg", "right_lower_leg",

            "left_leg", "left_lower_leg"
    };
    public static final String[] TRACK_BONES = STANDARD_BONES;
    private String[] activeTrackBones = STANDARD_BONES;
    private int verticalTrackScrollOffset = 0;

    public void updateActiveBones() {
        boolean bendable = (settings != null && settings.isBendableLimbs());
        boolean hasEyes = false;
        String[] base = bendable ? BENDABLE_BONES : STANDARD_BONES;

        if (!hasEyes) {
            this.activeTrackBones = base;
        } else {
            String[] withEyes = new String[base.length + 1];

            System.arraycopy(base, 0, withEyes, 0, base.length);
            withEyes[base.length] = "eyes";
            this.activeTrackBones = withEyes;
        }
    }

    private net.openstage.client.theme.EditorTheme theme = null;
    private String modelName = "Actor 1";
    private boolean modelTreeExpanded = false;
    private float treeAnim = 0.0f;
    private float treeTarget = 0.0f;
    private long lastAnimTime = 0;

    public void setTheme(net.openstage.client.theme.EditorTheme theme) {
        this.theme = theme;
    }

    public void setModelName(String modelName) {
        this.modelName = (modelName != null && !modelName.isBlank()) ? modelName : "Actor 1";
    }

    public String getModelName() {
        return modelName;
    }

    public boolean isModelTreeExpanded() {
        return modelTreeExpanded;
    }

    public void setModelTreeExpanded(boolean modelTreeExpanded) {
        this.modelTreeExpanded = modelTreeExpanded;
        this.treeTarget = modelTreeExpanded ? 1.0f : 0.0f;

        if (settings != null && !settings.isUiAnimations()) {
            this.treeAnim = this.treeTarget;
        }
    }

    public int getExpandedBonesHeight() {
        boolean anims = (settings == null) || settings.isUiAnimations();
        float progress = anims ? treeAnim : (modelTreeExpanded ? 1.0f : 0.0f);

        return Math.round(activeTrackBones.length * TRACK_HEIGHT * progress);
    }

    private int getBgColor() { return (theme != null) ? theme.getPanelBg() : COLOR_TIMELINE_BG; }

    private int getToolbarBg() { return (theme != null) ? theme.getTitleBarBg() : COLOR_TOOLBAR_BG; }

    private int getBorderColor() { return (theme != null) ? theme.getDivider() : COLOR_TOOLBAR_BORDER; }

    private int getTextColor() { return (theme != null) ? theme.getTextColor() : COLOR_TRACK_LABEL; }

    private int getMutedColor() { return (theme != null) ? theme.getTextMuted() : COLOR_RULER_TEXT; }

    private int getAccentColor() { return (theme != null) ? theme.getAccent() : 0xFF38BDF8; }

    private int getPlayheadColor() { return (theme != null) ? theme.getAccent() : COLOR_PLAYHEAD; }

    private net.openstage.client.model.ModelSettings.Language getLanguage() {
        if (settings != null) {
            return settings.getLanguage();
        }
        return net.openstage.client.model.ModelSettings.getGlobalLanguage();
    }

    public interface TimelineListener {
        void onKeyframeSelected(String bone, ChannelType channel, Keyframe keyframe);

        void onTextCardSelected(TextCard card);

        default void onTextCardDoubleClicked(TextCard card) {}

        default void onEnvironmentKeyframeSelected(net.openstage.core.scene.EnvironmentKeyframe keyframe) {}

        default void onAudioSegmentDoubleClicked(AudioPlaybackService.AudioSegment seg) {}
    }

    public interface TimelineActionListener {
        void onAddKeyframe();

        void onDeleteKeyframe();

        void onClearKeyframe();

        void onUndo();

        void onRedo();
        default void onEditStart() {}
        default void onEditEnd() {}

        default void onToggleGraph() {}

        default void onCopyPose() {}

        default void onPastePose() {}

        default void onDuplicatePose() {}

        default void onKeyAll() {}

        default void onMirrorPose() {}

        default void onAddParticlesRequested() {}
        default void onAddTextCardRequested() {}
        default void onAddEnvironmentKeyframeRequested() {}
    }

    private boolean showingGraph = false;

    public boolean isShowingGraph() {
        return showingGraph;
    }

    public void setShowingGraph(boolean showingGraph) {
        this.showingGraph = showingGraph;
    }

    private TimelineListener listener;
    private TimelineActionListener actionListener;
    private TextCard selectedTextCard;
    private long lastCardClickTime = 0L;
    private TextCard lastClickedCard = null;

    public void setSelectedTextCard(TextCard selectedTextCard) {
        this.selectedTextCard = selectedTextCard;
    }

    public TextCard getSelectedTextCard() {
        return selectedTextCard;
    }

    private net.openstage.core.scene.EnvironmentKeyframe selectedEnvironmentKeyframe;

    public void setSelectedEnvironmentKeyframe(net.openstage.core.scene.EnvironmentKeyframe selectedEnvironmentKeyframe) {
        this.selectedEnvironmentKeyframe = selectedEnvironmentKeyframe;
    }

    public net.openstage.core.scene.EnvironmentKeyframe getSelectedEnvironmentKeyframe() {
        return selectedEnvironmentKeyframe;
    }
    private Keyframe selectedKeyframe;

    public void setActionListener(TimelineActionListener actionListener) {
        this.actionListener = actionListener;
    }

    private int selectedActorIndex = 0;

    public void setSelectedActorIndex(int index) {
        this.selectedActorIndex = Math.max(0, index);
    }

    public int getSelectedActorIndex() {
        return selectedActorIndex;
    }

    private BoneTrack getBoneTrack(String boneName) {
        if (clip == null || boneName == null) return null;
        BoneTrack direct = clip.getBones().get(boneName);

        if (direct != null && !clip.hasActorPrefixes()) return direct;

        if (clip.hasActorPrefixes()) {
            BoneTrack t = clip.getBones().get("actor" + selectedActorIndex + ":" + boneName);

            if (t != null) return t;

            if (selectedActorIndex == 0) {
                t = clip.getBones().get("noob:" + boneName);

                if (t != null) return t;
            } else if (selectedActorIndex == 1) {
                t = clip.getBones().get("cat:" + boneName);

                if (t != null) return t;
            }
        }

        return direct;
    }

    private BoneTrack getOrCreateBoneTrack(String boneName) {
        if (clip == null || boneName == null) return null;

        if (clip.hasActorPrefixes()) {
            return clip.getOrCreateBone("actor" + selectedActorIndex + ":" + boneName);
        }

        return clip.getOrCreateBone(boneName);
    }

    public void setBendableMode(boolean bendable) {
        updateActiveBones();
    }

    public String[] getActiveTrackBones() {
        return activeTrackBones;
    }

    public TimelinePanel(int x, int y, int width, int height, int fps) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.currentFrame = 0;
        this.playing = false;
        this.lastTimeNano = System.nanoTime();
    }

    public void setListener(TimelineListener listener) {
        this.listener = listener;
    }

    public void setClip(AnimationClip clip) {
        setClip(clip, false);
    }

    public void setClip(AnimationClip clip, boolean preservePlayhead) {
        boolean clipChanged = (this.clip != clip);
        this.clip = clip;
        this.timelineCapacityFrames = 1000;
        if (clip != null) {
            int clipFrames = (int) (clip.getLength() * fps);
            while (clipFrames >= timelineCapacityFrames - 50) {
                timelineCapacityFrames += 1000;
            }
            if (clip.getLength() < (float) timelineCapacityFrames / fps) {
                clip.setLength((float) timelineCapacityFrames / fps);
            }
        }
        if (clipChanged && !preservePlayhead) {
            this.currentFrame = 0;
            this.selectedKeyframe = null;
            this.selectedTextCard = null;
            this.selectedKeyframes.clear();
        }
        updateActiveBones();
    }
    public AnimationClip getClip() {
        return clip;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Set<KeyframeRef> getSelectedKeyframes() {
        return selectedKeyframes;
    }

    public void selectAllKeyframes() {
        selectedKeyframes.clear();

        if (clip == null) return;

        for (Map.Entry<String, BoneTrack> bEntry : clip.getBones().entrySet()) {
            String bone = bEntry.getKey();
            BoneTrack bt = bEntry.getValue();

            for (Map.Entry<Float, Keyframe> kEntry : bt.getRotationKeys().entrySet()) {
                selectedKeyframes.add(new KeyframeRef(bone, ChannelType.ROTATION, kEntry.getKey(), kEntry.getValue()));
            }

            for (Map.Entry<Float, Keyframe> kEntry : bt.getPositionKeys().entrySet()) {
                selectedKeyframes.add(new KeyframeRef(bone, ChannelType.POSITION, kEntry.getKey(), kEntry.getValue()));
            }

            for (Map.Entry<Float, Keyframe> kEntry : bt.getScaleKeys().entrySet()) {
                selectedKeyframes.add(new KeyframeRef(bone, ChannelType.SCALE, kEntry.getKey(), kEntry.getValue()));
            }
        }

        for (Map.Entry<Float, CameraTrack.CameraKeyframe> kEntry : clip.getCameraTrack().getKeyframes().entrySet()) {
            selectedKeyframes.add(new KeyframeRef("camera", ChannelType.CAMERA, kEntry.getKey(), null));
        }
    }

    public void deleteSelectedKeyframes() {
        if (clip == null || selectedKeyframes.isEmpty()) return;

        for (KeyframeRef ref : selectedKeyframes) {
            if (ref.channel() == ChannelType.CAMERA) {
                clip.getCameraTrack().removeKey(ref.time());
            } else if (ref.channel() == ChannelType.PARTICLE) {
                clip.getParticleTrack().removeKeyframeAt(ref.time());
            } else {
                BoneTrack track = getBoneTrack(ref.bone());

                if (track != null) {
                    track.removeKeyframe(ref.channel(), ref.time());
                }
            }
        }

        selectedKeyframes.clear();
        selectedKeyframe = null;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;

        graphics.enableScissor(x, y, x + width, y + height);
        long now = System.nanoTime();

        if (playing && clip != null) {
            double elapsedSec = (now - lastTimeNano) / 1_000_000_000.0;

            if (elapsedSec >= (1.0 / fps)) {
                int framesToAdvance = (int) (elapsedSec * fps);
                currentFrame += framesToAdvance;
                int maxFrames = getTotalFrames();

                if (currentFrame >= maxFrames) {
                    if (clip.isLooping()) {
                        currentFrame = currentFrame % Math.max(1, maxFrames);

                        AudioPlaybackService.getInstance().playFrom((float) currentFrame / fps);
                    } else {
                        currentFrame = maxFrames;
                        playing = false;

                        AudioPlaybackService.getInstance().stop();
                    }
                }

                lastTimeNano = now - (long) ((elapsedSec - (double) framesToAdvance / fps) * 1_000_000_000.0);
            }
            float curSec = (float) currentFrame / fps;
            if (!AudioPlaybackService.getInstance().isPlaying() && AudioPlaybackService.getInstance().getSegmentAt(curSec) != null) {
                AudioPlaybackService.getInstance().playFrom(curSec);
            }
        } else {
            lastTimeNano = now;
        }

        long animNow = System.currentTimeMillis();

        if (lastAnimTime > 0) {
            float dt = Math.min(0.1f, (animNow - lastAnimTime) / 1000.0f);
            boolean anims = (settings == null) || settings.isUiAnimations();

            if (!anims) {
                treeAnim = treeTarget;
            } else {
                treeAnim += (treeTarget - treeAnim) * (1.0f - (float) Math.exp(-14.0f * dt));

                if (Math.abs(treeTarget - treeAnim) < 0.001f) {
                    treeAnim = treeTarget;
                }
            }
        }

        lastAnimTime = animNow;
        checkAutoExtendTimeline();
        int maxFrames = Math.max(timelineCapacityFrames, getTotalFrames());

        if (draggingPlayhead) {
            int leftBound = x + TRACK_HEADER_WIDTH + 1;
            int rightBound = x + width;
            if (mouseX < leftBound && scrollOffset > 0) {
                float over = leftBound - (float) mouseX;
                scrollOffset = Math.max(0.0f, scrollOffset - Math.min(25.0f, over * 0.35f));
                this.currentFrame = Math.max(0, screenXToFrame((float) mouseX));
                checkAutoExtendTimeline();
            } else if (mouseX > rightBound) {
                float over = (float) mouseX - rightBound;
                scrollOffset += Math.min(25.0f, over * 0.35f);
                this.currentFrame = Math.max(0, screenXToFrame((float) mouseX));
                checkAutoExtendTimeline();
            }
        }

        graphics.fill(x, y, x + width, y + height, getBgColor());

        graphics.fill(x, y, x + width, y + TOOLBAR_HEIGHT, getToolbarBg());

        graphics.fill(x, y + TOOLBAR_HEIGHT - 1, x + width, y + TOOLBAR_HEIGHT, getBorderColor());
        int by = y + 3;
        int bh = 16;
        int bx = x + 6;

        drawToolButton(graphics, font, "+◆", bx, by, 22, bh, mouseX, mouseY, false); bx += 24;
        boolean audioSelected = (AudioPlaybackService.getInstance().getSelectedSegment() != null);
        String actionBtnStr = audioSelected ? "✂" : "🗑";
        drawToolButton(graphics, font, actionBtnStr, bx, by, 20, bh, mouseX, mouseY, audioSelected); bx += 22;

        drawDivider(graphics, bx, by + 2, bh - 4); bx += 5;

        drawToolButton(graphics, font, "|◀", bx, by, 20, bh, mouseX, mouseY, false); bx += 22;
        drawToolButton(graphics, font, playing ? "⏸" : "▶", bx, by, 22, bh, mouseX, mouseY, playing); bx += 24;
        drawToolButton(graphics, font, "▶|", bx, by, 20, bh, mouseX, mouseY, false); bx += 22;
        boolean looping = (clip != null && clip.isLooping());
        drawToolButton(graphics, font, "⟳", bx, by, 20, bh, mouseX, mouseY, looping); bx += 22;

        drawDivider(graphics, bx, by + 2, bh - 4); bx += 5;

        drawToolButton(graphics, font, "📈", bx, by, 22, bh, mouseX, mouseY, showingGraph); bx += 24;
        boolean hasAudio = (!AudioPlaybackService.getInstance().getSegments().isEmpty() || AudioPlaybackService.getInstance().getCurrentTrack() != null);
        String audioBtnStr = (getLanguage() == ModelSettings.Language.RU) ? "♫ Аудио" : "♫ Audio";
        int audioBtnW = font.width(audioBtnStr) + 10;
        drawToolButton(graphics, font, audioBtnStr, bx, by, audioBtnW, bh, mouseX, mouseY, hasAudio); bx += audioBtnW + 2;

        boolean hasTextCards = (clip != null && clip.getTextTrack() != null && !clip.getTextTrack().getCards().isEmpty());
        String textBtnStr = (getLanguage() == ModelSettings.Language.RU) ? "T Текст" : "T Text";
        int textBtnW = font.width(textBtnStr) + 10;
        drawToolButton(graphics, font, textBtnStr, bx, by, textBtnW, bh, mouseX, mouseY, hasTextCards); bx += textBtnW + 2;

        drawDivider(graphics, bx, by + 2, bh - 4); bx += 5;

        drawToolButton(graphics, font, "📋", bx, by, 20, bh, mouseX, mouseY, false); bx += 22;
        drawToolButton(graphics, font, "📥", bx, by, 20, bh, mouseX, mouseY, false); bx += 22;
        int rx = x + width - 6;
        int minRx = bx + 8;

        String timeStr = formatTimeCode(currentFrame, fps);
        String info = String.format("%s | F:%d", timeStr, currentFrame);
        int infoW = font.width(info) + 8;

        if (rx - infoW >= minRx) {
            rx -= infoW;

            graphics.fill(rx, by, rx + infoW, by + bh, (theme != null) ? theme.getPanelBg() : 0xFF0F131A);

            graphics.renderOutline(rx, by, infoW, bh, getBorderColor());

            graphics.drawString(font, info, rx + 4, by + 4, (theme != null) ? theme.getAccent() : 0xFF93C5FD, false);
        }

        int rulerTop = y + TOOLBAR_HEIGHT;

        boolean hasParts = (clip != null && clip.getParticleTrack() != null && !clip.getParticleTrack().isEmpty());
        int numTracks = 1 + (modelTreeExpanded ? activeTrackBones.length : 0) + 1 + (hasParts ? 1 : 0) + 1 + 1 + 1;
        int vHeight = height - TOOLBAR_HEIGHT - RULER_HEIGHT;
        int maxScrl = Math.max(0, numTracks * TRACK_HEIGHT - vHeight);
        verticalTrackScrollOffset = Math.max(0, Math.min(maxScrl, verticalTrackScrollOffset));

        graphics.fill(x, rulerTop, x + width, rulerTop + RULER_HEIGHT, (theme != null) ? theme.getTitleBarBg() : COLOR_RULER_BG);
        int trackTop = rulerTop + RULER_HEIGHT - verticalTrackScrollOffset;
        int rowLinesY = trackTop;
        int bonesH = getExpandedBonesHeight();

        graphics.enableScissor(x, rulerTop + RULER_HEIGHT, x + width, y + height);

        if (rowLinesY >= rulerTop + RULER_HEIGHT && rowLinesY <= y + height) {
            graphics.fill(x, rowLinesY, x + width, rowLinesY + 1, getBorderColor());
        }
        rowLinesY += TRACK_HEIGHT;

        if (bonesH > 0) {
            for (int i = 0; i < activeTrackBones.length; i++) {
                int ty = trackTop + (1 + i) * TRACK_HEIGHT;

                if (ty > y + height) break;

                if (ty + TRACK_HEIGHT < rulerTop + RULER_HEIGHT) continue;

                graphics.fill(x, ty, x + width, ty + 1, (theme != null) ? theme.getDivider() : COLOR_TRACK_LINE);
            }
        }

        boolean hasParticles = (clip != null && clip.getParticleTrack() != null && !clip.getParticleTrack().isEmpty());
        int cameraTrackY = trackTop + TRACK_HEIGHT + bonesH;
        int nextTrackY = cameraTrackY;

        if (cameraTrackY < y + height && cameraTrackY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT) {
            graphics.fill(x, cameraTrackY, x + width, cameraTrackY + 1, (theme != null) ? theme.getDivider() : 0xFF26334D);
        }

        nextTrackY += TRACK_HEIGHT;
        int particleTrackY = -1;

        if (hasParticles) {
            particleTrackY = nextTrackY;

            if (particleTrackY < y + height && particleTrackY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT) {
                graphics.fill(x, particleTrackY, x + width, particleTrackY + 1, (theme != null) ? theme.getDivider() : 0xFF26334D);
            }

            nextTrackY += TRACK_HEIGHT;
        }

        int textTrackY = nextTrackY;

        if (textTrackY < y + height && textTrackY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT) {
            graphics.fill(x, textTrackY, x + width, textTrackY + 1, (theme != null) ? theme.getDivider() : 0xFF26334D);
        }

        nextTrackY += TRACK_HEIGHT;
        int audioTrackY = nextTrackY;

        if (audioTrackY < y + height && audioTrackY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT) {
            graphics.fill(x, audioTrackY, x + width, audioTrackY + 1, (theme != null) ? theme.getDivider() : 0xFF26334D);
        }

        nextTrackY += TRACK_HEIGHT;

        graphics.disableScissor();
        int contentAreaLeft = x + TRACK_HEADER_WIDTH + 1;

        graphics.enableScissor(contentAreaLeft, rulerTop, x + width, rulerTop + RULER_HEIGHT);
        int tickInterval;

        if (pixelsPerFrame >= 12.0f) {
            tickInterval = 1;
        } else if (pixelsPerFrame >= 6.0f) {
            tickInterval = 5;
        } else if (pixelsPerFrame >= 3.0f) {
            tickInterval = 10;
        } else if (pixelsPerFrame >= 1.5f) {
            tickInterval = 20;
        } else {
            tickInterval = 50;
        }

        for (int f = 0; f <= maxFrames; f++) {
            float fx = frameToScreenX(f);

            if (fx < contentAreaLeft || fx > x + width) continue;
            int sx = (int) fx;

            if (f % tickInterval == 0) {
                graphics.fill(sx, rulerTop + RULER_HEIGHT - 6, sx + 1, rulerTop + RULER_HEIGHT, getBorderColor());
                String str = String.valueOf(f);
                int strW = font.width(str);
                int strX = Math.max(contentAreaLeft + 2, sx - strW / 2);

                graphics.drawString(font, str, strX, rulerTop + 4, getMutedColor(), false);
            } else if (tickInterval >= 10 && f % (tickInterval / 2) == 0) {
                graphics.fill(sx, rulerTop + RULER_HEIGHT - 4, sx + 1, rulerTop + RULER_HEIGHT, getBorderColor());
            } else if (pixelsPerFrame >= 4.0f) {
                graphics.fill(sx, rulerTop + RULER_HEIGHT - 2, sx + 1, rulerTop + RULER_HEIGHT, (theme != null) ? theme.getDivider() : 0xFF1C2433);
            }
        }
        graphics.disableScissor();

        graphics.enableScissor(contentAreaLeft, rulerTop + RULER_HEIGHT, x + width, y + height);

        if (bonesH > 0) {
            for (int i = 0; i < activeTrackBones.length; i++) {
                int ty = trackTop + (1 + i) * TRACK_HEIGHT;

                if (ty + TRACK_HEIGHT <= rulerTop + RULER_HEIGHT || ty >= y + height) continue;

                if (clip != null) {
                    BoneTrack track = getBoneTrack(activeTrackBones[i]);

                    if (track != null) {
                        renderBoneKeyframes(graphics, activeTrackBones[i], ChannelType.ROTATION, track.getRotationKeys(), ty);

                        renderBoneKeyframes(graphics, activeTrackBones[i], ChannelType.POSITION, track.getPositionKeys(), ty);

                        renderBoneKeyframes(graphics, activeTrackBones[i], ChannelType.SCALE, track.getScaleKeys(), ty);
                    }
                }
            }
        }

        if (cameraTrackY < y + height && cameraTrackY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT && clip != null) {
            for (Map.Entry<Float, CameraTrack.CameraKeyframe> entry : clip.getCameraTrack().getKeyframes().entrySet()) {
                int frame = (int) (entry.getKey() * fps);
                float fx = frameToScreenX(frame);

                if (fx >= contentAreaLeft && fx <= x + width) {
                    boolean isSel = isKeyframeSelected("camera", ChannelType.CAMERA, entry.getKey(), null);
                    boolean isSharp = entry.getValue().isSharp();
                    int baseColor = isSharp ? 0xFFFF4444 : 0xFF38BDF8;
                    int cy = cameraTrackY + TRACK_HEIGHT / 2;

                    if (isSel) {
                        drawDiamond(graphics, (int) fx, cy, COLOR_KEYFRAME_SEL, 5);

                        drawDiamond(graphics, (int) fx, cy, baseColor, 3);
                    } else {
                        drawDiamond(graphics, (int) fx, cy, baseColor, 4);
                    }
                }
            }
        }

        if (hasParticles && particleTrackY != -1 && particleTrackY < y + height && particleTrackY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT && clip != null) {
            for (net.openstage.core.particle.ParticleKeyframe pk : clip.getParticleTrack().getKeyframes()) {
                int frame = (int) (pk.time() * fps);
                float fx = frameToScreenX(frame);
                int cy = particleTrackY + TRACK_HEIGHT / 2;

                if (pk.isContinuous()) {
                    int endFrame = (int) ((pk.time() + pk.duration()) * fps);
                    float ex = frameToScreenX(endFrame);
                    int barW = Math.max(8, (int) (ex - fx));

                    graphics.fill((int) fx, cy - 3, (int) fx + barW, cy + 4, 0x55E879F9);

                    graphics.renderOutline((int) fx, cy - 3, barW, 7, 0xAAE879F9);
                }

                if (fx >= contentAreaLeft && fx <= x + width) {
                    boolean isSel = isKeyframeSelected("particles", ChannelType.PARTICLE, pk.time(), null);

                    if (isSel) {
                        drawDiamond(graphics, (int) fx, cy, COLOR_KEYFRAME_SEL, 5);

                        drawDiamond(graphics, (int) fx, cy, 0xFFE879F9, 3);
                    } else {
                        drawDiamond(graphics, (int) fx, cy, 0xFFE879F9, 4);
                    }
                }
            }
        }

        if (textTrackY < y + height && textTrackY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT && clip != null) {
            List<TextCard> cards = clip.getTextTrack().getCards();

            for (TextCard card : cards) {
                int cardStartX = (int) frameToScreenX((int) (card.getStartTime() * fps));
                int cardEndX = (int) frameToScreenX((int) (card.getEndTime() * fps));
                int cardW = Math.max(12, cardEndX - cardStartX);

                if (cardStartX + cardW >= contentAreaLeft && cardStartX <= x + width) {
                    int boxBg = (card == selectedTextCard) ? COLOR_TEXT_CARD_SEL : COLOR_TEXT_CARD_BG;

                    graphics.fill(cardStartX, textTrackY + 2, cardStartX + cardW, textTrackY + TRACK_HEIGHT - 2, boxBg);

                    graphics.renderOutline(cardStartX, textTrackY + 2, cardW, TRACK_HEIGHT - 4, 0xFFFFFFFF);
                    String label = I18n.get(card.getText(), getLanguage());

                    if (font.width(label) > cardW - 4) {
                        label = "...";
                    }

                    graphics.drawString(font, label, cardStartX + 3, textTrackY + 4, 0xFFFFFFFF, false);
                }
            }
        }

        if (audioTrackY < y + height && audioTrackY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT) {
            List<AudioPlaybackService.AudioSegment> segList = AudioPlaybackService.getInstance().getSegments();
            for (AudioPlaybackService.AudioSegment seg : segList) {
                if (seg.getSourceData() == null) continue;
                float segStartSec = seg.getStartOffset();
                float segEndSec = seg.getEndOffset();
                int segStartX = (int) frameToScreenX((int) (segStartSec * fps));
                int segEndX = (int) frameToScreenX((int) (segEndSec * fps));
                int segW = Math.max(6, segEndX - segStartX);

                if (segEndX >= contentAreaLeft && segStartX <= x + width) {
                    int segBg = seg.isSelected() ? 0x660284C7 : 0x330284C7;
                    int segBorder = seg.isSelected() ? 0xFFFFFFFF : 0xFF00E5FF;
                    graphics.fill(segStartX, audioTrackY + 2, segEndX, audioTrackY + TRACK_HEIGHT - 2, segBg);
                    graphics.renderOutline(segStartX, audioTrackY + 2, segW, TRACK_HEIGHT - 4, segBorder);

                    int midY = audioTrackY + TRACK_HEIGHT / 2;
                    int startPx = Math.max(contentAreaLeft, segStartX + 2);
                    int endPx = Math.min(x + width, segEndX - 2);
                    int barColor = seg.isSelected() ? 0xFFFFFFFF : 0xFF38BDF8;

                    for (int px = startPx; px < endPx; px += 2) {
                        float tSec = ((float) (px - (x + TRACK_HEADER_WIDTH + 10) + scrollOffset) / pixelsPerFrame) / fps;
                        float relT = tSec - segStartSec;
                        if (relT >= 0.0f && relT <= seg.getDuration()) {
                            float srcT = seg.getInPoint() + relT;
                            float peak = seg.getSourceData().getPeakAt(srcT);
                            int h = Math.max(1, (int) (peak * (TRACK_HEIGHT / 2 - 4)));
                            graphics.fill(px, midY - h, px + 1, midY + h + 1, barColor);
                        }
                    }

                    int handleW = 5;
                    boolean leftHov = isInside(mouseX, mouseY, segStartX, audioTrackY + 2, handleW, TRACK_HEIGHT - 4);
                    boolean rightHov = isInside(mouseX, mouseY, segEndX - handleW, audioTrackY + 2, handleW, TRACK_HEIGHT - 4);
                    int leftHandleCol = (leftHov || (draggingAudioSegment == seg && draggingAudioHandle == 1)) ? 0xFFFFFFFF : 0xFF38BDF8;
                    int rightHandleCol = (rightHov || (draggingAudioSegment == seg && draggingAudioHandle == 2)) ? 0xFFFFFFFF : 0xFF38BDF8;
                    graphics.fill(segStartX, audioTrackY + 2, segStartX + handleW, audioTrackY + TRACK_HEIGHT - 2, leftHandleCol);
                    graphics.fill(segEndX - handleW, audioTrackY + 2, segEndX, audioTrackY + TRACK_HEIGHT - 2, rightHandleCol);
                }
            }
        }

        graphics.disableScissor();

        float phX = frameToScreenX(currentFrame);
        float rightThreshold = (float) (x + width - 35);
        if (phX > rightThreshold) {
            scrollOffset += (phX - rightThreshold);
            phX = frameToScreenX(currentFrame);
        }

        if (phX >= contentAreaLeft && phX <= x + width) {
            int px = (int) phX;

            graphics.fill(px, rulerTop, px + 2, y + height, getPlayheadColor());

            graphics.fill(px - 3, rulerTop, px + 5, rulerTop + 6, getPlayheadColor());
        }

        if (isBoxSelecting) {
            int bx0 = (int) Math.min(boxStartX, boxCurrentX);
            int by0 = (int) Math.min(boxStartY, boxCurrentY);
            int bx1 = (int) Math.max(boxStartX, boxCurrentX);
            int by1 = (int) Math.max(boxStartY, boxCurrentY);
            int boxAccent = getAccentColor();

            graphics.fill(bx0, by0, bx1, by1, (boxAccent & 0x00FFFFFF) | 0x33000000);

            graphics.renderOutline(bx0, by0, Math.max(1, bx1 - bx0), Math.max(1, by1 - by0), boxAccent);
        }

        graphics.fill(x, rulerTop, x + TRACK_HEADER_WIDTH, y + height, (theme != null) ? theme.getTitleBarBg() : COLOR_TOOLBAR_BG);

        graphics.fill(x + TRACK_HEADER_WIDTH, rulerTop, x + TRACK_HEADER_WIDTH + 1, y + height, getBorderColor());

        graphics.drawString(font, I18n.get("Tracks", getLanguage()), x + 6, rulerTop + 4, getMutedColor(), false);

        graphics.enableScissor(x + 2, rulerTop + RULER_HEIGHT, x + TRACK_HEADER_WIDTH - 2, y + height);
        int headerRowY = trackTop;
        String folderIcon = (treeAnim > 0.5f) ? "▼ " : "▶ ";

        if (headerRowY < y + height && headerRowY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT) {
            net.openstage.client.model.ModelSettings.Language hLang = getLanguage();
            graphics.drawString(font, folderIcon + I18n.get(modelName, hLang), x + 6, headerRowY + 4, getAccentColor(), false);
            int allEyeX = x + TRACK_HEADER_WIDTH - 15;
            int allEyeY = headerRowY + 2;
            boolean allEyeHov = mouseX >= allEyeX - 1 && mouseX <= allEyeX + 13 && mouseY >= allEyeY - 1 && mouseY <= allEyeY + 12;
            boolean anyGhosted = (settings != null && !settings.getGhostedBones().isEmpty());

            drawEyeIcon(graphics, allEyeX, allEyeY, !anyGhosted, allEyeHov);
        }

        headerRowY += TRACK_HEIGHT;

        if (bonesH > 0) {
            int boneClipTop = Math.max(rulerTop + RULER_HEIGHT, trackTop + TRACK_HEIGHT);
            int boneClipBottom = Math.min(y + height, trackTop + TRACK_HEIGHT + bonesH);

            if (boneClipBottom > boneClipTop) {
                graphics.enableScissor(x, boneClipTop, x + TRACK_HEADER_WIDTH, boneClipBottom);

                for (int i = 0; i < activeTrackBones.length; i++) {
                    int ty = trackTop + (1 + i) * TRACK_HEIGHT;

                    if (ty > y + height) break;

                    if (ty + TRACK_HEIGHT < rulerTop + RULER_HEIGHT) continue;
                    String bone = activeTrackBones[i];
                    boolean ghosted = (settings != null) && settings.isBoneGhosted(bone);
                    net.openstage.client.model.ModelSettings.Language tLang = getLanguage();
                    String bName = I18n.getBoneDisplayName(bone, tLang);
                    int textCol = ghosted ? 0xFF64748B : getTextColor();
                    int maxBoneW = Math.max(10, (x + TRACK_HEADER_WIDTH - 20) - (x + 12));

                    graphics.drawString(font, font.plainSubstrByWidth(bName, maxBoneW), x + 12, ty + 4, textCol, false);
                    int eyeX = x + TRACK_HEADER_WIDTH - 15;
                    int eyeY = ty + 2;
                    boolean eyeHov = mouseX >= eyeX - 1 && mouseX <= eyeX + 13 && mouseY >= eyeY - 1 && mouseY <= eyeY + 12;

                    drawEyeIcon(graphics, eyeX, eyeY, !ghosted, eyeHov);
                }

                graphics.disableScissor();
            }
        }

        if (cameraTrackY < y + height && cameraTrackY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT) {
            graphics.drawString(font, "🎥 " + I18n.get("Camera", getLanguage()), x + 6, cameraTrackY + 4, 0xFF38BDF8, false);
        }

        if (hasParticles && particleTrackY != -1 && particleTrackY < y + height && particleTrackY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT) {
            graphics.drawString(font, "✨ " + I18n.get("Particles", getLanguage()), x + 6, particleTrackY + 4, 0xFFE879F9, false);
        }

        if (textTrackY < y + height && textTrackY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT) {
            graphics.drawString(font, I18n.get("Text Cards", getLanguage()), x + 6, textTrackY + 4, getAccentColor(), false);
            int addX = x + TRACK_HEADER_WIDTH - 15;
            int addY = textTrackY + 2;
            boolean addHov = mouseX >= addX && mouseX <= addX + 12 && mouseY >= addY && mouseY <= addY + 12;
            graphics.drawString(font, "+", addX + 2, addY + 2, addHov ? 0xFFFFFFFF : getAccentColor(), false);
        }

        if (audioTrackY < y + height && audioTrackY + TRACK_HEIGHT > rulerTop + RULER_HEIGHT) {
            graphics.drawString(font, "♫ " + I18n.get("Audio", getLanguage()), x + 6, audioTrackY + 4, 0xFF00E5FF, false);
        }

        graphics.disableScissor();
        graphics.disableScissor();
    }

    private void drawEyeIcon(GuiGraphics graphics, int ex, int ey, boolean visible, boolean hovered) {
        int col = visible ? (hovered ? 0xFFFFFFFF : 0xFF94A3B8) : (hovered ? 0xFFF87171 : 0xFF64748B);
        int pupilCol = visible ? (hovered ? 0xFF38BDF8 : 0xFF60A5FA) : 0xFF475569;

        graphics.fill(ex + 3, ey + 2, ex + 9, ey + 3, col);

        graphics.fill(ex + 3, ey + 8, ex + 9, ey + 9, col);

        graphics.fill(ex + 1, ey + 4, ex + 3, ey + 7, col);

        graphics.fill(ex + 9, ey + 4, ex + 11, ey + 7, col);

        graphics.fill(ex + 2, ey + 3, ex + 4, ey + 4, col);

        graphics.fill(ex + 8, ey + 3, ex + 10, ey + 4, col);

        graphics.fill(ex + 2, ey + 7, ex + 4, ey + 8, col);

        graphics.fill(ex + 8, ey + 7, ex + 10, ey + 8, col);

        if (visible) {
            graphics.fill(ex + 5, ey + 4, ex + 7, ey + 7, pupilCol);
        } else {
            graphics.fill(ex + 2, ey + 2, ex + 4, ey + 4, 0xFFEF4444);

            graphics.fill(ex + 4, ey + 4, ex + 8, ey + 7, 0xFFEF4444);

            graphics.fill(ex + 8, ey + 7, ex + 10, ey + 9, 0xFFEF4444);
        }
    }

    private String formatTimeCode(int frame, int fps) {
        int totalSeconds = frame / fps;
        int remainderFrames = frame % fps;
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", mins, secs, remainderFrames);
    }

    private void drawDivider(GuiGraphics graphics, int dx, int dy, int dh) {
        graphics.fill(dx, dy, dx + 1, dy + dh, (theme != null) ? theme.getDivider() : 0xFF2E3545);
    }

    private void drawIconButton(GuiGraphics graphics, int u, int v, int bx, int by, int bw, int bh, int mouseX, int mouseY, boolean active) {
        boolean hovered = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        int bg = active ? getAccentColor() : (hovered ? ((theme != null) ? theme.getButtonHover() : 0xFF283344) : ((theme != null) ? theme.getButtonBg() : 0xFF141923));
        int border = active ? ((theme != null) ? theme.getAccent() : 0xFF3B82F6) : (hovered ? ((theme != null) ? theme.getDividerHover() : 0xFF475569) : getBorderColor());

        graphics.fill(bx, by, bx + bw, by + bh, bg);
        graphics.renderOutline(bx, by, bw, bh, border);
        int iconX = bx + (bw - 16) / 2;
        int iconY = by + (bh - 16) / 2;
        graphics.blit(ICONS_TEXTURE, iconX, iconY, u, v, 16, 16, 256, 256);
    }

    private void drawToolButton(GuiGraphics graphics, Font font, String text, int bx, int by, int bw, int bh, int mouseX, int mouseY, boolean active) {
        boolean hovered = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        int bg = active ? getAccentColor() : (hovered ? ((theme != null) ? theme.getButtonHover() : 0xFF283347) : ((theme != null) ? theme.getButtonBg() : 0xFF151B26));
        int border = active ? ((theme != null) ? theme.getAccent() : 0xFF3B82F6) : (hovered ? ((theme != null) ? theme.getAccent() : 0xFF475569) : ((theme != null) ? theme.getDivider() : 0xFF222B3D));

        graphics.fill(bx, by, bx + bw, by + bh, bg);
        graphics.renderOutline(bx, by, bw, bh, border);
        int tw = font.width(text);
        int tx = bx + (bw - tw) / 2;
        int ty = by + (bh - 8) / 2;

        int textColor = active ? 0xFFFFFFFF : (hovered ? 0xFFFFFFFF : (theme != null ? theme.getTextColor() : 0xFFCBD5E1));
        graphics.drawString(font, text, tx, ty, textColor, false);
    }

    private void renderBoneKeyframes(GuiGraphics graphics, String bone, ChannelType channel, Map<Float, Keyframe> keys, int trackY) {
        if (keys == null) return;

        for (Map.Entry<Float, Keyframe> entry : keys.entrySet()) {
            int frame = (int) (entry.getKey() * fps);
            float fx = frameToScreenX(frame);

            if (fx >= x && fx <= x + width) {
                Keyframe kf = entry.getValue();
                boolean isSel = (kf == selectedKeyframe) || isKeyframeSelected(bone, channel, entry.getKey(), kf);
                int color = isSel ? COLOR_KEYFRAME_SEL : COLOR_KEYFRAME;

                drawDiamond(graphics, (int) fx, trackY + TRACK_HEIGHT / 2, color);
            }
        }
    }

    private boolean isKeyframeSelected(String bone, ChannelType channel, float time, Keyframe kf) {
        if (selectedKeyframes.isEmpty()) return false;

        for (KeyframeRef ref : selectedKeyframes) {
            if (ref.bone().equals(bone) && ref.channel() == channel && Math.abs(ref.time() - time) < 0.001f) {
                return true;
            }
        }

        return false;
    }

    private void drawDiamond(GuiGraphics graphics, int cx, int cy, int color) {
        drawDiamond(graphics, cx, cy, color, DIAMOND_SIZE);
    }

    private void drawDiamond(GuiGraphics graphics, int cx, int cy, int color, int size) {
        for (int i = 0; i <= size; i++) {
            graphics.fill(cx - i, cy - size + i, cx + i + 1, cy - size + i + 1, color);

            graphics.fill(cx - i, cy + size - i, cx + i + 1, cy + size - i + 1, color);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false;
        }

        if (mouseY < y + TOOLBAR_HEIGHT) {
            int by = y + 3;
            int bh = 16;
            int bx = x + 6;

            if (isInside(mouseX, mouseY, bx, by, 22, bh)) { if (actionListener != null) actionListener.onAddKeyframe(); return true; }
            bx += 24;

            if (isInside(mouseX, mouseY, bx, by, 20, bh)) {
                if (AudioPlaybackService.getInstance().getSelectedSegment() != null) {
                    float curSec = (float) currentFrame / fps;
                    AudioPlaybackService.getInstance().splitAt(curSec);
                    return true;
                }
                if (actionListener != null) actionListener.onDeleteKeyframe();
                return true;
            }
            bx += 22;
            bx += 5;

            if (isInside(mouseX, mouseY, bx, by, 20, bh)) { stepFrame(-1); return true; }
            bx += 22;

            if (isInside(mouseX, mouseY, bx, by, 22, bh)) { togglePlay(); return true; }
            bx += 24;

            if (isInside(mouseX, mouseY, bx, by, 20, bh)) { stepFrame(1); return true; }
            bx += 22;

            if (isInside(mouseX, mouseY, bx, by, 20, bh)) { if (clip != null) clip.setLooping(!clip.isLooping()); return true; }
            bx += 22;
            bx += 5;

            if (isInside(mouseX, mouseY, bx, by, 22, bh)) { if (actionListener != null) actionListener.onToggleGraph(); return true; }
            bx += 24;

            Font font = Minecraft.getInstance().font;
            boolean hasAudio = (!AudioPlaybackService.getInstance().getSegments().isEmpty() || AudioPlaybackService.getInstance().getCurrentTrack() != null);
            String audioBtnStr = (getLanguage() == ModelSettings.Language.RU) ? "♫ Аудио" : "♫ Audio";
            int audioBtnW = font.width(audioBtnStr) + 10;
            if (isInside(mouseX, mouseY, bx, by, audioBtnW, bh)) {
                AudioPlaybackService.getInstance().openFileDialog(track -> {
                    if (clip != null && track != null && track.duration() > clip.getLength()) {
                        clip.setLength(track.duration());
                    }
                });
                return true;
            }
            bx += audioBtnW + 2;

            String textBtnStr = (getLanguage() == ModelSettings.Language.RU) ? "T Текст" : "T Text";
            int textBtnW = font.width(textBtnStr) + 10;
            if (isInside(mouseX, mouseY, bx, by, textBtnW, bh)) {
                if (clip != null && clip.getTextTrack() != null && !clip.getTextTrack().getCards().isEmpty()) {
                    TextCard card = (selectedTextCard != null && clip.getTextTrack().getCards().contains(selectedTextCard)) ? selectedTextCard : clip.getTextTrack().getCards().get(0);
                    this.selectedTextCard = card;
                    if (listener != null) {
                        listener.onTextCardSelected(card);
                        listener.onTextCardDoubleClicked(card);
                    }
                } else if (actionListener != null) {
                    actionListener.onAddTextCardRequested();
                }
                return true;
            }
            bx += textBtnW + 2;

            bx += 5;

            if (isInside(mouseX, mouseY, bx, by, 20, bh)) { if (actionListener != null) actionListener.onCopyPose(); return true; }
            bx += 22;

            if (isInside(mouseX, mouseY, bx, by, 20, bh)) { if (actionListener != null) actionListener.onPastePose(); return true; }
            bx += 22;

            return true;
        }

        int rulerTop = y + TOOLBAR_HEIGHT;

        if (mouseY < rulerTop + RULER_HEIGHT) {
            if (mouseX >= x + TRACK_HEADER_WIDTH) {
                AudioPlaybackService.getInstance().clearSegmentSelection();
                this.currentFrame = Math.max(0, screenXToFrame((float) mouseX));
                this.draggingPlayhead = true;
                if (playing) {
                    AudioPlaybackService.getInstance().playFrom((float) currentFrame / fps);
                }
                return true;
            }
        }

        int trackTop = rulerTop + RULER_HEIGHT - verticalTrackScrollOffset;

        if (mouseY >= trackTop && mouseY < trackTop + TRACK_HEIGHT) {
            if (mouseX < x + TRACK_HEADER_WIDTH) {
                int allEyeX = x + TRACK_HEADER_WIDTH - 15;

                if (mouseX >= allEyeX - 2 && mouseX <= allEyeX + 14) {
                    if (settings != null) {
                        if (!settings.getGhostedBones().isEmpty()) {
                            settings.getGhostedBones().clear();
                        } else {
                            for (String b : activeTrackBones) settings.setBoneGhosted(b, true);
                        }
                    }

                    return true;
                }

                this.modelTreeExpanded = !modelTreeExpanded;
                this.treeTarget = this.modelTreeExpanded ? 1.0f : 0.0f;

                if (settings != null && !settings.isUiAnimations()) {
                    this.treeAnim = this.treeTarget;
                }

                return true;
            }
        }

        int clickBonesH = getExpandedBonesHeight();

        if (clickBonesH > 0 && mouseY >= trackTop + TRACK_HEIGHT && mouseY < trackTop + TRACK_HEIGHT + clickBonesH) {
            for (int i = 0; i < activeTrackBones.length; i++) {
                int ty = trackTop + (1 + i) * TRACK_HEIGHT;

                if (mouseY >= ty && mouseY < ty + TRACK_HEIGHT && mouseX < x + TRACK_HEADER_WIDTH) {
                    String bone = activeTrackBones[i];
                    int eyeX = x + TRACK_HEADER_WIDTH - 15;

                    if (mouseX >= eyeX - 2 && mouseX <= eyeX + 14) {
                        if (settings != null) {
                            settings.toggleBoneGhosted(bone);

                            if (settings.isBoneGhosted(bone) && listener != null) {
                                listener.onKeyframeSelected(null, ChannelType.ROTATION, null);
                            }
                        }

                        return true;
                    }

                    if (settings != null && settings.isBoneGhosted(bone)) {
                        return true;
                    }

                    if (listener != null) listener.onKeyframeSelected(bone, ChannelType.ROTATION, null);

                    return true;
                }
            }
        }

        int cameraTrackY = trackTop + TRACK_HEIGHT + clickBonesH;

        if (mouseY >= cameraTrackY && mouseY < cameraTrackY + TRACK_HEIGHT && mouseX < x + TRACK_HEADER_WIDTH) {
            if (listener != null) listener.onKeyframeSelected("camera", ChannelType.CAMERA, null);

            return true;
        }

        boolean hasParticlesInClick = (clip != null && clip.getParticleTrack() != null && !clip.getParticleTrack().isEmpty());
        int particleTrackY = hasParticlesInClick ? (cameraTrackY + TRACK_HEIGHT) : -1;

        if (hasParticlesInClick && mouseY >= particleTrackY && mouseY < particleTrackY + TRACK_HEIGHT && mouseX < x + TRACK_HEADER_WIDTH) {
            if (listener != null) listener.onKeyframeSelected("particles", ChannelType.PARTICLE, null);

            return true;
        }

        int textTrackY = (hasParticlesInClick ? particleTrackY : cameraTrackY) + TRACK_HEIGHT;

        if (mouseY >= textTrackY && mouseY <= textTrackY + TRACK_HEIGHT && clip != null) {
            long nowClick = System.currentTimeMillis();
            if (mouseX < x + TRACK_HEADER_WIDTH) {
                int addX = x + TRACK_HEADER_WIDTH - 15;
                if (mouseX >= addX) {
                    if (actionListener != null) actionListener.onAddTextCardRequested();
                } else {
                    List<TextCard> cards = clip.getTextTrack().getCards();
                    if (!cards.isEmpty()) {
                        TextCard card = (selectedTextCard != null && cards.contains(selectedTextCard)) ? selectedTextCard : cards.get(0);
                        this.selectedTextCard = card;
                        if (listener != null) {
                            listener.onTextCardSelected(card);
                            listener.onTextCardDoubleClicked(card);
                        }
                    } else if (actionListener != null) {
                        actionListener.onAddTextCardRequested();
                    }
                }
                return true;
            }
            for (TextCard card : clip.getTextTrack().getCards()) {
                int sx = (int) frameToScreenX((int) (card.getStartTime() * fps));
                int ex = Math.max(sx + 14, (int) frameToScreenX((int) (card.getEndTime() * fps)));

                if (mouseX >= sx && mouseX <= ex) {
                    this.lastClickedCard = card;
                    this.lastCardClickTime = nowClick;
                    this.selectedTextCard = card;

                    if (listener != null) {
                        listener.onTextCardSelected(card);
                        listener.onTextCardDoubleClicked(card);
                    }

                    return true;
                }
            }
            float clickTime = Math.max(0.0f, (float) (mouseX - (x + TRACK_HEADER_WIDTH + 1)) / (pixelsPerFrame * fps) + scrollOffset / fps);
            String initText = (settings != null && settings.getLanguage() == net.openstage.client.model.ModelSettings.Language.RU) ? "Новая плашка" : "New Text Plate";
            TextCard newCard = new TextCard(clickTime, clickTime + 2.0f, initText);
            clip.getTextTrack().addCard(newCard);
            this.selectedTextCard = newCard;
            this.lastClickedCard = newCard;
            this.lastCardClickTime = nowClick;
            if (listener != null) {
                listener.onTextCardSelected(newCard);
                listener.onTextCardDoubleClicked(newCard);
            }
            return true;
        }

        int audioTrackY = textTrackY + TRACK_HEIGHT;
        if (mouseY >= audioTrackY && mouseY <= audioTrackY + TRACK_HEIGHT) {
            if (mouseX < x + TRACK_HEADER_WIDTH) {
                AudioPlaybackService.getInstance().openFileDialog(track -> {
                    if (clip != null && track != null && track.duration() > clip.getLength()) {
                        clip.setLength(track.duration());
                    }
                });
                return true;
            }
            List<AudioPlaybackService.AudioSegment> segList = AudioPlaybackService.getInstance().getSegments();
            for (AudioPlaybackService.AudioSegment seg : segList) {
                if (seg.getData() == null) continue;
                float segStartSec = seg.getStartOffset();
                float segEndSec = seg.getEndOffset();
                int segStartX = (int) frameToScreenX((int) (segStartSec * fps));
                int segEndX = (int) frameToScreenX((int) (segEndSec * fps));
                int handleW = 6;

                if (mouseX >= segStartX - 4 && mouseX <= segEndX + 4) {
                    AudioPlaybackService.getInstance().selectSegment(seg);
                    long now = System.currentTimeMillis();
                    if (button == 1 || (seg == lastAudioClickedSeg && (now - lastAudioClickTime) < 350L)) {
                        if (listener != null) listener.onAudioSegmentDoubleClicked(seg);
                        lastAudioClickTime = 0L;
                        return true;
                    }
                    lastAudioClickTime = now;
                    lastAudioClickedSeg = seg;

                    this.draggingAudioSegment = seg;
                    this.dragAudioStartX = mouseX;
                    this.dragAudioOriginalOffset = segStartSec;
                    this.dragAudioOriginalDuration = seg.getDuration();

                    if (mouseX <= segStartX + handleW) {
                        this.draggingAudioHandle = 1;
                    } else if (mouseX >= segEndX - handleW) {
                        this.draggingAudioHandle = 2;
                    } else {
                        this.draggingAudioHandle = 3;
                    }
                    return true;
                }
            }
            AudioPlaybackService.getInstance().clearSegmentSelection();
            return true;
        }

        KeyframeRef clickedKey = findKeyframeAt(mouseX, mouseY);

        if (clickedKey != null) {
            AudioPlaybackService.getInstance().clearSegmentSelection();
            if (!Screen.hasShiftDown()) {
                if (!selectedKeyframes.contains(clickedKey)) {
                    selectedKeyframes.clear();

                    selectedKeyframes.add(clickedKey);
                }
            } else {
                selectedKeyframes.add(clickedKey);
            }

            this.selectedKeyframe = clickedKey.keyframe();

            if (listener != null) {
                if (clickedKey.channel() == ChannelType.ENVIRONMENT && clip != null) {
                    var kf = clip.getEnvironmentTrack().getKeyframes().get(clickedKey.time());
                    setSelectedEnvironmentKeyframe(kf);
                    listener.onEnvironmentKeyframeSelected(kf);
                } else {
                    setSelectedEnvironmentKeyframe(null);
                    listener.onKeyframeSelected(clickedKey.bone(), clickedKey.channel(), clickedKey.keyframe());
                }
            }

            this.currentFrame = (int) (clickedKey.time() * fps);
            this.isDraggingKeyframes = true;
            if (actionListener != null) actionListener.onEditStart();
            this.dragKeyframesStartX = mouseX;
            this.currentDragDeltaTime = 0.0f;

            this.activeDraggedKeys.clear();

            if (clip != null) {
                for (KeyframeRef ref : selectedKeyframes) {
                    if (ref.channel() == ChannelType.CAMERA) {
                        CameraTrack.CameraKeyframe ck = clip.getCameraTrack().getKeyframes().get(ref.time());

                        if (ck != null) {
                            activeDraggedKeys.add(new DraggedKeyRecord("camera", ChannelType.CAMERA, ref.time(), null, ck, null));
                        }
                    } else if (ref.channel() == ChannelType.PARTICLE) {
                        net.openstage.core.particle.ParticleKeyframe pk = clip.getParticleTrack().getKeyframeAt(ref.time());

                        if (pk != null) {
                            activeDraggedKeys.add(new DraggedKeyRecord("particles", ChannelType.PARTICLE, ref.time(), null, null, pk));
                        }
                    } else {
                        BoneTrack track = getBoneTrack(ref.bone());

                        if (track != null) {
                            Keyframe kf = track.getKeyframe(ref.channel(), ref.time());

                            if (kf != null) {
                                activeDraggedKeys.add(new DraggedKeyRecord(ref.bone(), ref.channel(), ref.time(), kf, null, null));
                            }
                        }
                    }
                }
            }

            return true;
        }

        if (button == 0) {
            if (!Screen.hasShiftDown()) {
                selectedKeyframes.clear();
                selectedKeyframe = null;
            }

            this.isBoxSelecting = true;
            this.boxStartX = mouseX;
            this.boxStartY = mouseY;
            this.boxCurrentX = mouseX;
            this.boxCurrentY = mouseY;

            return true;
        }

        return false;
    }

    private KeyframeRef findKeyframeAt(double mx, double my) {
        if (clip == null) return null;
        int trackTop = y + TOOLBAR_HEIGHT + RULER_HEIGHT - verticalTrackScrollOffset;

        if (modelTreeExpanded) {
            for (int i = 0; i < activeTrackBones.length; i++) {
                int ty = trackTop + (1 + i) * TRACK_HEIGHT;
                int cy = ty + TRACK_HEIGHT / 2;

                if (Math.abs(my - cy) <= DIAMOND_SIZE + 2) {
                    String bone = activeTrackBones[i];
                    BoneTrack track = getBoneTrack(bone);

                    if (track != null) {
                        for (Map.Entry<Float, Keyframe> entry : track.getRotationKeys().entrySet()) {
                            float fx = frameToScreenX((int) (entry.getKey() * fps));

                            if (Math.abs(mx - fx) <= DIAMOND_SIZE + 2) {
                                return new KeyframeRef(bone, ChannelType.ROTATION, entry.getKey(), entry.getValue());
                            }
                        }

                        for (Map.Entry<Float, Keyframe> entry : track.getPositionKeys().entrySet()) {
                            float fx = frameToScreenX((int) (entry.getKey() * fps));

                            if (Math.abs(mx - fx) <= DIAMOND_SIZE + 2) {
                                return new KeyframeRef(bone, ChannelType.POSITION, entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
            }
        }

        int cameraTrackY = trackTop + (modelTreeExpanded ? (1 + activeTrackBones.length) : 1) * TRACK_HEIGHT;
        int camCy = cameraTrackY + TRACK_HEIGHT / 2;

        if (Math.abs(my - camCy) <= DIAMOND_SIZE + 2) {
            for (Map.Entry<Float, CameraTrack.CameraKeyframe> entry : clip.getCameraTrack().getKeyframes().entrySet()) {
                float fx = frameToScreenX((int) (entry.getKey() * fps));

                if (Math.abs(mx - fx) <= DIAMOND_SIZE + 2) {
                    return new KeyframeRef("camera", ChannelType.CAMERA, entry.getKey(), null);
                }
            }
        }

        boolean hasParticlesInHit = (clip != null && clip.getParticleTrack() != null && !clip.getParticleTrack().isEmpty());
        int afterPartY = cameraTrackY + (hasParticlesInHit ? 2 : 1) * TRACK_HEIGHT;
        int envTrackHitY = afterPartY + TRACK_HEIGHT;

        if (hasParticlesInHit) {
            int particleTrackY = cameraTrackY + TRACK_HEIGHT;
            int partCy = particleTrackY + TRACK_HEIGHT / 2;

            if (Math.abs(my - partCy) <= DIAMOND_SIZE + 2 && clip != null) {
                for (net.openstage.core.particle.ParticleKeyframe pk : clip.getParticleTrack().getKeyframes()) {
                    float fx = frameToScreenX((int) (pk.time() * fps));

                    if (Math.abs(mx - fx) <= DIAMOND_SIZE + 2) {
                        return new KeyframeRef("particles", ChannelType.PARTICLE, pk.time(), null);
                    }
                }
            }
        }

        return null;
    }

    private void updateZoomFromSlider(double mx, int sliderX, int sliderW) {
        float ratio = (float) Math.max(0.0, Math.min(1.0, (mx - sliderX) / (double) sliderW));
        this.pixelsPerFrame = 0.5f + ratio * 29.5f;
    }

    private boolean isInside(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingAudioSegment != null && draggingAudioHandle > 0) {
            float deltaSec = (float) ((mouseX - dragAudioStartX) / pixelsPerFrame) / fps;
            if (draggingAudioHandle == 1) {
                float newOffset = dragAudioOriginalOffset + deltaSec;
                AudioPlaybackService.getInstance().trimSegmentStart(draggingAudioSegment, newOffset);
                return true;
            } else if (draggingAudioHandle == 2) {
                float newEnd = dragAudioOriginalOffset + dragAudioOriginalDuration + deltaSec;
                AudioPlaybackService.getInstance().trimSegmentEnd(draggingAudioSegment, newEnd);
                return true;
            } else if (draggingAudioHandle == 3) {
                float newOffset = dragAudioOriginalOffset + deltaSec;
                AudioPlaybackService.getInstance().moveSegment(draggingAudioSegment, newOffset);
                return true;
            }
        }

        if (draggingPlayhead) {
            int leftBound = x + TRACK_HEADER_WIDTH + 1;
            int rightBound = x + width - 35;
            if (mouseX < leftBound && scrollOffset > 0) {
                float over = leftBound - (float) mouseX;
                scrollOffset = Math.max(0.0f, scrollOffset - Math.min(30.0f, over * 0.4f + 2.0f));
            } else if (mouseX > rightBound) {
                float over = (float) mouseX - rightBound;
                scrollOffset += Math.min(30.0f, over * 0.4f + 2.0f);
            }
            this.currentFrame = Math.max(0, screenXToFrame((float) mouseX));
            checkAutoExtendTimeline();
            if (playing) {
                AudioPlaybackService.getInstance().playFrom((float) currentFrame / fps);
            }
            return true;
        }

        if (draggingZoomSlider) {
            int by = y + 3;
            int bh = 16;
            int rx = x + width - 6 - 19 - 20;
            int zoomSliderW = 36;
            int sliderX = rx - zoomSliderW;

            updateZoomFromSlider(mouseX, sliderX, zoomSliderW);

            return true;
        }

        if (isDraggingKeyframes && clip != null && !activeDraggedKeys.isEmpty()) {
            int deltaFrames = Math.round((float) (mouseX - dragKeyframesStartX) / pixelsPerFrame);
            float deltaTime = (float) deltaFrames / fps;

            applyKeyframeDragOffset(deltaTime);

            return true;
        }

        if (isBoxSelecting) {
            boxCurrentX = mouseX;
            boxCurrentY = mouseY;

            return true;
        }

        return false;
    }

    private void applyKeyframeDragOffset(float deltaTime) {
        if (clip == null) return;

        for (DraggedKeyRecord rec : activeDraggedKeys) {
            float prevTime = Math.max(0.0f, Math.round((rec.originalTime() + currentDragDeltaTime) * fps) / (float) fps);

            if (rec.channel() == ChannelType.CAMERA) {
                clip.getCameraTrack().removeKey(prevTime);
            } else if (rec.channel() == ChannelType.PARTICLE) {
                clip.getParticleTrack().removeKeyframeAt(prevTime);
            } else {
                BoneTrack track = getBoneTrack(rec.bone());

                if (track != null) {
                    track.removeKeyframe(rec.channel(), prevTime);
                }
            }
        }

        currentDragDeltaTime = deltaTime;

        for (DraggedKeyRecord rec : activeDraggedKeys) {
            float newTime = Math.max(0.0f, Math.round((rec.originalTime() + currentDragDeltaTime) * fps) / (float) fps);

            if (rec.channel() == ChannelType.CAMERA && rec.camKey() != null) {
                CameraTrack.CameraKeyframe old = rec.camKey();

                clip.getCameraTrack().addKey(new CameraTrack.CameraKeyframe(newTime, old.x(), old.y(), old.z(), old.yaw(), old.pitch(), old.roll(), old.fov(), old.easing(), old.handles()));
            } else if (rec.channel() == ChannelType.PARTICLE && rec.particleKey() != null) {
                clip.getParticleTrack().addKeyframe(rec.particleKey().withTime(newTime));
            } else if (rec.keyframe() != null) {
                BoneTrack track = getOrCreateBoneTrack(rec.bone());
                Keyframe old = rec.keyframe();

                track.addKeyframe(rec.channel(), new Keyframe(newTime, old.value(), old.easing(), old.handles(), old.expressionSource()));
            }
        }
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingAudioSegment != null || draggingAudioHandle > 0) {
            draggingAudioSegment = null;
            dragAudioBaseData = null;
            draggingAudioHandle = 0;
            return true;
        }

        if (draggingPlayhead) {
            draggingPlayhead = false;
            if (playing) {
                AudioPlaybackService.getInstance().playFrom((float) currentFrame / fps);
            }
            return true;
        }

        if (draggingZoomSlider) {
            draggingZoomSlider = false;

            return true;
        }

        if (isDraggingKeyframes) {
            isDraggingKeyframes = false;

            selectedKeyframes.clear();

            for (DraggedKeyRecord rec : activeDraggedKeys) {
                float finalTime = Math.max(0.0f, Math.round((rec.originalTime() + currentDragDeltaTime) * fps) / (float) fps);

                if (rec.channel() == ChannelType.CAMERA) {
                    selectedKeyframes.add(new KeyframeRef("camera", ChannelType.CAMERA, finalTime, null));
                } else if (clip != null) {
                    BoneTrack track = getBoneTrack(rec.bone());

                    if (track != null) {
                        Keyframe kf = track.getKeyframe(rec.channel(), finalTime);

                        if (kf != null) {
                            selectedKeyframes.add(new KeyframeRef(rec.bone(), rec.channel(), finalTime, kf));
                        }
                    }
                }
            }

            activeDraggedKeys.clear();
            currentDragDeltaTime = 0.0f;

            if (actionListener != null) {
                actionListener.onAddKeyframe();
            }

            return true;
        }

        if (isBoxSelecting) {
            isBoxSelecting = false;
            int bx0 = (int) Math.min(boxStartX, boxCurrentX);
            int by0 = (int) Math.min(boxStartY, boxCurrentY);
            int bx1 = (int) Math.max(boxStartX, boxCurrentX);
            int by1 = (int) Math.max(boxStartY, boxCurrentY);

            if (clip != null && Math.abs(bx1 - bx0) > 3 && Math.abs(by1 - by0) > 3) {
                int trackTop = y + TOOLBAR_HEIGHT + RULER_HEIGHT - verticalTrackScrollOffset;

                if (modelTreeExpanded) {
                    for (int i = 0; i < activeTrackBones.length; i++) {
                        int ty = trackTop + (1 + i) * TRACK_HEIGHT;
                        int cy = ty + TRACK_HEIGHT / 2;

                        if (cy >= by0 && cy <= by1) {
                            String bone = activeTrackBones[i];
                            BoneTrack track = getBoneTrack(bone);

                            if (track != null) {
                                for (Map.Entry<Float, Keyframe> entry : track.getRotationKeys().entrySet()) {
                                    float fx = frameToScreenX((int) (entry.getKey() * fps));

                                    if (fx >= bx0 && fx <= bx1) {
                                        selectedKeyframes.add(new KeyframeRef(bone, ChannelType.ROTATION, entry.getKey(), entry.getValue()));
                                    }
                                }

                                for (Map.Entry<Float, Keyframe> entry : track.getPositionKeys().entrySet()) {
                                    float fx = frameToScreenX((int) (entry.getKey() * fps));

                                    if (fx >= bx0 && fx <= bx1) {
                                        selectedKeyframes.add(new KeyframeRef(bone, ChannelType.POSITION, entry.getKey(), entry.getValue()));
                                    }
                                }
                            }
                        }
                    }
                }

                int cameraTrackY = trackTop + (modelTreeExpanded ? (1 + activeTrackBones.length) : 1) * TRACK_HEIGHT;
                int camCy = cameraTrackY + TRACK_HEIGHT / 2;

                if (camCy >= by0 && camCy <= by1) {
                    for (Map.Entry<Float, CameraTrack.CameraKeyframe> entry : clip.getCameraTrack().getKeyframes().entrySet()) {
                        float fx = frameToScreenX((int) (entry.getKey() * fps));

                        if (fx >= bx0 && fx <= bx1) {
                            selectedKeyframes.add(new KeyframeRef("camera", ChannelType.CAMERA, entry.getKey(), null));
                        }
                    }
                }
            }

            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false;
        }

        if (Screen.hasControlDown()) {
            float oldZoom = pixelsPerFrame;
            float zoomDelta = (float) (verticalAmount * Math.max(0.5f, oldZoom * 0.15f));
            pixelsPerFrame = Math.max(0.5f, Math.min(30.0f, pixelsPerFrame + zoomDelta));
            float mouseRel = (float) (mouseX - x - 70 + scrollOffset);
            float frameAtMouse = mouseRel / oldZoom;
            scrollOffset = Math.max(0.0f, frameAtMouse * pixelsPerFrame - (float) (mouseX - x - 70));
            return true;
        } else if (Screen.hasShiftDown()) {
            scrollOffset = Math.max(0.0f, (float) (scrollOffset - verticalAmount * 25.0));
            return true;
        } else {
            boolean hasParticles = (clip != null && clip.getParticleTrack() != null && !clip.getParticleTrack().isEmpty());
            int totalTracks = 1 + (modelTreeExpanded ? activeTrackBones.length : 0) + 1 + (hasParticles ? 1 : 0) + 1 + 1 + 1;
            int visibleAreaHeight = height - TOOLBAR_HEIGHT - RULER_HEIGHT;
            int maxScroll = Math.max(0, totalTracks * TRACK_HEIGHT - visibleAreaHeight);
            verticalTrackScrollOffset = Math.max(0, Math.min(maxScroll, (int) (verticalTrackScrollOffset - verticalAmount * TRACK_HEIGHT)));
            return true;
        }
    }

    public float frameToScreenX(int frame) {
        return x + TRACK_HEADER_WIDTH + 10 + frame * pixelsPerFrame - scrollOffset;
    }

    public int screenXToFrame(float sx) {
        return Math.round((sx - (x + TRACK_HEADER_WIDTH + 10) + scrollOffset) / pixelsPerFrame);
    }

    public int getTotalFrames() {
        float len = (clip != null) ? clip.getLength() : 0.0f;
        net.openstage.client.audio.AudioTrackData audioData = AudioPlaybackService.getInstance().getCurrentTrack();

        if (audioData != null && audioData.duration() > len) {
            len = audioData.duration();
        }

        int framesFromLen = (int) (len * fps);
        return Math.max(timelineCapacityFrames, framesFromLen);
    }

    public float getCurrentTimeSeconds() {
        if (playing) {
            long now = System.nanoTime();
            float sub = Math.clamp((float) ((now - lastTimeNano) / 1_000_000_000.0 * fps), 0.0f, 1.0f);

            return (currentFrame + sub) / (float) fps;
        }

        return (float) currentFrame / (float) fps;
    }

    public void setFps(int newFps) {
        if (newFps <= 0) return;
        float currentSeconds = getCurrentTimeSeconds();
        this.fps = newFps;
        this.currentFrame = Math.round(currentSeconds * fps);
    }

    public int getFps() {
        return fps;
    }

    public void play() {
        this.playing = true;
        this.lastTimeNano = System.nanoTime();

        AudioPlaybackService.getInstance().playFrom((float) currentFrame / fps);
    }

    public void pause() {
        this.playing = false;

        AudioPlaybackService.getInstance().stop();
    }

    public void togglePlay() {
        if (playing) pause();

        else play();
    }

    public boolean isPlaying() {
        return playing;
    }

    public void stepFrame(int delta) {
        this.currentFrame = Math.max(0, this.currentFrame + delta);
        checkAutoExtendTimeline();
    }

    public void seek(int frame) {
        this.currentFrame = Math.max(0, frame);
        checkAutoExtendTimeline();
    }

    public void setCurrentFrame(int frame) {
        seek(frame);
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public void setCurrentTime(float time) {
        this.currentFrame = Math.max(0, Math.round(time * fps));
    }
}

