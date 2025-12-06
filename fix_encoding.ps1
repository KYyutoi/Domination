# GunHandler.javaの内容を正しいUTF-8で書き直す
$gunHandlerContent = @'
package io.github.LRGyutoi.domination_test;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class GunHandler implements Listener {
    private final Domination_test plugin;
    private final GunManager gunManager;
    private final Map<UUID, PlayerGunData> playerData = new HashMap<>();
    private final NamespacedKey gunIdKey;
    private final Set<UUID> autoFirePlayers = new HashSet<>();

    public GunHandler(Domination_test plugin, GunManager gunManager) {
        this.plugin = plugin;
        this.gunManager = gunManager;
        this.gunIdKey = new NamespacedKey(plugin, "gun_id");
    }

    public PlayerGunData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlayerGunData::new);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerData.remove(event.getPlayer().getUniqueId());
        autoFirePlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        PlayerGunData data = getPlayerData(player.getUniqueId());

        if (item != null && item.getType() == Material.CROSSBOW) {
            String gunId = getGunId(item);
            if (gunId != null) {
                data.setCurrentGun(gunId);
                GunStats gun = gunManager.getGun(gunId);
                if (gun != null) {
                    updateAmmoDisplay(player, item, gun, data);
                }
            }
        } else {
            data.setCurrentGun(null);
            autoFirePlayers.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.CROSSBOW) return;

        String gunId = getGunId(item);
        if (gunId == null) return;

        event.setCancelled(true);

        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) return;

        PlayerGunData data = getPlayerData(player.getUniqueId());

        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            reload(player, gun, data, item);
            return;
        }

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (gun.isAutoFire()) {
                if (!autoFirePlayers.contains(player.getUniqueId())) {
                    autoFirePlayers.add(player.getUniqueId());
                    startAutoFire(player, gun, data, item);
                }
            } else {
                fireGun(player, gun, data, item);
            }
        }
    }

    private void startAutoFire(Player player, GunStats gun, PlayerGunData data, ItemStack item) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!autoFirePlayers.contains(player.getUniqueId())
                    || !player.isOnline()
                    || player.getInventory().getItemInMainHand() != item) {
                    autoFirePlayers.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                if (!fireGun(player, gun, data, item)) {
                    autoFirePlayers.remove(player.getUniqueId());
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, gun.getFireRate());
    }

    private boolean fireGun(Player player, GunStats gun, PlayerGunData data, ItemStack item) {
        if (data.isReloading()) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastShot = currentTime - data.getLastShotTime();
        long requiredDelay = gun.getFireRate() * 50L;

        if (timeSinceLastShot < requiredDelay) {
            return false;
        }

        int ammo = data.getAmmo(gun.getId());
        if (ammo <= 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.0f);
            player.sendMessage("§c弾切れ！ §e左クリックでリロード");
            return false;
        }

        data.useAmmo(gun.getId());
        data.setLastShotTime(currentTime);

        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

        int pellets = gun.getPellets();
        for (int i = 0; i < pellets; i++) {
            shootProjectile(player, gun);
        }

        updateAmmoDisplay(player, item, gun, data);

        return true;
    }

    private void shootProjectile(Player player, GunStats gun) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        if (gun.getSpread() > 0) {
            Random random = new Random();
            double spread = gun.getSpread() * 0.1;
            direction.add(new Vector(
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread
            ));
            direction.normalize();
        }

        Arrow arrow = player.getWorld().spawnArrow(
            eyeLoc,
            direction,
            (float) gun.getProjectileSpeed(),
            0.0f
        );

        arrow.setShooter(player);
        arrow.setDamage(gun.getDamage());
        arrow.setPierceLevel(0);
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        arrow.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "gun_projectile"),
            PersistentDataType.STRING,
            gun.getId()
        );
    }

    private void reload(Player player, GunStats gun, PlayerGunData data, ItemStack item) {
        if (data.isReloading()) {
            player.sendMessage("§c既にリロード中です");
            return;
        }

        int currentAmmo = data.getAmmo(gun.getId());
        if (currentAmmo >= gun.getMagazineSize()) {
            player.sendMessage("§c弾薬は満タンです");
            return;
        }

        data.setReloading(true);
        player.playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
        player.sendMessage("§eリロード中...");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    data.setAmmo(gun.getId(), gun.getMagazineSize());
                    data.setReloading(false);
                    player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1.0f, 1.5f);
                    player.sendMessage("§aリロード完了！");
                    updateAmmoDisplay(player, item, gun, data);
                }
            }
        }.runTaskLater(plugin, gun.getReloadTime());
    }

    private void updateAmmoDisplay(Player player, ItemStack item, GunStats gun, PlayerGunData data) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int ammo = data.getAmmo(gun.getId());
        meta.displayName(net.kyori.adventure.text.Component.text(gun.getName()));
        meta.lore(Arrays.asList(
            net.kyori.adventure.text.Component.text("§7弾薬: §f" + ammo + " §7/ §f" + gun.getMagazineSize()),
            net.kyori.adventure.text.Component.text("§7ダメージ: §f" + gun.getDamage()),
            net.kyori.adventure.text.Component.text("§7連射速度: §f" + gun.getFireRate() + " ticks"),
            net.kyori.adventure.text.Component.text("§7" + (gun.isAutoFire() ? "フルオート" : "セミオート")),
            net.kyori.adventure.text.Component.text(""),
            net.kyori.adventure.text.Component.text("§e右クリック: §f発射"),
            net.kyori.adventure.text.Component.text("§e左クリック: §fリロード")
        ));

        item.setItemMeta(meta);
    }

    public ItemStack createGun(String gunId) {
        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) return null;

        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        meta.displayName(net.kyori.adventure.text.Component.text(gun.getName()));
        meta.getPersistentDataContainer().set(gunIdKey, PersistentDataType.STRING, gunId);
        meta.lore(Arrays.asList(
            net.kyori.adventure.text.Component.text("§7弾薬: §f" + gun.getMagazineSize() + " §7/ §f" + gun.getMagazineSize()),
            net.kyori.adventure.text.Component.text("§7ダメージ: §f" + gun.getDamage()),
            net.kyori.adventure.text.Component.text("§7連射速度: §f" + gun.getFireRate() + " ticks"),
            net.kyori.adventure.text.Component.text("§7" + (gun.isAutoFire() ? "フルオート" : "セミオート")),
            net.kyori.adventure.text.Component.text(""),
            net.kyori.adventure.text.Component.text("§e右クリック: §f発射"),
            net.kyori.adventure.text.Component.text("§e左クリック: §fリロード")
        ));

        if (meta instanceof CrossbowMeta crossbowMeta) {
            crossbowMeta.addChargedProjectile(new ItemStack(Material.ARROW));
        }

        item.setItemMeta(meta);
        return item;
    }

    public String getGunId(ItemStack item) {
        if (item == null || item.getType() != Material.CROSSBOW) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(gunIdKey, PersistentDataType.STRING);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;

        if (arrow.getPersistentDataContainer().has(
            new NamespacedKey(plugin, "gun_projectile"),
            PersistentDataType.STRING
        )) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    arrow.remove();
                }
            }.runTaskLater(plugin, 1);
        }
    }

    public void giveGunWithAmmo(Player player, String gunId) {
        ItemStack gun = createGun(gunId);
        if (gun == null) return;

        GunStats stats = gunManager.getGun(gunId);
        if (stats == null) return;

        PlayerGunData data = getPlayerData(player.getUniqueId());
        data.setAmmo(gunId, stats.getMagazineSize());

        player.getInventory().addItem(gun);
    }
}
'@

[System.IO.File]::WriteAllText("D:\Domination\Domination_test\src\main\java\io\github\LRGyutoi\domination_test\GunHandler.java", $gunHandlerContent, [System.Text.UTF8Encoding]::new($false))

Write-Host "GunHandler.java fixed"

# GunCommand.javaの内容を正しいUTF-8で書き直す
$gunCommandContent = @'
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
'@

[System.IO.File]::WriteAllText("D:\Domination\Domination_test\src\main\java\io\github\LRGyutoi\domination_test\GunCommand.java", $gunCommandContent, [System.Text.UTF8Encoding]::new($false))

Write-Host "GunCommand.java fixed"
Write-Host "All files have been fixed with proper UTF-8 encoding!"

