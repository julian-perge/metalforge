# Minecraft, NeoForge and Sodium lookups

Check every class, method, registry id and version against the `minecraft-modding` MCP server (`@adhisang/minecraft-modding-mcp`, run through `bunx`) before writing it. MetalForge implements Renderpearl's backend classes and mixes into Minecraft and Sodium, and a mixin target that does not exist fails only when the game loads. `.mcp.json` is gitignored, so add the server to your own client first.

The `target` decides whether a class is found:

| To read                                       | Target                                                                                             |
| --------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| Vanilla, including `com.mojang.renderpearl`   | `{kind: version, value: <minecraft_version>}`                                                      |
| Vanilla with NeoForge's patches, for `rg`     | unzip `build/moddev/artifacts/minecraft-patched-<neo_version>-sources.jar`                         |
| NeoForge source                               | `{kind: dependency, group: net.neoforged, name: neoforge, version: <neo_version>}`                 |
| NeoForge members, or a source read that fails | `{kind: jar, value: <neoforge-<version>-universal.jar>}`, found under `~/.gradle/caches/modules-2` |
| FML, such as `ModList`                        | `jar` target on `net.neoforged.fancymodloader:loader` at the version in the NeoForge pom           |
| Sodium                                        | `jar` target on `build/sodium-inner/*.jar`, because the Sodium jar from Modrinth only nests it     |
| Lootr and the LWJGL bindings                  | `jar` target on the jar under `~/.gradle/caches/modules-2`                                         |

- Pass explicit versions. `versionFromProject` reads `neoforge_version`, and `gradle.properties` only has `neo_version`.
- `workspace` targets miss NeoForge and FML classes, because the patched jars in `build/moddev/artifacts` do not contain them.
- Say what could not be verified when a lookup fails and the answer matters.
