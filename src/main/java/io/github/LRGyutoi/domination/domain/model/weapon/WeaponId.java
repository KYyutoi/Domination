package io.github.LRGyutoi.domination.domain.model.weapon;

import java.util.Objects;

/**
 * 武器の識別子を表す値オブジェクト
 * 型安全性を提供し、文字列の直接使用を防ぐ
 */
public record WeaponId(String value) {

    public WeaponId {
        Objects.requireNonNull(value, "WeaponId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("WeaponId cannot be blank");
        }
    }

    public static WeaponId of(String value) {
        return new WeaponId(value.toLowerCase());
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
