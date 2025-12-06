package io.github.LRGyutoi.domination_test;
public class GunStats {
    private final String id;
    private final String name;
    private double damage;
    private int fireRate;
    private int magazineSize;
    private int reloadTime;
    private final double projectileSpeed;
    private final double spread;
    private final boolean autoFire;
    private final int pellets;
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
    public String getId() { return id; }
    public String getName() { return name; }
    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }
    public int getFireRate() { return fireRate; }
    public void setFireRate(int fireRate) { this.fireRate = fireRate; }
    public int getMagazineSize() { return magazineSize; }
    public void setMagazineSize(int magazineSize) { this.magazineSize = magazineSize; }
    public int getReloadTime() { return reloadTime; }
    public void setReloadTime(int reloadTime) { this.reloadTime = reloadTime; }
    public double getProjectileSpeed() { return projectileSpeed; }
    public double getSpread() { return spread; }
    public boolean isAutoFire() { return autoFire; }
    public int getPellets() { return pellets; }
}