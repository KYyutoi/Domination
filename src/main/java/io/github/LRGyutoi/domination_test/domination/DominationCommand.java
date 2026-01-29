package io.github.LRGyutoi.domination_test.domination;

import io.github.LRGyutoi.domination_test.Domination_test;
import io.github.LRGyutoi.domination_test.team.Team;
import io.github.LRGyutoi.domination_test.team.TeamManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 陣地取りゲームのコマンドを処理するクラス
 */
public class DominationCommand implements CommandExecutor, TabCompleter {
    
    private final Domination_test plugin;
    private final DominationManager dominationManager;
    private final TeamManager teamManager;
    
    public DominationCommand(Domination_test plugin, DominationManager dominationManager) {
        this.plugin = plugin;
        this.dominationManager = dominationManager;
        this.teamManager = TeamManager.getInstance();
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "join" -> handleJoin(sender, args);
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "setpoint" -> handleSetPoint(sender, args);
            case "removepoint" -> handleRemovePoint(sender, args);
            case "listpoints" -> handleListPoints(sender);
            case "tickets" -> handleTickets(sender, args);
            case "status" -> handleStatus(sender);
            default -> sendHelp(sender);
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Domination コマンド ===");
        sender.sendMessage("§e/dom join [red|blue] §7- チームに参加");
        sender.sendMessage("§e/dom start §7- ゲームを開始");
        sender.sendMessage("§e/dom stop §7- ゲームを停止");
        sender.sendMessage("§e/dom setpoint <id> <name> <radius> §7- 拠点を設定");
        sender.sendMessage("§e/dom removepoint <id> §7- 拠点を削除");
        sender.sendMessage("§e/dom listpoints §7- 拠点一覧");
        sender.sendMessage("§e/dom tickets <red|blue> <amount> §7- チケットを設定");
        sender.sendMessage("§e/dom status §7- ゲーム状態を表示");
    }
    
    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行できます");
            return;
        }
        
        Team team;
        if (args.length >= 2) {
            team = Team.fromString(args[1]);
            if (team == Team.NEUTRAL) {
                sender.sendMessage("§c無効なチーム名です。red または blue を指定してください");
                return;
            }
        } else {
            // 自動振り分け
            team = teamManager.getSmallerTeam();
        }
        
        teamManager.setPlayerTeam(player, team);
    }
    
    private void handleStart(CommandSender sender) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§c権限がありません");
            return;
        }
        
        if (dominationManager.isGameActive()) {
            sender.sendMessage("§cゲームは既に開始されています");
            return;
        }
        
        if (dominationManager.getAllCapturePoints().isEmpty()) {
            sender.sendMessage("§c拠点が設定されていません。先に /dom setpoint で拠点を設定してください");
            return;
        }
        
        dominationManager.startGame();
        sender.sendMessage("§aゲームを開始しました");
    }
    
    private void handleStop(CommandSender sender) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§c権限がありません");
            return;
        }
        
        if (!dominationManager.isGameActive()) {
            sender.sendMessage("§cゲームは開始されていません");
            return;
        }
        
        dominationManager.stopGame();
        sender.sendMessage("§cゲームを停止しました");
    }
    
    private void handleSetPoint(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行できます");
            return;
        }
        
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§c権限がありません");
            return;
        }
        
        if (args.length < 4) {
            sender.sendMessage("§c使用法: /dom setpoint <id> <name> <radius>");
            return;
        }
        
        String id = args[1];
        String name = args[2];
        double radius;
        
        try {
            radius = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c無効な半径: " + args[3]);
            return;
        }
        
        Location location = player.getLocation();
        dominationManager.addCapturePoint(id, name, location, radius);
        sender.sendMessage(String.format("§a拠点 '%s' を設定しました (半径: %.1f)", name, radius));
    }
    
    private void handleRemovePoint(CommandSender sender, String[] args) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§c権限がありません");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /dom removepoint <id>");
            return;
        }
        
        String id = args[1];
        if (dominationManager.getCapturePoint(id) == null) {
            sender.sendMessage("§c拠点が見つかりません: " + id);
            return;
        }
        
        dominationManager.removeCapturePoint(id);
        sender.sendMessage("§a拠点 '" + id + "' を削除しました");
    }
    
    private void handleListPoints(CommandSender sender) {
        var points = dominationManager.getAllCapturePoints();
        
        if (points.isEmpty()) {
            sender.sendMessage("§7設定されている拠点はありません");
            return;
        }
        
        sender.sendMessage("§6=== 拠点一覧 ===");
        for (CapturePoint point : points.values()) {
            Location loc = point.getBeaconLocation();
            String owner = point.getOwningTeam().getDisplayName();
            sender.sendMessage(String.format("§7- §f%s §7(ID: %s) §7所有: %s §7位置: %.0f, %.0f, %.0f",
                point.getDisplayName(), point.getId(), owner, loc.getX(), loc.getY(), loc.getZ()));
        }
    }
    
    private void handleTickets(CommandSender sender, String[] args) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§c権限がありません");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§c使用法: /dom tickets <red|blue> <amount>");
            return;
        }
        
        Team team = Team.fromString(args[1]);
        if (team == Team.NEUTRAL) {
            sender.sendMessage("§c無効なチーム名です");
            return;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c無効な数値: " + args[2]);
            return;
        }
        
        teamManager.setTickets(team, amount);
        sender.sendMessage(String.format("§a%s チームのチケットを %d に設定しました", 
            team.getDisplayName(), amount));
    }
    
    private void handleStatus(CommandSender sender) {
        sender.sendMessage("§6=== ゲーム状態 ===");
        sender.sendMessage("§7ゲーム: " + (dominationManager.isGameActive() ? "§a進行中" : "§c停止"));
        sender.sendMessage("§c赤チーム: §f" + teamManager.getTickets(Team.RED) + " チケット (" + 
            teamManager.getTeamSize(Team.RED) + " 人)");
        sender.sendMessage("§9青チーム: §f" + teamManager.getTickets(Team.BLUE) + " チケット (" +
            teamManager.getTeamSize(Team.BLUE) + " 人)");
        
        sender.sendMessage("§7拠点:");
        for (CapturePoint point : dominationManager.getAllCapturePoints().values()) {
            String owner = point.getOwningTeam().getDisplayName();
            String contested = point.isContested() ? " §e[争奪中]" : "";
            sender.sendMessage(String.format("  §7- %s: %s%s", point.getDisplayName(), owner, contested));
        }
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(List.of("join", "start", "stop", "setpoint", 
                "removepoint", "listpoints", "tickets", "status"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "join", "tickets" -> completions.addAll(List.of("red", "blue"));
                case "removepoint" -> completions.addAll(dominationManager.getAllCapturePoints().keySet());
            }
        }
        
        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(input))
            .collect(Collectors.toList());
    }
}
