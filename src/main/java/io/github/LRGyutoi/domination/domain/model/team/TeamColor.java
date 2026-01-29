package io.github.LRGyutoi.domination.domain.model.team;

/**
 * チームカラーを表す値オブジェクト
 * RGB値を保持し、表示に使用
 */
public record TeamColor(int red, int green, int blue) {

    public static final TeamColor RED = new TeamColor(255, 0, 0);
    public static final TeamColor BLUE = new TeamColor(0, 0, 255);
    public static final TeamColor NEUTRAL = new TeamColor(128, 128, 128);

    /**
     * ファクトリメソッド
     */
    public static TeamColor of(int red, int green, int blue) {
        return new TeamColor(red, green, blue);
    }

    /**
     * 16進数カラーコードを取得
     */
    public String toHex() {
        return String.format("#%02X%02X%02X", red, green, blue);
    }
}
