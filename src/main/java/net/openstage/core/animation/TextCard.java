package net.openstage.core.animation;

import java.util.UUID;

public class TextCard {
    public enum PositionPreset {
        TOP_CENTER,
        CENTER,
        BOTTOM_CENTER,
        LOWER_THIRD_LEFT,
        LOWER_THIRD_RIGHT,
        CUSTOM
    }

    public enum AnimationEffect {
        NONE,
        FADE,
        SLIDE_UP,
        TYPEWRITER,
        FLIP_Y,
        FLIP_X,
        ZOOM_3D,
        SWING,
        GLITCH_DECODE,
        POP_BOUNCE,
        NEON_PULSE,
        SLIDE_SIDE
    }

    private final String id;
    private float startTime;
    private float endTime;
    private String text;
    private PositionPreset position;
    private float offsetX;
    private float offsetY;
    private float offsetZ;
    private float rotX;
    private float rotY;
    private float rotZ;
    private float scaleX;
    private float scaleY;
    private float fontSize;
    private int textColor;
    private int backgroundColor;
    private float fadeIn;
    private float fadeOut;
    private AnimationEffect effect;

    public TextCard(String id, float startTime, float endTime, String text) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.startTime = startTime;
        this.endTime = endTime;
        this.text = text != null ? text : "";
        this.position = PositionPreset.BOTTOM_CENTER;
        this.offsetX = 0.0f;
        this.offsetY = -24.0f;
        this.offsetZ = 0.0f;
        this.rotX = 0.0f;
        this.rotY = 0.0f;
        this.rotZ = 0.0f;
        this.scaleX = 1.0f;
        this.scaleY = 1.0f;
        this.fontSize = 1.0f;
        this.textColor = 0xFFFFFFFF;
        this.backgroundColor = 0x88000000;
        this.fadeIn = 0.3f;
        this.fadeOut = 0.3f;
        this.effect = AnimationEffect.SLIDE_UP;
    }

    public TextCard(float startTime, float endTime, String text) {
        this(UUID.randomUUID().toString(), startTime, endTime, text);
    }

    public String getId() {
        return id;
    }

    public float getStartTime() {
        return startTime;
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
    }

    public float getEndTime() {
        return endTime;
    }

    public void setEndTime(float endTime) {
        this.endTime = endTime;
    }

    public float getDuration() {
        return Math.max(0.0f, endTime - startTime);
    }

    public void setDuration(float duration) {
        this.endTime = this.startTime + Math.max(0.0f, duration);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public PositionPreset getPosition() {
        return position;
    }

    public void setPosition(PositionPreset position) {
        this.position = position;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public float getOffsetZ() {
        return offsetZ;
    }

    public void setOffsetZ(float offsetZ) {
        this.offsetZ = offsetZ;
    }

    public float getRotX() {
        return rotX;
    }

    public void setRotX(float rotX) {
        this.rotX = rotX;
    }

    public float getRotY() {
        return rotY;
    }

    public void setRotY(float rotY) {
        this.rotY = rotY;
    }

    public float getRotZ() {
        return rotZ;
    }

    public void setRotZ(float rotZ) {
        this.rotZ = rotZ;
    }

    public float getScaleX() {
        return scaleX;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = Math.max(0.01f, scaleX);
    }

    public float getScaleY() {
        return scaleY;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = Math.max(0.01f, scaleY);
    }

    public float getFontSize() {
        return fontSize;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = Math.max(0.5f, Math.min(4.0f, fontSize));
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public float getFadeIn() {
        return fadeIn;
    }

    public void setFadeIn(float fadeIn) {
        this.fadeIn = Math.max(0.0f, fadeIn);
    }

    public float getFadeOut() {
        return fadeOut;
    }

    public void setFadeOut(float fadeOut) {
        this.fadeOut = Math.max(0.0f, fadeOut);
    }

    public AnimationEffect getEffect() {
        return effect;
    }

    public void setEffect(AnimationEffect effect) {
        this.effect = effect != null ? effect : AnimationEffect.NONE;
    }

    public boolean isActiveAt(float time) {
        return time >= startTime && time <= endTime;
    }
}

