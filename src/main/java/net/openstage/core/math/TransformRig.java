package net.openstage.core.math;

import org.joml.Quaternionf;
import java.util.HashMap;
import java.util.Map;

public final class TransformRig {
    public static final String BONE_ROOT         = "root";
    public static final String BONE_BODY         = "body";
    public static final String BONE_HEAD         = "head";
    public static final String BONE_RIGHT_ARM       = "right_arm";
    public static final String BONE_LEFT_ARM        = "left_arm";
    public static final String BONE_RIGHT_FOREARM    = "right_forearm";
    public static final String BONE_LEFT_FOREARM     = "left_forearm";
    public static final String BONE_RIGHT_LEG        = "right_leg";
    public static final String BONE_LEFT_LEG         = "left_leg";
    public static final String BONE_RIGHT_LOWER_LEG  = "right_lower_leg";
    public static final String BONE_LEFT_LOWER_LEG   = "left_lower_leg";
    public static final String BONE_HAT          = "hat";
    public static final String BONE_JACKET       = "jacket";
    public static final String BONE_RIGHT_SLEEVE = "right_sleeve";
    public static final String BONE_LEFT_SLEEVE  = "left_sleeve";
    public static final String BONE_RIGHT_PANTS  = "right_pants";
    public static final String BONE_LEFT_PANTS   = "left_pants";
    private static final Map<String, String> PARENT_MAP = new HashMap<>();
    private static final Map<String, Boolean> OVERLAY_SET = new HashMap<>();
    private static final Map<String, String> ALIAS_MAP = new HashMap<>();
    private static final String[] ALL_BONES;

    static {
        PARENT_MAP.put(BONE_ROOT,         null);
        PARENT_MAP.put(BONE_BODY,         BONE_ROOT);
        PARENT_MAP.put(BONE_HEAD,         BONE_BODY);
        PARENT_MAP.put(BONE_RIGHT_ARM,       BONE_BODY);
        PARENT_MAP.put(BONE_LEFT_ARM,        BONE_BODY);
        PARENT_MAP.put(BONE_RIGHT_FOREARM,   BONE_RIGHT_ARM);
        PARENT_MAP.put(BONE_LEFT_FOREARM,    BONE_LEFT_ARM);
        PARENT_MAP.put(BONE_RIGHT_LEG,       BONE_BODY);
        PARENT_MAP.put(BONE_LEFT_LEG,        BONE_BODY);
        PARENT_MAP.put(BONE_RIGHT_LOWER_LEG, BONE_RIGHT_LEG);
        PARENT_MAP.put(BONE_LEFT_LOWER_LEG,  BONE_LEFT_LEG);

        PARENT_MAP.put(BONE_HAT,          BONE_HEAD);
        PARENT_MAP.put(BONE_JACKET,       BONE_BODY);
        PARENT_MAP.put(BONE_RIGHT_SLEEVE, BONE_RIGHT_ARM);
        PARENT_MAP.put(BONE_LEFT_SLEEVE,  BONE_LEFT_ARM);
        PARENT_MAP.put(BONE_RIGHT_PANTS,  BONE_RIGHT_LEG);
        PARENT_MAP.put(BONE_LEFT_PANTS,   BONE_LEFT_LEG);

        OVERLAY_SET.put(BONE_HAT, true);
        OVERLAY_SET.put(BONE_JACKET, true);
        OVERLAY_SET.put(BONE_RIGHT_SLEEVE, true);
        OVERLAY_SET.put(BONE_LEFT_SLEEVE, true);
        OVERLAY_SET.put(BONE_RIGHT_PANTS, true);
        OVERLAY_SET.put(BONE_LEFT_PANTS, true);

        ALIAS_MAP.put("torso",      BONE_BODY);
        ALIAS_MAP.put("chest",      BONE_BODY);
        ALIAS_MAP.put("rightarm",   BONE_RIGHT_ARM);
        ALIAS_MAP.put("leftarm",    BONE_LEFT_ARM);
        ALIAS_MAP.put("rightleg",   BONE_RIGHT_LEG);
        ALIAS_MAP.put("leftleg",    BONE_LEFT_LEG);
        ALIAS_MAP.put("right_hand", BONE_RIGHT_ARM);
        ALIAS_MAP.put("left_hand",  BONE_LEFT_ARM);
        ALIAS_MAP.put("right_elbow",     BONE_RIGHT_FOREARM);
        ALIAS_MAP.put("left_elbow",      BONE_LEFT_FOREARM);
        ALIAS_MAP.put("right_knee",      BONE_RIGHT_LOWER_LEG);
        ALIAS_MAP.put("left_knee",       BONE_LEFT_LOWER_LEG);
        ALIAS_MAP.put("helmet",     BONE_HAT);
        ALL_BONES = PARENT_MAP.keySet().toArray(new String[0]);
    }

    private TransformRig() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String[] getAllBoneNames() {
        return ALL_BONES.clone();
    }

    public static String getParentBone(String boneName) {
        if (boneName == null) return null;
        return PARENT_MAP.get(resolveAlias(boneName));
    }

    public static boolean isOverlayBone(String boneName) {
        if (boneName == null) return false;
        return OVERLAY_SET.containsKey(resolveAlias(boneName));
    }

    public static String resolveAlias(String boneName) {
        if (boneName == null) return null;
        return ALIAS_MAP.getOrDefault(boneName.toLowerCase(), boneName);
    }

    public static boolean isKnownBone(String boneName) {
        if (boneName == null) return false;
        return PARENT_MAP.containsKey(boneName) ||
               ALIAS_MAP.containsKey(boneName.toLowerCase());
    }

    public static Quaternionf eulerYZXToQuaternion(float yDeg, float zDeg, float xDeg) {
        float yRad = (float) Math.toRadians(yDeg);
        float zRad = (float) Math.toRadians(zDeg);
        float xRad = (float) Math.toRadians(xDeg);
        Quaternionf qy = new Quaternionf().rotationY(yRad);
        Quaternionf qz = new Quaternionf().rotationZ(zRad);
        Quaternionf qx = new Quaternionf().rotationX(xRad);

        return qy.mul(qz).mul(qx);
    }
}

