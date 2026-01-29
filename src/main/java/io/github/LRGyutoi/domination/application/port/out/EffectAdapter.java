package io.github.LRGyutoi.domination.application.port.out;

import io.github.LRGyutoi.domination.domain.model.player.PlayerId;

/**
 * エフェクト（パーティクル、サウンド）用出力ポート
 */
public interface EffectAdapter {

    /**
     * ヒットエフェクトを再生
     */
    void playHitEffect(double x, double y, double z, boolean isHeadshot);

    /**
     * 発砲エフェクトを再生
     */
    void playMuzzleFlash(PlayerId playerId);

    /**
     * リロードサウンドを再生
     */
    void playReloadSound(PlayerId playerId);

    /**
     * キルサウンドを再生（キラーに）
     */
    void playKillSound(PlayerId playerId);

    /**
     * ダメージサウンドを再生（被弾者に）
     */
    void playDamageSound(PlayerId playerId);

    /**
     * 空撃ちサウンドを再生
     */
    void playEmptySound(PlayerId playerId);
}
