package io.github.LRGyutoi.domination_test;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DominationCommand implements CommandExecutor, TabCompleter {
    private final Domination_test plugin;
    private final DominationManager dominationManager;

    public DominationCommand(Domination_test plugin, DominationManager dominationManager) {
        this.plugin = plugin;
        this.dominationManager = dominationManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c使用方法: /domination <join|start|stop|setpoint|tickets|listpoints>");
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "join" -> handleJoin(sender, args);
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "setpoint" -> handleSetPoint(sender, args);
            case "tickets" -> handleTickets(sender, args);
            case "listpoints" -> handleListPoints(sender);
            case "removepoint" -> handleRemovePoint(sender, args);
            default -> {
                sender.sendMessage("§c不明なサブコマンド: " + args[0]);
                yield true;
            }
        };
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c使用方法: /domination join <red|blue>");
            return true;
        }

        Team team = switch (args[1].toLowerCase()) {
            case "red" -> Team.RED;
            case "blue" -> Team.BLUE;
            default -> null;
        };

        if (team == null) {
            sender.sendMessage("§c無効なチーム: " + args[1] + " (red または blue)");
            return true;
        }

        TeamManager.getInstance().setPlayerTeam(player, team);
        return true;
    }

    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません");
            return true;
        }

        if (dominationManager.isGameActive()) {
            sender.sendMessage("§cゲームは既に開始されています");
            return true;
        }

        dominationManager.startGame();
        sender.sendMessage("§aゲームを開始しました");
        return true;
    }

    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません");
            return true;
        }

        if (!dominationManager.isGameActive()) {
            sender.sendMessage("§cゲームは開始されていません");
            return true;
        }

        dominationManager.stopGame();
        sender.sendMessage("§aゲームを停止しました");
        return true;
    }

    private boolean handleSetPoint(CommandSender sender, String[] args) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage("§c使用方法: /domination setpoint <id> <表示名> <範囲>");
            sender.sendMessage("§e例: /domination setpoint A 拠点A 10");
            return true;
        }

        String id = args[1];
        String displayName = args[2];
        double radius;

        try {
            radius = Double.parseDouble(args[3]);
            if (radius <= 0) {
                sender.sendMessage("§c範囲は正の数値である必要があります");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c無効な数値: " + args[3]);
            return true;
        }

        Location location = player.getLocation();
        dominationManager.addCapturePoint(id, displayName, location, radius);
        sender.sendMessage("§a拠点を設定しました: " + displayName + " (ID: " + id + ")");
        sender.sendMessage("§7場所: " + String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ()));
        sender.sendMessage("§7範囲: " + radius + " ブロック");
        return true;
    }

    private boolean handleTickets(CommandSender sender, String[] args) {
        if (args.length < 2) {
            // チケット表示
            int redTickets = TeamManager.getInstance().getTickets(Team.RED);
            int blueTickets = TeamManager.getInstance().getTickets(Team.BLUE);
            sender.sendMessage("§c赤チームのチケット: " + redTickets);
            sender.sendMessage("§9青チームのチケット: " + blueTickets);
            return true;
        }

        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage("§c使用方法: /domination tickets set <red|blue> <数値>");
            return true;
        }

        if (!args[1].equalsIgnoreCase("set")) {
            sender.sendMessage("§c使用方法: /domination tickets set <red|blue> <数値>");
            return true;
        }

        Team team = switch (args[2].toLowerCase()) {
            case "red" -> Team.RED;
            case "blue" -> Team.BLUE;
            default -> null;
        };

        if (team == null) {
            sender.sendMessage("§c無効なチーム: " + args[2] + " (red または blue)");
            return true;
        }

        int tickets;
        try {
            tickets = Integer.parseInt(args[3]);
            if (tickets < 0) {
                sender.sendMessage("§cチケット数は0以上である必要があります");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c無効な数値: " + args[3]);
            return true;
        }

        TeamManager.getInstance().setTickets(team, tickets);
        sender.sendMessage("§a" + team.getDisplayName() + " チームのチケットを " + tickets + " に設定しました");
        return true;
    }

    private boolean handleListPoints(CommandSender sender) {
        if (dominationManager.getAllCapturePoints().isEmpty()) {
            sender.sendMessage("§e拠点が設定されていません");
            return true;
        }

        sender.sendMessage("§6===== 設定されている拠点 =====");
        dominationManager.getAllCapturePoints().forEach((id, point) -> {
            Location loc = point.getBeaconLocation();
            sender.sendMessage(String.format("§e%s §7(%s) - %s",
                point.getDisplayName(),
                id,
                point.getOwningTeam().getDisplayName()
            ));
            sender.sendMessage(String.format("  §7場所: %.1f, %.1f, %.1f | 範囲: %.1f",
                loc.getX(), loc.getY(), loc.getZ(), point.getCaptureRadius()
            ));
        });
        return true;
    }

    private boolean handleRemovePoint(CommandSender sender, String[] args) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c使用方法: /domination removepoint <id>");
            return true;
        }

        String id = args[1];
        CapturePoint point = dominationManager.getCapturePoint(id);

        if (point == null) {
            sender.sendMessage("§c拠点が見つかりません: " + id);
            return true;
        }

        dominationManager.removeCapturePoint(id);
        sender.sendMessage("§a拠点を削除しました: " + point.getDisplayName() + " (ID: " + id + ")");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("join", "start", "stop", "setpoint", "tickets", "listpoints", "removepoint")
                .stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "join" -> Arrays.asList("red", "blue");
                case "tickets" -> List.of("set");
                case "removepoint" -> new ArrayList<>(dominationManager.getAllCapturePoints().keySet());
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("tickets") && args[1].equalsIgnoreCase("set")) {
            return Arrays.asList("red", "blue");
        }

        return List.of();
    }
}

