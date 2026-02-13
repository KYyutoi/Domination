package io.github.LRGyutoi.domination.application.usecase;

import io.github.LRGyutoi.domination.application.port.out.EffectAdapter;
import io.github.LRGyutoi.domination.application.port.out.MessageAdapter;
import io.github.LRGyutoi.domination.application.port.out.PlayerAdapter;
import io.github.LRGyutoi.domination.application.port.out.WorldAdapter;
import io.github.LRGyutoi.domination.domain.model.combat.DamageResult;
import io.github.LRGyutoi.domination.domain.model.combat.HitPart;
import io.github.LRGyutoi.domination.domain.model.player.GamePlayer;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import io.github.LRGyutoi.domination.domain.model.weapon.Weapon;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponId;
import io.github.LRGyutoi.domination.domain.repository.PlayerRepository;
import io.github.LRGyutoi.domination.domain.repository.WeaponRepository;
import io.github.LRGyutoi.domination.domain.service.DamageCalculationService;
import io.github.LRGyutoi.domination.domain.service.HeadshotDetectionService;

/**
 * ヒット処理ユースケース
 * 弾丸がプレイヤーに命中した時の処理を統括
 */
public class ProcessHitUseCase {

    private final PlayerRepository playerRepository;
    private final WeaponRepository weaponRepository;
    private final DamageCalculationService damageService;
    private final HeadshotDetectionService headshotService;
    private final WorldAdapter worldAdapter;
    private final PlayerAdapter playerAdapter;
    private final EffectAdapter effectAdapter;
    private final MessageAdapter messageAdapter;

    public ProcessHitUseCase(
            PlayerRepository playerRepository,
            WeaponRepository weaponRepository,
            DamageCalculationService damageService,
            HeadshotDetectionService headshotService,
            WorldAdapter worldAdapter,
            PlayerAdapter playerAdapter,
            EffectAdapter effectAdapter,
            MessageAdapter messageAdapter
    ) {
        this.playerRepository = playerRepository;
        this.weaponRepository = weaponRepository;
        this.damageService = damageService;
        this.headshotService = headshotService;
        this.worldAdapter = worldAdapter;
        this.playerAdapter = playerAdapter;
        this.effectAdapter = effectAdapter;
        this.messageAdapter = messageAdapter;
    }

    /**
     * ヒット処理を実行
     */
    public HitResult execute(HitCommand command) {
        GamePlayer attacker = playerRepository.findById(command.attackerId()).orElse(null);
        GamePlayer victim = playerRepository.findById(command.victimId()).orElse(null);

        if (attacker == null || victim == null) {
            return HitResult.ignored("プレイヤーが見つかりません");
        }

        // 死亡中のプレイヤーへの攻撃は無視
        if (!victim.isAlive()) {
            return HitResult.ignored("対象は既に死亡しています");
        }

        // 同チームへのフレンドリーファイアは無効
        if (attacker.isSameTeam(victim)) {
            return HitResult.ignored("味方へのダメージは無効");
        }

        if (!attacker.isInGame() || !victim.isInGame()) {
            return HitResult.ignored("ゲームに参加していないプレイヤー");
        }

        Weapon weapon = weaponRepository.findById(command.weaponId()).orElse(null);
        if (weapon == null) {
            return HitResult.ignored("武器が見つかりません");
        }

        // 被弾部位を判定
        WorldAdapter.Position victimPos = worldAdapter.getPlayerPosition(command.victimId());
        boolean isSneaking = worldAdapter.isPlayerSneaking(command.victimId());

        HitPart hitPart = headshotService.detectHitPart(
            command.hitY(), command.hitX(), command.hitZ(),
            victimPos.y(), victimPos.x(), victimPos.z(),
            isSneaking
        );

        // ダメージ計算
        double currentHealth = playerAdapter.getHealth(command.victimId());
        DamageResult damageResult = damageService.calculate(
            weapon.getStats(),
            hitPart,
            command.distance(),
            currentHealth
        );

        // ダメージを適用
        playerAdapter.dealDamage(command.victimId(), damageResult.finalDamage(), command.attackerId());

        // エフェクト再生（被弾者のワールドで表示）
        effectAdapter.playHitEffect(victimPos.worldName(), command.hitX(), command.hitY(), command.hitZ(), hitPart.isHeadshot());
        effectAdapter.playDamageSound(command.victimId());

        // キル判定
        if (damageResult.isLethal()) {
            handleKill(attacker, victim, weapon, hitPart.isHeadshot());
        }

        return HitResult.success(damageResult, hitPart);
    }

    /**
     * キル処理
     */
    private void handleKill(GamePlayer attacker, GamePlayer victim, Weapon weapon, boolean isHeadshot) {
        // ドメインモデルの死亡状態を更新（die()内でrecordDeathが呼ばれる）
        victim.die();

        // 攻撃者の統計を更新
        attacker.getStats().recordKill(isHeadshot);

        playerRepository.save(attacker);
        playerRepository.save(victim);

        effectAdapter.playKillSound(attacker.getId());
        messageAdapter.showKillFeed(attacker.getId(), victim.getId(), weapon.getDisplayName(), isHeadshot);
    }

    /**
     * ヒットコマンド（入力データ）
     */
    public record HitCommand(
        PlayerId attackerId,
        PlayerId victimId,
        WeaponId weaponId,
        double hitX,
        double hitY,
        double hitZ,
        double distance
    ) {}

    /**
     * ヒット結果
     */
    public record HitResult(
        boolean processed,
        String message,
        DamageResult damageResult,
        HitPart hitPart
    ) {
        public static HitResult success(DamageResult damage, HitPart hitPart) {
            return new HitResult(true, "ヒット処理完了", damage, hitPart);
        }

        public static HitResult ignored(String reason) {
            return new HitResult(false, reason, null, null);
        }
    }
}
