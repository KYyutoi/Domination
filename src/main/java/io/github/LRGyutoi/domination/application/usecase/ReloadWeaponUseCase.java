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
     * リロードを実行
     */
    public ReloadResult execute(PlayerId playerId, WeaponId weaponId) {
        // プレイヤー取得
        GamePlayer player = playerRepository.findById(playerId).orElse(null);
        if (player == null) {
            return ReloadResult.failure("プレイヤーが見つかりません");
        }

        if (!player.isInGame()) {
            return ReloadResult.failure("ゲームに参加していません");
        }

        // 武器取得
        Weapon weapon = weaponRepository.findById(weaponId).orElse(null);
        if (weapon == null) {
            return ReloadResult.failure("武器が見つかりません");
        }

        // リロード処理
        // TODO: プレイヤーごとの弾薬管理を実装
        int reloadTime = (int) weapon.getStats().reloadTimeMs();

        // エフェクト再生
        effectAdapter.playReloadSound(playerId);

        // アクションバーに表示
        messageAdapter.sendActionBar(playerId, "§eリロード中...");

        return ReloadResult.success(reloadTime);
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
