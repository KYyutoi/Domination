package io.github.LRGyutoi.domination.domain.model.player;

/**
 * プレイヤーの戦績を表す値オブジェクト
 */
public class PlayerStats {
    private int kills;
    private int deaths;
    private int headshots;
    private int assists;
    private int capturesContributed;

    public PlayerStats() {
        this.kills = 0;
        this.deaths = 0;
        this.headshots = 0;
        this.assists = 0;
        this.capturesContributed = 0;
    }

    // === クエリ ===

    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getHeadshots() { return headshots; }
    public int getAssists() { return assists; }
    public int getCapturesContributed() { return capturesContributed; }

    public double getKillDeathRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public double getHeadshotRate() {
        return kills == 0 ? 0 : (double) headshots / kills * 100;
    }

    // === コマンド ===

    public void recordKill(boolean isHeadshot) {
        kills++;
        if (isHeadshot) {
            headshots++;
        }
    }

    public void recordKill() {
        kills++;
    }

    public void recordDeath() {
        deaths++;
    }

    public void recordAssist() {
        assists++;
    }

    public void recordCaptureContribution() {
        capturesContributed++;
    }

    public void reset() {
        kills = 0;
        deaths = 0;
        headshots = 0;
        assists = 0;
        capturesContributed = 0;
    }
}
