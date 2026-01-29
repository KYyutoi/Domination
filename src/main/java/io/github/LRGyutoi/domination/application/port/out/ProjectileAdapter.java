package io.github.LRGyutoi.domination.application.port.out;

import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponId;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponStats;

import java.util.UUID;

/**
 * 弾丸（投射物）管理用の出力ポート
 */
public interface ProjectileAdapter {

    /**
     * 弾丸を発射
     *
     * @param shooterId     発射者のID
     * @param weaponId      使用武器のID
     * @param weaponStats   使用武器のステータス
     * @return 生成された弾丸のID
     */
    UUID spawnProjectile(PlayerId shooterId, WeaponId weaponId, WeaponStats weaponStats);

    /**
     * 弾丸を登録
     */
    void registerProjectile(UUID projectileId, ProjectileData data);

    /**
     * 弾丸データを取得
     */
    ProjectileData getProjectileData(UUID projectileId);

    /**
     * 弾丸を削除
     */
    void removeProjectile(UUID projectileId);

    /**
     * 弾丸データ
     */
    record ProjectileData(
        PlayerId shooterId,
        String weaponId,
        double damage,
        long spawnTime
    ) {}
}
