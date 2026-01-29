package io.github.LRGyutoi.domination.domain.model.capture;

/**
 * 拠点の占領状態を表す値オブジェクト
 */
public record CaptureState(
    double progress,      // -100（青完全占領）〜 +100（赤完全占領）
    boolean isContested,  // 両チームが範囲内にいる
    int redPlayersInZone,
    int bluePlayersInZone
) {
    private static final double CAPTURE_THRESHOLD = 75.0;

    public static CaptureState neutral() {
        return new CaptureState(0.0, false, 0, 0);
    }

    public boolean isOwnedByRed() {
        return progress >= CAPTURE_THRESHOLD;
    }

    public boolean isOwnedByBlue() {
        return progress <= -CAPTURE_THRESHOLD;
    }

    public boolean isNeutral() {
        return !isOwnedByRed() && !isOwnedByBlue();
    }

    /**
     * 占領の進行度を更新
     */
    public CaptureState withProgressUpdate(int redPlayers, int bluePlayers, double captureSpeed) {
        double newProgress = this.progress;
        boolean contested = redPlayers > 0 && bluePlayers > 0;

        if (!contested) {
            if (redPlayers > bluePlayers) {
                newProgress += (redPlayers - bluePlayers) * captureSpeed;
            } else if (bluePlayers > redPlayers) {
                newProgress -= (bluePlayers - redPlayers) * captureSpeed;
            }
        }

        // 範囲制限
        newProgress = Math.max(-100.0, Math.min(100.0, newProgress));

        return new CaptureState(newProgress, contested, redPlayers, bluePlayers);
    }
}
