package net.openstage.client.ui;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.openstage.block.entity.ModelBlockEntity;
import net.openstage.client.cinematic.CinematicPlaybackHandler;
import net.openstage.client.model.ModelSettings;
import net.openstage.client.renderer.*;
import net.openstage.client.skin.SkinManager;
import net.openstage.client.theme.EditorTheme;
import net.openstage.client.ui.widgets.*;
import net.openstage.core.UndoRedoManager;
import net.openstage.core.animation.*;
import net.openstage.core.scene.CameraTrack;
import net.openstage.io.AnimationFileManager;
import net.openstage.network.OpenStageNetwork;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class OpenStageScreen extends Screen implements

        TimelinePanel.TimelineListener,

        PropertyInspector.InspectorListener,

        TimelinePanel.TimelineActionListener,

        SettingsDialog.SettingsListener,

        GraphEditorPanel.GraphActionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("OpenStage/Screen");
    private static final String VERSION = "1.21.1";
    private static final float LEFT_RATIO   = 0.20f;
    private static final float CENTER_RATIO = 0.55f;
    private static final float VIEWPORT_HEIGHT_RATIO = 0.60f;
    private static final int TITLE_BAR_HEIGHT  = 22;

    public record SceneActorRef(BlockPos pos, String name, float relX, float relY, float relZ, String skin, boolean slim, String clipId) {}

    private final List<SceneActorRef> nearbyActors = new ArrayList<>();
    private int selectedActorIndex = 0;

    private enum DraggingSplitter {
        NONE, LEFT, RIGHT, HORIZONTAL
    }

    private DraggingSplitter draggingSplitter = DraggingSplitter.NONE;

    private enum MenuType {
        NONE, FILE, FOLDER, IMPORT, EXPORT
    }

        private MenuType activeMenu = MenuType.NONE;

    private final net.openstage.client.ui.widgets.VideoExportModal videoExportModal = new net.openstage.client.ui.widgets.VideoExportModal();
    private final net.openstage.client.ui.widgets.ModelImportModal modelImportModal = new net.openstage.client.ui.widgets.ModelImportModal();
    private final net.openstage.client.ui.widgets.SkinPickerModal skinPickerModal = new net.openstage.client.ui.widgets.SkinPickerModal();
    private float prevAnimTime = 0.0f;
    private long lastFrameTimeMs = 0L;

    private TimelinePanel timelinePanel;
    private PropertyInspector propertyInspector;
    private TextureTarget exportRenderTarget;
    private GraphEditorPanel graphEditorPanel;
    private SettingsDialog settingsDialog;
    private int leftW, centerW, rightW;
    private int leftX, centerX, rightX;
    private int contentY;
    private int viewportH, timelineH;
    private float cameraPitch = 15.0f;
    private float cameraYaw = 145.0f;
    private float cameraZoom = 2.0f;
    private float cameraPanX = 0.0f;
    private float cameraPanY = 0.0f;
    private boolean draggingViewport = false;
    private double lastMouseX, lastMouseY;
    private int activeGizmoAxis = -1;
    private boolean showingGraph = false;
    private boolean hasModel = false;
    private String currentModelName = "Steve";
    private boolean isStaticPoseMode = false;
    private boolean cameraViewMode = false;
    private AnimationClip currentClip = null;
    private String selectedBone = "body";
    private int fpsIndex = 1;
    private static final int[] FPS_OPTIONS = {24, 30, 60};
    private String currentFileName = null;
    private BlockPos targetBlockPos = null;
    private float[] clipboardRotation = null;
    private boolean rightPanelCollapsed = false;
    private float rightPanelAnim = 1.0f;
    private float rightPanelTarget = 1.0f;
    private long contextMenuOpenTime = 0;
    private int savedLeftW = 160;
    private boolean isProjectContextMenuOpen = false;
    private String contextProjectFile = null;
    private int contextMenuX = 0;
    private int contextMenuY = 0;
    private String renamingTargetFile = null;
    private long lastScreenAnimTime = 0;
    private int savedRightW = 0;
    private boolean isRenamingProject = false;
    private boolean isRenamingProjectClosing = false;
    private long renameCloseTime = 0;
    private long renameOpenTime = 0;
    private String renameInput = "";
    private boolean isEditingTextCard = false;
    private boolean isEditingTextCardClosing = false;
    private long textCardCloseTime = 0;
    private long textCardOpenTime = 0;
    private String textCardInput = "";
    private net.openstage.core.animation.TextCard activeEditingTextCard = null;
    private boolean isEditingAudioTrack = false;
    private boolean isEditingAudioTrackClosing = false;
    private long audioTrackOpenTime = 0;
    private long audioTrackCloseTime = 0;
    private net.openstage.client.audio.AudioPlaybackService.AudioSegment activeEditingAudioSegment = null;
    private boolean isCreatingNewModel = false;
    private boolean isAddingNewActor = false;
    private boolean isCreatingNewModelClosing = false;
    private long createModelCloseTime = 0;
    private long createModelOpenTime = 0;
    private String newModelNameInput = "MyModel";
    private boolean newModelIsPose = false;
    private long menuOpenTime = 0;
    private MenuType closingMenu = MenuType.NONE;
    private long menuCloseTime = 0;

    private void openCreateModelDialog() {
        this.isCreatingNewModel = true;
        this.isCreatingNewModelClosing = false;
        this.createModelOpenTime = System.currentTimeMillis();
        this.newModelNameInput = "MyModel";
        this.newModelIsPose = false;
    }

    public void openTextCardDialog(net.openstage.core.animation.TextCard card) {
        if (card == null) return;
        this.activeEditingTextCard = card;
        this.textCardInput = (card.getText() != null) ? card.getText() : "";
        this.isEditingTextCard = true;
        this.isEditingTextCardClosing = false;
        this.textCardOpenTime = System.currentTimeMillis();
    }

    public void closeTextCardDialog() {
        if (this.isEditingTextCard && !this.isEditingTextCardClosing) {
            this.isEditingTextCardClosing = true;
            this.textCardCloseTime = System.currentTimeMillis();
        }
    }

    public void openAudioTrackDialog(net.openstage.client.audio.AudioPlaybackService.AudioSegment seg) {
        this.activeEditingAudioSegment = seg;
        this.isEditingAudioTrack = true;
        this.isEditingAudioTrackClosing = false;
        this.audioTrackOpenTime = System.currentTimeMillis();
    }

    public void closeAudioTrackDialog() {
        boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();
        if (anims) {
            this.isEditingAudioTrackClosing = true;
            this.audioTrackCloseTime = System.currentTimeMillis();
        } else {
            this.isEditingAudioTrack = false;
            this.isEditingAudioTrackClosing = false;
            this.activeEditingAudioSegment = null;
        }
    }

    public void confirmTextCardEdit() {
        if (activeEditingTextCard != null) {
            activeEditingTextCard.setText(textCardInput);
        }
        closeTextCardDialog();
    }

    private void openRenameDialog() {
        openRenameDialogForFile(null);
    }

    private void openRenameDialogForFile(String targetFile) {
        this.renamingTargetFile = targetFile;
        this.isRenamingProject = true;
        this.isRenamingProjectClosing = false;
        this.renameOpenTime = System.currentTimeMillis();
        if (targetFile != null) {
            String baseName = targetFile.replace(".osanim.json", "").replace(".ospose.json", "");
            this.renameInput = baseName;
        } else {
            this.renameInput = (currentClip != null) ? currentClip.getName() : currentModelName;
        }
    }

    private void closeCreateModelDialog() {
        boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();

        if (!anims) {
            this.isCreatingNewModel = false;
            this.isCreatingNewModelClosing = false;

            return;
        }

        if (this.isCreatingNewModel && !this.isCreatingNewModelClosing) {
            this.isCreatingNewModelClosing = true;
            this.createModelCloseTime = System.currentTimeMillis();
        }
    }

    private void closeRenameDialog() {
        boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();

        if (!anims) {
            this.isRenamingProject = false;
            this.isRenamingProjectClosing = false;

            return;
        }

        if (this.isRenamingProject && !this.isRenamingProjectClosing) {
            this.isRenamingProjectClosing = true;
            this.renameCloseTime = System.currentTimeMillis();
        }
    }

    private void setActiveMenu(MenuType menu) {
        boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();

        if (this.activeMenu != menu) {
            if (this.activeMenu != MenuType.NONE && menu == MenuType.NONE && anims) {
                this.closingMenu = this.activeMenu;
                this.menuCloseTime = System.currentTimeMillis();
            } else {
                this.closingMenu = MenuType.NONE;
            }

            if (menu != MenuType.NONE) {
                this.menuOpenTime = System.currentTimeMillis();
            }
        }

        this.activeMenu = menu;
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

    @Override

    public boolean isPauseScreen() {
        return false;
    }

    public ModelSettings getModelSettings() {
        return (propertyInspector != null) ? propertyInspector.getModelSettings() : null;
    }

    private boolean isWorldMode() {
        ModelSettings ms = (propertyInspector != null) ? propertyInspector.getModelSettings() : null;

        if (ms == null || !ms.isShowWorldBlocks() || this.minecraft == null || this.minecraft.level == null) {
            return false;
        }

        if (targetBlockPos == null && this.minecraft.player != null) {
            targetBlockPos = this.minecraft.player.blockPosition();
        }

        return targetBlockPos != null;
    }

    public OpenStageScreen() {
        super(Component.literal("OpenStage v" + VERSION));
        this.hasModel = false;
        this.currentClip = null;
        this.currentFileName = null;
    }

    public static void openForBlock(BlockPos pos) {
        OpenStageScreen screen = new OpenStageScreen();
        screen.targetBlockPos = pos;
        screen.hasModel = true;
        screen.currentModelName = "Model Block";
        screen.currentFileName = "model_pose.ospose.json";
        screen.isStaticPoseMode = true;
        screen.currentClip = new AnimationClip(screen.currentModelName, 1.0f);

        for (String b : TimelinePanel.TRACK_BONES) {
            screen.currentClip.getOrCreateBone(b);
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null) {
            BlockEntity be = mc.level.getBlockEntity(pos);

            if (be instanceof ModelBlockEntity mbe && mbe.isStaticPose()) {
                for (Map.Entry<String, float[]> e : mbe.getPoseRotations().entrySet()) {
                    BoneTrack bt = screen.currentClip.getOrCreateBone(e.getKey());

                    bt.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, e.getValue(), EasingType.LINEAR, null));
                }

                for (Map.Entry<String, float[]> e : mbe.getPosePositions().entrySet()) {
                    BoneTrack bt = screen.currentClip.getOrCreateBone(e.getKey());

                    bt.addKeyframe(ChannelType.POSITION, new Keyframe(0.0f, e.getValue(), EasingType.LINEAR, null));
                }
            }
        }

        Minecraft.getInstance().setScreen(screen);
    }

    @Override

    protected void init() {
        super.init();

        skinPickerModal.setListener(skinName -> {
            boolean isSlim = net.openstage.client.skin.SkinManager.getInstance().isSkinSlim(skinName);
            if (propertyInspector != null) {
                propertyInspector.setCurrentSkin(skinName);
                propertyInspector.setSlimModel(isSlim);
            }
            onSkinChanged(skinName, isSlim);
        });

        modelImportModal.setListener((file, name, fmt, modelId, isSlim, isBendable) -> {
            if (isBendable) {
                if (getModelSettings() != null) getModelSettings().setBendableLimbs(true);

                if (propertyInspector != null) propertyInspector.getModelSettings().setBendableLimbs(true);
            } else if ("player:steve".equals(modelId) || "player:alex".equals(modelId)) {
                if (getModelSettings() != null) getModelSettings().setBendableLimbs(false);

                if (propertyInspector != null) propertyInspector.getModelSettings().setBendableLimbs(false);
            }

            if (isAddingNewActor) {
                isAddingNewActor = false;
                int nextIdx = nearbyActors.size();
                float offX = nextIdx * 24.0f;

                SceneActorRef newActor = new SceneActorRef(
                        targetBlockPos,

                        name,

                        offX, 0.0f, 0.0f,

                        name,

                        isSlim,

                        "idle"
                );

                nearbyActors.add(newActor);

                if (currentClip != null) {
                    currentClip.migrateToMultiActor();

                    currentClip.getOrCreateBone("actor" + nextIdx + ":root");

                    currentClip.getOrCreateBone("actor" + nextIdx + ":body");
                }

                this.selectedActorIndex = nextIdx;
                this.hasModel = true;
                this.currentModelName = name;

                if (timelinePanel != null) {
                    timelinePanel.setSelectedActorIndex(nextIdx);

                    timelinePanel.setModelName(name);

                    timelinePanel.setClip(currentClip);
                }

                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(Component.literal("§a[OpenStage] Added actor: " + name), true);
                }

                if (net.openstage.client.model.RecentStateStore.hasSnapshot(modelId)) {
                    applySnapshotToCurrentActor(net.openstage.client.model.RecentStateStore.getSnapshot(modelId));
                }

                recalculateLayout();

                return;
            }

            this.hasModel = true;
            this.currentModelName = name;

            if (this.currentClip == null) {
                this.currentClip = new AnimationClip(name, 5.0f);
            }

            if (nearbyActors.isEmpty()) {
                nearbyActors.add(new SceneActorRef(targetBlockPos, name, 0.0f, 0.0f, 0.0f, name, isSlim, "idle"));
                selectedActorIndex = 0;
            } else {
                int idx = (selectedActorIndex >= 0 && selectedActorIndex < nearbyActors.size()) ? selectedActorIndex : 0;
                SceneActorRef curActor = nearbyActors.get(idx);
                float rx = (nearbyActors.size() <= 1) ? 0.0f : curActor.relX();
                float ry = (nearbyActors.size() <= 1) ? 0.0f : curActor.relY();
                float rz = (nearbyActors.size() <= 1) ? 0.0f : curActor.relZ();

                                nearbyActors.set(idx, new SceneActorRef(
                        curActor.pos(),
                        name,
                        rx,
                        ry,
                        rz,
                        modelId,
                        isSlim,
                        curActor.clipId()
                ));
            }

            if (timelinePanel != null) {
                timelinePanel.setSelectedActorIndex(selectedActorIndex);

                timelinePanel.setModelName(name);

                timelinePanel.setClip(currentClip);
            }

            if (propertyInspector != null) {
                propertyInspector.setCurrentSkin(name);
                propertyInspector.setSlimModel(isSlim);
            }
            if (selectedActorIndex == 0 && targetBlockPos != null && this.minecraft != null && this.minecraft.level != null) {
                if (this.minecraft.level.getBlockEntity(targetBlockPos) instanceof ModelBlockEntity mbe) {
                    mbe.setModelId(modelId);
                    mbe.setSkin(name);
                    mbe.setSlim(isSlim);
                    mbe.setChanged();
                }
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                        new net.openstage.network.OpenStageNetwork.UpdateModelBlockPayload(
                                targetBlockPos, modelId, name, isSlim, (currentClip != null ? currentClip.getName() : "idle"), 0.0f, 1.0f
                        )
                );
            }
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(Component.literal("§a[OpenStage] Loaded model: " + name), true);
            }

            if (net.openstage.client.model.RecentStateStore.hasSnapshot(modelId)) {
                applySnapshotToCurrentActor(net.openstage.client.model.RecentStateStore.getSnapshot(modelId));
            } else {
                if (getModelSettings() != null) getModelSettings().setRootYaw(0.0f);
                if (propertyInspector != null) propertyInspector.getModelSettings().setRootYaw(0.0f);
                if (currentClip != null) {
                    int aIdx = (selectedActorIndex >= 0) ? selectedActorIndex : 0;
                    String pfx = (nearbyActors.size() > 1) ? ("actor" + aIdx + ":") : "";
                    for (String b : TimelinePanel.TRACK_BONES) {
                        currentClip.removeBone(pfx + b);
                    }
                    currentClip.removeBone(pfx + "root");
                    currentClip.removeBone(pfx + "body");
                }
            }

            recalculateLayout();
        });

        videoExportModal.setListener(new net.openstage.client.ui.widgets.VideoExportModal.ExportListener() {
            @Override
            public void onStartExport(int w, int h, int fps, boolean useFfmpeg, int bitrate) {
                if (currentClip == null) return;
                BlockPos exportOrigin = targetBlockPos;
                if (exportOrigin == null && !nearbyActors.isEmpty() && nearbyActors.get(0).pos() != null) {
                    exportOrigin = nearbyActors.get(0).pos();
                }
                if (exportOrigin == null && Minecraft.getInstance().player != null) {
                    exportOrigin = Minecraft.getInstance().player.blockPosition();
                }

                java.util.List<BlockPos> actorList = new java.util.ArrayList<>();
                for (SceneActorRef a : nearbyActors) {
                    if (a.pos() != null) actorList.add(a.pos());
                }
                if (actorList.isEmpty() && exportOrigin != null) {
                    actorList.add(exportOrigin);
                }

                net.openstage.core.scene.CameraTrack ct = currentClip.getCameraTrack();
                if (ct.getKeyframes().isEmpty()) {
                    float cx = (propertyInspector != null) ? propertyInspector.getCamX() : 0.0f;
                    float cy = (propertyInspector != null) ? propertyInspector.getCamY() : 1.4f;
                    float cz = (propertyInspector != null) ? propertyInspector.getCamZ() : -3.2f;
                    float cyaw = (propertyInspector != null) ? propertyInspector.getCamYaw() : 0.0f;
                    float cpitch = (propertyInspector != null) ? propertyInspector.getCamPitch() : 0.0f;
                    float croll = (propertyInspector != null) ? propertyInspector.getCamRoll() : 0.0f;
                    float cfov = (propertyInspector != null) ? propertyInspector.getCamFov() : 70.0f;
                    ct.addKey(new net.openstage.core.scene.CameraTrack.CameraKeyframe(0.0f, cx, cy, cz, cyaw, cpitch, croll, cfov, null));
                }

                net.openstage.client.cinematic.VideoExportService.getInstance().startExport(currentClip, w, h, fps, useFfmpeg, bitrate);
                net.openstage.client.cinematic.CinematicPlaybackHandler.getInstance().setReturnScreen(OpenStageScreen.this);
                net.openstage.client.cinematic.CinematicPlaybackHandler.getInstance().startExport(currentClip, exportOrigin, actorList, fps);
                videoExportModal.setExporting(true);
            }
            @Override
            public void onCancelExport() {
                net.openstage.client.cinematic.VideoExportService.getInstance().cancelExport();
                net.openstage.client.cinematic.CinematicPlaybackHandler.getInstance().stop();
                videoExportModal.setExporting(false);
            }
        });

        recalculateLayout();

        scanNearbyActors();

        if (timelinePanel == null) {
            timelinePanel = new TimelinePanel(centerX, contentY + viewportH, centerW, timelineH, FPS_OPTIONS[fpsIndex]);

            timelinePanel.setListener(this);

            timelinePanel.setActionListener(this);

            timelinePanel.setModelName(hasModel ? currentModelName : "No Model");

            timelinePanel.setClip(currentClip);
        } else {
            timelinePanel.setBounds(centerX, contentY + viewportH, centerW, timelineH);

            timelinePanel.setActionListener(this);

            timelinePanel.setModelName(hasModel ? currentModelName : "No Model");

            timelinePanel.setClip(currentClip);
        }
        timelinePanel.setSettings(getModelSettings());

        if (propertyInspector == null) {
            propertyInspector = new PropertyInspector(rightX, contentY, rightW, this.height - contentY);

            propertyInspector.setListener(this);

            propertyInspector.setSelectedBone(selectedBone);
            propertyInspector.setOnOpenSkinPicker(() -> skinPickerModal.open(propertyInspector.getCurrentSkin()));

            if (this.minecraft != null && this.minecraft.level != null && targetBlockPos != null) {
                if (this.minecraft.level.getBlockEntity(targetBlockPos) instanceof ModelBlockEntity mbe) {
                    propertyInspector.setCurrentSkin(mbe.getSkin());

                    propertyInspector.setSlimModel(mbe.isSlim());

                    propertyInspector.getModelSettings().setRootYaw(mbe.getYaw());

                    propertyInspector.getModelSettings().setScale(mbe.getScale());

                    propertyInspector.getModelSettings().setRightHandItem(mbe.getRightHandItem());

                    propertyInspector.getModelSettings().setLeftHandItem(mbe.getLeftHandItem());
                }
            }
        } else {
            propertyInspector.setBounds(rightX, contentY, rightW, this.height - contentY);

            propertyInspector.setListener(this);
            propertyInspector.setOnOpenSkinPicker(() -> skinPickerModal.open(propertyInspector.getCurrentSkin()));
        }

        if (graphEditorPanel == null) {
            graphEditorPanel = new GraphEditorPanel(centerX, contentY + viewportH, centerW, timelineH, FPS_OPTIONS[fpsIndex]);

            graphEditorPanel.setActionListener(this);

            graphEditorPanel.setClip(currentClip);

            graphEditorPanel.setSelectedBone(selectedBone);
        } else {
            graphEditorPanel.setBounds(centerX, contentY + viewportH, centerW, timelineH);

            graphEditorPanel.setClip(currentClip);

            graphEditorPanel.setSelectedBone(selectedBone);
        }

        if (settingsDialog == null) {
            ModelSettings ms = (propertyInspector != null) ? propertyInspector.getModelSettings() : new ModelSettings();
            settingsDialog = new SettingsDialog(ms, this);
        }

        SkinManager.getInstance().refreshSkins();
    }

    private void scanNearbyActors() {
        nearbyActors.clear();
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level != null) {
            if (targetBlockPos == null && mc.player != null) {
                HitResult hit = mc.hitResult;
                if (hit instanceof BlockHitResult bhr) {
                    BlockPos hp = bhr.getBlockPos();
                    if (level.getBlockEntity(hp) instanceof ModelBlockEntity) {
                        targetBlockPos = hp;
                    }
                }
                if (targetBlockPos == null) {
                    BlockPos pPos = mc.player.blockPosition();
                    double bestDist = Double.MAX_VALUE;
                    BlockPos closest = null;
                    int r = 24;
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dy = -8; dy <= 8; dy++) {
                            for (int dz = -r; dz <= r; dz++) {
                                BlockPos p = pPos.offset(dx, dy, dz);
                                if (level.getBlockEntity(p) instanceof ModelBlockEntity) {
                                    double d = p.distSqr(pPos);
                                    if (d < bestDist) {
                                        bestDist = d;
                                        closest = p;
                                    }
                                }
                            }
                        }
                    }
                    if (closest != null) {
                        targetBlockPos = closest;
                    }
                }
            }

            if (targetBlockPos != null) {
                String initSkin = "steve";
                boolean initSlim = false;
                String initClip = "idle";

                if (level.getBlockEntity(targetBlockPos) instanceof ModelBlockEntity targetMbe) {
                    initSkin = targetMbe.getSkin();
                    initSlim = targetMbe.isSlim();
                    initClip = targetMbe.getClipId();
                    if (targetMbe.getModelId() != null && !targetMbe.getModelId().isEmpty()) {
                        currentModelName = targetMbe.getModelId();
                    }
                    if (targetMbe.isStaticPose()) {
                        isStaticPoseMode = true;
                        boolean hasKeys = (currentClip != null) && currentClip.getBones().values().stream()
                                .anyMatch(bt -> !bt.getRotationKeys().isEmpty() || !bt.getPositionKeys().isEmpty());
                        if (currentClip == null || !hasKeys) {
                            currentClip = new AnimationClip(currentModelName != null ? currentModelName : "Model Block", 1.0f);
                            for (String b : TimelinePanel.TRACK_BONES) {
                                currentClip.getOrCreateBone(b);
                            }
                            for (Map.Entry<String, float[]> e : targetMbe.getPoseRotations().entrySet()) {
                                BoneTrack bt = currentClip.getOrCreateBone(e.getKey());
                                bt.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, e.getValue(), EasingType.LINEAR, null));
                            }
                            for (Map.Entry<String, float[]> e : targetMbe.getPosePositions().entrySet()) {
                                BoneTrack bt = currentClip.getOrCreateBone(e.getKey());
                                bt.addKeyframe(ChannelType.POSITION, new Keyframe(0.0f, e.getValue(), EasingType.LINEAR, null));
                            }
                            if (timelinePanel != null) {
                                timelinePanel.setClip(currentClip);
                            }
                        }
                    }
                }

                nearbyActors.add(new SceneActorRef(targetBlockPos, currentModelName, 0, 0, 0, initSkin, initSlim, initClip));
                int radius = 24;

                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -6; dy <= 6; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            BlockPos p = targetBlockPos.offset(dx, dy, dz);
                            BlockEntity be = level.getBlockEntity(p);

                            if (be instanceof ModelBlockEntity mbe) {
                                float rx = dx * 16.0f;
                                float ry = dy * 16.0f;
                                float rz = dz * 16.0f;
                                String name = "Actor (" + p.getX() + ", " + p.getZ() + ")";

                                nearbyActors.add(new SceneActorRef(p, name, rx, ry, rz, mbe.getSkin(), mbe.isSlim(), mbe.getClipId()));
                            }
                        }
                    }
                }
            } else if (hasModel) {
                nearbyActors.add(new SceneActorRef(null, currentModelName, 0, 0, 0, "steve", false, "idle"));
            }
        } else if (hasModel) {
            nearbyActors.add(new SceneActorRef(null, currentModelName, 0, 0, 0, "steve", false, "idle"));
        }

        selectedActorIndex = 0;
    }

    private void recalculateLayout() {
        contentY = TITLE_BAR_HEIGHT;
        int contentH = this.height - contentY;
        int maxLeft = Math.max(160, this.width - (rightW > 20 ? rightW : 0) - 80);
        int nominalLeftW = (savedLeftW > 0) ? savedLeftW : (int) (this.width * LEFT_RATIO);
        nominalLeftW = Math.max(100, Math.min(maxLeft, nominalLeftW));
        if (savedLeftW <= 0) savedLeftW = nominalLeftW;
        leftW = nominalLeftW;

        int nominalRightW = (savedRightW > 0) ? savedRightW : (int) (this.width * (1.0f - LEFT_RATIO - CENTER_RATIO));
        nominalRightW = Math.max(130, Math.min(this.width / 2, nominalRightW));
        if (savedRightW <= 0) savedRightW = nominalRightW;
        boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();
        float currentAnim = anims ? rightPanelAnim : (rightPanelCollapsed ? 0.0f : 1.0f);
        rightW = Math.round(nominalRightW * currentAnim);

        if (rightW <= 2) {
            rightW = 0;
            centerW = Math.max(120, this.width - leftW);
            rightX = this.width;
        } else {
            centerW = Math.max(120, this.width - leftW - rightW);
            rightX = this.width - rightW;
        }

        leftX = 0;
        centerX = leftW;

        if (isStaticPoseMode) {
            timelineH = 24;
            viewportH = contentH - timelineH;
        } else {
            if (viewportH <= 0 || viewportH > contentH - 50) {
                viewportH = (int) (contentH * VIEWPORT_HEIGHT_RATIO);
            }
            viewportH = Math.max(50, Math.min(contentH - 50, viewportH));
            timelineH = contentH - viewportH;
        }
    }

    private void recalculateLayoutAfterResize() {
        int maxLeft = Math.max(160, this.width - (rightW > 20 ? rightW : 0) - 80);
        int nominalLeftW = (savedLeftW > 0) ? savedLeftW : (int) (this.width * LEFT_RATIO);
        nominalLeftW = Math.max(100, Math.min(maxLeft, nominalLeftW));
        if (savedLeftW <= 0) savedLeftW = nominalLeftW;
        leftW = nominalLeftW;

        int nominalRightW = (savedRightW > 0) ? savedRightW : (int) (this.width * (1.0f - LEFT_RATIO - CENTER_RATIO));
        nominalRightW = Math.max(130, Math.min(this.width / 2, nominalRightW));
        if (savedRightW <= 0) savedRightW = nominalRightW;
        boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();
        float currentAnim = anims ? rightPanelAnim : (rightPanelCollapsed ? 0.0f : 1.0f);
        rightW = Math.round(nominalRightW * currentAnim);

        if (rightW <= 2) {
            rightW = 0;
            centerW = Math.max(120, this.width - leftW);
            rightX = this.width;
        } else {
            centerW = Math.max(120, this.width - leftW - rightW);
            rightX = this.width - rightW;
        }

        leftX = 0;
        centerX = leftW;
        int contentH = this.height - contentY;

        if (isStaticPoseMode) {
            timelineH = 24;
            viewportH = contentH - timelineH;
        } else {
            viewportH = Math.max(50, Math.min(contentH - 50, viewportH));
            timelineH = contentH - viewportH;
        }

        if (timelinePanel != null) {
            timelinePanel.setBounds(centerX, contentY + viewportH, centerW, timelineH);
        }

        if (graphEditorPanel != null) {
            graphEditorPanel.setBounds(centerX, contentY + viewportH, centerW, timelineH);
        }

        if (propertyInspector != null) {
            int nominalW = (savedRightW > 0) ? savedRightW : rightW;

            propertyInspector.setBounds(rightX, contentY, nominalW, this.height - contentY);
        }

    }

    @Override

    public void resize(Minecraft minecraft, int width, int height) {
        int oldW = this.width;
        int oldH = this.height;

        super.resize(minecraft, width, height);

        if (oldW > 0 && oldH > 0) {
            leftW = (int) ((float) leftW * width / oldW);
            rightW = (int) ((float) rightW * width / oldW);
            viewportH = (int) ((float) viewportH * height / oldH);
        }

        recalculateLayout();

        recalculateLayoutAfterResize();
    }

    @Override

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
        long screenNow = System.currentTimeMillis();

        if (lastScreenAnimTime > 0) {
            float dt = Math.min(0.1f, (screenNow - lastScreenAnimTime) / 1000.0f);
            boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();

            if (!anims) {
                if (rightPanelAnim != rightPanelTarget) {
                    rightPanelAnim = rightPanelTarget;

                    recalculateLayout();

                    recalculateLayoutAfterResize();
                }
            } else if (Math.abs(rightPanelTarget - rightPanelAnim) > 0.001f) {
                float t = 1.0f - (float) Math.exp(-11.0f * dt);
                rightPanelAnim += (rightPanelTarget - rightPanelAnim) * Math.min(1.0f, t);

                if (Math.abs(rightPanelTarget - rightPanelAnim) < 0.002f) {
                    rightPanelAnim = rightPanelTarget;
                }

                recalculateLayout();

                recalculateLayoutAfterResize();
            }
        }

        lastScreenAnimTime = screenNow;
        EditorTheme theme = (propertyInspector != null) ? propertyInspector.getModelSettings().getTheme() : EditorTheme.DARK_SLATE;

        if (timelinePanel != null) {
            timelinePanel.setTheme(theme);

            timelinePanel.setModelName(hasModel ? currentModelName : "No Model");

            if (propertyInspector != null) {
                timelinePanel.setSettings(propertyInspector.getModelSettings());
            }
        }

        if (graphEditorPanel != null) {
            graphEditorPanel.setTheme(theme);
        }

        graphics.fill(0, 0, this.width, this.height, theme.getViewportBg());

        recalculateLayoutAfterResize();

        renderTopToolbar(graphics, mouseX, mouseY, theme);

        renderLeftPanel(graphics, mouseX, mouseY, theme);

        graphics.bufferSource().endBatch();

        renderViewport(graphics, mouseX, mouseY, delta, theme);

        graphics.bufferSource().endBatch();

        if (isStaticPoseMode) {
            renderStaticPoseBar(graphics, mouseX, mouseY, theme);
        } else if (showingGraph) {
            if (graphEditorPanel != null) {
                if (timelinePanel != null) {
                    graphEditorPanel.setCurrentFrame(timelinePanel.getCurrentFrame());
                }

                graphEditorPanel.render(graphics, mouseX, mouseY, delta);
            }
        } else {
            if (timelinePanel != null) {
                timelinePanel.render(graphics, mouseX, mouseY, delta);
                graphics.bufferSource().endBatch();
            }
        }

        if (rightW > 2 && propertyInspector != null) {
            int nominalW = (savedRightW > 0) ? savedRightW : rightW;
            propertyInspector.setBounds(rightX, contentY, nominalW, this.height - contentY);
            propertyInspector.render(graphics, mouseX, mouseY, delta);
            graphics.bufferSource().endBatch();
        }

        renderDividers(graphics, mouseX, mouseY, theme);
        renderRightPanelToggle(graphics, mouseX, mouseY, theme);

        if (activeMenu != MenuType.NONE || closingMenu != MenuType.NONE) {
            renderDropdownMenu(graphics, mouseX, mouseY, theme);
        }
        renderProjectContextMenu(graphics, mouseX, mouseY, theme);

        if (showingGraph && graphEditorPanel != null && graphEditorPanel.getCustomizerModal().isVisible()) {
            graphics.bufferSource().endBatch();

            com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);

            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

            graphics.pose().pushPose();

            graphics.pose().translate(0, 0, 450.0f);

            graphEditorPanel.getCustomizerModal().setUiAnimations(getModelSettings() == null || getModelSettings().isUiAnimations());

            graphEditorPanel.getCustomizerModal().render(graphics, mouseX, mouseY);

            graphics.bufferSource().endBatch();

            graphics.pose().popPose();

            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }

        if (settingsDialog != null && settingsDialog.isOpen()) {
            graphics.bufferSource().endBatch();

            com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);

            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

            graphics.pose().pushPose();

            graphics.pose().translate(0, 0, 500.0f);

            settingsDialog.render(graphics, this.width, this.height, mouseX, mouseY);

            graphics.bufferSource().endBatch();

            graphics.pose().popPose();

            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }

        if (propertyInspector != null && propertyInspector.isItemPickerOpen()) {
            graphics.bufferSource().endBatch();

            com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);

            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

            graphics.pose().pushPose();

            graphics.pose().translate(0, 0, 520.0f);

            propertyInspector.renderItemPicker(graphics, mouseX, mouseY);

            graphics.bufferSource().endBatch();

            graphics.pose().popPose();

            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }

        if (isCreatingNewModel || isCreatingNewModelClosing) {
            graphics.bufferSource().endBatch();

            com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);

            com.mojang.blaze3d.systems.RenderSystem.enableBlend();

            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

            renderCreateModelDialog(graphics, mouseX, mouseY, theme);

            graphics.bufferSource().endBatch();
        }

        if (isRenamingProject) {
            graphics.bufferSource().endBatch();

            com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);

            renderRenameDialog(graphics, mouseX, mouseY, theme);

            graphics.bufferSource().endBatch();
        }

        if (net.openstage.client.cinematic.VideoExportService.getInstance().isExporting()) {
            net.openstage.client.cinematic.VideoExportService svc = net.openstage.client.cinematic.VideoExportService.getInstance();
            videoExportModal.updateProgress(
                    svc.getProgress(),
                    String.format("Кадр %d / %d (%.0f%%)", svc.getCurrentFrame(), svc.getTotalFrames(), svc.getProgress() * 100.0f)
            );
        } else if (videoExportModal.isVisible() && videoExportModal.isExporting()) {
            videoExportModal.setExporting(false);
            videoExportModal.updateProgress(1.0f, "Экспорт завершён!");
        }

        if (videoExportModal.isVisible()) {
            graphics.bufferSource().endBatch();

            com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);

            videoExportModal.render(graphics, this.font, this.width, this.height, mouseX, mouseY, theme);

            graphics.bufferSource().endBatch();
        }

        if (modelImportModal.isVisible()) {
            graphics.bufferSource().endBatch();

            com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);

            modelImportModal.render(graphics, this.font, this.width, this.height, mouseX, mouseY, theme);

            graphics.bufferSource().endBatch();
        }

        if (skinPickerModal.isVisible()) {
            graphics.bufferSource().endBatch();

            com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);

            skinPickerModal.render(graphics, this.font, this.width, this.height, mouseX, mouseY, theme);

            graphics.bufferSource().endBatch();
        }

        if (isEditingTextCard || isEditingTextCardClosing) {
            graphics.bufferSource().endBatch();
            com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);
            renderTextCardDialog(graphics, mouseX, mouseY, theme);
            graphics.bufferSource().endBatch();
        }

        if (isEditingAudioTrack || isEditingAudioTrackClosing) {
            graphics.bufferSource().endBatch();
            com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);
            renderAudioTrackDialog(graphics, mouseX, mouseY, theme);
            graphics.bufferSource().endBatch();
        }
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }

    private int getRightPanelToggleX() {
        return Math.min(this.width - 18, rightX - 18);
    }

    private int getRightPanelToggleY() {
        return contentY + 7;
    }

    private int getRightPanelToggleSize() {
        return 16;
    }

    private void renderRightPanelToggle(GuiGraphics graphics, int mouseX, int mouseY, EditorTheme theme) {
        int btnSize = getRightPanelToggleSize();
        int btnX = getRightPanelToggleX();
        int btnY = getRightPanelToggleY();
        boolean collapsed = (rightPanelAnim < 0.5f);
        String icon = collapsed ? "◀" : "▶";
        boolean hovered = mouseX >= btnX && mouseX <= btnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize;
        int bg = collapsed ? (hovered ? 0xFF2563EB : 0xFF1E293B) : (hovered ? theme.getButtonHover() : 0xFF111827);
        int border = collapsed ? 0xFF60A5FA : (hovered ? theme.getAccent() : theme.getDivider());

        graphics.fill(btnX, btnY, btnX + btnSize, btnY + btnSize, bg);

        graphics.renderOutline(btnX, btnY, btnSize, btnSize, border);
        int tw = this.font.width(icon);

        graphics.drawString(this.font, icon, btnX + (btnSize - tw) / 2, btnY + 4, hovered ? 0xFFFFFFFF : (collapsed ? 0xFF93C5FD : theme.getTextColor()), false);
    }

    private List<MenuItem> getProjectContextMenuItems(ModelSettings.Language lang) {
        List<MenuItem> items = new ArrayList<>();
        if (contextProjectFile == null) return items;
        final String file = contextProjectFile;
        items.add(new MenuItem("✏ " + I18n.get("Rename", lang) + "...", true, () -> {
            openRenameDialogForFile(file);
        }));
        items.add(new MenuItem("📋 " + I18n.get("Duplicate", lang), true, () -> {
            AnimationFileManager.getInstance().duplicateFile(file);
        }));
        items.add(new MenuItem("📁 " + I18n.get("Open Folder", lang), true, () -> {
            AnimationFileManager.getInstance().openFileFolder(file);
        }));
        items.add(new MenuItem("🗑 " + I18n.get("Delete", lang), true, () -> {
            boolean wasCurrent = file.equalsIgnoreCase(currentFileName);
            AnimationFileManager.getInstance().deleteFile(file);
            if (wasCurrent) {
                currentFileName = null;
            }
        }));
        return items;
    }

    private void renderProjectContextMenu(GuiGraphics graphics, int mouseX, int mouseY, EditorTheme theme) {
        if (!isProjectContextMenuOpen || contextProjectFile == null) return;
        ModelSettings.Language lang = (propertyInspector != null) ? propertyInspector.getModelSettings().getLanguage() : ModelSettings.Language.EN;
        List<MenuItem> items = getProjectContextMenuItems(lang);
        if (items.isEmpty()) return;
        int itemH = 18;
        int dw = 140;
        for (MenuItem it : items) {
            int w = this.font.width(it.label) + 20;
            if (w > dw) dw = w;
        }
        int dh = items.size() * itemH + 6;
        int dx = Math.min(contextMenuX, this.width - dw - 4);
        int dy = Math.min(contextMenuY, this.height - dh - 4);

        boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();
        long now = System.currentTimeMillis();
        float p = anims ? Math.min(1.0f, (now - contextMenuOpenTime) / 110.0f) : 1.0f;
        float ease = 1.0f - (float) Math.pow(1.0f - p, 2);
        float scaleY = anims ? (0.85f + 0.15f * ease) : 1.0f;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 600.0f);
        graphics.pose().translate(dx, dy, 0);
        graphics.pose().scale(1.0f, scaleY, 1.0f);
        graphics.pose().translate(-dx, -dy, 0);

        graphics.fill(dx - 1, dy - 1, dx + dw + 1, dy + dh + 1, 0x40000000);
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF0F172A);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF334155);

        int iy = dy + 3;
        for (MenuItem it : items) {
            boolean hov = mouseX >= dx && mouseX <= dx + dw && mouseY >= iy && mouseY <= iy + itemH;
            if (hov) {
                graphics.fill(dx + 2, iy, dx + dw - 2, iy + itemH, 0xFF1E293B);
                graphics.fill(dx + 2, iy + 2, dx + 4, iy + itemH - 2, theme.getAccent());
            }
            int col = hov ? 0xFFFFFFFF : 0xFFCBD5E1;
            if (it.label.contains("Delete") || it.label.contains("Удалить")) {
                col = hov ? 0xFFF87171 : 0xFFEF4444;
            }
            graphics.drawString(this.font, it.label, dx + 8, iy + 4, col, false);
            iy += itemH;
        }
        graphics.pose().popPose();
    }

    private void renderDividers(GuiGraphics graphics, int mouseX, int mouseY, EditorTheme theme) {
        boolean leftHov = isHoveringLeftSplitter(mouseX, mouseY);
        boolean rightHov = (rightW > 20) && isHoveringRightSplitter(mouseX, mouseY);
        boolean horizHov = isHoveringHorizontalSplitter(mouseX, mouseY);
        int leftCol = (leftHov || draggingSplitter == DraggingSplitter.LEFT) ? theme.getDividerHover() : theme.getDivider();

        if (leftW > 20) {
            graphics.fill(leftW - 1, contentY, leftW + 2, this.height, leftCol);
        }

        if (rightW > 20) {
            int rightCol = (rightHov || draggingSplitter == DraggingSplitter.RIGHT) ? theme.getDividerHover() : theme.getDivider();

            graphics.fill(rightX - 1, contentY, rightX + 2, this.height, rightCol);
        }

        int horizCol = (horizHov || draggingSplitter == DraggingSplitter.HORIZONTAL) ? theme.getDividerHover() : theme.getDivider();

        int hLeft = (leftW > 20) ? (leftW + 2) : 0;
        int hRight = (rightW > 20) ? (rightX - 1) : this.width;
        graphics.fill(hLeft, contentY + viewportH - 1, hRight, contentY + viewportH + 2, horizCol);
    }

    private boolean isHoveringLeftSplitter(int mx, int my) { return false; }

    private boolean isHoveringRightSplitter(int mx, int my) {
        if (rightW <= 20) return false;

        return mx >= rightX - 3 && mx <= rightX + 3 && my >= contentY;
    }

    private boolean isHoveringHorizontalSplitter(int mx, int my) {
        int splitY = contentY + viewportH;

        return mx >= centerX && mx <= rightX && my >= splitY - 3 && my <= splitY + 3;
    }

    private void rotateCameraOrbit(double mouseX, double mouseY, double dragX, double dragY) {
        cameraYaw += (float) (dragX * 0.5f);
        cameraPitch = Math.max(-89.0f, Math.min(89.0f, cameraPitch + (float) (dragY * 0.5f)));
    }

    private void renderViewport(GuiGraphics graphics, int mouseX, int mouseY, float delta, EditorTheme theme) {
        float currentTime = (timelinePanel != null) ? timelinePanel.getCurrentTimeSeconds() : 0.0f;
        BlockPos activeWorldAnchor = (targetBlockPos != null) ? targetBlockPos : (this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.blockPosition() : null);

        ModelSettings settings = (propertyInspector != null) ? propertyInspector.getModelSettings() : null;
        ModelSettings.BackgroundMode bgMode = (settings != null) ? settings.getBackgroundMode() : ModelSettings.BackgroundMode.DEFAULT;

        if (bgMode == ModelSettings.BackgroundMode.CHROMA_GREEN) {
            graphics.fill(centerX, contentY, rightX, contentY + viewportH, 0xFF00FF00);
        } else if (bgMode == ModelSettings.BackgroundMode.CHROMA_BLUE) {
            graphics.fill(centerX, contentY, rightX, contentY + viewportH, 0xFF0033FF);
        } else {
            if (isWorldMode() && this.minecraft.level != null && activeWorldAnchor != null && (settings == null || settings.isShowWorldBlocks())) {
                net.minecraft.client.multiplayer.ClientLevel level = (net.minecraft.client.multiplayer.ClientLevel) this.minecraft.level;
                boolean canSeeSky = level.canSeeSky(activeWorldAnchor.above())
                        || activeWorldAnchor.getY() >= level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, activeWorldAnchor.getX(), activeWorldAnchor.getZ()) - 1;
                if (!canSeeSky && activeWorldAnchor.getY() < level.getSeaLevel() - 10) {
                    graphics.fillGradient(centerX, contentY, rightX, contentY + viewportH, 0xFF0F172A, 0xFF050811);
                } else {
                    net.minecraft.world.phys.Vec3 skyCol = level.getSkyColor(new net.minecraft.world.phys.Vec3(activeWorldAnchor.getX(), activeWorldAnchor.getY() + 1, activeWorldAnchor.getZ()), delta);
                    int skyR = Math.min(255, Math.max(0, (int) (skyCol.x * 255.0f)));
                    int skyG = Math.min(255, Math.max(0, (int) (skyCol.y * 255.0f)));
                    int skyB = Math.min(255, Math.max(0, (int) (skyCol.z * 255.0f)));
                    int topColor = 0xFF000000 | (skyR << 16) | (skyG << 8) | skyB;
                    int botR = Math.min(255, (int) (skyR * 1.15f + 25));
                    int botG = Math.min(255, (int) (skyG * 1.15f + 30));
                    int botB = Math.min(255, (int) (skyB * 1.15f + 35));
                    int botColor = 0xFF000000 | (botR << 16) | (botG << 8) | botB;
                    graphics.fillGradient(centerX, contentY, rightX, contentY + viewportH, topColor, botColor);
                }
            } else {
                graphics.fill(centerX, contentY, rightX, contentY + viewportH, theme.getViewportBg());
            }
        }
        graphics.bufferSource().endBatch();

        graphics.enableScissor(centerX, contentY, rightX, contentY + viewportH);
        try {

        if (isWorldMode()) {
            graphics.renderOutline(centerX, contentY, centerW, viewportH, theme.getAccent());

            if (currentClip != null) {
                net.openstage.core.scene.SceneManager.getInstance().loadClip(currentClip.getName(), currentClip);
            }
        }

        settings = (propertyInspector != null) ? propertyInspector.getModelSettings() : null;
        long nowMs = System.currentTimeMillis();
        float realDt = (lastFrameTimeMs > 0L) ? (float) (nowMs - lastFrameTimeMs) / 1000.0f : 0.016f;
        lastFrameTimeMs = nowMs;

        if (realDt > 0.05f) realDt = 0.05f;

        if (realDt < 0.001f) realDt = 0.001f;

        if (timelinePanel != null && timelinePanel.isPlaying() && currentClip != null) {
            double ox = (targetBlockPos != null) ? targetBlockPos.getX() + 0.5 : (this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getX() : 0.0);
            double oy = (targetBlockPos != null) ? targetBlockPos.getY() : (this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getY() : 64.0);
            double oz = (targetBlockPos != null) ? targetBlockPos.getZ() + 0.5 : (this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getZ() : 0.0);

            net.openstage.client.particle.ParticlePlaybackService.getInstance().playAtTime(currentClip, currentTime, prevAnimTime, ox, oy, oz);
            prevAnimTime = currentTime;
        }

        float vpPitch = cameraPitch;
        float vpYaw = cameraYaw;
        float vpPanX = cameraPanX;
        float vpPanY = cameraPanY;

        if (settings != null && settings.isCameraViewMode() && currentClip != null) {
            CameraTrack ct = currentClip.getCameraTrack();

            if (ct != null && !ct.getKeyframes().isEmpty()) {
                try {
                    CameraTrack.CameraKeyframe kf = ct.interpolateAt(currentTime);
                    vpPitch = kf.pitch();
                    vpYaw = kf.yaw();
                    vpPanX = -kf.x() * 0.15f;
                    vpPanY = kf.y() * 0.15f;
                } catch (Exception ignored) {}
            }
        }

        if (!hasModel) {
            int cardW = 270;
            int cardH = 92;
            int cardX = centerX + (centerW - cardW) / 2;
            int cardY = contentY + (viewportH - cardH) / 2;

            graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xEE111827);

            graphics.renderOutline(cardX, cardY, cardW, cardH, theme.getDivider());
            ModelSettings.Language vpLang = (settings != null) ? settings.getLanguage() : ModelSettings.Language.EN;

            graphics.drawCenteredString(this.font, I18n.get("No Model Loaded", vpLang), cardX + cardW / 2, cardY + 15, 0xFFCBD5E1);

            graphics.drawCenteredString(this.font, I18n.get("Select or create a model to begin", vpLang), cardX + cardW / 2, cardY + 31, 0xFF64748B);
            int btnW = 160;
            int btnH = 20;
            int btnX = cardX + (cardW - btnW) / 2;
            int btnY = cardY + 56;
            boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

            graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnHovered ? theme.getButtonHover() : theme.getAccent());

            graphics.renderOutline(btnX, btnY, btnW, btnH, 0xFF60A5FA);

graphics.drawCenteredString(this.font, "+ " + I18n.get("Create New Model", vpLang), btnX + btnW / 2, btnY + 6, 0xFFFFFFFF);
            return;
        }

        boolean isSlim = (propertyInspector != null) && propertyInspector.isSlimModel();

        if (currentClip != null && propertyInspector != null && timelinePanel != null && !timelinePanel.isPlaying()) {
            if ("camera".equals(selectedBone)) {
                try {
                    CameraTrack.CameraKeyframe ckf = currentClip.getCameraTrack().interpolateOrDefault(currentTime);

                    propertyInspector.setCameraProperties(ckf.x(), ckf.y(), ckf.z(), ckf.yaw(), ckf.pitch(), ckf.roll(), ckf.fov());

                    for (var entry : currentClip.getCameraTrack().getKeyframes().entrySet()) {
                        if (Math.abs(entry.getKey() - currentTime) <= 0.05f) {
                            propertyInspector.setCameraSharp(entry.getValue().isSharp());

                            propertyInspector.setCameraHandles(entry.getValue().handles());
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            Map<String, float[]> rots = (currentClip.hasActorPrefixes())

                    ? currentClip.evaluateFrameForActor(currentTime, selectedActorIndex)

                    : currentClip.evaluateFrame(currentTime);

            if (rots.containsKey(selectedBone)) {
                float[] r = rots.get(selectedBone);

                propertyInspector.setBoneRotations(r[0], r[1], r[2]);
            }

            Map<String, float[]> poses = (currentClip.hasActorPrefixes())

                    ? currentClip.evaluatePositionsForActor(currentTime, selectedActorIndex)

                    : currentClip.evaluatePositions(currentTime);

            if (poses.containsKey(selectedBone)) {
                float[] p = poses.get(selectedBone);

                propertyInspector.setBonePositions(p[0], p[1], p[2]);
            }
        }

        if (settings != null && currentClip != null) {
            BoneTrack eyesTrack = currentClip.getBones().get("eyes");

            if (eyesTrack != null) {
                float[] eyePos = eyesTrack.getPositionAt(currentTime);
                float[] eyeRot = eyesTrack.getRotationAt(currentTime);

                if (eyePos != null && eyePos.length >= 2) {
                    settings.setPupilX(eyePos[0]);

                    settings.setPupilY(eyePos[1]);
                }

                if (eyeRot != null && eyeRot.length >= 2) {
                    settings.setLeftEyelid(eyeRot[0]);

                    settings.setRightEyelid(eyeRot[1]);
                }
            }
        }

        graphics.pose().pushPose();
        if (cameraViewMode) {
            float cX = (propertyInspector != null) ? propertyInspector.getCamX() : 0.0f;
            float cY = (propertyInspector != null) ? propertyInspector.getCamY() : 1.4f;
            float cZ = (propertyInspector != null) ? propertyInspector.getCamZ() : -3.2f;
            float cYaw = (propertyInspector != null) ? propertyInspector.getCamYaw() : 0.0f;
            float cPitch = (propertyInspector != null) ? propertyInspector.getCamPitch() : 0.0f;
            float cRoll = (propertyInspector != null) ? propertyInspector.getCamRoll() : 0.0f;
            float cFov = (propertyInspector != null) ? propertyInspector.getCamFov() : 70.0f;
            net.openstage.core.scene.CameraTrack ct = (currentClip != null) ? currentClip.getCameraTrack() : null;
            if (ct != null && !ct.getKeyframes().isEmpty()) {
                try {
                    float evalT = isStaticPoseMode ? 0.0f : currentTime;
                    net.openstage.core.scene.CameraTrack.CameraKeyframe kf = ct.interpolateAt(evalT);
                    cX = kf.x();
                    cY = kf.y();
                    cZ = kf.z();
                    cYaw = kf.yaw();
                    cPitch = kf.pitch();
                    cRoll = kf.roll();
                    cFov = kf.fov();
                } catch (Exception ignored) {}
            }
            float targetAspect = 16.0f / 9.0f;
            float currentAspect = (float) centerW / (float) viewportH;
            float frameH = (currentAspect > targetAspect) ? viewportH : (centerW / targetAspect);
            float zoomScale = (frameH * 0.5f) / (float) Math.tan(Math.toRadians(Math.max(10.0f, cFov) * 0.5f));
            graphics.pose().translate(centerX + centerW * 0.5f, contentY + viewportH * 0.5f, 6000.0f);
            graphics.pose().scale(zoomScale, -zoomScale, zoomScale);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(-cRoll));
            graphics.pose().mulPose(Axis.XP.rotationDegrees(cPitch));
            graphics.pose().mulPose(Axis.YP.rotationDegrees(cYaw + 180.0f));
            graphics.pose().translate(-cX, -cY, -cZ);
        } else {
            float s = 120.0f * (2.0f / Math.max(0.4f, cameraZoom));
            graphics.pose().translate(centerX + centerW * 0.5f + vpPanX * s, contentY + viewportH * 0.5f + 55.0f + vpPanY * s, 6000.0f);
            graphics.pose().scale(s, -s, s);
            graphics.pose().mulPose(Axis.XP.rotationDegrees(vpPitch));
            graphics.pose().mulPose(Axis.YP.rotationDegrees(vpYaw));
        }

        if (bgMode == ModelSettings.BackgroundMode.DEFAULT && isWorldMode() && this.minecraft.level != null && activeWorldAnchor != null && (settings == null || settings.isShowWorldBlocks())) {
            try {
                if (this.minecraft.level.hasChunkAt(activeWorldAnchor)) {
                    graphics.pose().pushPose();
                    WorldEnvironmentRenderer.renderEnvironment(graphics.pose(), graphics.bufferSource(), (ClientLevel) this.minecraft.level, delta, activeWorldAnchor);
                    WorldBlockRenderer.renderSurroundingWorld(graphics.pose(), graphics.bufferSource(), activeWorldAnchor, 32);
                    graphics.bufferSource().endBatch();
                    graphics.pose().popPose();
                } else if (settings == null || settings.isShowGrid()) {
                    renderDirectGrid(graphics, 36, 1.0f);
                }
            } catch (Throwable t) {
                if (settings == null || settings.isShowGrid()) {
                    renderDirectGrid(graphics, 36, 1.0f);
                }
            }
        } else if (settings == null || settings.isShowGrid()) {
            if (bgMode == ModelSettings.BackgroundMode.DEFAULT) {
                renderDirectGrid(graphics, 36, 1.0f);
            }
        }

        Map<String, float[]> radMap = new HashMap<>();
        Map<String, float[]> posMap = (currentClip != null) ? currentClip.evaluatePositions(currentTime) : Collections.emptyMap();

        if (currentClip != null) {
            Map<String, float[]> degMap = currentClip.evaluateFrame(currentTime);

            for (Map.Entry<String, float[]> entry : degMap.entrySet()) {
                float[] deg = entry.getValue();

                if (deg != null && deg.length >= 3) {
                    radMap.put(entry.getKey(), new float[]{
                            (float) Math.toRadians(deg[0]),

                            (float) Math.toRadians(deg[1]),

                            (float) Math.toRadians(deg[2])
                    });
                }
            }
        }

        Lighting.setupForEntityInInventory();

        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        SceneActorRef selActor = (selectedActorIndex >= 0 && selectedActorIndex < nearbyActors.size())

                ? nearbyActors.get(selectedActorIndex) : null;

        float baseRelX = (selActor != null) ? selActor.relX() : 0.0f;
        float baseRelY = (selActor != null) ? selActor.relY() : 0.0f;
        float baseRelZ = (selActor != null) ? selActor.relZ() : 0.0f;

        for (int i = 0; i < nearbyActors.size(); i++) {
            SceneActorRef actor = nearbyActors.get(i);
            boolean isSelActor = (i == selectedActorIndex);

            graphics.pose().pushPose();
            float dx = 0.0f;
            float dy = 0.0f;
            float dz = 0.0f;
            dx = (actor.relX() - baseRelX) / 16.0f;
            dy = -(actor.relY() - baseRelY) / 16.0f;
            dz = (actor.relZ() - baseRelZ) / 16.0f;

            graphics.pose().translate(dx, dy, dz);
            ResourceLocation skinTex = SkinManager.getInstance().getSkinTexture(actor.skin(), actor.slim());
            String boneHighlight = isSelActor ? selectedBone : null;

            if (isSelActor && settings != null && settings.isOnionSkin() && currentClip != null) {
                float prevT = Math.max(0.0f, currentTime - 0.05f);

                if (Math.abs(prevT - currentTime) > 0.001f) {
                    Map<String, float[]> prevRots = toRadiansMap(currentClip.evaluateFrame(prevT));
                    Map<String, float[]> prevPos = currentClip.evaluatePositions(prevT);

                    ActorEntityRenderer.renderGhostModel(
                            graphics.pose(), graphics.bufferSource(), 15728880,

                            prevRots, prevPos, actor.slim(), skinTex, settings, 0x443B82F6
                    );
                }

            }

            Map<String, float[]> actorRadMap;
            Map<String, float[]> actorPosMap;
            ModelSettings actorSettings = isSelActor ? settings : null;

            if (currentClip != null && currentClip.hasActorPrefixes()) {
                actorRadMap = toRadiansMap(currentClip.evaluateFrameForActor(currentTime, i));
                actorPosMap = currentClip.evaluatePositionsForActor(currentTime, i);

                if (!isSelActor && actor.pos() != null && this.minecraft != null && this.minecraft.level != null) {
                    if (this.minecraft.level.getBlockEntity(actor.pos()) instanceof ModelBlockEntity mbe) {
                        ModelSettings ms = new ModelSettings();

                        ms.setRootYaw(mbe.getYaw());

                        ms.setScale(mbe.getScale());

                        ms.setRightHandItem(mbe.getRightHandItem());

                        ms.setLeftHandItem(mbe.getLeftHandItem());
                        actorSettings = ms;
                    }
                }
            } else if (isSelActor) {
                actorRadMap = radMap;
                actorPosMap = posMap;
            } else {
                if (actor.pos() != null && this.minecraft != null && this.minecraft.level != null) {
                    if (this.minecraft.level.getBlockEntity(actor.pos()) instanceof ModelBlockEntity mbe) {
                        if (mbe.isStaticPose()) {
                            actorRadMap = toRadiansMap(mbe.getPoseRotations());
                            actorPosMap = mbe.getPosePositions();
                        } else {
                            AnimationClip otherClip = net.openstage.core.scene.SceneManager.getInstance().getClip(mbe.getClipId());

                            if (otherClip == null && mbe.getClipId() != null) {
                                otherClip = net.openstage.io.AnimationFileManager.getInstance().loadAnimation(mbe.getClipId());

                                if (otherClip == null && !mbe.getClipId().endsWith(".osanim.json")) {
                                    otherClip = net.openstage.io.AnimationFileManager.getInstance().loadAnimation(mbe.getClipId() + ".osanim.json");
                                }

                                if (otherClip != null) {
                                    net.openstage.core.scene.SceneManager.getInstance().loadClip(otherClip.getName(), otherClip);
                                }
                            }

                            if (otherClip == null && actor.clipId() != null) {
                                otherClip = net.openstage.io.AnimationFileManager.getInstance().loadAnimation(actor.clipId());

                                if (otherClip == null && !actor.clipId().endsWith(".osanim.json")) {
                                    otherClip = net.openstage.io.AnimationFileManager.getInstance().loadAnimation(actor.clipId() + ".osanim.json");
                                }

                                if (otherClip != null) {
                                    net.openstage.core.scene.SceneManager.getInstance().loadClip(otherClip.getName(), otherClip);
                                }
                            }

                            if (otherClip != null) {
                                actorRadMap = toRadiansMap(otherClip.evaluateFrame(currentTime));
                                actorPosMap = otherClip.evaluatePositions(currentTime);
                            } else {
                                actorRadMap = Collections.emptyMap();
                                actorPosMap = Collections.emptyMap();
                            }
                        }

                        ModelSettings ms = new ModelSettings();

                        ms.setRootYaw(mbe.getYaw());

                        ms.setScale(mbe.getScale());

                        ms.setRightHandItem(mbe.getRightHandItem());

                        ms.setLeftHandItem(mbe.getLeftHandItem());
                        actorSettings = ms;
                    } else {
                        actorRadMap = Collections.emptyMap();
                        actorPosMap = Collections.emptyMap();
                        actorSettings = null;
                    }
                } else {
                    actorRadMap = Collections.emptyMap();
                    actorPosMap = Collections.emptyMap();
                    actorSettings = null;
                }
            }

            net.openstage.client.model.ObjModel obj = net.openstage.client.model.ObjModelManager.getInstance().getOrLoadModel(actor.skin());

            if (obj == null && actor.name() != null) {
                obj = net.openstage.client.model.ObjModelManager.getInstance().getOrLoadModel(actor.name());
            }

            if (obj != null) {
                ActorEntityRenderer.renderObjModel(
                        graphics.pose(), graphics.bufferSource(), 15728880,

                        obj, actorRadMap, actorPosMap, actorSettings, boneHighlight
                );
            } else {
                ActorEntityRenderer.renderAnimatedPlayerModel(
                        graphics.pose(), graphics.bufferSource(), 15728880,

                        actorRadMap, actorPosMap, actor.slim(), skinTex, actorSettings, boneHighlight
                );
            }

            boolean boneHidden = (actorPosMap != null && actorPosMap.containsKey(selectedBone) && Math.abs(actorPosMap.get(selectedBone)[1]) > 50.0f);
            boolean isSpecialTrack = "camera".equals(selectedBone) || "particles".equals(selectedBone);

            if (isSelActor && !isSpecialTrack && !boneHidden && (settings == null || settings.isShowGizmo())) {
                boolean isBendable = (settings != null) && settings.isBendableLimbs();
                Vector3f effectivePivot;

                if (obj != null) {
                    effectivePivot = new Vector3f(0.0f, (obj.getMinY() + obj.getMaxY()) * 0.5f, 0.0f);

                    if (posMap != null && posMap.containsKey(selectedBone)) {
                        float[] p = posMap.get(selectedBone);

                        if (p != null && p.length >= 3) {
                            effectivePivot.add(p[0], p[1], p[2]);
                        }
                    }
                } else {
                    effectivePivot = GizmoRenderer.getBonePivot(
                            selectedBone, isSlim, isBendable, radMap, posMap, settings
                    );
                }

                graphics.pose().pushPose();

                graphics.pose().translate(effectivePivot.x, effectivePivot.y, effectivePivot.z);
                ModelSettings.GizmoMode gMode = (settings != null) ? settings.getGizmoMode() : ModelSettings.GizmoMode.ROTATE;

                int hoveredAxis = (activeGizmoAxis != -1) ? activeGizmoAxis : GizmoRenderer.testHitAxis(
                        mouseX, mouseY, centerX, contentY, centerW, viewportH,

                        vpPitch, vpYaw, cameraZoom, vpPanX, vpPanY, effectivePivot, 0.35f,

                        0.0f, 1.0f, gMode
                );

                GizmoRenderer.renderGizmo(graphics.pose(), graphics.bufferSource(), 0.35f, hoveredAxis, gMode);

                graphics.pose().popPose();
            }

            graphics.pose().popPose();
        }

        if (currentClip != null && currentClip.getCameraTrack() != null) {
            boolean isCamSelected = "camera".equals(selectedBone);
            net.openstage.core.scene.CameraTrack ct = currentClip.getCameraTrack();
            float evalT = isStaticPoseMode ? 0.0f : currentTime;
            net.openstage.core.scene.CameraTrack.CameraKeyframe kf;
            if (!ct.getKeyframes().isEmpty()) {
                kf = ct.interpolateAt(evalT);
            } else {
                float cx = (propertyInspector != null) ? propertyInspector.getCamX() : 0.0f;
                float cy = (propertyInspector != null) ? propertyInspector.getCamY() : 1.4f;
                float cz = (propertyInspector != null) ? propertyInspector.getCamZ() : -3.2f;
                float cyaw = (propertyInspector != null) ? propertyInspector.getCamYaw() : 0.0f;
                float cpitch = (propertyInspector != null) ? propertyInspector.getCamPitch() : 0.0f;
                float croll = (propertyInspector != null) ? propertyInspector.getCamRoll() : 0.0f;
                float cfov = (propertyInspector != null) ? propertyInspector.getCamFov() : 70.0f;
                kf = new net.openstage.core.scene.CameraTrack.CameraKeyframe(0.0f, cx, cy, cz, cyaw, cpitch, croll, cfov, null);
            }

            if (!cameraViewMode) {
                if (isCamSelected && !ct.getKeyframes().isEmpty()) {
                    CameraFrustumRenderer.renderCameraPath(graphics.pose(), graphics.bufferSource(), ct, currentClip.getLength());
                }
                try {
                    graphics.pose().pushPose();
                    graphics.pose().translate(kf.x(), kf.y(), kf.z());
                    graphics.pose().mulPose(Axis.YP.rotationDegrees(kf.yaw()));
                    graphics.pose().mulPose(Axis.XP.rotationDegrees(kf.pitch()));
                    int frustumColor = isCamSelected ? 0xFFFFCC00 : 0x8838BDF8;
                    CameraFrustumRenderer.renderFrustum(graphics.pose(), graphics.bufferSource(), kf.fov(), 16.0f / 9.0f, 0.18f, 0.65f, frustumColor);
                    graphics.pose().popPose();
                } catch (Exception ignored) {}
            }

            if (isCamSelected && (settings == null || settings.isShowGizmo())) {
                try {
                    graphics.pose().pushPose();
                    graphics.pose().translate(kf.x(), kf.y(), kf.z());
                    ModelSettings.GizmoMode gMode = (settings != null) ? settings.getGizmoMode() : ModelSettings.GizmoMode.ROTATE;

                    int hoveredAxis = (activeGizmoAxis != -1) ? activeGizmoAxis : GizmoRenderer.testHitAxis(
                            mouseX, mouseY, centerX, contentY, centerW, viewportH,
                            vpPitch, vpYaw, cameraZoom, vpPanX, vpPanY, new Vector3f(kf.x(), kf.y(), kf.z()), 0.35f,
                            0.0f, 1.0f, gMode
                    );

                    GizmoRenderer.renderGizmo(graphics.pose(), graphics.bufferSource(), 0.35f, hoveredAxis, gMode);
                    graphics.pose().popPose();
                } catch (Exception ignored) {}
            }
        }

        if (currentClip != null && !currentClip.getParticleTrack().isEmpty()) {
            net.openstage.core.particle.ParticleKeyframe pk = currentClip.getParticleTrack().getKeyframeAt(currentTime);

            if (pk != null) {
                net.openstage.client.particle.ParticlePlaybackService.getInstance().renderEmitterMarker(
                        graphics.pose(), graphics.bufferSource(), pk.posX(), pk.posY(), pk.posZ(), "particles".equals(selectedBone)
                );
            }
        } else if ("particles".equals(selectedBone) && propertyInspector != null) {
            net.openstage.client.particle.ParticlePlaybackService.getInstance().renderEmitterMarker(
                    graphics.pose(), graphics.bufferSource(), propertyInspector.getPartX(), propertyInspector.getPartY(), propertyInspector.getPartZ(), true
            );
        }

        net.openstage.client.particle.ParticlePlaybackService.getInstance().renderViewportParticles(
                graphics.pose(), graphics.bufferSource(), realDt
        );

        if ("particles".equals(selectedBone) && propertyInspector != null) {
            try {
                graphics.pose().pushPose();
                float ppx = propertyInspector.getPartX();
                float ppy = propertyInspector.getPartY();
                float ppz = propertyInspector.getPartZ();

                graphics.pose().translate(ppx, ppy, ppz);

                graphics.pose().mulPose(Axis.YP.rotationDegrees(-propertyInspector.getPartYaw()));

                graphics.pose().mulPose(Axis.XP.rotationDegrees(propertyInspector.getPartPitch()));

                graphics.pose().mulPose(Axis.ZP.rotationDegrees(propertyInspector.getPartRoll()));
                ModelSettings.GizmoMode gMode = (settings != null) ? settings.getGizmoMode() : ModelSettings.GizmoMode.ROTATE;

                int hoveredAxis = (activeGizmoAxis != -1) ? activeGizmoAxis : GizmoRenderer.testHitAxis(
                        mouseX, mouseY, centerX, contentY, centerW, viewportH,

                        vpPitch, vpYaw, cameraZoom, vpPanX, vpPanY, new Vector3f(ppx, ppy, ppz), 0.35f,

                        0.0f, 1.0f, gMode
                );

                GizmoRenderer.renderGizmo(graphics.pose(), graphics.bufferSource(), 0.35f, hoveredAxis, gMode);

                graphics.pose().popPose();
            } catch (Exception ignored) {}
        }

        graphics.bufferSource().endBatch();

        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

        Lighting.setupFor3DItems();

        graphics.pose().popPose();

        if (cameraViewMode) {
            net.openstage.client.model.ModelSettings.Language lang = (getModelSettings() != null) ? getModelSettings().getLanguage() : net.openstage.client.model.ModelSettings.Language.RU;
            float targetAspect = 16.0f / 9.0f;
            float currentAspect = (float) centerW / (float) viewportH;
            int fW, fH, fX, fY;
            if (currentAspect > targetAspect) {
                fH = viewportH;
                fW = (int) (fH * targetAspect);
                fX = centerX + (centerW - fW) / 2;
                fY = contentY;
                graphics.fill(centerX, contentY, fX, contentY + viewportH, 0xCC090D16);
                graphics.fill(fX + fW, contentY, rightX, contentY + viewportH, 0xCC090D16);
            } else {
                fW = centerW;
                fH = (int) (fW / targetAspect);
                fX = centerX;
                fY = contentY + (viewportH - fH) / 2;
                graphics.fill(centerX, contentY, rightX, fY, 0xCC090D16);
                graphics.fill(centerX, fY + fH, rightX, contentY + viewportH, 0xCC090D16);
            }
            graphics.renderOutline(fX, fY, fW, fH, 0xAA38BDF8);
            String camBadge = "\uD83D\uDCF7 16:9 " + I18n.get("Camera Mode", lang);
            int bW = this.font.width(camBadge) + 10;
            graphics.fill(fX + 4, fY + 4, fX + 4 + bW, fY + 18, 0xCC0B0F19);
            graphics.renderOutline(fX + 4, fY + 4, bW, 14, 0xFF38BDF8);
            graphics.drawString(this.font, camBadge, fX + 9, fY + 7, 0xFF38BDF8, false);
        }

        if (currentClip != null) {
            try {
                graphics.bufferSource().endBatch();
                com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);
                com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
                List<TextCard> activeCards = currentClip.getTextTrack().getActiveCardsAt(currentTime);

                for (TextCard card : activeCards) {
                    TextOverlayRenderer.render(graphics, card, currentTime, centerX, contentY, centerW, viewportH);
                }
                graphics.bufferSource().endBatch();
            } catch (Throwable ignored) {}
        }
        } finally {
            graphics.bufferSource().endBatch();
            graphics.disableScissor();
        }
    }

    private void renderStaticPoseBar(GuiGraphics graphics, int mouseX, int mouseY, EditorTheme theme) {
        int barY = contentY + viewportH;

        graphics.fill(centerX, barY, rightX, barY + 24, theme.getPanelBg());

        graphics.fill(centerX, barY, rightX, barY + 1, theme.getDivider());
        int curX = centerX + 10;

        ModelSettings.Language lang = (getModelSettings() != null) ? getModelSettings().getLanguage() : ModelSettings.Language.RU;
        if (selectedBone != null && !selectedBone.isEmpty()) {
            String bName = "camera".equals(selectedBone) ? I18n.get("Camera", lang) : I18n.getBoneDisplayName(selectedBone, lang);
            String selText = I18n.get("Selected: ", lang) + bName;
            graphics.drawString(this.font, selText, curX, barY + 8, theme.getAccent(), false);
            curX += this.font.width(selText) + 12;
        }

        if (isWorldMode()) {
            String wsText = (lang == ModelSettings.Language.RU) ? "Сканирование мира: Активно" : "World Scan: Active";
            String label = (selectedBone != null && !selectedBone.isEmpty()) ? "| ✦ " + wsText : "✦ " + wsText;

            graphics.drawString(this.font, label, curX, barY + 8, 0xFFF59E0B, false);
        }

        int btnW = Math.max(76, this.font.width(I18n.get("Zero Pose", lang)) + 14);
        int btnH = 16;
        int btnX = rightX - btnW - 8;
        int btnY = barY + 4;
        boolean btnHov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnHov ? theme.getButtonHover() : theme.getButtonBg());

        graphics.renderOutline(btnX, btnY, btnW, btnH, btnHov ? theme.getAccent() : theme.getDivider());

        graphics.drawCenteredString(this.font, I18n.get("Zero Pose", lang), btnX + btnW / 2, btnY + 4, btnHov ? 0xFFFFFFFF : theme.getTextColor());
    }

    private void renderDirectGrid(PoseStack poseStack, MultiBufferSource bufferSource, int radius, float spacing) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.gui());
        org.joml.Matrix4f mat = poseStack.last().pose();
        float lineW = 0.015f;

        for (int i = -radius; i <= radius; i++) {
            float coord = i * spacing;
            int color = (i == 0) ? 0xFF5C9DFF : 0x44CBD5E1;

            consumer.addVertex(mat, -radius * spacing, 0.0f, coord - lineW).setColor(color);

            consumer.addVertex(mat, -radius * spacing, 0.0f, coord + lineW).setColor(color);

            consumer.addVertex(mat, radius * spacing, 0.0f, coord + lineW).setColor(color);

            consumer.addVertex(mat, radius * spacing, 0.0f, coord - lineW).setColor(color);

            consumer.addVertex(mat, coord - lineW, 0.0f, -radius * spacing).setColor(color);

            consumer.addVertex(mat, coord - lineW, 0.0f, radius * spacing).setColor(color);

            consumer.addVertex(mat, coord + lineW, 0.0f, radius * spacing).setColor(color);

            consumer.addVertex(mat, coord + lineW, 0.0f, -radius * spacing).setColor(color);
        }
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.gui());
        }
    }

    private void renderScene3DDirect(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,

                                    float currentTime, float delta, boolean forExport) {
        ModelSettings settings = (propertyInspector != null) ? propertyInspector.getModelSettings() : null;

        ModelSettings.BackgroundMode bgMode = (settings != null) ? settings.getBackgroundMode() : ModelSettings.BackgroundMode.DEFAULT;
        if (bgMode == ModelSettings.BackgroundMode.DEFAULT && isWorldMode() && (settings == null || settings.isShowWorldBlocks())) {
            poseStack.pushPose();

            WorldEnvironmentRenderer.renderEnvironment(poseStack, bufferSource, (ClientLevel) this.minecraft.level, delta, targetBlockPos);

            WorldBlockRenderer.renderSurroundingWorld(poseStack, bufferSource, targetBlockPos, 32);

            bufferSource.endBatch();

            poseStack.popPose();
        } else if (settings == null || settings.isShowGrid()) {
            if (bgMode == ModelSettings.BackgroundMode.DEFAULT) {
                renderDirectGrid(poseStack, bufferSource, 36, 1.0f);
                bufferSource.endBatch();
            }
        }

        Map<String, float[]> radMap = new HashMap<>();
        Map<String, float[]> posMap = (currentClip != null) ? currentClip.evaluatePositions(currentTime) : Collections.emptyMap();

        if (currentClip != null) {
            Map<String, float[]> degMap = currentClip.evaluateFrame(currentTime);

            for (Map.Entry<String, float[]> entry : degMap.entrySet()) {
                float[] deg = entry.getValue();

                if (deg != null && deg.length >= 3) {
                    radMap.put(entry.getKey(), new float[]{
                            (float) Math.toRadians(deg[0]),

                            (float) Math.toRadians(deg[1]),

                            (float) Math.toRadians(deg[2])
                    });
                }
            }
        }

        SceneActorRef selActor = (selectedActorIndex >= 0 && selectedActorIndex < nearbyActors.size())

                ? nearbyActors.get(selectedActorIndex) : null;

        float baseRelX = (selActor != null) ? selActor.relX() : 0.0f;
        float baseRelY = (selActor != null) ? selActor.relY() : 0.0f;
        float baseRelZ = (selActor != null) ? selActor.relZ() : 0.0f;

        for (int i = 0; i < nearbyActors.size(); i++) {
            SceneActorRef actor = nearbyActors.get(i);
            boolean isSelActor = (i == selectedActorIndex);

            poseStack.pushPose();
            float dx = (actor.relX() - baseRelX) / 16.0f;
            float dy = -(actor.relY() - baseRelY) / 16.0f;
            float dz = (actor.relZ() - baseRelZ) / 16.0f;

            poseStack.translate(dx, dy, dz);
            ResourceLocation skinTex = SkinManager.getInstance().getSkinTexture(actor.skin(), actor.slim());
            String boneHighlight = (!forExport && isSelActor) ? selectedBone : null;
            Map<String, float[]> actorRadMap;
            Map<String, float[]> actorPosMap;
            ModelSettings actorSettings = isSelActor ? settings : null;

            if (currentClip != null && currentClip.hasActorPrefixes()) {
                actorRadMap = toRadiansMap(currentClip.evaluateFrameForActor(currentTime, i));
                actorPosMap = currentClip.evaluatePositionsForActor(currentTime, i);
            } else if (isSelActor) {
                actorRadMap = radMap;
                actorPosMap = posMap;
            } else {
                actorRadMap = Collections.emptyMap();
                actorPosMap = Collections.emptyMap();
            }

            net.openstage.client.model.ObjModel obj = net.openstage.client.model.ObjModelManager.getInstance().getOrLoadModel(actor.skin());

            if (obj == null && actor.name() != null) {
                obj = net.openstage.client.model.ObjModelManager.getInstance().getOrLoadModel(actor.name());
            }

            if (obj != null) {
                ActorEntityRenderer.renderObjModel(
                        poseStack, bufferSource, 15728880,

                        obj, actorRadMap, actorPosMap, actorSettings, boneHighlight
                );
            } else {
                ActorEntityRenderer.renderAnimatedPlayerModel(
                        poseStack, bufferSource, 15728880,

                        actorRadMap, actorPosMap, actor.slim(), skinTex, actorSettings, boneHighlight
                );
            }

            poseStack.popPose();
        }

        net.openstage.client.particle.ParticlePlaybackService.getInstance().renderViewportParticles(poseStack, bufferSource, delta);
    }

    private void renderDirectGrid(GuiGraphics graphics, int radius, float spacing) {
        VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
        org.joml.Matrix4f mat = graphics.pose().last().pose();
        float lineW = 0.015f;

        for (int i = -radius; i <= radius; i++) {
            float coord = i * spacing;
            int color = (i == 0) ? 0xFF5C9DFF : 0x44CBD5E1;

            consumer.addVertex(mat, -radius * spacing, 0.0f, coord - lineW).setColor(color);

            consumer.addVertex(mat, -radius * spacing, 0.0f, coord + lineW).setColor(color);

            consumer.addVertex(mat, radius * spacing, 0.0f, coord + lineW).setColor(color);

            consumer.addVertex(mat, radius * spacing, 0.0f, coord - lineW).setColor(color);

            consumer.addVertex(mat, coord - lineW, 0.0f, -radius * spacing).setColor(color);

            consumer.addVertex(mat, coord - lineW, 0.0f, radius * spacing).setColor(color);

            consumer.addVertex(mat, coord + lineW, 0.0f, radius * spacing).setColor(color);

            consumer.addVertex(mat, coord + lineW, 0.0f, -radius * spacing).setColor(color);
        }
        graphics.bufferSource().endBatch(RenderType.gui());
    }

    private void launchStaticCameraMode() {
        if (currentClip != null) {
            CameraTrack ct = currentClip.getCameraTrack();
            float cx = (propertyInspector != null) ? propertyInspector.getCamX() : 0.0f;
            float cy = (propertyInspector != null) ? propertyInspector.getCamY() : 1.4f;
            float cz = (propertyInspector != null) ? propertyInspector.getCamZ() : -3.2f;
            float cyaw = (propertyInspector != null) ? propertyInspector.getCamYaw() : 0.0f;
            float cpitch = (propertyInspector != null) ? propertyInspector.getCamPitch() : 0.0f;
            float croll = (propertyInspector != null) ? propertyInspector.getCamRoll() : 0.0f;
            float cfov = (propertyInspector != null) ? propertyInspector.getCamFov() : 70.0f;

            if (isStaticPoseMode || ct.getKeyframes().isEmpty()) {
                ct.addKey(new CameraTrack.CameraKeyframe(0.0f, cx, cy, cz, cyaw, cpitch, croll, cfov, null));
            }
        }

        List<BlockPos> actorList = new ArrayList<>();
        for (SceneActorRef a : nearbyActors) {
            if (a.pos() != null) actorList.add(a.pos());
        }

        float evalTime = isStaticPoseMode ? 0.0f : (timelinePanel != null ? timelinePanel.getCurrentTimeSeconds() : 0.0f);
        autoSavePoseToBlock(true);
        CinematicPlaybackHandler.getInstance().startStatic(currentClip, targetBlockPos, actorList, this, evalTime);
        this.onClose();
    }

    private void renderTopToolbar(GuiGraphics graphics, int mouseX, int mouseY, EditorTheme theme) {
        graphics.fill(0, 0, this.width, TITLE_BAR_HEIGHT, theme.getTitleBarBg());

        graphics.drawString(this.font, "OpenStage", 8, 7, theme.getAccent(), false);

        graphics.fill(0, TITLE_BAR_HEIGHT - 1, this.width, TITLE_BAR_HEIGHT, 0xFF1E293B);
        ModelSettings.Language lang = (propertyInspector != null) ? propertyInspector.getModelSettings().getLanguage() : ModelSettings.Language.EN;
        int menuX = 76;
        int menuH = 16;
        int menuY = 3;
        String fileText = I18n.get("File", lang) + " ▾";
        int fileW = this.font.width(fileText) + 12;

        drawMenuButton(graphics, fileText, menuX, menuY, fileW, menuH, activeMenu == MenuType.FILE, mouseX, mouseY, theme); menuX += fileW + 4;
        String setText = I18n.get("Settings", lang);
        int setW = this.font.width(setText) + 14;

        drawMenuButton(graphics, setText, menuX, menuY, setW, menuH, false, mouseX, mouseY, theme); menuX += setW + 4;
        int rightControlsX = this.width - 4;

        if (hasModel) {
            String camBtn = (lang == ModelSettings.Language.RU ? "📷 Камера" : "📷 Camera");
            int camW = this.font.width(camBtn) + 12;
            rightControlsX -= camW;
            drawToolbarButton(graphics, camBtn, rightControlsX, menuY, camW, menuH, false, mouseX, mouseY, theme);
            rightControlsX -= 4;
        }

        if (!isStaticPoseMode) {
            int cutsceneW = this.font.width(I18n.get("🎬 Cutscene", lang)) + 12;
            rightControlsX -= cutsceneW;
            drawToolbarButton(graphics, I18n.get("🎬 Cutscene", lang), rightControlsX, menuY, cutsceneW, menuH, false, mouseX, mouseY, theme);
            rightControlsX -= 4;

            if (hasModel) {
                int fpsW = 50;
                rightControlsX -= fpsW;
                drawToolbarButton(graphics, FPS_OPTIONS[fpsIndex] + " FPS", rightControlsX, menuY, fpsW, menuH, mouseX, mouseY, theme);
                rightControlsX -= 4;
            }
        }

    }

    private void drawDisabledButton(GuiGraphics graphics, String text, int bx, int by, int bw, int bh, EditorTheme theme) {
        graphics.fill(bx, by, bx + bw, by + bh, 0xFF141A24);

        graphics.renderOutline(bx, by, bw, bh, 0xFF222B38);
        int tw = this.font.width(text);
        int tx = bx + (bw - tw) / 2;

        graphics.drawString(this.font, text, tx, by + 4, 0xFF555566, false);
    }

    private void drawMenuButton(GuiGraphics graphics, String text, int bx, int by, int bw, int bh, boolean open, int mx, int my, EditorTheme theme) {
        boolean hovered = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;

        if (open) {
            graphics.fill(bx, by, bx + bw, by + bh, 0xFF1E293B);

            graphics.renderOutline(bx, by, bw, bh, theme.getAccent());

            graphics.fill(bx + 2, by + bh - 2, bx + bw - 2, by + bh, theme.getAccent());
        } else if (hovered) {
            graphics.fill(bx, by, bx + bw, by + bh, 0x25FFFFFF);

            graphics.renderOutline(bx, by, bw, bh, 0x4060A5FA);
        }

        int tw = this.font.width(text);
        int tx = bx + (bw - tw) / 2;
        int col = open ? 0xFFFFFFFF : (hovered ? 0xFFFFFFFF : 0xFFCBD5E1);

        graphics.drawString(this.font, text, tx, by + 4, col, false);
    }

    private int getDropdownMenuX(MenuType menu) {
        ModelSettings.Language lang = (propertyInspector != null) ? propertyInspector.getModelSettings().getLanguage() : ModelSettings.Language.EN;
        int x = 76;

        if (menu == MenuType.FILE || menu == MenuType.IMPORT) return x;
        x += this.font.width(I18n.get("File", lang) + " ▾") + 16;

        if (menu == MenuType.FOLDER || menu == MenuType.EXPORT) return x;
        return 76;
    }

    private List<MenuItem> getActiveMenuItems() {
        ModelSettings.Language lang = (propertyInspector != null) ? propertyInspector.getModelSettings().getLanguage() : ModelSettings.Language.EN;
        List<MenuItem> items = new ArrayList<>();

        switch (activeMenu) {

            case FILE -> {
                items.add(new MenuItem(I18n.get("New Model... (Ctrl+N)", lang), true, () -> {
                    setActiveMenu(MenuType.NONE);
                    openCreateModelDialog();
                }));

                items.add(new MenuItem(I18n.get("Import Model (.geo.json, .bbmodel, .obj)...", lang), true, () -> {
                    setActiveMenu(MenuType.NONE);
                    modelImportModal.open();
                }));

                items.add(new MenuItem(I18n.get("Export Video (.mp4 / PNG)...", lang), hasModel, () -> {
                    setActiveMenu(MenuType.NONE);
                    videoExportModal.open();
                }));

                items.add(new MenuItem(lang == ModelSettings.Language.RU ? "📁 Папка анимаций" : "📁 Animations Folder", true, () -> {
                    setActiveMenu(MenuType.NONE);
                    AnimationFileManager.getInstance().openAnimationsFolder();
                }));

                items.add(new MenuItem(lang == ModelSettings.Language.RU ? "📁 Папка поз" : "📁 Poses Folder", true, () -> {
                    setActiveMenu(MenuType.NONE);
                    AnimationFileManager.getInstance().openPosesFolder();
                }));

                items.add(new MenuItem(lang == ModelSettings.Language.RU ? "📁 Папка скинов" : "📁 Skins Folder", true, () -> {
                    setActiveMenu(MenuType.NONE);
                    SkinManager.getInstance().openSkinsFolder();
                }));
            }

            case IMPORT -> {
                items.add(new MenuItem("📦 3D Модель (.geo.json, .bbmodel, .obj)...", true, () -> {
                    setActiveMenu(MenuType.NONE);

                    modelImportModal.open();
                }));

                items.add(new MenuItem("📂 Папка анимаций...", true, () -> {
                    setActiveMenu(MenuType.NONE);

                    AnimationFileManager.getInstance().openAnimationsFolder();
                }));
            }

            case EXPORT -> {
                items.add(new MenuItem("🎬 Экспорт в видео (.mp4 / PNG)...", true, () -> {
                    setActiveMenu(MenuType.NONE);

                    videoExportModal.open();
                }));

                items.add(new MenuItem("💾 Сохранить проект (Ctrl+S)", hasModel, () -> {
                    setActiveMenu(MenuType.NONE);

                    if (hasModel && currentClip != null && currentFileName != null) {
                        AnimationFileManager.getInstance().saveAnimation(currentClip, currentFileName);

                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.player.displayClientMessage(Component.literal("§a[OpenStage] Project saved: " + currentFileName), true);
                        }
                    }
                }));
            }

            default -> {}
        }

        return items;
    }

    private void renderDropdownMenu(GuiGraphics graphics, int mouseX, int mouseY, EditorTheme theme) {
        MenuType menu = (activeMenu != MenuType.NONE) ? activeMenu : closingMenu;

        if (menu == MenuType.NONE) return;

        graphics.bufferSource().endBatch();

        com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);

        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();
        long now = System.currentTimeMillis();
        float ease = 1.0f;

        if (activeMenu == MenuType.NONE && closingMenu != MenuType.NONE) {
            float p = anims ? Math.min(1.0f, (now - menuCloseTime) / 100.0f) : 1.0f;
            ease = (float) Math.pow(1.0f - p, 2);

            if (p >= 1.0f) {
                closingMenu = MenuType.NONE;

                return;
            }
        } else {
            float p = anims ? Math.min(1.0f, (now - menuOpenTime) / 110.0f) : 1.0f;
            ease = 1.0f - (float) Math.pow(1.0f - p, 2);
        }

        float scaleY = anims ? (0.85f + 0.15f * ease) : 1.0f;

        graphics.pose().pushPose();

        graphics.pose().translate(0, 0, 500.0f);
        int dw = 190;
        int dx = getDropdownMenuX(menu);
        int dy = TITLE_BAR_HEIGHT + 2;

        graphics.pose().translate(dx, dy, 0);

        graphics.pose().scale(1.0f, scaleY, 1.0f);

        graphics.pose().translate(-dx, -dy, 0);
        List<MenuItem> items = getActiveMenuItems();
        int itemH = 20;
        int dh = items.size() * itemH + 8;

        for (MenuItem item : items) {
            int w = this.font.width(item.label) + 28;

            if (w > dw) dw = w;
        }

        graphics.fill(dx - 3, dy - 1, dx + dw + 3, dy + dh + 4, 0x18000000);

        graphics.fill(dx - 2, dy, dx + dw + 2, dy + dh + 3, 0x2A000000);

        graphics.fill(dx - 1, dy + 1, dx + dw + 1, dy + dh + 2, 0x40000000);

        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF0F172A);

        graphics.renderOutline(dx, dy, dw, dh, 0xFF334155);
        int iy = dy + 4;

        for (MenuItem item : items) {
            boolean hov = mouseX >= dx && mouseX <= dx + dw && mouseY >= iy && mouseY <= iy + itemH;

            if (hov && item.enabled) {
                graphics.fill(dx + 3, iy, dx + dw - 3, iy + itemH, 0xFF1E293B);

                graphics.fill(dx + 3, iy + 2, dx + 5, iy + itemH - 2, theme.getAccent());
            }

            String raw = item.label;
            String label = raw;
            String shortcut = "";
            int paren = raw.lastIndexOf('(');

            if (paren > 0 && raw.endsWith(")")) {
                label = raw.substring(0, paren).trim();
                shortcut = raw.substring(paren + 1, raw.length() - 1).trim();
            }

            int labelCol = item.enabled ? (hov ? 0xFFFFFFFF : 0xFFE2E8F0) : 0xFF64748B;

            graphics.drawString(this.font, label, dx + 10, iy + 6, labelCol, false);

            if (!shortcut.isEmpty()) {
                int scW = this.font.width(shortcut);
                int scCol = item.enabled ? (hov ? 0xFF93C5FD : 0xFF64748B) : 0xFF475569;

                graphics.drawString(this.font, shortcut, dx + dw - 10 - scW, iy + 6, scCol, false);
            }

            iy += itemH;
        }

        graphics.bufferSource().endBatch();

        graphics.pose().popPose();

        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }

    private void copyBoneRotation() {
        if (currentClip != null && timelinePanel != null) {
            float time = timelinePanel.getCurrentTimeSeconds();
            Map<String, float[]> rots = currentClip.evaluateFrame(time);

            if (rots.containsKey(selectedBone)) {
                float[] r = rots.get(selectedBone);
                clipboardRotation = new float[]{r[0], r[1], r[2]};

                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(
                            Component.literal("§7[OpenStage] Copied " + selectedBone + " rotation: [" + String.format("%.1f, %.1f, %.1f", r[0], r[1], r[2]) + "]"), true);
                }
            }
        }
    }

    private void pasteBoneRotation() {
        if (clipboardRotation != null && currentClip != null && timelinePanel != null) {
            float time = timelinePanel.getCurrentTimeSeconds();
            if (selectedBone == null || selectedBone.isEmpty()) selectedBone = "body";
            BoneTrack track = currentClip.getOrCreateBone(selectedBone);
            EasingType easing = (propertyInspector != null) ? propertyInspector.getCurrentEasing() : EasingType.LINEAR;

            track.addKeyframe(ChannelType.ROTATION, new Keyframe(time, new float[]{clipboardRotation[0], clipboardRotation[1], clipboardRotation[2]}, easing, null));

            if (propertyInspector != null) {
                propertyInspector.setBoneRotations(clipboardRotation[0], clipboardRotation[1], clipboardRotation[2]);
            }

            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.literal("§a[OpenStage] Pasted rotation to " + selectedBone), true);
            }
        }
    }

    private String getLocalizedBoneName(String bone, ModelSettings.Language lang) {
        boolean ru = (lang == ModelSettings.Language.RU);

        return switch (bone) {
            case "root" -> ru ? "Полное тело" : "Full Body";
            case "body" -> ru ? "Торс" : "Torso";
            case "head" -> ru ? "Голова" : "Head";
            case "right_arm" -> ru ? "Прав. рука" : "Right Arm";
            case "left_arm" -> ru ? "Лев. рука" : "Left Arm";
            case "right_leg" -> ru ? "Прав. нога" : "Right Leg";
            case "left_leg" -> ru ? "Лев. нога" : "Left Leg";
            case "right_forearm" -> ru ? "Прав. предпл." : "Right Forearm";
            case "left_forearm" -> ru ? "Лев. предпл." : "Left Forearm";
            case "right_lower_leg" -> ru ? "Прав. голень" : "Right Lower Leg";
            case "left_lower_leg" -> ru ? "Лев. голень" : "Left Lower Leg";
            case "eyes" -> ru ? "Глаза" : "Eyes";
            case "camera" -> ru ? "Камера" : "Camera";
            default -> bone;
        };
    }

    private void renderLeftPanel(GuiGraphics graphics, int mouseX, int mouseY, EditorTheme theme) {
        if (leftW <= 2) return;
        graphics.fill(leftX, contentY, leftX + leftW, this.height, theme.getPanelBg());

        graphics.enableScissor(leftX, contentY, leftX + leftW, this.height);
        int py = contentY + 8;
        ModelSettings.Language lang = (propertyInspector != null) ? propertyInspector.getModelSettings().getLanguage() : ModelSettings.Language.EN;

        graphics.drawString(this.font, I18n.get("SCENE ACTORS", lang) + " (" + nearbyActors.size() + ")", leftX + 8, py, theme.getTextMuted(), false);
        py += 16;

        if (!hasModel || nearbyActors.isEmpty()) {
            graphics.drawString(this.font, I18n.get("No active actors", lang), leftX + 10, py, 0xFF64748B, false);
            py += 16;
            int btnW = Math.max(50, leftW - 20);
            int btnH = 16;

            drawToolbarButton(graphics, "+ " + I18n.get("Add Actor", lang), leftX + 10, py, btnW, btnH, mouseX, mouseY, theme);
            py += 20;

            drawToolbarButton(graphics, "+ " + I18n.get("Create Project", lang), leftX + 10, py, btnW, btnH, mouseX, mouseY, theme);
            py += 22;
        } else {
            for (int i = 0; i < nearbyActors.size(); i++) {
                SceneActorRef actor = nearbyActors.get(i);
                boolean isSel = (i == selectedActorIndex);

                if (isSel) {
                    graphics.fill(leftX + 4, py - 2, leftX + leftW - 4, py + 12, theme.getAccent());
                }

                int textColor = isSel ? 0xFFFFFFFF : theme.getTextColor();
                String prefix = isSel ? "▼ " : "▶ ";

                String actorDisplayName = I18n.get(actor.name(), lang);
                graphics.drawString(this.font, prefix + actorDisplayName, leftX + 8, py, textColor, false);

                int delX = leftX + leftW - 16;
                boolean delHov = mouseX >= delX && mouseX <= delX + 12 && mouseY >= py - 2 && mouseY <= py + 10;
                int delCol = delHov ? 0xFFF87171 : (isSel ? 0xFFE2E8F0 : 0xFF94A3B8);
                graphics.drawString(this.font, "✕", delX, py, delCol, false);

                py += 14;

                if (isSel) {
                    String[] bones = (timelinePanel != null) ? timelinePanel.getActiveTrackBones() : TimelinePanel.TRACK_BONES;

                    for (String bone : bones) {
                        boolean boneSel = bone.equals(selectedBone);
                        boolean ghosted = (getModelSettings() != null) && getModelSettings().isBoneGhosted(bone);

                        int eyeX = leftX + leftW - 16;
                        if (boneSel) {
                            graphics.fill(leftX + 6, py - 2, eyeX - 3, py + 12, 0x33FFFFFF);
                        }

                        int bCol = ghosted ? 0xFF64748B : (boneSel ? 0xFFFFCC00 : 0xFF94A3B8);
                        String displayName = getLocalizedBoneName(bone, lang);
                        int maxTextW = Math.max(10, eyeX - (leftX + 24));
                        String drawName = this.font.plainSubstrByWidth(displayName, maxTextW);

                        graphics.drawString(this.font, "  ◆ " + drawName, leftX + 12, py, bCol, false);
                        int eyeY = py;
                        boolean eyeHov = mouseX >= eyeX - 2 && mouseX <= eyeX + 13 && mouseY >= eyeY - 1 && mouseY <= eyeY + 11;

                        drawEyeIcon(graphics, eyeX, eyeY, !ghosted, eyeHov);
                        py += 13;
                    }

                    boolean camSel = "camera".equals(selectedBone);

                    if (camSel) {
                        graphics.fill(leftX + 6, py - 2, leftX + leftW - 6, py + 12, 0x33FFFFFF);
                    }

                    String camText = (lang == ModelSettings.Language.RU) ? "🎥 Камера" : "🎥 Camera";
                    int maxCamW = Math.max(10, (leftX + leftW - 8) - (leftX + 22));

                    graphics.drawString(this.font, "  " + this.font.plainSubstrByWidth(camText, maxCamW), leftX + 12, py, camSel ? 0xFFFFCC00 : 0xFF38BDF8, false);
                    py += 13;
                    }
            }

            py += 4;
            int addBtnW = leftW - 16;

            drawToolbarButton(graphics, "+ " + I18n.get("Add Actor", lang), leftX + 8, py, addBtnW, 16, mouseX, mouseY, theme);
            py += 20;

        }

        py += 12;

        graphics.drawString(this.font, I18n.get("Saved Projects", lang), leftX + 8, py, theme.getTextMuted(), false);
        py += 16;
        List<String> animFiles = AnimationFileManager.getInstance().listAnimationFiles();
        List<String> poseFiles = AnimationFileManager.getInstance().listPoseFiles();

        if (animFiles.isEmpty() && poseFiles.isEmpty()) {
            graphics.drawString(this.font, I18n.get("No files found", lang), leftX + 10, py, 0xFF64748B, false);
        } else {
            if (!animFiles.isEmpty()) {
                graphics.drawString(this.font, "🎬 " + (lang == ModelSettings.Language.RU ? "Анимации" : "Animations"), leftX + 8, py, 0xFF60A5FA, false);
                py += 12;
                for (int i = 0; i < Math.min(animFiles.size(), 6); i++) {
                    String f = animFiles.get(i);
                    boolean active = f.equalsIgnoreCase(currentFileName);
                    if (active) {
                        graphics.fill(leftX + 4, py - 2, leftX + leftW - 4, py + 12, 0xFF1E3A8A);
                    }
                    graphics.drawString(this.font, "📄 " + f, leftX + 10, py, active ? 0xFF93C5FD : 0xFF94A3B8, false);
                    py += 14;
                }
            }
            if (!poseFiles.isEmpty()) {
                py += 4;
                graphics.drawString(this.font, "✦ " + (lang == ModelSettings.Language.RU ? "Позы" : "Poses"), leftX + 8, py, 0xFFA78BFA, false);
                py += 12;
                for (int i = 0; i < Math.min(poseFiles.size(), 6); i++) {
                    String f = poseFiles.get(i);
                    boolean active = f.equalsIgnoreCase(currentFileName);
                    if (active) {
                        graphics.fill(leftX + 4, py - 2, leftX + leftW - 4, py + 12, 0xFF1E3A8A);
                    }
                    graphics.drawString(this.font, "✦ " + f, leftX + 10, py, active ? 0xFF93C5FD : 0xFF94A3B8, false);
                    py += 14;
                }
            }
        }

        graphics.disableScissor();
    }

    private void drawToolbarButton(GuiGraphics graphics, String text, int bx, int by, int bw, int bh, int mx, int my, EditorTheme theme) {
        drawToolbarButton(graphics, text, bx, by, bw, bh, false, mx, my, theme);
    }

    private void drawToolbarButton(GuiGraphics graphics, String text, int bx, int by, int bw, int bh, boolean active, int mx, int my, EditorTheme theme) {
        boolean hovered = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        int bg = active ? 0xFF1D4ED8 : (hovered ? theme.getButtonHover() : theme.getButtonBg());

        graphics.fill(bx, by, bx + bw, by + bh, bg);

        graphics.renderOutline(bx, by, bw, bh, active ? 0xFF60A5FA : (hovered ? theme.getAccent() : theme.getDivider()));
        int tw = this.font.width(text);
        int tx = bx + (bw - tw) / 2;

        graphics.drawString(this.font, text, tx, by + 4, (active || hovered) ? 0xFFFFFFFF : theme.getTextColor(), false);
    }

    @Override

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isProjectContextMenuOpen) {
            ModelSettings.Language lang = (propertyInspector != null) ? propertyInspector.getModelSettings().getLanguage() : ModelSettings.Language.EN;
            List<MenuItem> items = getProjectContextMenuItems(lang);
            int itemH = 18;
            int dw = 140;
            for (MenuItem it : items) {
                int w = this.font.width(it.label) + 20;
                if (w > dw) dw = w;
            }
            int dh = items.size() * itemH + 6;
            int dx = Math.min(contextMenuX, this.width - dw - 4);
            int dy = Math.min(contextMenuY, this.height - dh - 4);

            if (mouseX >= dx && mouseX <= dx + dw && mouseY >= dy && mouseY <= dy + dh) {
                int idx = (int) ((mouseY - dy - 3) / itemH);
                if (idx >= 0 && idx < items.size()) {
                    MenuItem it = items.get(idx);
                    if (it.action != null) it.action.run();
                }
                isProjectContextMenuOpen = false;
                return true;
            }
            isProjectContextMenuOpen = false;
            if (button == 1) return true;
        }

        if (videoExportModal.isVisible()) {
            return videoExportModal.mouseClicked(mouseX, mouseY, button, this.width, this.height);
        }

        if (skinPickerModal.isVisible()) {
            return skinPickerModal.mouseClicked(mouseX, mouseY, button, this.width, this.height);
        }

        if (modelImportModal.isVisible()) {
            return modelImportModal.mouseClicked(mouseX, mouseY, button, this.width, this.height);
        }

        if (showingGraph && graphEditorPanel != null && graphEditorPanel.getCustomizerModal().isVisible()) {
            return graphEditorPanel.getCustomizerModal().mouseClicked(mouseX, mouseY, button);
        }

        if (propertyInspector != null && propertyInspector.isItemPickerOpen()) {
            return propertyInspector.mouseClicked(mouseX, mouseY, button);
        }

        if (settingsDialog != null && settingsDialog.isOpen()) {
            return settingsDialog.mouseClicked(mouseX, mouseY, button);
        }

        if (isEditingAudioTrack) {
            long elapsed = System.currentTimeMillis() - audioTrackOpenTime;
            if (elapsed < 30) return true;
            int dw = 320;
            int dh = 150;
            int dx = (this.width - dw) / 2;
            int dy = (this.height - dh) / 2;

            int closeX = dx + dw - 18;
            if (mouseX >= closeX - 2 && mouseX <= closeX + 14 && mouseY >= dy + 4 && mouseY <= dy + 18) {
                closeAudioTrackDialog();
                return true;
            }

            int btnY1 = dy + 66;
            int btnW = 140;
            int btnH = 20;
            if (isInside((int) mouseX, (int) mouseY, dx + 14, btnY1, btnW, btnH)) {
                net.openstage.client.audio.AudioPlaybackService.getInstance().resetTrim();
                return true;
            }
            if (isInside((int) mouseX, (int) mouseY, dx + 166, btnY1, btnW, btnH)) {
                net.openstage.client.audio.AudioPlaybackService.getInstance().openFileDialog();
                closeAudioTrackDialog();
                return true;
            }

            int btnY2 = dy + 92;
            if (isInside((int) mouseX, (int) mouseY, dx + 14, btnY2, btnW, btnH)) {
                net.openstage.client.audio.AudioPlaybackService.getInstance().deleteSelectedSegment();
                closeAudioTrackDialog();
                return true;
            }
            if (isInside((int) mouseX, (int) mouseY, dx + 166, btnY2, btnW, btnH)) {
                net.openstage.client.audio.AudioPlaybackService.getInstance().removeTrack();
                closeAudioTrackDialog();
                return true;
            }

            int doneY = dy + 120;
            int doneW = 80;
            int doneX = dx + (dw - doneW) / 2;
            if (isInside((int) mouseX, (int) mouseY, doneX, doneY, doneW, 20)) {
                closeAudioTrackDialog();
                return true;
            }
            return true;
        }

        if (isEditingTextCard) {
            long elapsed = System.currentTimeMillis() - textCardOpenTime;
            if (elapsed < 30) return true;
            float progress = Math.min(1.0f, elapsed / 170.0f);
            float ease = 1.0f - (float) Math.pow(1.0f - progress, 3);
            float scale = 0.88f + 0.12f * ease;
            int dw = 320;
            int dh = 150;
            int dx = (this.width - dw) / 2;
            int dy = (this.height - dh) / 2;
            int cx = dx + dw / 2;
            int cy = dy + dh / 2;
            double effMx = (progress < 1.0f && scale > 0.001f) ? (mouseX - cx) / scale + cx : mouseX;
            double effMy = (progress < 1.0f && scale > 0.001f) ? (mouseY - cy) / scale + cy : mouseY;

            if (isInside((int) effMx, (int) effMy, dx + 14, dy + 62, 140, 18)) {
                if (activeEditingTextCard != null) {
                    net.openstage.core.animation.TextCard.PositionPreset[] p = net.openstage.core.animation.TextCard.PositionPreset.values();
                    activeEditingTextCard.setPosition(p[(activeEditingTextCard.getPosition().ordinal() + 1) % p.length]);
                }
                return true;
            }

            if (isInside((int) effMx, (int) effMy, dx + 164, dy + 62, 142, 18)) {
                if (activeEditingTextCard != null) {
                    net.openstage.core.animation.TextCard.AnimationEffect[] eff = net.openstage.core.animation.TextCard.AnimationEffect.values();
                    activeEditingTextCard.setEffect(eff[(activeEditingTextCard.getEffect().ordinal() + 1) % eff.length]);
                }
                return true;
            }

            int pbtnW = 24;
            int pbtnY = dy + 88;
            if (isInside((int) effMx, (int) effMy, dx + 14, pbtnY, pbtnW, 14)) {
                if (activeEditingTextCard != null) {
                    activeEditingTextCard.setTextColor(0xFFFFFFFF);
                    activeEditingTextCard.setBackgroundColor(0x88000000);
                }
                return true;
            }
            if (isInside((int) effMx, (int) effMy, dx + 14 + 28, pbtnY, pbtnW, 14)) {
                if (activeEditingTextCard != null) {
                    activeEditingTextCard.setTextColor(0xFF00F0FF);
                    activeEditingTextCard.setBackgroundColor(0xAA051020);
                }
                return true;
            }
            if (isInside((int) effMx, (int) effMy, dx + 14 + 56, pbtnY, pbtnW, 14)) {
                if (activeEditingTextCard != null) {
                    activeEditingTextCard.setTextColor(0xFFFF007F);
                    activeEditingTextCard.setBackgroundColor(0xAA200010);
                }
                return true;
            }
            if (isInside((int) effMx, (int) effMy, dx + 14 + 84, pbtnY, pbtnW, 14)) {
                if (activeEditingTextCard != null) {
                    activeEditingTextCard.setTextColor(0xFFF59E0B);
                    activeEditingTextCard.setBackgroundColor(0xAA201500);
                }
                return true;
            }
            if (isInside((int) effMx, (int) effMy, dx + 14 + 112, pbtnY, pbtnW, 14)) {
                if (activeEditingTextCard != null) {
                    activeEditingTextCard.setTextColor(0xFF22C55E);
                    activeEditingTextCard.setBackgroundColor(0xAA002005);
                }
                return true;
            }

            int btnY = dy + 116;

            if (isInside((int) effMx, (int) effMy, dx + 14, btnY, 60, 20)) {
                if (activeEditingTextCard != null) {
                    onDeleteTextCardRequested(activeEditingTextCard);
                }
                closeTextCardDialog();
                return true;
            }

            if (isInside((int) effMx, (int) effMy, dx + dw - 146, btnY, 68, 20)) {
                confirmTextCardEdit();
                return true;
            }

            if (isInside((int) effMx, (int) effMy, dx + dw - 72, btnY, 58, 20)) {
                closeTextCardDialog();
                return true;
            }

            return true;
        }

        if (isCreatingNewModel) {
            long elapsed = System.currentTimeMillis() - createModelOpenTime;

            if (elapsed < 30) return true;
            float progress = Math.min(1.0f, elapsed / 170.0f);
            float ease = 1.0f - (float) Math.pow(1.0f - progress, 3);
            float scale = 0.88f + 0.12f * ease;
            int dw = 320;
            int dh = 150;
            int dx = (this.width - dw) / 2;
            int dy = (this.height - dh) / 2;
            int cx = dx + dw / 2;
            int cy = dy + dh / 2;
            double effMx = (progress < 1.0f && scale > 0.001f) ? (mouseX - cx) / scale + cx : mouseX;
            double effMy = (progress < 1.0f && scale > 0.001f) ? (mouseY - cy) / scale + cy : mouseY;

            if (effMx < dx || effMx > dx + dw || effMy < dy || effMy > dy + dh) {
                closeCreateModelDialog();

                return true;
            }

            int radioY = dy + 52;

            if (effMx >= dx + 14 && effMx <= dx + dw - 14 && effMy >= radioY && effMy <= radioY + 24) {
                newModelIsPose = true;

                return true;
            }

            int radio2Y = radioY + 28;

            if (effMx >= dx + 14 && effMx <= dx + dw - 14 && effMy >= radio2Y && effMy <= radio2Y + 24) {
                newModelIsPose = false;

                return true;
            }

            int btnY = dy + dh - 26;

            if (isInside((int) effMx, (int) effMy, dx + dw - 130, btnY, 56, 18)) {
                confirmCreateModel();

                return true;
            }

            if (isInside((int) effMx, (int) effMy, dx + dw - 66, btnY, 56, 18)) {
                closeCreateModelDialog();

                return true;
            }

            return true;
        }

        if (isRenamingProject) {
            long elapsed = System.currentTimeMillis() - renameOpenTime;

            if (elapsed < 30) return true;
            float progress = Math.min(1.0f, elapsed / 170.0f);
            float ease = 1.0f - (float) Math.pow(1.0f - progress, 3);
            float scale = 0.88f + 0.12f * ease;
            int dw = 320;
            int dh = 110;
            int dx = (this.width - dw) / 2;
            int dy = (this.height - dh) / 2;
            int cx = dx + dw / 2;
            int cy = dy + dh / 2;
            double effMx = (progress < 1.0f && scale > 0.001f) ? (mouseX - cx) / scale + cx : mouseX;
            double effMy = (progress < 1.0f && scale > 0.001f) ? (mouseY - cy) / scale + cy : mouseY;
            int btnY = dy + 74;

            if (isInside((int) effMx, (int) effMy, dx + dw - 130, btnY, 56, 18)) {
                confirmProjectRename();

                return true;
            }

            if (isInside((int) effMx, (int) effMy, dx + dw - 66, btnY, 56, 18)) {
                closeRenameDialog();

                return true;
            }

            return true;
        }

        if (activeMenu != MenuType.NONE) {
            int dx = getDropdownMenuX(activeMenu);
            List<MenuItem> items = getActiveMenuItems();
            int itemH = 20;
            int dh = items.size() * itemH + 8;
            int dw = 190;

            for (MenuItem it : items) {
                int w = this.font.width(it.label) + 28;

                if (w > dw) dw = w;
            }

            int dy = TITLE_BAR_HEIGHT + 2;

            if (mouseX >= dx && mouseX <= dx + dw && mouseY >= dy && mouseY <= dy + dh) {
                int itemIdx = (int) ((mouseY - dy - 4) / itemH);

                if (itemIdx >= 0 && itemIdx < items.size()) {
                    MenuItem item = items.get(itemIdx);

                    if (item.enabled && item.action != null) {
                        item.action.run();
                    }
                }

                activeMenu = MenuType.NONE;

                return true;
            }

            activeMenu = MenuType.NONE;
        }

        if (mouseY < contentY) {
            ModelSettings.Language lang = (propertyInspector != null) ? propertyInspector.getModelSettings().getLanguage() : ModelSettings.Language.EN;
            int menuX = 76;
            int menuH = 16;
            int menuY = 3;
            String fileText = I18n.get("File", lang) + " ▾";
            int fileW = this.font.width(fileText) + 12;

            if (isInside(mouseX, mouseY, menuX, menuY, fileW, menuH)) {
                setActiveMenu((activeMenu == MenuType.FILE) ? MenuType.NONE : MenuType.FILE);

                return true;
            }

            menuX += fileW + 4;
            String setText = I18n.get("Settings", lang);
            int setW = this.font.width(setText) + 14;

            if (isInside(mouseX, mouseY, menuX, menuY, setW, menuH)) {
                setActiveMenu(MenuType.NONE);

                if (settingsDialog != null) settingsDialog.open();

                return true;
            }

            menuX += setW + 4;

            handleToolbarRightClick(mouseX, mouseY);

            return true;
        }

        int toggleSize = getRightPanelToggleSize();
        int toggleX = getRightPanelToggleX();
        int toggleY = getRightPanelToggleY();

        if (isInside(mouseX, mouseY, toggleX, toggleY, toggleSize, toggleSize)) {
            if (!rightPanelCollapsed) {
                savedRightW = Math.max(130, rightW);
            }

            rightPanelCollapsed = !rightPanelCollapsed;
            rightPanelTarget = rightPanelCollapsed ? 0.0f : 1.0f;
            boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();

            if (!anims) {
                rightPanelAnim = rightPanelTarget;
            }

            recalculateLayout();
            recalculateLayoutAfterResize();

            return true;
        }

        if (isHoveringLeftSplitter((int) mouseX, (int) mouseY)) {
            draggingSplitter = DraggingSplitter.LEFT;

            return true;
        }

        if (isHoveringRightSplitter((int) mouseX, (int) mouseY)) {
            draggingSplitter = DraggingSplitter.RIGHT;

            return true;
        }

        if (isHoveringHorizontalSplitter((int) mouseX, (int) mouseY)) {
            draggingSplitter = DraggingSplitter.HORIZONTAL;

            return true;
        }

        if (mouseX < leftW) {
            handleLeftPanelClick(mouseX, mouseY, button);

            return true;
        }

        if (rightW > 20 && mouseX >= rightX) {
            if (propertyInspector != null) {
                return propertyInspector.mouseClicked(mouseX, mouseY, button);
            }
        }

        if (mouseY >= contentY + viewportH) {
            if (isStaticPoseMode) {
                int barY = contentY + viewportH;
                int zBtnW = 76;
                int zBtnH = 16;
                int zBtnX = rightX - zBtnW - 8;
                int zBtnY = barY + 4;

                if (isInside(mouseX, mouseY, zBtnX, zBtnY, zBtnW, zBtnH)) {
                    zeroAllBones();

                    return true;
                }

                return true;
            } else if (showingGraph) {
                if (graphEditorPanel != null) return graphEditorPanel.mouseClicked(mouseX, mouseY, button);
            } else {
                if (timelinePanel != null) return timelinePanel.mouseClicked(mouseX, mouseY, button);
            }
        }

        if (mouseX >= centerX && mouseX <= rightX && mouseY >= contentY && mouseY <= contentY + viewportH) {
            if (!hasModel) {
                int cardW = 270;
                int cardH = 92;
                int cardX = centerX + (centerW - cardW) / 2;
                int cardY = contentY + (viewportH - cardH) / 2;
                int btnW = 160;
                int btnH = 20;
                int btnX = cardX + (cardW - btnW) / 2;
                int btnY = cardY + 56;

                if (isInside(mouseX, mouseY, btnX, btnY, btnW, btnH)) {
                    openCreateModelDialog();

                    return true;
                }

                draggingViewport = true;
                lastMouseX = mouseX;
                lastMouseY = mouseY;

                return true;
            }

            boolean isBendable = (propertyInspector != null) && propertyInspector.getModelSettings().isBendableLimbs();
            boolean isSlimModel = (propertyInspector != null) && propertyInspector.isSlimModel();
            float time = (timelinePanel != null) ? timelinePanel.getCurrentTimeSeconds() : 0.0f;
            Map<String, float[]> curPoses = (currentClip != null) ? currentClip.evaluatePositions(time) : null;
            Map<String, float[]> curRots = (currentClip != null) ? currentClip.evaluateFrame(time) : null;
            ModelSettings ms = (propertyInspector != null) ? propertyInspector.getModelSettings() : null;
            float rootYaw = (ms != null) ? ms.getRootYaw() : 0.0f;
            float modelScale = (ms != null) ? ms.getScale() : 1.0f;
            net.openstage.client.model.ObjModel curObj = null;
            SceneActorRef curActor = (!nearbyActors.isEmpty() && selectedActorIndex >= 0 && selectedActorIndex < nearbyActors.size()) ? nearbyActors.get(selectedActorIndex) : null;

            if (curActor != null) {
                curObj = net.openstage.client.model.ObjModelManager.getInstance().getOrLoadModel(curActor.skin());

                if (curObj == null && curActor.name() != null) {
                    curObj = net.openstage.client.model.ObjModelManager.getInstance().getOrLoadModel(curActor.name());
                }
            }

            Vector3f effectivePivot;

            if (curObj != null) {
                float yOff = (curObj.getMinY() < 0.0f) ? -curObj.getMinY() : 0.0f;
                effectivePivot = new Vector3f(0.0f, (curObj.getMinY() + curObj.getMaxY()) * 0.5f + yOff, 0.0f);

                if (curPoses != null) {
                    float[] p = curPoses.get(selectedBone);

                    if (p == null && "body".equals(selectedBone)) p = curPoses.get("root");

                    if (p == null && "root".equals(selectedBone)) p = curPoses.get("body");

                    if (p != null && p.length >= 3) {
                        effectivePivot.add(p[0], p[1], p[2]);
                    }
                }
            } else {
                effectivePivot = GizmoRenderer.getBonePivot(
                        selectedBone, isSlimModel, isBendable, curRots, curPoses, ms
                );
            }

            if ("camera".equals(selectedBone) && currentClip != null) {
                currentClip.getCameraTrack().ensureDefaultKeyframe();
                try {
                    CameraTrack.CameraKeyframe ckf = currentClip.getCameraTrack().interpolateAt(time);
                    effectivePivot = new Vector3f(ckf.x(), ckf.y(), ckf.z());
                } catch (Exception ignored) {}
            } else if ("particles".equals(selectedBone) && propertyInspector != null) {
                effectivePivot = new Vector3f(propertyInspector.getPartX(), propertyInspector.getPartY(), propertyInspector.getPartZ());
            }

            ModelSettings.GizmoMode gMode = (propertyInspector != null) ? propertyInspector.getModelSettings().getGizmoMode() : ModelSettings.GizmoMode.ROTATE;

            int hitAxis = GizmoRenderer.testHitAxis(
                    mouseX, mouseY, centerX, contentY, centerW, viewportH,

                    cameraPitch, cameraYaw, cameraZoom, cameraPanX, cameraPanY, effectivePivot, 0.35f,

                    0.0f, 1.0f, gMode
            );

            if (hitAxis >= 0) {
                captureUndoBefore();
                this.activeGizmoAxis = hitAxis;

                return true;
            }

            if (button == 0) {
                if ("camera".equals(selectedBone)) {
                    draggingViewport = true;
                    lastMouseX = mouseX;
                    lastMouseY = mouseY;
                    return true;
                }

                net.openstage.core.scene.CameraTrack ct = (currentClip != null) ? currentClip.getCameraTrack() : null;
                if (ct != null) {
                    try {
                        CameraTrack.CameraKeyframe ckf = ct.interpolateOrDefault(time);
                        if (ModelRaycaster.testHitCamera(mouseX, mouseY, centerX, contentY, centerW, viewportH, cameraPitch, cameraYaw, cameraZoom, cameraPanX, cameraPanY, ckf.x(), ckf.y(), ckf.z())) {
                            this.selectedBone = "camera";
                            if (propertyInspector != null) {
                                propertyInspector.setSelectedBone("camera");
                                propertyInspector.setCameraProperties(ckf.x(), ckf.y(), ckf.z(), ckf.yaw(), ckf.pitch(), ckf.roll(), ckf.fov());
                            }
                            return true;
                        }
                    } catch (Exception ignored) {}
                }

                String hitBone;

                if (curObj != null) {
                    float yOff = (curObj.getMinY() < 0.0f) ? -curObj.getMinY() : 0.0f;
                    hitBone = ModelRaycaster.pickObjModel(
                            mouseX, mouseY, centerX, contentY, centerW, viewportH,
                            cameraPitch, cameraYaw, cameraZoom, cameraPanX, cameraPanY,
                            rootYaw, modelScale, curObj.getMinX(), curObj.getMinY() + yOff, curObj.getMinZ(),
                            curObj.getMaxX(), curObj.getMaxY() + yOff, curObj.getMaxZ(),

                            (curPoses != null ? (curPoses.containsKey(selectedBone) ? curPoses.get(selectedBone) : curPoses.get("root")) : null),

                            (curRots != null ? (curRots.containsKey(selectedBone) ? curRots.get(selectedBone) : curRots.get("root")) : null)
                    );
                } else {
                    hitBone = ModelRaycaster.pickBone(
                            mouseX, mouseY, centerX, contentY, centerW, viewportH,

                            cameraPitch, cameraYaw, cameraZoom, cameraPanX, cameraPanY,

                            rootYaw, modelScale, isSlimModel, isBendable, curRots, curPoses,

                            (getModelSettings() != null) ? getModelSettings().getGhostedBones() : null
                    );
                }

                if (hitBone != null) {
                    this.selectedBone = hitBone;

                    if (propertyInspector != null) {
                        propertyInspector.setSelectedBone(hitBone);

                        if (curRots != null && curRots.containsKey(hitBone)) {
                            float[] r = curRots.get(hitBone);

                            propertyInspector.setBoneRotations(r[0], r[1], r[2]);
                        } else {
                            propertyInspector.setBoneRotations(0, 0, 0);
                        }

                        if (curPoses != null && curPoses.containsKey(hitBone)) {
                            float[] p = curPoses.get(hitBone);

                            propertyInspector.setBonePositions(p[0], p[1], p[2]);
                        } else {
                            propertyInspector.setBonePositions(0, 0, 0);
                        }
                    }

                    if (timelinePanel != null) {
                        timelinePanel.setModelTreeExpanded(true);
                    }

                    if (graphEditorPanel != null) {
                        graphEditorPanel.setSelectedBone(hitBone);
                    }

                    return true;
                }
            }

            draggingViewport = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void zeroAllBones() {
        if (currentClip == null) return;

        for (String b : TimelinePanel.TRACK_BONES) {
            BoneTrack bt = currentClip.getOrCreateBone(b);

            bt.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, new float[]{0, 0, 0}, EasingType.LINEAR, null));
        }

        if (propertyInspector != null) {
            propertyInspector.setBoneRotations(0, 0, 0);
        }
    }

    private void handleToolbarRightClick(double mx, double my) {
        int menuY = 3;
        int menuH = 16;
        int rightControlsX = this.width - 4;
        net.openstage.client.model.ModelSettings.Language lang = (getModelSettings() != null) ? getModelSettings().getLanguage() : net.openstage.client.model.ModelSettings.Language.RU;

        if (hasModel) {
            String camBtn = (lang == ModelSettings.Language.RU ? "📷 Камера" : "📷 Camera");
            int camW = this.font.width(camBtn) + 12;
            rightControlsX -= camW;
            if (isInside(mx, my, rightControlsX, menuY, camW, menuH)) {
                launchStaticCameraMode();
                return;
            }
            rightControlsX -= 4;
        }

        if (!isStaticPoseMode) {
            int cutsceneW = this.font.width(I18n.get("🎬 Cutscene", lang)) + 12;
            rightControlsX -= cutsceneW;
            if (isInside(mx, my, rightControlsX, menuY, cutsceneW, menuH)) {
                if (currentClip == null || currentClip.getLength() <= 0.05f) {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.displayClientMessage(Component.literal("§e[OpenStage] " + (lang == ModelSettings.Language.RU ? "Анимация пуста (длина 0 сек)" : "Animation is empty (0s duration)")), true);
                    }
                    return;
                }
                List<BlockPos> actorList = new ArrayList<>();
                for (SceneActorRef a : nearbyActors) {
                    if (a.pos() != null) actorList.add(a.pos());
                }
                autoSavePoseToBlock(true);
                CinematicPlaybackHandler.getInstance().start(currentClip, targetBlockPos, actorList, this);
                this.onClose();
                return;
            }
            rightControlsX -= 4;

            if (hasModel) {
                int fpsW = 50;
                rightControlsX -= fpsW;
                if (isInside(mx, my, rightControlsX, menuY, fpsW, menuH)) {
                    fpsIndex = (fpsIndex + 1) % FPS_OPTIONS.length;
                    if (timelinePanel != null) timelinePanel.setFps(FPS_OPTIONS[fpsIndex]);
                    return;
                }
                rightControlsX -= 4;
            }
        }
    }

    private void applyAnimationToBlock() {
        if (targetBlockPos == null || !hasModel) return;
        String skin = (propertyInspector != null) ? propertyInspector.getCurrentSkin() : "steve";
        boolean isSlim = (propertyInspector != null) && propertyInspector.isSlimModel();
        float yaw = (propertyInspector != null) ? propertyInspector.getModelSettings().getRootYaw() : 0.0f;
        float scale = (propertyInspector != null) ? propertyInspector.getModelSettings().getScale() : 1.0f;
        String clipId = (currentClip != null) ? currentClip.getName() : "idle";

        if (currentClip != null && currentFileName != null) {
            net.openstage.core.scene.SceneManager.getInstance().loadClip(clipId, currentClip);

            AnimationFileManager.getInstance().saveAnimation(currentClip, currentFileName);
        }

        SceneActorRef curActorUpd = (!nearbyActors.isEmpty() && selectedActorIndex >= 0 && selectedActorIndex < nearbyActors.size()) ? nearbyActors.get(selectedActorIndex) : null;
        String curModelIdUpd = (curActorUpd != null) ? ((curActorUpd.skin() != null && !curActorUpd.skin().isEmpty()) ? curActorUpd.skin() : curActorUpd.name()) : currentModelName;
        if (curModelIdUpd == null) curModelIdUpd = "player:steve";
        ClientPlayNetworking.send(new OpenStageNetwork.UpdateModelBlockPayload(
                targetBlockPos, curModelIdUpd, skin, isSlim, clipId, yaw, scale
        ));
        if (this.minecraft != null && this.minecraft.level != null && targetBlockPos != null) {
            if (this.minecraft.level.getBlockEntity(targetBlockPos) instanceof ModelBlockEntity mbe) {
                mbe.setModelId(curModelIdUpd);
                mbe.setSkin(skin);
                mbe.setSlim(isSlim);
                mbe.setClipId(clipId);
                mbe.setYaw(yaw);
                mbe.setScale(scale);
                mbe.setStaticPose(false);
            }
        }

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.literal("§a[OpenStage] Applied animation to Model Block!"), true);
        }
    }

    public void loadProjectFile(String fileName) {
        if (fileName == null || fileName.isBlank()) return;
        autoSaveProject();
        AnimationClip loaded = AnimationFileManager.getInstance().loadAnimation(fileName);

        if (loaded != null) {
            autoSavePoseToBlock(true);
            this.currentFileName = fileName;
            this.currentClip = loaded;
            this.isStaticPoseMode = fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".ospose.json");
            String baseName = fileName.replace(".osanim.json", "").replace(".ospose.json", "");
            this.currentModelName = baseName;
            this.hasModel = true;

            SceneActorRef firstActor = this.nearbyActors.isEmpty()
                    ? new SceneActorRef(targetBlockPos, currentModelName, 0, 0, 0, "steve", false, "idle")
                    : new SceneActorRef(this.nearbyActors.get(0).pos(), currentModelName, 0, 0, 0, this.nearbyActors.get(0).skin(), this.nearbyActors.get(0).slim(), this.nearbyActors.get(0).clipId());
            this.nearbyActors.clear();
            this.nearbyActors.add(firstActor);
            this.selectedActorIndex = 0;
            if (timelinePanel != null) {
                timelinePanel.setSelectedActorIndex(0);
            }

            if (timelinePanel != null) {
                timelinePanel.setModelName(currentModelName);

                timelinePanel.setClip(loaded);

                timelinePanel.seek(0);

                timelinePanel.setModelTreeExpanded(true);
            }

            if (graphEditorPanel != null) {
                graphEditorPanel.setClip(loaded);
            }

            if (this.minecraft != null && this.minecraft.player != null) {
                ModelSettings.Language l = (propertyInspector != null) ? propertyInspector.getModelSettings().getLanguage() : ModelSettings.Language.EN;

                this.minecraft.player.displayClientMessage(
                    Component.literal("§a[OpenStage] §f" + I18n.get("Saved Projects", l) + ": " + fileName), true);
            }

            recalculateLayout();

            recalculateLayoutAfterResize();
        }
    }

    private void handleLeftPanelClick(double mx, double my, int button) {
        int py = contentY + 8 + 16;

        if (!hasModel || nearbyActors.isEmpty()) {
            int btnW = Math.max(50, leftW - 20);
            int btnH = 16;

            if (isInside(mx, my, leftX + 10, py + 16, btnW, btnH)) {
                this.isAddingNewActor = true;

                this.modelImportModal.open();

                return;
            }

            if (isInside(mx, my, leftX + 10, py + 36, btnW, btnH)) {
                isCreatingNewModel = true;
                newModelNameInput = "MyModel";
                newModelIsPose = false;

                return;
            }

            py += 58;
        } else {
            for (int i = 0; i < nearbyActors.size(); i++) {
                SceneActorRef actor = nearbyActors.get(i);
                boolean isSel = (i == selectedActorIndex);

                if (my >= py - 2 && my <= py + 12) {
                    int delX = leftX + leftW - 16;
                    if (mx >= delX - 2 && mx <= delX + 14) {
                        nearbyActors.remove(i);
                        if (nearbyActors.isEmpty()) {
                            hasModel = false;
                            currentClip = null;
                            currentFileName = null;
                            selectedActorIndex = 0;
                            if (timelinePanel != null) {
                                timelinePanel.setClip(null);
                                timelinePanel.setSelectedActorIndex(0);
                            }
                        } else {
                            if (selectedActorIndex >= nearbyActors.size()) {
                                selectedActorIndex = nearbyActors.size() - 1;
                            }
                            if (timelinePanel != null) {
                                timelinePanel.setSelectedActorIndex(selectedActorIndex);
                            }
                        }
                        return;
                    }
                    if (this.selectedActorIndex != i) {
                        autoSavePoseToBlock(true);
                        this.selectedActorIndex = i;
                        this.targetBlockPos = actor.pos();
                        this.currentModelName = actor.name();

                        if (timelinePanel != null) {
                            timelinePanel.setSelectedActorIndex(i);
                        }

                        if (this.minecraft != null && this.minecraft.level != null && actor.pos() != null) {
                            if (this.minecraft.level.getBlockEntity(actor.pos()) instanceof ModelBlockEntity mbe) {
                                if (propertyInspector != null) {
                                    propertyInspector.setCurrentSkin(mbe.getSkin());

                                    propertyInspector.setSlimModel(mbe.isSlim());

                                    propertyInspector.getModelSettings().setRootYaw(mbe.getYaw());

                                    propertyInspector.getModelSettings().setScale(mbe.getScale());

                                    propertyInspector.getModelSettings().setRightHandItem(mbe.getRightHandItem());

                                    propertyInspector.getModelSettings().setLeftHandItem(mbe.getLeftHandItem());
                                }

                                if (mbe.isStaticPose()) {
                                    AnimationClip poseClip = new AnimationClip(actor.name(), 0.0f);

                                    for (var entry : mbe.getPoseRotations().entrySet()) {
                                        BoneTrack bt = poseClip.getOrCreateBone(entry.getKey());

                                        bt.addKeyframe(ChannelType.ROTATION, new Keyframe(0.0f, entry.getValue().clone(), EasingType.LINEAR, null));
                                    }

                                    for (var entry : mbe.getPosePositions().entrySet()) {
                                        BoneTrack bt = poseClip.getOrCreateBone(entry.getKey());

                                        bt.addKeyframe(ChannelType.POSITION, new Keyframe(0.0f, entry.getValue().clone(), EasingType.LINEAR, null));
                                    }

                                    this.currentClip = poseClip;

                                    if (timelinePanel != null) {
                                        timelinePanel.setClip(poseClip);
                                    }
                                } else {
                                    AnimationClip c = net.openstage.core.scene.SceneManager.getInstance().getClip(mbe.getClipId());

                                    if (c != null) {
                                        this.currentClip = c;

                                        if (timelinePanel != null) {
                                            timelinePanel.setClip(c);
                                        }
                                    }
                                }
                            }
                        }

                        if (propertyInspector != null && currentClip != null) {
                            float time = (timelinePanel != null) ? timelinePanel.getCurrentTimeSeconds() : 0.0f;
                            Map<String, float[]> curRots = currentClip.evaluateFrame(time);
                            Map<String, float[]> curPoses = currentClip.evaluatePositions(time);

                            if (curRots.containsKey(selectedBone)) {
                                float[] r = curRots.get(selectedBone);

                                propertyInspector.setBoneRotations(r[0], r[1], r[2]);
                            } else {
                                propertyInspector.setBoneRotations(0, 0, 0);
                            }

                            if (curPoses.containsKey(selectedBone)) {
                                float[] p = curPoses.get(selectedBone);

                                propertyInspector.setBonePositions(p[0], p[1], p[2]);
                            } else {
                                propertyInspector.setBonePositions(0, 0, 0);
                            }
                        }
                    }

                    return;
                }

                py += 14;

                if (isSel) {
                    String[] bones = (timelinePanel != null) ? timelinePanel.getActiveTrackBones() : TimelinePanel.TRACK_BONES;

                    for (String bone : bones) {
                        if (my >= py - 2 && my <= py + 12) {
                            int eyeX = leftX + leftW - 16;

                            if (mx >= eyeX - 2 && mx <= eyeX + 14) {
                                if (getModelSettings() != null) {
                                    getModelSettings().toggleBoneGhosted(bone);

                                    if (getModelSettings().isBoneGhosted(bone) && bone.equals(selectedBone)) {
                                        this.selectedBone = null;

                                        if (propertyInspector != null) propertyInspector.setSelectedBone(null);
                                    }
                                }

                                return;
                            }

                            if (getModelSettings() != null && getModelSettings().isBoneGhosted(bone)) {
                                return;
                            }

                            this.selectedBone = bone;

                            if (propertyInspector != null) {
                                propertyInspector.setSelectedBone(bone);

                                if (currentClip != null && timelinePanel != null) {
                                    float time = timelinePanel.getCurrentTimeSeconds();
                                    Map<String, float[]> rots = currentClip.evaluateFrame(time);

                                    if (rots.containsKey(bone)) {
                                        float[] r = rots.get(bone);

                                        propertyInspector.setBoneRotations(r[0], r[1], r[2]);
                                    } else {
                                        propertyInspector.setBoneRotations(0, 0, 0);
                                    }
                                }
                            }

                            if (graphEditorPanel != null) graphEditorPanel.setSelectedBone(bone);

                            return;
                        }

                        py += 13;
                    }

                    if (my >= py - 2 && my <= py + 12) {
                        this.selectedBone = "camera";

                        if (propertyInspector != null) {
                            propertyInspector.setSelectedBone("camera");

                            if (currentClip != null) {
                                try {
                                    float time = (timelinePanel != null) ? timelinePanel.getCurrentTimeSeconds() : 0.0f;
                                    CameraTrack.CameraKeyframe ckf = currentClip.getCameraTrack().interpolateOrDefault(time);

                                    propertyInspector.setCameraProperties(ckf.x(), ckf.y(), ckf.z(), ckf.yaw(), ckf.pitch(), ckf.roll(), ckf.fov());
                                } catch (Exception ignored) {}
                            }
                        }

                        return;
                    }

                    py += 13;
                }
            }

            py += 4;
            int addBtnW = leftW - 16;

            if (isInside(mx, my, leftX + 8, py, addBtnW, 16)) {
                this.isAddingNewActor = true;

                this.modelImportModal.open();

                return;
            }

            py += 20;

        }

        py += 12;
        py += 16;
        java.util.List<String> animFiles = AnimationFileManager.getInstance().listAnimationFiles();
        java.util.List<String> poseFiles = AnimationFileManager.getInstance().listPoseFiles();

        if (!animFiles.isEmpty()) {
            py += 12;
            for (int i = 0; i < Math.min(animFiles.size(), 6); i++) {
                String f = animFiles.get(i);
                if (mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= py - 2 && my <= py + 12) {
                    if (button == 1) {
                        this.contextProjectFile = f;
                        this.contextMenuX = Math.min((int) mx, this.width - 150);
                        this.contextMenuY = Math.min((int) my, this.height - 110);
                        this.isProjectContextMenuOpen = true;
                        this.contextMenuOpenTime = System.currentTimeMillis();
                    } else {
                        loadProjectFile(f);
                    }
                    return;
                }
                py += 14;
            }
        }
        if (!poseFiles.isEmpty()) {
            py += 4;
            py += 12;
            for (int i = 0; i < Math.min(poseFiles.size(), 6); i++) {
                String f = poseFiles.get(i);
                if (mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= py - 2 && my <= py + 12) {
                    if (button == 1) {
                        this.contextProjectFile = f;
                        this.contextMenuX = Math.min((int) mx, this.width - 150);
                        this.contextMenuY = Math.min((int) my, this.height - 110);
                        this.isProjectContextMenuOpen = true;
                        this.contextMenuOpenTime = System.currentTimeMillis();
                    } else {
                        loadProjectFile(f);
                    }
                    return;
                }
                py += 14;
            }
        }
    }

    @Override

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (showingGraph && graphEditorPanel != null && graphEditorPanel.getCustomizerModal().isVisible()) {
            return graphEditorPanel.getCustomizerModal().mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        if (draggingSplitter == DraggingSplitter.LEFT) {
            int maxLeft = Math.max(160, this.width - (rightW > 20 ? rightW : 0) - 80);
            leftW = Math.max(60, Math.min(maxLeft, (int) mouseX));
            centerX = leftW;
            centerW = this.width - leftW - rightW;

            recalculateLayout();

            recalculateLayoutAfterResize();

            return true;
        }

        if (draggingSplitter == DraggingSplitter.RIGHT) {
        if (rightW > 20) {
                rightW = Math.max(130, Math.min(this.width - leftW - 120, this.width - (int) mouseX));

                recalculateLayoutAfterResize();
            }

            return true;
        }

        if (draggingSplitter == DraggingSplitter.HORIZONTAL) {
            int contentH = this.height - contentY;
            viewportH = Math.max(60, Math.min(contentH - 60, (int) mouseY - contentY));
            timelineH = contentH - viewportH;

            recalculateLayoutAfterResize();

            return true;
        }

        if (activeGizmoAxis != -1) {
            ModelSettings ms = (propertyInspector != null) ? propertyInspector.getModelSettings() : null;
            ModelSettings.GizmoMode gMode = (ms != null) ? ms.getGizmoMode() : ModelSettings.GizmoMode.ROTATE;

            if ("camera".equals(selectedBone) && propertyInspector != null) {
                float cx = propertyInspector.getCamX();
                float cy = propertyInspector.getCamY();
                float cz = propertyInspector.getCamZ();
                float cyaw = propertyInspector.getCamYaw();
                float cpitch = propertyInspector.getCamPitch();
                float croll = propertyInspector.getCamRoll();
                float cfov = propertyInspector.getCamFov();
                float radY = (float) Math.toRadians(cameraYaw);
                float radP = (float) Math.toRadians(cameraPitch);
                float cosY = (float) Math.cos(radY);
                float sinY = (float) Math.sin(radY);
                float cosP = (float) Math.cos(radP);
                float sinP = (float) Math.sin(radP);

                if (gMode == ModelSettings.GizmoMode.ROTATE) {
                    float deltaAngle = (float) (dragX - dragY) * 1.0f;

                    if (activeGizmoAxis == 0) cpitch += deltaAngle;

                    if (activeGizmoAxis == 1) cyaw += deltaAngle;

                    if (activeGizmoAxis == 2) croll += deltaAngle;
                } else {
                    float speed = 0.015f / Math.max(0.4f, cameraZoom);

                    if (activeGizmoAxis == 3) {
                        float rightX = cosY;
                        float rightZ = sinY;
                        float upX = -sinP * sinY;
                        float upY = cosP;
                        float upZ = sinP * cosY;
                        cx += (float) (dragX * rightX - dragY * upX) * speed;
                        cy += (float) (-dragY * upY) * speed;
                        cz += (float) (dragX * rightZ - dragY * upZ) * speed;
                    } else if (activeGizmoAxis == 0) {
                        cx += (float) (dragX * cosY - dragY * sinY) * speed;
                    } else if (activeGizmoAxis == 1) {
                        cy -= (float) dragY * speed;
                    } else if (activeGizmoAxis == 2) {
                        cz += (float) (dragX * sinY + dragY * cosY) * speed;
                    }
                }

                propertyInspector.setCameraProperties(cx, cy, cz, cyaw, cpitch, croll, cfov);

                onCameraChanged(cx, cy, cz, cyaw, cpitch, croll, cfov);

                return true;
            }

            if ("particles".equals(selectedBone) && propertyInspector != null) {
                float ppx = propertyInspector.getPartX();
                float ppy = propertyInspector.getPartY();
                float ppz = propertyInspector.getPartZ();
                float pyaw = propertyInspector.getPartYaw();
                float ppitch = propertyInspector.getPartPitch();
                float proll = propertyInspector.getPartRoll();
                float radY = (float) Math.toRadians(cameraYaw);
                float radP = (float) Math.toRadians(cameraPitch);
                float cosY = (float) Math.cos(radY);
                float sinY = (float) Math.sin(radY);
                float cosP = (float) Math.cos(radP);
                float sinP = (float) Math.sin(radP);

                if (gMode == ModelSettings.GizmoMode.ROTATE) {
                    float deltaAngle = (float) (dragX - dragY) * 1.0f;

                    if (activeGizmoAxis == 0) ppitch += deltaAngle;

                    if (activeGizmoAxis == 1) pyaw += deltaAngle;

                    if (activeGizmoAxis == 2) proll += deltaAngle;
                } else {
                    float speed = 0.015f / Math.max(0.4f, cameraZoom);

                    if (activeGizmoAxis == 3) {
                        float rightX = cosY;
                        float rightZ = sinY;
                        float upX = -sinP * sinY;
                        float upY = cosP;
                        float upZ = sinP * cosY;
                        ppx += (float) (dragX * rightX - dragY * upX) * speed;
                        ppy += (float) (-dragY * upY) * speed;
                        ppz += (float) (dragX * rightZ - dragY * upZ) * speed;
                    } else if (activeGizmoAxis == 0) {
                        ppx += (float) (dragX * cosY - dragY * sinY) * speed;
                    } else if (activeGizmoAxis == 1) {
                        ppy -= (float) dragY * (speed / Math.max(0.2f, (float) Math.abs(cosP)));
                    } else if (activeGizmoAxis == 2) {
                        ppz += (float) (dragX * sinY + dragY * cosY) * speed;
                    }
                }

                propertyInspector.setParticleProperties(ppx, ppy, ppz, pyaw, ppitch, proll, propertyInspector.getPartParticleId(), propertyInspector.getPartEmitterType(), propertyInspector.getPartCount(), propertyInspector.getPartSpeed(), propertyInspector.getPartSpread(), propertyInspector.getPartDuration(), propertyInspector.getPartColorRgb(), propertyInspector.getPartSize());

                onParticleChanged(ppx, ppy, ppz, pyaw, ppitch, proll, propertyInspector.getPartParticleId(), propertyInspector.getPartEmitterType(), propertyInspector.getPartCount(), propertyInspector.getPartSpeed(), propertyInspector.getPartSpread(), propertyInspector.getPartDuration(), propertyInspector.getPartColorRgb(), propertyInspector.getPartSize());

                return true;
            }

            if (gMode == ModelSettings.GizmoMode.ROTATE) {
                float rx = (propertyInspector != null) ? propertyInspector.getRotX() : 0.0f;
                float ry = (propertyInspector != null) ? propertyInspector.getRotY() : 0.0f;
                float rz = (propertyInspector != null) ? propertyInspector.getRotZ() : 0.0f;

                if (activeGizmoAxis == 0) {
                    rx += (float) -dragY * 0.4f;
                } else if (activeGizmoAxis == 1) {
                    ry += (float) dragX * 0.4f;
                } else if (activeGizmoAxis == 2) {
                    rz += (float) (dragX - dragY) * 0.3f;
                }

                onBoneTransformChanged(selectedBone, rx, ry, rz);

                if (propertyInspector != null) propertyInspector.setBoneRotations(rx, ry, rz);
            } else {
                float radYaw = (float) Math.toRadians(cameraYaw);
                float cosY = (float) Math.cos(radYaw);
                float sinY = (float) Math.sin(radYaw);
                float radPitch = (float) Math.toRadians(cameraPitch);
                float cosP = (float) Math.cos(radPitch);
                float sinP = (float) Math.sin(radPitch);
                float px = (propertyInspector != null) ? propertyInspector.getPosX() : 0.0f;
                float py = (propertyInspector != null) ? propertyInspector.getPosY() : 0.0f;
                float pz = (propertyInspector != null) ? propertyInspector.getPosZ() : 0.0f;
                float s = 120.0f * (2.0f / Math.max(0.4f, cameraZoom));
                float speed = 1.0f / s;

                if (activeGizmoAxis == 3) {
                    float rightX = cosY;
                    float rightZ = sinY;
                    float upX = -sinP * sinY;
                    float upY = cosP;
                    float upZ = sinP * cosY;
                    px += (float) (dragX * rightX - dragY * upX) * speed;
                    py += (float) (-dragY * upY) * speed;
                    pz += (float) (dragX * rightZ - dragY * upZ) * speed;
                } else if (activeGizmoAxis == 0) {
                    px += (float) (dragX * cosY - dragY * sinY * sinP) * speed;
                } else if (activeGizmoAxis == 1) {
                    py -= (float) dragY * speed;
                } else if (activeGizmoAxis == 2) {
                    pz += (float) (dragX * sinY + dragY * cosY * sinP) * speed;
                }

                onBonePositionChanged(selectedBone, px, py, pz);

                if (propertyInspector != null) propertyInspector.setBonePositions(px, py, pz);

                if (ms != null && ms.isIkMode()) {
                    applyInverseKinematics(selectedBone, px, py, pz, ms.isBendableLimbs());
                }
            }

            return true;
        }

        if (rightW > 20 && propertyInspector != null && propertyInspector.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }

        if (draggingViewport) {
            ModelSettings.CameraMode mode = (propertyInspector != null)

                    ? propertyInspector.getModelSettings().getCameraMode()

                    : ModelSettings.CameraMode.ORBIT;

            float s = 120.0f * (2.0f / Math.max(0.4f, cameraZoom));
            if (mode == ModelSettings.CameraMode.BLENDER_MMB) {
                if (button == 2) {
                    if (Screen.hasShiftDown()) {
                        cameraPanX += (float) (dragX / s);
                        cameraPanY += (float) (dragY / s);
                    } else if (Screen.hasControlDown()) {
                        float zoomDelta = (float) (dragY * 0.02f * (cameraZoom * 0.35f + 0.1f));
                        cameraZoom = Math.max(0.4f, Math.min(60.0f, cameraZoom - zoomDelta));
                    } else {
                        rotateCameraOrbit(mouseX, mouseY, dragX, dragY);
                    }
                }
            } else if (mode == ModelSettings.CameraMode.FREE_FLY_WASD) {
                if (button == 1 || button == 0) {
                    rotateCameraOrbit(mouseX, mouseY, dragX, dragY);
                }
            } else {
                if (button == 0 || button == 2) {
                    cameraPanX += (float) (dragX / s);
                    cameraPanY += (float) (dragY / s);
                } else if (button == 1) {
                    rotateCameraOrbit(mouseX, mouseY, dragX, dragY);
                }
            }

            return true;
        }

        if (!isStaticPoseMode) {
            if (showingGraph) {
                if (graphEditorPanel != null && graphEditorPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
            } else {
                if (timelinePanel != null && timelinePanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (showingGraph && graphEditorPanel != null && graphEditorPanel.getCustomizerModal().isVisible()) {
            return graphEditorPanel.getCustomizerModal().mouseReleased(mouseX, mouseY, button);
        }

        draggingSplitter = DraggingSplitter.NONE;
        draggingViewport = false;

        if (activeGizmoAxis != -1) {
            autoSavePoseToBlock(true);
            activeGizmoAxis = -1;

            commitUndo("Transform Bone");
        }

        if (rightW > 20 && propertyInspector != null) {
            propertyInspector.mouseReleased(mouseX, mouseY, button);
        }

        if (!isStaticPoseMode) {
            if (showingGraph) {
                if (graphEditorPanel != null) graphEditorPanel.mouseReleased(mouseX, mouseY, button);
            } else {
                if (timelinePanel != null) timelinePanel.mouseReleased(mouseX, mouseY, button);
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (skinPickerModal.isVisible() && skinPickerModal.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }

        if (modelImportModal.isVisible() && modelImportModal.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }

        if (showingGraph && graphEditorPanel != null && graphEditorPanel.getCustomizerModal().isVisible()) {
            return true;
        }

        if (propertyInspector != null && propertyInspector.isItemPickerOpen()) {
            return propertyInspector.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (rightW > 20 && propertyInspector != null && propertyInspector.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        if (!isStaticPoseMode) {
            if (showingGraph) {
                if (graphEditorPanel != null && graphEditorPanel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
            } else {
                if (timelinePanel != null && timelinePanel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
            }
        }

        if (mouseX >= centerX && mouseX <= centerX + centerW && mouseY >= contentY && mouseY <= contentY + viewportH) {
            float zoomDelta = (float) (verticalAmount * 0.25f * (cameraZoom * 0.35f + 0.1f));
            cameraZoom = Math.max(0.4f, Math.min(60.0f, cameraZoom - zoomDelta));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (skinPickerModal.isVisible() && skinPickerModal.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (modelImportModal.isVisible() && modelImportModal.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (settingsDialog != null && settingsDialog.isOpen()) {
            return settingsDialog.keyPressed(keyCode, scanCode, modifiers);
        }

        if (propertyInspector != null && propertyInspector.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (showingGraph && graphEditorPanel != null && graphEditorPanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (isCreatingNewModelClosing || isRenamingProjectClosing) return true;

        if (isEditingTextCard) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                closeTextCardDialog();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                confirmTextCardEdit();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                if (!textCardInput.isEmpty()) {
                    textCardInput = textCardInput.substring(0, textCardInput.length() - 1);
                }
                return true;
            }
            return true;
        }
        if (isCreatingNewModel) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeCreateModelDialog();

                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                confirmCreateModel();

                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_TAB) {
                newModelIsPose = !newModelIsPose;

                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!newModelNameInput.isEmpty()) {
                    newModelNameInput = newModelNameInput.substring(0, newModelNameInput.length() - 1);
                }

                return true;
            }

            return true;
        }

        if (isRenamingProject) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeRenameDialog();

                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                confirmProjectRename();

                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!renameInput.isEmpty()) {
                    renameInput = renameInput.substring(0, renameInput.length() - 1);
                }

                return true;
            }

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (activeMenu != MenuType.NONE) {
                setActiveMenu(MenuType.NONE);

                return true;
            }

            if (cameraViewMode) {
                this.cameraViewMode = false;
                if (getModelSettings() != null) getModelSettings().setCameraViewMode(false);
                return true;
            }

            if ("camera".equals(selectedBone)) {
                this.selectedBone = "body";
                if (propertyInspector != null) propertyInspector.setSelectedBone("body");
                return true;
            }

            if (showingGraph) {
                this.showingGraph = false;

                return true;
            }

            this.onClose();

            return true;
        }

        ModelSettings settings = (propertyInspector != null) ? propertyInspector.getModelSettings() : null;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;

        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            copyCurrentPose();

            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            pastePoseAtCurrentTime();

            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_D) {
            duplicatePoseToNextFrame();

            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_M) {
            mirrorCurrentPose();

            return true;
        }

        if (!ctrl && keyCode == GLFW.GLFW_KEY_K) {
            keyAllBones();

            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_N) {
            openCreateModelDialog();

            return true;
        }

        if (settings != null && settings.isFreeFlyCamera()) {
            float moveSpeed = Screen.hasShiftDown() ? 0.35f : 0.15f;

            if (keyCode == GLFW.GLFW_KEY_W) {
                cameraZoom = Math.max(0.1f, cameraZoom - moveSpeed);

                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_S) {
                cameraZoom = Math.min(25.0f, cameraZoom + moveSpeed);

                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_A) {
                cameraYaw -= 4.0f;

                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_D) {
                cameraYaw += 4.0f;

                return true;
            }
        }

        int keyPlay = (settings != null) ? settings.getKeyPlayPause() : GLFW.GLFW_KEY_SPACE;
        int keyAdd = (settings != null) ? settings.getKeyAddKeyframe() : GLFW.GLFW_KEY_K;
        int keyDel = (settings != null) ? settings.getKeyDelete() : GLFW.GLFW_KEY_DELETE;
        int keyGraph = (settings != null) ? settings.getKeyToggleGraph() : GLFW.GLFW_KEY_G;
        int keyCam = (settings != null) ? settings.getKeyCameraView() : GLFW.GLFW_KEY_KP_0;

        if (!isStaticPoseMode && keyCode == keyPlay) {
            if (timelinePanel != null) timelinePanel.togglePlay();

            return true;
        }

        if (!isStaticPoseMode && (keyCode == keyGraph || (keyCode == GLFW.GLFW_KEY_TAB && !ctrl))) {
            onToggleGraph();

            return true;
        }

        if (keyCode == keyCam) {
            launchStaticCameraMode();
            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            if (timelinePanel != null && !isStaticPoseMode) {
                timelinePanel.selectAllKeyframes();

                return true;
            }
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_S) {
            if (hasModel && currentClip != null && currentFileName != null) {
                AnimationFileManager.getInstance().saveAnimation(currentClip, currentFileName);
            }

            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_R) {
            if (currentFileName != null) {
                AnimationClip reloaded = AnimationFileManager.getInstance().loadAnimation(currentFileName);

                if (reloaded != null) {
                    this.currentClip = reloaded;

                    if (timelinePanel != null) timelinePanel.setClip(currentClip);

                    if (graphEditorPanel != null) graphEditorPanel.setClip(currentClip);
                }
            }

            SkinManager.getInstance().refreshSkins();

            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_Z) {
            onUndo();

            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_Y) {
            onRedo();

            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            copyBoneRotation();

            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            pasteBoneRotation();

            return true;
        }

        if (!isStaticPoseMode && keyCode == keyAdd) {
            onAddKeyframe();

            return true;
        }

        if (!isStaticPoseMode && (keyCode == keyDel || keyCode == GLFW.GLFW_KEY_BACKSPACE)) {
            if (net.openstage.client.audio.AudioPlaybackService.getInstance().getSelectedSegment() != null) {
                net.openstage.client.audio.AudioPlaybackService.getInstance().deleteSelectedSegment();
                return true;
            }

            if (timelinePanel != null && timelinePanel.getSelectedTextCard() != null) {
                onDeleteTextCardRequested(timelinePanel.getSelectedTextCard());
                return true;
            }

            if (timelinePanel != null && !timelinePanel.getSelectedKeyframes().isEmpty()) {
                timelinePanel.deleteSelectedKeyframes();

                return true;
            }

            onDeleteKeyframe();

            return true;
        }

        if (!isStaticPoseMode && keyCode == GLFW.GLFW_KEY_HOME) {
            if (timelinePanel != null) timelinePanel.seek(0);

            return true;
        }

        if (!isStaticPoseMode && keyCode == GLFW.GLFW_KEY_END) {
            if (timelinePanel != null) timelinePanel.seek(timelinePanel.getTotalFrames());

            return true;
        }

        if (!isStaticPoseMode && keyCode == GLFW.GLFW_KEY_LEFT) {
            int delta = Screen.hasShiftDown() ? -5 : -1;

            if (timelinePanel != null) timelinePanel.stepFrame(delta);

            return true;
        }

        if (!isStaticPoseMode && keyCode == GLFW.GLFW_KEY_RIGHT) {
            int delta = Screen.hasShiftDown() ? 5 : 1;

            if (timelinePanel != null) timelinePanel.stepFrame(delta);

            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override

    public void onAddKeyframe() {
        if (currentClip == null || timelinePanel == null || isStaticPoseMode) return;

        captureUndoBefore();
        float time = timelinePanel.getCurrentTimeSeconds();

        if ("camera".equals(selectedBone)) {
            if (propertyInspector != null) {
                onAddCameraKeyframe(
                        propertyInspector.getCamX(), propertyInspector.getCamY(), propertyInspector.getCamZ(),

                        propertyInspector.getCamYaw(), propertyInspector.getCamPitch(), propertyInspector.getCamRoll(),

                        propertyInspector.getCamFov()
                );
            }

            return;
        } else if ("eyes".equals(selectedBone)) {
            if (propertyInspector != null) {
                onAddKeyframeRequested(
                        "eyes",

                        new float[]{propertyInspector.getModelSettings().getLeftEyelid(), propertyInspector.getModelSettings().getRightEyelid(), 0.0f},

                        new float[]{propertyInspector.getModelSettings().getPupilX(), propertyInspector.getModelSettings().getPupilY(), 0.0f},

                        new float[]{1.0f, 1.0f, 1.0f},

                        propertyInspector.getCurrentEasing()
                );
            }
        } else {
            float rx = (propertyInspector != null) ? propertyInspector.getRotX() : 0.0f;
            float ry = (propertyInspector != null) ? propertyInspector.getRotY() : 0.0f;
            float rz = (propertyInspector != null) ? propertyInspector.getRotZ() : 0.0f;
            EasingType easing = (propertyInspector != null) ? propertyInspector.getCurrentEasing() : EasingType.LINEAR;
            if (selectedBone == null || selectedBone.isEmpty()) selectedBone = "body";
            BoneTrack track = currentClip.getOrCreateBone(selectedBone);

            track.addKeyframe(ChannelType.ROTATION, new Keyframe(time, new float[]{rx, ry, rz}, easing, null));
        }

        commitUndo("Add Keyframe");
    }

    @Override

    public void onDeleteKeyframe() {
        if (currentClip == null || timelinePanel == null || isStaticPoseMode) return;

        captureUndoBefore();

        if (!timelinePanel.getSelectedKeyframes().isEmpty()) {
            timelinePanel.deleteSelectedKeyframes();
        } else {
            onDeleteKeyframeRequested(selectedBone);
        }

        commitUndo("Delete Keyframe");
    }

    @Override

    public void onClearKeyframe() {
        if (currentClip == null || timelinePanel == null || isStaticPoseMode) return;

        captureUndoBefore();
        float time = timelinePanel.getCurrentTimeSeconds();
        int fps = timelinePanel.getFps();
        float maxDelta = (0.5f / (float) fps) + 0.005f;

        for (BoneTrack track : currentClip.getBones().values()) {
            Float bestKey = null;
            float bestDist = Float.MAX_VALUE;

            for (Float kTime : track.getRotationKeys().keySet()) {
                float dist = Math.abs(kTime - time);

                if (dist <= maxDelta && dist < bestDist) {
                    bestDist = dist;
                    bestKey = kTime;
                }
            }

            if (bestKey != null) {
                track.removeKeyframe(ChannelType.ROTATION, bestKey);

                track.removeKeyframe(ChannelType.POSITION, bestKey);

                track.removeKeyframe(ChannelType.SCALE, bestKey);
            }
        }

        commitUndo("Clear Keyframes");
    }

    private String undoSnapshotBefore = null;

    private void captureUndoBefore() {
        if (currentClip != null) {
            this.undoSnapshotBefore = net.openstage.io.AnimationSerializer.toJson(currentClip);
        }
    }

    private void commitUndo(String description) {
        if (currentClip == null || undoSnapshotBefore == null) return;
        String afterJson = net.openstage.io.AnimationSerializer.toJson(currentClip);

        if (undoSnapshotBefore.equals(afterJson)) {
            undoSnapshotBefore = null;

            return;
        }

        String before = undoSnapshotBefore;
        this.undoSnapshotBefore = null;

        UndoRedoManager.getInstance().perform(new UndoRedoManager.UndoableAction() {
            private boolean executedOnce = true;

            @Override
            public void execute() {
                if (!executedOnce) {
                    applyClipJson(afterJson);
                }
                executedOnce = false;
            }

            @Override
            public void undo() {
                applyClipJson(before);
            }

            @Override
            public String description() {
                return description;
            }
        });
    }

    private void applyClipJson(String json) {
        try {
            AnimationClip clip = net.openstage.io.AnimationSerializer.fromJson(json);

            if (clip != null) {
                int prevFrame = (timelinePanel != null) ? timelinePanel.getCurrentFrame() : 0;
                this.currentClip = clip;

                if (timelinePanel != null) {
                    timelinePanel.setClip(clip, true);
                    timelinePanel.setCurrentFrame(prevFrame);
                }

                if (graphEditorPanel != null) {
                    graphEditorPanel.setClip(clip);
                }

                updateInspectorFromCurrentTime();
                autoSavePoseToBlock(false);
            }
        } catch (Exception ignored) {}
    }

    private void updateInspectorFromCurrentTime() {
        if (propertyInspector == null || currentClip == null || timelinePanel == null) return;
        float time = timelinePanel.getCurrentTimeSeconds();

        if ("camera".equals(selectedBone)) {
            CameraTrack ct = currentClip.getCameraTrack();
            if (ct != null && !ct.getKeyframes().isEmpty()) {
                CameraTrack.CameraKeyframe kf = ct.interpolateAt(time);
                propertyInspector.setCameraProperties(kf.x(), kf.y(), kf.z(), kf.yaw(), kf.pitch(), kf.roll(), kf.fov());
                propertyInspector.setCameraSharp(kf.isSharp());
                propertyInspector.setCameraHandles(kf.handles());
            }
            return;
        }

        if (selectedBone != null) {
            Map<String, float[]> rots = currentClip.evaluateFrame(time);

            if (rots.containsKey(selectedBone)) {
                float[] r = rots.get(selectedBone);

                propertyInspector.setBoneRotations(r[0], r[1], r[2]);
            }

            Map<String, float[]> pos = currentClip.evaluatePositions(time);

            if (pos.containsKey(selectedBone)) {
                float[] p = pos.get(selectedBone);

                propertyInspector.setBonePositions(p[0], p[1], p[2]);
            }
        }
    }

    @Override
    public void onUndo() {
        if (UndoRedoManager.getInstance().canUndo()) {
            UndoRedoManager.getInstance().undo();

            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.literal("§e[OpenStage] Отменено (Undo)"), true
                );
            }
        } else {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.literal("§7[OpenStage] Нечего отменять"), true
                );
            }
        }
    }

    @Override
    public void onRedo() {
        if (UndoRedoManager.getInstance().canRedo()) {
            UndoRedoManager.getInstance().redo();

            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.literal("§a[OpenStage] Возвращено (Redo)"), true
                );
            }
        } else {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.literal("§7[OpenStage] Нечего возвращать"), true
                );
            }
        }
    }

    @Override
    public void onEditStart() {
        captureUndoBefore();
    }

    @Override
    public void onEditEnd() {
        commitUndo("Edit");
        autoSavePoseToBlock(true);
    }

    @Override

    public void onToggleGraph() {
        if (isStaticPoseMode) return;
        this.showingGraph = !showingGraph;

        if (timelinePanel != null) timelinePanel.setShowingGraph(showingGraph);
    }

    @Override

    public void onCloseGraph() {
        this.showingGraph = false;

        if (timelinePanel != null) timelinePanel.setShowingGraph(false);
    }

    private Map<String, float[]> poseClipboardRotations = null;
    private Map<String, float[]> poseClipboardPositions = null;

    @Override

    public void onCopyPose() {
        copyCurrentPose();
    }

    @Override

    public void onPastePose() {
        pastePoseAtCurrentTime();
    }

    @Override

    public void onDuplicatePose() {
        duplicatePoseToNextFrame();
    }

    @Override

    public void onKeyAll() {
        keyAllBones();
    }

    @Override

    public void onMirrorPose() {
        mirrorCurrentPose();
    }

    private void copyCurrentPose() {
        if (currentClip == null) return;
        float time = (timelinePanel != null) ? timelinePanel.getCurrentTimeSeconds() : 0.0f;
        this.poseClipboardRotations = new HashMap<>(currentClip.evaluateFrame(time));
        this.poseClipboardPositions = new HashMap<>(currentClip.evaluatePositions(time));

        if (selectedBone != null && propertyInspector != null) {
            poseClipboardRotations.put(selectedBone, propertyInspector.getBoneRotations().clone());

            poseClipboardPositions.put(selectedBone, propertyInspector.getBonePositions().clone());
        }

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.literal("§a[OpenStage] Поза кадра скопирована (Ctrl+C)!"), true
            );
        }
    }

    private void pastePoseAtCurrentTime() {
        if (currentClip == null || poseClipboardRotations == null) return;

        captureUndoBefore();
        float time = (timelinePanel != null) ? timelinePanel.getCurrentTimeSeconds() : 0.0f;

        for (Map.Entry<String, float[]> e : poseClipboardRotations.entrySet()) {
            BoneTrack bt = currentClip.getOrCreateBone(e.getKey());

            bt.addKeyframe(ChannelType.ROTATION, new Keyframe(time, e.getValue().clone(), EasingType.LINEAR, null));
        }

        if (poseClipboardPositions != null) {
            for (Map.Entry<String, float[]> e : poseClipboardPositions.entrySet()) {
                BoneTrack bt = currentClip.getOrCreateBone(e.getKey());

                bt.addKeyframe(ChannelType.POSITION, new Keyframe(time, e.getValue().clone(), EasingType.LINEAR, null));
            }
        }

        if (selectedBone != null && propertyInspector != null) {
            if (poseClipboardRotations.containsKey(selectedBone)) {
                float[] r = poseClipboardRotations.get(selectedBone);

                propertyInspector.setBoneRotations(r[0], r[1], r[2]);
            }

            if (poseClipboardPositions != null && poseClipboardPositions.containsKey(selectedBone)) {
                float[] p = poseClipboardPositions.get(selectedBone);

                propertyInspector.setBonePositions(p[0], p[1], p[2]);
            }
        }

        if (this.minecraft != null && this.minecraft.player != null) {
            int frame = (timelinePanel != null) ? Math.round(time * timelinePanel.getFps()) : 0;

            this.minecraft.player.displayClientMessage(
                    Component.literal("§a[OpenStage] Поза вставлена на кадр " + frame + " (Ctrl+V)!"), true
            );
        }

        commitUndo("Paste Pose");
    }

    private void duplicatePoseToNextFrame() {
        if (currentClip == null || timelinePanel == null) return;

        copyCurrentPose();
        int fps = timelinePanel.getFps();
        float step = 1.0f / (float) fps;
        float nextTime = timelinePanel.getCurrentTimeSeconds() + step;

        timelinePanel.setCurrentTime(nextTime);

        pastePoseAtCurrentTime();

        if (this.minecraft != null && this.minecraft.player != null) {
            int frame = Math.round(nextTime * fps);

            this.minecraft.player.displayClientMessage(
                    Component.literal("§b[OpenStage] Дублировано в след. кадр " + frame + " (Ctrl+D)!"), true
            );
        }
    }

    private void keyAllBones() {
        if (currentClip == null) return;
        float time = (timelinePanel != null) ? timelinePanel.getCurrentTimeSeconds() : 0.0f;
        Map<String, float[]> rots = currentClip.evaluateFrame(time);
        Map<String, float[]> poses = currentClip.evaluatePositions(time);

        for (String b : TimelinePanel.TRACK_BONES) {
            BoneTrack bt = currentClip.getOrCreateBone(b);
            float[] r = rots.getOrDefault(b, new float[]{0, 0, 0});

            bt.addKeyframe(ChannelType.ROTATION, new Keyframe(time, r.clone(), EasingType.LINEAR, null));
            float[] p = poses.getOrDefault(b, new float[]{0, 0, 0});

            bt.addKeyframe(ChannelType.POSITION, new Keyframe(time, p.clone(), EasingType.LINEAR, null));
        }

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.literal("§6[OpenStage] Зафиксированы все кости в текущем кадре (K)!"), true
            );
        }
    }

    private void mirrorCurrentPose() {
        if (currentClip == null) return;
        float time = (timelinePanel != null) ? timelinePanel.getCurrentTimeSeconds() : 0.0f;
        Map<String, float[]> rots = currentClip.evaluateFrame(time);
        float[] rArm = rots.getOrDefault("right_arm", new float[]{0,0,0}).clone();
        float[] lArm = rots.getOrDefault("left_arm", new float[]{0,0,0}).clone();
        float[] rLeg = rots.getOrDefault("right_leg", new float[]{0,0,0}).clone();
        float[] lLeg = rots.getOrDefault("left_leg", new float[]{0,0,0}).clone();

        flipLimb(rArm); flipLimb(lArm);

        flipLimb(rLeg); flipLimb(lLeg);

        setBoneKey(time, "right_arm", lArm);

        setBoneKey(time, "left_arm", rArm);

        setBoneKey(time, "right_leg", lLeg);

        setBoneKey(time, "left_leg", rLeg);

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.literal("§d[OpenStage] Поза отзеркалена (Ctrl+M)!"), true
            );
        }
    }

    private void flipLimb(float[] r) {
        if (r.length >= 3) {
            r[1] = -r[1];
            r[2] = -r[2];
        }
    }

    private void setBoneKey(float time, String bone, float[] rot) {
        BoneTrack bt = currentClip.getOrCreateBone(bone);

        bt.addKeyframe(ChannelType.ROTATION, new Keyframe(time, rot, EasingType.LINEAR, null));
    }

    @Override

    public void onKeyframeModified() {
        if ("camera".equals(selectedBone) && currentClip != null && timelinePanel != null && propertyInspector != null) {
            float time = timelinePanel.getCurrentTimeSeconds();
            CameraTrack ct = currentClip.getCameraTrack();

            for (var entry : ct.getKeyframes().entrySet()) {
                if (Math.abs(entry.getKey() - time) <= 0.05f) {
                    propertyInspector.setCameraHandles(entry.getValue().handles());
                    break;
                }
            }
        }
    }

    @Override

    public void onThemeChanged(EditorTheme theme) {
    }

    @Override

    public void onSettingsClosed() {
    }

    @Override

    public void onKeyframeSelected(String bone, ChannelType channel, Keyframe keyframe) {
        this.selectedBone = bone;

        if (propertyInspector != null) {
            propertyInspector.setSelectedBone(bone);
            propertyInspector.setSelectedEnvironmentKeyframe(null);

            if (keyframe != null) {
                propertyInspector.setBoneRotations(keyframe.value()[0], keyframe.value()[1], keyframe.value()[2]);
            }
        }

        if ("camera".equals(bone) && currentClip != null && timelinePanel != null) {
            float time = timelinePanel.getCurrentTimeSeconds();
            CameraTrack ct = currentClip.getCameraTrack();
            CameraTrack.CameraKeyframe closest = null;
            float minDist = 0.08f;

            for (var entry : ct.getKeyframes().entrySet()) {
                float dist = Math.abs(entry.getKey() - time);

                if (dist < minDist) {
                    minDist = dist;
                    closest = entry.getValue();
                }
            }

            if (closest != null && propertyInspector != null) {
                propertyInspector.setCameraSharp(closest.isSharp());

                propertyInspector.setCameraHandles(closest.handles());

                propertyInspector.setCameraProperties(closest.x(), closest.y(), closest.z(), closest.yaw(), closest.pitch(), closest.roll(), closest.fov());
            }
        }

        if (graphEditorPanel != null) {
            graphEditorPanel.setSelectedBone(bone);
        }
    }

    @Override

    public void onTextCardSelected(TextCard card) {
        this.selectedBone = null;
        if (card != null && rightPanelCollapsed) {
            rightPanelCollapsed = false;
            rightPanelTarget = 1.0f;
            rightPanelAnim = 1.0f;
            recalculateLayout();
        }
        if (timelinePanel != null) {
            timelinePanel.setSelectedTextCard(card);
        }
        if (propertyInspector != null) {
            propertyInspector.setSelectedTextCard(card);
            propertyInspector.setSelectedBone(null);
        }
    }

    @Override
    public void onTextCardDoubleClicked(TextCard card) {
        onTextCardSelected(card);
        openTextCardDialog(card);
    }

    @Override
    public void onEditTextCardRequested(TextCard card) {
        openTextCardDialog(card);
    }

    @Override
    public void onDeleteTextCardRequested(TextCard card) {
        if (currentClip != null && card != null) {
            currentClip.getTextTrack().removeCard(card);
        }
        if (timelinePanel != null) {
            timelinePanel.setSelectedTextCard(null);
        }
        if (propertyInspector != null) {
            propertyInspector.setSelectedTextCard(null);
        }
        this.activeEditingTextCard = null;
    }

    @Override
    public void onAudioSegmentDoubleClicked(net.openstage.client.audio.AudioPlaybackService.AudioSegment seg) {
        openAudioTrackDialog(seg);
    }

    @Override

    public void onSkinChanged(String skinName, boolean isSlim) {
        if (!nearbyActors.isEmpty() && selectedActorIndex >= 0 && selectedActorIndex < nearbyActors.size()) {
            SceneActorRef cur = nearbyActors.get(selectedActorIndex);

            nearbyActors.set(selectedActorIndex, new SceneActorRef(
                    cur.pos(), cur.name(), cur.relX(), cur.relY(), cur.relZ(),

                    skinName, isSlim, cur.clipId()
            ));

            if (cur.pos() != null && this.minecraft != null && this.minecraft.level != null) {
                if (this.minecraft.level.getBlockEntity(cur.pos()) instanceof ModelBlockEntity mbe) {
                    mbe.setSkin(skinName);

                    mbe.setSlim(isSlim);
                }
            }
        }
    }

    @Override

    public void onCopyCameraFromEditor() {
        if (currentClip == null || timelinePanel == null) return;
        float time = timelinePanel.getCurrentTimeSeconds();
        float radY = (float) Math.toRadians(cameraYaw);
        float radP = (float) Math.toRadians(cameraPitch);
        float dist = Math.max(1.0f, 2.5f * cameraZoom);
        float eyeX = -(float) (Math.sin(radY) * Math.cos(radP)) * dist - cameraPanX;
        float eyeY = 1.0f + (float) Math.sin(radP) * dist - cameraPanY;
        float eyeZ = (float) (Math.cos(radY) * Math.cos(radP)) * dist;
        float fov = (propertyInspector != null) ? propertyInspector.getCamFov() : 70.0f;
        boolean isSharp = (propertyInspector != null) && propertyInspector.isCameraSharp();
        float[] handles = isSharp ? null : ((propertyInspector != null) ? propertyInspector.getCameraHandles() : null);
        EasingType easing = isSharp ? EasingType.STEP : null;

        CameraTrack.CameraKeyframe kf = new CameraTrack.CameraKeyframe(
                time, eyeX, eyeY, eyeZ, cameraYaw, cameraPitch, 0.0f, fov, easing, handles
        );

        currentClip.getCameraTrack().addKey(kf);

        if (propertyInspector != null) {
            propertyInspector.setCameraProperties(eyeX, eyeY, eyeZ, cameraYaw, cameraPitch, 0.0f, fov);
        }
    }

    @Override
    public void onAddCameraKeyframe(float x, float y, float z, float yaw, float pitch, float roll, float fov) {
        if (currentClip == null || timelinePanel == null) return;
        captureUndoBefore();
        float time = isStaticPoseMode ? 0.0f : timelinePanel.getCurrentTimeSeconds();
        boolean isSharp = (propertyInspector != null) && propertyInspector.isCameraSharp();
        float[] handles = isSharp ? null : ((propertyInspector != null) ? propertyInspector.getCameraHandles() : null);
        EasingType easing = isSharp ? EasingType.STEP : null;
        CameraTrack.CameraKeyframe kf = new CameraTrack.CameraKeyframe(time, x, y, z, yaw, pitch, roll, fov, easing, handles);

        currentClip.getCameraTrack().addKey(kf);
        commitUndo("Add Camera Keyframe");
    }

    @Override
    public void onCameraChanged(float x, float y, float z, float yaw, float pitch, float roll, float fov) {
        if (currentClip == null || timelinePanel == null) return;
        CameraTrack ct = currentClip.getCameraTrack();
        if (isStaticPoseMode) {
            ct.addKey(new CameraTrack.CameraKeyframe(0.0f, x, y, z, yaw, pitch, roll, fov, null));
            return;
        }
        float time = timelinePanel.getCurrentTimeSeconds();
        boolean keyExistsAtTime = false;
        for (float kTime : ct.getKeyframes().keySet()) {
            if (Math.abs(kTime - time) <= 0.05f) {
                keyExistsAtTime = true;
                time = kTime;
                break;
            }
        }
        if (keyExistsAtTime || ct.getKeyframes().isEmpty()) {
            boolean isSharp = (propertyInspector != null) && propertyInspector.isCameraSharp();
            float[] handles = isSharp ? null : ((propertyInspector != null) ? propertyInspector.getCameraHandles() : null);
            EasingType easing = isSharp ? EasingType.STEP : null;
            CameraTrack.CameraKeyframe kf = new CameraTrack.CameraKeyframe(time, x, y, z, yaw, pitch, roll, fov, easing, handles);
            ct.addKey(kf);
        }
    }

    @Override

    public void onCameraModeChanged(boolean isSharp, float[] handles) {
        if (currentClip == null || timelinePanel == null) return;
        EasingType newEasing = isSharp ? EasingType.STEP : null;
        float[] newHandles = isSharp ? null : handles;
        float time = timelinePanel.getCurrentTimeSeconds();
        CameraTrack ct = currentClip.getCameraTrack();
        boolean updated = false;

        for (var ref : timelinePanel.getSelectedKeyframes()) {
            if (ref.channel() == ChannelType.CAMERA) {
                CameraTrack.CameraKeyframe target = ct.getKeyframes().get(ref.time());

                if (target != null) {
                    ct.addKey(new CameraTrack.CameraKeyframe(
                            target.time(), target.x(), target.y(), target.z(),

                            target.yaw(), target.pitch(), target.roll(), target.fov(),

                            newEasing, newHandles
                    ));
                    updated = true;
                }
            }
        }

        if (!updated) {
            for (var entry : ct.getKeyframes().entrySet()) {
                if (Math.abs(entry.getKey() - time) <= 0.05f) {
                    CameraTrack.CameraKeyframe old = entry.getValue();

                    ct.addKey(new CameraTrack.CameraKeyframe(
                            old.time(), old.x(), old.y(), old.z(),

                            old.yaw(), old.pitch(), old.roll(), old.fov(),

                            newEasing, newHandles
                    ));
                    break;
                }
            }
        }
    }

    @Override

    public void onOpenGraphEditor() {
        this.showingGraph = true;

        if (timelinePanel != null) timelinePanel.setShowingGraph(true);

        if (graphEditorPanel != null) graphEditorPanel.setSelectedBone("particles".equals(selectedBone) ? "particles" : ("camera".equals(selectedBone) ? "camera" : selectedBone));
    }

    @Override

    public void onModelChanged(boolean isSlim) {
        boolean bendable = (propertyInspector != null) && propertyInspector.getModelSettings().isBendableLimbs();

        if (timelinePanel != null) {
            timelinePanel.setBendableMode(bendable);
        }

        if (bendable && currentClip != null) {
            currentClip.getOrCreateBone("right_forearm");

            currentClip.getOrCreateBone("left_forearm");

            currentClip.getOrCreateBone("right_lower_leg");

            currentClip.getOrCreateBone("left_lower_leg");
        }
    }

    @Override

    public void onAddKeyframeRequested(String bone, float[] rot, float[] pos, float[] scale, EasingType easing) {
        if (currentClip == null || timelinePanel == null) return;
        float time = isStaticPoseMode ? 0.0f : timelinePanel.getCurrentTimeSeconds();
        BoneTrack track = currentClip.getOrCreateBone(resolveActorBone(bone));

        track.addKeyframe(ChannelType.ROTATION, new Keyframe(time, rot, easing, null));

        if ("eyes".equals(bone) && pos != null) {
            track.addKeyframe(ChannelType.POSITION, new Keyframe(time, pos, easing, null));
        }
    }

    @Override

    public void onBoneTransformChanged(String bone, float rx, float ry, float rz) {
        if (currentClip == null || timelinePanel == null) return;
        float time = isStaticPoseMode ? 0.0f : timelinePanel.getCurrentTimeSeconds();

        if ("eyes".equals(bone)) {
            BoneTrack track = currentClip.getOrCreateBone("eyes");
            EasingType easing = (propertyInspector != null) ? propertyInspector.getCurrentEasing() : EasingType.LINEAR;

            track.addKeyframe(ChannelType.ROTATION, new Keyframe(time, new float[]{rx, ry, 0.0f}, easing, null));

            if (propertyInspector != null) {
                track.addKeyframe(ChannelType.POSITION, new Keyframe(time,

                        new float[]{propertyInspector.getModelSettings().getPupilX(), propertyInspector.getModelSettings().getPupilY(), 0.0f},

                        easing, null));
            }
        } else {
            BoneTrack track = currentClip.getOrCreateBone(resolveActorBone(bone));
            EasingType easing = (propertyInspector != null) ? propertyInspector.getCurrentEasing() : EasingType.LINEAR;

            track.addKeyframe(ChannelType.ROTATION, new Keyframe(time, new float[]{rx, ry, rz}, easing, null));

            timelinePanel.setClip(currentClip);
        }

        autoSavePoseToBlock(false);
    }

    private String resolveActorBone(String bone) {
        if (currentClip != null && currentClip.hasActorPrefixes() && bone != null && !bone.contains(":")) {
            return "actor" + selectedActorIndex + ":" + bone;
        }

        return bone;
    }

    @Override

    public void onBonePositionChanged(String bone, float px, float py, float pz) {
        if (currentClip == null || timelinePanel == null) return;
        float time = isStaticPoseMode ? 0.0f : timelinePanel.getCurrentTimeSeconds();
        BoneTrack track = currentClip.getOrCreateBone(resolveActorBone(bone));
        EasingType easing = (propertyInspector != null) ? propertyInspector.getCurrentEasing() : EasingType.LINEAR;

        track.addKeyframe(ChannelType.POSITION, new Keyframe(time, new float[]{px, py, pz}, easing, null));

        timelinePanel.setClip(currentClip);

        autoSavePoseToBlock(false);
    }

    @Override

    public void onDeleteKeyframeRequested(String bone) {
        if (currentClip == null || timelinePanel == null || isStaticPoseMode) return;
        float time = timelinePanel.getCurrentTimeSeconds();
        int fps = timelinePanel.getFps();
        float maxDelta = (0.5f / (float) fps) + 0.005f;

        if ("camera".equals(bone)) {
            Float bestKey = null;
            float bestDist = Float.MAX_VALUE;

            for (Float kTime : currentClip.getCameraTrack().getKeyframes().keySet()) {
                float dist = Math.abs(kTime - time);

                if (dist <= maxDelta && dist < bestDist) {
                    bestDist = dist;
                    bestKey = kTime;
                }
            }

            if (bestKey != null) {
                currentClip.getCameraTrack().removeKey(bestKey);
            }

            return;
        }

        BoneTrack track = currentClip.getBones().get(bone);

        if (track != null) {
            Float bestKey = null;
            float bestDist = Float.MAX_VALUE;

            for (Float kTime : track.getRotationKeys().keySet()) {
                float dist = Math.abs(kTime - time);

                if (dist <= maxDelta && dist < bestDist) {
                    bestDist = dist;
                    bestKey = kTime;
                }
            }

            for (Float kTime : track.getPositionKeys().keySet()) {
                float dist = Math.abs(kTime - time);

                if (dist <= maxDelta && dist < bestDist) {
                    bestDist = dist;
                    bestKey = kTime;
                }
            }

            for (Float kTime : track.getScaleKeys().keySet()) {
                float dist = Math.abs(kTime - time);

                if (dist <= maxDelta && dist < bestDist) {
                    bestDist = dist;
                    bestKey = kTime;
                }
            }

            if (bestKey != null) {
                track.removeKeyframe(ChannelType.ROTATION, bestKey);

                track.removeKeyframe(ChannelType.POSITION, bestKey);

                track.removeKeyframe(ChannelType.SCALE, bestKey);
            }
        }
    }

    @Override

    public void onAddTextCardRequested() {
        if (currentClip == null || timelinePanel == null) return;
        float time = isStaticPoseMode ? 0.0f : timelinePanel.getCurrentTimeSeconds();

        String initText = (getModelSettings() != null && getModelSettings().getLanguage() == ModelSettings.Language.RU) ? "Новая плашка" : "New Text Plate";
        TextCard card = new TextCard(time, time + 2.0f, initText);
        currentClip.getTextTrack().addCard(card);
        this.selectedBone = null;
        timelinePanel.setSelectedTextCard(card);
        if (propertyInspector != null) {
            propertyInspector.setSelectedTextCard(card);
            propertyInspector.setSelectedEnvironmentKeyframe(null);
            propertyInspector.setSelectedBone(null);
        }
        openTextCardDialog(card);
    }

    @Override
    public void onAddEnvironmentKeyframeRequested() {
        if (currentClip == null || timelinePanel == null) return;
        float time = isStaticPoseMode ? 0.0f : timelinePanel.getCurrentTimeSeconds();
        BlockPos anchor = (targetBlockPos != null) ? targetBlockPos : (this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.blockPosition() : BlockPos.ZERO);
        net.openstage.core.scene.EnvironmentKeyframe.EnvironmentMode mode = isWorldMode() ? net.openstage.core.scene.EnvironmentKeyframe.EnvironmentMode.WORLD : net.openstage.core.scene.EnvironmentKeyframe.EnvironmentMode.DARK_VOID;
        net.openstage.core.scene.EnvironmentKeyframe kf = new net.openstage.core.scene.EnvironmentKeyframe(time, mode, anchor, net.openstage.core.scene.EnvironmentKeyframe.EnvironmentTransition.CUT, 0.5f);
        currentClip.getEnvironmentTrack().addKey(kf);
        this.selectedBone = null;
        timelinePanel.setSelectedEnvironmentKeyframe(kf);
        timelinePanel.setSelectedTextCard(null);
        if (propertyInspector != null) {
            propertyInspector.setSelectedEnvironmentKeyframe(kf);
            propertyInspector.setSelectedBone(null);
            propertyInspector.setSelectedTextCard(null);
        }
    }

    @Override
    public void onEnvironmentKeyframeSelected(net.openstage.core.scene.EnvironmentKeyframe keyframe) {
        this.selectedBone = null;
        if (propertyInspector != null) {
            propertyInspector.setSelectedEnvironmentKeyframe(keyframe);
            propertyInspector.setSelectedBone(null);
            propertyInspector.setSelectedTextCard(null);
        }
    }

    @Override
    public void onEnvironmentKeyframeChanged(net.openstage.core.scene.EnvironmentKeyframe keyframe) {
        if (currentClip == null || keyframe == null) return;
        currentClip.getEnvironmentTrack().addKey(keyframe);
        if (timelinePanel != null) {
            timelinePanel.setSelectedEnvironmentKeyframe(keyframe);
        }
    }

    @Override
    public void onDeleteEnvironmentKeyframeRequested(net.openstage.core.scene.EnvironmentKeyframe keyframe) {
        if (currentClip == null || keyframe == null) return;
        currentClip.getEnvironmentTrack().removeKey(keyframe.time());
        if (timelinePanel != null) {
            timelinePanel.setSelectedEnvironmentKeyframe(null);
        }
        if (propertyInspector != null) {
            propertyInspector.setSelectedEnvironmentKeyframe(null);
        }
    }

    @Override
    public void onTakePlayerCoordinatesRequested() {
        if (this.minecraft == null || this.minecraft.player == null || propertyInspector == null) return;
        net.openstage.core.scene.EnvironmentKeyframe cur = propertyInspector.getSelectedEnvironmentKeyframe();
        if (cur == null || currentClip == null) return;
        BlockPos pPos = this.minecraft.player.blockPosition();
        net.openstage.core.scene.EnvironmentKeyframe updated = new net.openstage.core.scene.EnvironmentKeyframe(
                cur.time(), cur.mode(), pPos.getX(), pPos.getY(), pPos.getZ(), cur.transition(), cur.fadeDuration()
        );
        currentClip.getEnvironmentTrack().addKey(updated);
        propertyInspector.setSelectedEnvironmentKeyframe(updated);
        if (timelinePanel != null) timelinePanel.setSelectedEnvironmentKeyframe(updated);
        this.minecraft.player.displayClientMessage(
                Component.literal("§a[OpenStage] Координаты игрока (" + pPos.getX() + ", " + pPos.getY() + ", " + pPos.getZ() + ") сохранены в кадр окружения!"), true
        );
    }

    @Override

    public void onPresetApplied(AnimationClip presetClip) {
        if (presetClip != null) {
            this.currentClip = presetClip;
            this.currentModelName = presetClip.getName();
            this.hasModel = true;
            this.currentFileName = presetClip.getName() + (isStaticPoseMode ? ".ospose.json" : ".osanim.json");

            if (timelinePanel != null) {
                timelinePanel.setModelName(currentModelName);

                timelinePanel.setClip(currentClip);

                timelinePanel.seek(0);
            }

            if (graphEditorPanel != null) {
                graphEditorPanel.setClip(currentClip);
            }
        }
    }

    @Override

    public void onResetCameraRequested() {
        this.cameraPitch = 15.0f;
        this.cameraYaw = 145.0f;
        this.cameraZoom = 2.0f;
        this.cameraPanX = 0.0f;
        this.cameraPanY = 0.0f;
    }

    private boolean isInside(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    @Override

    public boolean charTyped(char chr, int modifiers) {
        if (skinPickerModal.isVisible() && skinPickerModal.charTyped(chr, modifiers)) {
            return true;
        }

        if (modelImportModal.isVisible() && modelImportModal.charTyped(chr, modifiers)) {
            return true;
        }

        if (propertyInspector != null && propertyInspector.charTyped(chr, modifiers)) {
            return true;
        }

        if (isEditingTextCard) {
            if (chr >= 32 && chr != 127 && textCardInput.length() < 120) {
                textCardInput += chr;
                return true;
            }
            return true;
        }
        if (isRenamingProject) {
            if (chr >= 32 && chr != 127 && renameInput.length() < 36) {
                renameInput += chr;

                return true;
            }

            return true;
        }

        return super.charTyped(chr, modifiers);
    }

    private void confirmCreateModel() {
        String trimmed = newModelNameInput.trim();
        String safeName = trimmed.replaceAll("[^a-zA-Z0-9_-]", "_");

        if (safeName.isEmpty()) {
            safeName = "model_1";
        }

        this.currentModelName = safeName;
        this.isStaticPoseMode = newModelIsPose;

        if (isStaticPoseMode) {
            this.currentFileName = safeName + ".ospose.json";
            this.currentClip = new AnimationClip(safeName, 1.0f);

            this.currentClip.setLooping(false);
        } else {
            this.currentFileName = safeName + ".osanim.json";
            this.currentClip = new AnimationClip(safeName, 2.0f);

            this.currentClip.setLooping(true);
        }

        for (String b : TimelinePanel.TRACK_BONES) {
            currentClip.getOrCreateBone(b);
        }

        this.hasModel = true;

        this.nearbyActors.clear();

        this.nearbyActors.add(new SceneActorRef(targetBlockPos, currentModelName, 0, 0, 0, "steve", false, "idle"));
        this.selectedActorIndex = 0;

        if (timelinePanel != null) {
            timelinePanel.setModelName(currentModelName);

            timelinePanel.setClip(currentClip);

            timelinePanel.setModelTreeExpanded(true);
        }

        if (graphEditorPanel != null) {
            graphEditorPanel.setClip(currentClip);
        }

        if (propertyInspector != null) {
            propertyInspector.setSelectedBone(selectedBone);
        }

        recalculateLayout();

        recalculateLayoutAfterResize();

        closeCreateModelDialog();
    }

    private void confirmProjectRename() {
        String trimmed = renameInput.trim();

        if (!trimmed.isEmpty()) {
            String safeName = trimmed.replaceAll("[^a-zA-Z0-9_-]", "_");

            if (renamingTargetFile != null) {
                AnimationFileManager.getInstance().renameProjectFile(renamingTargetFile, safeName);
                if (renamingTargetFile.equalsIgnoreCase(currentFileName)) {
                    String suffix = renamingTargetFile.toLowerCase(java.util.Locale.ROOT).endsWith(".ospose.json") ? ".ospose.json" : ".osanim.json";
                    currentFileName = safeName + suffix;
                    currentModelName = safeName;
                    if (currentClip != null) currentClip.setName(safeName);
                    if (timelinePanel != null) timelinePanel.setModelName(safeName);
                }
                renamingTargetFile = null;
            } else {
                if (currentClip != null) {
                    currentClip.setName(safeName);
                }

                this.currentModelName = safeName;
                String suffix = isStaticPoseMode ? ".ospose.json" : ".osanim.json";
                currentFileName = safeName.endsWith(suffix) ? safeName : safeName + suffix;

                if (currentClip != null) {
                    AnimationFileManager.getInstance().saveAnimation(currentClip, currentFileName);
                }

                if (timelinePanel != null) {
                    timelinePanel.setModelName(currentModelName);
                }
            }

            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.literal("§6[OpenStage] §aProject renamed to: §f" + safeName), true);
            }
        }

        closeRenameDialog();
    }

    private void renderCreateModelDialog(GuiGraphics graphics, int mouseX, int mouseY, EditorTheme theme) {
        if (!isCreatingNewModel && !isCreatingNewModelClosing) return;

        graphics.bufferSource().endBatch();

        com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);

        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();

        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        long now = System.currentTimeMillis();
        boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();
        float ease = 1.0f;
        float progress = 1.0f;

        if (isCreatingNewModelClosing) {
            float p = anims ? Math.min(1.0f, (now - createModelCloseTime) / 140.0f) : 1.0f;
            progress = p;
            ease = (float) Math.pow(1.0f - p, 3);

            if (p >= 1.0f) {
                isCreatingNewModel = false;
                isCreatingNewModelClosing = false;

                return;
            }
        } else {
            float p = anims ? Math.min(1.0f, (now - createModelOpenTime) / 160.0f) : 1.0f;
            progress = p;
            ease = 1.0f - (float) Math.pow(1.0f - p, 3);
        }

        float scale = anims ? (0.88f + 0.12f * ease) : 1.0f;
        int bgAlpha = anims ? (int) (0x77 * ease) : 0x77;

        graphics.fill(0, 0, this.width, this.height, (bgAlpha << 24));
        int dw = 320;
        int dh = 150;
        int dx = (this.width - dw) / 2;
        int dy = (this.height - dh) / 2;
        int cx = dx + dw / 2;
        int cy = dy + dh / 2;
        int effMouseX = (progress < 1.0f && scale > 0.001f) ? (int) ((mouseX - cx) / scale + cx) : mouseX;
        int effMouseY = (progress < 1.0f && scale > 0.001f) ? (int) ((mouseY - cy) / scale + cy) : mouseY;

        graphics.pose().pushPose();

        graphics.pose().translate(0, 0, 500.0f);

        graphics.pose().translate(cx, cy, 0);

        graphics.pose().scale(scale, scale, 1.0f);

        graphics.pose().translate(-cx, -cy, 0);

        graphics.fill(dx, dy, dx + dw, dy + dh, theme.getPanelBg());

        graphics.fill(dx, dy, dx + dw, dy + 22, theme.getTitleBarBg());

        graphics.renderOutline(dx, dy, dw, dh, theme.getAccent());
        ModelSettings.Language dlgLang = (getModelSettings() != null) ? getModelSettings().getLanguage() : ModelSettings.Language.EN;

        graphics.drawString(this.font, I18n.get("Create New Model / Project", dlgLang), dx + 8, dy + 7, theme.getAccent(), false);

        graphics.drawString(this.font, I18n.get("Name:", dlgLang), dx + 12, dy + 31, theme.getTextColor(), false);
        int inputX = dx + 48;
        int inputY = dy + 28;
        int inputW = dw - 60;
        int inputH = 16;

        graphics.fill(inputX, inputY, inputX + inputW, inputY + inputH, 0xFF111827);

        graphics.renderOutline(inputX, inputY, inputW, inputH, theme.getAccent());
        boolean blink = (System.currentTimeMillis() / 450) % 2 == 0;
        String displayTxt = newModelNameInput + (blink ? "_" : "");

        graphics.drawString(this.font, displayTxt, inputX + 4, inputY + 4, 0xFFFFFFFF, false);
        int radioY = dy + 52;
        String radio1 = newModelIsPose ? "(●) Static Pose (.ospose.json)" : "(○) Static Pose (.ospose.json)";
        int r1Col = newModelIsPose ? 0xFFF59E0B : theme.getTextColor();

        graphics.drawString(this.font, radio1, dx + 14, radioY, r1Col, false);

        graphics.drawString(this.font, "    " + I18n.get("Single pose for statues, props, model blocks", dlgLang), dx + 14, radioY + 11, theme.getTextMuted(), false);
        int radio2Y = radioY + 26;
        String radio2 = !newModelIsPose ? "(●) Animation (.osanim.json)" : "(○) Animation (.osanim.json)";
        int r2Col = !newModelIsPose ? 0xFF38BDF8 : theme.getTextColor();

        graphics.drawString(this.font, radio2, dx + 14, radio2Y, r2Col, false);

        graphics.drawString(this.font, "    " + I18n.get("Full timeline with keyframes & motion playback", dlgLang), dx + 14, radio2Y + 11, theme.getTextMuted(), false);
        int btnY = dy + dh - 26;

        drawToolbarButton(graphics, I18n.get("Create", dlgLang), dx + dw - 130, btnY, 56, 18, effMouseX, effMouseY, theme);

        drawToolbarButton(graphics, I18n.get("Cancel", dlgLang), dx + dw - 66, btnY, 56, 18, effMouseX, effMouseY, theme);

        graphics.bufferSource().endBatch();

        graphics.pose().popPose();

        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }

    private void renderRenameDialog(GuiGraphics graphics, int mouseX, int mouseY, EditorTheme theme) {
        if (!isRenamingProject && !isRenamingProjectClosing) return;

        graphics.bufferSource().endBatch();

        com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);

        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        long now = System.currentTimeMillis();
        boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();
        float ease = 1.0f;
        float progress = 1.0f;

        if (isRenamingProjectClosing) {
            float p = anims ? Math.min(1.0f, (now - renameCloseTime) / 140.0f) : 1.0f;
            progress = p;
            ease = (float) Math.pow(1.0f - p, 3);

            if (p >= 1.0f) {
                isRenamingProject = false;
                isRenamingProjectClosing = false;

                return;
            }
        } else {
            float p = anims ? Math.min(1.0f, (now - renameOpenTime) / 160.0f) : 1.0f;
            progress = p;
            ease = 1.0f - (float) Math.pow(1.0f - p, 3);
        }

        float scale = anims ? (0.88f + 0.12f * ease) : 1.0f;
        int bgAlpha = anims ? (int) (0x77 * ease) : 0x77;

        graphics.fill(0, 0, this.width, this.height, (bgAlpha << 24));
        int dw = 320;
        int dh = 110;
        int dx = (this.width - dw) / 2;
        int dy = (this.height - dh) / 2;
        int cx = dx + dw / 2;
        int cy = dy + dh / 2;
        int effMouseX = (progress < 1.0f && scale > 0.001f) ? (int) ((mouseX - cx) / scale + cx) : mouseX;
        int effMouseY = (progress < 1.0f && scale > 0.001f) ? (int) ((mouseY - cy) / scale + cy) : mouseY;

        graphics.pose().pushPose();

        graphics.pose().translate(0, 0, 500.0f);

        graphics.pose().translate(cx, cy, 0);

        graphics.pose().scale(scale, scale, 1.0f);

        graphics.pose().translate(-cx, -cy, 0);

        graphics.fill(dx, dy, dx + dw, dy + dh, theme.getPanelBg());

        graphics.fill(dx, dy, dx + dw, dy + 22, theme.getTitleBarBg());

        graphics.renderOutline(dx, dy, dw, dh, theme.getAccent());
        ModelSettings.Language renLang = (getModelSettings() != null) ? getModelSettings().getLanguage() : ModelSettings.Language.EN;

        graphics.drawString(this.font, I18n.get("Rename Project", renLang), dx + 8, dy + 7, theme.getAccent(), false);

        graphics.drawString(this.font, I18n.get("New Name:", renLang), dx + 12, dy + 30, theme.getTextColor(), false);
        int inputX = dx + 12;
        int inputY = dy + 46;
        int inputW = dw - 24;
        int inputH = 18;

        graphics.fill(inputX, inputY, inputX + inputW, inputY + inputH, 0xFF111827);

        graphics.renderOutline(inputX, inputY, inputW, inputH, theme.getAccent());
        boolean blink = (System.currentTimeMillis() / 450) % 2 == 0;
        String displayTxt = renameInput + (blink ? "_" : "");

        graphics.drawString(this.font, displayTxt, inputX + 4, inputY + 5, 0xFFFFFFFF, false);
        int btnY = dy + 74;

        drawToolbarButton(graphics, I18n.get("Save", renLang), dx + dw - 130, btnY, 56, 18, effMouseX, effMouseY, theme);

        drawToolbarButton(graphics, I18n.get("Cancel", renLang), dx + dw - 66, btnY, 56, 18, effMouseX, effMouseY, theme);

        graphics.bufferSource().endBatch();

        graphics.pose().popPose();

        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }

    private long lastSyncTimeMs = 0L;

    private void applySnapshotToCurrentActor(net.openstage.client.model.RecentStateStore.StateSnapshot snap) {
        if (snap == null) return;

        for (Map.Entry<String, float[]> e : snap.rotations.entrySet()) {
            float[] r = e.getValue();

            if (r != null && r.length >= 3) {
                onBoneTransformChanged(e.getKey(), r[0], r[1], r[2]);
            }
        }

        for (Map.Entry<String, float[]> e : snap.positions.entrySet()) {
            float[] p = e.getValue();

            if (p != null && p.length >= 3) {
                onBonePositionChanged(e.getKey(), p[0], p[1], p[2]);
            }
        }

        if (getModelSettings() != null) {
            getModelSettings().setRootYaw(snap.rootYaw);

            getModelSettings().setScale(snap.scale);

            getModelSettings().setBendableLimbs(snap.isBendable);

            getModelSettings().setRightHandItem(snap.rightHandItem);

            getModelSettings().setLeftHandItem(snap.leftHandItem);

            getModelSettings().setAnimatedEyes(snap.animatedEyes);

            getModelSettings().setPupilX(snap.pupilX);

            getModelSettings().setPupilY(snap.pupilY);

            getModelSettings().setLeftEyelid(snap.leftEyelid);

            getModelSettings().setRightEyelid(snap.rightEyelid);
        }

        if (propertyInspector != null) {
            propertyInspector.getModelSettings().setRootYaw(snap.rootYaw);

            propertyInspector.getModelSettings().setScale(snap.scale);

            propertyInspector.getModelSettings().setBendableLimbs(snap.isBendable);

            propertyInspector.getModelSettings().setRightHandItem(snap.rightHandItem);

            propertyInspector.getModelSettings().setLeftHandItem(snap.leftHandItem);

            propertyInspector.getModelSettings().setAnimatedEyes(snap.animatedEyes);

            propertyInspector.getModelSettings().setPupilX(snap.pupilX);

            propertyInspector.getModelSettings().setPupilY(snap.pupilY);

            propertyInspector.getModelSettings().setLeftEyelid(snap.leftEyelid);

            propertyInspector.getModelSettings().setRightEyelid(snap.rightEyelid);
        }

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal("§a[OpenStage] Restored saved pose for: " + snap.name), true);
        }
    }

        private void autoSavePoseToBlock() {
        autoSavePoseToBlock(false);
    }

    private void autoSavePoseToBlock(boolean force) {
        if (targetBlockPos == null || !hasModel) return;
        long now = System.currentTimeMillis();

        if (!force && now - lastSyncTimeMs < 50L) {
            return;
        }

        lastSyncTimeMs = now;
        float time = (timelinePanel != null) ? timelinePanel.getCurrentTimeSeconds() : 0.0f;
        Map<String, float[]> curRots = (currentClip != null) ? currentClip.evaluateFrame(time) : new HashMap<>();
        Map<String, float[]> curPoses = (currentClip != null) ? currentClip.evaluatePositions(time) : new HashMap<>();
        SceneActorRef curActor = (!nearbyActors.isEmpty() && selectedActorIndex >= 0 && selectedActorIndex < nearbyActors.size()) ? nearbyActors.get(selectedActorIndex) : null;
        String curId = (curActor != null) ? ((curActor.skin() != null && !curActor.skin().isEmpty()) ? curActor.skin() : curActor.name()) : currentModelName;
        boolean snapSlim = (curActor != null) && curActor.slim();
        boolean snapBend = (getModelSettings() != null) && getModelSettings().isBendableLimbs();

        net.openstage.client.model.RecentStateStore.saveSnapshot(
                curId, curId, "player", null, curId, snapSlim, snapBend,

                curRots, curPoses, getModelSettings()
        );

        if (curId != null && !curId.startsWith("player:") && !curId.startsWith("custom:") && !curId.startsWith("skin:")) {
            net.openstage.client.model.RecentStateStore.saveSnapshot(
                    "custom:" + curId, curId, "custom", null, curId, snapSlim, snapBend,

                    curRots, curPoses, getModelSettings()
            );

            net.openstage.client.model.RecentStateStore.saveSnapshot(
                    "player:" + curId, curId, "player", null, curId, snapSlim, snapBend,

                    curRots, curPoses, getModelSettings()
            );
        }

        StringBuilder rotSb = new StringBuilder();

        for (Map.Entry<String, float[]> e : curRots.entrySet()) {
            if (rotSb.length() > 0) rotSb.append(";");

            rotSb.append(e.getKey()).append("=").append(e.getValue()[0]).append(",").append(e.getValue()[1]).append(",").append(e.getValue()[2]);
        }

        StringBuilder posSb = new StringBuilder();

        for (Map.Entry<String, float[]> e : curPoses.entrySet()) {
            if (posSb.length() > 0) posSb.append(";");

            posSb.append(e.getKey()).append("=").append(e.getValue()[0]).append(",").append(e.getValue()[1]).append(",").append(e.getValue()[2]);
        }

        String skin = (propertyInspector != null) ? propertyInspector.getCurrentSkin() : "steve";
        boolean isSlim = (propertyInspector != null) && propertyInspector.isSlimModel();
        float yaw = (propertyInspector != null) ? propertyInspector.getModelSettings().getRootYaw() : 0.0f;
        float scale = (propertyInspector != null) ? propertyInspector.getModelSettings().getScale() : 1.0f;

        ClientPlayNetworking.send(new OpenStageNetwork.ApplyPoseToBlockPayload(
                targetBlockPos, curId, skin, isSlim, yaw, scale, rotSb.toString(), posSb.toString()
        ));
        if (this.minecraft != null && this.minecraft.level != null && targetBlockPos != null) {
            if (this.minecraft.level.getBlockEntity(targetBlockPos) instanceof ModelBlockEntity mbe) {
                mbe.setModelId(curId);
                mbe.setSkin(skin);
                mbe.setSlim(isSlim);
                mbe.setYaw(yaw);
                mbe.setScale(scale);
                mbe.setStaticPose(true);
                mbe.setPoseRotations(curRots);
                mbe.setPosePositions(curPoses);
                if (propertyInspector != null) {
                    mbe.setRightHandItem(propertyInspector.getModelSettings().getRightHandItem());
                    mbe.setLeftHandItem(propertyInspector.getModelSettings().getLeftHandItem());
                }
            }
        }
    }

    private void autoSaveProject() {
        if (hasModel && currentClip != null) {
            String targetFile = currentFileName;
            if (targetFile == null || targetFile.trim().isEmpty()) {
                String base = (currentClip.getName() != null && !currentClip.getName().trim().isEmpty())
                        ? currentClip.getName().trim()
                        : ((currentModelName != null && !currentModelName.trim().isEmpty()) ? currentModelName.trim() : "autosave");
                String suffix = isStaticPoseMode ? ".ospose.json" : ".osanim.json";
                targetFile = base.endsWith(suffix) ? base : base + suffix;
                this.currentFileName = targetFile;
            }
            try {
                AnimationFileManager.getInstance().saveAnimation(currentClip, targetFile);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void removed() {
        autoSaveProject();
        autoSavePoseToBlock(true);
        super.removed();
    }

    @Override
    public void onClose() {
        net.openstage.client.audio.AudioPlaybackService.getInstance().stop();

        autoSaveProject();
        autoSavePoseToBlock(true);

        if (exportRenderTarget != null) {
            exportRenderTarget.destroyBuffers();
            exportRenderTarget = null;
        }

        super.onClose();
    }

    private static Map<String, float[]> toRadiansMap(Map<String, float[]> degMap) {
        Map<String, float[]> result = new HashMap<>();

        if (degMap != null) {
            for (Map.Entry<String, float[]> entry : degMap.entrySet()) {
                float[] deg = entry.getValue();

                if (deg != null && deg.length >= 3) {
                    result.put(entry.getKey(), new float[]{
                            (float) Math.toRadians(deg[0]),

                            (float) Math.toRadians(deg[1]),

                            (float) Math.toRadians(deg[2])
                    });
                }
            }
        }

        return result;
    }

    private void applyInverseKinematics(String bone, float px, float py, float pz, boolean isBendable) {
        boolean isLeg = bone.contains("leg");
        boolean isArm = bone.contains("arm");

        if (!isLeg && !isArm) return;
        boolean isRight = bone.contains("right");
        String rootBone = isLeg ? (isRight ? "right_leg" : "left_leg") : (isRight ? "right_arm" : "left_arm");
        String childBone = isLeg ? (isRight ? "right_lower_leg" : "left_lower_leg") : (isRight ? "right_forearm" : "left_forearm");
        org.joml.Vector3f root = new org.joml.Vector3f(0.0f, isLeg ? 12.0f : 2.0f, 0.0f);
        org.joml.Vector3f target = new org.joml.Vector3f(px, py, pz);

        if (isBendable) {
            var ik = net.openstage.core.math.InverseKinematicsSolver.solveTwoBone(root, target, 6.0f, 6.0f, isLeg);

            onBoneTransformChanged(rootBone, ik.rootPitch(), ik.rootYaw(), ik.rootRoll());

            onBoneTransformChanged(childBone, ik.childPitch(), ik.childYaw(), ik.childRoll());

            if (propertyInspector != null) {
                if (bone.equals(rootBone)) {
                    propertyInspector.setBoneRotations(ik.rootPitch(), ik.rootYaw(), ik.rootRoll());
                } else {
                    propertyInspector.setBoneRotations(ik.childPitch(), ik.childYaw(), ik.childRoll());
                }
            }
        } else {
            float[] rot = net.openstage.core.math.InverseKinematicsSolver.solveSingleBone(root, target);

            onBoneTransformChanged(rootBone, rot[0], rot[1], rot[2]);

            if (propertyInspector != null) propertyInspector.setBoneRotations(rot[0], rot[1], rot[2]);
        }
    }

    @Override

    public void onAddParticleKeyframe(float x, float y, float z, float yaw, float pitch, float roll, String type, net.openstage.core.particle.ParticleEmitterType shape, int count, float speed, float spread, float duration, int colorRgb, float size) {
        if (currentClip == null || timelinePanel == null) return;
        float time = timelinePanel.getCurrentTimeSeconds();
        net.openstage.core.particle.ParticleKeyframe pk = new net.openstage.core.particle.ParticleKeyframe(time, type, shape, x, y, z, yaw, pitch, roll, count, speed, spread, duration, colorRgb, size, -1, "");

        currentClip.getParticleTrack().addKeyframe(pk);
    }

    @Override

    public void onParticleChanged(float x, float y, float z, float yaw, float pitch, float roll, String type, net.openstage.core.particle.ParticleEmitterType shape, int count, float speed, float spread, float duration, int colorRgb, float size) {
        if (currentClip == null || timelinePanel == null) return;
        float time = timelinePanel.getCurrentTimeSeconds();
        net.openstage.core.particle.ParticleKeyframe existing = currentClip.getParticleTrack().getKeyframeAt(time);

        if (existing != null) {
            currentClip.getParticleTrack().addKeyframe(new net.openstage.core.particle.ParticleKeyframe(time, type, shape, x, y, z, yaw, pitch, roll, count, speed, spread, duration, colorRgb, size, existing.attachedActorIndex(), existing.attachedBone()));
        }
    }

    @Override

    public void onParticleTestFire() {
        if (propertyInspector == null) return;

        net.openstage.core.particle.ParticleKeyframe testPk = new net.openstage.core.particle.ParticleKeyframe(
                0.0f,

                propertyInspector.getPartParticleId(),

                propertyInspector.getPartEmitterType(),

                propertyInspector.getPartX(),

                propertyInspector.getPartY(),

                propertyInspector.getPartZ(),

                propertyInspector.getPartYaw(),

                propertyInspector.getPartPitch(),

                propertyInspector.getPartRoll(),

                propertyInspector.getPartCount(),

                propertyInspector.getPartSpeed(),

                propertyInspector.getPartSpread(),

                propertyInspector.getPartDuration(),

                propertyInspector.getPartColorRgb(),

                propertyInspector.getPartSize(),

                -1,

                ""
        );
        double ox = (targetBlockPos != null) ? targetBlockPos.getX() + 0.5 : (this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getX() : 0.0);
        double oy = (targetBlockPos != null) ? targetBlockPos.getY() : (this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getY() : 64.0);
        double oz = (targetBlockPos != null) ? targetBlockPos.getZ() + 0.5 : (this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getZ() : 0.0);

        net.openstage.client.particle.ParticlePlaybackService.getInstance().triggerTestFire(testPk, ox, oy, oz);
    }

    @Override

    public void onAddParticlesRequested() {
        if (currentClip == null) return;
        float time = (timelinePanel != null) ? timelinePanel.getCurrentTimeSeconds() : 0.0f;

        if (currentClip.getParticleTrack().isEmpty()) {
            net.openstage.core.particle.ParticleKeyframe pk = new net.openstage.core.particle.ParticleKeyframe(
                    time, "minecraft:flame", net.openstage.core.particle.ParticleEmitterType.BURST_SPHERE,

                    0.0f, 1.0f, 0.0f, 0, 0, 0, 20, 0.2f, 0.2f, 1.0f, 0xFF5500, 1.0f, -1, ""
            );

            currentClip.getParticleTrack().addKeyframe(pk);
        }

        this.selectedBone = "particles";

        if (propertyInspector != null) {
            propertyInspector.setSelectedBone("particles");
            net.openstage.core.particle.ParticleKeyframe pk = currentClip.getParticleTrack().getKeyframeAt(time);

            if (pk != null) {
                propertyInspector.setParticleProperties(pk.posX(), pk.posY(), pk.posZ(), pk.rotYaw(), pk.rotPitch(), pk.rotRoll(), pk.particleId(), pk.emitterType(), pk.count(), pk.speed(), pk.spread(), pk.duration(), pk.colorRgb(), pk.size());
            }
        }
    }

    @Override

    public void onDeleteParticleEmitterRequested() {
        if (currentClip != null) {
            currentClip.getParticleTrack().clear();
        }

        this.selectedBone = "body";

        if (propertyInspector != null) {
            propertyInspector.setSelectedBone("body");
        }
    }

    private void renderTextCardDialog(GuiGraphics graphics, int mouseX, int mouseY, EditorTheme theme) {
        if (!isEditingTextCard && !isEditingTextCardClosing) return;

        graphics.bufferSource().endBatch();
        com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

        long now = System.currentTimeMillis();
        boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();
        float ease = 1.0f;
        float progress = 1.0f;

        if (isEditingTextCardClosing) {
            float p = anims ? Math.min(1.0f, (now - textCardCloseTime) / 140.0f) : 1.0f;
            progress = p;
            ease = (float) Math.pow(1.0f - p, 3);
            if (p >= 1.0f) {
                isEditingTextCard = false;
                isEditingTextCardClosing = false;
                activeEditingTextCard = null;
                return;
            }
        } else {
            float p = anims ? Math.min(1.0f, (now - textCardOpenTime) / 160.0f) : 1.0f;
            progress = p;
            ease = 1.0f - (float) Math.pow(1.0f - p, 3);
        }

        float scale = anims ? (0.88f + 0.12f * ease) : 1.0f;
        int bgAlpha = anims ? (int) (0x77 * ease) : 0x77;
        graphics.fill(0, 0, this.width, this.height, (bgAlpha << 24));

        int dw = 320;
        int dh = 150;
        int dx = (this.width - dw) / 2;
        int dy = (this.height - dh) / 2;
        int cx = dx + dw / 2;
        int cy = dy + dh / 2;

        graphics.pose().pushPose();
        if (anims && (progress < 1.0f || isEditingTextCardClosing)) {
            graphics.pose().translate(cx, cy, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.pose().translate(-cx, -cy, 0);
        }

        graphics.fill(dx, dy, dx + dw, dy + dh, theme.getPanelBg());
        graphics.renderOutline(dx, dy, dw, dh, theme.getDivider());

        ModelSettings.Language lang = (getModelSettings() != null) ? getModelSettings().getLanguage() : ModelSettings.Language.RU;
        String title = (lang == ModelSettings.Language.RU) ? "📝 Настройки текстовой плашки" : "📝 Text Plate Settings";
        graphics.drawString(this.font, title, dx + 14, dy + 12, 0xFFFFFFFF, false);

        int boxY = dy + 32;
        graphics.fill(dx + 14, boxY, dx + dw - 14, boxY + 22, 0xFF0B131B);
        graphics.renderOutline(dx + 14, boxY, dw - 28, 22, 0xFF38BDF8);

        String cursor = ((now / 450) % 2 == 0) ? "|" : "";
        String displayText = textCardInput + cursor;
        graphics.drawString(this.font, displayText, dx + 20, boxY + 7, 0xFFE0F2FE, false);

        String posName = (activeEditingTextCard != null) ? activeEditingTextCard.getPosition().name() : "CENTER";
        String effName = (activeEditingTextCard != null) ? activeEditingTextCard.getEffect().name() : "NONE";
        boolean posHov = mouseX >= dx + 14 && mouseX <= dx + 154 && mouseY >= dy + 62 && mouseY <= dy + 80;
        boolean effHov = mouseX >= dx + 164 && mouseX <= dx + 306 && mouseY >= dy + 62 && mouseY <= dy + 80;

        graphics.fill(dx + 14, dy + 62, dx + 154, dy + 80, posHov ? 0xFF1E293B : 0xFF141923);
        graphics.renderOutline(dx + 14, dy + 62, 140, 18, posHov ? 0xFF38BDF8 : theme.getDivider());
        graphics.drawString(this.font, "Поз: " + this.font.plainSubstrByWidth(posName, 105), dx + 18, dy + 67, 0xFFCBD5E1, false);

        graphics.fill(dx + 164, dy + 62, dx + 306, dy + 80, effHov ? 0xFF1E293B : 0xFF141923);
        graphics.renderOutline(dx + 164, dy + 62, 142, 18, effHov ? 0xFF38BDF8 : theme.getDivider());
        graphics.drawString(this.font, "Эфф: " + this.font.plainSubstrByWidth(effName, 105), dx + 168, dy + 67, 0xFFCBD5E1, false);

        int pbtnY = dy + 88;
        int pbtnW = 24;
        drawTextCardColorChip(graphics, dx + 14, pbtnY, pbtnW, 14, 0xFFFFFFFF, 0x88000000, mouseX, mouseY);
        drawTextCardColorChip(graphics, dx + 14 + 28, pbtnY, pbtnW, 14, 0xFF00F0FF, 0xAA051020, mouseX, mouseY);
        drawTextCardColorChip(graphics, dx + 14 + 56, pbtnY, pbtnW, 14, 0xFFFF007F, 0xAA200010, mouseX, mouseY);
        drawTextCardColorChip(graphics, dx + 14 + 84, pbtnY, pbtnW, 14, 0xFFF59E0B, 0xAA201500, mouseX, mouseY);
        drawTextCardColorChip(graphics, dx + 14 + 112, pbtnY, pbtnW, 14, 0xFF22C55E, 0xAA002005, mouseX, mouseY);

        int btnY = dy + 116;

        boolean delHov = mouseX >= dx + 14 && mouseX <= dx + 74 && mouseY >= btnY && mouseY <= btnY + 20;
        graphics.fill(dx + 14, btnY, dx + 74, btnY + 20, delHov ? 0xFF7F1D1D : 0xFF450A0A);
        graphics.renderOutline(dx + 14, btnY, 60, 20, delHov ? 0xFFEF4444 : 0xFF991B1B);
        graphics.drawCenteredString(this.font, "🗑 Удалить", dx + 44, btnY + 6, 0xFFFCA5A5);

        boolean saveHov = mouseX >= dx + dw - 146 && mouseX <= dx + dw - 78 && mouseY >= btnY && mouseY <= btnY + 20;
        graphics.fill(dx + dw - 146, btnY, dx + dw - 78, btnY + 20, saveHov ? 0xFF2563EB : theme.getAccent());
        graphics.renderOutline(dx + dw - 146, btnY, 68, 20, 0xFF60A5FA);
        graphics.drawCenteredString(this.font, "✔ Готово", dx + dw - 112, btnY + 6, 0xFFFFFFFF);

        boolean cancelHov = mouseX >= dx + dw - 72 && mouseX <= dx + dw - 14 && mouseY >= btnY && mouseY <= btnY + 20;
        graphics.fill(dx + dw - 72, btnY, dx + dw - 14, btnY + 20, cancelHov ? 0xFF334155 : 0xFF1E293B);
        graphics.renderOutline(dx + dw - 72, btnY, 58, 20, cancelHov ? 0xFF94A3B8 : theme.getDivider());
        graphics.drawCenteredString(this.font, "Отмена", dx + dw - 43, btnY + 6, 0xFFCBD5E1);

        graphics.pose().popPose();
    }

    private void drawTextCardColorChip(GuiGraphics graphics, int bx, int by, int bw, int bh, int textCol, int bgCol, int mx, int my) {
        boolean hovered = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        graphics.fill(bx, by, bx + bw, by + bh, bgCol);
        graphics.renderOutline(bx, by, bw, bh, hovered ? 0xFFFFFFFF : textCol);
        graphics.fill(bx + 2, by + 2, bx + bw - 2, by + bh - 2, textCol);
    }

    private void renderAudioTrackDialog(GuiGraphics graphics, int mouseX, int mouseY, EditorTheme theme) {
        if (!isEditingAudioTrack && !isEditingAudioTrackClosing) return;

        graphics.bufferSource().endBatch();
        com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

        long now = System.currentTimeMillis();
        boolean anims = (getModelSettings() == null) || getModelSettings().isUiAnimations();
        float ease = 1.0f;
        float progress = 1.0f;

        if (isEditingAudioTrackClosing) {
            float p = anims ? Math.min(1.0f, (now - audioTrackCloseTime) / 140.0f) : 1.0f;
            progress = p;
            ease = (float) Math.pow(1.0f - p, 3);
            if (p >= 1.0f) {
                isEditingAudioTrack = false;
                isEditingAudioTrackClosing = false;
                activeEditingAudioSegment = null;
                return;
            }
        } else {
            float p = anims ? Math.min(1.0f, (now - audioTrackOpenTime) / 160.0f) : 1.0f;
            progress = p;
            ease = 1.0f - (float) Math.pow(1.0f - p, 3);
        }

        float scale = anims ? (0.88f + 0.12f * ease) : 1.0f;
        int bgAlpha = anims ? (int) (0x77 * ease) : 0x77;
        graphics.fill(0, 0, this.width, this.height, (bgAlpha << 24));

        int dw = 320;
        int dh = 150;
        int dx = (this.width - dw) / 2;
        int dy = (this.height - dh) / 2;
        int cx = dx + dw / 2;
        int cy = dy + dh / 2;

        graphics.pose().pushPose();
        if (anims && (progress < 1.0f || isEditingAudioTrackClosing)) {
            graphics.pose().translate(cx, cy, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.pose().translate(-cx, -cy, 0);
        }

        graphics.fill(dx, dy, dx + dw, dy + dh, theme.getPanelBg());
        graphics.renderOutline(dx, dy, dw, dh, theme.getDivider());

        ModelSettings.Language lang = (getModelSettings() != null) ? getModelSettings().getLanguage() : ModelSettings.Language.RU;
        boolean ru = (lang == ModelSettings.Language.RU);

        graphics.fill(dx + 2, dy + 2, dx + dw - 2, dy + 24, theme.getTitleBarBg());
        graphics.drawString(this.font, ru ? "♫ Настройки аудиодорожки" : "♫ Audio Track Settings", dx + 8, dy + 7, theme.getAccent(), false);

        int closeX = dx + dw - 18;
        boolean closeHov = mouseX >= closeX - 2 && mouseX <= closeX + 14 && mouseY >= dy + 4 && mouseY <= dy + 18;
        graphics.drawString(this.font, "✕", closeX, dy + 6, closeHov ? 0xFFF87171 : 0xFF94A3B8, false);

        net.openstage.client.audio.AudioPlaybackService svc = net.openstage.client.audio.AudioPlaybackService.getInstance();
        net.openstage.client.audio.AudioPlaybackService.AudioSegment seg = (activeEditingAudioSegment != null) ? activeEditingAudioSegment : svc.getSelectedSegment();
        if (seg == null && !svc.getSegments().isEmpty()) {
            seg = svc.getSegments().get(0);
        }

        String fileName = (seg != null && seg.getSourceData() != null) ? seg.getSourceData().fileName() : "audio.wav";
        float segDur = (seg != null) ? seg.getDuration() : 0.0f;
        float segStart = (seg != null) ? seg.getStartOffset() : 0.0f;

        graphics.drawString(this.font, (ru ? "Файл: " : "File: ") + this.font.plainSubstrByWidth(fileName, 240), dx + 14, dy + 32, 0xFFFFFFFF, false);
        graphics.drawString(this.font, String.format(ru ? "Сегмент: от %.2fс | Длина: %.2fс" : "Segment: from %.2fs | Length: %.2fs", segStart, segDur), dx + 14, dy + 46, 0xFF94A3B8, false);

        int btnY1 = dy + 66;
        int btnW = 140;
        int btnH = 20;

        boolean resetHov = mouseX >= dx + 14 && mouseX <= dx + 14 + btnW && mouseY >= btnY1 && mouseY <= btnY1 + btnH;
        graphics.fill(dx + 14, btnY1, dx + 14 + btnW, btnY1 + btnH, resetHov ? 0xFF334155 : 0xFF1E293B);
        graphics.renderOutline(dx + 14, btnY1, btnW, btnH, resetHov ? theme.getAccent() : theme.getDivider());
        graphics.drawCenteredString(this.font, ru ? "↺ Сбросить обрезку" : "↺ Reset Trim", dx + 14 + btnW / 2, btnY1 + 6, 0xFFE2E8F0);

        boolean changeHov = mouseX >= dx + 166 && mouseX <= dx + 166 + btnW && mouseY >= btnY1 && mouseY <= btnY1 + btnH;
        graphics.fill(dx + 166, btnY1, dx + 166 + btnW, btnY1 + btnH, changeHov ? 0xFF334155 : 0xFF1E293B);
        graphics.renderOutline(dx + 166, btnY1, btnW, btnH, changeHov ? theme.getAccent() : theme.getDivider());
        graphics.drawCenteredString(this.font, ru ? "📁 Сменить файл..." : "📁 Change File...", dx + 166 + btnW / 2, btnY1 + 6, 0xFFE2E8F0);

        int btnY2 = dy + 92;

        boolean delSegHov = mouseX >= dx + 14 && mouseX <= dx + 14 + btnW && mouseY >= btnY2 && mouseY <= btnY2 + btnH;
        graphics.fill(dx + 14, btnY2, dx + 14 + btnW, btnY2 + btnH, delSegHov ? 0xFF7F1D1D : 0xFF450A0A);
        graphics.renderOutline(dx + 14, btnY2, btnW, btnH, delSegHov ? 0xFFEF4444 : 0xFF991B1B);
        graphics.drawCenteredString(this.font, ru ? "🗑 Удалить кусок" : "🗑 Delete Segment", dx + 14 + btnW / 2, btnY2 + 6, 0xFFFCA5A5);

        boolean delTrackHov = mouseX >= dx + 166 && mouseX <= dx + 166 + btnW && mouseY >= btnY2 && mouseY <= btnY2 + btnH;
        graphics.fill(dx + 166, btnY2, dx + 166 + btnW, btnY2 + btnH, delTrackHov ? 0xFF991B1B : 0xFF581C87);
        graphics.renderOutline(dx + 166, btnY2, btnW, btnH, delTrackHov ? 0xFFEF4444 : 0xFFA855F7);
        graphics.drawCenteredString(this.font, ru ? "❌ Удалить весь трек" : "❌ Remove All Audio", dx + 166 + btnW / 2, btnY2 + 6, 0xFFFBCFE8);

        int doneY = dy + 120;
        int doneW = 80;
        int doneX = dx + (dw - doneW) / 2;
        boolean doneHov = mouseX >= doneX && mouseX <= doneX + doneW && mouseY >= doneY && mouseY <= doneY + 20;
        graphics.fill(doneX, doneY, doneX + doneW, doneY + 20, doneHov ? 0xFF2563EB : theme.getAccent());
        graphics.renderOutline(doneX, doneY, doneW, 20, 0xFF60A5FA);
        graphics.drawCenteredString(this.font, ru ? "✔ Готово" : "✔ Done", doneX + doneW / 2, doneY + 6, 0xFFFFFFFF);

        graphics.pose().popPose();
    }

}
