package net.openstage.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ObjModel {
    public record ObjVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}
    public record ObjTriangle(ObjVertex a, ObjVertex b, ObjVertex c) {}
    public record ObjFace(ObjVertex a, ObjVertex b, ObjVertex c, ObjVertex d) {}

    private final String name;
    private final List<ObjTriangle> triangles;
    private final List<ObjFace> faces;
    private final ResourceLocation textureLocation;
    private final float minX, minY, minZ;
    private final float maxX, maxY, maxZ;
    private static ResourceLocation defaultTextureLocation = null;

    public ObjModel(String name, List<ObjTriangle> triangles, List<ObjFace> faces, ResourceLocation textureLocation,
                    float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.name = name;
        this.triangles = triangles;
        this.faces = faces;
        this.textureLocation = textureLocation != null ? textureLocation : getDefaultTexture();
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public ObjModel(String name, List<ObjTriangle> triangles, ResourceLocation textureLocation,
                    float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this(name, triangles, toFaces(triangles), textureLocation, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static List<ObjFace> toFaces(List<ObjTriangle> tris) {
        List<ObjFace> list = new ArrayList<>(tris.size());
        for (ObjTriangle t : tris) {
            list.add(new ObjFace(t.a(), t.b(), t.c(), t.c()));
        }
        return list;
    }

    public String getName() {
        return name;
    }

    public List<ObjTriangle> getTriangles() {
        return triangles;
    }

    public List<ObjFace> getFaces() {
        return faces;
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    public float getMinX() { return minX; }
    public float getMinY() { return minY; }
    public float getMinZ() { return minZ; }
    public float getMaxX() { return maxX; }
    public float getMaxY() { return maxY; }
    public float getMaxZ() { return maxZ; }

    public float getWidth() { return maxX - minX; }
    public float getHeight() { return maxY - minY; }
    public float getDepth() { return maxZ - minZ; }

    public static synchronized ResourceLocation getDefaultTexture() {
        if (defaultTextureLocation != null) {
            return defaultTextureLocation;
        }
        try {
            NativeImage img = new NativeImage(16, 16, true);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    boolean border = (x == 0 || x == 15 || y == 0 || y == 15);
                    int color = border ? 0xFF94A3B8 : (((x + y) % 2 == 0) ? 0xFFE2E8F0 : 0xFFCBD5E1);
                    img.setPixelRGBA(x, y, color);
                }
            }
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("openstage", "textures/models/default_obj.png");
            Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(img));
            defaultTextureLocation = loc;
        } catch (Exception e) {
            defaultTextureLocation = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/iron_block.png");
        }
        return defaultTextureLocation;
    }

    public static ObjModel load(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }

        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<ObjTriangle> triangles = new ArrayList<>();
        List<ObjFace> faces = new ArrayList<>();
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        String mtlLibName = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("mtllib ")) {
                    mtlLibName = line.substring(7).trim();
                } else if (line.startsWith("v ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        float x = Float.parseFloat(parts[1]);
                        float y = Float.parseFloat(parts[2]);
                        float z = Float.parseFloat(parts[3]);
                        positions.add(new float[]{x, y, z});
                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (z < minZ) minZ = z;
                        if (x > maxX) maxX = x;
                        if (y > maxY) maxY = y;
                        if (z > maxZ) maxZ = z;
                    }
                } else if (line.startsWith("vt ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3) {
                        float u = Float.parseFloat(parts[1]);
                        float v = Float.parseFloat(parts[2]);
                        uvs.add(new float[]{u, v});
                    }
                } else if (line.startsWith("vn ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        float nx = Float.parseFloat(parts[1]);
                        float ny = Float.parseFloat(parts[2]);
                        float nz = Float.parseFloat(parts[3]);
                        normals.add(new float[]{nx, ny, nz});
                    }
                } else if (line.startsWith("f ")) {
                    String[] parts = line.split("\\s+");
                    List<ObjVertex> faceVerts = new ArrayList<>();
                    for (int i = 1; i < parts.length; i++) {
                        String p = parts[i];
                        if (p.isEmpty()) continue;
                        String[] idx = p.split("/", -1);
                        int posIdx = Integer.parseInt(idx[0]);
                        int uvIdx = (idx.length > 1 && !idx[1].isEmpty()) ? Integer.parseInt(idx[1]) : 0;
                        int nrmIdx = (idx.length > 2 && !idx[2].isEmpty()) ? Integer.parseInt(idx[2]) : 0;
                        float[] pos = resolveIndex(positions, posIdx);
                        float[] uv = (uvIdx != 0) ? resolveIndex(uvs, uvIdx) : new float[]{0.0f, 0.0f};
                        float[] nrm = (nrmIdx != 0) ? resolveIndex(normals, nrmIdx) : null;

                        if (pos != null) {
                            float nx = (nrm != null) ? nrm[0] : 0.0f;
                            float ny = (nrm != null) ? nrm[1] : 1.0f;
                            float nz = (nrm != null) ? nrm[2] : 0.0f;
                            faceVerts.add(new ObjVertex(pos[0], pos[1], pos[2], uv[0], uv[1], nx, ny, nz));
                        }
                    }

                    if (faceVerts.size() == 3) {
                        ObjVertex v0 = faceVerts.get(0);
                        ObjVertex v1 = faceVerts.get(1);
                        ObjVertex v2 = faceVerts.get(2);
                        if (v0.nx == 0.0f && v0.ny == 1.0f && v0.nz == 0.0f && normals.isEmpty()) {
                            float[] fn = computeNormal(v0, v1, v2);
                            v0 = new ObjVertex(v0.x, v0.y, v0.z, v0.u, v0.v, fn[0], fn[1], fn[2]);
                            v1 = new ObjVertex(v1.x, v1.y, v1.z, v1.u, v1.v, fn[0], fn[1], fn[2]);
                            v2 = new ObjVertex(v2.x, v2.y, v2.z, v2.u, v2.v, fn[0], fn[1], fn[2]);
                        }
                        triangles.add(new ObjTriangle(v0, v1, v2));
                        faces.add(new ObjFace(v0, v1, v2, v2));
                    } else if (faceVerts.size() == 4) {
                        ObjVertex v0 = faceVerts.get(0);
                        ObjVertex v1 = faceVerts.get(1);
                        ObjVertex v2 = faceVerts.get(2);
                        ObjVertex v3 = faceVerts.get(3);
                        if (v0.nx == 0.0f && v0.ny == 1.0f && v0.nz == 0.0f && normals.isEmpty()) {
                            float[] fn = computeNormal(v0, v1, v2);
                            v0 = new ObjVertex(v0.x, v0.y, v0.z, v0.u, v0.v, fn[0], fn[1], fn[2]);
                            v1 = new ObjVertex(v1.x, v1.y, v1.z, v1.u, v1.v, fn[0], fn[1], fn[2]);
                            v2 = new ObjVertex(v2.x, v2.y, v2.z, v2.u, v2.v, fn[0], fn[1], fn[2]);
                            v3 = new ObjVertex(v3.x, v3.y, v3.z, v3.u, v3.v, fn[0], fn[1], fn[2]);
                        }
                        triangles.add(new ObjTriangle(v0, v1, v2));
                        triangles.add(new ObjTriangle(v0, v2, v3));
                        faces.add(new ObjFace(v0, v1, v2, v3));
                    } else if (faceVerts.size() > 4) {
                        ObjVertex v0 = faceVerts.get(0);
                        for (int i = 1; i < faceVerts.size() - 1; i++) {
                            ObjVertex v1 = faceVerts.get(i);
                            ObjVertex v2 = faceVerts.get(i + 1);
                            if (v0.nx == 0.0f && v0.ny == 1.0f && v0.nz == 0.0f && normals.isEmpty()) {
                                float[] fn = computeNormal(v0, v1, v2);
                                v0 = new ObjVertex(v0.x, v0.y, v0.z, v0.u, v0.v, fn[0], fn[1], fn[2]);
                                v1 = new ObjVertex(v1.x, v1.y, v1.z, v1.u, v1.v, fn[0], fn[1], fn[2]);
                                v2 = new ObjVertex(v2.x, v2.y, v2.z, v2.u, v2.v, fn[0], fn[1], fn[2]);
                            }
                            triangles.add(new ObjTriangle(v0, v1, v2));
                            faces.add(new ObjFace(v0, v1, v2, v2));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }

        if (faces.isEmpty()) {
            return null;
        }

        ResourceLocation texLoc = resolveTexture(file, mtlLibName);

        return new ObjModel(file.getName(), triangles, faces, texLoc, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static float[] computeNormal(ObjVertex v0, ObjVertex v1, ObjVertex v2) {
        float ax = v1.x - v0.x;
        float ay = v1.y - v0.y;
        float az = v1.z - v0.z;
        float bx = v2.x - v0.x;
        float by = v2.y - v0.y;
        float bz = v2.z - v0.z;
        float fnx = ay * bz - az * by;
        float fny = az * bx - ax * bz;
        float fnz = ax * by - ay * bx;
        float len = (float) Math.sqrt(fnx * fnx + fny * fny + fnz * fnz);
        if (len > 0.0001f) {
            fnx /= len;
            fny /= len;
            fnz /= len;
        } else {
            fnx = 0.0f;
            fny = 1.0f;
            fnz = 0.0f;
        }
        return new float[]{fnx, fny, fnz};
    }

    private static ResourceLocation resolveTexture(File objFile, String mtlLibName) {
        File parent = objFile.getParentFile();
        String baseName = objFile.getName();
        if (baseName.lastIndexOf('.') > 0) {
            baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        }

        if (mtlLibName != null && parent != null) {
            File mtlFile = new File(parent, mtlLibName);
            if (mtlFile.exists() && mtlFile.isFile()) {
                String textureName = findTextureInMtl(mtlFile);
                if (textureName != null) {
                    File texFile = new File(parent, textureName);
                    if (texFile.exists() && texFile.isFile()) {
                        ResourceLocation loc = loadTextureFile(texFile);
                        if (loc != null) return loc;
                    }
                }
            }
        }

        if (parent != null) {
            File directPng = new File(parent, baseName + ".png");
            if (directPng.exists() && directPng.isFile()) {
                ResourceLocation loc = loadTextureFile(directPng);
                if (loc != null) return loc;
            }
            File[] pngs = parent.listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
            if (pngs != null && pngs.length > 0) {
                for (File p : pngs) {
                    if (p.getName().toLowerCase().contains(baseName.toLowerCase())) {
                        ResourceLocation loc = loadTextureFile(p);
                        if (loc != null) return loc;
                    }
                }
                if (pngs.length == 1) {
                    ResourceLocation loc = loadTextureFile(pngs[0]);
                    if (loc != null) return loc;
                }
            }
        }

        return getDefaultTexture();
    }

    private static String findTextureInMtl(File mtlFile) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(mtlFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("map_Kd ")) {
                    return line.substring(7).trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static ResourceLocation loadTextureFile(File imageFile) {
        try (InputStream in = new FileInputStream(imageFile)) {
            NativeImage img = NativeImage.read(in);
            String safeName = imageFile.getName().toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("openstage", "textures/models/" + safeName);
            Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(img));
            return loc;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static float[] resolveIndex(List<float[]> list, int idx) {
        if (idx > 0 && idx <= list.size()) {
            return list.get(idx - 1);
        } else if (idx < 0 && Math.abs(idx) <= list.size()) {
            return list.get(list.size() + idx);
        }
        return null;
    }

    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
                       float r, float g, float b, float a) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(textureLocation));
        Matrix4f mat = poseStack.last().pose();
        PoseStack.Pose normalPose = poseStack.last();

        for (ObjFace f : faces) {
            renderVertex(consumer, mat, normalPose, f.a, light, overlay, r, g, b, a);
            renderVertex(consumer, mat, normalPose, f.b, light, overlay, r, g, b, a);
            renderVertex(consumer, mat, normalPose, f.c, light, overlay, r, g, b, a);
            renderVertex(consumer, mat, normalPose, f.d, light, overlay, r, g, b, a);
        }
    }

    private static void renderVertex(VertexConsumer consumer, Matrix4f mat, PoseStack.Pose normalPose,
                                     ObjVertex v, int light, int overlay, float r, float g, float b, float a) {
        consumer.addVertex(mat, v.x, v.y, v.z)
                .setColor(r, g, b, a)
                .setUv(v.u, 1.0f - v.v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(normalPose, v.nx, v.ny, v.nz);
    }
}

