package io.github.LRGyutoi.domination_test;

import io.github.LRGyutoi.domination_test.combat.CombatValidator;
import io.github.LRGyutoi.domination_test.combat.DamageCalculator;
import io.github.LRGyutoi.domination_test.config.ConfigManager;
import io.github.LRGyutoi.domination_test.domination.DominationCommand;
import io.github.LRGyutoi.domination_test.domination.DominationManager;
import io.github.LRGyutoi.domination_test.gun.GunCommand;
import io.github.LRGyutoi.domination_test.gun.GunHandler;
import io.github.LRGyutoi.domination_test.gun.GunManager;
import io.github.LRGyutoi.domination_test.player.PlayerStateManager;
import io.github.LRGyutoi.domination_test.projectile.ProjectileManager;
import io.github.LRGyutoi.domination_test.team.TeamManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Domination_test メインプラグインクラス
 * 拠点制圧型TPSゲームプラグイン
 */
public final class Domination_test extends JavaPlugin implements Listener {

    // === コアシステム ===
    private ConfigManager configManager;
    private TeamManager teamManager;
    
    // === 銃システム ===
    private GunManager gunManager;
    private GunHandler gunHandler;
    
    // === 戦闘システム ===
    private DamageCalculator damageCalculator;
    private CombatValidator combatValidator;
    private ProjectileManager projectileManager;
    
    // === プレイヤーシステム ===
    private PlayerStateManager playerStateManager;
    
    // === ゲームシステム ===
    private DominationManager dominationManager;
    
    // === クリーンアップタスク ===
    private BukkitRunnable cleanupTask;

    @Override
    public void onEnable() {
        getLogger().info("Domination_test プラグインを有効化しています...");
        
        // === Phase 1: コア設定の初期化 ===
        configManager = new ConfigManager(this);
        teamManager = TeamManager.getInstance();
        teamManager.setInitialTickets(configManager.getInitialTickets());
        
        // === Phase 2: 銃システムの初期化 ===
        gunManager = new GunManager(this);
        
        // === Phase 3: 戦闘システムの初期化 ===
        damageCalculator = new DamageCalculator(configManager);
        combatValidator = new CombatValidator(configManager, teamManager);
        
        // === Phase 4: GunHandler初期化 ===
        gunHandler = new GunHandler(this, gunManager, configManager, combatValidator);
        
        // === Phase 5: プレイヤー状態管理の初期化 ===
        playerStateManager = new PlayerStateManager(this, configManager, teamManager, gunManager);
        gunHandler.setPlayerStateManager(playerStateManager);
        
        // === Phase 6: 弾丸システムの初期化 ===
        projectileManager = new ProjectileManager(this, configManager, damageCalculator, 
                                                   combatValidator, teamManager);
        gunHandler.setProjectileManager(projectileManager);
        
        // === Phase 7: ゲームシステムの初期化 ===
        dominationManager = new DominationManager(this, configManager);
        
        // === イベントリスナーの登録 ===
        getServer().getPluginManager().registerEvents(gunHandler, this);
        getServer().getPluginManager().registerEvents(playerStateManager, this);
        getServer().getPluginManager().registerEvents(this, this);
        
        // === コマンドの登録 ===
        registerCommands();
        
        // === 定期クリーンアップタスク ===
        startCleanupTask();
        
        // === 起動完了ログ ===
        getLogger().info("=================================");
        getLogger().info("Domination_test プラグイン v" + getDescription().getVersion());
        getLogger().info("銃システム: " + gunManager.getGunIds().size() + "種類の銃を読み込みました");
        getLogger().info("ヘッドショット倍率: x" + configManager.getHeadshotMultiplier());
        getLogger().info("フレンドリーファイア: " + (configManager.isFriendlyFireEnabled() ? "有効" : "無効"));
        getLogger().info("弾道パーティクル: " + (configManager.isBulletTrailEnabled() ? "有効" : "無効"));
        getLogger().info("=================================");
    }

    private void registerCommands() {
        GunCommand gunCommand = new GunCommand(gunManager, gunHandler);
        PluginCommand gunCmd = getCommand("gun");
        if (gunCmd != null) {
            gunCmd.setExecutor(gunCommand);
            gunCmd.setTabCompleter(gunCommand);
        }
        
        DominationCommand dominationCommand = new DominationCommand(this, dominationManager);
        PluginCommand domCmd = getCommand("domination");
        if (domCmd != null) {
            domCmd.setExecutor(dominationCommand);
            domCmd.setTabCompleter(dominationCommand);
        }
    }
    
    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                combatValidator.cleanup();
                
                if (configManager.isDebugMode()) {
                    getLogger().info("[Debug] クリーンアップ実行 - アクティブ弾丸: " + 
                        projectileManager.getActiveProjectileCount());
                }
            }
        };
        cleanupTask.runTaskTimer(this, 1200L, 1200L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("§6=== Domination TPS ==="));
        event.getPlayer().sendMessage(Component.text("§e右クリック: §f発射"));
        event.getPlayer().sendMessage(Component.text("§eFキー: §fリロード"));
        event.getPlayer().sendMessage(Component.text("§e/dom join: §fチームに参加"));
    }

    @Override
    public void onDisable() {
        getLogger().info("Domination_test プラグインを無効化しています...");
        
        if (dominationManager != null && dominationManager.isGameActive()) {
            dominationManager.stopGame();
        }
        
        if (projectileManager != null) {
            projectileManager.shutdown();
        }
        
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        
        if (playerStateManager != null) {
            playerStateManager.clearAll();
        }
        
        TeamManager.getInstance().reset();
        
        if (configManager != null) {
            configManager.saveConfig();
        }
        
        getLogger().info("Domination_test プラグインを無効化しました");
    }

    // === ゲッター ===
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public GunManager getGunManager() {
        return gunManager;
    }

    public GunHandler getGunHandler() {
        return gunHandler;
    }
    
    public DamageCalculator getDamageCalculator() {
        return damageCalculator;
    }
    
    public CombatValidator getCombatValidator() {
        return combatValidator;
    }
    
    public ProjectileManager getProjectileManager() {
        return projectileManager;
    }
    
    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }

    public DominationManager getDominationManager() {
        return dominationManager;
    }
    
    public TeamManager getTeamManager() {
        return teamManager;
    }
}
