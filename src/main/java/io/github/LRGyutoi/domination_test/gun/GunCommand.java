package io.github.LRGyutoi.domination_test.gun;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 銃関連のコマンドを処理するクラス
 */
public class GunCommand implements CommandExecutor, TabCompleter {
    
    private final GunManager gunManager;
    private final GunHandler gunHandler;
    
    public GunCommand(GunManager gunManager, GunHandler gunHandler) {
        this.gunManager = gunManager;
        this.gunHandler = gunHandler;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c使用法: /gun <give|list|reload|set>");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "set" -> handleSet(sender, args);
            default -> sender.sendMessage("§c不明なサブコマンド: " + args[0]);
        }
        
        return true;
    }
    
    private void handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行できます");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /gun give <銃ID>");
            return;
        }
        
        String gunId = args[1].toLowerCase();
        GunStats gun = gunManager.getGun(gunId);
        
        if (gun == null) {
            sender.sendMessage("§c銃が見つかりません: " + gunId);
            return;
        }
        
        gunHandler.giveGunWithAmmo(player, gunId);
        sender.sendMessage("§a" + gun.getName() + " §aを付与しました");
    }
    
    private void handleList(CommandSender sender) {
        sender.sendMessage("§6=== 利用可能な銃 ===");
        for (String gunId : gunManager.getGunIds()) {
            GunStats gun = gunManager.getGun(gunId);
            sender.sendMessage(String.format("§7- §f%s §7(ID: %s) §7ダメージ: §c%.1f §7連射: §e%d ticks",
                gun.getName(), gunId, gun.getDamage(), gun.getFireRate()));
        }
    }
    
    private void handleReload(CommandSender sender) {
        gunManager.reloadConfig();
        sender.sendMessage("§a銃の設定をリロードしました");
    }
    
    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c使用法: /gun set <銃ID> <property> <value>");
            sender.sendMessage("§7プロパティ: damage, fire-rate, magazine-size, reload-time, spread, range");
            return;
        }
        
        String gunId = args[1].toLowerCase();
        String property = args[2].toLowerCase();
        String valueStr = args[3];
        
        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) {
            sender.sendMessage("§c銃が見つかりません: " + gunId);
            return;
        }
        
        try {
            Object value;
            if (property.equals("fire-rate") || property.equals("magazine-size") || 
                property.equals("reload-time") || property.equals("pellets")) {
                value = Integer.parseInt(valueStr);
            } else if (property.equals("auto-fire")) {
                value = Boolean.parseBoolean(valueStr);
            } else {
                value = Double.parseDouble(valueStr);
            }
            
            if (gunManager.setGunProperty(gunId, property, value)) {
                sender.sendMessage(String.format("§a%s の %s を %s に設定しました", 
                    gun.getName(), property, valueStr));
            } else {
                sender.sendMessage("§c設定の保存に失敗しました");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c無効な値: " + valueStr);
        }
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(List.of("give", "list", "reload", "set"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set")) {
                completions.addAll(gunManager.getGunIds());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            completions.addAll(List.of("damage", "fire-rate", "magazine-size", 
                "reload-time", "projectile-speed", "spread", "auto-fire", "pellets", "range"));
        }
        
        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(input))
            .collect(Collectors.toList());
    }
}
