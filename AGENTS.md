# MetalForge

NeoForge 26.1.2 mod that replaces Minecraft's OpenGL renderer with a native Metal renderer on macOS.

## Every task

- Use placeholders such as `<path to jar>` instead of local paths and personal details in commits, issues and comments, because the repo is public.
- Remove Iris from every instance used for a Metal run, because with Iris installed MetalForge uses the OpenGL fallback ([ADR 0001](docs/adr/0001-iris-forces-opengl-fallback.md)).
- Before any in-game run, ask the owner and wait for a yes. The request says what will run, how long it takes, whether the game window must be in front or the keyboard free, how you will stop it and where the output lands. A run is one game launch through quit plus whatever drives it (a sampler, a bridge script, key events), and one yes covers exactly what the request said. The owner has other sessions that need window focus and access.

## Read the doc that matches the task

- [Build and tooling](docs/agents/build-and-tooling.md): before changing the Gradle setup, the native build, CI or hooks, or before moving the mod to a new Minecraft version.
- [Verifying changes](docs/agents/verifying.md): before checking a change in the game, or before driving a dev client through the Structure Lab bridge.

## Agent skills

### Issue tracker

Issues live in GitHub Issues for julian-perge/metalforge, via the `gh` CLI. See [Issue tracker](docs/agents/issue-tracker.md).

### Triage labels

Use the five default triage labels (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See [Triage labels](docs/agents/triage-labels.md).

### Domain docs

Single-context: root `GLOSSARY.md` and `docs/adr/`. See [Domain docs](docs/agents/domain.md).
