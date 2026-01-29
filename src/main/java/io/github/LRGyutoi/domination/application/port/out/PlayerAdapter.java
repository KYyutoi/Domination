package io.github.LRGyutoi.domination.application.port.out;

import io.github.LRGyutoi.domination.domain.model.player.PlayerId;

/**
 * プレイヤー操作用出力ポート
 * HP設定、リスポーンなど
 */
public interface PlayerAdapter {

    /**
     * プレイヤーにダメージを与える
     */
    void dealDamage(PlayerId playerId, double damage, PlayerId attackerId);

    /**
     * プレイヤーの現在HPを取得
     */
    double getHealth(PlayerId playerId);

    /**
     * プレイヤーのHPを設定
     */
    void setHealth(PlayerId playerId, double health);

    /**
     * プレイヤーをリスポーン
     */
    void respawn(PlayerId playerId);

    /**
     * プレイヤーをリスポーン地点にテレポート
     */
    void teleportToSpawn(PlayerId playerId, String teamId);

    /**
     * プレイヤーの動きを制限（リスポーン待ち中）
     */
    void freeze(PlayerId playerId);

    /**
     * プレイヤーの動きを解除
     */
    void unfreeze(PlayerId playerId);
}
