package net.openstage.client.model;

import net.minecraft.world.item.ItemStack;
import net.openstage.client.theme.EditorTheme;
import org.lwjgl.glfw.GLFW;

public class ModelSettings {
    public enum CameraMode {
        ORBIT("Orbit (Mouse Drag)"),
        FREE_FLY_WASD("Free Fly (WASD + Look)"),
        BLENDER_MMB("Blender Style (MMB Orbit/Pan)");
        private final String label;

        CameraMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum BackgroundMode {
        DEFAULT("По умолчанию", 0.0f, 0.0f, 0.0f),
        CHROMA_GREEN("Зелёный (Хромакей)", 0.0f, 1.0f, 0.0f),
        CHROMA_BLUE("Синий (Хромакей)", 0.0f, 0.2f, 1.0f);
        private final String label;
        private final float r, g, b;

        BackgroundMode(String label, float r, float g, float b) {
            this.label = label;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public String getLabel() { return label; }
        public float getR() { return r; }
        public float getG() { return g; }
        public float getB() { return b; }
    }

    private float scale = 1.0f;
    private float rootYaw = 0.0f;
    private boolean showHat = true;
    private boolean showJacket = true;
    private boolean showRightSleeve = true;
    private boolean showLeftSleeve = true;
    private boolean showRightPants = true;
    private boolean showLeftPants = true;
    private BackgroundMode backgroundMode = BackgroundMode.DEFAULT;
    private boolean showGrid = true;
    private boolean cinematicCamera = false;
    private boolean animatedEyes = false;
    private boolean bendableLimbs = false;
    private boolean showGizmo = true;
    private CameraMode cameraMode = CameraMode.ORBIT;
    private EditorTheme theme = EditorTheme.DARK_SLATE;
    private boolean showWorldBlocks = false;
    private boolean cameraViewMode = false;
    private final java.util.Set<String> ghostedBones = new java.util.HashSet<>();
    private int keyPlayPause = GLFW.GLFW_KEY_SPACE;
    private int keyAddKeyframe = GLFW.GLFW_KEY_K;
    private int keyDelete = GLFW.GLFW_KEY_DELETE;
    private int keyToggleGraph = GLFW.GLFW_KEY_G;
    private int keyCameraView = GLFW.GLFW_KEY_KP_0;
    private float pupilX = 0.0f;
    private float pupilY = 0.0f;
    private float leftEyelid = 0.0f;
    private float rightEyelid = 0.0f;
    private int eyeColor = 0xFF2563EB;
    private int scleraColor = 0xFFFFFFFF;

    public static final int[] EYE_COLORS = {
            0xFF2563EB,
            0xFF16A34A,
            0xFF78350F,
            0xFFDC2626,
            0xFF9333EA,
            0xFF06B6D4,
            0xFF1E293B
    };
    public static final String[] EYE_COLOR_NAMES = {
            "Blue", "Green", "Brown", "Red", "Purple", "Cyan", "Black"
    };

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.2f, Math.min(3.0f, scale));
    }

    public float getRootYaw() {
        return rootYaw;
    }

    public void setRootYaw(float rootYaw) {
        this.rootYaw = rootYaw % 360.0f;
    }

    public boolean isShowHat() { return showHat; }
    public void setShowHat(boolean showHat) { this.showHat = showHat; }

    public boolean isShowJacket() { return showJacket; }
    public void setShowJacket(boolean showJacket) { this.showJacket = showJacket; }

    public boolean isShowRightSleeve() { return showRightSleeve; }
    public void setShowRightSleeve(boolean showRightSleeve) { this.showRightSleeve = showRightSleeve; }

    public boolean isShowLeftSleeve() { return showLeftSleeve; }
    public void setShowLeftSleeve(boolean showLeftSleeve) { this.showLeftSleeve = showLeftSleeve; }

    public boolean isShowRightPants() { return showRightPants; }
    public void setShowRightPants(boolean showRightPants) { this.showRightPants = showRightPants; }

    public boolean isShowLeftPants() { return showLeftPants; }
    public void setShowLeftPants(boolean showLeftPants) { this.showLeftPants = showLeftPants; }

    public java.util.Set<String> getGhostedBones() { return ghostedBones; }
    public boolean isBoneGhosted(String bone) { return ghostedBones.contains(bone); }
    public void setBoneGhosted(String bone, boolean ghosted) {
        if (ghosted) ghostedBones.add(bone);
        else ghostedBones.remove(bone);
    }
    public void toggleBoneGhosted(String bone) {
        if (ghostedBones.contains(bone)) ghostedBones.remove(bone);
        else ghostedBones.add(bone);
    }
    public BackgroundMode getBackgroundMode() { return backgroundMode; }
    public void setBackgroundMode(BackgroundMode backgroundMode) {
        this.backgroundMode = backgroundMode != null ? backgroundMode : BackgroundMode.DEFAULT;
    }

    public boolean isShowGrid() { return showGrid; }
    public void setShowGrid(boolean showGrid) { this.showGrid = showGrid; }

    public boolean isCinematicCamera() { return cinematicCamera; }
    public void setCinematicCamera(boolean cinematicCamera) { this.cinematicCamera = cinematicCamera; }

    public boolean isAnimatedEyes() { return false; }
    public void setAnimatedEyes(boolean animatedEyes) { this.animatedEyes = false; }

    public float getPupilX() { return pupilX; }
    public void setPupilX(float pupilX) { this.pupilX = Math.max(-1.0f, Math.min(1.0f, pupilX)); }

    public float getPupilY() { return pupilY; }
    public void setPupilY(float pupilY) { this.pupilY = Math.max(-1.0f, Math.min(1.0f, pupilY)); }

    public float getLeftEyelid() { return leftEyelid; }
    public void setLeftEyelid(float leftEyelid) { this.leftEyelid = Math.max(0.0f, Math.min(1.0f, leftEyelid)); }

    public float getRightEyelid() { return rightEyelid; }
    public void setRightEyelid(float rightEyelid) { this.rightEyelid = Math.max(0.0f, Math.min(1.0f, rightEyelid)); }

    public int getEyeColor() { return eyeColor; }
    public void setEyeColor(int eyeColor) { this.eyeColor = eyeColor; }

    public int getScleraColor() { return scleraColor; }
    public void setScleraColor(int scleraColor) { this.scleraColor = scleraColor; }

    public String getEyeColorName() {
        for (int i = 0; i < EYE_COLORS.length; i++) {
            if (EYE_COLORS[i] == eyeColor) return EYE_COLOR_NAMES[i];
        }
        return "Custom";
    }

    public void cycleEyeColor() {
        for (int i = 0; i < EYE_COLORS.length; i++) {
            if (EYE_COLORS[i] == eyeColor) {
                eyeColor = EYE_COLORS[(i + 1) % EYE_COLORS.length];
                return;
            }
        }
        eyeColor = EYE_COLORS[0];
    }

    public boolean isBendableLimbs() {
        return bendableLimbs;
    }

    public void setBendableLimbs(boolean bendableLimbs) {
        this.bendableLimbs = bendableLimbs;
    }

    public boolean isShowGizmo() {
        return showGizmo;
    }

    public void setShowGizmo(boolean showGizmo) {
        this.showGizmo = showGizmo;
    }

    public boolean isFreeFlyCamera() {
        return cameraMode == CameraMode.FREE_FLY_WASD;
    }

    public void setFreeFlyCamera(boolean freeFlyCamera) {
        this.cameraMode = freeFlyCamera ? CameraMode.FREE_FLY_WASD : CameraMode.ORBIT;
    }

    public CameraMode getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(CameraMode cameraMode) {
        this.cameraMode = (cameraMode != null) ? cameraMode : CameraMode.ORBIT;
    }

    public EditorTheme getTheme() {
        return theme;
    }

    public void setTheme(EditorTheme theme) {
        this.theme = (theme != null) ? theme : EditorTheme.DARK_SLATE;
    }

    public boolean isShowWorldBlocks() {
        return showWorldBlocks;
    }

    public void setShowWorldBlocks(boolean showWorldBlocks) {
        this.showWorldBlocks = showWorldBlocks;
    }

    public boolean isCameraViewMode() {
        return cameraViewMode;
    }

    public void setCameraViewMode(boolean cameraViewMode) {
        this.cameraViewMode = cameraViewMode;
    }

    public int getKeyPlayPause() {
        return keyPlayPause;
    }

    public void setKeyPlayPause(int keyPlayPause) {
        this.keyPlayPause = keyPlayPause;
    }

    public int getKeyAddKeyframe() {
        return keyAddKeyframe;
    }

    public void setKeyAddKeyframe(int keyAddKeyframe) {
        this.keyAddKeyframe = keyAddKeyframe;
    }

    public int getKeyDelete() {
        return keyDelete;
    }

    public void setKeyDelete(int keyDelete) {
        this.keyDelete = keyDelete;
    }

    public int getKeyToggleGraph() {
        return keyToggleGraph;
    }

    public void setKeyToggleGraph(int keyToggleGraph) {
        this.keyToggleGraph = keyToggleGraph;
    }

    public int getKeyCameraView() {
        return keyCameraView;
    }

    public void setKeyCameraView(int keyCameraView) {
        this.keyCameraView = keyCameraView;
    }

    public enum Language {
        EN("English", "en"),
        RU("Русский", "ru");
        private final String displayName;
        private final String code;

        Language(String displayName, String code) {
            this.displayName = displayName;
            this.code = code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCode() {
            return code;
        }
    }

    private static Language globalLanguage = null;

    public static Language getGlobalLanguage() {
        if (globalLanguage == null) {
            try {
                String mcLang = net.minecraft.client.Minecraft.getInstance().options.languageCode;
                if (mcLang != null && mcLang.toLowerCase().startsWith("ru")) {
                    globalLanguage = Language.RU;
                } else {
                    globalLanguage = Language.EN;
                }
            } catch (Throwable ignored) {
                globalLanguage = Language.RU;
            }
        }
        return globalLanguage;
    }

    public static void setGlobalLanguage(Language lang) {
        globalLanguage = (lang != null) ? lang : Language.EN;
    }

    private Language language = null;

    public Language getLanguage() {
        if (language != null) {
            return language;
        }
        return getGlobalLanguage();
    }

    public void setLanguage(Language language) {
        this.language = (language != null) ? language : Language.EN;
        setGlobalLanguage(this.language);
    }

    public enum GizmoMode {
        ROTATE,
        TRANSLATE
    }

    private GizmoMode gizmoMode = GizmoMode.ROTATE;
    private ItemStack rightHandItem = ItemStack.EMPTY;
    private ItemStack leftHandItem = ItemStack.EMPTY;
    private boolean ikMode = false;
    private boolean onionSkin = false;
    private boolean uiAnimations = true;

    public ItemStack getRightHandItem() {
        return rightHandItem;
    }

    public void setRightHandItem(ItemStack stack) {
        this.rightHandItem = (stack != null) ? stack : ItemStack.EMPTY;
    }

    public ItemStack getLeftHandItem() {
        return leftHandItem;
    }

    public void setLeftHandItem(ItemStack stack) {
        this.leftHandItem = (stack != null) ? stack : ItemStack.EMPTY;
    }

    public boolean isIkMode() {
        return ikMode;
    }

    public void setIkMode(boolean ikMode) {
        this.ikMode = ikMode;
    }

    public boolean isOnionSkin() {
        return onionSkin;
    }

    public void setOnionSkin(boolean onionSkin) {
        this.onionSkin = onionSkin;
    }

    public GizmoMode getGizmoMode() {
        return gizmoMode;
    }

    public void setGizmoMode(GizmoMode gizmoMode) {
        this.gizmoMode = (gizmoMode != null) ? gizmoMode : GizmoMode.ROTATE;
    }
    public boolean isUiAnimations() {
        return uiAnimations;
    }

    public void setUiAnimations(boolean uiAnimations) {
        this.uiAnimations = uiAnimations;
    }
}

