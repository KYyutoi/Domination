package io.github.LRGyutoi.domination_test.gun;

/**
 * 銃のステータスを定義するクラス
 */
public class GunStats {
    private final String id;
    private final String name;
    private double damage;
    private int fireRate;
    private int magazineSize;
    private int reloadTime;
    private double projectileSpeed;
    private double spread;
    private boolean autoFire;
    private int pellets;
    
    // 追加のステータス
    private double headshotMultiplier = 1.0; // 銃固有のHS倍率（グローバル倍率に乗算）
    private double range = 100.0; // 有効射程
    private double moveSpeedMultiplier = 1.0; // 移動速度係数
    
    public GunStats(String id, String name, double damage, int fireRate, 
                    int magazineSize, int reloadTime, double projectileSpeed, 
                    double spread, boolean autoFire, int pellets) {
        this.id = id;
        this.name = name;
        this.damage = damage;
        this.fireRate = fireRate;
        this.magazineSize = magazineSize;
        this.reloadTime = reloadTime;
        this.projectileSpeed = projectileSpeed;
        this.spread = spread;
        this.autoFire = autoFire;
        this.pellets = pellets;
    }
    
    // === ゲッター ===
    
    public String getId() { return id; }
    public String getName() { return name; }
    public double getDamage() { return damage; }
    public int getFireRate() { return fireRate; }
    public int getMagazineSize() { return magazineSize; }
    public int getReloadTime() { return reloadTime; }
    public double getProjectileSpeed() { return projectileSpeed; }
    public double getSpread() { return spread; }
    public boolean isAutoFire() { return autoFire; }
    public int getPellets() { return pellets; }
    public double getHeadshotMultiplier() { return headshotMultiplier; }
    public double getRange() { return range; }
    public double getMoveSpeedMultiplier() { return moveSpeedMultiplier; }
    
    // === セッター ===
    
    public void setDamage(double damage) { this.damage = damage; }
    public void setFireRate(int fireRate) { this.fireRate = fireRate; }
    public void setMagazineSize(int magazineSize) { this.magazineSize = magazineSize; }
    public void setReloadTime(int reloadTime) { this.reloadTime = reloadTime; }
    public void setProjectileSpeed(double projectileSpeed) { this.projectileSpeed = projectileSpeed; }
    public void setSpread(double spread) { this.spread = spread; }
    public void setAutoFire(boolean autoFire) { this.autoFire = autoFire; }
    public void setPellets(int pellets) { this.pellets = pellets; }
    public void setHeadshotMultiplier(double headshotMultiplier) { this.headshotMultiplier = headshotMultiplier; }
    public void setRange(double range) { this.range = range; }
    public void setMoveSpeedMultiplier(double moveSpeedMultiplier) { this.moveSpeedMultiplier = moveSpeedMultiplier; }
    
    @Override
    public String toString() {
        return "GunStats{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", damage=" + damage +
                ", fireRate=" + fireRate +
                ", magazineSize=" + magazineSize +
                ", autoFire=" + autoFire +
                '}';
    }
}
