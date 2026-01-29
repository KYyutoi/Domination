package io.github.LRGyutoi.domination.domain.model.player;

import java.util.UUID;

/**
 * プレイヤーIDを表す値オブジェクト
 * UUIDをラップして型安全性を提供
 */
public record PlayerId(UUID value) {

    public static PlayerId of(UUID uuid) {
        return new PlayerId(uuid);
    }

    public static PlayerId of(String uuidString) {
        return new PlayerId(UUID.fromString(uuidString));
    }

    public String getValue() {
        return value.toString();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
