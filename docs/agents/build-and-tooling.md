# Build and tooling

- The jar builds only on macOS with the Xcode Command Line Tools. `compileNative` compiles `libmetalforge.dylib` with clang, and the `jar` task fails when that library is missing.
- `./gradlew clean` also deletes `build/moddev`, which ModDevGradle downloads again, so the next build needs the network.
- `brew install just prek` installs the two tools. Run `prek install` once in a clone, and each commit then runs the hooks in `prek.toml` on the staged files. `just lint` runs them on every file.
- No Java formatter runs, because the ported Java is indented with tabs in some files and four spaces in others, and a formatter would rewrite most of it.

## Moving to a new Minecraft version

`main` follows the newest Minecraft version, and older versions live on protected version branches ([ADR 0002](../adr/0002-main-follows-the-newest-minecraft.md)). Finish each step before the next:

1. Land the work the old version still needs on `main`.
2. Cut `port-<new version>` from `main` and port in slices, checking each slice in a dev client.
3. Take the targets (NeoForge, LWJGL, Java, optional mods) from the NeoForge primers and changelog for every version crossed. When the newest [mcopt](https://github.com/noahdunnagan/mcopt) already targets the new version, diff its Metal backend against the `metal` package and `src/main/native/mcmetal.m` to find the new signatures, and keep `NOTICE` as is.
4. When the port runs, cut `<old version>` from `main` and push it. The fork's "Version branches" ruleset (pattern `refs/heads/[0-9]*.*`) blocks deleting and force-pushing it from the first push, and `gh api repos/julian-perge/metalforge/rules/branches/<old version> --jq '[.[].type]'` lists `deletion` and `non_fast_forward` when it applies. Then fast-forward `main` to the port branch, so history stays linear. If the ruleset is ever missing, this recreates it:

   ```
   gh api repos/julian-perge/metalforge/rulesets -X POST --input - <<'EOF'
   {"name":"Version branches","target":"branch","enforcement":"active",
    "conditions":{"ref_name":{"include":["refs/heads/[0-9]*.*"],"exclude":[]}},
    "rules":[{"type":"deletion"},{"type":"non_fast_forward"}]}
   EOF
   ```
5. Update the README (version, a link to the version branch, performance numbers labeled with the version they were measured on) and note the new branch on open issues.
