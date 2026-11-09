package net.openstage.client.ui;

public class MenuItem {
    public final String label;
    public final boolean enabled;
    public final Runnable action;

    public MenuItem(String label, boolean enabled, Runnable action) {
        this.label = label;
        this.enabled = enabled;
        this.action = action;
    }
}

