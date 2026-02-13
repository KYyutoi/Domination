package io.github.LRGyutoi.domination;

import io.github.LRGyutoi.domination.application.port.out.*;
import io.github.LRGyutoi.domination.application.usecase.*;
import io.github.LRGyutoi.domination.domain.repository.*;
import io.github.LRGyutoi.domination.domain.service.*;
import io.github.LRGyutoi.domination.infrastructure.adapter.*;
import io.github.LRGyutoi.domination.infrastructure.repository.*;
import io.github.LRGyutoi.domination.presentation.command.*;
import io.github.LRGyutoi.domination.presentation.listener.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Dominationプラグイン メインクラス
 *
 * クリーンアーキテクチャに基づく拠点制圧型TPSゲームプラグイン
 * このクラスはComposition Rootとして依存関係の構築のみを担当する
 */
public class DominationPlugin extends JavaPlugin {

    private Logger logger;

    // Domain Services
    private DamageCalculationService damageCalculationService;
    private HeadshotDetectionService headshotDetectionService;
    private CaptureProgressService captureProgressService;

    // Repositories（インターフェース型で保持）
    private PlayerRepository playerRepository;
    private TeamRepository teamRepository;
    private WeaponRepository weaponRepository;
    private CapturePointRepository capturePointRepository;

    // Adapters（インターフェース型で保持 = DIPに準拠）
    private WorldAdapter worldAdapter;
    private MessageAdapter messageAdapter;
    private EffectAdapter effectAdapter;
    private PlayerAdapter playerAdapter;
    private ProjectileAdapter projectileAdapter;

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
     * ドメインサービスを初期化（config.ymlの値を使用）
     */
    private void initializeDomainServices() {
        logger.info("ドメインサービスを初期化中...");

        FileConfiguration config = getConfig();

        damageCalculationService = new DamageCalculationService(
            config.getDouble("damage.headshot-multiplier", 2.0),
            config.getBoolean("damage.distance-falloff.enabled", false),
            config.getDouble("damage.distance-falloff.start-distance", 30.0),
            config.getDouble("damage.distance-falloff.end-distance", 100.0),
            config.getDouble("damage.distance-falloff.min-damage-percent", 0.5)
        );

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
