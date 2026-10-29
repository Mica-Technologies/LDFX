# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

LDFX (Limitless Development: Fixes & Optimizations) is a Forge 1.12.2 mod of bug fixes and
performance improvements, for any pack, on a client, a dedicated server, or both. It targets what
the maintained 1.12.2 optimization mods (Universal Tweaks, LoliASM/Chibi, VintageFix, Alfheim) do
not cover, chiefly chunk loading and the client's handling of server data, and it absorbs the
feature sets of unmaintained mods (FPS Reducer, the 1.12.2 Performant) so packs can drop them.
The Alto modpack is the flagship test pack.

Rules that shape every feature:
- **Works with and without OptiFine.** OptiFine replaces whole classes (`RenderGlobal`,
  `ChunkRenderDispatcher`, `RenderChunk`, `EntityRenderer`, `BufferBuilder`, ...). Patches there
  belong in LDOG (the OptiFine replacement), not here. LDFX targets classes OptiFine leaves alone
  (`Minecraft`, `World`, `Chunk`, `ChunkCache`, `NetHandlerPlayClient`, `ChunkProviderClient`, ...).
- **Every feature has a config switch**, and a feature whose job another installed mod already
  does steps aside (log it once) instead of fighting it.
- **Licences:** LDFX is LGPL-2.1. Code may be adapted from LGPL-2.1 or MIT/Apache/CC0 mods with
  attribution. Universal Tweaks and VintageFix are LGPL-3.0 and closed-source mods have no
  licence: reimplement from behaviour, never copy.

## Commands

There is no `java` on PATH; prefix every Gradle call with a JDK from `~/.jdks`:

```sh
# Build (runs unit tests and the reobfJar manifest guard in addon.gradle)
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew build

# Dev client / server (logs: run/logs/latest.log)
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew runClient
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew runServer
```

Never build while a `runClient`/`runServer` is live: it corrupts the running classpath. Stop the
dev game first (the `java.exe` whose command line contains both `LDFX` and `GradleStart`).

## Architecture

- `mixin.LdfxCoreMod` registers `mixins.ldfx.json` through MixinBooter's `IEarlyMixinLoader`
  (never through the jar manifest; see its javadoc). The config is hand-maintained: client-only
  mixins in `"client"`, dedicated-server-only in `"server"`, the rest in `"mixins"`.
- Keep budgeting and data-structure logic free of Minecraft types where possible so JUnit can
  test it.

Plans and decisions live in `docs/AGENT-PLANS/MASTER_PLAN.md` (gitignored by design).

## Conventions & gotchas

- `buildscript.properties` keeps `modVersion =` empty: CI pins it to the release tag.
- Mixin targets and accessors must land in the refmap; check the built jar's refmap before a
  release. A wrong descriptor kills the whole mixin class silently.
- Verify in a dev client and in the Alto pack (with OptiFine) before releasing anything that
  touches rendering or chunk handling.
- Third-party reference source goes in `example-source/` (gitignored), never into `src/`.
- CI workflows come from `dev-configurations/minecraft-mod-workflows/template/`.
