package io.github.LRGyutoi.domination;

import io.github.LRGyutoi.domination.application.port.out.*;
import io.github.LRGyutoi.domination.application.usecase.*;
import io.github.LRGyutoi.domination.domain.repository.*;
import io.github.LRGyutoi.domination.domain.service.*;
import io.github.LRGyutoi.domination.infrastructure.adapter.*;
import io.github.LRGyutoi.domination.infrastructure.repository.*;
import io.github.LRGyutoi.domination.presentation.command.*;
import io.github.LRGyutoi.domination.presentation.listener.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Dominationプラグイン メインクラス
 * 
 * クリーンアーキテクチャに基づく拠点制圧型TPSゲームプラグイン
 * 
 * アーキテクチャ概要:
 * - Domain層: ビジネスロジック（Minecraft依存なし）
 * - Application層: ユースケース、ポート定義
 * - Infrastructure層: Bukkit実装、永続化
 * - Presentation層: コマンド、イベントリスナー
 */
public class DominationPlugin extends JavaPlugin {

    private Logger logger;

    // Domain Services
    private DamageCalculationService damageCalculationService;
    private HeadshotDetectionService headshotDetectionService;
    private CaptureProgressService captureProgressService;

    // Repositories
    private PlayerRepository playerRepository;
    private TeamRepository teamRepository;
    private WeaponRepository weaponRepository;
    private CapturePointRepository capturePointRepository;

    // Adapters
    private BukkitWorldAdapter worldAdapter;
    private BukkitMessageAdapter messageAdapter;
    private BukkitEffectAdapter effectAdapter;
    private BukkitPlayerAdapter playerAdapter;
    private BukkitProjectileAdapter projectileAdapter;

    // Use Cases
    private FireWeaponUseCase fireWeaponUseCase;
    private ProcessHitUseCase processHitUseCase;
    private ReloadWeaponUseCase reloadWeaponUseCase;
    private RespawnPlayerUseCase respawnPlayerUseCase;
    private CaptureProgressUseCase captureProgressUseCase;

    @Override
    public void onEnable() {
        logger = getLogger();
        logger.info("Domination プラグインを起動中...");

        // 設定ファイルを保存
        saveDefaultConfig();
        saveResource("guns.yml", false);

        // 依存関係を構築
        initializeDomainServices();
        initializeRepositories();
        initializeAdapters();
        initializeUseCases();
        registerCommands();
        registerListeners();

        logger.info("Domination プラグインが正常に起動しました！");
    }

    @Override
    public void onDisable() {
        logger.info("Domination プラグインを停止中...");

        // クリーンアップ処理
        if (playerRepository != null) {
            playerRepository.clear();
        }

        logger.info("Domination プラグインを停止しました");
    }

    /**
     * ドメインサービスを初期化
     */
    private void initializeDomainServices() {
        logger.info("ドメインサービスを初期化中...");

        damageCalculationService = DamageCalculationService.withDefaults();
        headshotDetectionService = new HeadshotDetectionService();
        captureProgressService = new CaptureProgressService();
    }

    /**
     * リポジトリを初期化
     */
    private void initializeRepositories() {
        logger.info("リポジトリを初期化中...");

        playerRepository = new InMemoryPlayerRepository();
        teamRepository = new InMemoryTeamRepository();
        weaponRepository = new YamlWeaponRepository(this);
        capturePointRepository = new InMemoryCapturePointRepository();
    }

    /**
     * アダプターを初期化
     */
    private void initializeAdapters() {
        logger.info("アダプターを初期化中...");

        worldAdapter = new BukkitWorldAdapter();
        messageAdapter = new BukkitMessageAdapter();
        effectAdapter = new BukkitEffectAdapter();
        playerAdapter = new BukkitPlayerAdapter();
        projectileAdapter = new BukkitProjectileAdapter();
    }

    /**
     * ユースケースを初期化
     */
    private void initializeUseCases() {
        logger.info("ユースケースを初期化中...");

        fireWeaponUseCase = new FireWeaponUseCase(
            playerRepository,
            weaponRepository,
            projectileAdapter,
            effectAdapter,
            messageAdapter
        );

        processHitUseCase = new ProcessHitUseCase(
            playerRepository,
            weaponRepository,
            damageCalculationService,
            headshotDetectionService,
            worldAdapter,
            playerAdapter,
            effectAdapter,
            messageAdapter
        );

        reloadWeaponUseCase = new ReloadWeaponUseCase(
            playerRepository,
            weaponRepository,
            effectAdapter,
            messageAdapter
        );

        respawnPlayerUseCase = new RespawnPlayerUseCase(
            playerRepository,
            teamRepository,
            playerAdapter,
            messageAdapter
        );

        captureProgressUseCase = new CaptureProgressUseCase(
            capturePointRepository,
            playerRepository,
            messageAdapter
        );
    }

    /**
     * コマンドを登録
     */
    private void registerCommands() {
        logger.info("コマンドを登録中...");

        TeamCommand teamCommand = new TeamCommand(playerRepository, teamRepository);
        getCommand("team").setExecutor(teamCommand);
        getCommand("team").setTabCompleter(teamCommand);

        WeaponCommand weaponCommand = new WeaponCommand(weaponRepository, this);
        getCommand("weapon").setExecutor(weaponCommand);
        getCommand("weapon").setTabCompleter(weaponCommand);

        GameCommand gameCommand = new GameCommand(capturePointRepository, teamRepository);
        getCommand("domination").setExecutor(gameCommand);
        getCommand("domination").setTabCompleter(gameCommand);
    }

    /**
     * イベントリスナーを登録
     */
    private void registerListeners() {
        logger.info("イベントリスナーを登録中...");

        getServer().getPluginManager().registerEvents(
            new ProjectileHitListener(processHitUseCase, projectileAdapter),
            this
        );

        getServer().getPluginManager().registerEvents(
            new WeaponFireListener(fireWeaponUseCase, weaponRepository, this),
            this
        );

        getServer().getPluginManager().registerEvents(
            new PlayerStateListener(playerRepository, respawnPlayerUseCase, this),
            this
        );
    }

    // === Getter methods for testing and external access ===

    public PlayerRepository getPlayerRepository() {
        return playerRepository;
    }

    public TeamRepository getTeamRepository() {
        return teamRepository;
    }

    public WeaponRepository getWeaponRepository() {
        return weaponRepository;
    }
}
