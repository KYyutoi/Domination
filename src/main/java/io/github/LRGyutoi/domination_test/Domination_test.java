package io.github.LRGyutoi.domination_test;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Domination_test extends JavaPlugin implements Listener {

    private GunManager gunManager;
    private GunHandler gunHandler;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Domination_test プラグインを有効化しています...");

        // 銃システムの初期化
        gunManager = new GunManager(this);
        gunHandler = new GunHandler(this, gunManager);

        // イベントリスナーの登録
        getServer().getPluginManager().registerEvents(gunHandler, this);
        getServer().getPluginManager().registerEvents(this, this);

        // コマンドの登録
        GunCommand gunCommand = new GunCommand(gunManager, gunHandler);
        getCommand("gun").setExecutor(gunCommand);
        getCommand("gun").setTabCompleter(gunCommand);

        getLogger().info("銃システムを正常に読み込みました！");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Domination_test プラグインを無効化しました");
    }

    public GunManager getGunManager() {
        return gunManager;
    }

    public GunHandler getGunHandler() {
        return gunHandler;
    }
}
