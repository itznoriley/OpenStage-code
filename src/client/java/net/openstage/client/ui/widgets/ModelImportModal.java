package net.openstage.client.ui.widgets;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.openstage.client.model.ModelSettings;
import net.openstage.client.skin.SkinManager;
import net.openstage.client.theme.EditorTheme;
import net.openstage.client.ui.I18n;
import net.openstage.client.ui.OpenStageScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.openstage.client.renderer.ActorEntityRenderer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class ModelImportModal {
    public interface ModelImportListener {
        void onModelImported(File modelFile, String modelName, String format, String modelId, boolean isSlim, boolean isBendable);
    }

    public static class ModelItem {
        public final String id;
        public final String name;
        public final String format;
        public final File file;
        public final String skin;
        public final boolean isSlim;
        public final boolean isBendable;
        public final String categoryId;
        public float hoverYaw = 0.0f;

        public ModelItem(String id, String name, String format, File file, String skin, boolean isSlim, boolean isBendable, String categoryId) {
            this.id = id;
            this.name = name;
            this.format = format;
            this.file = file;
            this.skin = skin;
            this.isSlim = isSlim;
            this.isBendable = isBendable;
            this.categoryId = categoryId;
            this.hoverYaw = format.equals("player") ? 180.0f : 45.0f;
        }

        public ModelItem(String id, String name, String format, File file, String skin, boolean isSlim, String categoryId) {
            this(id, name, format, file, skin, isSlim, false, categoryId);
        }
    }

    public static class ModelCategory {
        public final String id;
        public final String titleKey;
        public boolean expanded = true;
        public final List<ModelItem> items = new ArrayList<>();

        public ModelCategory(String id, String titleKey) {
            this.id = id;
            this.titleKey = titleKey;
        }
    }

    private boolean visible = false;
    private boolean closing = false;
    private long openStartTime = 0;
    private long closeStartTime = 0;
    private int width = 540;
    private int height = 370;
    private final List<ModelCategory> categories = new ArrayList<>();
    private static final List<String> SESSION_RECENTS = new ArrayList<>();
    private ModelItem selectedItem = null;
    private String searchQuery = "";
    private boolean searchFocused = false;
    private int scrollY = 0;
    private int maxScrollY = 0;
    private long lastClickTime = 0;
    private ModelItem lastClickedItem = null;
    private ModelImportListener listener;

    public ModelImportModal() {
        initCategories();
    }

    private void initCategories() {
        categories.clear();
        categories.add(new ModelCategory("recent", "Recent"));
        categories.add(new ModelCategory("player", "Player Models"));
        categories.add(new ModelCategory("custom", "Custom Models"));
    }

    public void setListener(ModelImportListener listener) {
        this.listener = listener;
    }

    public boolean isVisible() {
        return visible || closing;
    }

    public void open() {
        this.visible = true;
        this.closing = false;
        this.searchQuery = "";
        this.searchFocused = false;
        this.scrollY = 0;
        this.openStartTime = System.currentTimeMillis();

        loadRecentModels();

        scanModelsFolder();
    }

    public void close() {
        if (visible && !closing) {
            this.closing = true;
            this.closeStartTime = System.currentTimeMillis();
        }
    }

    private void loadRecentModels() {
        File rf = new File("openstage/models/.recent_models.txt");

        if (rf.exists()) {
            try { rf.delete(); } catch (Exception ignored) {}
        }
    }

    private void saveRecentModel(String id) {
        if (id == null || id.isEmpty()) return;

        SESSION_RECENTS.remove(id);

        SESSION_RECENTS.add(0, id);

        while (SESSION_RECENTS.size() > 12) {
            SESSION_RECENTS.remove(SESSION_RECENTS.size() - 1);
        }
    }

    public void scanModelsFolder() {
        File dir = new File("openstage/models");

        if (!dir.exists()) {
            dir.mkdirs();
        }

        ModelCategory recentCat = categories.get(0);
        ModelCategory playerCat = categories.get(1);
        ModelCategory customCat = categories.get(2);

        customCat.items.clear();
        playerCat.items.clear();
        recentCat.items.clear();

        playerCat.items.add(new ModelItem("player:steve", "Steve (Standard)", "player", null, "steve", false, false, "player"));
        playerCat.items.add(new ModelItem("player:alex", "Alex (Standard)", "player", null, "alex", true, false, "player"));
        playerCat.items.add(new ModelItem("player:steve_bends", "Steve (Bends)", "player", null, "steve", false, true, "player"));
        playerCat.items.add(new ModelItem("player:alex_bends", "Alex (Bends)", "player", null, "alex", true, true, "player"));

        File[] files = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase();

            return lower.endsWith(".geo.json") || lower.endsWith(".bbmodel") || lower.endsWith(".obj");
        });

        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    String fn = f.getName().toLowerCase();
                    String fmt = fn.endsWith(".bbmodel") ? "bbmodel" : (fn.endsWith(".obj") ? "obj" : "geo.json");
                    String cleanName = f.getName();

                    if (cleanName.endsWith(".geo.json")) cleanName = cleanName.substring(0, cleanName.length() - 9);

                    else if (cleanName.lastIndexOf('.') > 0) cleanName = cleanName.substring(0, cleanName.lastIndexOf('.'));

                    customCat.items.add(new ModelItem("custom:" + f.getName(), cleanName, fmt, f, null, false, false, "custom"));
                }
            }
        }

        for (String rid : SESSION_RECENTS) {
            if (rid.startsWith("player:")) continue;
            ModelItem match = findItemById(rid);

            if (match != null) {
                recentCat.items.add(new ModelItem(match.id, match.name, match.format, match.file, match.skin, match.isSlim, match.isBendable, "recent"));
            } else if (rid.startsWith("skin:")) {
                String skinName = rid.substring(5);
                boolean isSlim = SkinManager.getInstance().isSkinSlim(skinName);

                recentCat.items.add(new ModelItem(rid, skinName, "player", null, skinName, isSlim, false, "recent"));
            } else if (net.openstage.client.model.RecentStateStore.hasSnapshot(rid)) {
                net.openstage.client.model.RecentStateStore.StateSnapshot snap = net.openstage.client.model.RecentStateStore.getSnapshot(rid);
                if (!snap.modelId.startsWith("player:")) {
                    recentCat.items.add(new ModelItem(snap.modelId, snap.name, snap.format, snap.file, snap.skin, snap.isSlim, snap.isBendable, "recent"));
                }
            }
        }

        if (selectedItem == null) {
            if (!customCat.items.isEmpty()) selectedItem = customCat.items.get(0);

            else if (!recentCat.items.isEmpty()) selectedItem = recentCat.items.get(0);

            else if (!playerCat.items.isEmpty()) selectedItem = playerCat.items.get(0);
        }
    }

    private ModelItem findItemById(String id) {
        for (ModelCategory cat : categories) {
            if (cat.id.equals("recent")) continue;

            for (ModelItem item : cat.items) {
                if (item.id.equals(id)) return item;
            }
        }

        return null;
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
        this.width = Math.min(540, screenW - 24);
        this.height = Math.min(370, screenH - 24);
        int modalX = (screenW - width) / 2;
        int modalY = (screenH - height) / 2;
        int cx = modalX + width / 2;
        int cy = modalY + height / 2;

        graphics.pose().pushPose();

        graphics.pose().translate(cx, cy, 0);

        graphics.pose().scale(scale, scale, 1.0f);

        graphics.pose().translate(-cx, -cy, 0);
        int panelBg = (theme != null) ? theme.getPanelBg() : 0xFF141923;
        int titleBg = (theme != null) ? theme.getTitleBarBg() : 0xFF1E2838;
        int divider = (theme != null) ? theme.getDivider() : 0xFF2A364F;
        int accent = (theme != null) ? theme.getAccent() : 0xFF38BDF8;
        int textCol = (theme != null) ? theme.getTextColor() : 0xFFFFFFFF;
        int textMuted = (theme != null) ? theme.getTextMuted() : 0xFF94A3B8;

        graphics.fill(modalX + 3, modalY + 3, modalX + width + 3, modalY + height + 3, 0x66000000);

        graphics.fill(modalX, modalY, modalX + width, modalY + height, panelBg);

        graphics.renderOutline(modalX, modalY, width, height, divider);

        graphics.fill(modalX, modalY, modalX + width, modalY + 24, titleBg);

        graphics.fill(modalX, modalY + 24, modalX + width, modalY + 25, divider);
        ModelSettings.Language lang = ModelSettings.Language.EN;

        if (Minecraft.getInstance().screen instanceof OpenStageScreen oss) {
            ModelSettings ms = oss.getModelSettings();

            if (ms != null && ms.getLanguage() != null) {
                lang = ms.getLanguage();
            }
        }

        String titleText = "📦 " + I18n.get("Model Palette", lang);

        graphics.drawString(font, titleText, modalX + 10, modalY + 8, accent, false);
        int closeSize = 14;
        int closeX = modalX + width - closeSize - 7;
        int closeY = modalY + 5;
        boolean closeHov = mouseX >= closeX && mouseX <= closeX + closeSize && mouseY >= closeY && mouseY <= closeY + closeSize;

        graphics.fill(closeX, closeY, closeX + closeSize, closeY + closeSize, closeHov ? 0xFFEF4444 : 0x00000000);
        int closeTw = font.width("✕");

        graphics.drawString(font, "✕", closeX + (closeSize - closeTw) / 2, closeY + 3, closeHov ? 0xFFFFFFFF : textMuted, false);
        int topBarY = modalY + 28;
        int topBarH = 22;
        int bx = modalX + 10;
        int addBtnW = 160;
        boolean addHov = mouseX >= bx && mouseX <= bx + addBtnW && mouseY >= topBarY && mouseY <= topBarY + topBarH;

        graphics.fill(bx, topBarY, bx + addBtnW, topBarY + topBarH, addHov ? 0xFF0284C7 : 0xFF0369A1);

        graphics.renderOutline(bx, topBarY, addBtnW, topBarH, accent);

        graphics.drawCenteredString(font, "✚ " + I18n.get("Add Model...", lang), bx + addBtnW / 2, topBarY + 6, 0xFFFFFFFF);
        bx += addBtnW + 8;
        int folderBtnW = 140;
        boolean folderHov = mouseX >= bx && mouseX <= bx + folderBtnW && mouseY >= topBarY && mouseY <= topBarY + topBarH;

        graphics.fill(bx, topBarY, bx + folderBtnW, topBarY + topBarH, folderHov ? 0xFF243245 : 0xFF1A2433);

        graphics.renderOutline(bx, topBarY, folderBtnW, topBarH, divider);

        graphics.drawCenteredString(font, "📁 " + I18n.get("Models Folder", lang), bx + folderBtnW / 2, topBarY + 6, textCol);
        int listX = modalX + 10;
        int listY = modalY + 54;
        int listW = width - 20;
        int listH = height - 54 - 34;

        graphics.fill(listX, listY, listX + listW, listY + listH, 0xFF0D121B);

        graphics.renderOutline(listX, listY, listW, listH, divider);

        graphics.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);
        int contentY = listY + 6 - scrollY;
        int cardW = 92;
        int cardH = 88;
        int cardsPerRow = Math.max(1, (listW - 16) / (cardW + 8));
        int totalContentHeight = 0;

        for (ModelCategory cat : categories) {
            List<ModelItem> filteredItems = getFilteredItems(cat);

            if (filteredItems.isEmpty() && !searchQuery.isEmpty()) {
                continue;
            }

            int catHeaderY = contentY;

            if (catHeaderY + 18 >= listY && catHeaderY <= listY + listH) {
                boolean catHov = mouseX >= listX + 6 && mouseX <= listX + listW - 6 && mouseY >= catHeaderY && mouseY <= catHeaderY + 18;

                if (catHov) {
                    graphics.fill(listX + 6, catHeaderY, listX + listW - 6, catHeaderY + 18, 0x1FFFFFFF);
                }

                String arrow = cat.expanded ? "▼ " : "▶ ";
                String title = arrow + I18n.get(cat.titleKey, lang) + " (" + filteredItems.size() + ")";

                graphics.drawString(font, title, listX + 8, catHeaderY + 5, accent, false);
                int tw = font.width(title);

                graphics.fill(listX + 12 + tw, catHeaderY + 9, listX + listW - 8, catHeaderY + 10, divider);
            }

            contentY += 22;
            totalContentHeight += 22;

            if (cat.expanded) {
                if (filteredItems.isEmpty()) {
                    if (contentY + 16 >= listY && contentY <= listY + listH) {
                        graphics.drawString(font, "  " + (cat.id.equals("custom") ? I18n.get("No custom models found in openstage/models", lang) : I18n.get("None", lang)), listX + 16, contentY + 4, textMuted, false);
                    }

                    contentY += 20;
                    totalContentHeight += 20;
                } else {
                    int rows = (int) Math.ceil((double) filteredItems.size() / cardsPerRow);

                    for (int i = 0; i < filteredItems.size(); i++) {
                        ModelItem item = filteredItems.get(i);
                        int col = i % cardsPerRow;
                        int row = i / cardsPerRow;
                        int cardX = listX + 8 + col * (cardW + 8);
                        int cardY = contentY + row * (cardH + 8);

                        if (cardY + cardH >= listY && cardY <= listY + listH) {
                            renderModelCard(graphics, font, item, cardX, cardY, cardW, cardH, mouseX, mouseY, theme);
                        }
                    }

                    int catGridH = rows * (cardH + 8) + 6;
                    contentY += catGridH;
                    totalContentHeight += catGridH;
                }
            }
        }

        graphics.disableScissor();
        maxScrollY = Math.max(0, totalContentHeight - listH + 12);

        if (scrollY > maxScrollY) scrollY = maxScrollY;

        if (maxScrollY > 0) {
            int sbW = 4;
            int sbX = listX + listW - sbW - 2;
            int sbH = Math.max(16, (int) ((float) listH / (totalContentHeight + 12) * listH));
            int sbY = listY + (int) ((float) scrollY / maxScrollY * (listH - sbH));

            graphics.fill(sbX, listY, sbX + sbW, listY + listH, 0x33000000);

            graphics.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x8864748B);
        }

        int bottomY = modalY + height - 28;
        int searchX = modalX + 10;
        int searchW = width - 160;
        int searchH = 20;
        boolean searchHov = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= bottomY && mouseY <= bottomY + searchH;

        graphics.fill(searchX, bottomY, searchX + searchW, bottomY + searchH, 0xFF0E141D);

        graphics.renderOutline(searchX, bottomY, searchW, searchH, searchFocused ? accent : (searchHov ? 0xFF475569 : divider));

        graphics.drawString(font, "🔍", searchX + 5, bottomY + 5, accent, false);
        String displaySearch = searchQuery;

        if (displaySearch.isEmpty() && !searchFocused) {
            graphics.drawString(font, I18n.get("Search models...", lang), searchX + 22, bottomY + 5, 0xFF64748B, false);
        } else {
            if (searchFocused && (now / 500) % 2 == 0) {
                displaySearch += "|";
            }

            graphics.drawString(font, displaySearch, searchX + 22, bottomY + 5, 0xFFFFFFFF, false);
        }

        if (!searchQuery.isEmpty()) {
            int clearX = searchX + searchW - 16;

            graphics.drawString(font, "✕", clearX, bottomY + 5, 0xFF94A3B8, false);
        }

        int loadBtnX = searchX + searchW + 8;
        int loadBtnW = width - 20 - searchW - 8;
        boolean hasSel = (selectedItem != null);
        boolean loadHov = hasSel && mouseX >= loadBtnX && mouseX <= loadBtnX + loadBtnW && mouseY >= bottomY && mouseY <= bottomY + searchH;
        int loadBg = hasSel ? (loadHov ? 0xFF0284C7 : 0xFF0369A1) : 0xFF1C2433;

        graphics.fill(loadBtnX, bottomY, loadBtnX + loadBtnW, bottomY + searchH, loadBg);

        if (hasSel) graphics.renderOutline(loadBtnX, bottomY, loadBtnW, searchH, accent);

        graphics.drawCenteredString(font, "📥 " + I18n.get("Load Model", lang), loadBtnX + loadBtnW / 2, bottomY + 6, hasSel ? 0xFFFFFFFF : 0xFF64748B);

        graphics.pose().popPose();
    }

    private void renderModelCard(GuiGraphics graphics, Font font, ModelItem item, int cx, int cy, int cw, int ch, int mx, int my, EditorTheme theme) {
        boolean isSel = (item == selectedItem);
        boolean isHov = mx >= cx && mx <= cx + cw && my >= cy && my <= cy + ch;
        int cardBg = isSel ? 0xFF1B2E46 : (isHov ? 0xFF1F2B3B : 0xFF141C27);
        int borderColor = isSel ? theme.getAccent() : (isHov ? 0xFF475569 : 0xFF233042);

        graphics.fill(cx, cy, cx + cw, cy + ch, cardBg);

        graphics.renderOutline(cx, cy, cw, ch, borderColor);

        if (isSel) {
            graphics.renderOutline(cx - 1, cy - 1, cw + 2, ch + 2, theme.getAccent());
        }

        PoseStack poseStack = graphics.pose();

        if (item.format.equals("player")) {
            poseStack.pushPose();

            poseStack.translate(cx + cw / 2.0f, cy + 64.0f, 150.0f);
            float s = 23.0f;

            poseStack.scale(s, -s, s);

            if (isHov) {
                item.hoverYaw = (item.hoverYaw + 1.2f) % 360.0f;
            } else {
                float diff = net.minecraft.util.Mth.wrapDegrees(180.0f - item.hoverYaw);
                if (Math.abs(diff) < 1.0f) {
                    item.hoverYaw = 180.0f;
                } else {
                    item.hoverYaw += Math.signum(diff) * Math.min(Math.abs(diff), 2.0f);
                }
            }

            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(item.hoverYaw));
            ResourceLocation skinTex = SkinManager.getInstance().getSkinTexture(item.skin != null ? item.skin : "steve", item.isSlim);

            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            net.openstage.client.model.ModelSettings previewSettings = new net.openstage.client.model.ModelSettings();

            previewSettings.setBendableLimbs(item.isBendable);

            ActorEntityRenderer.renderAnimatedPlayerModel(
                    poseStack, graphics.bufferSource(), 15728880,

                    null, null, item.isSlim, skinTex, previewSettings, null
            );

            graphics.bufferSource().endBatch();

            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

            poseStack.popPose();
        } else {
            net.openstage.client.model.ObjModel obj = null;

            if (item.file != null) {
                obj = net.openstage.client.model.ObjModelManager.getInstance().getOrLoadModel(item.file.getName());

                if (obj == null) {
                    obj = net.openstage.client.model.ObjModel.load(item.file);
                }
            } else if (item.name != null) {
                obj = net.openstage.client.model.ObjModelManager.getInstance().getOrLoadModel(item.name);
            }

            if (obj != null) {
                poseStack.pushPose();

                poseStack.translate(cx + cw / 2.0f, cy + 40.0f, 150.0f);
                float maxDim = Math.max(Math.max(obj.getWidth(), obj.getHeight()), obj.getDepth());
                float scale = (maxDim > 0.001f) ? (35.0f / maxDim) : 22.0f;

                poseStack.scale(scale, -scale, scale);

                if (isHov) {
                    item.hoverYaw = (item.hoverYaw + 1.2f) % 360.0f;
                } else {
                    float diff = net.minecraft.util.Mth.wrapDegrees(45.0f - item.hoverYaw);
                    if (Math.abs(diff) < 1.0f) {
                        item.hoverYaw = 45.0f;
                    } else {
                        item.hoverYaw += Math.signum(diff) * Math.min(Math.abs(diff), 2.0f);
                    }
                }

                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(item.hoverYaw));

                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(25.0f));
                float midX = (obj.getMinX() + obj.getMaxX()) * 0.5f;
                float midY = (obj.getMinY() + obj.getMaxY()) * 0.5f;
                float midZ = (obj.getMinZ() + obj.getMaxZ()) * 0.5f;

                poseStack.translate(-midX, -midY, -midZ);

                com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

                obj.render(poseStack, graphics.bufferSource(), 15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);

                graphics.bufferSource().endBatch();

                com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

                poseStack.popPose();
            } else {
                renderIsometricMeshPreview(graphics, cx + cw / 2, cy + 38, 0xFF00E5FF);
            }
        }

        ModelSettings.Language lang = ModelSettings.Language.EN;
        if (Minecraft.getInstance().screen instanceof OpenStageScreen oss && oss.getModelSettings() != null) {
            lang = oss.getModelSettings().getLanguage();
        }
        String displayName = I18n.get(item.name, lang);
        int maxTextW = cw - 8;

        if (font.width(displayName) > maxTextW) {
            while (displayName.length() > 3 && font.width(displayName + "..") > maxTextW) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }

            displayName += "..";
        }

        graphics.drawCenteredString(font, displayName, cx + cw / 2, cy + ch - 13, isSel ? 0xFFFFFFFF : 0xFFCBD5E1);
    }

    private void renderIsometricMeshPreview(GuiGraphics graphics, int cx, int cy, int color) {
        graphics.fill(cx - 16, cy - 14, cx + 16, cy + 16, 0xFF090E15);

        graphics.renderOutline(cx - 16, cy - 14, 32, 30, (color & 0x00FFFFFF) | 0x44000000);

        graphics.fill(cx - 8, cy - 8, cx + 8, cy - 2, (color & 0x00FFFFFF) | 0x33000000);

        graphics.renderOutline(cx - 8, cy - 8, 16, 6, color);

        graphics.fill(cx - 8, cy - 2, cx, cy + 8, (color & 0x00FFFFFF) | 0x22000000);

        graphics.fill(cx, cy - 2, cx + 8, cy + 8, (color & 0x00FFFFFF) | 0x11000000);

        graphics.renderOutline(cx - 8, cy - 2, 8, 10, color);

        graphics.renderOutline(cx, cy - 2, 8, 10, color);

        graphics.fill(cx - 1, cy - 9, cx + 1, cy - 7, 0xFFFFFFFF);

        graphics.fill(cx - 1, cy + 7, cx + 1, cy + 9, color);
    }

    private List<ModelItem> getFilteredItems(ModelCategory cat) {
        if (searchQuery.isEmpty()) return cat.items;
        List<ModelItem> list = new ArrayList<>();
        String q = searchQuery.toLowerCase().trim();

        for (ModelItem item : cat.items) {
            if (item.name.toLowerCase().contains(q) || item.format.toLowerCase().contains(q)) {
                list.add(item);
            }
        }

        return list;
    }

    public boolean mouseClicked(double mx, double my, int button, int screenW, int screenH) {
        if (!visible || closing) return closing;
        int modalX = (screenW - width) / 2;
        int modalY = (screenH - height) / 2;

        if (mx < modalX || mx > modalX + width || my < modalY || my > modalY + height) {
            close();

            return true;
        }

        int closeSize = 14;
        int closeX = modalX + width - closeSize - 7;
        int closeY = modalY + 5;

        if (mx >= closeX && mx <= closeX + closeSize && my >= closeY && my <= closeY + closeSize) {
            close();

            return true;
        }

        int topBarY = modalY + 28;
        int topBarH = 22;
        int bx = modalX + 10;
        int addBtnW = 160;

        if (mx >= bx && mx <= bx + addBtnW && my >= topBarY && my <= topBarY + topBarH) {
            openSystemFileDialog();

            return true;
        }

        bx += addBtnW + 8;
        int folderBtnW = 140;

        if (mx >= bx && mx <= bx + folderBtnW && my >= topBarY && my <= topBarY + topBarH) {
            File dir = new File("openstage/models");

            if (!dir.exists()) dir.mkdirs();

            Util.getPlatform().openFile(dir);

            return true;
        }

        int bottomY = modalY + height - 28;
        int searchX = modalX + 10;
        int searchW = width - 160;
        int searchH = 20;

        if (mx >= searchX && mx <= searchX + searchW && my >= bottomY && my <= bottomY + searchH) {
            int clearX = searchX + searchW - 16;

            if (!searchQuery.isEmpty() && mx >= clearX) {
                searchQuery = "";
            } else {
                searchFocused = true;
            }

            return true;
        } else {
            searchFocused = false;
        }

        int loadBtnX = searchX + searchW + 8;
        int loadBtnW = width - 20 - searchW - 8;

        if (mx >= loadBtnX && mx <= loadBtnX + loadBtnW && my >= bottomY && my <= bottomY + searchH) {
            if (selectedItem != null) {
                applyModelSelection(selectedItem);
            }

            return true;
        }

        int listX = modalX + 10;
        int listY = modalY + 54;
        int listW = width - 20;
        int listH = height - 54 - 34;

        if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
            int contentY = listY + 6 - scrollY;
            int cardW = 92;
            int cardH = 88;
            int cardsPerRow = Math.max(1, (listW - 16) / (cardW + 8));

            for (ModelCategory cat : categories) {
                List<ModelItem> filteredItems = getFilteredItems(cat);

                if (filteredItems.isEmpty() && !searchQuery.isEmpty()) continue;
                int catHeaderY = contentY;

                if (mx >= listX + 6 && mx <= listX + listW - 6 && my >= catHeaderY && my <= catHeaderY + 18) {
                    cat.expanded = !cat.expanded;

                    return true;
                }

                contentY += 22;

                if (cat.expanded) {
                    if (filteredItems.isEmpty()) {
                        contentY += 20;
                    } else {
                        int rows = (int) Math.ceil((double) filteredItems.size() / cardsPerRow);

                        for (int i = 0; i < filteredItems.size(); i++) {
                            ModelItem item = filteredItems.get(i);
                            int col = i % cardsPerRow;
                            int row = i / cardsPerRow;
                            int cardX = listX + 8 + col * (cardW + 8);
                            int cardY = contentY + row * (cardH + 8);

                            if (mx >= cardX && mx <= cardX + cardW && my >= cardY && my <= cardY + cardH) {
                                long now = System.currentTimeMillis();

                                if (lastClickedItem == item && (now - lastClickTime) < 350) {
                                    applyModelSelection(item);
                                } else {
                                    selectedItem = item;
                                    lastClickedItem = item;
                                    lastClickTime = now;
                                }

                                return true;
                            }
                        }

                        contentY += rows * (cardH + 8) + 6;
                    }
                }
            }
        }

        return true;
    }

    private void applyModelSelection(ModelItem item) {
        saveRecentModel(item.id);

        if (listener != null) {
            listener.onModelImported(item.file, item.name, item.format, item.id, item.isSlim, item.isBendable);
        }

        close();
    }

    private void openSystemFileDialog() {
        new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer aFilterPatterns = stack.mallocPointer(3);

                aFilterPatterns.put(stack.UTF8("*.bbmodel"));

                aFilterPatterns.put(stack.UTF8("*.geo.json"));

                aFilterPatterns.put(stack.UTF8("*.obj"));

                aFilterPatterns.flip();

                String result = TinyFileDialogs.tinyfd_openFileDialog(
                        "Import 3D Model",

                        "",

                        aFilterPatterns,

                        "3D Models (*.bbmodel, *.geo.json, *.obj)",

                        false
                );

                if (result != null && !result.isEmpty()) {
                    File src = new File(result);

                    if (src.exists() && src.isFile()) {
                        File destDir = new File("openstage/models");

                        if (!destDir.exists()) destDir.mkdirs();
                        File dest = new File(destDir, src.getName());

                        try {
                            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ignored) {}

                        Minecraft.getInstance().execute(() -> {
                            scanModelsFolder();

                            for (ModelCategory cat : categories) {
                                for (ModelItem it : cat.items) {
                                    if (it.file != null && it.file.getName().equals(dest.getName())) {
                                        selectedItem = it;
                                        break;
                                    }
                                }
                            }
                        });
                    }
                }
            } catch (Exception ignored) {}
        }, "OpenStage-ModelChooser").start();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (!visible || closing) return false;
        scrollY = Math.max(0, Math.min(maxScrollY, (int) (scrollY - verticalAmount * 24)));

        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || closing) return false;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (selectedItem != null) {
                applyModelSelection(selectedItem);
            }

            return true;
        }

        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    scrollY = 0;
                }

                return true;
            }
        }

        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!visible || closing) return false;

        if (searchFocused && chr >= 32) {
            searchQuery += chr;
            scrollY = 0;

            return true;
        }

        return false;
    }
}

