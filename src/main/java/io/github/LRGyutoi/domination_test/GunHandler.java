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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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
                    // 初回装備時に弾薬を初期化（満タン）
                    if (!data.hasAmmoEntry(gunId)) {
                        data.setAmmo(gunId, gun.getMagazineSize());
                    }
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
        // メインハンド以外（オフハンド）のインタラクトは無視
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.CROSSBOW) return;

        String gunId = getGunId(item);
        if (gunId == null) return;

        // カスタム銃の場合、バニラの動作をキャンセル
        event.setCancelled(true);

        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) {
            plugin.getLogger().warning("銃が見つかりません: " + gunId);
            return;
        }

        PlayerGunData data = getPlayerData(player.getUniqueId());

        Action action = event.getAction();

        plugin.getLogger().info("PlayerInteract - GunID: " + gunId + ", Action: " + action);

        // 右クリックで発射
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            plugin.getLogger().info("Right click detected for gun: " + gunId + ", AutoFire: " + gun.isAutoFire());
            if (gun.isAutoFire()) {
                if (!autoFirePlayers.contains(player.getUniqueId())) {
                    autoFirePlayers.add(player.getUniqueId());
                    // まず1発目を即座に発射
                    boolean fired = fireGun(player, gun, data, item);
                    if (fired) {
                        // 発射成功したら自動連射を開始
                        startAutoFire(player, gun, data, item);
                    } else {
                        // 発射失敗（弾切れなど）の場合は自動連射リストから削除
                        autoFirePlayers.remove(player.getUniqueId());
                    }
                }
            } else {
                fireGun(player, gun, data, item);
            }
        }
    }

    // Rキー（持ち替えキー/Fキー）でリロード
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.CROSSBOW) return;

        String gunId = getGunId(item);
        if (gunId == null) return;

        // オフハンドスワップをキャンセル（Rキー/Fキーをリロードとして使用する）
        event.setCancelled(true);

        plugin.getLogger().info("PlayerSwapHandItems - Reload requested for gun: " + gunId);

        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) {
            plugin.getLogger().warning("銃が見つかりません: " + gunId);
            return;
        }

        PlayerGunData data = getPlayerData(player.getUniqueId());
        reload(player, gun, data, item);
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
        }.runTaskTimer(plugin, gun.getFireRate(), gun.getFireRate()); // 初回もfire-rate後に実行
    }

    private boolean fireGun(Player player, GunStats gun, PlayerGunData data, ItemStack item) {
        plugin.getLogger().info("fireGun called for gun: " + gun.getId() + ", player: " + player.getName());

        if (data.isReloading()) {
            plugin.getLogger().info("Player is reloading, cannot fire");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastShot = currentTime - data.getLastShotTime();
        // オートファイアの場合は、より緩いチェック（fire-rate - 10ms）
        long requiredDelay = gun.isAutoFire()
            ? Math.max(0, gun.getFireRate() * 50L - 10)
            : gun.getFireRate() * 50L;

        if (timeSinceLastShot < requiredDelay) {
            plugin.getLogger().info("Fire rate limit: " + timeSinceLastShot + "ms < " + requiredDelay + "ms");
            return false;
        }

        int ammo = data.getAmmo(gun.getId());
        plugin.getLogger().info("Current ammo for " + gun.getId() + ": " + ammo);

        if (ammo <= 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.0f);
            player.sendMessage("§c弾切れ！ §eRキー（F）でリロード");
            return false;
        }

        data.useAmmo(gun.getId());
        data.setLastShotTime(currentTime);

        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

        int pellets = gun.getPellets();
        plugin.getLogger().info("Firing " + pellets + " pellet(s)");

        for (int i = 0; i < pellets; i++) {
            shootProjectile(player, gun);
        }

        updateAmmoDisplay(player, item, gun, data);

        return true;
    }

    private void shootProjectile(Player player, GunStats gun) {
        Location eyeLoc = player.getEyeLocation();
        // 弾の発射位置を0.2ブロック下げる
        eyeLoc.add(0, -0.2, 0);
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
            net.kyori.adventure.text.Component.text("弾薬: ", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .append(net.kyori.adventure.text.Component.text(ammo + " / " + gun.getMagazineSize(), net.kyori.adventure.text.format.NamedTextColor.WHITE)),
            net.kyori.adventure.text.Component.text("ダメージ: ", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .append(net.kyori.adventure.text.Component.text(gun.getDamage(), net.kyori.adventure.text.format.NamedTextColor.WHITE)),
            net.kyori.adventure.text.Component.text("連射速度: ", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .append(net.kyori.adventure.text.Component.text(gun.getFireRate() + " ticks", net.kyori.adventure.text.format.NamedTextColor.WHITE)),
            net.kyori.adventure.text.Component.text(gun.isAutoFire() ? "フルオート" : "セミオート", net.kyori.adventure.text.format.NamedTextColor.GRAY),
            net.kyori.adventure.text.Component.empty(),
            net.kyori.adventure.text.Component.text("右クリック: ", net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .append(net.kyori.adventure.text.Component.text("発射", net.kyori.adventure.text.format.NamedTextColor.WHITE)),
            net.kyori.adventure.text.Component.text("Rキー（F）: ", net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .append(net.kyori.adventure.text.Component.text("リロード", net.kyori.adventure.text.format.NamedTextColor.WHITE))
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
            net.kyori.adventure.text.Component.text("弾薬: ", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .append(net.kyori.adventure.text.Component.text(gun.getMagazineSize() + " / " + gun.getMagazineSize(), net.kyori.adventure.text.format.NamedTextColor.WHITE)),
            net.kyori.adventure.text.Component.text("ダメージ: ", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .append(net.kyori.adventure.text.Component.text(gun.getDamage(), net.kyori.adventure.text.format.NamedTextColor.WHITE)),
            net.kyori.adventure.text.Component.text("連射速度: ", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .append(net.kyori.adventure.text.Component.text(gun.getFireRate() + " ticks", net.kyori.adventure.text.format.NamedTextColor.WHITE)),
            net.kyori.adventure.text.Component.text(gun.isAutoFire() ? "フルオート" : "セミオート", net.kyori.adventure.text.format.NamedTextColor.GRAY),
            net.kyori.adventure.text.Component.empty(),
            net.kyori.adventure.text.Component.text("右クリック: ", net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .append(net.kyori.adventure.text.Component.text("発射", net.kyori.adventure.text.format.NamedTextColor.WHITE)),
            net.kyori.adventure.text.Component.text("Rキー（F）: ", net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .append(net.kyori.adventure.text.Component.text("リロード", net.kyori.adventure.text.format.NamedTextColor.WHITE))
        ));

        // クロスボウに矢を装填しない（バニラの動作を防ぐ）

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