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

        playerRepository.findById(playerId).ifPresent(gamePlayer -> {
            if (gamePlayer.isInGame()) {
                event.deathMessage(null);

                // リスポーン開始（遅延秒数を取得）
                int delaySec = respawnPlayerUseCase.startRespawn(playerId);
                if (delaySec > 0) {
                    scheduleRespawnWithCountdown(playerId, delaySec);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerId playerId = PlayerId.of(player.getUniqueId().toString());

        playerRepository.findById(playerId).ifPresent(gamePlayer -> {
            if (gamePlayer.isInGame()) {
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
     * カウントダウン付きリスポーンをスケジュール
     * 毎秒カウントダウンを表示し、完了後にリスポーンを実行
     */
    private void scheduleRespawnWithCountdown(PlayerId playerId, int totalSeconds) {
        new BukkitRunnable() {
            int remaining = totalSeconds;

            @Override
            public void run() {
                if (remaining > 0) {
                    respawnPlayerUseCase.showCountdown(playerId, remaining);
                    remaining--;
                } else {
                    respawnPlayerUseCase.completeRespawn(playerId);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 20ticks = 1秒間隔
    }
}
