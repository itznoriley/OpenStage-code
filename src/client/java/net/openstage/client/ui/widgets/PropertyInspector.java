package net.openstage.client.ui.widgets;

import net.openstage.client.ui.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.openstage.client.model.ModelSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.openstage.client.skin.SkinManager;
import net.openstage.client.theme.EditorTheme;
import net.openstage.core.animation.*;
import net.openstage.core.animation.presets.PresetLibrary;
import java.util.List;

public class PropertyInspector {
    public enum InspectorTab {
        TRANSFORM("Transform"),

        MODEL("Model"),

        PRESETS("Presets");
        private final String title;

        InspectorTab(String title) { this.title = title; }

        public String getTitle() { return title; }
    }

    private EditorTheme getTheme() {
        return (modelSettings != null && modelSettings.getTheme() != null)

                ? modelSettings.getTheme()

                : EditorTheme.DARK_SLATE;
    }

    private int getHeaderColor() { return 0xFFFFFFFF; }

    private int getLabelColor() { return getTheme().getTextMuted(); }

    private int getValueColor() { return getTheme().getTextColor(); }

    private int getBtnBg() { return getTheme().getButtonBg(); }

    private int getBtnHover() { return getTheme().getButtonHover(); }

    private int getTabActive() { return getTheme().getAccent(); }

    private int getTabInactive() { return getTheme().getButtonBg(); }

    private int getActiveToggleBg() {
        int a = getTheme().getAccent();

        return (a & 0x00FFFFFF) | 0x55000000;
    }

    private int x, y, width, height;
    private InspectorTab currentTab = InspectorTab.TRANSFORM;
    private final ModelSettings modelSettings = new ModelSettings();
    private boolean slimModel = false;
    private int currentSkinIndex = 0;
    private String selectedBone = "body";
    private TextCard selectedTextCard = null;
    private float rotX = 0, rotY = 0, rotZ = 0;
    private float posX = 0, posY = 0, posZ = 0;
    private float scaleX = 1.0f, scaleY = 1.0f, scaleZ = 1.0f;
    private int easingIndex = 0;
    private static final EasingType[] EASINGS = EasingType.values();
    private int draggingAxis = -1;
    private double dragStartX;
    private float dragStartAngle;
    private boolean choosingRightHand = true;
    private final ItemPickerDialog itemPickerDialog;
    private float camX = 0.0f, camY = 1.4f, camZ = -3.2f;
    private float camYaw = 0.0f, camPitch = 0.0f, camRoll = 0.0f;
    private float camFov = 70.0f;
    private int draggingCamField = -1;
    private boolean cameraModeDropdownOpen = false;
    private boolean layersDropdownOpen = false;
    private boolean cameraSharp = false;
    private int cameraCurvePresetIndex = 0;
    private float[] cameraCustomHandles = null;
    private float partX = 0.0f, partY = 1.0f, partZ = 0.0f;
    private float partYaw = 0.0f, partPitch = 0.0f, partRoll = 0.0f;
    private int partCount = 20;
    private float partSpeed = 0.2f;
    private float partSpread = 0.15f;
    private float partDuration = 0.0f;
    private int partColorRgb = 0xFF5500;
    private float partSize = 1.0f;
    private String partParticleId = "minecraft:flame";
    private net.openstage.core.particle.ParticleEmitterType partEmitterType = net.openstage.core.particle.ParticleEmitterType.BURST_SPHERE;
    private int draggingPartField = -1;
    private int draggingCardField = -1;
    private int draggingEnvField = -1;
    private net.openstage.core.scene.EnvironmentKeyframe selectedEnvironmentKeyframe = null;

    public void setSelectedEnvironmentKeyframe(net.openstage.core.scene.EnvironmentKeyframe kf) {
        this.selectedEnvironmentKeyframe = kf;
        if (kf != null) {
            this.selectedTextCard = null;
            this.currentTab = InspectorTab.TRANSFORM;
        }
    }

    public net.openstage.core.scene.EnvironmentKeyframe getSelectedEnvironmentKeyframe() {
        return selectedEnvironmentKeyframe;
    }

    public interface InspectorListener {
        void onSkinChanged(String skinName, boolean isSlim);

        default void onEnvironmentKeyframeChanged(net.openstage.core.scene.EnvironmentKeyframe keyframe) {}
        default void onDeleteEnvironmentKeyframeRequested(net.openstage.core.scene.EnvironmentKeyframe keyframe) {}
        default void onTakePlayerCoordinatesRequested() {}

        void onModelChanged(boolean isSlim);

        void onAddKeyframeRequested(String bone, float[] rot, float[] pos, float[] scale, EasingType easing);

        void onBoneTransformChanged(String bone, float rx, float ry, float rz);

        default void onBonePositionChanged(String bone, float px, float py, float pz) {}

        void onDeleteKeyframeRequested(String bone);

        void onAddTextCardRequested();
        default void onDeleteTextCardRequested(TextCard card) {}
        default void onEditTextCardRequested(TextCard card) {}

        void onPresetApplied(AnimationClip presetClip);

        void onResetCameraRequested();

        default void onAddCameraKeyframe(float x, float y, float z, float yaw, float pitch, float roll, float fov) {}

        default void onCopyCameraFromEditor() {}

        default void onCameraChanged(float x, float y, float z, float yaw, float pitch, float roll, float fov) {}

        default void onCameraModeChanged(boolean isSharp, float[] handles) {}

        default void onAnimatedEyesToggled(boolean enabled) {}

        default void onOpenGraphEditor() {}

        default void onAddParticleKeyframe(float x, float y, float z, float yaw, float pitch, float roll, String type, net.openstage.core.particle.ParticleEmitterType shape, int count, float speed, float spread, float duration, int colorRgb, float size) {}
        default void onParticleChanged(float x, float y, float z, float yaw, float pitch, float roll, String type, net.openstage.core.particle.ParticleEmitterType shape, int count, float speed, float spread, float duration, int colorRgb, float size) {}
        default void onParticleTestFire() {}
        default void onDeleteParticleEmitterRequested() {}
        default void onAddParticlesRequested() {}
        default void onEditStart() {}
        default void onEditEnd() {}
    }

    private InspectorListener listener;
    private Runnable onOpenSkinPicker;

    public void setOnOpenSkinPicker(Runnable onOpenSkinPicker) {
        this.onOpenSkinPicker = onOpenSkinPicker;
    }

    public boolean isItemPickerOpen() {
        return itemPickerDialog != null && itemPickerDialog.isVisible();
    }

    public void setCameraProperties(float x, float y, float z, float yaw, float pitch, float roll, float fov) {
        this.camX = x;
        this.camY = y;
        this.camZ = z;
        this.camYaw = yaw;
        this.camPitch = pitch;
        this.camRoll = roll;
        this.camFov = Math.clamp(fov, 10.0f, 120.0f);
    }

    public float getCamX() { return camX; }

    public float getCamY() { return camY; }

    public float getCamZ() { return camZ; }

    public float getCamYaw() { return camYaw; }

    public float getCamPitch() { return camPitch; }

    public float getCamRoll() { return camRoll; }

    public float getCamFov() { return camFov; }

    public boolean isCameraSharp() {
        return cameraSharp;
    }

    public void setCameraSharp(boolean cameraSharp) {
        this.cameraSharp = cameraSharp;
    }

    public EasingType getCameraEasing() {
        return cameraSharp ? EasingType.STEP : null;
    }

    public void setParticleProperties(float x, float y, float z, float yaw, float pitch, float roll, String id, net.openstage.core.particle.ParticleEmitterType type, int count, float speed, float spread, float duration, int color, float size) {
        this.partX = x; this.partY = y; this.partZ = z;
        this.partYaw = yaw; this.partPitch = pitch; this.partRoll = roll;
        if (id != null) this.partParticleId = id;
        if (type != null) this.partEmitterType = type;
        this.partCount = count;
        this.partSpeed = speed;
        this.partSpread = spread;
        this.partDuration = duration;
        this.partColorRgb = color;
        this.partSize = size;
    }

    public float getPartX() { return partX; }
    public float getPartY() { return partY; }
    public float getPartZ() { return partZ; }
    public float getPartYaw() { return partYaw; }
    public float getPartPitch() { return partPitch; }
    public float getPartRoll() { return partRoll; }
    public String getPartParticleId() { return partParticleId; }
    public net.openstage.core.particle.ParticleEmitterType getPartEmitterType() { return partEmitterType; }
    public int getPartCount() { return partCount; }
    public float getPartSpeed() { return partSpeed; }
    public float getPartSpread() { return partSpread; }
    public float getPartDuration() { return partDuration; }
    public int getPartColorRgb() { return partColorRgb; }
    public float getPartSize() { return partSize; }

    public String getCameraCurveName() {
        if (cameraCustomHandles != null) {
            return "Свой";
        }

        var presets = net.openstage.core.animation.curve.CurvePresetRegistry.getAll();

        if (cameraCurvePresetIndex >= 0 && cameraCurvePresetIndex < presets.size()) {
            return presets.get(cameraCurvePresetIndex).displayName();
        }

        return "Линейный";
    }

    public float[] getCameraHandles() {
        if (cameraSharp) return null;

        if (cameraCustomHandles != null) return java.util.Arrays.copyOf(cameraCustomHandles, cameraCustomHandles.length);
        var presets = net.openstage.core.animation.curve.CurvePresetRegistry.getAll();

        if (cameraCurvePresetIndex >= 0 && cameraCurvePresetIndex < presets.size()) {
            return presets.get(cameraCurvePresetIndex).copyDefaultPoints();
        }

        return new float[]{0.0f, 0.0f, 1.0f, 1.0f};
    }

    public void setCameraHandles(float[] handles) {
        if (handles == null || handles.length < 4) {
            this.cameraCustomHandles = null;
            this.cameraCurvePresetIndex = 0;

            return;
        }

        var presets = net.openstage.core.animation.curve.CurvePresetRegistry.getAll();

        for (int i = 0; i < presets.size(); i++) {
            if (java.util.Arrays.equals(presets.get(i).defaultPoints(), handles)) {
                this.cameraCurvePresetIndex = i;
                this.cameraCustomHandles = null;

                return;
            }
        }

        this.cameraCustomHandles = java.util.Arrays.copyOf(handles, handles.length);
    }

    public void cycleCameraCurvePreset() {
        var presets = net.openstage.core.animation.curve.CurvePresetRegistry.getAll();
        cameraCurvePresetIndex = (cameraCurvePresetIndex + 1) % presets.size();
        cameraCustomHandles = null;
    }

    public PropertyInspector(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.itemPickerDialog = new ItemPickerDialog(item -> {
            if (choosingRightHand) {
                modelSettings.setRightHandItem(item);
            } else {
                modelSettings.setLeftHandItem(item);
            }
        }, () -> {});
    }

    public void setListener(InspectorListener listener) {
        this.listener = listener;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public ModelSettings getModelSettings() {
        return modelSettings;
    }

    public EasingType getCurrentEasing() {
        return EASINGS[easingIndex];
    }

    public void setSelectedBone(String bone) {
        this.selectedBone = (bone != null) ? bone : "body";
        this.selectedTextCard = null;
    }

    public void setSelectedTextCard(TextCard card) {
        this.selectedTextCard = card;
        this.selectedEnvironmentKeyframe = null;

        if (card != null) {
            this.selectedBone = null;
            this.currentTab = InspectorTab.TRANSFORM;
        }
    }

    public void setBoneRotations(float rx, float ry, float rz) {
        this.rotX = rx;
        this.rotY = ry;
        this.rotZ = rz;

        if ("eyes".equals(selectedBone)) {
            modelSettings.setLeftEyelid(rx);

            modelSettings.setRightEyelid(ry);
        }
    }

    public float getRotX() {
        return rotX;
    }

    public float getRotY() {
        return rotY;
    }

    public float getRotZ() {
        return rotZ;
    }

    public float[] getBoneRotations() {
        return new float[]{getRotX(), getRotY(), getRotZ()};
    }

    public void setBonePositions(float px, float py, float pz) {
        this.posX = px;
        this.posY = py;
        this.posZ = pz;
    }

    public float getPosX() {
        return posX;
    }

    public float getPosY() {
        return posY;
    }

    public float getPosZ() {
        return posZ;
    }

    public float[] getBonePositions() {
        return new float[]{getPosX(), getPosY(), getPosZ()};
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        int winW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int winH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int scX1 = Math.max(0, Math.min(winW, x));
        int scY1 = Math.max(0, Math.min(winH, y));
        int scX2 = Math.max(scX1, Math.min(winW, x + width));
        int scY2 = Math.max(scY1, Math.min(winH, y + height));

        graphics.fill(scX1, scY1, scX2, scY2, getTheme().getPanelBg());
        boolean hasValidScissor = (scX2 > scX1 && scY2 > scY1);
        if (hasValidScissor) {
            graphics.enableScissor(scX1, scY1, scX2, scY2);
        }

        renderTabBar(graphics, font, mouseX, mouseY);
        int px = x + 10;
        int py = y + 36;
        int contentW = width - 20;

        switch (currentTab) {
            case TRANSFORM -> renderTransformTab(graphics, font, px, py, contentW, mouseX, mouseY);

            case MODEL -> renderModelTab(graphics, font, px, py, contentW, mouseX, mouseY);

            case PRESETS -> renderPresetsTab(graphics, font, px, py, contentW, mouseX, mouseY);

        }

        if (hasValidScissor) {
            graphics.disableScissor();
        }
    }

    public void renderItemPicker(GuiGraphics graphics, int mouseX, int mouseY) {
        if (itemPickerDialog != null && itemPickerDialog.isVisible()) {
            itemPickerDialog.setTheme(getTheme());
            itemPickerDialog.setUiAnimations(modelSettings.isUiAnimations());
            itemPickerDialog.render(graphics, mouseX, mouseY);
        }
    }

    private void renderTabBar(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        InspectorTab[] tabs = InspectorTab.values();
        int tabW = (width - 8 - 30) / tabs.length;
        int tabH = 18;
        int ty = y + 8;

        for (int i = 0; i < tabs.length; i++) {
            int tx = x + 4 + i * tabW;
            boolean active = (tabs[i] == currentTab);
            boolean hovered = mouseX >= tx && mouseX <= tx + tabW && mouseY >= ty && mouseY <= ty + tabH;
            int bg = active ? getTabActive() : (hovered ? getBtnHover() : getTabInactive());

            graphics.fill(tx, ty, tx + tabW - 2, ty + tabH, bg);

            graphics.renderOutline(tx, ty, tabW - 2, tabH, active ? getTheme().getAccent() : getTheme().getDivider());
            ModelSettings.Language lang = (modelSettings != null) ? modelSettings.getLanguage() : ModelSettings.Language.EN;
            String label = switch (tabs[i]) {
                case TRANSFORM -> (tabW < 75) ? I18n.get("Pose", lang) : I18n.get("Transform", lang);
                case MODEL -> I18n.get("Model", lang);
                case PRESETS -> (tabW < 65) ? I18n.get("Preset", lang) : I18n.get("Presets", lang);
            };

            if (tabW < 36) {
                label = switch (tabs[i]) {
                    case TRANSFORM -> "T";
                    case MODEL -> "M";
                    case PRESETS -> "P";
                };
            }

            int tw = font.width(label);
            int labelX = tx + (tabW - 2 - tw) / 2;

            if (labelX < tx + 2) labelX = tx + 2;

            if (tx + tabW - 3 > tx + 1 && ty + tabH - 1 > ty + 1) {
                graphics.enableScissor(tx + 1, ty + 1, tx + tabW - 3, ty + tabH - 1);
                graphics.drawString(font, label, labelX, ty + 5, active ? 0xFFFFFFFF : getTheme().getTextMuted(), false);
                graphics.disableScissor();
            } else {
                graphics.drawString(font, label, labelX, ty + 5, active ? 0xFFFFFFFF : getTheme().getTextMuted(), false);
            }
        }
    }

    private void renderTransformTab(GuiGraphics graphics, Font font, int px, int py, int contentW, int mouseX, int mouseY) {
        if (selectedEnvironmentKeyframe != null) {
            renderEnvironmentSection(graphics, font, px, py, contentW, mouseX, mouseY);
        } else if (selectedTextCard != null) {
            renderTextCardSection(graphics, font, px, py, contentW, mouseX, mouseY);
        } else if ("particles".equals(selectedBone)) {
            renderParticlesSection(graphics, font, px, py, contentW, mouseX, mouseY);
        } else if ("camera".equals(selectedBone)) {
            renderCameraSection(graphics, font, px, py, contentW, mouseX, mouseY);
        } else if ("eyes".equals(selectedBone)) {
            renderEyesSection(graphics, font, px, py, contentW, mouseX, mouseY);
        } else {
            renderBoneSection(graphics, font, px, py, contentW, mouseX, mouseY);
        }
    }

    private void renderEnvironmentSection(GuiGraphics graphics, Font font, int px, int py, int contentW, int mouseX, int mouseY) {
        boolean ru = (modelSettings != null && modelSettings.getLanguage() == ModelSettings.Language.RU);
        graphics.drawString(font, ru ? "🏞 Окружение сцены" : "🏞 Scene Environment", px, py, 0xFF34D399, false);
        py += 14;

        if (selectedEnvironmentKeyframe == null) return;
        var kf = selectedEnvironmentKeyframe;

        String timeInfo = String.format(java.util.Locale.ROOT, ru ? "Время: %.2fs" : "Time: %.2fs", kf.time());
        graphics.drawString(font, timeInfo, px, py, getTheme().getTextMuted(), false);
        py += 13;

        String modeText = (ru ? "Режим: [ " : "Mode: [ ") + kf.mode().getDisplayName(ru) + " ▾ ]";
        drawButton(graphics, font, modeText, px, py, contentW, 16, mouseX, mouseY);
        py += 19;

        if (kf.mode() == net.openstage.core.scene.EnvironmentKeyframe.EnvironmentMode.WORLD) {
            graphics.drawString(font, ru ? "Анкор мира (World Anchor):" : "World Anchor:", px, py, getTheme().getTextMuted(), false);
            py += 12;

            drawScrubber(graphics, font, "X", (float) kf.anchorX(), 0xFFF87171, px, py, contentW, 16, mouseX, mouseY, draggingEnvField == 0, "", 2000.0f);
            py += 18;
            drawScrubber(graphics, font, "Y", (float) kf.anchorY(), 0xFF4ADE80, px, py, contentW, 16, mouseX, mouseY, draggingEnvField == 1, "", 256.0f);
            py += 18;
            drawScrubber(graphics, font, "Z", (float) kf.anchorZ(), 0xFF60A5FA, px, py, contentW, 16, mouseX, mouseY, draggingEnvField == 2, "", 2000.0f);
            py += 20;

            drawButton(graphics, font, ru ? "📍 Взять координаты игрока" : "📍 Take Player Pos", px, py, contentW, 16, mouseX, mouseY);
            py += 20;
        }

        String transText = (ru ? "Переход: [ " : "Transition: [ ") + kf.transition().getDisplayName(ru) + " ▾ ]";
        drawButton(graphics, font, transText, px, py, contentW, 16, mouseX, mouseY);
        py += 19;

        if (kf.transition() == net.openstage.core.scene.EnvironmentKeyframe.EnvironmentTransition.FADE) {
            drawScrubber(graphics, font, ru ? "Затухание" : "Fade Time", kf.fadeDuration(), 0xFFF59E0B, px, py, contentW, 16, mouseX, mouseY, draggingEnvField == 3, "s", 3.0f);
            py += 19;
        }

        drawButton(graphics, font, ru ? "🗑 Удалить ключ окружения" : "🗑 Delete Keyframe", px, py, contentW, 16, mouseX, mouseY);
    }

    private void renderParticlesSection(GuiGraphics graphics, Font font, int px, int py, int contentW, int mouseX, int mouseY) {
        graphics.drawString(font, "✨ Эмиттер частиц (Grid 3D)", px, py, 0xFFE879F9, false);
        py += 14;

        drawButton(graphics, font, "🔥 Тестовый залп", px, py, contentW, 16, mouseX, mouseY);
        py += 20;

        graphics.drawString(font, "Позиция на гриде (X/Y/Z):", px, py, getTheme().getTextMuted(), false);
        py += 12;
        drawScrubber(graphics, font, "X", partX, 0xFFF87171, px, py, contentW, 16, mouseX, mouseY, draggingPartField == 0, "b", 30.0f);
        py += 18;
        drawScrubber(graphics, font, "Y", partY, 0xFF4ADE80, px, py, contentW, 16, mouseX, mouseY, draggingPartField == 1, "b", 30.0f);
        py += 18;
        drawScrubber(graphics, font, "Z", partZ, 0xFF60A5FA, px, py, contentW, 16, mouseX, mouseY, draggingPartField == 2, "b", 30.0f);
        py += 20;

        graphics.drawString(font, "Вращение эмиттера (Yaw/Pitch/Roll):", px, py, getTheme().getTextMuted(), false);
        py += 12;
        drawScrubber(graphics, font, "Yaw", partYaw, 0xFFF59E0B, px, py, contentW, 16, mouseX, mouseY, draggingPartField == 3, "°", 180.0f);
        py += 18;
        drawScrubber(graphics, font, "Pit", partPitch, 0xFFEC4899, px, py, contentW, 16, mouseX, mouseY, draggingPartField == 4, "°", 90.0f);
        py += 18;
        drawScrubber(graphics, font, "Rol", partRoll, 0xFF8B5CF6, px, py, contentW, 16, mouseX, mouseY, draggingPartField == 5, "°", 180.0f);
        py += 20;

        graphics.drawString(font, "Форма вылета:", px, py, getTheme().getTextMuted(), false);
        py += 12;
        drawButton(graphics, font, "Форма: " + partEmitterType.getDisplayName(), px, py, contentW, 16, mouseX, mouseY);
        py += 19;

        graphics.drawString(font, "Тип частицы MC 1.21.1:", px, py, getTheme().getTextMuted(), false);
        py += 12;
        String cleanName = partParticleId.replace("minecraft:", "");
        drawButton(graphics, font, "Частица: " + cleanName, px, py, contentW, 16, mouseX, mouseY);
        py += 19;

        graphics.drawString(font, "Параметры потока:", px, py, getTheme().getTextMuted(), false);
        py += 12;
        drawScrubber(graphics, font, "Кол-во", (float) partCount, 0xFF38BDF8, px, py, contentW, 16, mouseX, mouseY, draggingPartField == 6, "count", 100.0f);
        py += 18;
        drawScrubber(graphics, font, "Скор.", partSpeed, 0xFF34D399, px, py, contentW, 16, mouseX, mouseY, draggingPartField == 7, "speed", 2.0f);
        py += 18;
        drawScrubber(graphics, font, "Разброс", partSpread, 0xFFA78BFA, px, py, contentW, 16, mouseX, mouseY, draggingPartField == 8, "spread", 2.0f);
        py += 18;
        drawScrubber(graphics, font, "Длительн.", partDuration, 0xFFF472B6, px, py, contentW, 16, mouseX, mouseY, draggingPartField == 9, "time", 5.0f);
        py += 22;

        drawButton(graphics, font, "+ Ключ частиц", px, py, contentW, 18, mouseX, mouseY);
        py += 21;
        drawButton(graphics, font, "📈 Редактор кривых частиц", px, py, contentW, 16, mouseX, mouseY);
        py += 19;
        drawButton(graphics, font, "🗑 Удалить ключ", px, py, contentW, 16, mouseX, mouseY);
        py += 19;
        drawButton(graphics, font, "🗑 Удалить эмиттер целиком", px, py, contentW, 16, mouseX, mouseY);
    }

    private void renderCameraSection(GuiGraphics graphics, Font font, int px, int py, int contentW, int mouseX, int mouseY) {
        graphics.drawString(font, "Bone: 🎥 Камера", px, py, getHeaderColor(), false);
        py += 14;

        drawButton(graphics, font, "📸 Скопировать вид редактора", px, py, contentW, 16, mouseX, mouseY);
        py += 19;

        graphics.drawString(font, "Позиция (X / Y / Z):", px, py, getTheme().getTextMuted(), false);
        py += 12;

        drawScrubber(graphics, font, "X", camX, 0xFFF87171, px, py, contentW, 16, mouseX, mouseY, draggingCamField == 0, "m", 50.0f);
        py += 18;

        drawScrubber(graphics, font, "Y", camY, 0xFF4ADE80, px, py, contentW, 16, mouseX, mouseY, draggingCamField == 1, "m", 50.0f);
        py += 18;

        drawScrubber(graphics, font, "Z", camZ, 0xFF60A5FA, px, py, contentW, 16, mouseX, mouseY, draggingCamField == 2, "m", 50.0f);
        py += 20;

        graphics.drawString(font, "Вращение (Yaw / Pit / Rol):", px, py, getTheme().getTextMuted(), false);
        py += 12;

        drawScrubber(graphics, font, "Yaw", camYaw, 0xFFFBBF24, px, py, contentW, 16, mouseX, mouseY, draggingCamField == 3, "°", 180.0f);
        py += 18;

        drawScrubber(graphics, font, "Pit", camPitch, 0xFFA78BFA, px, py, contentW, 16, mouseX, mouseY, draggingCamField == 4, "°", 90.0f);
        py += 18;

        drawScrubber(graphics, font, "Rol", camRoll, 0xFFF472B6, px, py, contentW, 16, mouseX, mouseY, draggingCamField == 5, "°", 180.0f);
        py += 20;

        graphics.drawString(font, "FOV (угол обзора):", px, py, getTheme().getTextMuted(), false);
        py += 12;

        drawScrubber(graphics, font, "FOV", camFov, 0xFF38BDF8, px, py, contentW, 16, mouseX, mouseY, draggingCamField == 6, "°", 120.0f);
        py += 20;

        graphics.drawString(font, "Режим камеры:", px, py, getTheme().getTextMuted(), false);
        py += 12;
        String modeLabel = cameraSharp ? "Режим: ⚡ Резкая ▾" : "Режим: 🌊 Плавная ▾";
        int modeBtnY = py;

        drawButton(graphics, font, modeLabel, px, py, contentW, 16, mouseX, mouseY);
        py += 18;

        if (!cameraSharp) {
            drawButton(graphics, font, "📈 Редактор графов", px, py, contentW, 15, mouseX, mouseY);
            py += 17;
        }

        drawButton(graphics, font, "✚ Добавить кадр камеры", px, py, contentW, 16, mouseX, mouseY);
        py += 18;

        drawButton(graphics, font, "🗑 Удалить кадр камеры", px, py, contentW, 16, mouseX, mouseY);

        if (cameraModeDropdownOpen) {
            int dropY = modeBtnY + 16;
            int dropH = 34;

            graphics.fill(px, dropY, px + contentW, dropY + dropH, 0xFF141923);

            graphics.renderOutline(px, dropY, contentW, dropH, 0xFF00FFCC);
            boolean hov1 = mouseX >= px && mouseX <= px + contentW && mouseY >= dropY && mouseY < dropY + 17;
            int bg1 = hov1 ? 0xFF2A3448 : (!cameraSharp ? 0x3300FFCC : 0x00000000);

            graphics.fill(px + 1, dropY + 1, px + contentW - 1, dropY + 17, bg1);

            graphics.drawString(font, (!cameraSharp ? "✔ " : "   ") + "🌊 Плавная", px + 4, dropY + 5, !cameraSharp ? 0xFF38BDF8 : 0xFFCCCCCC, false);
            boolean hov2 = mouseX >= px && mouseX <= px + contentW && mouseY >= dropY + 17 && mouseY < dropY + dropH;
            int bg2 = hov2 ? 0xFF2A3448 : (cameraSharp ? 0x33FF5533 : 0x00000000);

            graphics.fill(px + 1, dropY + 17, px + contentW - 1, dropY + dropH - 1, bg2);

            graphics.drawString(font, (cameraSharp ? "✔ " : "   ") + "⚡ Резкая", px + 4, dropY + 21, cameraSharp ? 0xFFFF5533 : 0xFFCCCCCC, false);
        }
    }

    private void renderEyesSection(GuiGraphics graphics, Font font, int px, int py, int contentW, int mouseX, int mouseY) {
        ModelSettings.Language piLang = (modelSettings != null) ? modelSettings.getLanguage() : ModelSettings.Language.EN;
        graphics.drawString(font, I18n.get("Bone: ", piLang) + I18n.get("eyes (Animated)", piLang), px, py, getHeaderColor(), false);
        py += 15;
        String lookText = String.format("Pupil: X:%.2f Y:%.2f", modelSettings.getPupilX(), modelSettings.getPupilY());

        graphics.drawString(font, lookText, px, py, getLabelColor(), false);
        py += 13;
        int qw = (contentW - 8) / 4;

        drawButton(graphics, font, "⬅ L", px, py, qw, 14, mouseX, mouseY);

        drawButton(graphics, font, "⬆ U", px + qw + 2, py, qw, 14, mouseX, mouseY);

        drawButton(graphics, font, "⬇ D", px + 2 * (qw + 2), py, qw, 14, mouseX, mouseY);

        drawButton(graphics, font, "➡ R", px + 3 * (qw + 2), py, qw, 14, mouseX, mouseY);
        py += 16;

        drawButton(graphics, font, "• Look Center", px, py, contentW, 14, mouseX, mouseY);
        py += 18;
        String lidText = String.format("Eyelids: L:%.0f%% R:%.0f%%", modelSettings.getLeftEyelid() * 100, modelSettings.getRightEyelid() * 100);

        graphics.drawString(font, lidText, px, py, getLabelColor(), false);
        py += 13;
        int thw = (contentW - 6) / 3;

        drawButton(graphics, font, "😉 Wink L", px, py, thw, 14, mouseX, mouseY);

        drawButton(graphics, font, "😉 Wink R", px + thw + 3, py, thw, 14, mouseX, mouseY);

        drawButton(graphics, font, "😑 Blink", px + 2 * (thw + 3), py, thw, 14, mouseX, mouseY);
        py += 16;

        drawButton(graphics, font, "👀 Open Eyes", px, py, contentW, 14, mouseX, mouseY);
        py += 18;
        String easingLabel = I18n.get("Easing: ", piLang) + I18n.get(EASINGS[easingIndex].getJsonName(), piLang);

        drawButton(graphics, font, easingLabel, px, py, contentW, 15, mouseX, mouseY);
        py += 18;

        drawButton(graphics, font, "✚ Add Eyes Keyframe", px, py, contentW, 16, mouseX, mouseY);
        py += 19;

        drawButton(graphics, font, "🗑 Delete Keyframe", px, py, contentW, 16, mouseX, mouseY);
    }

    private void renderBoneSection(GuiGraphics graphics, Font font, int px, int py, int contentW, int mouseX, int mouseY) {
        ModelSettings.Language piLang = (modelSettings != null) ? modelSettings.getLanguage() : ModelSettings.Language.EN;
        String boneName = "camera".equals(selectedBone) ? I18n.get("Camera", piLang) : I18n.getBoneDisplayName(selectedBone, piLang);
        graphics.drawString(font, I18n.get("Bone: ", piLang) + boneName, px, py, getHeaderColor(), false);
        py += 14;
        int modeW = (contentW - 4) / 2;
        boolean isRotate = (modelSettings.getGizmoMode() == ModelSettings.GizmoMode.ROTATE);

        drawModeButton(graphics, font, "↻ " + I18n.get("Rotate", piLang), isRotate, px, py, modeW, 16, mouseX, mouseY);

        drawModeButton(graphics, font, "⇄ " + I18n.get("Move", piLang), !isRotate, px + modeW + 4, py, modeW, 16, mouseX, mouseY);
        py += 20;

        if (isRotate) {
            graphics.drawString(font, I18n.get("Drag to rotate:", piLang), px, py, getTheme().getTextMuted(), false);
            py += 12;

            drawScrubber(graphics, font, "X", rotX, 0xFFF87171, px, py, contentW, 16, mouseX, mouseY, draggingAxis == 0, "°", 180.0f);
            py += 19;

            drawScrubber(graphics, font, "Y", rotY, 0xFF4ADE80, px, py, contentW, 16, mouseX, mouseY, draggingAxis == 1, "°", 180.0f);
            py += 19;

            drawScrubber(graphics, font, "Z", rotZ, 0xFF60A5FA, px, py, contentW, 16, mouseX, mouseY, draggingAxis == 2, "°", 180.0f);
            py += 19;

            drawButton(graphics, font, "↺ " + I18n.get("Reset Rotations (0°)", piLang), px, py, contentW, 15, mouseX, mouseY);
            py += 19;
        } else {
            graphics.drawString(font, I18n.get("Drag to move position:", piLang), px, py, getTheme().getTextMuted(), false);
            py += 12;

            drawScrubber(graphics, font, "X", posX, 0xFFF87171, px, py, contentW, 16, mouseX, mouseY, draggingAxis == 0, "m", 2.0f);
            py += 19;

            drawScrubber(graphics, font, "Y", posY, 0xFF4ADE80, px, py, contentW, 16, mouseX, mouseY, draggingAxis == 1, "m", 2.0f);
            py += 19;

            drawScrubber(graphics, font, "Z", posZ, 0xFF60A5FA, px, py, contentW, 16, mouseX, mouseY, draggingAxis == 2, "m", 2.0f);
            py += 19;

            drawButton(graphics, font, "↺ " + I18n.get("Reset Position (0, 0, 0)", piLang), px, py, contentW, 15, mouseX, mouseY);
            py += 19;
        }

        String easingLabel = I18n.get("Easing: ", piLang) + I18n.get(EASINGS[easingIndex].getJsonName(), piLang);

        drawButton(graphics, font, easingLabel, px, py, contentW, 15, mouseX, mouseY);
        py += 20;

        drawButton(graphics, font, "✚ " + I18n.get("Add Text Card", piLang), px, py, contentW, 16, mouseX, mouseY);
        py += 20;
        drawButton(graphics, font, "✚ " + I18n.get("Particles", piLang), px, py, contentW, 16, mouseX, mouseY);
    }

    private void drawScrubber(GuiGraphics graphics, Font font, String axisName, float angleDeg, int axisColor,

                              int bx, int by, int bw, int bh, int mouseX, int mouseY, boolean dragging) {
        drawScrubber(graphics, font, axisName, angleDeg, axisColor, bx, by, bw, bh, mouseX, mouseY, dragging, "°", 180.0f);
    }

    private void drawScrubber(GuiGraphics graphics, Font font, String axisName, float val, int axisColor,

                              int bx, int by, int bw, int bh, int mouseX, int mouseY, boolean dragging, String unit, float range) {
        boolean hovered = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        int bg = dragging ? getActiveToggleBg() : (hovered ? getBtnHover() : getBtnBg());
        int border = dragging ? getTheme().getAccent() : (hovered ? getTheme().getDividerHover() : getTheme().getDivider());

        graphics.fill(bx, by, bx + bw, by + bh, bg);
        int sliderFill = (getTheme().getAccent() & 0x00FFFFFF) | 0x44000000;
        boolean isUnipolar = unit.equals("count") || unit.equals("speed") || unit.equals("time") || unit.equals("s") || unit.equals("м/с") || unit.equals("шт") || unit.isEmpty();

        if (isUnipolar) {
            float norm = Math.max(0.0f, Math.min(1.0f, val / range));
            int barX = bx + 2 + (int) (norm * (bw - 4));
            graphics.fill(bx + 2, by + 2, barX, by + bh - 2, sliderFill);
            graphics.fill(barX - 1, by + 1, barX + 2, by + bh - 1, getTheme().getAccent());
        } else {
            float norm = unit.equals("x")
                    ? Math.max(-1.0f, Math.min(1.0f, (val - 1.0f) / 2.0f))
                    : Math.max(-1.0f, Math.min(1.0f, val / range));
            int midX = bx + bw / 2;
            int barX = midX + (int) (norm * (bw * 0.45f));

            if (norm < 0) {
                graphics.fill(barX, by + 2, midX, by + bh - 2, sliderFill);
            } else {
                graphics.fill(midX, by + 2, barX, by + bh - 2, sliderFill);
            }

            graphics.fill(midX - 1, by + 1, midX + 1, by + bh - 1, getTheme().getDivider());
            graphics.fill(barX - 1, by + 1, barX + 2, by + bh - 1, getTheme().getAccent());
        }

        graphics.renderOutline(bx, by, bw, bh, border);

        graphics.drawString(font, axisName, bx + 5, by + 4, axisColor, false);
        String valText;
        if (unit.equals("°")) {
            valText = String.format(java.util.Locale.ROOT, "%.1f°", val);
        } else if (unit.equals("x")) {
            valText = String.format(java.util.Locale.ROOT, "%.2fx", val);
        } else if (unit.equals("count")) {
            valText = String.format(java.util.Locale.ROOT, "%d", (int) val);
        } else if (unit.equals("speed")) {
            valText = String.format(java.util.Locale.ROOT, "%.2f", val);
        } else if (unit.equals("spread")) {
            valText = String.format(java.util.Locale.ROOT, "%.2f", val);
        } else if (unit.equals("time") || unit.equals("s")) {
            valText = String.format(java.util.Locale.ROOT, "%.2f с", val);
        } else if (unit.equals("m") || unit.equals("b")) {
            valText = String.format(java.util.Locale.ROOT, "%.2f", val);
        } else if (unit.isEmpty()) {
            valText = String.format(java.util.Locale.ROOT, "%.2f", val);
        } else {
            valText = String.format(java.util.Locale.ROOT, "%.2f %s", val, unit);
        }
        int vw = font.width(valText);

        graphics.drawString(font, valText, bx + bw - vw - 6, by + 4, getTheme().getTextColor(), false);
    }

    private void renderTextCardSection(GuiGraphics graphics, Font font, int px, int py, int contentW, int mouseX, int mouseY) {
        boolean ru = (modelSettings != null && modelSettings.getLanguage() == ModelSettings.Language.RU);
        graphics.drawString(font, ru ? "Текстовая 3D-плашка" : "3D Text Plate", px, py, getHeaderColor(), false);
        py += 15;

        String rawTxt = selectedTextCard.getText();
        String txt = font.plainSubstrByWidth((rawTxt != null && !rawTxt.isEmpty()) ? rawTxt : (ru ? "Текст плашки..." : "Plate text..."), contentW - 20);
        drawButton(graphics, font, "✏ " + txt, px, py, contentW, 16, mouseX, mouseY);
        py += 19;

        String timeInfo = String.format(java.util.Locale.ROOT, ru ? "Время: %.1fс - %.1fс (%.1fс)" : "Time: %.1fs - %.1fs (%.1fs)",
                selectedTextCard.getStartTime(), selectedTextCard.getEndTime(),
                selectedTextCard.getDuration());
        graphics.drawString(font, timeInfo, px, py, getLabelColor(), false);
        py += 15;

        drawButton(graphics, font, (ru ? "Позиция: " : "Position: ") + selectedTextCard.getPosition().name(), px, py, contentW, 16, mouseX, mouseY);
        py += 19;

        drawButton(graphics, font, (ru ? "Эффект: " : "Effect: ") + selectedTextCard.getEffect().name(), px, py, contentW, 16, mouseX, mouseY);
        py += 20;

        graphics.drawString(font, ru ? "3D Вращение (Rot):" : "3D Rotation (Rot):", px, py, getTheme().getTextMuted(), false);
        py += 12;
        drawScrubber(graphics, font, "Rot X", selectedTextCard.getRotX(), 0xFFF87171, px, py, contentW, 16, mouseX, mouseY, draggingCardField == 0, "°", 180.0f);
        py += 18;
        drawScrubber(graphics, font, "Rot Y", selectedTextCard.getRotY(), 0xFF4ADE80, px, py, contentW, 16, mouseX, mouseY, draggingCardField == 1, "°", 180.0f);
        py += 18;
        drawScrubber(graphics, font, "Rot Z", selectedTextCard.getRotZ(), 0xFF60A5FA, px, py, contentW, 16, mouseX, mouseY, draggingCardField == 2, "°", 180.0f);
        py += 22;

        graphics.drawString(font, ru ? "Позиция и масштаб:" : "Position & Scale:", px, py, getTheme().getTextMuted(), false);
        py += 12;
        drawScrubber(graphics, font, ru ? "Смещ X" : "Offset X", selectedTextCard.getOffsetX(), 0xFFF59E0B, px, py, contentW, 16, mouseX, mouseY, draggingCardField == 3, "", 200.0f);
        py += 18;
        drawScrubber(graphics, font, ru ? "Смещ Y" : "Offset Y", selectedTextCard.getOffsetY(), 0xFFEC4899, px, py, contentW, 16, mouseX, mouseY, draggingCardField == 4, "", 200.0f);
        py += 18;
        drawScrubber(graphics, font, ru ? "Глуб Z" : "Depth Z", selectedTextCard.getOffsetZ(), 0xFF8B5CF6, px, py, contentW, 16, mouseX, mouseY, draggingCardField == 5, "", 300.0f);
        py += 18;
        drawScrubber(graphics, font, ru ? "Масштаб" : "Scale", selectedTextCard.getScaleX(), 0xFF38BDF8, px, py, contentW, 16, mouseX, mouseY, draggingCardField == 6, "x", 3.0f);
        py += 22;

        graphics.drawString(font, ru ? "Цветовые пресеты:" : "Color Presets:", px, py, getTheme().getTextMuted(), false);
        py += 12;
        int pbtnW = (contentW - 8) / 5;
        drawColorPresetButton(graphics, px, py, pbtnW, 14, 0xFFFFFFFF, 0x88000000, mouseX, mouseY);
        drawColorPresetButton(graphics, px + pbtnW + 2, py, pbtnW, 14, 0xFF00F0FF, 0xAA051020, mouseX, mouseY);
        drawColorPresetButton(graphics, px + (pbtnW + 2) * 2, py, pbtnW, 14, 0xFFFF007F, 0xAA200010, mouseX, mouseY);
        drawColorPresetButton(graphics, px + (pbtnW + 2) * 3, py, pbtnW, 14, 0xFFF59E0B, 0xAA201500, mouseX, mouseY);
        drawColorPresetButton(graphics, px + (pbtnW + 2) * 4, py, pbtnW, 14, 0xFF22C55E, 0xAA002005, mouseX, mouseY);
        py += 20;

        drawButton(graphics, font, "↺ Сбросить 3D поворот", px, py, contentW, 16, mouseX, mouseY);
        py += 19;
        drawButton(graphics, font, "+1с Длительность", px, py, contentW, 16, mouseX, mouseY);
        py += 19;
        drawButton(graphics, font, "🗑 Удалить плашку", px, py, contentW, 16, mouseX, mouseY);
    }

    private void drawColorPresetButton(GuiGraphics graphics, int bx, int by, int bw, int bh, int textCol, int bgCol, int mx, int my) {
        boolean hovered = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        graphics.fill(bx, by, bx + bw, by + bh, bgCol);
        graphics.renderOutline(bx, by, bw, bh, hovered ? 0xFFFFFFFF : textCol);
        graphics.fill(bx + 2, by + 2, bx + bw - 2, by + bh - 2, textCol);
    }

    private void renderModelTab(GuiGraphics graphics, Font font, int px, int py, int contentW, int mouseX, int mouseY) {
        ModelSettings.Language piLang = (modelSettings != null) ? modelSettings.getLanguage() : ModelSettings.Language.EN;
        graphics.drawString(font, I18n.get("Character Settings", piLang), px, py, getHeaderColor(), false);
        py += 16;
        List<String> available = SkinManager.getInstance().getAvailableSkinNames();
        String currentSkin = (currentSkinIndex >= 0 && currentSkinIndex < available.size())
                ? available.get(currentSkinIndex) : "default";

        String modelName;
        if (currentSkin != null && (currentSkin.endsWith(".obj") || currentSkin.endsWith(".geo.json") || currentSkin.endsWith(".bbmodel"))) {
            modelName = currentSkin;
        } else if (slimModel) {
            modelName = modelSettings.isBendableLimbs() ? "Alex (Bendable)" : "Alex (Slim)";
        } else {
            modelName = modelSettings.isBendableLimbs() ? "Steve (Bendable)" : "Steve (Wide)";
        }

        drawButton(graphics, font, I18n.get("Model", piLang) + ": " + I18n.get(modelName, piLang), px, py, contentW, 16, mouseX, mouseY);
        py += 20;
        String skinLabel = I18n.get("Skin", piLang) + ": " + currentSkin;

        drawButton(graphics, font, skinLabel, px, py, contentW, 16, mouseX, mouseY);
        py += 20;

        drawButton(graphics, font, "📁 " + I18n.get("Skin Folder", piLang), px, py, contentW, 15, mouseX, mouseY);
        py += 19;

        int uvBoxSize = 54;
        int uvX = px + contentW - uvBoxSize;
        int leftW = (uvX - 6) - px;

        graphics.drawString(font, I18n.get("Held Items", piLang), px, py, getHeaderColor(), false);
        int itemBtnY = py + 12;
        int itemBtnH = 18;

        ItemStack rItem = modelSettings.getRightHandItem();
        String rPrefix = (piLang == ModelSettings.Language.RU) ? "Пр: " : "R Hand: ";
        String rEmpty = I18n.get("Right: [Empty - Click]", piLang);
        drawItemSlotButton(graphics, font, rItem, rPrefix, rEmpty, px, itemBtnY, leftW, itemBtnH, mouseX, mouseY);

        itemBtnY += 21;
        ItemStack lItem = modelSettings.getLeftHandItem();
        String lPrefix = (piLang == ModelSettings.Language.RU) ? "Лев: " : "L Hand: ";
        String lEmpty = I18n.get("Left: [Empty - Click]", piLang);
        drawItemSlotButton(graphics, font, lItem, lPrefix, lEmpty, px, itemBtnY, leftW, itemBtnH, mouseX, mouseY);

        graphics.fill(uvX, py, uvX + uvBoxSize, py + uvBoxSize, 0xFF0B131B);
        ResourceLocation skinTexture = SkinManager.getInstance().getSkinTexture(currentSkin, slimModel);
        graphics.blit(skinTexture, uvX, py, 0, 0, uvBoxSize, uvBoxSize, 64, 64);
        graphics.renderOutline(uvX, py, uvBoxSize, uvBoxSize, getTheme().getDivider());

        String resText = SkinManager.getInstance().getSkinDimensions(currentSkin);
        int rw = font.width(resText);
        graphics.drawString(font, resText, uvX + (uvBoxSize - rw) / 2, py + uvBoxSize + 2, getLabelColor(), false);

        int sectionH = Math.max(itemBtnY + 16, py + uvBoxSize + 12) - py;
        py += sectionH + 8;

        graphics.drawString(font, I18n.get("Animation & Joints", piLang), px, py, getHeaderColor(), false);
        py += 14;

        drawToggleButton(graphics, font, I18n.get("Onion Skin", piLang), modelSettings.isOnionSkin(), px, py, contentW, 16, mouseX, mouseY);
        py += 22;

        int activeCount = (modelSettings.isShowHat() ? 1 : 0) +
                (modelSettings.isShowJacket() ? 1 : 0) +
                (modelSettings.isShowRightSleeve() ? 1 : 0) +
                (modelSettings.isShowLeftSleeve() ? 1 : 0) +
                (modelSettings.isShowRightPants() ? 1 : 0) +
                (modelSettings.isShowLeftPants() ? 1 : 0);
        String layerTitle = (piLang == ModelSettings.Language.RU)
                ? "Слои скина (" + activeCount + "/6) " + (layersDropdownOpen ? "▲" : "▼")
                : "Skin Layers (" + activeCount + "/6) " + (layersDropdownOpen ? "▲" : "▼");

        drawButton(graphics, font, layerTitle, px, py, contentW, 16, mouseX, mouseY);
        py += 19;

        if (layersDropdownOpen) {
            int dropBg = (modelSettings != null && modelSettings.getTheme() != null) ? modelSettings.getTheme().getPanelBg() : 0xFF0B1017;
            int dropH = 6 * 17 + 24;
            graphics.fill(px, py, px + contentW, py + dropH, dropBg);
            graphics.renderOutline(px, py, contentW, dropH, getTheme().getAccent());
            int itemY = py + 3;

            drawCheckboxRow(graphics, font, I18n.get("Hat", piLang), modelSettings.isShowHat(), px + 4, itemY, contentW - 8, 15, mouseX, mouseY); itemY += 17;
            drawCheckboxRow(graphics, font, I18n.get("Jacket", piLang), modelSettings.isShowJacket(), px + 4, itemY, contentW - 8, 15, mouseX, mouseY); itemY += 17;
            drawCheckboxRow(graphics, font, I18n.get("R Sleeve", piLang), modelSettings.isShowRightSleeve(), px + 4, itemY, contentW - 8, 15, mouseX, mouseY); itemY += 17;
            drawCheckboxRow(graphics, font, I18n.get("L Sleeve", piLang), modelSettings.isShowLeftSleeve(), px + 4, itemY, contentW - 8, 15, mouseX, mouseY); itemY += 17;
            drawCheckboxRow(graphics, font, I18n.get("R Pants", piLang), modelSettings.isShowRightPants(), px + 4, itemY, contentW - 8, 15, mouseX, mouseY); itemY += 17;
            drawCheckboxRow(graphics, font, I18n.get("L Pants", piLang), modelSettings.isShowLeftPants(), px + 4, itemY, contentW - 8, 15, mouseX, mouseY); itemY += 17;

            int halfDropW = (contentW - 12) / 2;
            drawSmallBtn(graphics, font, (piLang == ModelSettings.Language.RU ? "Все вкл" : "All On"), px + 4, itemY, halfDropW, 14, mouseX, mouseY);
            drawSmallBtn(graphics, font, (piLang == ModelSettings.Language.RU ? "Все выкл" : "All Off"), px + 8 + halfDropW, itemY, halfDropW, 14, mouseX, mouseY);
            py += dropH + 4;
        }

    }

    private void renderPresetsTab(GuiGraphics graphics, Font font, int px, int py, int contentW, int mouseX, int mouseY) {
        ModelSettings.Language lang = (modelSettings != null) ? modelSettings.getLanguage() : ModelSettings.Language.EN;
        graphics.drawString(font, I18n.get("Animation Presets", lang), px, py, getHeaderColor(), false);
        py += 16;
        String[] presets = {"T-Pose", "Idle", "Walk", "Run", "Wave", "Sitting"};

        for (String p : presets) {
            drawButton(graphics, font, "▶ " + I18n.get(p, lang), px, py, contentW, 16, mouseX, mouseY);
            py += 20;
        }
    }

    private void renderViewportTab(GuiGraphics graphics, Font font, int px, int py, int contentW, int mouseX, int mouseY) {
        ModelSettings.Language lang = (modelSettings != null) ? modelSettings.getLanguage() : ModelSettings.Language.EN;
        graphics.drawString(font, I18n.get("Viewport Display", lang), px, py, getHeaderColor(), false);
        py += 16;

        drawToggleButton(graphics, font, I18n.get("Ground Grid", lang), modelSettings.isShowGrid(), px, py, contentW, 16, mouseX, mouseY);
        py += 20;

        drawToggleButton(graphics, font, I18n.get("In-World View", lang), modelSettings.isShowWorldBlocks(), px, py, contentW, 16, mouseX, mouseY);
        py += 20;

        drawToggleButton(graphics, font, I18n.get("3D Rotation Gizmo", lang), modelSettings.isShowGizmo(), px, py, contentW, 16, mouseX, mouseY);
        py += 20;
        String camModeKey = modelSettings.isFreeFlyCamera() ? "Camera: Free Fly (WASD)" : "Camera: Mouse Orbit";

        drawButton(graphics, font, I18n.get(camModeKey, lang), px, py, contentW, 16, mouseX, mouseY);
        py += 20;

        drawButton(graphics, font, "↺ " + I18n.get("Reset Camera", lang), px, py, contentW, 16, mouseX, mouseY);
    }

    private void drawCheckboxRow(GuiGraphics graphics, Font font, String text, boolean checked, int bx, int by, int bw, int bh, int mx, int my) {
        boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        int bg = hov ? getBtnHover() : getBtnBg();
        graphics.fill(bx, by, bx + bw, by + bh, bg);
        graphics.renderOutline(bx, by, bw, bh, hov ? getTheme().getDividerHover() : getTheme().getDivider());
        int boxSize = 10;
        int boxX = bx + 4;
        int boxY = by + (bh - boxSize) / 2;
        graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, checked ? getTheme().getAccent() : 0xFF0F172A);
        graphics.renderOutline(boxX, boxY, boxSize, boxSize, checked ? 0xFF93C5FD : getTheme().getDivider());
        if (checked) {
            graphics.drawString(font, "✓", boxX + 2, boxY + 1, 0xFFFFFFFF, false);
        }
        graphics.drawString(font, text, boxX + boxSize + 6, by + 4, checked ? 0xFFFFFFFF : getLabelColor(), false);
    }

    private void drawSmallBtn(GuiGraphics graphics, Font font, String text, int bx, int by, int bw, int bh, int mx, int my) {
        boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        int bg = hov ? getTheme().getButtonHover() : getTheme().getButtonBg();
        graphics.fill(bx, by, bx + bw, by + bh, bg);
        graphics.renderOutline(bx, by, bw, bh, hov ? getTheme().getAccent() : getTheme().getDivider());
        int tw = font.width(text);
        graphics.drawString(font, text, bx + (bw - tw) / 2, by + 3, hov ? 0xFFFFFFFF : getTheme().getTextColor(), false);
    }

    private void drawModeButton(GuiGraphics graphics, Font font, String text, boolean active, int bx, int by, int bw, int bh, int mx, int my) {
        boolean hovered = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        int bg = active ? getTheme().getAccent() : (hovered ? 0xFF2A374A : 0xFF111827);
        int border = active ? 0xFF93C5FD : (hovered ? getTheme().getDividerHover() : getTheme().getDivider());

        graphics.fill(bx, by, bx + bw, by + bh, bg);
        graphics.renderOutline(bx, by, bw, bh, border);
        int tw = font.width(text);
        int tx = bx + (bw - tw) / 2;
        int ty = by + (bh - 8) / 2;
        graphics.drawString(font, text, tx, ty, active ? 0xFFFFFFFF : (hovered ? 0xFFE2E8F0 : getTheme().getTextMuted()), false);
    }

    private void drawButton(GuiGraphics graphics, Font font, String text, int bx, int by, int bw, int bh, int mx, int my) {
        boolean hovered = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;

        graphics.fill(bx, by, bx + bw, by + bh, hovered ? getBtnHover() : getBtnBg());

        graphics.renderOutline(bx, by, bw, bh, hovered ? getTheme().getAccent() : getTheme().getDivider());
        int tw = font.width(text);
        int tx = bx + (bw - tw) / 2;
        int ty = by + (bh - 8) / 2;

        if (bx + bw - 1 > bx + 1 && by + bh - 1 > by + 1) {
            graphics.enableScissor(bx + 1, by + 1, bx + bw - 1, by + bh - 1);
            graphics.drawString(font, text, tx, ty, hovered ? 0xFFFFFFFF : getTheme().getTextColor(), false);
            graphics.disableScissor();
        } else {
            graphics.drawString(font, text, tx, ty, hovered ? 0xFFFFFFFF : getTheme().getTextColor(), false);
        }
    }

    private void drawToggleButton(GuiGraphics graphics, Font font, String text, boolean active, int bx, int by, int bw, int bh, int mx, int my) {
        boolean hovered = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        int bg = active ? getActiveToggleBg() : (hovered ? getBtnHover() : getBtnBg());
        int border = active ? getTheme().getAccent() : (hovered ? getTheme().getDividerHover() : getTheme().getDivider());

        graphics.fill(bx, by, bx + bw, by + bh, bg);

        graphics.renderOutline(bx, by, bw, bh, border);
        String display = (active ? "✔ " : "✖ ") + text;
        int tw = font.width(display);
        int tx = bx + (bw - tw) / 2;
        int ty = by + (bh - 8) / 2;

        if (bx + bw - 1 > bx + 1 && by + bh - 1 > by + 1) {
            graphics.enableScissor(bx + 1, by + 1, bx + bw - 1, by + bh - 1);
            graphics.drawString(font, display, tx, ty, active ? getTheme().getTextColor() : getTheme().getTextMuted(), false);
            graphics.disableScissor();
        } else {
            graphics.drawString(font, display, tx, ty, active ? getTheme().getTextColor() : getTheme().getTextMuted(), false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (itemPickerDialog.isVisible()) {
            return itemPickerDialog.mouseClicked(mouseX, mouseY, button);
        }

        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false;
        }

        InspectorTab[] tabs = InspectorTab.values();
        int tabW = (width - 8 - 30) / tabs.length;
        int tabH = 18;
        int ty = y + 8;

        for (int i = 0; i < tabs.length; i++) {
            int tx = x + 4 + i * tabW;

            if (mouseX >= tx && mouseX <= tx + tabW - 2 && mouseY >= ty && mouseY <= ty + tabH) {
                this.currentTab = tabs[i];

                return true;
            }
        }

        int px = x + 10;
        int py = y + 36;
        int contentW = width - 20;

        return switch (currentTab) {
            case TRANSFORM -> handleTransformClick(mouseX, mouseY, px, py, contentW);

            case MODEL -> handleModelClick(mouseX, mouseY, px, py, contentW);

            case PRESETS -> handlePresetsClick(mouseX, mouseY, px, py, contentW);
        };
    }

    private void notifyTransformChanged() {
        if (listener != null) {
            listener.onBoneTransformChanged(selectedBone, rotX, rotY, rotZ);
        }
    }

    private void notifyPositionChanged() {
        if (listener != null) {
            listener.onBonePositionChanged(selectedBone, posX, posY, posZ);
        }
    }

    private void notifyEyesChanged() {
        if (listener != null) {
            listener.onBoneTransformChanged("eyes", modelSettings.getLeftEyelid(), modelSettings.getRightEyelid(), 0.0f);
        }
    }

    private void startEnvDrag(int field, double mx, float startVal) {
        if (listener != null) listener.onEditStart();
        this.draggingEnvField = field;
        this.dragStartX = mx;
        this.dragStartAngle = startVal;
    }

    private boolean handleEnvironmentClick(double mx, double my, int px, int py, int contentW) {
        if (selectedEnvironmentKeyframe == null) return false;
        var kf = selectedEnvironmentKeyframe;
        py += 14 + 13;

        if (isInside(mx, my, px, py, contentW, 16)) {
            var modes = net.openstage.core.scene.EnvironmentKeyframe.EnvironmentMode.values();
            int nextM = (kf.mode().ordinal() + 1) % modes.length;
            selectedEnvironmentKeyframe = new net.openstage.core.scene.EnvironmentKeyframe(
                    kf.time(), modes[nextM], kf.anchorX(), kf.anchorY(), kf.anchorZ(), kf.transition(), kf.fadeDuration()
            );
            if (listener != null) listener.onEnvironmentKeyframeChanged(selectedEnvironmentKeyframe);
            return true;
        }
        py += 19;

        if (kf.mode() == net.openstage.core.scene.EnvironmentKeyframe.EnvironmentMode.WORLD) {
            py += 12;
            if (isInside(mx, my, px, py, contentW, 16)) {
                startEnvDrag(0, mx, (float) kf.anchorX());
                return true;
            }
            py += 18;
            if (isInside(mx, my, px, py, contentW, 16)) {
                startEnvDrag(1, mx, (float) kf.anchorY());
                return true;
            }
            py += 18;
            if (isInside(mx, my, px, py, contentW, 16)) {
                startEnvDrag(2, mx, (float) kf.anchorZ());
                return true;
            }
            py += 20;

            if (isInside(mx, my, px, py, contentW, 16)) {
                if (listener != null) listener.onTakePlayerCoordinatesRequested();
                return true;
            }
            py += 20;
        }

        if (isInside(mx, my, px, py, contentW, 16)) {
            var trans = net.openstage.core.scene.EnvironmentKeyframe.EnvironmentTransition.values();
            int nextT = (kf.transition().ordinal() + 1) % trans.length;
            selectedEnvironmentKeyframe = new net.openstage.core.scene.EnvironmentKeyframe(
                    kf.time(), kf.mode(), kf.anchorX(), kf.anchorY(), kf.anchorZ(), trans[nextT], kf.fadeDuration()
            );
            if (listener != null) listener.onEnvironmentKeyframeChanged(selectedEnvironmentKeyframe);
            return true;
        }
        py += 19;

        if (kf.transition() == net.openstage.core.scene.EnvironmentKeyframe.EnvironmentTransition.FADE) {
            if (isInside(mx, my, px, py, contentW, 16)) {
                startEnvDrag(3, mx, kf.fadeDuration());
                return true;
            }
            py += 19;
        }

        if (isInside(mx, my, px, py, contentW, 16)) {
            var toDel = selectedEnvironmentKeyframe;
            selectedEnvironmentKeyframe = null;
            if (listener != null) listener.onDeleteEnvironmentKeyframeRequested(toDel);
            return true;
        }

        return false;
    }

    private boolean handleTransformClick(double mx, double my, int px, int py, int contentW) {
        if (selectedEnvironmentKeyframe != null) {
            return handleEnvironmentClick(mx, my, px, py, contentW);
        }
        if (selectedTextCard != null) {
            py += 15;
            if (isInside(mx, my, px, py, contentW, 16)) {
                if (listener != null) listener.onEditTextCardRequested(selectedTextCard);
                return true;
            }
            py += 19 + 15;

            if (isInside(mx, my, px, py, contentW, 16)) {
                TextCard.PositionPreset[] p = TextCard.PositionPreset.values();
                selectedTextCard.setPosition(p[(selectedTextCard.getPosition().ordinal() + 1) % p.length]);
                return true;
            }
            py += 19;

            if (isInside(mx, my, px, py, contentW, 16)) {
                TextCard.AnimationEffect[] eff = TextCard.AnimationEffect.values();
                selectedTextCard.setEffect(eff[(selectedTextCard.getEffect().ordinal() + 1) % eff.length]);
                return true;
            }
            py += 20 + 12;

            if (isInside(mx, my, px, py, contentW, 16)) {
                startCardDrag(0, mx, selectedTextCard.getRotX());
                return true;
            }
            py += 18;
            if (isInside(mx, my, px, py, contentW, 16)) {
                startCardDrag(1, mx, selectedTextCard.getRotY());
                return true;
            }
            py += 18;
            if (isInside(mx, my, px, py, contentW, 16)) {
                startCardDrag(2, mx, selectedTextCard.getRotZ());
                return true;
            }
            py += 22 + 12;

            if (isInside(mx, my, px, py, contentW, 16)) {
                startCardDrag(3, mx, selectedTextCard.getOffsetX());
                return true;
            }
            py += 18;
            if (isInside(mx, my, px, py, contentW, 16)) {
                startCardDrag(4, mx, selectedTextCard.getOffsetY());
                return true;
            }
            py += 18;
            if (isInside(mx, my, px, py, contentW, 16)) {
                startCardDrag(5, mx, selectedTextCard.getOffsetZ());
                return true;
            }
            py += 18;
            if (isInside(mx, my, px, py, contentW, 16)) {
                startCardDrag(6, mx, selectedTextCard.getScaleX());
                return true;
            }
            py += 22 + 12;

            int pbtnW = (contentW - 8) / 5;
            if (isInside(mx, my, px, py, pbtnW, 14)) {
                selectedTextCard.setTextColor(0xFFFFFFFF);
                selectedTextCard.setBackgroundColor(0x88000000);
                return true;
            }
            if (isInside(mx, my, px + pbtnW + 2, py, pbtnW, 14)) {
                selectedTextCard.setTextColor(0xFF00F0FF);
                selectedTextCard.setBackgroundColor(0xAA051020);
                return true;
            }
            if (isInside(mx, my, px + (pbtnW + 2) * 2, py, pbtnW, 14)) {
                selectedTextCard.setTextColor(0xFFFF007F);
                selectedTextCard.setBackgroundColor(0xAA200010);
                return true;
            }
            if (isInside(mx, my, px + (pbtnW + 2) * 3, py, pbtnW, 14)) {
                selectedTextCard.setTextColor(0xFFF59E0B);
                selectedTextCard.setBackgroundColor(0xAA201500);
                return true;
            }
            if (isInside(mx, my, px + (pbtnW + 2) * 4, py, pbtnW, 14)) {
                selectedTextCard.setTextColor(0xFF22C55E);
                selectedTextCard.setBackgroundColor(0xAA002005);
                return true;
            }
            py += 20;

            if (isInside(mx, my, px, py, contentW, 16)) {
                selectedTextCard.setRotX(0.0f);
                selectedTextCard.setRotY(0.0f);
                selectedTextCard.setRotZ(0.0f);
                return true;
            }
            py += 19;

            if (isInside(mx, my, px, py, contentW, 16)) {
                selectedTextCard.setDuration(selectedTextCard.getDuration() + 1.0f);
                return true;
            }
            py += 19;

            if (isInside(mx, my, px, py, contentW, 16)) {
                TextCard toDelete = selectedTextCard;
                this.selectedTextCard = null;
                if (listener != null) listener.onDeleteTextCardRequested(toDelete);
                return true;
            }
        } else if ("particles".equals(selectedBone)) {
            return handleParticlesSectionClick(px, py, contentW, mx, my);
        } else if ("camera".equals(selectedBone)) {
            return handleCameraSectionClick(px, py, contentW, mx, my);
        } else if ("eyes".equals(selectedBone)) {
            return handleEyesClick(mx, my, px, py, contentW);
        } else {
            py += 14;
            int modeW = (contentW - 4) / 2;

            if (isInside(mx, my, px, py, modeW, 16)) {
                modelSettings.setGizmoMode(ModelSettings.GizmoMode.ROTATE);

                return true;
            }

            if (isInside(mx, my, px + modeW + 4, py, modeW, 16)) {
                modelSettings.setGizmoMode(ModelSettings.GizmoMode.TRANSLATE);

                return true;
            }

            py += 20 + 12;
            boolean isRotate = (modelSettings.getGizmoMode() == ModelSettings.GizmoMode.ROTATE);

            if (isInside(mx, my, px, py, contentW, 16)) {
                if (listener != null) listener.onEditStart(); draggingAxis = 0; dragStartX = mx; dragStartAngle = isRotate ? rotX : posX; return true;
            }

            py += 19;

            if (isInside(mx, my, px, py, contentW, 16)) {
                if (listener != null) listener.onEditStart(); draggingAxis = 1; dragStartX = mx; dragStartAngle = isRotate ? rotY : posY; return true;
            }

            py += 19;

            if (isInside(mx, my, px, py, contentW, 16)) {
                if (listener != null) listener.onEditStart(); draggingAxis = 2; dragStartX = mx; dragStartAngle = isRotate ? rotZ : posZ; return true;
            }

            py += 19;

            if (isInside(mx, my, px, py, contentW, 15)) {
                if (isRotate) {
                    rotX = 0; rotY = 0; rotZ = 0;

                    notifyTransformChanged();
                } else {
                    posX = 0; posY = 0; posZ = 0;

                    notifyPositionChanged();
                }

                return true;
            }

            py += 19;

            if (isInside(mx, my, px, py, contentW, 15)) {
                easingIndex = (easingIndex + 1) % EASINGS.length;

                return true;
            }

            py += 20;

            if (isInside(mx, my, px, py, contentW, 16)) {
                if (listener != null) listener.onAddTextCardRequested();

                return true;
            }

            py += 20;

            if (isInside(mx, my, px, py, contentW, 16)) {
                if (listener != null) listener.onAddParticlesRequested();

                return true;
            }
        }

        return false;
    }

    private boolean handleCameraSectionClick(int px, int py, int contentW, double mx, double my) {
        py += 14;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onCopyCameraFromEditor();

            return true;
        }

        py += 19 + 12;

        if (isInside(mx, my, px, py, contentW, 16)) { startCamDrag(0, mx, camX); return true; }

        py += 18;

        if (isInside(mx, my, px, py, contentW, 16)) { startCamDrag(1, mx, camY); return true; }

        py += 18;

        if (isInside(mx, my, px, py, contentW, 16)) { startCamDrag(2, mx, camZ); return true; }

        py += 20 + 12;

        if (isInside(mx, my, px, py, contentW, 16)) { startCamDrag(3, mx, camYaw); return true; }

        py += 18;

        if (isInside(mx, my, px, py, contentW, 16)) { startCamDrag(4, mx, camPitch); return true; }

        py += 18;

        if (isInside(mx, my, px, py, contentW, 16)) { startCamDrag(5, mx, camRoll); return true; }

        py += 20 + 12;

        if (isInside(mx, my, px, py, contentW, 16)) { startCamDrag(6, mx, camFov); return true; }

        py += 20 + 12;
        int modeBtnY = py;

        if (cameraModeDropdownOpen) {
            int dropY = modeBtnY + 16;

            if (isInside(mx, my, px, dropY, contentW, 17)) {
                setCameraSharp(false);
                cameraModeDropdownOpen = false;

                if (listener != null) listener.onCameraModeChanged(false, getCameraHandles());

                return true;
            } else if (isInside(mx, my, px, dropY + 17, contentW, 17)) {
                setCameraSharp(true);
                cameraModeDropdownOpen = false;

                if (listener != null) listener.onCameraModeChanged(true, null);

                return true;
            } else {
                cameraModeDropdownOpen = false;

                return true;
            }
        }

        if (isInside(mx, my, px, py, contentW, 16)) {
            cameraModeDropdownOpen = !cameraModeDropdownOpen;

            return true;
        }

        py += 18;

        if (!cameraSharp) {
            if (isInside(mx, my, px, py, contentW, 15)) {
                if (listener != null) listener.onOpenGraphEditor();

                return true;
            }

            py += 17;
        }

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onAddCameraKeyframe(camX, camY, camZ, camYaw, camPitch, camRoll, camFov);

            return true;
        }

        py += 18;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onDeleteKeyframeRequested("camera");

            return true;
        }

        return false;
    }

    private void startPartDrag(int field, double mx, float startVal) {
        if (listener != null) listener.onEditStart();
        this.draggingPartField = field;
        this.dragStartX = mx;
        this.dragStartAngle = startVal;
    }

    private void notifyParticleChanged() {
        if (listener != null) {
            listener.onParticleChanged(partX, partY, partZ, partYaw, partPitch, partRoll, partParticleId, partEmitterType, partCount, partSpeed, partSpread, partDuration, partColorRgb, partSize);
        }
    }

    private boolean handleParticlesSectionClick(int px, int py, int contentW, double mx, double my) {
        py += 14;
        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onParticleTestFire();
            return true;
        }
        py += 20;
        py += 12;
        if (isInside(mx, my, px, py, contentW, 16)) { startPartDrag(0, mx, partX); return true; }
        py += 18;
        if (isInside(mx, my, px, py, contentW, 16)) { startPartDrag(1, mx, partY); return true; }
        py += 18;
        if (isInside(mx, my, px, py, contentW, 16)) { startPartDrag(2, mx, partZ); return true; }
        py += 20;
        py += 12;
        if (isInside(mx, my, px, py, contentW, 16)) { startPartDrag(3, mx, partYaw); return true; }
        py += 18;
        if (isInside(mx, my, px, py, contentW, 16)) { startPartDrag(4, mx, partPitch); return true; }
        py += 18;
        if (isInside(mx, my, px, py, contentW, 16)) { startPartDrag(5, mx, partRoll); return true; }
        py += 20;
        py += 12;
        if (isInside(mx, my, px, py, contentW, 16)) {
            net.openstage.core.particle.ParticleEmitterType[] shapes = net.openstage.core.particle.ParticleEmitterType.values();
            partEmitterType = shapes[(partEmitterType.ordinal() + 1) % shapes.length];
            notifyParticleChanged();
            return true;
        }
        py += 19;
        py += 12;
        if (isInside(mx, my, px, py, contentW, 16)) {
            var list = net.openstage.client.particle.ParticlePlaybackService.getInstance().getAvailableParticleIds();
            int curIdx = list.indexOf(partParticleId);
            partParticleId = list.get((curIdx + 1) % list.size());
            notifyParticleChanged();
            return true;
        }
        py += 19;
        py += 12;
        if (isInside(mx, my, px, py, contentW, 16)) { startPartDrag(6, mx, (float) partCount); return true; }
        py += 18;
        if (isInside(mx, my, px, py, contentW, 16)) { startPartDrag(7, mx, partSpeed); return true; }
        py += 18;
        if (isInside(mx, my, px, py, contentW, 16)) { startPartDrag(8, mx, partSpread); return true; }
        py += 18;
        if (isInside(mx, my, px, py, contentW, 16)) { startPartDrag(9, mx, partDuration); return true; }
        py += 22;

        if (isInside(mx, my, px, py, contentW, 18)) {
            if (listener != null) listener.onAddParticleKeyframe(partX, partY, partZ, partYaw, partPitch, partRoll, partParticleId, partEmitterType, partCount, partSpeed, partSpread, partDuration, partColorRgb, partSize);
            return true;
        }
        py += 21;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onOpenGraphEditor();
            return true;
        }
        py += 19;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onDeleteKeyframeRequested("particles");
            return true;
        }
        py += 19;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onDeleteParticleEmitterRequested();
            return true;
        }

        return false;
    }

    private void startCamDrag(int field, double mx, float startVal) {
        if (listener != null) listener.onEditStart();
        this.draggingCamField = field;
        this.dragStartX = mx;
        this.dragStartAngle = startVal;
    }

    private boolean handleEyesClick(double mx, double my, int px, int py, int contentW) {
        py += 15 + 13;
        int qw = (contentW - 8) / 4;

        if (isInside(mx, my, px, py, qw, 14)) { modelSettings.setPupilX(-1.0f); notifyEyesChanged(); return true; }

        if (isInside(mx, my, px + qw + 2, py, qw, 14)) { modelSettings.setPupilY(1.0f); notifyEyesChanged(); return true; }

        if (isInside(mx, my, px + 2 * (qw + 2), py, qw, 14)) { modelSettings.setPupilY(-1.0f); notifyEyesChanged(); return true; }

        if (isInside(mx, my, px + 3 * (qw + 2), py, qw, 14)) { modelSettings.setPupilX(1.0f); notifyEyesChanged(); return true; }

        py += 16;

        if (isInside(mx, my, px, py, contentW, 14)) {
            modelSettings.setPupilX(0.0f);

            modelSettings.setPupilY(0.0f);

            notifyEyesChanged();

            return true;
        }

        py += 18 + 13;
        int thw = (contentW - 6) / 3;

        if (isInside(mx, my, px, py, thw, 14)) {
            modelSettings.setLeftEyelid(1.0f);

            modelSettings.setRightEyelid(0.0f);

            notifyEyesChanged();

            return true;
        }

        if (isInside(mx, my, px + thw + 3, py, thw, 14)) {
            modelSettings.setLeftEyelid(0.0f);

            modelSettings.setRightEyelid(1.0f);

            notifyEyesChanged();

            return true;
        }

        if (isInside(mx, my, px + 2 * (thw + 3), py, thw, 14)) {
            modelSettings.setLeftEyelid(1.0f);

            modelSettings.setRightEyelid(1.0f);

            notifyEyesChanged();

            return true;
        }

        py += 16;

        if (isInside(mx, my, px, py, contentW, 14)) {
            modelSettings.setLeftEyelid(0.0f);

            modelSettings.setRightEyelid(0.0f);

            notifyEyesChanged();

            return true;
        }

        py += 18;

        if (isInside(mx, my, px, py, contentW, 15)) {
            easingIndex = (easingIndex + 1) % EASINGS.length;

            return true;
        }

        py += 18;

        if (isInside(mx, my, px, py, contentW, 16)) {
            modelSettings.setAnimatedEyes(true);

            if (listener != null) {
                listener.onAddKeyframeRequested(
                        "eyes",

                        new float[]{modelSettings.getLeftEyelid(), modelSettings.getRightEyelid(), 0.0f},

                        new float[]{modelSettings.getPupilX(), modelSettings.getPupilY(), 0.0f},

                        new float[]{1.0f, 1.0f, 1.0f},

                        EASINGS[easingIndex]
                );
            }

            return true;
        }

        py += 19;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onDeleteKeyframeRequested("eyes");

            return true;
        }

        return false;
    }

    private boolean handleModelClick(double mx, double my, int px, int py, int contentW) {
        py += 16;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (!slimModel && !modelSettings.isBendableLimbs()) {
                modelSettings.setBendableLimbs(true);
            } else if (!slimModel && modelSettings.isBendableLimbs()) {
                slimModel = true;

                modelSettings.setBendableLimbs(false);
            } else if (slimModel && !modelSettings.isBendableLimbs()) {
                modelSettings.setBendableLimbs(true);
            } else {
                slimModel = false;

                modelSettings.setBendableLimbs(false);
            }

            String skin = getCurrentSkin();

            if (listener != null) {
                listener.onModelChanged(slimModel);

                listener.onSkinChanged(skin, slimModel);
            }

            return true;
        }

        py += 20;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (onOpenSkinPicker != null) {
                onOpenSkinPicker.run();
            } else {
                List<String> list = SkinManager.getInstance().getAvailableSkinNames();
                if (!list.isEmpty()) {
                    currentSkinIndex = (currentSkinIndex + 1) % list.size();
                    String skin = list.get(currentSkinIndex);
                    if (listener != null) listener.onSkinChanged(skin, slimModel);
                }
            }
            return true;
        }

        py += 20;

        if (isInside(mx, my, px, py, contentW, 15)) {
            SkinManager.getInstance().openSkinsFolder();
            return true;
        }

        py += 19;

        int uvBoxSize = 54;
        int uvX = px + contentW - uvBoxSize;
        int leftW = (uvX - 6) - px;
        int itemBtnY = py + 12;
        int itemBtnH = 18;

        if (isInside(mx, my, px, itemBtnY, leftW, itemBtnH)) {
            choosingRightHand = true;
            itemPickerDialog.open();
            return true;
        }

        itemBtnY += 21;

        if (isInside(mx, my, px, itemBtnY, leftW, itemBtnH)) {
            choosingRightHand = false;
            itemPickerDialog.open();
            return true;
        }

        int sectionH = Math.max(itemBtnY + itemBtnH, py + uvBoxSize + 12) - py;
        py += sectionH + 8;

        py += 14;

        if (isInside(mx, my, px, py, contentW, 16)) {
            modelSettings.setOnionSkin(!modelSettings.isOnionSkin());

            return true;
        }
        py += 22;

        if (isInside(mx, my, px, py, contentW, 16)) {
            layersDropdownOpen = !layersDropdownOpen;
            return true;
        }
        py += 19;

        if (layersDropdownOpen) {
            int itemY = py + 3;
            if (isInside(mx, my, px + 4, itemY, contentW - 8, 15)) { modelSettings.setShowHat(!modelSettings.isShowHat()); return true; } itemY += 17;
            if (isInside(mx, my, px + 4, itemY, contentW - 8, 15)) { modelSettings.setShowJacket(!modelSettings.isShowJacket()); return true; } itemY += 17;
            if (isInside(mx, my, px + 4, itemY, contentW - 8, 15)) { modelSettings.setShowRightSleeve(!modelSettings.isShowRightSleeve()); return true; } itemY += 17;
            if (isInside(mx, my, px + 4, itemY, contentW - 8, 15)) { modelSettings.setShowLeftSleeve(!modelSettings.isShowLeftSleeve()); return true; } itemY += 17;
            if (isInside(mx, my, px + 4, itemY, contentW - 8, 15)) { modelSettings.setShowRightPants(!modelSettings.isShowRightPants()); return true; } itemY += 17;
            if (isInside(mx, my, px + 4, itemY, contentW - 8, 15)) { modelSettings.setShowLeftPants(!modelSettings.isShowLeftPants()); return true; } itemY += 17;

            int halfDropW = (contentW - 12) / 2;
            if (isInside(mx, my, px + 4, itemY, halfDropW, 14)) {
                modelSettings.setShowHat(true); modelSettings.setShowJacket(true);
                modelSettings.setShowRightSleeve(true); modelSettings.setShowLeftSleeve(true);
                modelSettings.setShowRightPants(true); modelSettings.setShowLeftPants(true);
                return true;
            }
            if (isInside(mx, my, px + 8 + halfDropW, itemY, halfDropW, 14)) {
                modelSettings.setShowHat(false); modelSettings.setShowJacket(false);
                modelSettings.setShowRightSleeve(false); modelSettings.setShowLeftSleeve(false);
                modelSettings.setShowRightPants(false); modelSettings.setShowLeftPants(false);
                return true;
            }

            int dropH = 6 * 17 + 24;
            py += dropH + 4;
        }

        return false;
    }

    private boolean handlePresetsClick(double mx, double my, int px, int py, int contentW) {
        py += 16;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onPresetApplied(PresetLibrary.createTPose());

            return true;
        }

        py += 20;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onPresetApplied(PresetLibrary.createIdle());

            return true;
        }

        py += 20;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onPresetApplied(PresetLibrary.createWalk());

            return true;
        }

        py += 20;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onPresetApplied(PresetLibrary.createRun());

            return true;
        }

        py += 20;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onPresetApplied(PresetLibrary.createWave());

            return true;
        }

        py += 20;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onPresetApplied(PresetLibrary.createSitting());

            return true;
        }

        return false;
    }

    private boolean handleViewportClick(double mx, double my, int px, int py, int contentW) {
        py += 16;

        if (isInside(mx, my, px, py, contentW, 16)) {
            modelSettings.setShowGrid(!modelSettings.isShowGrid());

            return true;
        }

        py += 20;

        if (isInside(mx, my, px, py, contentW, 16)) {
            modelSettings.setShowWorldBlocks(!modelSettings.isShowWorldBlocks());

            return true;
        }

        py += 20;

        if (isInside(mx, my, px, py, contentW, 16)) {
            modelSettings.setShowGizmo(!modelSettings.isShowGizmo());

            return true;
        }

        py += 20;

        if (isInside(mx, my, px, py, contentW, 16)) {
            modelSettings.setFreeFlyCamera(!modelSettings.isFreeFlyCamera());

            return true;
        }

        py += 20;

        if (isInside(mx, my, px, py, contentW, 16)) {
            if (listener != null) listener.onResetCameraRequested();

            return true;
        }

        return false;
    }

    public String getCurrentSkin() {
        List<String> list = SkinManager.getInstance().getAvailableSkinNames();

        if (currentSkinIndex >= 0 && currentSkinIndex < list.size()) {
            return list.get(currentSkinIndex);
        }

        return "default";
    }

    public boolean isSlimModel() {
        return slimModel;
    }

    public void setSlimModel(boolean slim) {
        this.slimModel = slim;
    }

    public void setCurrentSkin(String skinName) {
        if (skinName == null) return;
        java.util.List<String> list = SkinManager.getInstance().getAvailableSkinNames();
        int idx = list.indexOf(skinName.toLowerCase(java.util.Locale.ROOT));

        if (idx >= 0) {
            this.currentSkinIndex = idx;
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingEnvField != -1 && selectedEnvironmentKeyframe != null) {
            float delta = (float) (mouseX - dragStartX);
            float scale = (draggingEnvField < 3) ? 1.0f : 0.05f;
            float newVal = dragStartAngle + delta * scale;
            var kf = selectedEnvironmentKeyframe;
            int ax = kf.anchorX(), ay = kf.anchorY(), az = kf.anchorZ();
            float fade = kf.fadeDuration();
            switch (draggingEnvField) {
                case 0 -> ax = (int) newVal;
                case 1 -> ay = Math.clamp((int) newVal, -64, 320);
                case 2 -> az = (int) newVal;
                case 3 -> fade = Math.clamp(newVal, 0.1f, 5.0f);
            }
            selectedEnvironmentKeyframe = new net.openstage.core.scene.EnvironmentKeyframe(
                    kf.time(), kf.mode(), ax, ay, az, kf.transition(), fade
            );
            if (listener != null) listener.onEnvironmentKeyframeChanged(selectedEnvironmentKeyframe);
            return true;
        }
        if (draggingPartField != -1) {
            float delta = (float) (mouseX - dragStartX);
            float scale = (draggingPartField < 3) ? 0.05f : ((draggingPartField < 6) ? 0.5f : ((draggingPartField == 6) ? 1.0f : 0.05f));
            float newVal = dragStartAngle + delta * scale;
            switch (draggingPartField) {
                case 0 -> partX = newVal;
                case 1 -> partY = newVal;
                case 2 -> partZ = newVal;
                case 3 -> partYaw = (newVal % 360.0f + 360.0f) % 360.0f;
                case 4 -> partPitch = Math.clamp(newVal, -90.0f, 90.0f);
                case 5 -> partRoll = (newVal % 360.0f + 360.0f) % 360.0f;
                case 6 -> partCount = Math.clamp((int) newVal, 1, 300);
                case 7 -> partSpeed = Math.clamp(newVal, 0.0f, 10.0f);
                case 8 -> partSpread = Math.clamp(newVal, 0.0f, 3.0f);
                case 9 -> partDuration = Math.max(0.0f, newVal);
            }
            notifyParticleChanged();
            return true;
        }

        if (draggingCamField != -1) {
            float delta = (float) (mouseX - dragStartX);
            float scale = (draggingCamField < 3) ? 0.05f : ((draggingCamField == 6) ? 0.2f : 0.5f);
            float val = dragStartAngle + delta * scale;

            switch (draggingCamField) {
                case 0 -> camX = val;
                case 1 -> camY = val;
                case 2 -> camZ = val;
                case 3 -> camYaw = val;
                case 4 -> camPitch = Math.clamp(val, -90.0f, 90.0f);
                case 5 -> camRoll = val;
                case 6 -> camFov = Math.clamp(val, 10.0f, 120.0f);
            }

            if (listener != null) {
                listener.onCameraChanged(camX, camY, camZ, camYaw, camPitch, camRoll, camFov);
            }

            return true;
        }

        if (draggingCardField != -1 && selectedTextCard != null) {
            float delta = (float) (mouseX - dragStartX);
            float scale = (draggingCardField < 3) ? 0.5f : ((draggingCardField == 6) ? 0.02f : 1.0f);
            float newVal = dragStartAngle + delta * scale;
            switch (draggingCardField) {
                case 0 -> selectedTextCard.setRotX((newVal % 360.0f + 360.0f) % 360.0f);
                case 1 -> selectedTextCard.setRotY((newVal % 360.0f + 360.0f) % 360.0f);
                case 2 -> selectedTextCard.setRotZ((newVal % 360.0f + 360.0f) % 360.0f);
                case 3 -> selectedTextCard.setOffsetX(newVal);
                case 4 -> selectedTextCard.setOffsetY(newVal);
                case 5 -> selectedTextCard.setOffsetZ(newVal);
                case 6 -> {
                    float s = Math.max(0.1f, Math.min(5.0f, newVal));
                    selectedTextCard.setScaleX(s);
                    selectedTextCard.setScaleY(s);
                }
            }
            return true;
        }

        if (draggingAxis == 3) {
            float delta = (float) ((mouseX - dragStartX) * 0.012);
            float newVal = Math.max(0.1f, Math.min(5.0f, dragStartAngle + delta));
            newVal = Math.round(newVal * 100.0f) / 100.0f;
            modelSettings.setScale(newVal);
            return true;
        }

        if (draggingAxis >= 0) {
            double delta = mouseX - dragStartX;
            boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
            boolean ctrl = net.minecraft.client.gui.screens.Screen.hasControlDown();
            boolean isRotate = (modelSettings.getGizmoMode() == ModelSettings.GizmoMode.ROTATE);

            if (isRotate) {
                float speed = shift ? 0.2f : 1.0f;
                float newVal = dragStartAngle + (float) (delta * speed);

                if (ctrl) {
                    newVal = Math.round(newVal / 15.0f) * 15.0f;
                }

                newVal = clamp180(newVal);

                if (draggingAxis == 0) rotX = newVal;

                else if (draggingAxis == 1) rotY = newVal;

                else if (draggingAxis == 2) rotZ = newVal;

                notifyTransformChanged();
            } else {
                float speed = shift ? 0.002f : 0.01f;
                float newVal = dragStartAngle + (float) (delta * speed);

                if (ctrl) {
                    newVal = Math.round(newVal / 0.1f) * 0.1f;
                }

                if (draggingAxis == 0) posX = newVal;

                else if (draggingAxis == 1) posY = newVal;

                else if (draggingAxis == 2) posZ = newVal;

                notifyPositionChanged();
            }

            return true;
        }

        return false;
    }

    private void startCardDrag(int field, double mx, float startVal) {
        if (listener != null) listener.onEditStart();
        this.draggingCardField = field;
        this.dragStartX = mx;
        this.dragStartAngle = startVal;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean hadDrag = (draggingPartField != -1 || draggingCamField != -1 || draggingCardField != -1 || draggingEnvField != -1 || draggingAxis >= 0);
        draggingPartField = -1;
        draggingCamField = -1;
        draggingCardField = -1;
        draggingEnvField = -1;
        draggingAxis = -1;
        if (hadDrag) {
            if (listener != null) listener.onEditEnd();
            return true;
        }
        return false;
    }
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (itemPickerDialog.isVisible()) {
            return itemPickerDialog.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false;
        }

        if (currentTab == InspectorTab.TRANSFORM && selectedTextCard == null && !"eyes".equals(selectedBone) && !"camera".equals(selectedBone) && !"particles".equals(selectedBone)) {
            int px = x + 10;
            int py = y + 36 + 15 + 12;
            int contentW = width - 20;
            float step = net.minecraft.client.gui.screens.Screen.hasShiftDown() ? 15.0f : 1.0f;
            float delta = (float) (verticalAmount * step);

            if (isInside(mouseX, mouseY, px, py, contentW, 16)) {
                rotX = clamp180(rotX + delta); notifyTransformChanged(); return true;
            }

            py += 19;

            if (isInside(mouseX, mouseY, px, py, contentW, 16)) {
                rotY = clamp180(rotY + delta); notifyTransformChanged(); return true;
            }

            py += 19;

            if (isInside(mouseX, mouseY, px, py, contentW, 16)) {
                rotZ = clamp180(rotZ + delta); notifyTransformChanged(); return true;
            }
        }

        return false;
    }

    private static float clamp180(float val) {
        while (val > 180.0f) val -= 360.0f;

        while (val < -180.0f) val += 360.0f;

        return val;
    }

    private boolean isInside(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    private void renderBoneUvHighlight(GuiGraphics graphics, int bx, int by, int boxSize) {
        if (selectedBone == null) return;
        float scale = (float) boxSize / 64.0f;
        int u = 0, v = 0, w = 0, h = 0;

        switch (selectedBone) {
            case "head" -> { u = 0; v = 0; w = 32; h = 16; }

            case "body" -> { u = 16; v = 16; w = 24; h = 16; }

            case "right_arm" -> { u = 40; v = 16; w = 16; h = 16; }

            case "left_arm" -> { u = 32; v = 48; w = 16; h = 16; }

            case "right_leg" -> { u = 0; v = 16; w = 16; h = 16; }

            case "left_leg" -> { u = 16; v = 48; w = 16; h = 16; }

            default -> { return; }
        }

        int hx = bx + (int) (u * scale);
        int hy = by + (int) (v * scale);
        int hw = Math.max(2, (int) (w * scale));
        int hh = Math.max(2, (int) (h * scale));

        graphics.renderOutline(hx, hy, hw, hh, 0xFF00E5FF);
    }

    public boolean charTyped(char c, int modifiers) {
        if (itemPickerDialog.isVisible()) {
            return itemPickerDialog.charTyped(c, modifiers);
        }

        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (itemPickerDialog.isVisible()) {
            return itemPickerDialog.keyPressed(keyCode, scanCode, modifiers);
        }

        return false;
    }

    private void drawItemSlotButton(GuiGraphics graphics, Font font, ItemStack item, String labelPrefix, String emptyLabel, int bx, int by, int bw, int bh, int mx, int my) {
        boolean hovered = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        graphics.fill(bx, by, bx + bw, by + bh, hovered ? getBtnHover() : getBtnBg());
        graphics.renderOutline(bx, by, bw, bh, hovered ? getTheme().getAccent() : getTheme().getDivider());
        int ty = by + (bh - 8) / 2;
        if (!item.isEmpty()) {
            graphics.renderItem(item, bx + 2, by + (bh - 16) / 2);
            String fullText = labelPrefix + item.getHoverName().getString();
            int textX = bx + 22;
            int maxTextW = Math.max(10, bw - 24);
            String text = font.plainSubstrByWidth(fullText, maxTextW);
            if (bx + bw - 1 > textX && by + bh - 1 > by + 1) {
                graphics.enableScissor(textX, by + 1, bx + bw - 1, by + bh - 1);
                graphics.drawString(font, text, textX, ty, hovered ? 0xFFFFFFFF : getTheme().getTextColor(), false);
                graphics.disableScissor();
            } else {
                graphics.drawString(font, text, textX, ty, hovered ? 0xFFFFFFFF : getTheme().getTextColor(), false);
            }
        } else {
            int tw = font.width(emptyLabel);
            int tx = bx + (bw - tw) / 2;
            graphics.drawString(font, emptyLabel, tx, ty, hovered ? 0xFF94A3B8 : getTheme().getTextMuted(), false);
        }
    }

}
