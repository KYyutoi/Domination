package io.github.LRGyutoi.domination.presentation.command;

import io.github.LRGyutoi.domination.domain.model.weapon.Weapon;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponId;
import io.github.LRGyutoi.domination.domain.repository.WeaponRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 武器配布コマンド
 * /weapon give <weapon_id>
 * /weapon list
 * /weapon reload
 */
public class WeaponCommand implements CommandExecutor, TabCompleter {

    private final WeaponRepository weaponRepository;
    private final NamespacedKey weaponIdKey;

    public WeaponCommand(WeaponRepository weaponRepository, Plugin plugin) {
        this.weaponRepository = weaponRepository;
        this.weaponIdKey = new NamespacedKey(plugin, "weapon_id");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            showUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            default -> {
                showUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c使用法: /weapon give <weapon_id>");
            return true;
        }

        String weaponIdStr = args[1];
        Weapon weapon = weaponRepository.findById(WeaponId.of(weaponIdStr)).orElse(null);

        if (weapon == null) {
            sender.sendMessage("§c武器が見つかりません: " + weaponIdStr);
            return true;
        }

        // 武器アイテムを作成
        ItemStack weaponItem = createWeaponItem(weapon);
        player.getInventory().addItem(weaponItem);

        player.sendMessage("§a武器を取得しました: " + weapon.getDisplayName());

        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage("§7=== 利用可能な武器 ===");

        for (Weapon weapon : weaponRepository.findAll()) {
            sender.sendMessage("§e" + weapon.getId().getValue() + " §7- " + weapon.getDisplayName());
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§cこのコマンドを実行する権限がありません");
            return true;
        }

        weaponRepository.reload();
        sender.sendMessage("§a武器設定をリロードしました");

        return true;
    }

    private ItemStack createWeaponItem(Weapon weapon) {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();

        // 名前を設定
        meta.displayName(Component.text(weapon.getDisplayName()).color(NamedTextColor.GOLD));

        // Loreを設定
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("ダメージ: " + weapon.getStats().baseDamage()).color(NamedTextColor.GRAY));
        lore.add(Component.text("弾数: " + weapon.getStats().magazineSize()).color(NamedTextColor.GRAY));
        lore.add(Component.text("リロード: " + weapon.getStats().reloadTimeMs() + "ms").color(NamedTextColor.GRAY));
        meta.lore(lore);

        // 武器IDをPersistentDataに保存
        meta.getPersistentDataContainer().set(weaponIdKey, PersistentDataType.STRING, weapon.getId().getValue());

        item.setItemMeta(meta);
        return item;
    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage("§7=== Weapon Commands ===");
        sender.sendMessage("§e/weapon give <weapon_id> §7- 武器を取得");
        sender.sendMessage("§e/weapon list §7- 武器一覧を表示");
        sender.sendMessage("§e/weapon reload §7- 武器設定をリロード");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("give", "list", "reload").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return weaponRepository.findAll().stream()
                .map(w -> w.getId().getValue())
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        return List.of();
    }
}
