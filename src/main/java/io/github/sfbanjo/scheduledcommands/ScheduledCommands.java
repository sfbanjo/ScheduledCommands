package io.github.sfbanjo.scheduledcommands;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ScheduledCommands extends JavaPlugin {

    private SchedulerManager schedulerManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.schedulerManager = new SchedulerManager(this);
        this.schedulerManager.loadAndStart(true);

        SchedCommand schedCommand = new SchedCommand(this, schedulerManager);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        "scheduledcommands",
                        "Manage real-world scheduled console commands",
                        List.of("sched", "scmd"),
                        schedCommand
                )
        );

        getLogger().info("ScheduledCommands enabled!");
    }

    @Override
    public void onDisable() {
        if (schedulerManager != null) {
            schedulerManager.shutdown();
        }
        getLogger().info("ScheduledCommands disabled.");
    }
}