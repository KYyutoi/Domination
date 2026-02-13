package io.github.LRGyutoi.domination.application.usecase;

import io.github.LRGyutoi.domination.application.port.out.MessageAdapter;
import io.github.LRGyutoi.domination.application.port.out.PlayerAdapter;
import io.github.LRGyutoi.domination.domain.model.player.GamePlayer;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import io.github.LRGyutoi.domination.domain.model.team.Team;
import io.github.LRGyutoi.domination.domain.repository.PlayerRepository;
import io.github.LRGyutoi.domination.domain.repository.TeamRepository;

/**
 * プレイヤーリスポーンユースケース
 */
public class RespawnPlayerUseCase {

    private static final int DEFAULT_RESPAWN_DELAY_SECONDS = 5;

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final PlayerAdapter playerAdapter;
    private final MessageAdapter messageAdapter;

    public RespawnPlayerUseCase(
            PlayerRepository playerRepository,
            TeamRepository teamRepository,
            PlayerAdapter playerAdapter,
            MessageAdapter messageAdapter
    ) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.playerAdapter = playerAdapter;
        this.messageAdapter = messageAdapter;
    }

    /**
     * リスポーン処理を開始
     *
     * @return リスポーン遅延秒数（Presentation層でタイマーをスケジュールするために使用）
     */
    public int startRespawn(PlayerId playerId) {
        GamePlayer player = playerRepository.findById(playerId).orElse(null);
        if (player == null || !player.isInGame()) {
            return 0;
        }

        // ドメインモデルに死亡状態を反映
        if (player.isAlive()) {
            player.die();
        }
        playerRepository.save(player);

        // 動きを制限
        playerAdapter.freeze(playerId);

        // チームのチケットを減らす
        Team team = teamRepository.findById(player.getTeamId()).orElse(null);
        if (team != null) {
            team.decrementTickets();
            teamRepository.save(team);

            messageAdapter.sendActionBar(playerId,
                "§c残りチケット: §f" + team.getTickets());
        }

        return DEFAULT_RESPAWN_DELAY_SECONDS;
    }

    /**
     * カウントダウンの1秒ごとの表示を送信
     * Presentation層のタイマーから毎秒呼び出される
     */
    public void showCountdown(PlayerId playerId, int remainingSeconds) {
        messageAdapter.sendTitle(playerId,
            "§c" + remainingSeconds,
            "§7リスポーンまで",
            0, 25, 0);
    }

    /**
     * リスポーンを完了
     */
    public void completeRespawn(PlayerId playerId) {
        GamePlayer player = playerRepository.findById(playerId).orElse(null);
        if (player == null || !player.isInGame()) {
            return;
        }

        // ドメインモデルの生存状態を復帰
        player.respawn();
        playerRepository.save(player);

        // スポーン地点にテレポート
        playerAdapter.teleportToSpawn(playerId, player.getTeamId().name());

        // HP回復
        playerAdapter.setHealth(playerId, 20.0);

        // 動き制限解除
        playerAdapter.unfreeze(playerId);

        messageAdapter.sendTitle(playerId,
            "§aリスポーン完了",
            "",
            5, 20, 5);
    }
}
