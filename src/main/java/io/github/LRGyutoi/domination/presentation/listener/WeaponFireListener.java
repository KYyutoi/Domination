package io.github.LRGyutoi.domination.presentation.listener;

import io.github.LRGyutoi.domination.application.usecase.FireWeaponUseCase;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponId;
import io.github.LRGyutoi.domination.domain.repository.WeaponRepository;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 武器発射イベントリスナー
 */
public class WeaponFireListener implements Listener {

    private final FireWeaponUseCase fireWeaponUseCase;
    private final WeaponRepository weaponRepository;
    private final NamespacedKey weaponIdKey;

    // 発射クールダウン管理
    private final Map<UUID, Long> lastFireTime = new HashMap<>();

    public WeaponFireListener(
            FireWeaponUseCase fireWeaponUseCase,
            WeaponRepository weaponRepository,
            Plugin plugin
    ) {
        this.fireWeaponUseCase = fireWeaponUseCase;
        this.weaponRepository = weaponRepository;
        this.weaponIdKey = new NamespacedKey(plugin, "weapon_id");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 右クリックのみ
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 武器かどうかチェック
        String weaponId = getWeaponId(item);
        if (weaponId == null) {
            return;
        }

        // クールダウンチェック
        if (isOnCooldown(player.getUniqueId(), 100)) { // 100ms (仮の値)
            return;
        }

        // イベントをキャンセル（通常のアイテム使用を防ぐ）
        event.setCancelled(true);

        // 発射処理
        FireWeaponUseCase.FireResult result = fireWeaponUseCase.execute(
            PlayerId.of(player.getUniqueId().toString()),
            WeaponId.of(weaponId)
        );

        // クールダウンを記録
        if (result.isSuccess()) {
            lastFireTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    /**
     * アイテムから武器IDを取得
     */
    private String getWeaponId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(weaponIdKey, PersistentDataType.STRING);
    }

    /**
     * クールダウン中かどうか
     */
    private boolean isOnCooldown(UUID playerId, int cooldownMs) {
        Long lastFire = lastFireTime.get(playerId);
        if (lastFire == null) {
            return false;
        }

        return System.currentTimeMillis() - lastFire < cooldownMs;
    }
}
