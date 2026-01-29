package io.github.LRGyutoi.domination.domain.repository;

import io.github.LRGyutoi.domination.domain.model.team.Team;
import io.github.LRGyutoi.domination.domain.model.team.TeamId;

import java.util.Optional;

/**
 * チームリポジトリインターフェース
 */
public interface TeamRepository {

    /**
     * チームを保存
     */
    void save(Team team);

    /**
     * チームIDで検索
     */
    Optional<Team> findById(TeamId id);

    /**
     * 赤チームを取得
     */
    Team getRedTeam();

    /**
     * 青チームを取得
     */
    Team getBlueTeam();

    /**
     * 全チームを初期化
     */
    void reset();
}
