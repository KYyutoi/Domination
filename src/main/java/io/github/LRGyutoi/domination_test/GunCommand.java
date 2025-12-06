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
            sender.sendMessage("§c使用方法: /gun <give|list|reload|setdamage|setfirerate|setmagazine|setreloadtime>");
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "setdamage" -> handleSetDamage(sender, args);
            case "setfirerate" -> handleSetFireRate(sender, args);
            case "setmagazine" -> handleSetMagazine(sender, args);
            case "setreloadtime" -> handleSetReloadTime(sender, args);
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

    private boolean handleSetDamage(CommandSender sender, String[] args) {
        if (!sender.hasPermission("domination.gun.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§c使用方法: /gun setdamage <gun_type> <damage>");
            return true;
        }

        String gunId = args[1].toLowerCase();
        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) {
            sender.sendMessage("§c不明な銃: " + gunId);
            sender.sendMessage("§e/gun list で利用可能な銃を確認してください");
            return true;
        }

        double damage;
        try {
            damage = Double.parseDouble(args[2]);
            if (damage < 0) {
                sender.sendMessage("§cダメージは0以上の数値である必要があります");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c無効な数値: " + args[2]);
            return true;
        }

        double oldDamage = gun.getDamage();
        if (gunManager.setGunDamage(gunId, damage)) {
            sender.sendMessage("§a" + gun.getName() + " §aのダメージを §f" + oldDamage + " §a-> §f" + damage + " §aに変更しました");
            sender.sendMessage("§7変更は即座に反映され、guns.ymlに保存されました");
        } else {
            sender.sendMessage("§cダメージの変更に失敗しました");
        }
        return true;
    }

    private boolean handleSetFireRate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("domination.gun.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§c使用方法: /gun setfirerate <gun_type> <ticks>");
            sender.sendMessage("§7(数値が小さいほど連射速度が速い。1tick = 0.05秒)");
            return true;
        }

        String gunId = args[1].toLowerCase();
        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) {
            sender.sendMessage("§c不明な銃: " + gunId);
            sender.sendMessage("§e/gun list で利用可能な銃を確認してください");
            return true;
        }

        int fireRate;
        try {
            fireRate = Integer.parseInt(args[2]);
            if (fireRate < 1) {
                sender.sendMessage("§c連射速度は1以上の整数である必要があります");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c無効な数値: " + args[2]);
            return true;
        }

        int oldFireRate = gun.getFireRate();
        if (gunManager.setGunFireRate(gunId, fireRate)) {
            sender.sendMessage("§a" + gun.getName() + " §aの連射速度を §f" + oldFireRate + "tick §a-> §f" + fireRate + "tick §aに変更しました");
            sender.sendMessage("§7(" + (fireRate * 0.05) + "秒間隔)");
            sender.sendMessage("§7変更は即座に反映され、guns.ymlに保存されました");
        } else {
            sender.sendMessage("§c連射速度の変更に失敗しました");
        }
        return true;
    }

    private boolean handleSetMagazine(CommandSender sender, String[] args) {
        if (!sender.hasPermission("domination.gun.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§c使用方法: /gun setmagazine <gun_type> <size>");
            return true;
        }

        String gunId = args[1].toLowerCase();
        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) {
            sender.sendMessage("§c不明な銃: " + gunId);
            sender.sendMessage("§e/gun list で利用可能な銃を確認してください");
            return true;
        }

        int magazineSize;
        try {
            magazineSize = Integer.parseInt(args[2]);
            if (magazineSize < 1) {
                sender.sendMessage("§c弾数は1以上の整数である必要があります");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c無効な数値: " + args[2]);
            return true;
        }

        int oldMagazineSize = gun.getMagazineSize();
        if (gunManager.setGunMagazineSize(gunId, magazineSize)) {
            sender.sendMessage("§a" + gun.getName() + " §aの弾数を §f" + oldMagazineSize + " §a-> §f" + magazineSize + " §aに変更しました");
            sender.sendMessage("§7変更は即座に反映され、guns.ymlに保存されました");
        } else {
            sender.sendMessage("§c弾数の変更に失敗しました");
        }
        return true;
    }

    private boolean handleSetReloadTime(CommandSender sender, String[] args) {
        if (!sender.hasPermission("domination.gun.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§c使用方法: /gun setreloadtime <gun_type> <ticks>");
            sender.sendMessage("§7(20ticks = 1秒)");
            return true;
        }

        String gunId = args[1].toLowerCase();
        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) {
            sender.sendMessage("§c不明な銃: " + gunId);
            sender.sendMessage("§e/gun list で利用可能な銃を確認してください");
            return true;
        }

        int reloadTime;
        try {
            reloadTime = Integer.parseInt(args[2]);
            if (reloadTime < 1) {
                sender.sendMessage("§cリロード時間は1以上の整数である必要があります");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c無効な数値: " + args[2]);
            return true;
        }

        int oldReloadTime = gun.getReloadTime();
        if (gunManager.setGunReloadTime(gunId, reloadTime)) {
            sender.sendMessage("§a" + gun.getName() + " §aのリロード時間を §f" + (oldReloadTime / 20.0) + "秒 §a-> §f" + (reloadTime / 20.0) + "秒 §aに変更しました");
            sender.sendMessage("§7変更は即座に反映され、guns.ymlに保存されました");
        } else {
            sender.sendMessage("§cリロード時間の変更に失敗しました");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("give", "list", "reload", "setdamage", "setfirerate", "setmagazine", "setreloadtime")
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

        // setdamage, setfirerate, setmagazine, setreloadtime の銃ID補完
        if (args.length == 2 && (args[0].equalsIgnoreCase("setdamage")
                || args[0].equalsIgnoreCase("setfirerate")
                || args[0].equalsIgnoreCase("setmagazine")
                || args[0].equalsIgnoreCase("setreloadtime"))) {
            return new ArrayList<>(gunManager.getGunIds()).stream()
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        // 数値の例を提案
        if (args.length == 3 && args[0].equalsIgnoreCase("setdamage")) {
            return Stream.of("1.0", "5.0", "10.0", "15.0", "20.0")
                .filter(s -> s.startsWith(args[2]))
                .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setfirerate")) {
            return Stream.of("1", "2", "5", "10", "20")
                .filter(s -> s.startsWith(args[2]))
                .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setmagazine")) {
            return Stream.of("10", "20", "30", "50", "100")
                .filter(s -> s.startsWith(args[2]))
                .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setreloadtime")) {
            return Stream.of("20", "40", "60", "80", "100")
                .filter(s -> s.startsWith(args[2]))
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}