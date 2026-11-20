# Grubie Companion

Client mod for Grubie Launcher. It shows friends' invites and messages as in-game toasts, lets the player accept an invite without leaving the game, gives the Play together host a panel with the players and a Kick button, and saves the player's statistics from multiplayer servers so the launcher can count achievements there.

Without the launcher the mod does nothing: it starts only when the game was launched with `GRUBIE_COMPANION_ADDRESS` and `GRUBIE_COMPANION_TOKEN` in the environment.

## Targets

| Project | Loader | Minecraft | Java |
|---|---|---|---|
| `targets/fabric-1.20.1` | Fabric | 1.20, 1.20.1 | 17 |
| `targets/fabric-1.21.1` | Fabric | 1.21, 1.21.1 | 21 |
| `targets/neoforge-1.21.1` | NeoForge | 1.21, 1.21.1 | 21 |
| `targets/fabric-26.3` | Fabric | 26.3 | 25 |
| `targets/neoforge-26.3` | NeoForge | 26.3 | 25 |

The Fabric jars do not need Fabric API.

## Layout

- `core/` — plain Java without Minecraft: the launcher protocol, the reconnecting socket, the state of toasts and the host panel, the statistics schedule and the snapshot file. Unit tests live here.
- `src/common/` — Minecraft code shared by every target: mixins, statistics reader, panel rows, translations.
- `src/mc/<version>/` — the small adapter for one Minecraft line: toasts, screens, joining a server, key mapping, the panel screen.
- `src/fabric/`, `src/neoforge/` — loader entry points and metadata.
- `targets/<loader>-<version>/` — one Gradle project per jar. `gradle.properties` there sets the Minecraft version, Java release, adapter and the versions the jar is offered for.

A new Minecraft line is a new `src/mc/<version>` adapter plus a target folder.

## Building

JDK 25 is needed to run Gradle; the Java 17/21 targets are compiled with `--release`.

```
./gradlew :core:test
./gradlew syncToLauncher
```

`syncToLauncher` builds every target and copies the jars with `manifest.json` into `../launcher/resources/companion`. The launcher picks the jar by loader and Minecraft version from that manifest, puts it into the instance `mods` folder at launch when the instance has "Add the companion mod" on, and removes it when the switch is off.

## Protocol

One TCP connection to `127.0.0.1`, one JSON object per line.

Game to launcher: `hello` (token, mod, minecraft, loader), `accept` (toast id), `kick` (guest key).

Launcher to game: `welcome`, `toasts` (current toasts), `host` (whether this instance is sharing and who is in), `connect` (address to join right now).

Server statistics go to `GRUBIE_COMPANION_STATS_DIR` as `<sha1(server, uuid)>.json`: server address, player, time, the statistics the server sent to the client and the advancement progress.
