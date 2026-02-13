package io.github.LRGyutoi.domination.application.usecase;

import io.github.LRGyutoi.domination.application.port.out.EffectAdapter;
import io.github.LRGyutoi.domination.application.port.out.MessageAdapter;
import io.github.LRGyutoi.domination.domain.model.player.GamePlayer;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import io.github.LRGyutoi.domination.domain.model.weapon.Weapon;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponId;
import io.github.LRGyutoi.domination.domain.repository.PlayerRepository;
import io.github.LRGyutoi.domination.domain.repository.WeaponRepository;

/**
 * 武器リロードユースケース
 */
public class ReloadWeaponUseCase {

    private final PlayerRepository playerRepository;
    private final WeaponRepository weaponRepository;
    private final EffectAdapter effectAdapter;
    private final MessageAdapter messageAdapter;

    public ReloadWeaponUseCase(
            PlayerRepository playerRepository,
            WeaponRepository weaponRepository,
            EffectAdapter effectAdapter,
            MessageAdapter messageAdapter
    ) {
        this.playerRepository = playerRepository;
        this.weaponRepository = weaponRepository;
        this.effectAdapter = effectAdapter;
        this.messageAdapter = messageAdapter;
    }

    /**
     * リロード開始を実行
     *
     * @return リロード結果（成功時はreloadTimeMsにリロード所要時間を含む）
     */
    public ReloadResult startReload(PlayerId playerId, WeaponId weaponId) {
        GamePlayer player = playerRepository.findById(playerId).orElse(null);
        if (player == null) {
            return ReloadResult.failure("プレイヤーが見つかりません");
        }

        if (!player.isInGame()) {
            return ReloadResult.failure("ゲームに参加していません");
        }

        if (!player.isAlive()) {
            return ReloadResult.failure("死亡中はリロードできません");
        }

        if (player.isReloading()) {
            return ReloadResult.failure("すでにリロード中です");
        }

        Weapon weapon = weaponRepository.findById(weaponId).orElse(null);
        if (weapon == null) {
            return ReloadResult.failure("武器が見つかりません");
        }

        // すでに満タンならリロード不要
        int currentAmmo = player.getAmmo(weaponId);
        if (currentAmmo >= weapon.getStats().magazineSize()) {
            return ReloadResult.failure("弾薬は満タンです");
        }

        // リロード状態を開始
        player.startReloading();
        playerRepository.save(player);

        effectAdapter.playReloadSound(playerId);
        messageAdapter.sendActionBar(playerId, "§eリロード中...");

        int reloadTimeMs = (int) weapon.getStats().reloadTimeMs();
        return ReloadResult.success(reloadTimeMs);
    }

    /**
     * リロード完了処理
     * タイマー経過後にPresentation層から呼び出される
     */
    public void completeReload(PlayerId playerId, WeaponId weaponId) {
        GamePlayer player = playerRepository.findById(playerId).orElse(null);
        if (player == null || !player.isReloading()) {
            return;
        }

        Weapon weapon = weaponRepository.findById(weaponId).orElse(null);
        if (weapon == null) {
            player.cancelReloading();
            playerRepository.save(player);
            return;
        }

        player.finishReloading(weapon.getStats().magazineSize());
        playerRepository.save(player);

        messageAdapter.sendActionBar(playerId,
            "§aリロード完了 §7| §f" + weapon.getStats().magazineSize() + "§7/" + weapon.getStats().magazineSize());
    }

    /**
     * リロード結果
     */
    public record ReloadResult(
        boolean success,
        String message,
        int reloadTimeMs
    ) {
        public static ReloadResult success(int reloadTimeMs) {
            return new ReloadResult(true, "リロード開始", reloadTimeMs);
        }

        public static ReloadResult failure(String message) {
            return new ReloadResult(false, message, 0);
        }
    }
}
