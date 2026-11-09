package net.openstage.client.ui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.openstage.client.model.ModelSettings;
import net.openstage.client.renderer.ActorEntityRenderer;
import net.openstage.client.skin.SkinManager;
import net.openstage.client.theme.EditorTheme;
import net.openstage.client.ui.I18n;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class SkinPickerModal {
    public static class SkinItem {
        public final String name;
        public final boolean isSlim;
        public float hoverYaw = 180.0f;

        public SkinItem(String name, boolean isSlim) {
            this.name = name;
            this.isSlim = isSlim;
        }
    }

    private boolean visible = false;
    private boolean closing = false;
    private long openStartTime = 0;
    private long closeStartTime = 0;
    private int width = 480;
    private int height = 340;
    private final List<SkinItem> allItems = new ArrayList<>();
    private final List<SkinItem> filteredItems = new ArrayList<>();
    private String selectedSkin = "steve";
    private String searchQuery = "";
    private boolean searchFocused = false;
    private int scrollY = 0;
    private int maxScrollY = 0;
    private Consumer<String> listener;

    public SkinPickerModal() {}

    public void setListener(Consumer<String> listener) {
        this.listener = listener;
    }

    public void open(String currentSkin) {
        SkinManager.getInstance().refreshSkins();
        this.selectedSkin = (currentSkin != null) ? currentSkin : "steve";
        this.visible = true;
        this.closing = false;
        this.searchQuery = "";
        this.searchFocused = false;
        this.scrollY = 0;
        this.openStartTime = System.currentTimeMillis();
        refreshSkinList();
    }

    public void close() {
        if (visible && !closing) {
            this.closing = true;
            this.closeStartTime = System.currentTimeMillis();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    private void refreshSkinList() {
        allItems.clear();
        List<String> names = SkinManager.getInstance().getAvailableSkinNames();
        for (String name : names) {
            boolean isSlim = SkinManager.getInstance().isSkinSlim(name);
            allItems.add(new SkinItem(name, isSlim));
        }
        applyFilter();
    }

    private void applyFilter() {
        filteredItems.clear();
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        for (SkinItem item : allItems) {
            if (q.isEmpty() || item.name.toLowerCase(Locale.ROOT).contains(q)) {
                filteredItems.add(item);
            }
        }
    }

    public void render(GuiGraphics graphics, Font font, int screenW, int screenH, int mouseX, int mouseY, EditorTheme theme) {
        if (!visible) return;

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

        int bgAlpha = (int) (0xCC * ease);
        graphics.fill(0, 0, screenW, screenH, (bgAlpha << 24));

        float scale = 0.88f + 0.12f * ease;
        int modalW = Math.min(width, screenW - 20);
        int modalH = Math.min(height, screenH - 20);
        int modalX = (screenW - modalW) / 2;
        int modalY = (screenH - modalH) / 2;
        int cx = modalX + modalW / 2;
        int cy = modalY + modalH / 2;

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().translate(-cx, -cy, 0);

        int panelBg = (theme != null) ? theme.getPanelBg() : 0xFF0E131B;
        int titleBarBg = (theme != null) ? theme.getTitleBarBg() : 0xFF182232;
        int accent = (theme != null) ? theme.getAccent() : 0xFF3B82F6;
        int divider = (theme != null) ? theme.getDivider() : 0xFF1E293B;
        int textMuted = (theme != null) ? theme.getTextMuted() : 0xFF64748B;

        graphics.fill(modalX, modalY, modalX + modalW, modalY + modalH, panelBg);
        graphics.renderOutline(modalX, modalY, modalW, modalH, accent);

        graphics.fill(modalX, modalY, modalX + modalW, modalY + 28, titleBarBg);
        graphics.drawString(font, "Выбор скина", modalX + 12, modalY + 9, accent, false);

        int folderBtnW = 96;
        int folderBtnX = modalX + modalW - 28 - folderBtnW;
        int folderBtnY = modalY + 5;
        boolean folderHov = mouseX >= folderBtnX && mouseX <= folderBtnX + folderBtnW && mouseY >= folderBtnY && mouseY <= folderBtnY + 18;
        graphics.fill(folderBtnX, folderBtnY, folderBtnX + folderBtnW, folderBtnY + 18, folderHov ? 0xFF2563EB : 0xFF1E293B);
        graphics.renderOutline(folderBtnX, folderBtnY, folderBtnW, 18, folderHov ? 0xFF60A5FA : divider);
        graphics.drawString(font, "📁 Папка скинов", folderBtnX + 6, folderBtnY + 5, folderHov ? 0xFFFFFFFF : 0xFFCBD5E1, false);

        int closeX = modalX + modalW - 22;
        boolean closeHov = mouseX >= closeX && mouseX <= closeX + 16 && mouseY >= modalY + 6 && mouseY <= modalY + 22;
        graphics.drawString(font, "✕", closeX, modalY + 8, closeHov ? 0xFFEF4444 : textMuted, false);

        int listX = modalX + 10;
        int listY = modalY + 34;
        int listW = modalW - 20;
        int listH = modalH - 72;

        graphics.fill(listX, listY, listX + listW, listY + listH, 0xFF090D14);
        graphics.renderOutline(listX, listY, listW, listH, divider);

        graphics.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);

        int cardW = 96;
        int cardH = 120;
        int cardsPerRow = Math.max(1, (listW - 12) / (cardW + 8));
        int totalRows = (int) Math.ceil((double) filteredItems.size() / cardsPerRow);
        int totalH = totalRows * (cardH + 8) + 8;
        maxScrollY = Math.max(0, totalH - listH);
        if (scrollY > maxScrollY) scrollY = maxScrollY;

        for (int i = 0; i < filteredItems.size(); i++) {
            SkinItem item = filteredItems.get(i);
            int col = i % cardsPerRow;
            int row = i / cardsPerRow;
            int cardX = listX + 8 + col * (cardW + 8);
            int cardY = listY + 8 + row * (cardH + 8) - scrollY;

            if (cardY + cardH < listY || cardY > listY + listH) continue;

            renderSkinCard(graphics, font, item, cardX, cardY, cardW, cardH, mouseX, mouseY, theme);
        }

        graphics.disableScissor();

        if (maxScrollY > 0) {
            int sbW = 4;
            int sbX = listX + listW - sbW - 2;
            int sbH = Math.max(16, (int) ((float) listH / (totalH) * listH));
            int sbY = listY + (int) ((float) scrollY / maxScrollY * (listH - sbH));
            graphics.fill(sbX, listY, sbX + sbW, listY + listH, 0x33000000);
            graphics.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x8864748B);
        }

        int bottomY = modalY + modalH - 28;
        int searchX = modalX + 10;
        int searchW = modalW - 20;
        int searchH = 20;
        boolean searchHov = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= bottomY && mouseY <= bottomY + searchH;
        graphics.fill(searchX, bottomY, searchX + searchW, bottomY + searchH, 0xFF0E141D);
        graphics.renderOutline(searchX, bottomY, searchW, searchH, searchFocused ? accent : (searchHov ? 0xFF475569 : divider));

        graphics.drawString(font, "🔍", searchX + 5, bottomY + 5, accent, false);
        String displaySearch = searchQuery;
        if (displaySearch.isEmpty() && !searchFocused) {
            graphics.drawString(font, "Поиск скинов...", searchX + 22, bottomY + 5, 0xFF64748B, false);
        } else {
            if (searchFocused && (now / 500) % 2 == 0) displaySearch += "|";
            graphics.drawString(font, displaySearch, searchX + 22, bottomY + 5, 0xFFFFFFFF, false);
        }

        if (!searchQuery.isEmpty()) {
            int clearX = searchX + searchW - 16;
            graphics.drawString(font, "✕", clearX, bottomY + 5, 0xFF94A3B8, false);
        }

        graphics.pose().popPose();
    }

    private void renderSkinCard(GuiGraphics graphics, Font font, SkinItem item, int cx, int cy, int cw, int ch, int mouseX, int mouseY, EditorTheme theme) {
        boolean isSel = item.name.equalsIgnoreCase(selectedSkin);
        boolean isHov = mouseX >= cx && mouseX <= cx + cw && mouseY >= cy && mouseY <= cy + ch;

        int cardBg = isSel ? 0xFF172554 : (isHov ? 0xFF1E293B : 0xFF111827);
        int borderColor = isSel ? 0xFF3B82F6 : (isHov ? 0xFF475569 : 0xFF233042);

        graphics.fill(cx, cy, cx + cw, cy + ch, cardBg);
        graphics.renderOutline(cx, cy, cw, ch, borderColor);
        if (isSel) {
            graphics.renderOutline(cx - 1, cy - 1, cw + 2, ch + 2, 0xFF60A5FA);
        }

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(cx + cw / 2.0f, cy + 86.0f, 150.0f);
        float s = 34.0f;
        poseStack.scale(s, -s, s);

        if (isHov) {
            item.hoverYaw = (item.hoverYaw + 1.0f) % 360.0f;
        } else {
            item.hoverYaw = net.minecraft.util.Mth.rotLerp(0.08f, item.hoverYaw, 180.0f);
        }
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(item.hoverYaw));

        ResourceLocation skinTex = SkinManager.getInstance().getSkinTexture(item.name, item.isSlim);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        ModelSettings previewSettings = new ModelSettings();

        ActorEntityRenderer.renderAnimatedPlayerModel(
                poseStack, graphics.bufferSource(), 15728880,
                null, null, item.isSlim, skinTex, previewSettings, null
        );
        graphics.bufferSource().endBatch();
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        poseStack.popPose();

        String displayName = item.name;
        int maxTextW = cw - 8;
        if (font.width(displayName) > maxTextW) {
            while (displayName.length() > 3 && font.width(displayName + "..") > maxTextW) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            displayName += "..";
        }
        int tw = font.width(displayName);
        graphics.drawString(font, displayName, cx + (cw - tw) / 2, cy + ch - 14, isSel ? 0xFF93C5FD : (isHov ? 0xFFFFFFFF : 0xFF94A3B8), false);
    }

    public boolean mouseClicked(double mx, double my, int button, int screenW, int screenH) {
        if (!visible || closing) return false;

        int modalW = Math.min(width, screenW - 20);
        int modalH = Math.min(height, screenH - 20);
        int modalX = (screenW - modalW) / 2;
        int modalY = (screenH - modalH) / 2;

        if (mx < modalX || mx > modalX + modalW || my < modalY || my > modalY + modalH) {
            close();
            return true;
        }

        int closeX = modalX + modalW - 22;
        if (mx >= closeX && mx <= closeX + 16 && my >= modalY + 6 && my <= modalY + 22) {
            close();
            return true;
        }

        int folderBtnW = 96;
        int folderBtnX = modalX + modalW - 28 - folderBtnW;
        int folderBtnY = modalY + 5;
        if (mx >= folderBtnX && mx <= folderBtnX + folderBtnW && my >= folderBtnY && my <= folderBtnY + 18) {
            SkinManager.getInstance().openSkinsFolder();
            return true;
        }

        int bottomY = modalY + modalH - 28;
        int searchX = modalX + 10;
        int searchW = modalW - 20;
        int searchH = 20;
        if (mx >= searchX && mx <= searchX + searchW && my >= bottomY && my <= bottomY + searchH) {
            if (!searchQuery.isEmpty() && mx >= searchX + searchW - 18) {
                searchQuery = "";
                applyFilter();
            } else {
                searchFocused = true;
            }
            return true;
        } else {
            searchFocused = false;
        }

        int listX = modalX + 10;
        int listY = modalY + 34;
        int listW = modalW - 20;
        int listH = modalH - 72;

        if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
            int cardW = 96;
            int cardH = 120;
            int cardsPerRow = Math.max(1, (listW - 12) / (cardW + 8));

            for (int i = 0; i < filteredItems.size(); i++) {
                SkinItem item = filteredItems.get(i);
                int col = i % cardsPerRow;
                int row = i / cardsPerRow;
                int cardX = listX + 8 + col * (cardW + 8);
                int cardY = listY + 8 + row * (cardH + 8) - scrollY;

                if (cardY + cardH < listY || cardY > listY + listH) continue;

                if (mx >= cardX && mx <= cardX + cardW && my >= cardY && my <= cardY + cardH) {
                    this.selectedSkin = item.name;
                    if (listener != null) {
                        listener.accept(item.name);
                    }
                    close();
                    return true;
                }
            }
        }

        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible) return false;
        scrollY -= (int) (amount * 24);
        if (scrollY < 0) scrollY = 0;
        if (scrollY > maxScrollY) scrollY = maxScrollY;
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                applyFilter();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!visible || !searchFocused) return false;
        if (chr >= 32 && chr != 127) {
            searchQuery += chr;
            applyFilter();
            return true;
        }
        return false;
    }
}

