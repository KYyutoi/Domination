package io.github.LRGyutoi.domination.domain.model.capture;

import io.github.LRGyutoi.domination.domain.model.team.TeamId;

/**
 * 拠点（キャプチャポイント）エンティティ
 * 位置情報はドメインモデルでは抽象化（座標は外部で管理）
 */
public class CapturePoint {
    private final String id;
    private final String displayName;
    private final double captureRadius;
    private final double captureSpeed;
    private double x, y, z;
    private CaptureState state;
    private TeamId previousOwner;

    public CapturePoint(String id, String displayName, double captureRadius, double captureSpeed) {
        this.id = id;
        this.displayName = displayName;
        this.captureRadius = captureRadius;
        this.captureSpeed = captureSpeed;
        this.state = CaptureState.neutral();
        this.previousOwner = TeamId.NEUTRAL;
    }

    public CapturePoint(String name, double x, double y, double z, double radius) {
        this.id = name;
        this.displayName = name;
        this.captureRadius = radius;
        this.captureSpeed = 1.0;
        this.x = x;
        this.y = y;
        this.z = z;
        this.state = CaptureState.neutral();
        this.previousOwner = TeamId.NEUTRAL;
    }

    // === クエリ ===

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public double getCaptureRadius() { return captureRadius; }
    public CaptureState getState() { return state; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public double getCaptureProgress() {
        return state.progress();
    }

    public TeamId getOwningTeamId() {
        if (state.isOwnedByRed()) return TeamId.RED;
        if (state.isOwnedByBlue()) return TeamId.BLUE;
        return TeamId.NEUTRAL;
    }

    public boolean isContested() {
        return state.isContested();
    }

    public boolean isNeutral() {
        return state.isNeutral();
    }

    /**
     * 所有権が変わったかどうか
     */
    public boolean hasOwnershipChanged() {
        return !previousOwner.equals(getOwningTeamId());
    }

    // === コマンド ===

    /**
     * 占領を進行させる
     */
    public void advanceCapture(double progressDelta, TeamId capturingTeam) {
        double currentProgress = state.progress();

        // RED = 正の進行、BLUE = 負の進行
        double direction = capturingTeam.equals(TeamId.RED) ? 1.0 : -1.0;
        double newProgress = currentProgress + (progressDelta * direction);

        // 範囲制限
        newProgress = Math.max(-100.0, Math.min(100.0, newProgress));

        this.state = new CaptureState(newProgress, false, 0, 0);

        // 所有権チェック
        TeamId newOwner = getOwningTeamId();
        if (!newOwner.equals(previousOwner)) {
            this.previousOwner = newOwner;
        }
    }

    /**
     * プレイヤー数に基づいて占領状態を更新
     * @return 所有権が変わった場合true
     */
    public boolean updateCapture(int redPlayersInZone, int bluePlayersInZone) {
        previousOwner = getOwningTeamId();
        this.state = state.withProgressUpdate(redPlayersInZone, bluePlayersInZone, captureSpeed);
        return hasOwnershipChanged();
    }

    public void reset() {
        this.state = CaptureState.neutral();
        this.previousOwner = TeamId.NEUTRAL;
    }

    // === ファクトリ ===

    public static CapturePoint create(String id, String displayName, double radius) {
        return new CapturePoint(id, displayName, radius, 1.0);
    }
}
