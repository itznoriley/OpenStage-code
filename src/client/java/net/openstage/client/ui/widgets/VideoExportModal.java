package net.openstage.client.ui.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.openstage.client.theme.EditorTheme;

public final class VideoExportModal {
    public interface ExportListener {
        void onStartExport(int width, int height, int fps, boolean useFfmpegMp4, int bitrateMbps);
        void onCancelExport();
    }

    private boolean visible = false;
    private boolean closing = false;
    private long openStartTime = 0;
    private long closeStartTime = 0;
    private int width = 320;
    private int height = 230;
    private int resIndex = 0;
    private static final String[] RES_NAMES = {"1920x1080 (1080p)", "1280x720 (720p)", "2560x1440 (2K)", "3840x2160 (4K)"};
    private static final int[][] RES_VALUES = {{1920, 1080}, {1280, 720}, {2560, 1440}, {3840, 2160}};
    private int fpsIndex = 2;
    private static final int[] FPS_VALUES = {24, 30, 60, 120};
    private boolean formatMp4 = true;
    private int bitrateIndex = 1;
    private static final int[] BITRATES = {8, 16, 32, 50};
    private boolean exporting = false;
    private float progress = 0.0f;
    private String statusText = "Готов к рендерингу";
    private ExportListener listener;

    public void setListener(ExportListener listener) {
        this.listener = listener;
    }

    public boolean isVisible() {
        return visible || closing;
    }

    public void open() {
        this.visible = true;
        this.closing = false;
        this.exporting = false;
        this.progress = 0.0f;
        this.statusText = "Готов к рендерингу";
        this.openStartTime = System.currentTimeMillis();
    }

    public void close() {
        if (visible && !closing) {
            this.closing = true;
            this.closeStartTime = System.currentTimeMillis();
            if (listener != null && exporting) {
                listener.onCancelExport();
            }
            this.exporting = false;
        }
    }

    public void updateProgress(float progress, String statusText) {
        this.progress = Math.clamp(progress, 0.0f, 1.0f);
        this.statusText = statusText != null ? statusText : "";
    }

    public void setExporting(boolean exporting) {
        this.exporting = exporting;
    }

    public boolean isExporting() {
        return exporting;
    }

    public void render(GuiGraphics graphics, Font font, int screenW, int screenH, int mouseX, int mouseY, EditorTheme theme) {
        if (!visible && !closing) return;
        long now = System.currentTimeMillis();
        float ease = 1.0f;

        if (closing) {
            float p = Math.min(1.0f, (now - closeStartTime) / 140.0f);
            ease = (float) Math.pow(1.0f - p, 3);
            if (p >= 1.0f) {
                visible = false;
                closing = false;
                return;
            }
        } else {
            float p = Math.min(1.0f, (now - openStartTime) / 160.0f);
            ease = 1.0f - (float) Math.pow(1.0f - p, 3);
        }

        float scale = 0.88f + 0.12f * ease;
        int bgAlpha = (int) (0x88 * ease);
        graphics.fill(0, 0, screenW, screenH, (bgAlpha << 24));
        int modalX = (screenW - width) / 2;
        int modalY = (screenH - height) / 2;
        int cx = modalX + width / 2;
        int cy = modalY + height / 2;

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().translate(-cx, -cy, 0);
        int panelBg = (theme != null) ? theme.getPanelBg() : 0xFF141923;
        int divider = (theme != null) ? theme.getDivider() : 0xFF2A364F;
        int accent = (theme != null) ? theme.getAccent() : 0xFF38BDF8;
        int textCol = (theme != null) ? theme.getTextColor() : 0xFFFFFFFF;
        int textMuted = (theme != null) ? theme.getTextMuted() : 0xFF94A3B8;

        graphics.fill(modalX, modalY, modalX + width, modalY + height, panelBg);
        graphics.renderOutline(modalX, modalY, width, height, divider);
        graphics.fill(modalX, modalY, modalX + width, modalY + 24, (divider & 0x00FFFFFF) | 0x44000000);

        graphics.drawString(font, "🎬 Video Export (Offline Render)", modalX + 10, modalY + 8, accent, false);
        int closeX = modalX + width - 20;
        int closeY = modalY + 6;
        boolean closeHov = mouseX >= closeX && mouseX <= closeX + 14 && mouseY >= closeY && mouseY <= closeY + 14;
        graphics.drawString(font, "✕", closeX, closeY, closeHov ? 0xFFF87171 : textMuted, false);
        int py = modalY + 34;
        int btnW = width - 20;

        graphics.drawString(font, "Resolution:", modalX + 10, py + 3, textMuted, false);
        drawOptionButton(graphics, font, RES_NAMES[resIndex], modalX + 110, py, btnW - 100, 16, mouseX, mouseY, theme);
        py += 22;

        graphics.drawString(font, "Framerate:", modalX + 10, py + 3, textMuted, false);
        drawOptionButton(graphics, font, FPS_VALUES[fpsIndex] + " FPS", modalX + 110, py, btnW - 100, 16, mouseX, mouseY, theme);
        py += 22;

        graphics.drawString(font, "Format:", modalX + 10, py + 3, textMuted, false);
        String formatLabel = formatMp4 ? "MP4 (H.264 FFmpeg Pipe)" : "PNG Image Sequence";
        drawOptionButton(graphics, font, formatLabel, modalX + 110, py, btnW - 100, 16, mouseX, mouseY, theme);
        py += 22;

        if (formatMp4) {
            graphics.drawString(font, "Video Bitrate:", modalX + 10, py + 3, textMuted, false);
            drawOptionButton(graphics, font, BITRATES[bitrateIndex] + " Mbps", modalX + 110, py, btnW - 100, 16, mouseX, mouseY, theme);
            py += 22;
        } else {
            py += 4;
        }

        graphics.fill(modalX + 10, py, modalX + width - 10, py + 14, 0xFF0B0F19);
        graphics.renderOutline(modalX + 10, py, width - 20, 14, divider);
        int fillW = (int) ((width - 22) * progress);
        if (fillW > 0) {
            graphics.fill(modalX + 11, py + 1, modalX + 11 + fillW, py + 13, accent);
        }
        graphics.drawCenteredString(font, statusText, modalX + width / 2, py + 3, textCol);
        py += 22;
        int actionBtnY = modalY + height - 26;
        if (!exporting) {
            boolean startHov = mouseX >= modalX + 10 && mouseX <= modalX + width - 10 && mouseY >= actionBtnY && mouseY <= actionBtnY + 18;
            int startBg = startHov ? 0xFF0284C7 : 0xFF0369A1;
            graphics.fill(modalX + 10, actionBtnY, modalX + width - 10, actionBtnY + 18, startBg);
            graphics.renderOutline(modalX + 10, actionBtnY, width - 20, 18, accent);
            graphics.drawCenteredString(font, "🎬 Start Export", modalX + width / 2, actionBtnY + 5, 0xFFFFFFFF);
        } else {
            boolean cancelHov = mouseX >= modalX + 10 && mouseX <= modalX + width - 10 && mouseY >= actionBtnY && mouseY <= actionBtnY + 18;
            int cancelBg = cancelHov ? 0xFFDC2626 : 0xFFB91C1C;
            graphics.fill(modalX + 10, actionBtnY, modalX + width - 10, actionBtnY + 18, cancelBg);
            graphics.drawCenteredString(font, "⛔ Cancel Export", modalX + width / 2, actionBtnY + 5, 0xFFFFFFFF);
        }

        graphics.pose().popPose();
    }

    private void drawOptionButton(GuiGraphics graphics, Font font, String label, int bx, int by, int bw, int bh, int mouseX, int mouseY, EditorTheme theme) {
        boolean hov = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        int bg = hov ? ((theme != null) ? theme.getButtonHover() : 0xFF222F43) : ((theme != null) ? theme.getButtonBg() : 0xFF192231);
        int border = (theme != null) ? theme.getDivider() : 0xFF2A364F;
        graphics.fill(bx, by, bx + bw, by + bh, bg);
        graphics.renderOutline(bx, by, bw, bh, border);
        graphics.drawCenteredString(font, label, bx + bw / 2, by + 4, 0xFFFFFFFF);
    }

    public boolean mouseClicked(double mx, double my, int button, int screenW, int screenH) {
        if (!visible || closing) return closing;
        int modalX = (screenW - width) / 2;
        int modalY = (screenH - height) / 2;

        if (mx < modalX || mx > modalX + width || my < modalY || my > modalY + height) {
            close();
            return true;
        }

        int closeX = modalX + width - 20;
        int closeY = modalY + 6;
        if (mx >= closeX && mx <= closeX + 14 && my >= closeY && my <= closeY + 14) {
            close();
            return true;
        }

        int py = modalY + 34;
        int btnW = width - 20;

        if (mx >= modalX + 110 && mx <= modalX + 110 + (btnW - 100) && my >= py && my <= py + 16) {
            resIndex = (resIndex + 1) % RES_NAMES.length;
            return true;
        }
        py += 22;

        if (mx >= modalX + 110 && mx <= modalX + 110 + (btnW - 100) && my >= py && my <= py + 16) {
            fpsIndex = (fpsIndex + 1) % FPS_VALUES.length;
            return true;
        }
        py += 22;

        if (mx >= modalX + 110 && mx <= modalX + 110 + (btnW - 100) && my >= py && my <= py + 16) {
            formatMp4 = !formatMp4;
            return true;
        }
        py += 22;

        if (formatMp4) {
            if (mx >= modalX + 110 && mx <= modalX + 110 + (btnW - 100) && my >= py && my <= py + 16) {
                bitrateIndex = (bitrateIndex + 1) % BITRATES.length;
                return true;
            }
            py += 22;
        } else {
            py += 4;
        }

        int actionBtnY = modalY + height - 26;
        if (mx >= modalX + 10 && mx <= modalX + width - 10 && my >= actionBtnY && my <= actionBtnY + 18) {
            if (!exporting) {
                exporting = true;
                progress = 0.0f;
                statusText = "Инициализация...";
                if (listener != null) {
                    listener.onStartExport(
                            RES_VALUES[resIndex][0],
                            RES_VALUES[resIndex][1],
                            FPS_VALUES[fpsIndex],
                            formatMp4,
                            BITRATES[bitrateIndex]
                    );
                }
            } else {
                exporting = false;
                statusText = "Экспорт отменен";
                if (listener != null) {
                    listener.onCancelExport();
                }
            }
            return true;
        }

        return true;
    }
}

