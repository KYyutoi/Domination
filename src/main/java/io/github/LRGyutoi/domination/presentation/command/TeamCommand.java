package io.github.LRGyutoi.domination.presentation.command;

import io.github.LRGyutoi.domination.domain.model.player.GamePlayer;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import io.github.LRGyutoi.domination.domain.model.team.Team;
import io.github.LRGyutoi.domination.domain.model.team.TeamId;
import io.github.LRGyutoi.domination.domain.repository.PlayerRepository;
import io.github.LRGyutoi.domination.domain.repository.TeamRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * チーム管理コマンド
 * /team join <red|blue>
 * /team leave
 * /team list
 */
public class TeamCommand implements CommandExecutor, TabCompleter {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    public TeamCommand(PlayerRepository playerRepository, TeamRepository teamRepository) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます");
            return true;
        }

        if (args.length == 0) {
            showUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "list" -> handleList(player);
            default -> {
                showUsage(player);
                yield true;
            }
        };
    }

    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c使用法: /team join <red|blue>");
            return true;
        }

        String teamName = args[1].toLowerCase();
        TeamId teamId;

        try {
            teamId = TeamId.valueOf(teamName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c無効なチーム名です。red または blue を指定してください");
            return true;
        }

        if (teamId == TeamId.NEUTRAL) {
            player.sendMessage("§cニュートラルチームには参加できません");
            return true;
        }

        PlayerId playerId = PlayerId.of(player.getUniqueId().toString());
        GamePlayer gamePlayer = playerRepository.findById(playerId)
            .orElseGet(() -> GamePlayer.create(playerId, player.getName()));

        // チームに参加
        gamePlayer.joinTeam(teamId);
        playerRepository.save(gamePlayer);

        // チームメンバーを追加
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team != null) {
            team.addMember(playerId);
            teamRepository.save(team);
        }

        String teamDisplayName = teamId == TeamId.RED ? "§c赤チーム" : "§9青チーム";
        player.sendMessage("§a" + teamDisplayName + "§aに参加しました！");

        return true;
    }

    private boolean handleLeave(Player player) {
        PlayerId playerId = PlayerId.of(player.getUniqueId().toString());
        GamePlayer gamePlayer = playerRepository.findById(playerId).orElse(null);

        if (gamePlayer == null || !gamePlayer.isInGame()) {
            player.sendMessage("§cチームに参加していません");
            return true;
        }

        TeamId oldTeamId = gamePlayer.getTeamId();

        // チームから離脱
        gamePlayer.leaveGame();
        playerRepository.save(gamePlayer);

        // チームメンバーから削除
        Team team = teamRepository.findById(oldTeamId).orElse(null);
        if (team != null) {
            team.removeMember(playerId);
            teamRepository.save(team);
        }

        player.sendMessage("§eチームから離脱しました");

        return true;
    }

    private boolean handleList(Player player) {
        Team redTeam = teamRepository.getRedTeam();
        Team blueTeam = teamRepository.getBlueTeam();

        player.sendMessage("§7=== チーム一覧 ===");
        player.sendMessage("§c赤チーム §7(" + redTeam.getMemberCount() + "人) - チケット: " + redTeam.getTickets());
        player.sendMessage("§9青チーム §7(" + blueTeam.getMemberCount() + "人) - チケット: " + blueTeam.getTickets());

        return true;
    }

    private void showUsage(Player player) {
        player.sendMessage("§7=== Team Commands ===");
        player.sendMessage("§e/team join <red|blue> §7- チームに参加");
        player.sendMessage("§e/team leave §7- チームから離脱");
        player.sendMessage("§e/team list §7- チーム一覧を表示");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("join", "leave", "list").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return Arrays.asList("red", "blue").stream()
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        return List.of();
    }
}
