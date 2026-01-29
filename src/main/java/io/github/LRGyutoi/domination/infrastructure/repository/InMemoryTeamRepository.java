package io.github.LRGyutoi.domination.infrastructure.repository;

import io.github.LRGyutoi.domination.domain.model.team.Team;
import io.github.LRGyutoi.domination.domain.model.team.TeamColor;
import io.github.LRGyutoi.domination.domain.model.team.TeamId;
import io.github.LRGyutoi.domination.domain.repository.TeamRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * インメモリTeamRepository実装
 */
public class InMemoryTeamRepository implements TeamRepository {

    private static final int DEFAULT_TICKETS = 200;

    private final Map<TeamId, Team> teams = new ConcurrentHashMap<>();

    public InMemoryTeamRepository() {
        reset();
    }

    @Override
    public void save(Team team) {
        teams.put(team.getId(), team);
    }

    @Override
    public Optional<Team> findById(TeamId id) {
        return Optional.ofNullable(teams.get(id));
    }

    @Override
    public Team getRedTeam() {
        return teams.get(TeamId.RED);
    }

    @Override
    public Team getBlueTeam() {
        return teams.get(TeamId.BLUE);
    }

    @Override
    public void reset() {
        teams.clear();

        // 赤チーム
        Team redTeam = new Team(
            TeamId.RED,
            "赤チーム",
            TeamColor.of(255, 0, 0),
            DEFAULT_TICKETS
        );
        teams.put(TeamId.RED, redTeam);

        // 青チーム
        Team blueTeam = new Team(
            TeamId.BLUE,
            "青チーム",
            TeamColor.of(0, 0, 255),
            DEFAULT_TICKETS
        );
        teams.put(TeamId.BLUE, blueTeam);
    }
}
