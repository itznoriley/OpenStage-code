package net.openstage.client.ui.widgets;

import net.openstage.client.ui.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.openstage.client.model.ModelSettings;
import net.openstage.client.theme.EditorTheme;
import org.lwjgl.glfw.GLFW;

public class SettingsDialog {
    private boolean open = false;
    private boolean closing = false;
    private long openStartTime = 0;
    private long closeStartTime = 0;
    private final ModelSettings settings;
    private int listeningKeybind = -1;
    private boolean languageDropdownOpen = false;
    private boolean backgroundDropdownOpen = false;
    private static String donationUrl = "https://www.donationalerts.com/r/itznoriley";

    public static void setDonationUrl(String url) {
        if (url != null && !url.isBlank()) {
            donationUrl = url;
        }
    }

    public static String getDonationUrl() {
        return donationUrl;
    }

    public interface SettingsListener {
        void onThemeChanged(EditorTheme theme);
        void onSettingsClosed();
    }

    private final SettingsListener listener;

    public SettingsDialog(ModelSettings settings, SettingsListener listener) {
        this.settings = settings;
        this.listener = listener;
    }

    public boolean isOpen() {
        return open || closing;
    }

    public void open() {
        this.open = true;
        this.closing = false;
        this.listeningKeybind = -1;
        this.languageDropdownOpen = false;
        this.backgroundDropdownOpen = false;
        this.openStartTime = System.currentTimeMillis();
    }

    public void close() {
        if (settings != null && !settings.isUiAnimations()) {
            this.open = false;
            this.closing = false;
            this.listeningKeybind = -1;
            this.languageDropdownOpen = false;
        this.backgroundDropdownOpen = false;
            if (listener != null) listener.onSettingsClosed();
            return;
        }
        if (open && !closing) {
            this.closing = true;
            this.closeStartTime = System.currentTimeMillis();
            this.listeningKeybind = -1;
        }
    }

    public void render(GuiGraphics graphics, int screenW, int screenH, int mouseX, int mouseY) {
        if (!open && !closing) return;
        boolean anims = (settings == null) || settings.isUiAnimations();
        long now = System.currentTimeMillis();
        float ease = 1.0f;

        if (closing) {
            float p = Math.min(1.0f, (now - closeStartTime) / 140.0f);
            ease = (float) Math.pow(1.0f - p, 3);
            if (p >= 1.0f) {
                open = false;
                closing = false;
                if (listener != null) listener.onSettingsClosed();
                return;
            }
        } else {
            float p = anims ? Math.min(1.0f, (now - openStartTime) / 160.0f) : 1.0f;
            ease = 1.0f - (float) Math.pow(1.0f - p, 3);
        }

        float scale = anims ? (0.88f + 0.12f * ease) : 1.0f;
        int bgAlpha = anims ? (int) (0x77 * ease) : 0x77;

        graphics.fill(0, 0, screenW, screenH, (bgAlpha << 24));
        Font font = Minecraft.getInstance().font;
        int dw = 440;
        int dh = 420;
        int dx = (screenW - dw) / 2;
        int dy = (screenH - dh) / 2;
        int cx = dx + dw / 2;
        int cy = dy + dh / 2;
        int effMouseX = (scale > 0.001f) ? (int) ((mouseX - cx) / scale + cx) : mouseX;
        int effMouseY = (scale > 0.001f) ? (int) ((mouseY - cy) / scale + cy) : mouseY;

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().translate(-cx, -cy, 0);
        EditorTheme theme = settings.getTheme();
        int bg = theme.getPanelBg();
        int titleBg = theme.getTitleBarBg();
        int border = theme.getAccent();

        graphics.fill(dx, dy, dx + dw, dy + dh, bg);
        graphics.fill(dx, dy, dx + dw, dy + 24, titleBg);
        graphics.renderOutline(dx, dy, dw, dh, border);
        ModelSettings.Language lang = (settings != null) ? settings.getLanguage() : ModelSettings.Language.EN;
        graphics.drawString(font, I18n.get("OpenStage Preferences", lang), dx + 12, dy + 8, theme.getAccent(), false);
        int closeSize = 16;
        int closeX = dx + dw - closeSize - 6;
        int closeY = dy + 4;
        boolean closeHov = effMouseX >= closeX && effMouseX <= closeX + closeSize && effMouseY >= closeY && effMouseY <= closeY + closeSize;
        int verW = font.width("v1.0-release");
        int verX = closeX - verW - 8;
        graphics.drawString(font, "v1.0-release", verX, dy + 8, 0xFF64748B, false);

        graphics.fill(closeX, closeY, closeX + closeSize, closeY + closeSize, closeHov ? 0xFFDC2626 : 0xFF334155);
        graphics.renderOutline(closeX, closeY, closeSize, closeSize, closeHov ? 0xFFEF4444 : 0xFF475569);
        int closeTw = font.width("✕");
        int tx = closeX + (closeSize - closeTw) / 2;
        int ty = closeY + (closeSize - 8) / 2;
        graphics.drawString(font, "✕", tx, ty, 0xFFFFFFFF, false);
        int py = dy + 32;

        graphics.drawString(font, I18n.get("UI Color Themes", lang), dx + 12, py, 0xFFFFFFFF, false);
        py += 14;
        EditorTheme[] themes = EditorTheme.values();
        String[] shortNames = {"Dark Slate", "Midnight", "Charcoal", "Emerald", "Amber", "Synthwave", "Monokai", "Light Studio"};
        int colW = 98;
        int colH = 20;
        for (int i = 0; i < themes.length; i++) {
            EditorTheme t = themes[i];
            int bx = dx + 12 + (i % 4) * (colW + 6);
            int by = py + (i / 4) * (colH + 4);
            boolean sel = t == settings.getTheme();
            boolean hov = effMouseX >= bx && effMouseX <= bx + colW && effMouseY >= by && effMouseY <= by + colH;

            graphics.fill(bx, by, bx + colW, by + colH, sel ? t.getAccent() : (hov ? 0xFF2E3D4F : 0xFF1E293B));
            graphics.fill(bx + 4, by + 4, bx + 12, by + colH - 4, t.getAccent());
            graphics.renderOutline(bx, by, colW, colH, sel ? 0xFFFFFFFF : 0xFF334155);
            graphics.drawString(font, I18n.get(shortNames[i], lang), bx + 16, by + 6, sel ? 0xFFFFFFFF : 0xFFCBD5E1, false);
        }
        py += 52;

        graphics.drawString(font, I18n.get("Camera Navigation Mode", lang), dx + 12, py, 0xFFFFFFFF, false);
        py += 14;
        ModelSettings.CameraMode[] modes = ModelSettings.CameraMode.values();
        String[] modeLabels = {"Orbit (Mouse)", "Free Fly (WASD)", "Blender (MMB)"};
        int modeW = 132;
        for (int i = 0; i < modes.length; i++) {
            ModelSettings.CameraMode m = modes[i];
            int bx = dx + 12 + i * (modeW + 8);
            boolean sel = m == settings.getCameraMode();
            boolean hov = effMouseX >= bx && effMouseX <= bx + modeW && effMouseY >= py && effMouseY <= py + colH;

            graphics.fill(bx, py, bx + modeW, py + colH, sel ? theme.getAccent() : (hov ? 0xFF2E3D4F : 0xFF1E293B));
            graphics.renderOutline(bx, py, modeW, colH, sel ? 0xFFFFFFFF : 0xFF334155);
            String ml = I18n.get(modeLabels[i], lang);
            int tw = font.width(ml);
            graphics.drawString(font, ml, bx + (modeW - tw) / 2, py + 6, sel ? 0xFFFFFFFF : 0xFFCBD5E1, false);
        }
        py += 28;

        graphics.drawString(font, I18n.get("Viewport Environment", lang), dx + 12, py, 0xFFFFFFFF, false);
        py += 14;

        drawCheckbox(graphics, font, I18n.get("Floor Grid", lang), settings.isShowGrid(), dx + 12, py, effMouseX, effMouseY);
        drawCheckbox(graphics, font, I18n.get("World Scan", lang), settings.isShowWorldBlocks(), dx + 190, py, effMouseX, effMouseY);
        py += 18;
        drawCheckbox(graphics, font, I18n.get("Gizmo", lang), settings.isShowGizmo(), dx + 12, py, effMouseX, effMouseY);
        drawCheckbox(graphics, font, I18n.get("UI Animations", lang), settings.isUiAnimations(), dx + 190, py, effMouseX, effMouseY);
        py += 20;

        String bgLabel = (lang == ModelSettings.Language.RU) ? "Фон редактора:" : "Editor Backdrop:";
        graphics.drawString(font, bgLabel, dx + 12, py + 4, 0xFF94A3B8, false);
        int bgBtnX = dx + 130;
        int bgBtnW = dw - 142;
        int bgBtnH = 20;
        ModelSettings.BackgroundMode currentBg = settings.getBackgroundMode();
        String currentBgName = (lang == ModelSettings.Language.RU)
                ? (currentBg == ModelSettings.BackgroundMode.CHROMA_GREEN ? "Зелёный (Хромакей)" : (currentBg == ModelSettings.BackgroundMode.CHROMA_BLUE ? "Синий (Хромакей)" : "По умолчанию"))
                : (currentBg == ModelSettings.BackgroundMode.CHROMA_GREEN ? "Chroma Green" : (currentBg == ModelSettings.BackgroundMode.CHROMA_BLUE ? "Chroma Blue" : "Default"));
        String bgText = currentBgName + (backgroundDropdownOpen ? " ▲" : " ▾");
        boolean bgHov = effMouseX >= bgBtnX && effMouseX <= bgBtnX + bgBtnW && effMouseY >= py && effMouseY <= py + bgBtnH;
        graphics.fill(bgBtnX, py, bgBtnX + bgBtnW, py + bgBtnH, backgroundDropdownOpen ? 0xFF253346 : (bgHov ? 0xFF2A3448 : 0xFF141923));
        graphics.renderOutline(bgBtnX, py, bgBtnW, bgBtnH, backgroundDropdownOpen ? theme.getAccent() : (bgHov ? 0xFF38BDF8 : 0xFF334155));
        graphics.drawString(font, bgText, bgBtnX + 8, py + 6, 0xFFFFFFFF, false);

        int savedBgPy = py;
        py += 26;

        graphics.drawString(font, I18n.get("Keybindings", lang), dx + 12, py, 0xFFFFFFFF, false);
        py += 14;

        drawKeybindRow(graphics, font, I18n.get("Play / Pause", lang), getKeyName(settings.getKeyPlayPause()), listeningKeybind == 0, dx + 12, py, effMouseX, effMouseY);
        drawKeybindRow(graphics, font, I18n.get("Add Keyframe", lang), getKeyName(settings.getKeyAddKeyframe()), listeningKeybind == 1, dx + 225, py, effMouseX, effMouseY);
        py += 22;

        drawKeybindRow(graphics, font, I18n.get("Delete Key", lang), getKeyName(settings.getKeyDelete()), listeningKeybind == 2, dx + 12, py, effMouseX, effMouseY);
        drawKeybindRow(graphics, font, I18n.get("Toggle Graph", lang), getKeyName(settings.getKeyToggleGraph()), listeningKeybind == 3, dx + 225, py, effMouseX, effMouseY);
        py += 22;

        drawKeybindRow(graphics, font, I18n.get("Camera View", lang), getKeyName(settings.getKeyCameraView()), listeningKeybind == 4, dx + 12, py, effMouseX, effMouseY);
        py += 24;

        graphics.drawString(font, I18n.get("Interface Language", lang), dx + 12, py, 0xFFFFFFFF, false);
        py += 14;
        int langBtnW = 190;
        int langBtnH = 22;
        int langBtnX = dx + 12;
        int langBtnY = py;
        boolean langHov = effMouseX >= langBtnX && effMouseX <= langBtnX + langBtnW && effMouseY >= langBtnY && effMouseY <= langBtnY + langBtnH;
        int langBtnBg = languageDropdownOpen ? 0xFF253346 : (langHov ? 0xFF2A3B52 : 0xFF1E293B);
        int langBtnBorder = languageDropdownOpen ? theme.getAccent() : (langHov ? 0xFF64748B : 0xFF334155);
        graphics.fill(langBtnX, langBtnY, langBtnX + langBtnW, langBtnY + langBtnH, langBtnBg);
        graphics.renderOutline(langBtnX, langBtnY, langBtnW, langBtnH, langBtnBorder);
        String currentLangName = settings.getLanguage().getDisplayName();
        String btnText = I18n.get("Language", lang) + ": " + currentLangName;
        graphics.drawString(font, btnText, langBtnX + 10, langBtnY + 7, 0xFFFFFFFF, false);
        String arrow = languageDropdownOpen ? "▲" : "▼";
        graphics.drawString(font, arrow, langBtnX + langBtnW - 16, langBtnY + 7, theme.getAccent(), false);

        String cookieText = (lang == ModelSettings.Language.RU) ? "На печеньку!" : "Buy a cookie!";
        int cookieBtnW = font.width(cookieText) + 24;
        int cookieBtnH = 20;
        int cookieBtnX = dx + 12;
        int cookieBtnY = langBtnY + langBtnH + 10;
        boolean cookieHov = effMouseX >= cookieBtnX && effMouseX <= cookieBtnX + cookieBtnW && effMouseY >= cookieBtnY && effMouseY <= cookieBtnY + cookieBtnH;
        int cookieBg = cookieHov ? 0xFF2A3B52 : 0xFF1E293B;
        int cookieBorder = cookieHov ? theme.getAccent() : 0xFF334155;
        graphics.fill(cookieBtnX, cookieBtnY, cookieBtnX + cookieBtnW, cookieBtnY + cookieBtnH, cookieBg);
        graphics.renderOutline(cookieBtnX, cookieBtnY, cookieBtnW, cookieBtnH, cookieBorder);
        int ctw = font.width(cookieText);
        graphics.drawString(font, cookieText, cookieBtnX + (cookieBtnW - ctw) / 2, cookieBtnY + 6, 0xFFFFFFFF, false);

        if (languageDropdownOpen) {
            ModelSettings.Language[] langs = ModelSettings.Language.values();
            int itemH = 20;
            int dropH = langs.length * itemH + 6;
            int dropY = langBtnY + langBtnH + 3;

            graphics.fill(langBtnX + 2, dropY + 2, langBtnX + langBtnW + 2, dropY + dropH + 2, 0x66000000);
            graphics.fill(langBtnX, dropY, langBtnX + langBtnW, dropY + dropH, 0xFF131B26);
            graphics.renderOutline(langBtnX, dropY, langBtnW, dropH, theme.getAccent());

            for (int i = 0; i < langs.length; i++) {
                ModelSettings.Language l = langs[i];
                int iy = dropY + 3 + i * itemH;
                boolean isSel = (l == settings.getLanguage());
                boolean iHov = effMouseX >= langBtnX + 2 && effMouseX <= langBtnX + langBtnW - 2 && effMouseY >= iy && effMouseY < iy + itemH;

                if (iHov || isSel) {
                    graphics.fill(langBtnX + 2, iy, langBtnX + langBtnW - 2, iy + itemH, isSel ? 0xFF1E3A5F : 0xFF233246);
                }

                int textColor = isSel ? theme.getAccent() : (iHov ? 0xFFFFFFFF : 0xFFCBD5E1);
                String prefix = isSel ? "● " : "  ";
                graphics.drawString(font, prefix + l.getDisplayName(), langBtnX + 8, iy + 6, textColor, false);
            }
        }

        if (backgroundDropdownOpen) {
            ModelSettings.BackgroundMode[] bgModesList = new ModelSettings.BackgroundMode[]{
                    ModelSettings.BackgroundMode.DEFAULT,
                    ModelSettings.BackgroundMode.CHROMA_GREEN,
                    ModelSettings.BackgroundMode.CHROMA_BLUE
            };
            int bgDropY = savedBgPy + 20 + 2;
            int bgItemH = 20;
            int bgDropH = bgModesList.length * bgItemH + 6;
            graphics.fill(bgBtnX + 2, bgDropY + 2, bgBtnX + bgBtnW + 2, bgDropY + bgDropH + 2, 0x66000000);
            graphics.fill(bgBtnX, bgDropY, bgBtnX + bgBtnW, bgDropY + bgDropH, 0xFF131B26);
            graphics.renderOutline(bgBtnX, bgDropY, bgBtnW, bgDropH, theme.getAccent());
            for (int i = 0; i < bgModesList.length; i++) {
                int itemY = bgDropY + 3 + i * bgItemH;
                boolean itemHov = effMouseX >= bgBtnX + 2 && effMouseX <= bgBtnX + bgBtnW - 2 && effMouseY >= itemY && effMouseY < itemY + bgItemH;
                ModelSettings.BackgroundMode m = bgModesList[i];
                boolean isSel = (m == settings.getBackgroundMode());
                if (itemHov || isSel) {
                    graphics.fill(bgBtnX + 2, itemY, bgBtnX + bgBtnW - 2, itemY + bgItemH, isSel ? 0xFF1E3A5F : 0xFF233246);
                }
                int textCol = isSel ? theme.getAccent() : (itemHov ? 0xFFFFFFFF : 0xFFCBD5E1);
                String prefix = isSel ? "● " : "  ";
                String mName = (lang == ModelSettings.Language.RU)
                        ? (m == ModelSettings.BackgroundMode.CHROMA_GREEN ? "Зелёный (Хромакей)" : (m == ModelSettings.BackgroundMode.CHROMA_BLUE ? "Синий (Хромакей)" : "По умолчанию"))
                        : (m == ModelSettings.BackgroundMode.CHROMA_GREEN ? "Chroma Green" : (m == ModelSettings.BackgroundMode.CHROMA_BLUE ? "Chroma Blue" : "Default"));
                graphics.drawString(font, prefix + mName, bgBtnX + 8, itemY + 6, textCol, false);
            }
        }

        graphics.pose().popPose();
    }

    private void drawCheckbox(GuiGraphics graphics, Font font, String text, boolean checked, int bx, int by, int mx, int my) {
        boolean hov = mx >= bx && mx <= bx + 110 && my >= by && my <= by + 14;
        graphics.fill(bx, by, bx + 12, by + 12, checked ? 0xFF2563EB : (hov ? 0xFF334155 : 0xFF1E293B));
        graphics.renderOutline(bx, by, 12, 12, checked ? 0xFF60A5FA : 0xFF475569);
        if (checked) {
            graphics.drawString(font, "✓", bx + 2, by + 2, 0xFFFFFFFF, false);
        }
        graphics.drawString(font, text, bx + 16, by + 2, 0xFFCBD5E1, false);
    }

    private void drawKeybindRow(GuiGraphics graphics, Font font, String label, String keyName, boolean listening, int bx, int by, int mx, int my) {
        graphics.drawString(font, label, bx, by + 3, 0xFF94A3B8, false);
        int btnW = 75;
        int btnX = bx + 95;
        boolean hov = mx >= btnX && mx <= btnX + btnW && my >= by && my <= by + 16;
        int col = listening ? 0xFFEF4444 : (hov ? 0xFF334155 : 0xFF1E293B);
        graphics.fill(btnX, by, btnX + btnW, by + 16, col);
        graphics.renderOutline(btnX, by, btnW, 16, listening ? 0xFFF87171 : 0xFF475569);
        String text = listening ? "> ? <" : keyName;
        int tw = font.width(text);
        graphics.drawString(font, text, btnX + (btnW - tw) / 2, by + 4, listening ? 0xFFFFFFFF : 0xFFE2E8F0, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open || closing) return closing;
        Minecraft mc = Minecraft.getInstance();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int dw = 440;
        int dh = 420;
        int dx = (screenW - dw) / 2;
        int dy = (screenH - dh) / 2;
        int cx = dx + dw / 2;
        int cy = dy + dh / 2;
        long elapsed = System.currentTimeMillis() - openStartTime;
        if (elapsed < 30) return true;
        float progress = Math.min(1.0f, elapsed / 170.0f);
        float ease = 1.0f - (float) Math.pow(1.0f - progress, 3);
        float scale = 0.88f + 0.12f * ease;
        double effMouseX = (progress < 1.0f && scale > 0.001f) ? (mouseX - cx) / scale + cx : mouseX;
        double effMouseY = (progress < 1.0f && scale > 0.001f) ? (mouseY - cy) / scale + cy : mouseY;

        int maxDropH = 0;
        if (languageDropdownOpen) maxDropH = Math.max(maxDropH, 60);
        if (effMouseX < dx || effMouseX > dx + dw || effMouseY < dy || effMouseY > dy + dh + maxDropH) {
            close();
            return true;
        }

        int closeSize = 16;
        int closeX = dx + dw - closeSize - 6;
        int closeY = dy + 4;
        if (effMouseX >= closeX && effMouseX <= closeX + closeSize && effMouseY >= closeY && effMouseY <= closeY + closeSize) {
            close();
            return true;
        }

        int py = dy + 32 + 14;
        EditorTheme[] themes = EditorTheme.values();
        int colW = 98;
        int colH = 20;
        for (int i = 0; i < themes.length; i++) {
            int bx = dx + 12 + (i % 4) * (colW + 6);
            int by = py + (i / 4) * (colH + 4);
            if (effMouseX >= bx && effMouseX <= bx + colW && effMouseY >= by && effMouseY <= by + colH) {
                settings.setTheme(themes[i]);
                if (listener != null) listener.onThemeChanged(themes[i]);
                return true;
            }
        }
        py += 52;
        py += 14;
        ModelSettings.CameraMode[] modes = ModelSettings.CameraMode.values();
        int modeW = 132;
        for (int i = 0; i < modes.length; i++) {
            int bx = dx + 12 + i * (modeW + 8);
            if (effMouseX >= bx && effMouseX <= bx + modeW && effMouseY >= py && effMouseY <= py + colH) {
                settings.setCameraMode(modes[i]);
                return true;
            }
        }
        py += 28;
        py += 14;
        if (effMouseX >= dx + 12 && effMouseX <= dx + 12 + 160 && effMouseY >= py && effMouseY <= py + 14) {
            settings.setShowGrid(!settings.isShowGrid());
            return true;
        }
        if (effMouseX >= dx + 190 && effMouseX <= dx + 190 + 190 && effMouseY >= py && effMouseY <= py + 14) {
            settings.setShowWorldBlocks(!settings.isShowWorldBlocks());
            return true;
        }
        py += 18;
        if (effMouseX >= dx + 12 && effMouseX <= dx + 12 + 160 && effMouseY >= py && effMouseY <= py + 14) {
            settings.setShowGizmo(!settings.isShowGizmo());
            return true;
        }
        if (effMouseX >= dx + 190 && effMouseX <= dx + 190 + 190 && effMouseY >= py && effMouseY <= py + 14) {
            settings.setUiAnimations(!settings.isUiAnimations());
            return true;
        }
        py += 20;
        int bgBtnX = dx + 130;
        int bgBtnW = dw - 142;
        int bgBtnH = 20;

        if (backgroundDropdownOpen) {
            ModelSettings.BackgroundMode[] bgModesList = new ModelSettings.BackgroundMode[]{
                    ModelSettings.BackgroundMode.DEFAULT,
                    ModelSettings.BackgroundMode.CHROMA_GREEN,
                    ModelSettings.BackgroundMode.CHROMA_BLUE
            };
            int bgDropY = py + bgBtnH + 2;
            int bgDropH = bgModesList.length * 22 + 4;
            if (effMouseX >= bgBtnX && effMouseX <= bgBtnX + bgBtnW && effMouseY >= bgDropY && effMouseY <= bgDropY + bgDropH) {
                int clickedIdx = (int) ((effMouseY - (bgDropY + 2)) / 22);
                if (clickedIdx >= 0 && clickedIdx < bgModesList.length) {
                    settings.setBackgroundMode(bgModesList[clickedIdx]);
                    backgroundDropdownOpen = false;
                    return true;
                }
            } else {
                backgroundDropdownOpen = false;
            }
        }

        if (effMouseX >= bgBtnX && effMouseX <= bgBtnX + bgBtnW && effMouseY >= py && effMouseY <= py + bgBtnH) {
            backgroundDropdownOpen = !backgroundDropdownOpen;
            if (backgroundDropdownOpen) languageDropdownOpen = false;
            return true;
        }
        py += 26;
        py += 14;
        if (checkKeybindClick(effMouseX, effMouseY, dx + 12 + 95, py, 0)) return true;
        if (checkKeybindClick(effMouseX, effMouseY, dx + 225 + 95, py, 1)) return true;
        py += 22;

        if (checkKeybindClick(effMouseX, effMouseY, dx + 12 + 95, py, 2)) return true;
        if (checkKeybindClick(effMouseX, effMouseY, dx + 225 + 95, py, 3)) return true;
        py += 22;

        if (checkKeybindClick(effMouseX, effMouseY, dx + 12 + 95, py, 4)) return true;
        py += 24;
        py += 14;
        int langBtnW = 190;
        int langBtnH = 22;
        int langBtnX = dx + 12;
        int langBtnY = py;
        ModelSettings.Language[] langs = ModelSettings.Language.values();

        ModelSettings.Language curLang = (settings != null) ? settings.getLanguage() : ModelSettings.Language.EN;
        String cookieText = (curLang == ModelSettings.Language.RU) ? "На печеньку!" : "Buy a cookie!";
        int cookieBtnW = Minecraft.getInstance().font.width(cookieText) + 24;
        int cookieBtnH = 20;
        int cookieBtnX = dx + 12;
        int cookieBtnY = langBtnY + langBtnH + 10;
        if (!languageDropdownOpen && effMouseX >= cookieBtnX && effMouseX <= cookieBtnX + cookieBtnW && effMouseY >= cookieBtnY && effMouseY <= cookieBtnY + cookieBtnH) {
            try {
                net.minecraft.Util.getPlatform().openUri(java.net.URI.create(donationUrl));
            } catch (Throwable t) {
                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().browse(java.net.URI.create(donationUrl));
                    }
                } catch (Throwable ignored) {}
            }
            return true;
        }

        if (languageDropdownOpen) {
            int dropY = langBtnY + langBtnH + 3;
            int itemH = 20;
            int dropH = langs.length * itemH + 6;
            if (effMouseX >= langBtnX && effMouseX <= langBtnX + langBtnW && effMouseY >= dropY && effMouseY <= dropY + dropH) {
                int clickedIdx = (int) ((effMouseY - (dropY + 3)) / itemH);
                if (clickedIdx >= 0 && clickedIdx < langs.length) {
                    settings.setLanguage(langs[clickedIdx]);
                    languageDropdownOpen = false;
                    return true;
                }
            }
        }

        if (effMouseX >= langBtnX && effMouseX <= langBtnX + langBtnW && effMouseY >= langBtnY && effMouseY <= langBtnY + langBtnH) {
            languageDropdownOpen = !languageDropdownOpen;
            return true;
        }

        if (languageDropdownOpen) {
            languageDropdownOpen = false;
        }

        return true;
    }

    private boolean checkKeybindClick(double mx, double my, int btnX, int by, int bindIdx) {
        if (mx >= btnX && mx <= btnX + 75 && my >= by && my <= by + 16) {
            this.listeningKeybind = (this.listeningKeybind == bindIdx) ? -1 : bindIdx;
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!open) return false;

        if (listeningKeybind != -1) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listeningKeybind = -1;
                return true;
            }
            switch (listeningKeybind) {
                case 0 -> settings.setKeyPlayPause(keyCode);
                case 1 -> settings.setKeyAddKeyframe(keyCode);
                case 2 -> settings.setKeyDelete(keyCode);
                case 3 -> settings.setKeyToggleGraph(keyCode);
                case 4 -> settings.setKeyCameraView(keyCode);
            }
            listeningKeybind = -1;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        return false;
    }

    private String getKeyName(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) return "Space";
        if (keyCode == GLFW.GLFW_KEY_DELETE) return "Delete";
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) return "Bksp";
        if (keyCode == GLFW.GLFW_KEY_TAB) return "Tab";
        if (keyCode == GLFW.GLFW_KEY_KP_0) return "Num 0";
        String n = GLFW.glfwGetKeyName(keyCode, 0);
        return (n != null && !n.isEmpty()) ? n.toUpperCase() : "Key " + keyCode;
    }
}

