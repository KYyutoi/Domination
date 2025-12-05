package io.github.LRGyutoi.hello;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Hello extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getLogger().info("HelloPlugin が起動しました！");
        getServer().getPluginManager().registerEvents(this, this); // イベントを登録
    }

    @Override
    public void onDisable() {
        getLogger().info("HelloPlugin が終了しました！");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // サーバーに入った人にメッセージを送る
        event.getPlayer().sendMessage("こんにちは！ Paperサーバーへようこそ！");
    }
}
