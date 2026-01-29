package io.github.LRGyutoi.domination.application.usecase;

import io.github.LRGyutoi.domination.application.port.out.EffectAdapter;
import io.github.LRGyutoi.domination.application.port.out.MessageAdapter;
import io.github.LRGyutoi.domination.application.port.out.ProjectileAdapter;
import io.github.LRGyutoi.domination.domain.model.player.GamePlayer;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import io.github.LRGyutoi.domination.domain.model.weapon.Weapon;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponId;
import io.github.LRGyutoi.domination.domain.repository.PlayerRepository;
import io.github.LRGyutoi.domination.domain.repository.WeaponRepository;

/**
 * 武器発射ユースケース
 * プレイヤーが武器を発射する処理を統括
 */
public class FireWeaponUseCase {

    private final PlayerRepository playerRepository;
    private final WeaponRepository weaponRepository;
    private final ProjectileAdapter projectileAdapter;
    private final EffectAdapter effectAdapter;
    private final MessageAdapter messageAdapter;

    public FireWeaponUseCase(
            PlayerRepository playerRepository,
            WeaponRepository weaponRepository,
            ProjectileAdapter projectileAdapter,
            EffectAdapter effectAdapter,
            MessageAdapter messageAdapter
    ) {
        this.playerRepository = playerRepository;
        this.weaponRepository = weaponRepository;
        this.projectileAdapter = projectileAdapter;
        this.effectAdapter = effectAdapter;
        this.messageAdapter = messageAdapter;
    }

    /**
     * 武器を発射
     *
     * @param playerId 発射するプレイヤーのID
     * @param weaponId 使用する武器のID
     * @return 発射結果
     */
    public FireResult execute(PlayerId playerId, WeaponId weaponId) {
        // プレイヤー取得
        GamePlayer player = playerRepository.findById(playerId)
            .orElse(null);

        if (player == null) {
            return FireResult.failure("プレイヤーが見つかりません");
        }

        // ゲーム中でなければ発射不可
        if (!player.isInGame()) {
            return FireResult.failure("ゲームに参加していません");
        }

        // 武器取得
        Weapon weapon = weaponRepository.findById(weaponId)
            .orElse(null);

        if (weapon == null) {
            return FireResult.failure("武器が見つかりません");
        }

        // 残弾チェック（プレイヤーの弾薬管理はここで行う予定）
        // TODO: 弾薬管理をGamePlayerに追加

        // 弾丸を生成
        projectileAdapter.spawnProjectile(playerId, weaponId, weapon.getStats());

        // エフェクト再生
        effectAdapter.playMuzzleFlash(playerId);

        return FireResult.success();
    }

    /**
     * 発射結果
     */
    public record FireResult(
        boolean isSuccess,
        String errorMessage
    ) {
        public static FireResult success() {
            return new FireResult(true, null);
        }

        public static FireResult failure(String message) {
            return new FireResult(false, message);
        }
    }
}
