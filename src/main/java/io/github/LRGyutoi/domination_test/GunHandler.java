package io.github.LRGyutoi.domination_test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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
                        data.ensureAmmoInitialized(gunId, gun.getMagazineSize());
                    }
                    updateAmmoDisplay(player, item, gun, data);
                }
            }
        } else {
            data.setCurrentGun(null);
            autoFirePlayers.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // メインハンド以外（オフハンド）のインタラクトは無視
        if (event.getHand() != EquipmentSlot.HAND) return;

        // 受信デバッグ（キャンセルフラグのログは削除）
        plugin.getLogger().info("onPlayerInteract received: action=" + event.getAction());

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // MainHand は常に非nullのため型のみチェック
        if (item.getType() != Material.CROSSBOW) {
            plugin.getLogger().info("Main hand is not CROSSBOW: " + item.getType());
            return;
        }

        String gunId = getGunId(item);
        if (gunId == null) {
            plugin.getLogger().info("CROSSBOW without gun_id tag");
            return;
        }

        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) {
            player.sendMessage("§c銃の設定が見つかりません: " + gunId);
            return;
        }

        PlayerGunData data = getPlayerData(player.getUniqueId());
        Action action = event.getAction();

        // カスタム銃の場合、バニラの動作をキャンセル
        event.setCancelled(true);

        // デバッグ情報
        plugin.getLogger().info("PlayerInteract - Action: " + action + ", Gun: " + gunId);

        // 右クリックで発射
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            plugin.getLogger().info("Right click detected, attempting to fire");

            // まず1発目を発射
            boolean fired = fireGun(player, gun, data, item);

            // フルオートの場合は連続発射を開始
            if (gun.isAutoFire() && fired) {
                if (!autoFirePlayers.contains(player.getUniqueId())) {
                    autoFirePlayers.add(player.getUniqueId());
                    startAutoFire(player, gun, data, item);
                }
            }
        } else {
            // 非対応操作のログ
            plugin.getLogger().info("Non-fire action: " + action);
        }
    }

    // Rキー（持ち替えキー）でリロード
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // MainHand は常に非nullのため型のみチェック
        if (item.getType() != Material.CROSSBOW) return;

        String gunId = getGunId(item);
        if (gunId == null) return;

        // オフハンドスワップをキャンセル（Rキーをリロードとして使用する）
        event.setCancelled(true);

        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) return;

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
        }.runTaskTimer(plugin, 0, gun.getFireRate());
    }

    private boolean fireGun(Player player, GunStats gun, PlayerGunData data, ItemStack item) {
        plugin.getLogger().info("fireGun called for player: " + player.getName());

        if (data.isReloading()) {
            player.sendMessage("§cリロード中です");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastShot = currentTime - data.getLastShotTime();
        long requiredDelay = gun.getFireRate() * 50L;

        if (timeSinceLastShot < requiredDelay) {
            plugin.getLogger().info("Fire rate limit: " + timeSinceLastShot + "ms < " + requiredDelay + "ms");
            return false;
        }

        int ammo = data.getAmmo(gun.getId());
        plugin.getLogger().info("Current ammo: " + ammo);

        if (ammo <= 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.0f);
            player.sendMessage("§c弾切れ！ §eRキーでリロード (持ち替えキー)");
            return false;
        }

        data.useAmmo(gun.getId());
        data.setLastShotTime(currentTime);

        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

        int pellets = gun.getPellets();
        plugin.getLogger().info("Firing " + pellets + " pellets");

        for (int i = 0; i < pellets; i++) {
            shootProjectile(player, gun);
        }

        updateAmmoDisplay(player, item, gun, data);
        player.sendMessage("§7[§e" + data.getAmmo(gun.getId()) + "§7/§e" + gun.getMagazineSize() + "§7]");

        return true;
    }

    private void shootProjectile(Player player, GunStats gun) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        // スプレッドなし - 一直線に飛ぶ

        plugin.getLogger().info("Spawning arrow at: " + eyeLoc + " with direction: " + direction);

        Arrow arrow = player.getWorld().spawnArrow(
            eyeLoc,
            direction,
            (float) gun.getProjectileSpeed(),
            0.0f // スプレッドを0に固定
        );

        arrow.setShooter(player);
        arrow.setDamage(gun.getDamage());
        arrow.setPierceLevel(0);
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

        // 矢を透明化
        arrow.setVisibleByDefault(false);
        arrow.setInvulnerable(true);

        arrow.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "gun_projectile"),
            PersistentDataType.STRING,
            gun.getId()
        );

        // 火のエフェクトのみを飛ばすための軌跡タスク
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100; // 最大5秒間追跡

            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead() || arrow.isOnGround() || ticks++ > maxTicks) {
                    this.cancel();
                    return;
                }

                Location arrowLoc = arrow.getLocation();
                // 火のエフェクトを矢の軌跡に沿って表示
                player.getWorld().spawnParticle(Particle.FLAME, arrowLoc, 3, 0.05, 0.05, 0.05, 0.01);
                player.getWorld().spawnParticle(Particle.SMOKE, arrowLoc, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // 発射時の視覚効果
        player.getWorld().spawnParticle(Particle.FLAME, eyeLoc, 15, 0.1, 0.1, 0.1, 0.08);
        player.getWorld().spawnParticle(Particle.SMOKE, eyeLoc, 8, 0.05, 0.05, 0.05, 0.03);
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
        player.sendMessage("§eリロード中... (§c" + (gun.getReloadTime() / 20) + "秒§e)");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    data.setAmmo(gun.getId(), gun.getMagazineSize());
                    data.setReloading(false);
                    player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1.0f, 1.5f);
                    player.sendMessage("§aリロード完了！ §7[§e" + gun.getMagazineSize() + "§7/§e" + gun.getMagazineSize() + "§7]");
                    updateAmmoDisplay(player, item, gun, data);
                }
            }
        }.runTaskLater(plugin, gun.getReloadTime());
    }

    private void updateAmmoDisplay(Player player, ItemStack item, GunStats gun, PlayerGunData data) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int ammo = data.getAmmo(gun.getId());

        // 銃の名前をパース（レガシーカラーコードを含む可能性がある）
        String gunName = gun.getName();
        Component displayName;
        if (gunName.contains("§")) {
            displayName = Component.text(gunName.replaceAll("§.", ""));
        } else {
            displayName = Component.text(gunName);
        }

        meta.displayName(displayName);
        meta.lore(Arrays.asList(
            Component.text("弾薬: ", NamedTextColor.GRAY)
                .append(Component.text(ammo + " / " + gun.getMagazineSize(), NamedTextColor.WHITE)),
            Component.text("ダメージ: ", NamedTextColor.GRAY)
                .append(Component.text(gun.getDamage(), NamedTextColor.WHITE)),
            Component.text("連射速度: ", NamedTextColor.GRAY)
                .append(Component.text(gun.getFireRate() + " ticks", NamedTextColor.WHITE)),
            Component.text(gun.isAutoFire() ? "フルオート" : "セミオート", NamedTextColor.GRAY),
            Component.empty(),
            Component.text("右クリック: ", NamedTextColor.YELLOW)
                .append(Component.text("発射", NamedTextColor.WHITE)),
            Component.text("Rキー: ", NamedTextColor.YELLOW)
                .append(Component.text("リロード (持ち替えキー)", NamedTextColor.WHITE))
        ));

        item.setItemMeta(meta);
    }

    public ItemStack createGun(String gunId) {
        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) return null;

        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        // 銃の名前をパース（レガシーカラーコードを含む可能性がある）
        String gunName = gun.getName();
        Component displayName;
        if (gunName.contains("§")) {
            displayName = Component.text(gunName.replaceAll("§.", ""));
        } else {
            displayName = Component.text(gunName);
        }

        meta.displayName(displayName);
        meta.getPersistentDataContainer().set(gunIdKey, PersistentDataType.STRING, gunId);
        meta.lore(Arrays.asList(
            Component.text("弾薬: ", NamedTextColor.GRAY)
                .append(Component.text(gun.getMagazineSize() + " / " + gun.getMagazineSize(), NamedTextColor.WHITE)),
            Component.text("ダメージ: ", NamedTextColor.GRAY)
                .append(Component.text(gun.getDamage(), NamedTextColor.WHITE)),
            Component.text("連射速度: ", NamedTextColor.GRAY)
                .append(Component.text(gun.getFireRate() + " ticks", NamedTextColor.WHITE)),
            Component.text(gun.isAutoFire() ? "フルオート" : "セミオート", NamedTextColor.GRAY),
            Component.empty(),
            Component.text("右クリック: ", NamedTextColor.YELLOW)
                .append(Component.text("発射", NamedTextColor.WHITE)),
            Component.text("Rキー: ", NamedTextColor.YELLOW)
                .append(Component.text("リロード (持ち替えキー)", NamedTextColor.WHITE))
        ));

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
