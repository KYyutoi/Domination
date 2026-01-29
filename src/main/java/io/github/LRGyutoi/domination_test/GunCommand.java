package io.github.LRGyutoi.domination_test;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GunCommand implements CommandExecutor, TabCompleter {
    private final GunManager gunManager;
    private final GunHandler gunHandler;

    public GunCommand(GunManager gunManager, GunHandler gunHandler) {
        this.gunManager = gunManager;
        this.gunHandler = gunHandler;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c使用方法: /gun <give|list|reload>");
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            default -> {
                sender.sendMessage("§c不明なサブコマンド: " + args[0]);
                yield true;
            }
        };
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("domination.gun.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§c使用方法: /gun give <player> <gun_type>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cプレイヤーが見つかりません: " + args[1]);
            return true;
        }

        String gunId = args[2].toLowerCase();
        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) {
            sender.sendMessage("§c不明な銃: " + gunId);
            sender.sendMessage("§e/gun list で利用可能な銃を確認してください");
            return true;
        }

        gunHandler.giveGunWithAmmo(target, gunId);
        sender.sendMessage("§a" + target.getName() + " に " + gun.getName() + " §aを付与しました");
        target.sendMessage("§a" + gun.getName() + " §aを受け取りました！");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage("§6===== 利用可能な銃 =====");
        gunManager.getAllGuns().forEach((id, gun) -> {
            sender.sendMessage("§e" + id + " §7- " + gun.getName());
            sender.sendMessage("  §7ダメージ: §f" + gun.getDamage() +
                             " §7| 連射速度: §f" + gun.getFireRate() + "tick" +
                             " §7| 弾薬: §f" + gun.getMagazineSize());
        });
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("domination.gun.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません");
            return true;
        }

        gunManager.loadGuns();
        sender.sendMessage("§a銃の設定をリロードしました");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("give", "list", "reload")
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return new ArrayList<>(gunManager.getGunIds()).stream()
                .filter(s -> s.startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}