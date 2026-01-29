package io.github.LRGyutoi.domination_test.gun;

import io.github.LRGyutoi.domination_test.Domination_test;
import io.github.LRGyutoi.domination_test.combat.CombatValidator;
import io.github.LRGyutoi.domination_test.config.ConfigManager;
import io.github.LRGyutoi.domination_test.player.PlayerStateManager;
import io.github.LRGyutoi.domination_test.projectile.ProjectileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * 銃のインタラクション処理を行うクラス
 * 発射、リロード、弾薬表示などを管理
 */
public class GunHandler implements Listener {
    
    private final Domination_test plugin;
    private final GunManager gunManager;
    private final ConfigManager configManager;
    private final CombatValidator combatValidator;
    private ProjectileManager projectileManager;
    private PlayerStateManager playerStateManager;
    
    private final NamespacedKey gunIdKey;
    private final Set<UUID> autoFirePlayers = new HashSet<>();
    
    // レガシー用：Arrow方式を使用するかどうか
    private boolean useLegacyProjectiles = false;
    
    public GunHandler(Domination_test plugin, GunManager gunManager, 
                      ConfigManager configManager, CombatValidator combatValidator) {
        this.plugin = plugin;
        this.gunManager = gunManager;
        this.configManager = configManager;
        this.combatValidator = combatValidator;
        this.gunIdKey = new NamespacedKey(plugin, "gun_id");
    }
    
    public void setProjectileManager(ProjectileManager projectileManager) {
        this.projectileManager = projectileManager;
    }
    
    public void setPlayerStateManager(PlayerStateManager playerStateManager) {
        this.playerStateManager = playerStateManager;
    }
    
    private PlayerGunData getPlayerData(UUID playerId) {
        if (playerStateManager != null) {
            return playerStateManager.getPlayerGunData(playerId);
        }
        // フォールバック：直接作成
        return new PlayerGunData(playerId);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        autoFirePlayers.remove(event.getPlayer().getUniqueId());
        combatValidator.clearPlayerData(event.getPlayer().getUniqueId());
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
                    // 初回装備時に弾薬を初期化
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
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType() != Material.CROSSBOW) return;
        
        String gunId = getGunId(item);
        if (gunId == null) return;
        
        // バニラの動作をキャンセル
        event.setCancelled(true);
        
        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) {
            plugin.getLogger().warning("銃が見つかりません: " + gunId);
            return;
        }
        
        PlayerGunData data = getPlayerData(player.getUniqueId());
        Action action = event.getAction();
        
        // 右クリックで発射
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (gun.isAutoFire()) {
                if (!autoFirePlayers.contains(player.getUniqueId())) {
                    autoFirePlayers.add(player.getUniqueId());
                    boolean fired = fireGun(player, gun, data, item);
                    if (fired) {
                        startAutoFire(player, gun, data, item);
                    } else {
                        autoFirePlayers.remove(player.getUniqueId());
                    }
                }
            } else {
                fireGun(player, gun, data, item);
            }
        }
    }
    
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType() != Material.CROSSBOW) return;
        
        String gunId = getGunId(item);
        if (gunId == null) return;
        
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
        }.runTaskTimer(plugin, gun.getFireRate(), gun.getFireRate());
    }
    
    private boolean fireGun(Player player, GunStats gun, PlayerGunData data, ItemStack item) {
        if (data.isReloading()) {
            return false;
        }
        
        // 連射速度チェック（チート対策）
        if (!combatValidator.validateShot(player, gun.getFireRate())) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastShot = currentTime - data.getLastShotTime();
        long requiredDelay = gun.isAutoFire()
            ? Math.max(0, gun.getFireRate() * 50L - 10)
            : gun.getFireRate() * 50L;
        
        if (timeSinceLastShot < requiredDelay) {
            return false;
        }
        
        int ammo = data.getAmmo(gun.getId());
        if (ammo <= 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.0f);
            player.sendMessage(Component.text("弾切れ！ ", NamedTextColor.RED)
                .append(Component.text("Fキーでリロード", NamedTextColor.YELLOW)));
            return false;
        }
        
        data.useAmmo(gun.getId());
        data.setLastShotTime(currentTime);
        
        // 発射音
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
        
        // 弾丸を発射
        int pellets = gun.getPellets();
        for (int i = 0; i < pellets; i++) {
            shootProjectile(player, gun);
        }
        
        updateAmmoDisplay(player, item, gun, data);
        
        return true;
    }
    
    private void shootProjectile(Player player, GunStats gun) {
        Location eyeLoc = player.getEyeLocation();
        eyeLoc.add(0, -0.2, 0); // 発射位置を少し下げる
        Vector direction = eyeLoc.getDirection();
        
        // スプレッド（拡散）
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
        
        if (projectileManager != null && !useLegacyProjectiles) {
            // 新しいカスタム弾道システム
            projectileManager.fireProjectile(player, gun, eyeLoc, direction);
        } else {
            // レガシー：Arrow方式
            shootArrowProjectile(player, gun, eyeLoc, direction);
        }
    }
    
    private void shootArrowProjectile(Player player, GunStats gun, Location eyeLoc, Vector direction) {
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
            player.sendMessage(Component.text("既にリロード中です", NamedTextColor.RED));
            return;
        }
        
        int currentAmmo = data.getAmmo(gun.getId());
        if (currentAmmo >= gun.getMagazineSize()) {
            player.sendMessage(Component.text("弾薬は満タンです", NamedTextColor.RED));
            return;
        }
        
        // オートファイアを停止
        autoFirePlayers.remove(player.getUniqueId());
        
        data.setReloading(true);
        player.playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
        player.sendMessage(Component.text("リロード中...", NamedTextColor.YELLOW));
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    data.setAmmo(gun.getId(), gun.getMagazineSize());
                    data.setReloading(false);
                    player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1.0f, 1.5f);
                    player.sendMessage(Component.text("リロード完了！", NamedTextColor.GREEN));
                    updateAmmoDisplay(player, item, gun, data);
                }
            }
        }.runTaskLater(plugin, gun.getReloadTime());
    }
    
    private void updateAmmoDisplay(Player player, ItemStack item, GunStats gun, PlayerGunData data) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        int ammo = data.getAmmo(gun.getId());
        meta.displayName(Component.text(gun.getName()));
        meta.lore(Arrays.asList(
            Component.text("弾薬: ", NamedTextColor.GRAY)
                .append(Component.text(ammo + " / " + gun.getMagazineSize(), NamedTextColor.WHITE)),
            Component.text("ダメージ: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(gun.getDamage()), NamedTextColor.WHITE)),
            Component.text("連射速度: ", NamedTextColor.GRAY)
                .append(Component.text(gun.getFireRate() + " ticks", NamedTextColor.WHITE)),
            Component.text(gun.isAutoFire() ? "フルオート" : "セミオート", NamedTextColor.GRAY),
            Component.empty(),
            Component.text("右クリック: ", NamedTextColor.YELLOW)
                .append(Component.text("発射", NamedTextColor.WHITE)),
            Component.text("Fキー: ", NamedTextColor.YELLOW)
                .append(Component.text("リロード", NamedTextColor.WHITE))
        ));
        
        item.setItemMeta(meta);
    }
    
    public ItemStack createGun(String gunId) {
        GunStats gun = gunManager.getGun(gunId);
        if (gun == null) return null;
        
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        meta.displayName(Component.text(gun.getName()));
        meta.getPersistentDataContainer().set(gunIdKey, PersistentDataType.STRING, gunId);
        meta.lore(Arrays.asList(
            Component.text("弾薬: ", NamedTextColor.GRAY)
                .append(Component.text(gun.getMagazineSize() + " / " + gun.getMagazineSize(), NamedTextColor.WHITE)),
            Component.text("ダメージ: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(gun.getDamage()), NamedTextColor.WHITE)),
            Component.text("連射速度: ", NamedTextColor.GRAY)
                .append(Component.text(gun.getFireRate() + " ticks", NamedTextColor.WHITE)),
            Component.text(gun.isAutoFire() ? "フルオート" : "セミオート", NamedTextColor.GRAY),
            Component.empty(),
            Component.text("右クリック: ", NamedTextColor.YELLOW)
                .append(Component.text("発射", NamedTextColor.WHITE)),
            Component.text("Fキー: ", NamedTextColor.YELLOW)
                .append(Component.text("リロード", NamedTextColor.WHITE))
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
    
    public void stopAutoFire(Player player) {
        autoFirePlayers.remove(player.getUniqueId());
    }
    
    public void setUseLegacyProjectiles(boolean useLegacy) {
        this.useLegacyProjectiles = useLegacy;
    }
}
