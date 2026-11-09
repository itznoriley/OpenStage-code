package net.openstage.client.ui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.openstage.client.theme.EditorTheme;
import net.openstage.core.animation.AnimationClip;
import net.openstage.core.animation.BoneTrack;
import net.openstage.core.animation.Keyframe;
import net.openstage.core.animation.curve.CurvePreset;
import net.openstage.core.animation.curve.CurvePresetRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphEditorPanel {
    private int x, y, width, height;
    private AnimationClip clip;
    private String selectedBone = "body";
    private int fps = 30;
    private int currentFrame = 0;
    private EditorTheme theme = null;
    private int scrollOffset = 0;
    private CurvePreset selectedPreset = null;
    private final Map<String, float[]> customPresetHandles = new HashMap<>();
    private final CurveCustomizerModal customizerModal;

    public interface GraphActionListener {
        void onCloseGraph();

        void onKeyframeModified();
    }

    private GraphActionListener actionListener;

    public GraphEditorPanel(int x, int y, int width, int height, int fps) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.selectedPreset = CurvePresetRegistry.getAll().getFirst();

        this.customizerModal = new CurveCustomizerModal(
                this::onCustomCurveApplied,

                () -> {}
        );
    }

    public void setTheme(EditorTheme theme) {
        this.theme = theme;

        this.customizerModal.setTheme(theme);
    }

    public void setActionListener(GraphActionListener listener) {
        this.actionListener = listener;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setClip(AnimationClip clip) {
        this.clip = clip;
    }

    public void setSelectedBone(String bone) {
        this.selectedBone = (bone != null) ? bone : "body";
    }

    public void setCurrentFrame(int frame) {
        this.currentFrame = frame;
    }

    public CurveCustomizerModal getCustomizerModal() {
        return customizerModal;
    }

    private void onCustomCurveApplied(float[] points) {
        if (selectedPreset == null) return;

        customPresetHandles.put(selectedPreset.id(), points);

        applyCurrentCurveToClip(points);

        if (actionListener != null) {
            actionListener.onKeyframeModified();
        }
    }

    private void applyCurrentCurveToClip(float[] points) {
        if (clip == null) return;

        if ("model".equalsIgnoreCase(selectedBone)) {
            for (BoneTrack track : clip.getBones().values()) {
                applyPointsToTrack(track, points);
            }
        } else if ("camera".equalsIgnoreCase(selectedBone)) {
            net.openstage.core.scene.CameraTrack track = clip.getCameraTrack();

            if (track != null) {
                for (var entry : new java.util.ArrayList<>(track.getKeyframes().entrySet())) {
                    net.openstage.core.scene.CameraTrack.CameraKeyframe old = entry.getValue();

                    if (!old.isSharp()) {
                        track.addKey(new net.openstage.core.scene.CameraTrack.CameraKeyframe(
                                old.time(), old.x(), old.y(), old.z(),

                                old.yaw(), old.pitch(), old.roll(), old.fov(),

                                null, points
                        ));
                    }
                }
            }
        } else if ("particles".equalsIgnoreCase(selectedBone)) {
            net.openstage.core.particle.ParticleTrack pTrack = clip.getParticleTrack();

            if (pTrack != null) {
                for (net.openstage.core.particle.ParticleKeyframe old : new java.util.ArrayList<>(pTrack.getKeyframes())) {
                    pTrack.addKeyframe(old.withCurveHandles(points));
                }
            }
        } else {
            BoneTrack track = clip.getBones().get(selectedBone);

            if (track != null) {
                applyPointsToTrack(track, points);
            }
        }
    }

    private void applyPointsToTrack(BoneTrack track, float[] points) {
        var rotMap = track.getRotationKeys();

        for (var entry : rotMap.entrySet()) {
            Keyframe old = entry.getValue();
            Keyframe updated = new Keyframe(old.time(), old.value(), null, points, old.expressionSource());

            track.addKeyframe(net.openstage.core.animation.ChannelType.ROTATION, updated);
        }

        var posMap = track.getPositionKeys();

        for (var entry : posMap.entrySet()) {
            Keyframe old = entry.getValue();
            Keyframe updated = new Keyframe(old.time(), old.value(), null, points, old.expressionSource());

            track.addKeyframe(net.openstage.core.animation.ChannelType.POSITION, updated);
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        customizerModal.setScreenSize(screenW, screenH);

        graphics.enableScissor(x, y, x + width, y + height);

        graphics.fill(x, y, x + width, y + height, (theme != null) ? theme.getTimelineBg() : 0xFF121B20);
        int headerH = 22;

        graphics.fill(x, y, x + width, y + headerH, (theme != null) ? theme.getTitleBarBg() : 0xFF1A262E);

        graphics.fill(x, y + headerH - 1, x + width, y + headerH, (theme != null) ? theme.getDivider() : 0xFF2B3F4D);

        graphics.drawString(font, "Кривые ⓘ", x + 10, y + 7, (theme != null) ? theme.getAccent() : 0xFF5EEAD4, false);
        int closeW = 18;
        int closeX = x + width - closeW - 6;
        boolean closeHov = mouseX >= closeX && mouseX <= closeX + closeW && mouseY >= y + 3 && mouseY <= y + headerH - 3;

        graphics.drawString(font, "✕", closeX + 5, y + 6, closeHov ? 0xFFEF4444 : 0xFF94A3B8, false);
        int cardW = 58;
        int cardH = 70;
        int startX = x + 10 - scrollOffset;
        int cardY = y + headerH + 6;
        List<CurvePreset> list = CurvePresetRegistry.getAll();

        for (int i = 0; i < list.size(); i++) {
            CurvePreset preset = list.get(i);
            int cx = startX + i * (cardW + 8);

            if (cx + cardW < x || cx > x + width) continue;
            boolean isSelected = (selectedPreset != null && selectedPreset.id().equals(preset.id()));
            boolean hov = mouseX >= cx && mouseX <= cx + cardW && mouseY >= cardY && mouseY <= cardY + cardH;
            int bgCol = isSelected ? 0xFF26333D : (hov ? 0xFF1E2830 : 0xFF151E24);
            int borderCol = isSelected ? 0xFF00E5FF : (hov ? 0xFF475569 : 0xFF2A3742);

            graphics.fill(cx, cardY, cx + cardW, cardY + cardH, bgCol);

            graphics.renderOutline(cx, cardY, cardW, cardH, borderCol);
            int previewH = 40;
            int pyTop = cardY + 6;
            int pyBottom = cardY + previewH - 4;
            int pyMid = (pyTop + pyBottom) / 2;

            for (int gx = cx + 4; gx < cx + cardW - 4; gx += 6) {
                graphics.fill(gx, pyTop, Math.min(gx + 3, cx + cardW - 4), pyTop + 1, 0x22FFFFFF);

                graphics.fill(gx, pyBottom, Math.min(gx + 3, cx + cardW - 4), pyBottom + 1, 0x22FFFFFF);
            }

            graphics.fill(cx + 4, pyMid, cx + cardW - 4, pyMid + 1, 0x18FFFFFF);
            float[] pts = customPresetHandles.getOrDefault(preset.id(), preset.defaultPoints());
            int prevX = cx + 6;
            int prevY = toPreviewY(CurvePreset.evaluate(pts, 0.0f), pyTop, pyBottom);
            int stepCount = 20;
            int pw = cardW - 12;

            for (int s = 1; s <= stepCount; s++) {
                float t = (float) s / stepCount;
                int px = cx + 6 + (int) (t * pw);
                int py = toPreviewY(CurvePreset.evaluate(pts, t), pyTop, pyBottom);

                graphics.fill(Math.min(prevX, px), Math.min(prevY, py), Math.max(prevX, px) + 1, Math.max(prevY, py) + 2, 0xFFFFFFFF);
                prevX = px;
                prevY = py;
            }

            int labelY = cardY + previewH + 4;
            String name = preset.displayName();
            int nw = font.width(name);
            int tx = cx + Math.max(2, (cardW - nw) / 2);

            graphics.enableScissor(cx + 2, labelY, cx + cardW - 2, cardY + cardH - 2);

            graphics.drawString(font, name, tx, labelY, isSelected ? 0xFF00E5FF : 0xFF94A3B8, false);

            graphics.disableScissor();

            if (isSelected) {
                int editH = 12;
                int editY = cardY + cardH - editH - 1;
                boolean editHov = mouseX >= cx + 2 && mouseX <= cx + cardW - 2 && mouseY >= editY && mouseY <= editY + editH;

                graphics.fill(cx + 2, editY, cx + cardW - 2, editY + editH, editHov ? 0xFF0284C7 : 0xFF0369A1);
                String editTxt = "Настроить";
                int ew = font.width(editTxt);

                graphics.drawString(font, editTxt, cx + (cardW - ew) / 2, editY + 2, 0xFFFFFFFF, false);
            }
        }

        graphics.disableScissor();
    }

    private int toPreviewY(float valY, int yTop, int yBottom) {
        float span = yBottom - yTop;
        int py = (int) (yBottom - valY * span);

        return Math.clamp(py, yTop - 4, yBottom + 4);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (customizerModal.isVisible()) {
            return customizerModal.mouseClicked(mx, my, button);
        }

        if (mx < x || mx > x + width || my < y || my > y + height) {
            return false;
        }

        int headerH = 22;
        int closeW = 18;
        int closeX = x + width - closeW - 6;

        if (mx >= closeX && mx <= closeX + closeW && my >= y + 3 && my <= y + headerH - 3) {
            if (actionListener != null) actionListener.onCloseGraph();

            return true;
        }

        int cardW = 58;
        int cardH = 70;
        int startX = x + 10 - scrollOffset;
        int cardY = y + headerH + 6;
        List<CurvePreset> list = CurvePresetRegistry.getAll();

        for (int i = 0; i < list.size(); i++) {
            CurvePreset preset = list.get(i);
            int cx = startX + i * (cardW + 8);

            if (mx >= cx && mx <= cx + cardW && my >= cardY && my <= cardY + cardH) {
                boolean wasSelected = (selectedPreset != null && selectedPreset.id().equals(preset.id()));

                if (wasSelected) {
                    int editH = 12;
                    int editY = cardY + cardH - editH - 1;

                    if (my >= editY && my <= editY + editH) {
                        float[] pts = customPresetHandles.getOrDefault(preset.id(), preset.defaultPoints());

                        customizerModal.open(preset, pts);

                        return true;
                    }
                }

                selectedPreset = preset;

                customPresetHandles.remove(preset.id());

                applyCurrentCurveToClip(preset.defaultPoints());

                if (actionListener != null) {
                    actionListener.onKeyframeModified();
                }

                return true;
            }
        }

        return true;
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (customizerModal.isVisible()) {
            return customizerModal.mouseDragged(mx, my, button, dx, dy);
        }

        if (my >= y && my <= y + height) {
            scrollOffset = (int) Math.max(0, scrollOffset - dx);

            return true;
        }

        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        if (customizerModal.isVisible()) {
            return customizerModal.mouseReleased(mx, my, button);
        }

        return false;
    }

    public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
        if (customizerModal.isVisible()) return true;

        if (my >= y && my <= y + height) {
            scrollOffset = (int) Math.max(0, scrollOffset - verticalAmount * 24);

            return true;
        }

        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (customizerModal.isVisible()) {
            return customizerModal.keyPressed(keyCode, scanCode, modifiers);
        }

        return false;
    }
}

