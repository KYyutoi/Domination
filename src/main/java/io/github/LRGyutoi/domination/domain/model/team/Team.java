package io.github.LRGyutoi.domination.domain.model.team;

import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * チームエンティティ
 * チームのメンバー管理とチケット管理を行う
 */
public class Team {
    private final TeamId id;
    private final String displayName;
    private final TeamColor color;
    private final Set<UUID> members;
    private int tickets;

    public Team(TeamId id, String displayName, TeamColor color, int initialTickets) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.members = new HashSet<>();
        this.tickets = initialTickets;
    }

    // === クエリ ===

    public TeamId getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TeamColor getColor() {
        return color;
    }

    public int getTickets() {
        return tickets;
    }

    public int getMemberCount() {
        return members.size();
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public boolean hasMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean isDefeated() {
        return tickets <= 0;
    }

    // === コマンド ===

    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    public void addMember(PlayerId playerId) {
        members.add(playerId.value());
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public void removeMember(PlayerId playerId) {
        members.remove(playerId.value());
    }

    public void reduceTickets(int amount) {
        this.tickets = Math.max(0, this.tickets - amount);
    }

    public void decrementTickets() {
        this.tickets = Math.max(0, this.tickets - 1);
    }

    public void setTickets(int tickets) {
        this.tickets = Math.max(0, tickets);
    }

    public void reset(int initialTickets) {
        this.members.clear();
        this.tickets = initialTickets;
    }

    // === ファクトリ ===

    public static Team createRed(int initialTickets) {
        return new Team(TeamId.RED, "赤", TeamColor.RED, initialTickets);
    }

    public static Team createBlue(int initialTickets) {
        return new Team(TeamId.BLUE, "青", TeamColor.BLUE, initialTickets);
    }
}
