package io.github.LRGyutoi.domination.infrastructure.repository;

import io.github.LRGyutoi.domination.domain.model.player.GamePlayer;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import io.github.LRGyutoi.domination.domain.repository.PlayerRepository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * インメモリPlayerRepository実装
 */
public class InMemoryPlayerRepository implements PlayerRepository {

    private final Map<String, GamePlayer> players = new ConcurrentHashMap<>();

    @Override
    public void save(GamePlayer player) {
        players.put(player.getId().getValue(), player);
    }

    @Override
    public Optional<GamePlayer> findById(PlayerId id) {
        return Optional.ofNullable(players.get(id.getValue()));
    }

    @Override
    public Collection<GamePlayer> findAll() {
        return players.values();
    }

    @Override
    public Collection<GamePlayer> findAllInGame() {
        return players.values().stream()
            .filter(GamePlayer::isInGame)
            .collect(Collectors.toList());
    }

    @Override
    public void remove(PlayerId id) {
        players.remove(id.getValue());
    }

    @Override
    public void clear() {
        players.clear();
    }

    @Override
    public boolean exists(PlayerId id) {
        return players.containsKey(id.getValue());
    }
}
