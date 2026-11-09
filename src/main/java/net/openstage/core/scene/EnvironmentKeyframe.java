package net.openstage.core.scene;

import net.minecraft.core.BlockPos;

public record EnvironmentKeyframe(
        float time,
        EnvironmentMode mode,
        int anchorX,
        int anchorY,
        int anchorZ,
        EnvironmentTransition transition,
        float fadeDuration
) {
    public enum EnvironmentMode {
        WORLD("Мир Minecraft", "World"),
        DARK_VOID("Темная пустота", "Dark Void"),
        CHROMA_GREEN("Зеленый хромакей", "Chroma Green"),
        CHROMA_BLUE("Синий хромакей", "Chroma Blue"),
        SUNSET_SKY("Закат", "Sunset Sky"),
        DAYLIGHT("Дневной свет", "Daylight");

        private final String ruName;
        private final String enName;

        EnvironmentMode(String ruName, String enName) {
            this.ruName = ruName;
            this.enName = enName;
        }

        public String getDisplayName(boolean ru) {
            return ru ? ruName : enName;
        }
    }

    public enum EnvironmentTransition {
        CUT("Срез (Cut)", "Cut"),
        FADE("Затухание (Fade)", "Fade");

        private final String ruName;
        private final String enName;

        EnvironmentTransition(String ruName, String enName) {
            this.ruName = ruName;
            this.enName = enName;
        }

        public String getDisplayName(boolean ru) {
            return ru ? ruName : enName;
        }
    }

    public EnvironmentKeyframe(float time, EnvironmentMode mode, BlockPos anchor, EnvironmentTransition transition, float fadeDuration) {
        this(time, mode,
                anchor != null ? anchor.getX() : 0,
                anchor != null ? anchor.getY() : 64,
                anchor != null ? anchor.getZ() : 0,
                transition, fadeDuration);
    }

    public BlockPos getAnchorPos() {
        return new BlockPos(anchorX, anchorY, anchorZ);
    }
}

