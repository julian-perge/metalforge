# Verifying changes

MetalForge has no unit tests or GameTests, so a change is checked by `just build` and by running the game. Every in-game run follows the rule in [AGENTS.md](../../AGENTS.md): ask the owner first.

## Structure Lab bridge

Structure Lab is a local mod that drives a dev client from a script, so a check needs no one at the keyboard. It is not published: build it from the Structure Lab repo's branch for the same Minecraft version, and add it to a dev client with `just client -PstructureLabJar=<path to jar>`.

The bridge writes `<game dir>/structurelab/connection.json` with a `url` and a `token`. POST a JSON body `{"op": "<op>", ...}` to the `url` with the header `Authorization: Bearer <token>`.

| Op             | Arguments                        | What it does                                                                                                                                                     |
| -------------- | -------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `client`       | none                             | Reports `in_world`, `paused`, the open `screen`, and the player's position and dimension.                                                                       |
| `new_world`    | `name`                           | Creates a creative world with a random seed and returns its `folder`. Fails while a world is open.                                                               |
| `command`      | `command`                        | Runs a command as the player and returns the messages it would print.                                                                                            |
| `aim`          | `from`, `at` (each `[x, y, z]`)  | Puts the player's eyes at `from`, looking at the center of block `at`, closes any open screen, and returns `yaw` and `pitch`.                                    |
| `screenshot`   | `name` (letters, digits, `_`, `-`) | Saves `screenshots/<name>.png` in the game dir, with the HUD and any open screen, and returns its `path`.                                                      |
| `chunk_loaded` | `position`, optional `dimension` | Reports whether that chunk is loaded.                                                                                                                            |
| `close_screen` | none                             | Closes the open screen.                                                                                                                                          |
| `quit`         | none                             | Stops the client.                                                                                                                                                |

- The bridge creates worlds but cannot open an existing one, so the owner opens a saved world.
- Wait for `client` to report `in_world` true before sending commands.
- The player is not frozen after `aim`, so build a floor under `from` (a barrier block works), or the player falls before the screenshot.
- `screenshot` calls vanilla's screenshot code on the main render target, so under Metal it exercises MetalForge's texture readback.
- A fresh `run/` opens the accessibility onboarding screen first, and `new_world` works from it.
- With a warm Gradle cache the bridge answered 15 seconds after launch, and a new world was playable 7 seconds after `new_world`. When the bridge is not up after a minute, read `run/logs/latest.log`.
- Confirm that Metal is active from the log line `MetalForge Metal backend`, which names the GPU.
- A covered window keeps rendering (the pacing log shows about 120 frames per second), but `[shown]` counts most of those frames as `untimed`. Hitch counts and other timing checks need the game window uncovered.
- Tabbing to the game window can fling the camera (a macOS mouse quirk, not MetalForge), so call `aim` right before each `screenshot` and leave the window alone during a run.
