# MineStorm BedWars

BedWars minigame for the **MineStorm** server (aqua `&b` theme), based on
[BedWars1058](https://github.com/andrei1058/BedWars1058) by andrei1058 (GPL-3.0).

* Minecraft: **Spigot / Paper 1.8.8 – 1.8.9** (`v1_8_R3`)
* Java: **JDK 17**
* Plugin name: `MineStormBedWars` (data folder: `plugins/MineStormBedWars`)

## What's new
| Feature | Details |
|---|---|
| MineStorm branding | Console banner, chat prefix `&b&lMineStorm &8»`, animated aqua scoreboard title, `{poweredBy}` = MineStorm |
| BedFight party hook | If **BedFight** is installed, BedWars uses BedFight's `/party` system (invite, join, leader...). BedWars' own `/party` is not registered so BedFight keeps it. |
| `/bw private [group\|arena]` | Party leader starts a **private match**: a free arena is locked to the party, only party members can join, members are split across teams so they fight each other, countdown starts automatically. |

All original commands and features are unchanged.

### Permissions
| Permission | Default | Description |
|---|---|---|
| `bw.private` | everyone | use `/bw private` |
| `bw.private.bypass` | op | join any private match |

## Build
```bash
mvn -B clean package
```
Jar: `bedwars-plugin/target/MineStorm-BedWars-<version>.jar`

GitHub Actions (`.github/workflows/build.yml`) builds on every push/PR and uploads the jar as an artifact.
Push a tag like `v1.0.0` to publish a GitHub Release with the jar attached.

## Setup notes
* Put `BedFight.jar` and `MineStorm-BedWars.jar` in `plugins/`.
* Change `server-ip` in `plugins/MineStormBedWars/config.yml` (default `play.minestorm.net`).
* Coming from BedWars1058? Move your `plugins/BedWars1058` folder to `plugins/MineStormBedWars`
  and delete the `Languages/messages_*.yml` files so the new MineStorm texts are generated.
