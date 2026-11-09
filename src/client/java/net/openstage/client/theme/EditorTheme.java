package net.openstage.client.theme;

public enum EditorTheme {
    DARK_SLATE(
            "Dark Slate",
            0xFF0D1424, 0xFF111827, 0xFF0B0F19, 0xFF131C2E,
            0xFF1E293B, 0xFF3B82F6, 0xFF2563EB, 0xFF1E293B, 0xFF334155, 0xFFFFFFFF, 0xFF94A3B8
    ),
    MIDNIGHT_BLUE(
            "Midnight Blue",
            0xFF09122C, 0xFF0F1A3A, 0xFF060D20, 0xFF13224B,
            0xFF1E3A8A, 0xFF60A5FA, 0xFF3B82F6, 0xFF1B2A52, 0xFF253B73, 0xFFE0E7FF, 0xFF93C5FD
    ),
    CYBER_CHARCOAL(
            "Cyber Charcoal",
            0xFF121212, 0xFF181818, 0xFF0A0A0A, 0xFF1E1E1E,
            0xFF2D2D2D, 0xFF00FFCC, 0xFF00E5FF, 0xFF262626, 0xFF363636, 0xFFEEEEEE, 0xFFAAAAAA
    ),
    EMERALD_FOREST(
            "Emerald Forest",
            0xFF062117, 0xFF0A2E20, 0xFF041710, 0xFF0F3D2C,
            0xFF1A533C, 0xFF10B981, 0xFF059669, 0xFF144733, 0xFF1C6347, 0xFFD1FAE5, 0xFF6EE7B7
    ),
    SUNSET_AMBER(
            "Sunset Amber",
            0xFF261206, 0xFF361B0A, 0xFF1A0A03, 0xFF45230D,
            0xFF613315, 0xFFF59E0B, 0xFFD97706, 0xFF4A250E, 0xFF633213, 0xFFFEF3C7, 0xFFFCD34D
    ),
    SYNTHWAVE_PURPLE(
            "Synthwave",
            0xFF1F0C2E, 0xFF2C1340, 0xFF150620, 0xFF3B1854,
            0xFF552277, 0xFFE040FB, 0xFFC2185B, 0xFF3F195C, 0xFF582480, 0xFFF3E5F5, 0xFFCE93D8
    ),
    MONOKAI(
            "Monokai Pro",
            0xFF221F22, 0xFF2D2A2E, 0xFF19181A, 0xFF353236,
            0xFF403E41, 0xFFFFD866, 0xFFFF6188, 0xFF403E41, 0xFF555356, 0xFFFCFCFA, 0xFF939293
    ),
    LIGHT_STUDIO(
            "Light Studio",
            0xFFE2E8F0, 0xFFF1F5F9, 0xFFF8FAFC, 0xFFE2E8F0,
            0xFFCBD5E1, 0xFF2563EB, 0xFF3B82F6, 0xFFE2E8F0, 0xFFCBD5E1, 0xFF0F172A, 0xFF475569
    );
    private final String displayName;
    private final int titleBarBg;
    private final int panelBg;
    private final int viewportBg;
    private final int timelineBg;
    private final int divider;
    private final int dividerHover;
    private final int accent;
    private final int buttonBg;
    private final int buttonHover;
    private final int textColor;
    private final int textMuted;

    EditorTheme(String displayName, int titleBarBg, int panelBg, int viewportBg, int timelineBg,
                int divider, int dividerHover, int accent, int buttonBg, int buttonHover,
                int textColor, int textMuted) {
        this.displayName = displayName;
        this.titleBarBg = titleBarBg;
        this.panelBg = panelBg;
        this.viewportBg = viewportBg;
        this.timelineBg = timelineBg;
        this.divider = divider;
        this.dividerHover = dividerHover;
        this.accent = accent;
        this.buttonBg = buttonBg;
        this.buttonHover = buttonHover;
        this.textColor = textColor;
        this.textMuted = textMuted;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTitleBarBg() {
        return titleBarBg;
    }

    public int getPanelBg() {
        return panelBg;
    }

    public int getViewportBg() {
        return viewportBg;
    }

    public int getTimelineBg() {
        return timelineBg;
    }

    public int getDivider() {
        return divider;
    }

    public int getDividerHover() {
        return dividerHover;
    }

    public int getAccent() {
        return accent;
    }

    public int getButtonBg() {
        return buttonBg;
    }

    public int getButtonHover() {
        return buttonHover;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getTextMuted() {
        return textMuted;
    }
}

