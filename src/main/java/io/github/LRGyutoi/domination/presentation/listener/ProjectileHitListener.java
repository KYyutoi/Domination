package io.github.LRGyutoi.domination.presentation.listener;

import io.github.LRGyutoi.domination.application.port.out.ProjectileAdapter;
import io.github.LRGyutoi.domination.application.usecase.ProcessHitUseCase;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponId;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * 弾丸ヒットイベントリスナー
 * Presentation層はインターフェース（ProjectileAdapter）にのみ依存する
 */
public class ProjectileHitListener implements Listener {

    private final ProcessHitUseCase processHitUseCase;
    private final ProjectileAdapter projectileAdapter;

    public ProjectileHitListener(
            ProcessHitUseCase processHitUseCase,
            ProjectileAdapter projectileAdapter
    ) {
        this.processHitUseCase = processHitUseCase;
        this.projectileAdapter = projectileAdapter;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        // 弾丸データを取得
        ProjectileAdapter.ProjectileData data = projectileAdapter.getProjectileData(projectile.getUniqueId());
        if (data == null) {
            return; // 管理外の弾丸
        }

        // プレイヤーにヒットした場合
        if (event.getHitEntity() instanceof Player victim) {
            handlePlayerHit(projectile, victim, data);
        }

        // 弾丸データをクリーンアップ
        projectileAdapter.removeProjectile(projectile.getUniqueId());
    }

    private void handlePlayerHit(Projectile projectile, Player victim, ProjectileAdapter.ProjectileData data) {
        Location hitLocation = projectile.getLocation();
        Location shooterLocation = getShooterLocation(projectile);

        double distance = shooterLocation != null
            ? shooterLocation.distance(hitLocation)
            : 0;

        ProcessHitUseCase.HitCommand command = new ProcessHitUseCase.HitCommand(
            data.shooterId(),
            PlayerId.of(victim.getUniqueId().toString()),
            WeaponId.of(data.weaponId()),
            hitLocation.getX(),
            hitLocation.getY(),
            hitLocation.getZ(),
            distance
        );

        processHitUseCase.execute(command);
    }

    private Location getShooterLocation(Projectile projectile) {
        if (projectile.getShooter() instanceof Player shooter) {
            return shooter.getLocation();
        }
        return null;
    }
}
