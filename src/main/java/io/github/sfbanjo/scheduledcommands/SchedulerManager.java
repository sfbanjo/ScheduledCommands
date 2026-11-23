package io.github.sfbanjo.scheduledcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SchedulerManager {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final class ClockTask {
        final String id;
        final LocalTime time;
        final Set<DayOfWeek> days;
        final List<String> commands;
        ZonedDateTime nextRun;

        ClockTask(String id, LocalTime time, Set<DayOfWeek> days, List<String> commands, ZonedDateTime nextRun) {
            this.id = id;
            this.time = time;
            this.days = days;
            this.commands = commands;
            this.nextRun = nextRun;
        }
    }

    private static final class IntervalTask {
        final String id;
        final Duration interval;
        final List<String> commands;
        Instant nextRun;

        IntervalTask(String id, Duration interval, List<String> commands, Instant nextRun) {
            this.id = id;
            this.interval = interval;
            this.commands = commands;
            this.nextRun = nextRun;
        }
    }

    private final ScheduledCommands plugin;
    private final List<ClockTask> clockTasks = new ArrayList<>();
    private final List<IntervalTask> intervalTasks = new ArrayList<>();

    private ZoneId zoneId = ZoneId.systemDefault();
    private BukkitTask tickerTask;

    public SchedulerManager(ScheduledCommands plugin) {
        this.plugin = plugin;
    }

    public void loadAndStart(boolean isStartup) {
        shutdown();
        clockTasks.clear();
        intervalTasks.clear();

        String rawZone = plugin.getConfig().getString("settings.timezone", "SYSTEM");
        ZoneId parsedZone = TimeParser.parseZoneId(rawZone);
        if (parsedZone == null) {
            plugin.getLogger().warning("Invalid timezone '" + rawZone + "' in config.yml. Falling back to system default.");
            this.zoneId = ZoneId.systemDefault();
        } else {
            this.zoneId = parsedZone;
        }

        Instant nowInstant = Instant.now();
        ZonedDateTime nowZoned = nowInstant.atZone(zoneId);

        loadClockTasks(nowZoned);
        loadIntervalTasks(nowInstant, isStartup);

        plugin.getLogger().info("Loaded " + clockTasks.size() + " scheduled task(s) and "
                + intervalTasks.size() + " interval task(s) using timezone: " + zoneId.getId());

        this.tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void loadClockTasks(ZonedDateTime nowZoned) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("scheduled-tasks");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection taskSec = section.getConfigurationSection(id);
            if (taskSec == null || !taskSec.getBoolean("enabled", true)) {
                continue;
            }

            String rawTime = taskSec.getString("time");
            LocalTime targetTime = TimeParser.parseTime(rawTime);
            if (targetTime == null) {
                plugin.getLogger().warning("Skipping scheduled-task '" + id + "': invalid time '" + rawTime + "'. Expected HH:mm or HH:mm:ss.");
                continue;
            }

            List<String> rawDays = taskSec.getStringList("days");
            Set<DayOfWeek> days = TimeParser.parseDays(rawDays);
            if (days.isEmpty()) {
                plugin.getLogger().warning("Skipping scheduled-task '" + id + "': invalid 'days' list.");
                continue;
            }

            List<String> commands = cleanCommands(taskSec.getStringList("commands"));
            if (commands.isEmpty()) {
                plugin.getLogger().warning("Skipping scheduled-task '" + id + "': no commands configured.");
                continue;
            }

            ZonedDateTime nextRun = computeNextClockRun(targetTime, days, nowZoned);
            clockTasks.add(new ClockTask(id, targetTime, days, commands, nextRun));
        }
    }

    private void loadIntervalTasks(Instant nowInstant, boolean isStartup) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("interval-tasks");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection taskSec = section.getConfigurationSection(id);
            if (taskSec == null || !taskSec.getBoolean("enabled", true)) {
                continue;
            }

            String rawInterval = taskSec.getString("interval");
            Duration interval = TimeParser.parseDuration(rawInterval);
            if (interval == null) {
                plugin.getLogger().warning("Skipping interval-task '" + id + "': invalid interval '" + rawInterval + "'. Examples: 45s, 20m, 1h30m.");
                continue;
            }

            List<String> commands = cleanCommands(taskSec.getStringList("commands"));
            if (commands.isEmpty()) {
                plugin.getLogger().warning("Skipping interval-task '" + id + "': no commands configured.");
                continue;
            }

            boolean runOnStartup = taskSec.getBoolean("run-on-startup", false);
            if (isStartup && runOnStartup) {
                dispatchCommands(id, commands);
            }

            Instant nextRun = nowInstant.plus(interval);
            intervalTasks.add(new IntervalTask(id, interval, commands, nextRun));
        }
    }

    private void tick() {
        Instant nowInstant = Instant.now();
        ZonedDateTime nowZoned = nowInstant.atZone(zoneId);

        for (ClockTask task : clockTasks) {
            if (!nowZoned.isBefore(task.nextRun)) {
                dispatchCommands(task.id, task.commands);
                task.nextRun = computeNextClockRun(task.time, task.days, nowZoned);
            }
        }

        for (IntervalTask task : intervalTasks) {
            if (!nowInstant.isBefore(task.nextRun)) {
                dispatchCommands(task.id, task.commands);
                while (!task.nextRun.isAfter(nowInstant)) {
                    task.nextRun = task.nextRun.plus(task.interval);
                }
            }
        }
    }

    private ZonedDateTime computeNextClockRun(LocalTime targetTime, Set<DayOfWeek> days, ZonedDateTime now) {
        ZonedDateTime candidate = now.toLocalDate().atTime(targetTime).atZone(zoneId);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        for (int i = 0; i < 7; i++) {
            if (days.contains(candidate.getDayOfWeek())) {
                return candidate;
            }
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private List<String> cleanCommands(List<String> rawCommands) {
        List<String> cleaned = new ArrayList<>();
        for (String cmd : rawCommands) {
            if (cmd == null) {
                continue;
            }
            String trimmed = cmd.trim();
            if (trimmed.startsWith("/")) {
                trimmed = trimmed.substring(1).trim();
            }
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        return cleaned;
    }

    private void dispatchCommands(String taskId, List<String> commands) {
        for (String command : commands) {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception e) {
                plugin.getLogger().warning("Error executing command '" + command + "' for task '" + taskId + "': " + e.getMessage());
            }
        }
    }

    public boolean runTaskNow(String taskId) {
        for (ClockTask task : clockTasks) {
            if (task.id.equalsIgnoreCase(taskId)) {
                dispatchCommands(task.id, task.commands);
                return true;
            }
        }
        for (IntervalTask task : intervalTasks) {
            if (task.id.equalsIgnoreCase(taskId)) {
                dispatchCommands(task.id, task.commands);
                return true;
            }
        }
        return false;
    }

    public List<String> getTaskIds() {
        List<String> ids = new ArrayList<>();
        for (ClockTask task : clockTasks) {
            ids.add(task.id);
        }
        for (IntervalTask task : intervalTasks) {
            ids.add(task.id);
        }
        return ids;
    }

    public List<Component> getStatusLines() {
        List<Component> lines = new ArrayList<>();
        Instant nowInstant = Instant.now();
        ZonedDateTime nowZoned = nowInstant.atZone(zoneId);

        if (clockTasks.isEmpty() && intervalTasks.isEmpty()) {
            lines.add(Component.text("No enabled tasks found in config.yml.", NamedTextColor.YELLOW));
            return lines;
        }

        if (!clockTasks.isEmpty()) {
            lines.add(Component.text("--- Scheduled Tasks (Clock) ---", NamedTextColor.GOLD));
            for (ClockTask task : clockTasks) {
                Duration until = Duration.between(nowZoned, task.nextRun);
                lines.add(Component.text("• " + task.id, NamedTextColor.AQUA)
                        .append(Component.text(" -> next: " + DISPLAY_FORMAT.format(task.nextRun)
                                + " (in " + TimeParser.formatDuration(until) + ")", NamedTextColor.GRAY)));
            }
        }

        if (!intervalTasks.isEmpty()) {
            lines.add(Component.text("--- Interval Tasks (Cyclic) ---", NamedTextColor.GOLD));
            for (IntervalTask task : intervalTasks) {
                Duration until = Duration.between(nowInstant, task.nextRun);
                lines.add(Component.text("• " + task.id, NamedTextColor.GREEN)
                        .append(Component.text(" (every " + TimeParser.formatDuration(task.interval)
                                + ") -> next in " + TimeParser.formatDuration(until), NamedTextColor.GRAY)));
            }
        }

        return lines;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public void shutdown() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
    }
}