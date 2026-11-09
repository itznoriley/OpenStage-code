package net.openstage.client.renderer;

import java.util.Map;

public final class ModelRaycaster {
    private record BoneBox(String name, float cx, float cy, float cz, float hw, float hh, float hd, float px, float py, float pz) {}

    public static String pickBone(
            double mouseX, double mouseY,
            int vpX, int vpY, int vpW, int vpH,
            float camPitch, float camYaw, float camZoom,
            Map<String, float[]> boneRotations,
            boolean isSlim, boolean isBendable
    ) {
        return pickBone(mouseX, mouseY, vpX, vpY, vpW, vpH, camPitch, camYaw, camZoom, 0.0f, 0.0f, 0.0f, 1.0f, isSlim, isBendable, boneRotations, null);
    }

    public static String pickBone(
            double mouseX, double mouseY,
            int vpX, int vpY, int vpW, int vpH,
            float camPitch, float camYaw, float camZoom,
            float camPanX, float camPanY,
            float rootYaw, float modelScale,
            boolean isSlim, boolean isBendable
    ) {
        return pickBone(mouseX, mouseY, vpX, vpY, vpW, vpH, camPitch, camYaw, camZoom, camPanX, camPanY, rootYaw, modelScale, isSlim, isBendable, null, null);
    }

    public static String pickBone(
            double mouseX, double mouseY,
            int vpX, int vpY, int vpW, int vpH,
            float camPitch, float camYaw, float camZoom,
            float camPanX, float camPanY,
            float rootYaw, float modelScale,
            boolean isSlim, boolean isBendable,
            Map<String, float[]> boneRotations
    ) {
        return pickBone(mouseX, mouseY, vpX, vpY, vpW, vpH, camPitch, camYaw, camZoom, camPanX, camPanY, rootYaw, modelScale, isSlim, isBendable, boneRotations, null);
    }

    public static String pickBone(
            double mouseX, double mouseY,
            int vpX, int vpY, int vpW, int vpH,
            float camPitch, float camYaw, float camZoom,
            float camPanX, float camPanY,
            float rootYaw, float modelScale,
            boolean isSlim, boolean isBendable,
            Map<String, float[]> boneRotations,
            Map<String, float[]> bonePositions
    ) {
        return pickBone(mouseX, mouseY, vpX, vpY, vpW, vpH, camPitch, camYaw, camZoom, camPanX, camPanY, rootYaw, modelScale, isSlim, isBendable, boneRotations, bonePositions, null);
    }

    public static String pickBone(
            double mouseX, double mouseY,
            int vpX, int vpY, int vpW, int vpH,
            float camPitch, float camYaw, float camZoom,
            float camPanX, float camPanY,
            float rootYaw, float modelScale,
            boolean isSlim, boolean isBendable,
            Map<String, float[]> boneRotations,
            Map<String, float[]> bonePositions,
            java.util.Set<String> ghostedBones
    ) {
        if (mouseX < vpX || mouseX > vpX + vpW || mouseY < vpY || mouseY > vpY + vpH) {
            return null;
        }

        float s = 120.0f * (2.0f / Math.max(0.4f, camZoom));
        float cx0 = vpX + vpW * 0.5f + camPanX * s;
        float cy0 = vpY + vpH * 0.5f + 55.0f + camPanY * s;
        float radRoot = (float) Math.toRadians(rootYaw);
        float cosR = (float) Math.cos(radRoot);
        float sinR = (float) Math.sin(radRoot);
        float radYaw = (float) Math.toRadians(camYaw);
        float radPitch = (float) Math.toRadians(camPitch);
        float cosY = (float) Math.cos(radYaw);
        float sinY = (float) Math.sin(radYaw);
        float cosP = (float) Math.cos(radPitch);
        float sinP = (float) Math.sin(radPitch);
        float armX = isSlim ? 0.28125f : 0.3125f;
        float armHW = isSlim ? 0.09375f : 0.125f;

        BoneBox[] boxes = isBendable ? new BoneBox[]{
                new BoneBox("head", 0.0f, 1.75f, 0.0f, 0.25f, 0.25f, 0.25f, 0.0f, 1.50f, 0.0f),
                new BoneBox("body", 0.0f, 1.125f, 0.0f, 0.25f, 0.375f, 0.125f, 0.0f, 0.75f, 0.0f),
                new BoneBox("right_arm", armX, 1.3125f, 0.0f, armHW, 0.1875f, 0.125f, armX, 1.375f, 0.0f),
                new BoneBox("right_forearm", armX, 0.9375f, 0.0f, armHW, 0.1875f, 0.125f, armX, 1.125f, 0.0f),
                new BoneBox("left_arm", -armX, 1.3125f, 0.0f, armHW, 0.1875f, 0.125f, -armX, 1.375f, 0.0f),
                new BoneBox("left_forearm", -armX, 0.9375f, 0.0f, armHW, 0.1875f, 0.125f, -armX, 1.125f, 0.0f),
                new BoneBox("right_leg", 0.11875f, 0.5625f, 0.0f, 0.125f, 0.1875f, 0.125f, 0.11875f, 0.75f, 0.0f),
                new BoneBox("right_lower_leg", 0.11875f, 0.1875f, 0.0f, 0.125f, 0.1875f, 0.125f, 0.11875f, 0.375f, 0.0f),
                new BoneBox("left_leg", -0.11875f, 0.5625f, 0.0f, 0.125f, 0.1875f, 0.125f, -0.11875f, 0.75f, 0.0f),
                new BoneBox("left_lower_leg", -0.11875f, 0.1875f, 0.0f, 0.125f, 0.1875f, 0.125f, -0.11875f, 0.375f, 0.0f)
        } : new BoneBox[]{
                new BoneBox("head", 0.0f, 1.75f, 0.0f, 0.25f, 0.25f, 0.25f, 0.0f, 1.50f, 0.0f),
                new BoneBox("body", 0.0f, 1.125f, 0.0f, 0.25f, 0.375f, 0.125f, 0.0f, 0.75f, 0.0f),
                new BoneBox("right_arm", armX, 1.125f, 0.0f, armHW, 0.375f, 0.125f, armX, 1.375f, 0.0f),
                new BoneBox("left_arm", -armX, 1.125f, 0.0f, armHW, 0.375f, 0.125f, -armX, 1.375f, 0.0f),
                new BoneBox("right_leg", 0.11875f, 0.375f, 0.0f, 0.125f, 0.375f, 0.125f, 0.11875f, 0.75f, 0.0f),
                new BoneBox("left_leg", -0.11875f, 0.375f, 0.0f, 0.125f, 0.375f, 0.125f, -0.11875f, 0.75f, 0.0f)
        };
        String bestBone = null;
        float bestZ = Float.MAX_VALUE;
        float bestDistNorm = Float.MAX_VALUE;
        String bestProximityBone = null;
        float bestProximityDist = Float.MAX_VALUE;

        for (BoneBox box : boxes) {
            if (ghostedBones != null && ghostedBones.contains(box.name)) {
                continue;
            }
            float minSX = Float.MAX_VALUE;
            float maxSX = -Float.MAX_VALUE;
            float minSY = Float.MAX_VALUE;
            float maxSY = -Float.MAX_VALUE;
            float sumZ = 0.0f;
            float sumSX = 0.0f;
            float sumSY = 0.0f;
            float[] rot = (boneRotations != null) ? boneRotations.get(box.name) : null;
            boolean hasRot = (rot != null && rot.length >= 3 && (rot[0] != 0.0f || rot[1] != 0.0f || rot[2] != 0.0f));
            float[] pos = (bonePositions != null) ? bonePositions.get(box.name) : null;
            float offX = (pos != null && pos.length >= 3) ? pos[0] : 0.0f;
            float offY = (pos != null && pos.length >= 3) ? pos[1] : 0.0f;
            float offZ = (pos != null && pos.length >= 3) ? pos[2] : 0.0f;
            float pX = box.px + offX;
            float pY = box.py + offY;
            float pZ = box.pz + offZ;

            for (int i = 0; i < 8; i++) {
                float kx = box.cx + offX + ((i & 1) != 0 ? box.hw : -box.hw);
                float ky = box.cy + offY + ((i & 2) != 0 ? box.hh : -box.hh);
                float kz = box.cz + offZ + ((i & 4) != 0 ? box.hd : -box.hd);

                if (hasRot) {
                    float[] rp = rotatePoint(kx, ky, kz, pX, pY, pZ, rot[0], rot[1], rot[2]);
                    kx = rp[0];
                    ky = rp[1];
                    kz = rp[2];
                }

                float rx = (kx * cosR + kz * sinR) * modelScale;
                float ry = ky * modelScale;
                float rz = (-kx * sinR + kz * cosR) * modelScale;
                float x1 = rx * cosY + rz * sinY;
                float y1 = ry;
                float z1 = -rx * sinY + rz * cosY;
                float x2 = x1;
                float y2 = y1 * cosP - z1 * sinP;
                float z2 = y1 * sinP + z1 * cosP;
                float sx = cx0 + x2 * s;
                float sy = cy0 - y2 * s;
                minSX = Math.min(minSX, sx);
                maxSX = Math.max(maxSX, sx);
                minSY = Math.min(minSY, sy);
                maxSY = Math.max(maxSY, sy);
                sumZ += z2;
                sumSX += sx;
                sumSY += sy;
            }

            float avgZ = sumZ * 0.125f;
            float centerSX = sumSX * 0.125f;
            float centerSY = sumSY * 0.125f;
            float margin = Math.max(5.0f, 0.03f * s);
            float bMinX = minSX - margin;
            float bMaxX = maxSX + margin;
            float bMinY = minSY - margin;
            float bMaxY = maxSY + margin;
            float halfW = Math.max(8.0f, (bMaxX - bMinX) * 0.5f);
            float halfH = Math.max(8.0f, (bMaxY - bMinY) * 0.5f);
            float ndx = (float) (mouseX - centerSX) / halfW;
            float ndy = (float) (mouseY - centerSY) / halfH;
            float distNorm = ndx * ndx + ndy * ndy;
            float proxDist = (float) Math.hypot(mouseX - centerSX, mouseY - centerSY);
            if (proxDist < bestProximityDist) {
                bestProximityDist = proxDist;
                bestProximityBone = box.name;
            }

            if (mouseX >= bMinX && mouseX <= bMaxX && mouseY >= bMinY && mouseY <= bMaxY) {
                if (avgZ < bestZ - 0.06f) {
                    bestZ = avgZ;
                    bestDistNorm = distNorm;
                    bestBone = box.name;
                } else if (Math.abs(avgZ - bestZ) <= 0.06f && distNorm < bestDistNorm) {
                    bestZ = Math.min(bestZ, avgZ);
                    bestDistNorm = distNorm;
                    bestBone = box.name;
                }
            }
        }

        if (bestBone == null && bestProximityDist <= Math.max(20.0f, 0.15f * s)) {
            bestBone = bestProximityBone;
        }

        return bestBone;
    }

    private static float[] rotatePoint(float x, float y, float z, float px, float py, float pz, float rxDeg, float ryDeg, float rzDeg) {
        float dx = x - px;
        float dy = y - py;
        float dz = z - pz;
        float rz = (float) Math.toRadians(rzDeg);
        float cosZ = (float) Math.cos(rz);
        float sinZ = (float) Math.sin(rz);
        float x1 = dx * cosZ - dy * sinZ;
        float y1 = dx * sinZ + dy * cosZ;
        float z1 = dz;
        float ry = (float) Math.toRadians(ryDeg);
        float cosY = (float) Math.cos(ry);
        float sinY = (float) Math.sin(ry);
        float x2 = x1 * cosY + z1 * sinY;
        float y2 = y1;
        float z2 = -x1 * sinY + z1 * cosY;
        float rx = (float) Math.toRadians(rxDeg);
        float cosX = (float) Math.cos(rx);
        float sinX = (float) Math.sin(rx);
        float x3 = x2;
        float y3 = y2 * cosX - z2 * sinX;
        float z3 = y2 * sinX + z2 * cosX;

        return new float[]{px + x3, py + y3, pz + z3};
    }

    public static String pickObjModel(
            double mouseX, double mouseY,
            int vpX, int vpY, int vpW, int vpH,
            float camPitch, float camYaw, float camZoom,
            float camPanX, float camPanY,
            float rootYaw, float modelScale,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            float[] rootPos, float[] rootRot
    ) {
        if (mouseX < vpX || mouseX > vpX + vpW || mouseY < vpY || mouseY > vpY + vpH) {
            return null;
        }

        float s = 120.0f * (2.0f / Math.max(0.4f, camZoom));
        float cx0 = vpX + vpW * 0.5f + camPanX * s;
        float cy0 = vpY + vpH * 0.5f + 55.0f + camPanY * s;
        float radRoot = (float) Math.toRadians(rootYaw);
        float cosR = (float) Math.cos(radRoot);
        float sinR = (float) Math.sin(radRoot);
        float radYaw = (float) Math.toRadians(camYaw);
        float radPitch = (float) Math.toRadians(camPitch);
        float cosY = (float) Math.cos(radYaw);
        float sinY = (float) Math.sin(radYaw);
        float cosP = (float) Math.cos(radPitch);
        float sinP = (float) Math.sin(radPitch);
        float offX = (rootPos != null && rootPos.length >= 3) ? rootPos[0] : 0.0f;
        float offY = (rootPos != null && rootPos.length >= 3) ? rootPos[1] : 0.0f;
        float offZ = (rootPos != null && rootPos.length >= 3) ? rootPos[2] : 0.0f;
        float cx = (minX + maxX) * 0.5f;
        float cy = (minY + maxY) * 0.5f;
        float cz = (minZ + maxZ) * 0.5f;
        float minSX = Float.MAX_VALUE;
        float maxSX = -Float.MAX_VALUE;
        float minSY = Float.MAX_VALUE;
        float maxSY = -Float.MAX_VALUE;

        for (int i = 0; i < 8; i++) {
            float kx = ((i & 1) != 0 ? maxX : minX) + offX;
            float ky = ((i & 2) != 0 ? maxY : minY) + offY;
            float kz = ((i & 4) != 0 ? maxZ : minZ) + offZ;

            if (rootRot != null && rootRot.length >= 3) {
                float rxDeg = (float) Math.toDegrees(rootRot[0]);
                float ryDeg = (float) Math.toDegrees(rootRot[1]);
                float rzDeg = (float) Math.toDegrees(rootRot[2]);
                float[] rp = rotatePoint(kx, ky, kz, cx + offX, cy + offY, cz + offZ, rxDeg, ryDeg, rzDeg);
                kx = rp[0];
                ky = rp[1];
                kz = rp[2];
            }

            float rx = (kx * cosR + kz * sinR) * modelScale;
            float ry = ky * modelScale;
            float rz = (-kx * sinR + kz * cosR) * modelScale;
            float x1 = rx * cosY + rz * sinY;
            float y1 = ry;
            float z1 = -rx * sinY + rz * cosY;
            float x2 = x1;
            float y2 = y1 * cosP - z1 * sinP;
            float z2 = y1 * sinP + z1 * cosP;
            float sx = cx0 + x2 * s;
            float sy = cy0 - y2 * s;
            minSX = Math.min(minSX, sx);
            maxSX = Math.max(maxSX, sx);
            minSY = Math.min(minSY, sy);
            maxSY = Math.max(maxSY, sy);
        }

        float margin = Math.max(8.0f, 0.04f * s);
        if (mouseX >= minSX - margin && mouseX <= maxSX + margin &&
            mouseY >= minSY - margin && mouseY <= maxSY + margin) {
            return "body";
        }
        return null;
    }
    public static boolean testHitCamera(
            double mouseX, double mouseY,
            int vpX, int vpY, int vpW, int vpH,
            float camPitch, float camYaw, float camZoom,
            float camPanX, float camPanY,
            float camX, float camY, float camZ
    ) {
        if (mouseX < vpX || mouseX > vpX + vpW || mouseY < vpY || mouseY > vpY + vpH) {
            return false;
        }
        float s = 120.0f * (2.0f / Math.max(0.4f, camZoom));
        float cx0 = vpX + vpW * 0.5f + camPanX * s;
        float cy0 = vpY + vpH * 0.5f + 55.0f + camPanY * s;
        float radYaw = (float) Math.toRadians(camYaw);
        float radPitch = (float) Math.toRadians(camPitch);
        float cosY = (float) Math.cos(radYaw);
        float sinY = (float) Math.sin(radYaw);
        float cosP = (float) Math.cos(radPitch);
        float sinP = (float) Math.sin(radPitch);
        float x1 = camX * cosY + camZ * sinY;
        float y1 = camY;
        float z1 = -camX * sinY + camZ * cosY;
        float x2 = x1;
        float y2 = y1 * cosP - z1 * sinP;
        float z2 = y1 * sinP + z1 * cosP;
        float sx = cx0 + x2 * s;
        float sy = cy0 - y2 * s;
        float hitRadius = Math.max(22.0f, 0.22f * s);
        return Math.hypot(mouseX - sx, mouseY - sy) <= hitRadius;
    }
}

