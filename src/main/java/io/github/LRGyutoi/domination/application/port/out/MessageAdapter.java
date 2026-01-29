package io.github.LRGyutoi.domination.application.port.out;

import io.github.LRGyutoi.domination.domain.model.player.PlayerId;

/**
 * プレイヤーへのメッセージ送信用出力ポート
 */
public interface MessageAdapter {

    /**
     * プレイヤーにメッセージを送信
     */
    void sendMessage(PlayerId playerId, String message);

    /**
     * プレイヤーにエラーメッセージを送信
     */
    void sendError(PlayerId playerId, String message);

    /**
     * プレイヤーに成功メッセージを送信
     */
    void sendSuccess(PlayerId playerId, String message);

    /**
     * 全プレイヤーにブロードキャスト
     */
    void broadcast(String message);

    /**
     * アクションバーに表示
     */
    void sendActionBar(PlayerId playerId, String message);

    /**
     * タイトルを表示
     */
    void sendTitle(PlayerId playerId, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    /**
     * キルフィードを表示
     */
    void showKillFeed(PlayerId killerId, PlayerId victimId, String weaponName, boolean isHeadshot);
}
