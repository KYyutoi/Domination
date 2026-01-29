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
     */
    public void startRespawn(PlayerId playerId) {
        GamePlayer player = playerRepository.findById(playerId).orElse(null);
        if (player == null || !player.isInGame()) {
            return;
        }

        // 動きを制限
        playerAdapter.freeze(playerId);

        // チームのチケットを減らす
        Team team = teamRepository.findById(player.getTeamId()).orElse(null);
        if (team != null) {
            team.decrementTickets();
            teamRepository.save(team);

            // チケット残数を通知
            messageAdapter.sendActionBar(playerId, 
                "§c残りチケット: §f" + team.getTickets());
        }

        // リスポーンカウントダウンを表示
        showRespawnCountdown(playerId, DEFAULT_RESPAWN_DELAY_SECONDS);
    }

    /**
     * リスポーンカウントダウンを表示
     */
    private void showRespawnCountdown(PlayerId playerId, int seconds) {
        for (int i = seconds; i > 0; i--) {
            final int remaining = i;
            // NOTE: 実際のタイマー処理はインフラ層で実装
            messageAdapter.sendTitle(playerId, 
                "§c" + remaining, 
                "§7リスポーンまで", 
                0, 20, 0);
        }
    }

    /**
     * リスポーンを完了
     */
    public void completeRespawn(PlayerId playerId) {
        GamePlayer player = playerRepository.findById(playerId).orElse(null);
        if (player == null || !player.isInGame()) {
            return;
        }

        // スポーン地点にテレポート
        playerAdapter.teleportToSpawn(playerId, player.getTeamId().name());

        // HP回復
        playerAdapter.setHealth(playerId, 20.0);

        // 動き制限解除
        playerAdapter.unfreeze(playerId);

        // 通知
        messageAdapter.sendTitle(playerId, 
            "§aリスポーン完了", 
            "", 
            5, 20, 5);
    }
}
