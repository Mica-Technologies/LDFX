# LDFX: Limitless Development: Fixes & Optimizations

Bug fixes and performance improvements for Minecraft 1.12.2 (Forge), aimed at what the other
1.12.2 optimization mods leave alone:

- **Smoother chunk loading:** the work of applying incoming chunks is spread over frames instead
  of landing in one, so flying over a busy area stops stuttering.
- **Fewer wasted chunk rebuilds:** only the parts of a chunk that actually changed are rebuilt.
- **Cheaper entities and tile entities:** faster removal, and sensible render distances for
  decorative entities.
- **A client chunk cache:** chunks you just left are kept for drawing, so coming back does not
  rebuild them.

Works with or without OptiFine. Install it on the client, the server, or both; each side helps on
its own. Every feature can be switched off in the config.

Status: early development.

## Requirements

- Minecraft 1.12.2, Forge 14.23.5.2847 or newer
- [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter)

## Licence

LGPL-2.1. See [LICENSE](LICENSE).
