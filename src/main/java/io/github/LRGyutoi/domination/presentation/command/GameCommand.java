package io.github.LRGyutoi.domination.presentation.command;

import io.github.LRGyutoi.domination.domain.model.capture.CapturePoint;
import io.github.LRGyutoi.domination.domain.model.team.TeamId;
import io.github.LRGyutoi.domination.domain.repository.CapturePointRepository;
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
 * ゲーム管理コマンド
 * /domination start
 * /domination stop
 * /domination status
 * /domination addpoint <name> <radius>
 */
public class GameCommand implements CommandExecutor, TabCompleter {

    private final CapturePointRepository capturePointRepository;
    private final TeamRepository teamRepository;

    private boolean gameRunning = false;

    public GameCommand(
            CapturePointRepository capturePointRepository,
            TeamRepository teamRepository
    ) {
        this.capturePointRepository = capturePointRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            showUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "status" -> handleStatus(sender);
            case "addpoint" -> handleAddPoint(sender, args);
            case "removepoint" -> handleRemovePoint(sender, args);
            case "reset" -> handleReset(sender);
            default -> {
                showUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§cこのコマンドを実行する権限がありません");
            return true;
        }

        if (gameRunning) {
            sender.sendMessage("§cゲームはすでに開始しています");
            return true;
        }

        // チームをリセット
        teamRepository.reset();

        gameRunning = true;
        sender.sendMessage("§aゲームを開始しました！");

        return true;
    }

    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§cこのコマンドを実行する権限がありません");
            return true;
        }

        if (!gameRunning) {
            sender.sendMessage("§cゲームは開始していません");
            return true;
        }

        gameRunning = false;
        sender.sendMessage("§eゲームを停止しました");

        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        sender.sendMessage("§7=== ゲームステータス ===");
        sender.sendMessage("§7状態: " + (gameRunning ? "§a進行中" : "§c停止中"));

        sender.sendMessage("§7拠点:");
        for (CapturePoint point : capturePointRepository.findAll()) {
            TeamId owner = point.getOwningTeamId();
            String ownerColor;
            if (TeamId.RED.equals(owner)) {
                ownerColor = "§c";
            } else if (TeamId.BLUE.equals(owner)) {
                ownerColor = "§9";
            } else {
                ownerColor = "§7";
            }
            sender.sendMessage("  " + ownerColor + point.getDisplayName() +
                " §7(" + String.format("%.0f%%", point.getCaptureProgress() * 100) + ")");
        }

        return true;
    }

    private boolean handleAddPoint(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます");
            return true;
        }

        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§cこのコマンドを実行する権限がありません");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§c使用法: /domination addpoint <name> <radius>");
            return true;
        }

        String name = args[1];
        double radius;

        try {
            radius = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c無効な半径です");
            return true;
        }

        // プレイヤーの現在位置に拠点を追加
        double x = player.getLocation().getX();
        double y = player.getLocation().getY();
        double z = player.getLocation().getZ();

        CapturePoint point = new CapturePoint(name, x, y, z, radius);
        capturePointRepository.save(point);

        sender.sendMessage("§a拠点 '" + name + "' を追加しました");

        return true;
    }

    private boolean handleRemovePoint(CommandSender sender, String[] args) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§cこのコマンドを実行する権限がありません");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c使用法: /domination removepoint <name>");
            return true;
        }

        String name = args[1];
        capturePointRepository.remove(name);

        sender.sendMessage("§e拠点 '" + name + "' を削除しました");

        return true;
    }

    private boolean handleReset(CommandSender sender) {
        if (!sender.hasPermission("domination.admin")) {
            sender.sendMessage("§cこのコマンドを実行する権限がありません");
            return true;
        }

        teamRepository.reset();
        capturePointRepository.clear();
        gameRunning = false;

        sender.sendMessage("§eゲームをリセットしました");

        return true;
    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage("§7=== Domination Commands ===");
        sender.sendMessage("§e/domination start §7- ゲームを開始");
        sender.sendMessage("§e/domination stop §7- ゲームを停止");
        sender.sendMessage("§e/domination status §7- ステータスを表示");
        sender.sendMessage("§e/domination addpoint <name> <radius> §7- 拠点を追加");
        sender.sendMessage("§e/domination removepoint <name> §7- 拠点を削除");
        sender.sendMessage("§e/domination reset §7- ゲームをリセット");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "stop", "status", "addpoint", "removepoint", "reset")
                .stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("removepoint")) {
            return capturePointRepository.findAll().stream()
                .map(CapturePoint::getDisplayName)
                .filter(s -> s.startsWith(args[1]))
                .collect(Collectors.toList());
        }

        return List.of();
    }

    public boolean isGameRunning() {
        return gameRunning;
    }
}
