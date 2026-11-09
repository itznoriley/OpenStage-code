package net.openstage.client.particle;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.openstage.core.animation.AnimationClip;
import net.openstage.core.particle.ParticleEmitterType;
import net.openstage.core.particle.ParticleKeyframe;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ParticlePlaybackService {
    public static final class ViewportParticle {
        public float x, y, z;
        public float vx, vy, vz;
        public int color;
        public float size;
        public float age;
        public float maxAge;
        public String particleId;

        public ViewportParticle(float x, float y, float z, float vx, float vy, float vz, int color, float size, float maxAge, String particleId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.color = color;
            this.size = size;
            this.age = 0.0f;
            this.maxAge = maxAge;
            this.particleId = particleId;
        }

        public boolean tick(float dt) {
            age += dt;
            x += vx * dt * 20.0f;
            y += vy * dt * 20.0f;
            z += vz * dt * 20.0f;
            float drag = (float) Math.pow(0.96, Math.max(0.1, dt * 20.0));
            vx *= drag;
            vy = (vy - 0.005f * dt * 20.0f) * drag;
            vz *= drag;
            return age < maxAge;
        }
    }

    private static final ParticlePlaybackService INSTANCE = new ParticlePlaybackService();
    private final Random random = new Random();
    private final Map<String, ParticleOptions> defaultOptions = new LinkedHashMap<>();
    private final List<String> availableParticleIds = new ArrayList<>();
    private final List<ViewportParticle> activeViewportParticles = new ArrayList<>();
    private static ResourceLocation defaultParticleTexture = null;

    private ParticlePlaybackService() {
        registerDefault("minecraft:flame", ParticleTypes.FLAME);
        registerDefault("minecraft:soul_fire_flame", ParticleTypes.SOUL_FIRE_FLAME);
        registerDefault("minecraft:crit", ParticleTypes.CRIT);
        registerDefault("minecraft:enchanted_hit", ParticleTypes.ENCHANTED_HIT);
        registerDefault("minecraft:smoke", ParticleTypes.SMOKE);
        registerDefault("minecraft:large_smoke", ParticleTypes.LARGE_SMOKE);
        registerDefault("minecraft:campfire_cosy_smoke", ParticleTypes.CAMPFIRE_COSY_SMOKE);
        registerDefault("minecraft:portal", ParticleTypes.PORTAL);
        registerDefault("minecraft:reverse_portal", ParticleTypes.REVERSE_PORTAL);
        registerDefault("minecraft:dragon_breath", ParticleTypes.DRAGON_BREATH);
        registerDefault("minecraft:electric_spark", ParticleTypes.ELECTRIC_SPARK);
        registerDefault("minecraft:sonic_boom", ParticleTypes.SONIC_BOOM);
        registerDefault("minecraft:cherry_leaves", ParticleTypes.CHERRY_LEAVES);
        registerDefault("minecraft:sculk_charge_pop", ParticleTypes.SCULK_CHARGE_POP);
        registerDefault("minecraft:sculk_soul", ParticleTypes.SCULK_SOUL);
        registerDefault("minecraft:explosion", ParticleTypes.EXPLOSION);
        registerDefault("minecraft:explosion_emitter", ParticleTypes.EXPLOSION_EMITTER);
        registerDefault("minecraft:totem_of_undying", ParticleTypes.TOTEM_OF_UNDYING);
        registerDefault("minecraft:firework", ParticleTypes.FIREWORK);
        registerDefault("minecraft:heart", ParticleTypes.HEART);
        registerDefault("minecraft:snowflake", ParticleTypes.SNOWFLAKE);
        registerDefault("minecraft:glow", ParticleTypes.GLOW);
        registerDefault("minecraft:wax_on", ParticleTypes.WAX_ON);
        registerDefault("minecraft:squid_ink", ParticleTypes.SQUID_INK);
        registerDefault("minecraft:glow_squid_ink", ParticleTypes.GLOW_SQUID_INK);
        registerDefault("minecraft:witch", ParticleTypes.WITCH);
        registerDefault("minecraft:dust", createDustOptions(0xFF5500, 1.0f));
    }

    public static synchronized ResourceLocation getDefaultParticleTexture() {
        if (defaultParticleTexture != null) return defaultParticleTexture;
        try {
            NativeImage img = new NativeImage(16, 16, true);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    float dx = (x - 7.5f) / 7.5f;
                    float dy = (y - 7.5f) / 7.5f;
                    float dist2 = dx * dx + dy * dy;
                    if (dist2 <= 1.0f) {
                        int alpha = (int) ((1.0f - dist2) * 255.0f);
                        int col = (alpha << 24) | 0x00FFFFFF;
                        img.setPixelRGBA(x, y, col);
                    } else {
                        img.setPixelRGBA(x, y, 0);
                    }
                }
            }
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("openstage", "textures/particle/soft_particle.png");
            Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(img));
            defaultParticleTexture = loc;
        } catch (Exception e) {
            defaultParticleTexture = ResourceLocation.withDefaultNamespace("textures/particle/flame.png");
        }
        return defaultParticleTexture;
    }

    private void registerDefault(String id, ParticleOptions opt) {
        defaultOptions.put(id, opt);
        availableParticleIds.add(id);
    }

    public static ParticlePlaybackService getInstance() {
        return INSTANCE;
    }

    public List<String> getAvailableParticleIds() {
        return Collections.unmodifiableList(availableParticleIds);
    }

    public static DustParticleOptions createDustOptions(int colorRgb, float size) {
        float r = ((colorRgb >> 16) & 0xFF) / 255.0f;
        float g = ((colorRgb >> 8) & 0xFF) / 255.0f;
        float b = (colorRgb & 0xFF) / 255.0f;
        return new DustParticleOptions(new Vector3f(r, g, b), size);
    }

    public ParticleOptions resolveOptions(String particleId, int colorRgb, float size) {
        if ("minecraft:dust".equals(particleId)) {
            return createDustOptions(colorRgb, size);
        }
        return defaultOptions.getOrDefault(particleId, ParticleTypes.FLAME);
    }

    public int resolveParticleColor(String id, int colorRgb) {
        if ("minecraft:dust".equals(id)) return 0xFF000000 | (colorRgb & 0x00FFFFFF);
        if (id.contains("flame")) return 0xFFFF6600;
        if (id.contains("soul")) return 0xFF00E5FF;
        if (id.contains("crit")) return 0xFFFFD700;
        if (id.contains("portal")) return 0xFFBD00FF;
        if (id.contains("smoke")) return 0xFFAAAAAA;
        if (id.contains("sonic")) return 0xFF00F5D4;
        if (id.contains("totem")) return 0xFFFFD166;
        if (id.contains("cherry")) return 0xFFFF99C8;
        if (id.contains("electric")) return 0xFF38BDF8;
        return 0xFFFF7700;
    }

    public ResourceLocation getParticleTexture(String id) {
        if (id == null) return ResourceLocation.withDefaultNamespace("textures/particle/flame.png");
        if (id.contains("flame") && !id.contains("soul")) return ResourceLocation.withDefaultNamespace("textures/particle/flame.png");
        if (id.contains("soul")) return ResourceLocation.withDefaultNamespace("textures/particle/soul_fire_flame.png");
        if (id.contains("crit") || id.contains("enchanted")) return ResourceLocation.withDefaultNamespace("textures/particle/critical_hit.png");
        if (id.contains("smoke")) return ResourceLocation.withDefaultNamespace("textures/particle/big_smoke_0.png");
        if (id.contains("heart")) return ResourceLocation.withDefaultNamespace("textures/particle/heart.png");
        if (id.contains("cherry")) return ResourceLocation.withDefaultNamespace("textures/particle/cherry_0.png");
        if (id.contains("sonic")) return ResourceLocation.withDefaultNamespace("textures/particle/sonic_boom_0.png");
        if (id.contains("explosion")) return ResourceLocation.withDefaultNamespace("textures/particle/explosion_0.png");
        if (id.contains("totem") || id.contains("spark") || id.contains("wax")) return ResourceLocation.withDefaultNamespace("textures/particle/glitter_4.png");
        if (id.contains("portal")) return ResourceLocation.withDefaultNamespace("textures/particle/sga_a.png");
        if (id.contains("sculk")) return ResourceLocation.withDefaultNamespace("textures/particle/sculk_soul_0.png");
        if (id.contains("dust")) return ResourceLocation.withDefaultNamespace("textures/particle/generic_0.png");
        return ResourceLocation.withDefaultNamespace("textures/particle/flame.png");
    }

    public void playAtTime(AnimationClip clip, float currentTime, float lastTime, double originX, double originY, double originZ) {
        if (clip == null) return;
        List<ParticleKeyframe> keys = clip.getParticleTrack().getKeyframes();
        if (keys.isEmpty()) return;

        for (ParticleKeyframe k : keys) {
            if (k.isContinuous()) {
                if (currentTime >= k.time() && currentTime <= k.time() + k.duration()) {
                    spawnEmitter(k, originX, originY, originZ, false);
                }
            } else {
                if (lastTime < k.time() && currentTime >= k.time()) {
                    spawnEmitter(k, originX, originY, originZ, true);
                }
            }
        }
    }

    public void triggerTestFire(ParticleKeyframe keyframe, double originX, double originY, double originZ) {
        if (keyframe == null) return;
        spawnEmitter(keyframe, originX, originY, originZ, true, true);
    }

    public void spawnEmitter(ParticleKeyframe keyframe, double originX, double originY, double originZ, boolean isBurst) {
        spawnEmitter(keyframe, originX, originY, originZ, isBurst, false);
    }

    public void spawnEmitter(ParticleKeyframe keyframe, double originX, double originY, double originZ, boolean isBurst, boolean isTestFire) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        double cx = originX + (double) keyframe.posX();
        double cy = originY + (double) keyframe.posY();
        double cz = originZ + (double) keyframe.posZ();
        float lx = keyframe.posX();
        float ly = keyframe.posY();
        float lz = keyframe.posZ();
        String pId = keyframe.particleId();
        ParticleOptions options = resolveOptions(pId, keyframe.colorRgb(), keyframe.size());
        ParticleEmitterType type = keyframe.emitterType();
        int partColor = resolveParticleColor(pId, keyframe.colorRgb());
        int spawnCount = isBurst ? keyframe.count() : Math.max(1, keyframe.count() / 10);
        float speed = isTestFire ? Math.max(0.04f, keyframe.speed() * 0.35f) : keyframe.speed();
        float spread = keyframe.spread();
        float pSize = keyframe.size();
        float baseAge = isTestFire ? 2.5f : 1.0f;

        Quaternionf q = new Quaternionf()
                .rotateY((float) Math.toRadians(-keyframe.rotYaw()))
                .rotateX((float) Math.toRadians(keyframe.rotPitch()))
                .rotateZ((float) Math.toRadians(keyframe.rotRoll()));

        switch (type) {
            case BURST_SPHERE -> spawnSphere(level, options, cx, cy, cz, lx, ly, lz, q, spawnCount, speed, spread, partColor, pSize, pId, baseAge);
            case BURST_RING -> spawnRing(level, options, cx, cy, cz, lx, ly, lz, q, spawnCount, speed, partColor, pSize, pId, baseAge);
            case STREAM_CONE -> spawnCone(level, options, cx, cy, cz, lx, ly, lz, q, spawnCount, speed, spread, partColor, pSize, pId, baseAge);
            case STREAM_SPIRAL -> spawnSpiral(level, options, cx, cy, cz, lx, ly, lz, q, spawnCount, speed, spread, partColor, pSize, pId, baseAge);
            case AMBIENT_BOX -> spawnBox(level, options, cx, cy, cz, lx, ly, lz, q, spawnCount, speed, spread, partColor, pSize, pId, baseAge);
            case POINT -> spawnPoint(level, options, cx, cy, cz, lx, ly, lz, q, spawnCount, speed, partColor, pSize, pId, baseAge);
        }
    }

    private void spawnSphere(ClientLevel level, ParticleOptions opt, double cx, double cy, double cz, float lx, float ly, float lz, Quaternionf q, int count, float speed, float spread, int color, float size, String pId, float baseAge) {
        for (int i = 0; i < count; i++) {
            double u = random.nextDouble();
            double v = random.nextDouble();
            double theta = u * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * v - 1.0);
            double r = Math.sin(phi);
            float vx = (float) (r * Math.cos(theta) * speed);
            float vy = (float) (Math.cos(phi) * speed);
            float vz = (float) (r * Math.sin(theta) * speed);
            Vector3f dir = new Vector3f(vx, vy, vz);
            q.transform(dir);
            double ox = cx + (random.nextDouble() - 0.5) * spread;
            double oy = cy + (random.nextDouble() - 0.5) * spread;
            double oz = cz + (random.nextDouble() - 0.5) * spread;

            if (level != null) {
                level.addAlwaysVisibleParticle(opt, true, ox, oy, oz, dir.x, dir.y, dir.z);
            }

            float px = lx + (float) (random.nextDouble() - 0.5) * spread;
            float py = ly + (float) (random.nextDouble() - 0.5) * spread;
            float pz = lz + (float) (random.nextDouble() - 0.5) * spread;
            if (activeViewportParticles.size() < 400) {
                activeViewportParticles.add(new ViewportParticle(px, py, pz, dir.x, dir.y, dir.z, color, size, baseAge + (float) random.nextDouble() * 0.4f, pId));
            }
        }
    }

    private void spawnRing(ClientLevel level, ParticleOptions opt, double cx, double cy, double cz, float lx, float ly, float lz, Quaternionf q, int count, float speed, int color, float size, String pId, float baseAge) {
        for (int i = 0; i < count; i++) {
            double angle = (2.0 * Math.PI * i) / (double) count;
            float vx = (float) (Math.cos(angle) * speed);
            float vy = 0.0f;
            float vz = (float) (Math.sin(angle) * speed);
            Vector3f dir = new Vector3f(vx, vy, vz);
            q.transform(dir);

            if (level != null) {
                level.addAlwaysVisibleParticle(opt, true, cx, cy, cz, dir.x, dir.y, dir.z);
            }
            if (activeViewportParticles.size() < 400) {
                activeViewportParticles.add(new ViewportParticle(lx, ly, lz, dir.x, dir.y, dir.z, color, size, baseAge, pId));
            }
        }
    }

    private void spawnCone(ClientLevel level, ParticleOptions opt, double cx, double cy, double cz, float lx, float ly, float lz, Quaternionf q, int count, float speed, float spread, int color, float size, String pId, float baseAge) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double radius = Math.sqrt(random.nextDouble()) * spread;
            float vx = (float) (Math.cos(angle) * radius);
            float vy = 1.0f;
            float vz = (float) (Math.sin(angle) * radius);
            Vector3f dir = new Vector3f(vx, vy, vz).normalize().mul(speed);
            q.transform(dir);

            if (level != null) {
                level.addAlwaysVisibleParticle(opt, true, cx, cy, cz, dir.x, dir.y, dir.z);
            }
            if (activeViewportParticles.size() < 400) {
                activeViewportParticles.add(new ViewportParticle(lx, ly, lz, dir.x, dir.y, dir.z, color, size, baseAge, pId));
            }
        }
    }

    private void spawnSpiral(ClientLevel level, ParticleOptions opt, double cx, double cy, double cz, float lx, float ly, float lz, Quaternionf q, int count, float speed, float spread, int color, float size, String pId, float baseAge) {
        long now = System.currentTimeMillis();
        double baseAngle = (now % 3600L) / 3600.0 * 2.0 * Math.PI * 4.0;

        for (int i = 0; i < count; i++) {
            double angle = baseAngle + (2.0 * Math.PI * i) / (double) count;
            float offX = (float) (Math.cos(angle) * spread);
            float offY = 0.0f;
            float offZ = (float) (Math.sin(angle) * spread);
            Vector3f localPos = new Vector3f(offX, offY, offZ);
            q.transform(localPos);
            float vx = (float) (-Math.sin(angle) * 0.2f);
            float vy = 1.0f;
            float vz = (float) (Math.cos(angle) * 0.2f);
            Vector3f dir = new Vector3f(vx, vy, vz).normalize().mul(speed);
            q.transform(dir);

            if (level != null) {
                level.addAlwaysVisibleParticle(opt, true, cx + localPos.x, cy + localPos.y, cz + localPos.z, dir.x, dir.y, dir.z);
            }
            if (activeViewportParticles.size() < 400) {
                activeViewportParticles.add(new ViewportParticle(lx + localPos.x, ly + localPos.y, lz + localPos.z, dir.x, dir.y, dir.z, color, size, baseAge, pId));
            }
        }
    }

    private void spawnBox(ClientLevel level, ParticleOptions opt, double cx, double cy, double cz, float lx, float ly, float lz, Quaternionf q, int count, float speed, float spread, int color, float size, String pId, float baseAge) {
        for (int i = 0; i < count; i++) {
            float offX = (float) ((random.nextDouble() * 2.0 - 1.0) * spread);
            float offY = (float) ((random.nextDouble() * 2.0 - 1.0) * spread);
            float offZ = (float) ((random.nextDouble() * 2.0 - 1.0) * spread);
            Vector3f localPos = new Vector3f(offX, offY, offZ);
            q.transform(localPos);
            Vector3f dir = new Vector3f(0.0f, speed * 0.2f, 0.0f);
            q.transform(dir);

            if (level != null) {
                level.addAlwaysVisibleParticle(opt, true, cx + localPos.x, cy + localPos.y, cz + localPos.z, dir.x, dir.y, dir.z);
            }
            if (activeViewportParticles.size() < 400) {
                activeViewportParticles.add(new ViewportParticle(lx + localPos.x, ly + localPos.y, lz + localPos.z, dir.x, dir.y, dir.z, color, size, baseAge, pId));
            }
        }
    }

    private void spawnPoint(ClientLevel level, ParticleOptions opt, double cx, double cy, double cz, float lx, float ly, float lz, Quaternionf q, int count, float speed, int color, float size, String pId, float baseAge) {
        for (int i = 0; i < count; i++) {
            Vector3f dir = new Vector3f(0.0f, speed, 0.0f);
            q.transform(dir);
            if (level != null) {
                level.addAlwaysVisibleParticle(opt, true, cx, cy, cz, dir.x, dir.y, dir.z);
            }
            if (activeViewportParticles.size() < 400) {
                activeViewportParticles.add(new ViewportParticle(lx, ly, lz, dir.x, dir.y, dir.z, color, size, baseAge, pId));
            }
        }
    }

    public void renderViewportParticles(PoseStack pose, MultiBufferSource buffers, float dt) {
        if (activeViewportParticles.isEmpty()) return;
        Matrix4f mat = pose.last().pose();

        activeViewportParticles.removeIf(p -> !p.tick(dt));
        ResourceLocation fallbackTex = getDefaultParticleTexture();

        for (ViewportParticle p : activeViewportParticles) {
            ResourceLocation tex = getParticleTexture(p.particleId);
            if (tex == null) tex = fallbackTex;
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(tex));
            float fade = Math.max(0.0f, 1.0f - (p.age / p.maxAge));
            int a = Math.clamp((int) (fade * 255.0f), 0, 255);
            int r = (p.color >> 16) & 0xFF;
            int g = (p.color >> 8) & 0xFF;
            int b = p.color & 0xFF;
            float hs = Math.max(0.04f, p.size * 0.065f);

            consumer.addVertex(mat, p.x - hs, p.y - hs, p.z).setColor(r, g, b, a).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0.0f, 1.0f, 0.0f);
            consumer.addVertex(mat, p.x + hs, p.y - hs, p.z).setColor(r, g, b, a).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0.0f, 1.0f, 0.0f);
            consumer.addVertex(mat, p.x + hs, p.y + hs, p.z).setColor(r, g, b, a).setUv(1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0.0f, 1.0f, 0.0f);
            consumer.addVertex(mat, p.x - hs, p.y + hs, p.z).setColor(r, g, b, a).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0.0f, 1.0f, 0.0f);

            consumer.addVertex(mat, p.x, p.y - hs, p.z - hs).setColor(r, g, b, a).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(1.0f, 0.0f, 0.0f);
            consumer.addVertex(mat, p.x, p.y - hs, p.z + hs).setColor(r, g, b, a).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(1.0f, 0.0f, 0.0f);
            consumer.addVertex(mat, p.x, p.y + hs, p.z + hs).setColor(r, g, b, a).setUv(1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(1.0f, 0.0f, 0.0f);
            consumer.addVertex(mat, p.x, p.y + hs, p.z - hs).setColor(r, g, b, a).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(1.0f, 0.0f, 0.0f);
        }
    }

    public void renderEmitterMarker(PoseStack pose, MultiBufferSource buffers, float x, float y, float z, boolean selected) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.gui());
        Matrix4f mat = pose.last().pose();
        float r = 0.09f;
        int col = selected ? 0xFFFCD34D : 0xFFE879F9;

        consumer.addVertex(mat, x - r, y, z).setColor(col);
        consumer.addVertex(mat, x, y + r, z).setColor(col);
        consumer.addVertex(mat, x + r, y, z).setColor(col);
        consumer.addVertex(mat, x, y - r, z).setColor(col);

        consumer.addVertex(mat, x, y, z - r).setColor(col);
        consumer.addVertex(mat, x, y + r, z).setColor(col);
        consumer.addVertex(mat, x, y, z + r).setColor(col);
        consumer.addVertex(mat, x, y - r, z).setColor(col);
    }
}

