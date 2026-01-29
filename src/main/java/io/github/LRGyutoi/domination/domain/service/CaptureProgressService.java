package io.github.LRGyutoi.domination.domain.service;

import io.github.LRGyutoi.domination.domain.model.capture.CapturePoint;
import io.github.LRGyutoi.domination.domain.model.team.Team;
import io.github.LRGyutoi.domination.domain.model.team.TeamId;

import java.util.Collection;

/**
 * 拠点占領に関するドメインサービス
 * チケット減少ロジックなどを担当
 */
public class CaptureProgressService {

    /**
     * 拠点の占領状況に基づいてチケット減少量を計算
     *
     * @param capturePoints 全拠点
     * @param team          対象チーム
     * @return このチームが失うチケット数
     */
    public int calculateTicketDrain(Collection<CapturePoint> capturePoints, Team team) {
        int ownedByTeam = 0;
        int ownedByEnemy = 0;

        TeamId teamId = team.getId();
        TeamId enemyTeamId = teamId.equals(TeamId.RED) ? TeamId.BLUE : TeamId.RED;

        for (CapturePoint point : capturePoints) {
            TeamId owner = point.getOwningTeamId();
            if (owner.equals(teamId)) {
                ownedByTeam++;
            } else if (owner.equals(enemyTeamId)) {
                ownedByEnemy++;
            }
        }

        // 敵が多く占領しているほどチケットが減る
        int difference = ownedByEnemy - ownedByTeam;
        return Math.max(0, difference);
    }

    /**
     * ゲーム終了条件をチェック
     */
    public boolean isGameOver(Team redTeam, Team blueTeam) {
        return redTeam.isDefeated() || blueTeam.isDefeated();
    }

    /**
     * 勝者チームを取得
     */
    public TeamId getWinner(Team redTeam, Team blueTeam) {
        if (redTeam.isDefeated()) {
            return TeamId.BLUE;
        }
        if (blueTeam.isDefeated()) {
            return TeamId.RED;
        }
        return TeamId.NEUTRAL;
    }
}
