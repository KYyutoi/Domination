package io.github.LRGyutoi.domination.application.usecase;

import io.github.LRGyutoi.domination.application.port.out.MessageAdapter;
import io.github.LRGyutoi.domination.domain.model.capture.CapturePoint;
import io.github.LRGyutoi.domination.domain.model.player.GamePlayer;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import io.github.LRGyutoi.domination.domain.model.team.TeamId;
import io.github.LRGyutoi.domination.domain.repository.CapturePointRepository;
import io.github.LRGyutoi.domination.domain.repository.PlayerRepository;

import java.util.List;

/**
 * 拠点占領進行ユースケース
 */
public class CaptureProgressUseCase {

    private static final double CAPTURE_RATE_PER_SECOND = 0.1; // 10秒で完全占領

    private final CapturePointRepository capturePointRepository;
    private final PlayerRepository playerRepository;
    private final MessageAdapter messageAdapter;

    public CaptureProgressUseCase(
            CapturePointRepository capturePointRepository,
            PlayerRepository playerRepository,
            MessageAdapter messageAdapter
    ) {
        this.capturePointRepository = capturePointRepository;
        this.playerRepository = playerRepository;
        this.messageAdapter = messageAdapter;
    }

    /**
     * 拠点占領を更新（定期的に呼び出される）
     */
    public void updateCaptureProgress(String pointName, List<PlayerId> playersInRange) {
        CapturePoint point = capturePointRepository.findByName(pointName).orElse(null);
        if (point == null) {
            return;
        }

        // 範囲内のプレイヤーをチームごとにカウント
        int redCount = 0;
        int blueCount = 0;

        for (PlayerId playerId : playersInRange) {
            GamePlayer player = playerRepository.findById(playerId).orElse(null);
            if (player == null || !player.isInGame()) continue;

            TeamId teamId = player.getTeamId();
            if (teamId == TeamId.RED) redCount++;
            else if (teamId == TeamId.BLUE) blueCount++;
        }

        // 占領進行
        CaptureResult result = processCapture(point, redCount, blueCount);

        // 結果に応じて通知
        if (result.isCaptured()) {
            notifyCaptured(point, result.newOwner());
        } else if (result.isContested()) {
            notifyContested(point, playersInRange);
        }

        capturePointRepository.save(point);
    }

    /**
     * 占領処理
     */
    private CaptureResult processCapture(CapturePoint point, int redCount, int blueCount) {
        if (redCount == 0 && blueCount == 0) {
            // 誰もいない場合は進行なし
            return CaptureResult.noChange();
        }

        if (redCount > 0 && blueCount > 0) {
            // 両チームがいる場合は拮抗
            return CaptureResult.contested();
        }

        TeamId capturingTeam = redCount > 0 ? TeamId.RED : TeamId.BLUE;
        int playerCount = Math.max(redCount, blueCount);

        // プレイヤー数に応じて占領速度UP（最大3倍）
        double speedMultiplier = Math.min(3.0, 1.0 + (playerCount - 1) * 0.5);
        double progressDelta = CAPTURE_RATE_PER_SECOND * speedMultiplier;

        // 現在の所有者と占領中のチームが同じなら防御進行
        if (point.getOwningTeamId().equals(capturingTeam)) {
            // すでに占領済み
            if (point.getCaptureProgress() >= 1.0) {
                return CaptureResult.noChange();
            }
            // 占領を進める
            point.advanceCapture(progressDelta, capturingTeam);
        } else {
            // 相手チームの場合は奪取
            point.advanceCapture(progressDelta, capturingTeam);

            if (point.getOwningTeamId().equals(capturingTeam)) {
                return CaptureResult.captured(capturingTeam);
            }
        }

        return CaptureResult.inProgress(capturingTeam);
    }

    /**
     * 占領完了を通知
     */
    private void notifyCaptured(CapturePoint point, TeamId newOwner) {
        String teamName = newOwner == TeamId.RED ? "§c赤チーム" : "§9青チーム";
        messageAdapter.broadcast(teamName + "§fが§e" + point.getName() + "§fを占領しました！");
    }

    /**
     * 拮抗状態を通知
     */
    private void notifyContested(CapturePoint point, List<PlayerId> players) {
        for (PlayerId playerId : players) {
            messageAdapter.sendActionBar(playerId, "§e" + point.getName() + " - 拮抗中！");
        }
    }

    /**
     * 占領結果
     */
    private record CaptureResult(
        boolean isCaptured,
        boolean isContested,
        TeamId newOwner,
        TeamId capturingTeam
    ) {
        static CaptureResult noChange() {
            return new CaptureResult(false, false, null, null);
        }

        static CaptureResult contested() {
            return new CaptureResult(false, true, null, null);
        }

        static CaptureResult captured(TeamId newOwner) {
            return new CaptureResult(true, false, newOwner, newOwner);
        }

        static CaptureResult inProgress(TeamId capturingTeam) {
            return new CaptureResult(false, false, null, capturingTeam);
        }
    }
}
