package io.github.LRGyutoi.domination.domain.model.team;

import java.util.Objects;

/**
 * チームIDを表す値オブジェクト
 */
public record TeamId(String value) {

    public static final TeamId RED = new TeamId("red");
    public static final TeamId BLUE = new TeamId("blue");
    public static final TeamId NEUTRAL = new TeamId("neutral");

    public TeamId {
        Objects.requireNonNull(value, "TeamId cannot be null");
    }

    public boolean isNeutral() {
        return this.equals(NEUTRAL);
    }

    public static TeamId of(String value) {
        return new TeamId(value.toLowerCase());
    }

    public static TeamId valueOf(String value) {
        String lower = value.toLowerCase();
        return switch (lower) {
            case "red" -> RED;
            case "blue" -> BLUE;
            case "neutral" -> NEUTRAL;
            default -> new TeamId(lower);
        };
    }

    public String name() {
        return value.toUpperCase();
    }
}
