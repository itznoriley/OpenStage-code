package net.openstage.client.ui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.openstage.client.theme.EditorTheme;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ItemPickerDialog {
    private boolean visible = false;
    private boolean closing = false;
    private long openStartTime = 0;
    private long closeStartTime = 0;
    private boolean uiAnimations = true;
    private final Consumer<ItemStack> onSelect;
    private final Runnable onClose;
    private String searchQuery = "";
    private int scrollOffset = 0;
    private final List<ItemStack> filteredItems = new ArrayList<>();
    private EditorTheme theme = EditorTheme.DARK_SLATE;

    public ItemPickerDialog(Consumer<ItemStack> onSelect, Runnable onClose) {
        this.onSelect = onSelect;
        this.onClose = onClose;
        refreshItems();
    }

    public void setTheme(EditorTheme theme) {
        if (theme != null) this.theme = theme;
    }

    public void setUiAnimations(boolean uiAnimations) {
        this.uiAnimations = uiAnimations;
    }

    public void open() {
        this.visible = true;
        this.closing = false;
        this.searchQuery = "";
        this.scrollOffset = 0;
        this.openStartTime = System.currentTimeMillis();
        refreshItems();
    }

    public void close() {
        if (!uiAnimations) {
            this.visible = false;
            this.closing = false;
            if (onClose != null) onClose.run();
            return;
        }
        if (visible && !closing) {
            this.closing = true;
            this.closeStartTime = System.currentTimeMillis();
        }
    }

    public boolean isVisible() {
        return visible || closing;
    }

    private void refreshItems() {
        filteredItems.clear();
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            String name = item.getName(item.getDefaultInstance()).getString().toLowerCase(Locale.ROOT);
            String id = BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase(Locale.ROOT);
            if (q.isEmpty() || name.contains(q) || id.contains(q)) {
                filteredItems.add(new ItemStack(item));
            }
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!visible && !closing) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
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
        int cols = 12;
        int slotSize = 18;
        int slotStep = 20;
        int dialogW = Math.min(cols * slotStep + 20, screenW - 20);
        int dialogH = Math.min(224, screenH - 20);
        int dx = (screenW - dialogW) / 2;
        int dy = (screenH - dialogH) / 2;
        int cx = dx + dialogW / 2;
        int cy = dy + dialogH / 2;
        int effMouseX = (progress < 1.0f && scale > 0.001f) ? (int) ((mouseX - cx) / scale + cx) : mouseX;
        int effMouseY = (progress < 1.0f && scale > 0.001f) ? (int) ((mouseY - cy) / scale + cy) : mouseY;

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().translate(-cx, -cy, 0);

        graphics.fill(dx, dy, dx + dialogW, dy + dialogH, theme.getPanelBg());
        graphics.fill(dx + 1, dy + 1, dx + dialogW - 1, dy + 22, theme.getTitleBarBg());
        graphics.renderOutline(dx, dy, dialogW, dialogH, theme.getAccent());
        graphics.drawString(font, "Выбор предмета", dx + 8, dy + 6, theme.getAccent(), false);
        int closeW = 16;
        int closeX = dx + dialogW - closeW - 4;
        boolean closeHov = effMouseX >= closeX && effMouseX <= closeX + closeW && effMouseY >= dy + 3 && effMouseY <= dy + 19;
        graphics.drawString(font, "✕", closeX + 4, dy + 6, closeHov ? 0xFFEF4444 : theme.getTextMuted(), false);
        int searchY = dy + 26;
        int searchW = dialogW - 16;
        graphics.fill(dx + 8, searchY, dx + 8 + searchW, searchY + 16, theme.getButtonBg());
        graphics.renderOutline(dx + 8, searchY, searchW, 16, theme.getDivider());
        String displaySearch = searchQuery.isEmpty() ? "§8Поиск предмета..." : searchQuery + "_";
        graphics.drawString(font, displaySearch, dx + 12, searchY + 4, theme.getTextColor(), false);
        int btnY = searchY + 20;
        int btnW = (dialogW - 20) / 2;
        boolean fromHandHov = effMouseX >= dx + 8 && effMouseX <= dx + 8 + btnW && effMouseY >= btnY && effMouseY <= btnY + 16;
        graphics.fill(dx + 8, btnY, dx + 8 + btnW, btnY + 16, fromHandHov ? theme.getButtonHover() : theme.getButtonBg());
        graphics.renderOutline(dx + 8, btnY, btnW, 16, theme.getDivider());
        graphics.drawString(font, "Из руки игрока", dx + 12, btnY + 4, fromHandHov ? 0xFFFFFFFF : theme.getTextColor(), false);
        int clearX = dx + 12 + btnW;
        boolean clearHov = effMouseX >= clearX && effMouseX <= clearX + btnW && effMouseY >= btnY && effMouseY <= btnY + 16;
        graphics.fill(clearX, btnY, clearX + btnW, btnY + 16, clearHov ? 0xFFEF4444 : theme.getButtonBg());
        graphics.renderOutline(clearX, btnY, btnW, 16, theme.getDivider());
        graphics.drawString(font, "Очистить руку", clearX + 8, btnY + 4, clearHov ? 0xFFFFFFFF : theme.getTextMuted(), false);
        int gridBoxX = dx + 8;
        int gridBoxY = btnY + 20;
        int gridBoxW = dialogW - 16;
        int gridBoxH = dialogH - (gridBoxY - dy) - 8;
        graphics.fill(gridBoxX, gridBoxY, gridBoxX + gridBoxW, gridBoxY + gridBoxH, 0xFF0B1017);
        graphics.renderOutline(gridBoxX, gridBoxY, gridBoxW, gridBoxH, theme.getDivider());

        int gridX = gridBoxX + (gridBoxW - cols * slotStep) / 2;
        int gridY = gridBoxY + 4;
        int rows = 7;
        int visibleSlots = cols * rows;
        ItemStack hoveredItem = null;

        for (int i = 0; i < visibleSlots; i++) {
            int itemIdx = scrollOffset * cols + i;
            if (itemIdx >= filteredItems.size()) break;
            int col = i % cols;
            int row = i / cols;
            int sx = gridX + col * slotStep;
            int sy = gridY + row * slotStep;
            boolean hov = effMouseX >= sx && effMouseX <= sx + slotSize && effMouseY >= sy && effMouseY <= sy + slotSize;
            graphics.fill(sx, sy, sx + slotSize, sy + slotSize, hov ? theme.getButtonHover() : 0xFF141E2C);
            graphics.renderOutline(sx, sy, slotSize, slotSize, hov ? theme.getAccent() : 0xFF27384E);
            ItemStack stack = filteredItems.get(itemIdx);
            graphics.renderItem(stack, sx + 1, sy + 1);
            if (hov) {
                hoveredItem = stack;
            }
        }

        int totalRows = (int) Math.ceil((double) filteredItems.size() / cols);
        if (totalRows > rows) {
            int sbW = 3;
            int sbX = gridBoxX + gridBoxW - sbW - 2;
            int sbH = Math.max(16, (int) ((float) rows / totalRows * (gridBoxH - 4)));
            int maxOffset = totalRows - rows;
            int sbY = gridBoxY + 2 + (int) ((float) scrollOffset / maxOffset * (gridBoxH - 4 - sbH));
            graphics.fill(sbX, sbY, sbX + sbW, sbY + sbH, theme.getAccent());
        }

        graphics.fill(dx, dy, dx + dialogW, dy + 1, theme.getAccent());
        graphics.fill(dx, dy + dialogH - 1, dx + dialogW, dy + dialogH, theme.getAccent());
        graphics.fill(dx, dy, dx + 1, dy + dialogH, theme.getAccent());
        graphics.fill(dx + dialogW - 1, dy, dx + dialogW, dy + dialogH, theme.getAccent());
        graphics.fill(dx, dy + 22, dx + dialogW, dy + 23, theme.getDivider());

        graphics.pose().popPose();

        if (hoveredItem != null) {
            graphics.renderTooltip(font, hoveredItem, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (closing) return true;
        if (!visible) return false;
        Minecraft mc = Minecraft.getInstance();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int cols = 12;
        int slotSize = 18;
        int slotStep = 20;
        int dialogW = Math.min(cols * slotStep + 20, screenW - 20);
        int dialogH = Math.min(224, screenH - 20);
        int dx = (screenW - dialogW) / 2;
        int dy = (screenH - dialogH) / 2;
        int cx = dx + dialogW / 2;
        int cy = dy + dialogH / 2;
        long elapsed = System.currentTimeMillis() - openStartTime;
        if (elapsed < 30) return true;
        float progress = Math.min(1.0f, elapsed / 170.0f);
        float ease = 1.0f - (float) Math.pow(1.0f - progress, 3);
        float scale = 0.88f + 0.12f * ease;
        double effMx = (progress < 1.0f && scale > 0.001f) ? (mx - cx) / scale + cx : mx;
        double effMy = (progress < 1.0f && scale > 0.001f) ? (my - cy) / scale + cy : my;

        if (effMx < dx || effMx > dx + dialogW || effMy < dy || effMy > dy + dialogH) {
            close();
            return true;
        }

        int closeW = 16;
        int closeX = dx + dialogW - closeW - 4;
        if (effMx >= closeX && effMx <= closeX + closeW && effMy >= dy + 3 && effMy <= dy + 19) {
            close();
            return true;
        }

        int btnY = dy + 46;
        int btnW = (dialogW - 20) / 2;
        if (effMx >= dx + 8 && effMx <= dx + 8 + btnW && effMy >= btnY && effMy <= btnY + 16) {
            if (mc.player != null && !mc.player.getMainHandItem().isEmpty()) {
                if (onSelect != null) onSelect.accept(mc.player.getMainHandItem().copy());
            }
            close();
            return true;
        }

        int clearX = dx + 12 + btnW;
        if (effMx >= clearX && effMx <= clearX + btnW && effMy >= btnY && effMy <= btnY + 16) {
            if (onSelect != null) onSelect.accept(ItemStack.EMPTY);
            close();
            return true;
        }

        int gridX = dx + (dialogW - cols * slotStep) / 2;
        int gridY = btnY + 20;
        int rows = 7;
        int visibleSlots = cols * rows;

        for (int i = 0; i < visibleSlots; i++) {
            int itemIdx = scrollOffset * cols + i;
            if (itemIdx >= filteredItems.size()) break;
            int col = i % cols;
            int row = i / cols;
            int sx = gridX + col * (slotSize + 2);
            int sy = gridY + row * (slotSize + 2);

            if (effMx >= sx && effMx <= sx + slotSize && effMy >= sy && effMy <= sy + slotSize) {
                ItemStack stack = filteredItems.get(itemIdx);
                if (onSelect != null) onSelect.accept(stack.copy());
                close();
                return true;
            }
        }

        return true;
    }

    public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
        if (closing) return true;
        if (!visible) return false;
        int cols = 12;
        int totalRows = (int) Math.ceil((double) filteredItems.size() / cols);
        int maxOffset = Math.max(0, totalRows - 7);
        if (verticalAmount > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (verticalAmount < 0) {
            scrollOffset = Math.min(maxOffset, scrollOffset + 1);
        }
        return true;
    }

    public boolean charTyped(char c, int modifiers) {
        if (closing) return true;
        if (!visible) return false;
        if (c >= 32 && c != 127) {
            searchQuery += c;
            scrollOffset = 0;
            refreshItems();
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
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                scrollOffset = 0;
                refreshItems();
            }
            return true;
        }
        return false;
    }
}

