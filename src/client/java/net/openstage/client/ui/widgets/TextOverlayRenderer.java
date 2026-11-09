package net.openstage.client.ui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.openstage.core.animation.TextCard;

public final class TextOverlayRenderer {
    private TextOverlayRenderer() {}

    public static void render(GuiGraphics graphics, TextCard card, float currentTime,
                              int vx, int vy, int vw, int vh) {
        if (card == null || !card.isActiveAt(currentTime) || vw <= 0 || vh <= 0) {
            return;
        }

        float progress = calculateProgress(card, currentTime);
        if (progress <= 0.0f) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        net.openstage.client.model.ModelSettings.Language lang = net.openstage.client.model.ModelSettings.Language.RU;
        if (mc.screen instanceof net.openstage.client.ui.OpenStageScreen oss && oss.getModelSettings() != null) {
            lang = oss.getModelSettings().getLanguage();
        }
        String rawText = net.openstage.client.ui.I18n.get(card.getText(), lang);
        String displayText = rawText;

        if (card.getEffect() == TextCard.AnimationEffect.TYPEWRITER) {
            int charCount = Math.max(0, Math.min(rawText.length(), (int) (progress * rawText.length())));
            displayText = rawText.substring(0, charCount);
        } else if (card.getEffect() == TextCard.AnimationEffect.GLITCH_DECODE) {
            int len = rawText.length();
            int decoded = Math.max(0, Math.min(len, (int) (progress * (len + 1))));
            char[] chars = rawText.toCharArray();
            String glyphs = "01#@$%&?<>*+^!█▓░";
            long seed = (long) (currentTime * 60.0f);
            for (int ci = decoded; ci < len; ci++) {
                if (!Character.isWhitespace(chars[ci])) {
                    int gIdx = (int) (Math.abs((ci * 31L + seed) % glyphs.length()));
                    chars[ci] = glyphs.charAt(gIdx);
                }
            }
            displayText = new String(chars);
        }

        int textWidth = font.width(displayText);
        int textHeight = font.lineHeight;
        int paddingH = 8;
        int paddingV = 4;
        int boxW = textWidth + paddingH * 2;
        int boxH = textHeight + paddingV * 2;
        int centerX = vx + vw / 2;
        int centerY = vy + vh / 2;

        int targetX = switch (card.getPosition()) {
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> centerX - boxW / 2;
            case LOWER_THIRD_LEFT -> vx + 24;
            case LOWER_THIRD_RIGHT -> vx + vw - boxW - 24;
            case CUSTOM -> vx + (int) card.getOffsetX();
        };

        int targetY = switch (card.getPosition()) {
            case TOP_CENTER -> vy + 24;
            case CENTER -> centerY - boxH / 2;
            case BOTTOM_CENTER, LOWER_THIRD_LEFT, LOWER_THIRD_RIGHT -> vy + vh - boxH - 28;
            case CUSTOM -> vy + (int) card.getOffsetY();
        };

        if (card.getPosition() != TextCard.PositionPreset.CUSTOM) {
            targetX += (int) card.getOffsetX();
            targetY += (int) card.getOffsetY();
        }

        if (card.getEffect() == TextCard.AnimationEffect.SLIDE_UP) {
            targetY += (int) ((1.0f - progress) * 12.0f);
        } else if (card.getEffect() == TextCard.AnimationEffect.SLIDE_SIDE) {
            targetX -= (int) ((1.0f - progress) * 45.0f);
        }

        int origBgAlpha = (card.getBackgroundColor() >> 24) & 0xFF;
        int currentBgAlpha = (int) (origBgAlpha * progress);
        int currentBgColor = (currentBgAlpha << 24) | (card.getBackgroundColor() & 0x00FFFFFF);
        int origTextAlpha = (card.getTextColor() >> 24) & 0xFF;
        int currentTextAlpha = (int) (origTextAlpha * progress);
        int currentTextColor = (currentTextAlpha << 24) | (card.getTextColor() & 0x00FFFFFF);
        com.mojang.blaze3d.vertex.PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        float pivotX = targetX + boxW / 2.0f;
        float pivotY = targetY + boxH / 2.0f;

        poseStack.translate(pivotX, pivotY, card.getOffsetZ());
        float animRotX = card.getRotX();
        float animRotY = card.getRotY();
        float animRotZ = card.getRotZ();
        float animScaleX = card.getScaleX() * card.getFontSize();
        float animScaleY = card.getScaleY() * card.getFontSize();

        if (card.getEffect() == TextCard.AnimationEffect.FLIP_Y) {
            float flipEntrance = (progress < 1.0f && currentTime < card.getStartTime() + card.getFadeIn())
                    ? (1.0f - progress) : 0.0f;
            animRotY += flipEntrance * 90.0f;
        } else if (card.getEffect() == TextCard.AnimationEffect.FLIP_X) {
            float flipEntrance = (progress < 1.0f && currentTime < card.getStartTime() + card.getFadeIn())
                    ? (1.0f - progress) : 0.0f;
            animRotX += flipEntrance * -90.0f;
        } else if (card.getEffect() == TextCard.AnimationEffect.ZOOM_3D) {
            animScaleX *= Math.max(0.01f, progress);
            animScaleY *= Math.max(0.01f, progress);
        } else if (card.getEffect() == TextCard.AnimationEffect.POP_BOUNCE) {
            float p = progress;
            float bounce = (p < 1.0f && currentTime < card.getStartTime() + card.getFadeIn())
                    ? (float) (Math.sin(p * Math.PI) * 0.35f * (1.0f - p)) : 0.0f;
            animScaleX *= Math.max(0.01f, p + bounce);
            animScaleY *= Math.max(0.01f, p + bounce);
        } else if (card.getEffect() == TextCard.AnimationEffect.NEON_PULSE) {
            float pulse = (float) (0.88f + 0.12f * Math.sin((currentTime - card.getStartTime()) * 8.0));
            animScaleX *= pulse;
            animScaleY *= pulse;
        } else if (card.getEffect() == TextCard.AnimationEffect.SWING) {
            float swingTime = currentTime - card.getStartTime();
            animRotZ += (float) (Math.sin(swingTime * 4.0) * 8.0 * (1.0f - progress * 0.5f));
        }

        if (animRotX != 0.0f) poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(animRotX));
        if (animRotY != 0.0f) poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(animRotY));
        if (animRotZ != 0.0f) poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(animRotZ));

        poseStack.scale(animScaleX, animScaleY, 1.0f);

        poseStack.translate(-boxW / 2.0f, -boxH / 2.0f, 0.0f);

        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        graphics.fill(0, 0, boxW, boxH, currentBgColor);
        int outlineAlpha = Math.min(255, currentBgAlpha + 30);
        int outlineColor = (outlineAlpha << 24) | 0x00FFFFFF;
        graphics.renderOutline(0, 0, boxW, boxH, outlineColor);

        graphics.drawString(
                font,
                displayText,
                paddingH,
                paddingV,
                currentTextColor,
                true
        );

        poseStack.pushPose();
        poseStack.translate(boxW, 0.0f, -0.05f);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f));
        graphics.fill(0, 0, boxW, boxH, currentBgColor);
        graphics.renderOutline(0, 0, boxW, boxH, outlineColor);
        graphics.drawString(font, displayText, paddingH, paddingV, currentTextColor, true);
        poseStack.popPose();

        RenderSystem.enableCull();

        poseStack.popPose();
    }

    private static float calculateProgress(TextCard card, float currentTime) {
        float start = card.getStartTime();
        float end = card.getEndTime();
        float fadeIn = card.getFadeIn();
        float fadeOut = card.getFadeOut();

        if (currentTime < start || currentTime > end) return 0.0f;

        if (fadeIn > 0.0f && currentTime < start + fadeIn) {
            return Math.max(0.0f, Math.min(1.0f, (currentTime - start) / fadeIn));
        }

        if (fadeOut > 0.0f && currentTime > end - fadeOut) {
            return Math.max(0.0f, Math.min(1.0f, (end - currentTime) / fadeOut));
        }

        return 1.0f;
    }
}

