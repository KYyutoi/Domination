package io.github.LRGyutoi.domination.domain.model.combat;

/**
 * 被弾部位を表す値オブジェクト
 * ダメージ倍率はドメインルールとして定義
 */
public enum HitPart {
    HEAD("頭", 1.0),
    BODY("胴体", 1.0),
    LEGS("脚", 0.8);

    private final String displayName;
    private final double baseDamageMultiplier;

    HitPart(String displayName, double baseDamageMultiplier) {
        this.displayName = displayName;
        this.baseDamageMultiplier = baseDamageMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBaseDamageMultiplier() {
        return baseDamageMultiplier;
    }

    public boolean isHeadshot() {
        return this == HEAD;
    }
}
