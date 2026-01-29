package io.github.LRGyutoi.domination.presentation.listener;

import io.github.LRGyutoi.domination.application.usecase.RespawnPlayerUseCase;
import io.github.LRGyutoi.domination.domain.model.player.GamePlayer;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import io.github.LRGyutoi.domination.domain.model.team.TeamId;
import io.github.LRGyutoi.domination.domain.repository.PlayerRepository;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * プレイヤー状態変化リスナー
 */
public class PlayerStateListener implements Listener {

    private final PlayerRepository playerRepository;
    private final RespawnPlayerUseCase respawnPlayerUseCase;
    private final Plugin plugin;

    public PlayerStateListener(
            PlayerRepository playerRepository,
            RespawnPlayerUseCase respawnPlayerUseCase,
            Plugin plugin
    ) {
        this.playerRepository = playerRepository;
        this.respawnPlayerUseCase = respawnPlayerUseCase;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerId playerId = PlayerId.of(player.getUniqueId().toString());

        // 既存プレイヤーかチェック
        if (!playerRepository.exists(playerId)) {
            // 新規プレイヤーとして登録（ニュートラル状態）
            GamePlayer gamePlayer = GamePlayer.create(playerId, player.getName());
            playerRepository.save(gamePlayer);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerId playerId = PlayerId.of(player.getUniqueId().toString());

        // ゲーム中だった場合、離脱処理
        playerRepository.findById(playerId).ifPresent(gamePlayer -> {
            if (gamePlayer.isInGame()) {
                gamePlayer.leaveGame();
                playerRepository.save(gamePlayer);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerId playerId = PlayerId.of(player.getUniqueId().toString());

        // ゲーム中のプレイヤーのみ処理
        playerRepository.findById(playerId).ifPresent(gamePlayer -> {
            if (gamePlayer.isInGame()) {
                // デスメッセージをカスタマイズ
                event.deathMessage(null);

                // リスポーン処理を開始
                respawnPlayerUseCase.startRespawn(playerId);

                // 5秒後に自動リスポーン
                scheduleRespawn(playerId, 100); // 100 ticks = 5秒
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerId playerId = PlayerId.of(player.getUniqueId().toString());

        playerRepository.findById(playerId).ifPresent(gamePlayer -> {
            if (gamePlayer.isInGame()) {
                // リスポーン完了処理を1tick後に実行
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        respawnPlayerUseCase.completeRespawn(playerId);
                    }
                }.runTaskLater(plugin, 1L);
            }
        });
    }

    /**
     * 自動リスポーンをスケジュール
     */
    private void scheduleRespawn(PlayerId playerId, long delayTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                respawnPlayerUseCase.completeRespawn(playerId);
            }
        }.runTaskLater(plugin, delayTicks);
    }
}
