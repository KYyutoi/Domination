package io.github.LRGyutoi.domination_test;

import org.bukkit.ChatColor;

public enum Team {
    RED("赤", ChatColor.RED, org.bukkit.Color.RED),
    BLUE("青", ChatColor.BLUE, org.bukkit.Color.BLUE),
    NEUTRAL("中立", ChatColor.GRAY, org.bukkit.Color.WHITE);

    private final String displayName;
    private final ChatColor color;
    private final org.bukkit.Color beaconColor;

    Team(String displayName, ChatColor color, org.bukkit.Color beaconColor) {
        this.displayName = displayName;
        this.color = color;
        this.beaconColor = beaconColor;
    }

    public String getDisplayName() {
        return color + displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    public org.bukkit.Color getBeaconColor() {
        return beaconColor;
    }

    public Team getOpposite() {
        return switch (this) {
            case RED -> BLUE;
            case BLUE -> RED;
            case NEUTRAL -> NEUTRAL;
        };
    }
}

