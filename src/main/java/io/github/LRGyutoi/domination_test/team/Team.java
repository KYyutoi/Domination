package io.github.LRGyutoi.domination_test.team;

import org.bukkit.ChatColor;

/**
 * チームの列挙型
 */
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
    
    /**
     * 文字列からチームを取得
     */
    public static Team fromString(String name) {
        if (name == null) return NEUTRAL;
        return switch (name.toLowerCase()) {
            case "red", "赤" -> RED;
            case "blue", "青" -> BLUE;
            default -> NEUTRAL;
        };
    }
}
