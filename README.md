# ScheduledCommands

A lightweight **Paper / Purpur 26.3** plugin (Java 25) that executes console commands at **real-world clock times** or **real-world recurring intervals** — completely independent of Minecraft server ticks or TPS.

---

## Features

- **Real-world time (`scheduled-tasks`):** Run one or multiple console commands at exact wall-clock times (e.g. `04:00` or `21:30:15`).
- **Day-of-week filter:** Run daily (`days: []`) or only on specific days (e.g. `SATURDAY`, `SUNDAY`).
- **Real-world intervals (`interval-tasks`):** Run cyclic tasks every `45s`, `20m`, `1h30m`, `2d`, etc., unaffected by server TPS drops.
- **Timezone support:** Configure any IANA timezone (e.g. `Europe/Rome`, `UTC`) or `SYSTEM`, with full Daylight Saving Time support.
- **Multiple commands per task:** Execute as many console commands as you want in sequence (with or without leading `/`).
- **Very low footprint:** Minimal RAM and negligible CPU usage (one lightweight check per second).

---

## Requirements

- **Server:** Paper or Purpur `26.3` (or compatible)
- **Java:** `25`

---

## Installation

1. Download `ScheduledCommands-1.0.0.jar` from **Releases** (or build it with `./gradlew build`).
2. Drop the `.jar` into your server's `plugins/` folder.
3. Start or restart the server to generate `plugins/ScheduledCommands/config.yml`.
4. Edit `config.yml` and run `/sched reload`.

---

## Commands & Permissions

Main command: `/scheduledcommands` (aliases: `/sched`, `/scmd`)

| Command | Description | Default Permission |
| :--- | :--- | :--- |
| `/sched list` | Show all enabled tasks, next run time, and countdown | `scheduledcommands.admin` |
| `/sched time` | Display the current date, time, and active timezone | `scheduledcommands.admin` |
| `/sched run <task>` | Immediately execute a task for testing | `scheduledcommands.admin` |
| `/sched reload` | Reload `config.yml` and recalculate all schedules | `scheduledcommands.admin` |
| `/sched help` | Show command help | `scheduledcommands.admin` |

*(The permission node can be customized in `config.yml` under `settings.permission`.)*

---

## Configuration Example (`config.yml`)

```yaml
settings:
  timezone: "Europe/Rome" # or "SYSTEM"
  permission: "scheduledcommands.admin"

scheduled-tasks:
  daily-save:
    enabled: true
    time: "04:00" # HH:mm or HH:mm:ss
    days: []      # [] = every day, or [SATURDAY, SUNDAY]
    commands:
      - "say [Auto] Daily world save in progress..."
      - "save-all"

interval-tasks:
  vote-reminder:
    enabled: true
    interval: "20m" # supports d, h, m, s (e.g. "45s", "20m", "1h30m")
    run-on-startup: false
    commands:
      - "say [Tip] Remember to vote for the server!"

---

## Documentation

📖 Read the full guide and ready-to-use recipes in the **[ScheduledCommands Wiki](https://github.com/sfbanjo/ScheduledCommands/wiki)**!