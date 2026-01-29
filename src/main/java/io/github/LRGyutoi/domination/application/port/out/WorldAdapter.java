package io.github.LRGyutoi.domination.application.port.out;

import io.github.LRGyutoi.domination.domain.model.player.PlayerId;

/**
 * ワールド操作用の出力ポート
 * インフラ層でBukkit APIを使用して実装
 */
public interface WorldAdapter {

    /**
     * 座標間の距離を計算
     */
    double getDistance(double x1, double y1, double z1, double x2, double y2, double z2);

    /**
     * プレイヤーの現在位置を取得
     */
    Position getPlayerPosition(PlayerId playerId);

    /**
     * プレイヤーの向きを取得
     */
    Direction getPlayerDirection(PlayerId playerId);

    /**
     * プレイヤーがしゃがんでいるか
     */
    boolean isPlayerSneaking(PlayerId playerId);

    /**
     * プレイヤーがオンラインか
     */
    boolean isPlayerOnline(PlayerId playerId);

    /**
     * 座標データ
     */
    record Position(double x, double y, double z, String worldName) {}

    /**
     * 方向データ
     */
    record Direction(float yaw, float pitch) {}
}
