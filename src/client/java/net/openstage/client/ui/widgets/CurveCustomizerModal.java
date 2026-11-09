package net.openstage.client.ui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.openstage.client.theme.EditorTheme;
import net.openstage.core.animation.curve.CurvePreset;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class CurveCustomizerModal {
    public static class Point {
        public float x;
        public float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private boolean visible = false;
    private boolean closing = false;
    private long openStartTime = 0;
    private long closeStartTime = 0;
    private boolean uiAnimations = true;
    private CurvePreset currentPreset;
    private final List<Point> points = new ArrayList<>();
    private final Consumer<float[]> onApply;
    private final Runnable onClose;
    private int screenW, screenH;
    private int draggingPointIndex = -1;
    private EditorTheme theme = EditorTheme.DARK_SLATE;

    public CurveCustomizerModal(Consumer<float[]> onApply, Runnable onClose) {
        this.onApply = onApply;
        this.onClose = onClose;
    }

    public void setTheme(EditorTheme theme) {
        if (theme != null) this.theme = theme;
    }

    public void setUiAnimations(boolean uiAnimations) {
        this.uiAnimations = uiAnimations;
    }

    public void open(CurvePreset preset, float[] currentHandles) {
        this.currentPreset = preset;
        this.points.clear();

        float[] src = (currentHandles != null && currentHandles.length >= 4)
                ? currentHandles
                : preset.defaultPoints();

        for (int i = 0; i < src.length; i += 2) {
            points.add(new Point(src[i], src[i + 1]));
        }
        points.sort(Comparator.comparingDouble(p -> p.x));
        this.visible = true;
        this.closing = false;
        this.openStartTime = System.currentTimeMillis();
        this.draggingPointIndex = -1;
    }

    public void close() {
        if (!uiAnimations) {
            this.visible = false;
            this.closing = false;
            this.draggingPointIndex = -1;
            if (onClose != null) onClose.run();
            return;
        }
        if (visible && !closing) {
            this.closing = true;
            this.closeStartTime = System.currentTimeMillis();
            this.draggingPointIndex = -1;
        }
    }

    public boolean isVisible() {
        return visible || closing;
    }

    public void setScreenSize(int w, int h) {
        this.screenW = w;
        this.screenH = h;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if ((!visible && !closing) || currentPreset == null) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        long now = System.currentTimeMillis();
        float ease = 1.0f;
        float progress = 1.0f;

        if (closing) {
            float p = uiAnimations ? Math.min(1.0f, (now - closeStartTime) / 140.0f) : 1.0f;
            progress = p;
            ease = (float) Math.pow(1.0f - p, 3);
            if (p >= 1.0f) {
                visible = false;
                closing = false;
                if (onClose != null) onClose.run();
                return;
            }
        } else {
            float p = uiAnimations ? Math.min(1.0f, (now - openStartTime) / 160.0f) : 1.0f;
            progress = p;
            ease = 1.0f - (float) Math.pow(1.0f - p, 3);
        }

        float scale = uiAnimations ? (0.88f + 0.12f * ease) : 1.0f;
        int bgAlpha = uiAnimations ? (int) (0xCC * ease) : 0xCC;

        graphics.fill(0, 0, screenW, screenH, (bgAlpha << 24));
        int modalW = Math.min(420, screenW - 40);
        int modalH = Math.min(280, screenH - 40);
        int mx = (screenW - modalW) / 2;
        int my = (screenH - modalH) / 2;
        int cx = mx + modalW / 2;
        int cy = my + modalH / 2;
        int effMouseX = (progress < 1.0f && scale > 0.001f) ? (int) ((mouseX - cx) / scale + cx) : mouseX;
        int effMouseY = (progress < 1.0f && scale > 0.001f) ? (int) ((mouseY - cy) / scale + cy) : mouseY;

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().translate(-cx, -cy, 0);

        graphics.fill(mx, my, mx + modalW, my + modalH, 0xFF0A0F14);
        graphics.renderOutline(mx, my, modalW, modalH, 0xFF223540);
        int graphX = mx + 24;
        int graphY = my + 24;
        int graphW = modalW - 48;
        int graphH = modalH - 74;
        int yTop = graphY + (int) (graphH * 0.15f);
        int yBottom = graphY + (int) (graphH * 0.85f);
        int yMid = (yTop + yBottom) / 2;

        for (int gx = graphX; gx < graphX + graphW; gx += 8) {
            graphics.fill(gx, yTop, Math.min(gx + 4, graphX + graphW), yTop + 1, 0x44FFFFFF);
            graphics.fill(gx, yBottom, Math.min(gx + 4, graphX + graphW), yBottom + 1, 0x44FFFFFF);
        }
        graphics.fill(graphX, yMid, graphX + graphW, yMid + 1, 0x33FFFFFF);
        graphics.fill(graphX, graphY, graphX + 2, graphY + graphH, 0xAAFFFFFF);
        float[] arr = toArray();
        int steps = 120;
        int prevPx = graphX;
        int prevPy = toScreenY(CurvePreset.evaluate(arr, 0.0f), yTop, yBottom);

        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            int px = graphX + (int) (t * graphW);
            float valY = CurvePreset.evaluate(arr, t);
            int py = toScreenY(valY, yTop, yBottom);

            graphics.fill(Math.min(prevPx, px), Math.min(prevPy, py), Math.max(prevPx, px) + 1, Math.max(prevPy, py) + 2, 0xFF00E5FF);
            prevPx = px;
            prevPy = py;
        }

        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            int ptX = graphX + (int) (p.x * graphW);
            int ptY = toScreenY(p.y, yTop, yBottom);
            boolean hov = Math.hypot(effMouseX - ptX, effMouseY - ptY) <= 8;
            boolean isDragged = (draggingPointIndex == i);
            int outerColor = (hov || isDragged) ? 0xFF00E5FF : 0xFFFFFFFF;
            int innerColor = (isDragged) ? 0xFF00E5FF : 0xFF000000;

            graphics.fill(ptX - 5, ptY - 5, ptX + 6, ptY + 6, outerColor);
            graphics.fill(ptX - 3, ptY - 3, ptX + 4, ptY + 4, innerColor);
        }

        int bottomY = my + modalH - 36;
        int btnH = 22;
        boolean resetHov = effMouseX >= mx + 16 && effMouseX <= mx + 76 && effMouseY >= bottomY && effMouseY <= bottomY + btnH;
        graphics.fill(mx + 16, bottomY, mx + 76, bottomY + btnH, resetHov ? 0xFF334155 : 0xFF1E293B);
        graphics.renderOutline(mx + 16, bottomY, 60, btnH, 0xFF475569);
        graphics.drawString(font, "Сброс", mx + 28, bottomY + 7, resetHov ? 0xFFFFFFFF : 0xFF94A3B8, false);
        int titleW = font.width(currentPreset.displayName());
        graphics.drawString(font, currentPreset.displayName(), mx + (modalW - titleW) / 2, bottomY + 7, 0xFFE2E8F0, false);
        int okW = 32;
        int okX = mx + modalW - okW - 16;
        boolean okHov = effMouseX >= okX && effMouseX <= okX + okW && effMouseY >= bottomY && effMouseY <= bottomY + btnH;
        graphics.fill(okX, bottomY, okX + okW, bottomY + btnH, okHov ? 0xFF0284C7 : 0xFF0369A1);
        graphics.renderOutline(okX, bottomY, okW, btnH, 0xFF38BDF8);
        graphics.drawString(font, "✓", okX + 11, bottomY + 6, 0xFFFFFFFF, false);

        graphics.pose().popPose();
    }

    private int toScreenY(float valY, int yTop, int yBottom) {
        float span = yBottom - yTop;
        int py = (int) (yBottom - valY * span);
        return Math.clamp(py, yTop - (int) (span * 0.5f), yBottom + (int) (span * 0.5f));
    }

    private float fromScreenY(int screenY, int yTop, int yBottom) {
        float span = yBottom - yTop;
        return (yBottom - screenY) / span;
    }

    private float[] toArray() {
        float[] arr = new float[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            arr[i * 2] = points.get(i).x;
            arr[i * 2 + 1] = points.get(i).y;
        }
        return arr;
    }

    private double getEffX(double x) {
        long elapsed = System.currentTimeMillis() - openStartTime;
        float progress = Math.min(1.0f, elapsed / 170.0f);
        if (progress >= 1.0f) return x;
        float ease = 1.0f - (float) Math.pow(1.0f - progress, 3);
        float scale = 0.88f + 0.12f * ease;
        int modalW = Math.min(420, screenW - 40);
        int cx = (screenW - modalW) / 2 + modalW / 2;
        return (x - cx) / scale + cx;
    }

    private double getEffY(double y) {
        long elapsed = System.currentTimeMillis() - openStartTime;
        float progress = Math.min(1.0f, elapsed / 170.0f);
        if (progress >= 1.0f) return y;
        float ease = 1.0f - (float) Math.pow(1.0f - progress, 3);
        float scale = 0.88f + 0.12f * ease;
        int modalH = Math.min(280, screenH - 40);
        int cy = (screenH - modalH) / 2 + modalH / 2;
        return (y - cy) / scale + cy;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (closing) return true;
        if (!visible || currentPreset == null) return false;
        long elapsed = System.currentTimeMillis() - openStartTime;
        if (elapsed < 30) return true;
        double effMx = getEffX(mx);
        double effMy = getEffY(my);
        int modalW = Math.min(420, screenW - 40);
        int modalH = Math.min(280, screenH - 40);
        int bx = (screenW - modalW) / 2;
        int by = (screenH - modalH) / 2;
        int bottomY = by + modalH - 36;
        int btnH = 22;

        if (effMx >= bx + 16 && effMx <= bx + 76 && effMy >= bottomY && effMy <= bottomY + btnH) {
            float[] def = currentPreset.copyDefaultPoints();
            points.clear();
            for (int i = 0; i < def.length; i += 2) {
                points.add(new Point(def[i], def[i + 1]));
            }
            return true;
        }

        int okW = 32;
        int okX = bx + modalW - okW - 16;
        if (effMx >= okX && effMx <= okX + okW && effMy >= bottomY && effMy <= bottomY + btnH) {
            if (onApply != null) {
                onApply.accept(toArray());
            }
            close();
            return true;
        }

        int graphX = bx + 24;
        int graphY = by + 24;
        int graphW = modalW - 48;
        int graphH = modalH - 74;
        int yTop = graphY + (int) (graphH * 0.15f);
        int yBottom = graphY + (int) (graphH * 0.85f);

        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            int cx = graphX + (int) (p.x * graphW);
            int cy = toScreenY(p.y, yTop, yBottom);
            if (Math.hypot(effMx - cx, effMy - cy) <= 8) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && points.size() > 2) {
                    points.remove(i);
                    return true;
                }
                draggingPointIndex = i;
                return true;
            }
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && effMx >= graphX && effMx <= graphX + graphW && effMy >= graphY && effMy <= graphY + graphH) {
            float newX = Math.clamp((float) (effMx - graphX) / graphW, 0.0f, 1.0f);
            float newY = fromScreenY((int) effMy, yTop, yBottom);
            points.add(new Point(newX, newY));
            points.sort(Comparator.comparingDouble(p -> p.x));
            for (int i = 0; i < points.size(); i++) {
                if (Math.abs(points.get(i).x - newX) < 0.001f) {
                    draggingPointIndex = i;
                    break;
                }
            }
            return true;
        }

        return true;
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (closing) return true;
        if (!visible || draggingPointIndex < 0 || draggingPointIndex >= points.size()) {
            return false;
        }

        double effMx = getEffX(mx);
        double effMy = getEffY(my);
        int modalW = Math.min(420, screenW - 40);
        int modalH = Math.min(280, screenH - 40);
        int bx = (screenW - modalW) / 2;
        int by = (screenH - modalH) / 2;
        int graphX = bx + 24;
        int graphY = by + 24;
        int graphW = modalW - 48;
        int graphH = modalH - 74;
        int yTop = graphY + (int) (graphH * 0.15f);
        int yBottom = graphY + (int) (graphH * 0.85f);
        Point p = points.get(draggingPointIndex);

        if (draggingPointIndex == 0) {
            p.x = 0.0f;
        } else if (draggingPointIndex == points.size() - 1) {
            p.x = 1.0f;
        } else {
            float minX = points.get(draggingPointIndex - 1).x + 0.01f;
            float maxX = points.get(draggingPointIndex + 1).x - 0.01f;
            p.x = Math.clamp((float) (effMx - graphX) / graphW, minX, maxX);
        }

        p.y = fromScreenY((int) effMy, yTop, yBottom);
        return true;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        if (closing) return true;
        if (draggingPointIndex >= 0) {
            draggingPointIndex = -1;
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) return true;
        if (!visible) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return false;
    }
}

