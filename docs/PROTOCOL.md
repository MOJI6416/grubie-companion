# Launcher protocol

One TCP connection from the game to `127.0.0.1:<port>`, one JSON object per line, UTF-8. The launcher passes the address and a one-time token in `GRUBIE_COMPANION_ADDRESS` and `GRUBIE_COMPANION_TOKEN`; the statistics folder in `GRUBIE_COMPANION_STATS_DIR`. The mod reconnects with a back-off when the connection drops. Lines longer than 1 MiB are rejected by the mod, 64 KiB by the launcher.

Version 2. A launcher that answers `welcome` without `protocol` speaks version 1: the mod then shows only toasts, invites and the host panel.

## Game → launcher

| `t` | Fields | Meaning |
|---|---|---|
| `hello` | `protocol`, `token`, `mod`, `minecraft`, `loader`, `features[]` | First line. Nothing else is accepted before `welcome`. |
| `accept` / `decline` | `id` | Answer an invite (an actionable toast). |
| `kick` | `key` | Remove a guest from the world the player hosts. |
| `invite` | `req`, `friend`, `server?: {address, name}` | Invite a friend to the server the player is on, or (without `server`) to the world the player shares. |
| `join` | `req`, `friend` | Join the world or server a friend is playing on. |
| `share` | `req`, `action: "start"`, `port`, `visibility`, `world` | The mod opened the singleplayer world to the LAN on `port`; share it through Play Together. |
| `share` | `req`, `action: "stop"` | Stop sharing. |
| `voice` | `action`: `mute`, `unmute`, `deafen`, `undeafen` | Change the player's own voice state. |
| `ptt` | `down` | Push-to-talk key pressed or released while the game has focus. |
| `sendScreenshot` | `req`, `path`, `friend` | Send a screenshot the launcher announced to a friend. |
| `stats` | `kind` (`server`, `world`), `key`, `name`, `snapshot` | Statistics and advancement progress, every 45 s in a world and every 5 min on a server (plus on pause). `snapshot` has the same shape as the files in the statistics folder. |
| `stage` | `stage`, `detail?` | `init`, `resources`, `menu`, `joining`, `world`, `server`, `resumed` (after a freeze), `selftest` (CI only). |
| `hang` | `seconds`, `thread`, `frames[]`, `mods[]`, `stage` | The game thread has not ticked for 15 s (90 s during start-up). `mods` are the jar file names the stack runs through, top first. |

`req` is an id the mod chooses; the launcher answers it with `result`.

## Launcher → game

The launcher sends whole snapshots; the mod replaces what it had.

| `t` | Fields |
|---|---|
| `welcome` | `protocol`, `selfId`, `selfName` |
| `toasts` | `items[]`: `id`, `kind` (`message`, `invite`, `call`, `achievement`, `screenshot`, `system`), `title`, `body`, `meta?`, `count`, `actionable`, `avatar?`. A toast shows once per `id` and again when `count` grows. For `screenshot`, `id` is the file path. |
| `friends` | `online[]`: `id`, `name`, `status` (`online`, `away`, `busy`, `playing`), `activity?`, `avatar?`, `joinable`; `offline` (count) |
| `host` | `active`, `kind`, `title?`, `code?`, `visibility?`, `max?`, `guests[]`: `key`, `name`, `transport` (`direct`, `turn`, `relay`), `ping?`, `avatar?`; `invited[]` (names) |
| `voice` | `active`, `channel?`, `muted`, `deafened`, `ptt`, `pttKey?` (label), `pttCode?` (GLFW key code; on 26.3+, which uses SDL, the mod maps it through the vanilla key name), `members[]`: `id`, `name`, `speaking`, `muted`, `avatar?` |
| `avatar` | `id`, `png` (base64, at most 64 KiB). Sent once per id; `avatar` fields elsewhere refer to it. |
| `connect` | `address`, `name`, `kind?` (`world` for a friend's world, else `server`) — join it now. |
| `place` | `address`, `name`, `kind` — sent right after `welcome` when the launcher started the game straight into a friend's world or a server, so the panel names it instead of showing a relay or loopback address. |
| `result` | `req`, `ok`, `error?` (`offline`, `not_hosting`, `lan_not_found`, `busy`, `denied`, `friend_offline`, `rate_limited`, `not_found`, `unknown`) |
