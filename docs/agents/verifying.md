# Verifying changes

MetalForge has no unit tests or GameTests, so a change is checked by `just build` and by running the game. Every in-game run follows the rule in [AGENTS.md](../../AGENTS.md): ask the owner first.

## Structure Lab bridge

Structure Lab is a local mod that drives a dev client from a script, so a check needs no one at the keyboard. It is not published: build it from the Structure Lab repo's branch for the same Minecraft version, and add it to a dev client with `just client -PstructureLabJar=<path to jar>`.

The bridge writes `<game dir>/structurelab/connection.json` with a `url` and a `token`. POST a JSON body `{"op": "<op>", ...}` to the `url` with the header `Authorization: Bearer <token>`.

| Op             | Arguments                        | What it does                                                                                                                                                     |
| -------------- | -------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `client`       | none                             | Reports `in_world`, `paused`, the open `screen`, and the player's position and dimension.                                                                       |
| `new_world`    | `name`, optional `seed`          | Creates a creative world with `seed` (a whole number) or a random seed, and returns its `folder` and `seed`. Fails while a world is open.                        |
| `command`      | `command`                        | Runs a command as the player and returns the messages it would print.                                                                                            |
| `aim`          | `from`, `at` (each `[x, y, z]`)  | Puts the player's eyes at `from`, looking at the center of block `at`, closes any open screen, and returns `yaw` and `pitch`.                                    |
| `screenshot`   | `name` (letters, digits, `_`, `-`) | Saves `screenshots/<name>.png` in the game dir, with the HUD and any open screen, and returns its `path`.                                                      |
| `reload`       | none                             | Reloads resource packs like F3+T, waits until the loading overlay has closed, and returns `ok` and `elapsed_ms`. Fails after 90 seconds.                         |
| `chunk_loaded` | `position`, optional `dimension` | Reports whether that chunk is loaded.                                                                                                                            |
| `close_screen` | none                             | Closes the open screen.                                                                                                                                          |
| `quit`         | none                             | Stops the client.                                                                                                                                                |

- The bridge creates worlds but cannot open an existing one, so the owner opens a saved world.
- Wait for `client` to report `in_world` true before sending commands.
- The player is not frozen after `aim`, so build a floor under `from` (a barrier block works), or the player falls before the screenshot.
- `screenshot` calls vanilla's screenshot code on the main render target, so under Metal it exercises MetalForge's texture readback. It never sees the window: an OpenGL window that stayed black still gave normal screenshots, so when a change touches presenting to the window, ask the owner to watch the window during the run.
- Give both runs of an A/B screenshot comparison the same `seed` in `new_world`, so they render the same terrain. No op leaves a world, so each world needs its own game launch. A fixed seed keeps the terrain but not the exact spawn, which varies by a few blocks, so `aim` from fixed positions.
- `reload` replaces an F3+T keypress, so a harness needs no macOS Accessibility permission. Its `elapsed_ms` includes the overlay's two-second fade-out. Minecraft never finishes a failed reload, so `reload` reports one as the 90-second timeout, and the cause is in `run/logs/latest.log`.
- Run `tick freeze` before placing any block, or water and other ticking blocks change between two A/B runs. Set the player's gravity with `attribute @s minecraft:gravity base set 0`, because no op turns on creative flight and the player otherwise falls between shots.
- A fresh `run/` opens the accessibility onboarding screen first, and `new_world` works from it. `close_screen` does not save the choice, so the screen and its spoken narrator prompt come back on every launch until `onboardAccessibility:false` is set in `run/options.txt`. Wait about 10 seconds after the bridge comes up before `new_world`: once, a `new_world` sent during the first resource reload failed with "Failed to load datapacks" and the world never opened.
- With a warm Gradle cache the bridge answered 15 seconds after launch, and a new world was playable 7 seconds after `new_world`. When the bridge is not up after a minute, read `run/logs/latest.log`.
- Confirm that Metal is active from the log line `MetalForge Metal backend`, which names the GPU.
- Sodium loads in the dev client (it is on `localRuntime`), so the Sodium integrations run in every dev launch. Lootr and ToroHealth do not, so `lootr_far` and `toro_light` only apply in a modpack instance.
- The log does not say which mixins applied. Launch with `-Dmixin.debug.export=true` and the transformed classes land in `run/.mixin.out/class/`, where `javap -p` lists MetalForge's handlers by their `metalforge$` names.
- A covered window keeps rendering (the pacing log shows about 120 frames per second), but `[shown]` counts most of those frames as `untimed`. Hitch counts and other timing checks need the game window uncovered.
- Before a timing run, set `inactivityFpsLimit:"minimized"` in the instance's `options.txt` while the game is closed. With `"afk"`, Minecraft caps the game at 30 frames per second after 60 seconds without keyboard or mouse input and at 10 after 10 minutes, and bridge ops and datapack commands do not count as input. The frame log then shows a p50 of exactly 33.4 ms or 100 ms.
- Tabbing to the game window can fling the camera (a macOS mouse quirk, not MetalForge), so call `aim` right before each `screenshot` and leave the window alone during a run.
