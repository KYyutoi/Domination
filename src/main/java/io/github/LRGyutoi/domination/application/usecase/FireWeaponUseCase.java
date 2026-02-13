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
        GamePlayer player = playerRepository.findById(playerId).orElse(null);
        if (player == null) {
            return FireResult.failure("プレイヤーが見つかりません");
        }

        if (!player.isInGame()) {
            return FireResult.failure("ゲームに参加していません");
        }

        Weapon weapon = weaponRepository.findById(weaponId).orElse(null);
        if (weapon == null) {
            return FireResult.failure("武器が見つかりません");
        }

        // 発射可能かチェック（リロード中・死亡中・クールダウンを含む）
        if (!player.canFire(weapon.getFireIntervalMillis())) {
            if (player.isReloading()) {
                return FireResult.failure("リロード中");
            }
            return FireResult.failure("発射できません");
        }

        // 残弾チェック
        if (!player.hasAmmo(weaponId)) {
            effectAdapter.playEmptySound(playerId);
            messageAdapter.sendActionBar(playerId, "§c弾切れ - リロードしてください");
            return FireResult.failure("弾切れ");
        }

        // 弾薬を消費し、発射時刻を記録
        player.consumeAmmo();
        player.recordShot();

        // 散弾銃の場合は複数弾生成
        int pellets = weapon.getStats().pelletsPerShot();
        for (int i = 0; i < pellets; i++) {
            projectileAdapter.spawnProjectile(playerId, weaponId, weapon.getStats());
        }

        effectAdapter.playMuzzleFlash(playerId);

        // 残弾表示
        int remainingAmmo = player.getAmmo(weaponId);
        messageAdapter.sendActionBar(playerId,
            "§f" + weapon.getDisplayName() + " §7| §f" + remainingAmmo + "§7/" + weapon.getStats().magazineSize());

        playerRepository.save(player);

        return FireResult.success(remainingAmmo);
    }

    /**
     * 発射結果
     */
    public record FireResult(
        boolean isSuccess,
        String errorMessage,
        int remainingAmmo
    ) {
        public static FireResult success(int remainingAmmo) {
            return new FireResult(true, null, remainingAmmo);
        }

        public static FireResult failure(String message) {
            return new FireResult(false, message, -1);
        }
    }
}
