package io.github.sfbanjo.scheduledcommands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class SchedCommand implements BasicCommand {

    private static final DateTimeFormatter TIME_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (EEEE)");

    private final ScheduledCommands plugin;
    private final SchedulerManager scheduler;

    public SchedCommand(ScheduledCommands plugin, SchedulerManager scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        String permission = plugin.getConfig().getString("settings.permission", "scheduledcommands.admin");

        if (sender instanceof Player && !sender.isOp() && !sender.hasPermission(permission)) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                for (Component line : scheduler.getStatusLines()) {
                    sender.sendMessage(line);
                }
            }
            case "time" -> {
                ZonedDateTime now = ZonedDateTime.now(scheduler.getZoneId());
                sender.sendMessage(Component.text("Current scheduler time [" + scheduler.getZoneId().getId() + "]: ", NamedTextColor.GRAY)
                        .append(Component.text(TIME_DISPLAY.format(now), NamedTextColor.AQUA)));
            }
            case "run" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /sched run <task_id>", NamedTextColor.YELLOW));
                    return;
                }
                String taskId = args[1];
                if (scheduler.runTaskNow(taskId)) {
                    sender.sendMessage(Component.text("Executed task '" + taskId + "'!", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Task '" + taskId + "' not found (or disabled).", NamedTextColor.RED));
                }
            }
            case "reload" -> {
                plugin.reloadConfig();
                scheduler.loadAndStart(false);
                sender.sendMessage(Component.text("ScheduledCommands configuration reloaded!", NamedTextColor.GREEN));
            }
            default -> sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("--- ScheduledCommands Help ---", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/sched list", NamedTextColor.YELLOW).append(Component.text(" - Show all active tasks and countdowns", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sched time", NamedTextColor.YELLOW).append(Component.text(" - Show real-world clock & timezone", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sched run <task>", NamedTextColor.YELLOW).append(Component.text(" - Run a task immediately", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sched reload", NamedTextColor.YELLOW).append(Component.text(" - Reload config.yml and restart timers", NamedTextColor.GRAY)));
    }

    @Override
    public Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        String permission = plugin.getConfig().getString("settings.permission", "scheduledcommands.admin");
        if (sender instanceof Player && !sender.isOp() && !sender.hasPermission(permission)) {
            return List.of();
        }

        if (args.length <= 1) {
            String prefix = (args.length == 1) ? args[0].toLowerCase(Locale.ROOT) : "";
            return filterPrefix(List.of("list", "time", "run", "reload", "help"), prefix);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return filterPrefix(scheduler.getTaskIds(), prefix);
        }

        return List.of();
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(opt);
            }
        }
        return result;
    }
}