# Local shortcuts for building and running MetalForge. `just` lists them, and `just <recipe>` runs one.
#
# CI does not call just. The workflow in .github/workflows runs Gradle directly.

set shell := ["bash", "-euo", "pipefail", "-c"]

# Recipe arguments arrive as $1, $2 and so on, so variadic recipes forward them with "$@" and keep each argument intact.
set positional-arguments

[private]
default:
    @just --list --unsorted

[doc('Compile the Java and the native Metal library, and build the jar into build/libs/')]
[group('gradle')]
build:
    ./gradlew build

[doc('Start a dev client capped at 2 GB. It opens a game window, so ask the owner first')]
[group('gradle')]
client *flags:
    JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:--Xmx2G}" ./gradlew runClient "$@"

# The first run updates gradle-wrapper.properties with the old Gradle, and the second run lets the new Gradle rewrite gradlew and gradle-wrapper.jar to match it.
[doc('Upgrade the Gradle wrapper, for example: just gradle-update 9.8.2')]
[group('gradle')]
gradle-update version:
    ./gradlew wrapper --gradle-version "$1"
    ./gradlew wrapper --gradle-version "$1"
