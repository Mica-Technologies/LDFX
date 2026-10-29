package com.micatechnologies.minecraft.ldfx.chunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Accumulates where the server's main thread spends time sending chunks, for the
 * {@code debug.chunk_send_profiling} switch. No Minecraft types, so it is unit-tested.
 *
 * <p>Phases: {@code load} (finding or loading a chunk for players), {@code build} (constructing a
 * chunk packet, split into {@code blocks}, {@code tile_tags} and the rest, which includes any mod
 * work hooked into the constructor such as Alfheim's light flush), {@code send} (sending a new
 * chunk to its players, which includes {@code build}) and {@code update} (sending block changes
 * in already-sent chunks: partial chunk packets, block changes and tile entity update packets).
 * Tile entity tags are also counted per key (block id or class) to name expensive mods.</p>
 *
 * <p>Not thread-safe: chunk sending happens on the server thread.</p>
 */
public final class ChunkSendStats {

    public static final String LOAD = "load";
    public static final String BUILD = "build";
    public static final String BLOCKS = "blocks";
    public static final String TILE_TAGS = "tile_tags";
    public static final String SEND = "send";
    public static final String UPDATE = "update";

    private static final class Acc {
        long count;
        long nanos;
        long maxNanos;

        void add(long n) {
            count++;
            nanos += n;
            if (n > maxNanos) {
                maxNanos = n;
            }
        }
    }

    private final Map<String, Acc> phases = new HashMap<>();
    private final Map<String, Acc> tileKeys = new HashMap<>();
    private long since;

    public ChunkSendStats(long now) {
        since = now;
    }

    public void add(String phase, long nanos) {
        phases.computeIfAbsent(phase, p -> new Acc()).add(nanos);
    }

    public void addTileTag(String key, long nanos) {
        add(TILE_TAGS, nanos);
        tileKeys.computeIfAbsent(key, k -> new Acc()).add(nanos);
    }

    public long count(String phase) {
        Acc a = phases.get(phase);
        return a == null ? 0 : a.count;
    }

    public long nanos(String phase) {
        Acc a = phases.get(phase);
        return a == null ? 0 : a.nanos;
    }

    public long maxNanos(String phase) {
        Acc a = phases.get(phase);
        return a == null ? 0 : a.maxNanos;
    }

    /** Build time not accounted for by block data or tile entity tags. */
    public long otherBuildNanos() {
        return Math.max(0, nanos(BUILD) - nanos(BLOCKS) - nanos(TILE_TAGS));
    }

    /** The {@code n} tile entity keys that cost the most in total, as "key: ms total (count)". */
    public List<String> topTileKeys(int n) {
        List<Map.Entry<String, Acc>> list = new ArrayList<>(tileKeys.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue().nanos, a.getValue().nanos));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(n, list.size()); i++) {
            Acc a = list.get(i).getValue();
            out.add(String.format(Locale.ROOT, "%s: %.1f ms over %d tags (max %.2f ms)",
                    list.get(i).getKey(), a.nanos / 1e6, a.count, a.maxNanos / 1e6));
        }
        return out;
    }

    /** A multi-line report of everything since the last reset. */
    public List<String> report(long now) {
        double seconds = Math.max(1e-9, (now - since) / 1e9);
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ROOT, "Chunk sending on the server thread over %.1f s:", seconds));
        for (String phase : new String[] {LOAD, SEND, BUILD, UPDATE}) {
            long c = count(phase);
            if (c == 0) {
                lines.add(String.format(Locale.ROOT, "  %-7s none", phase));
                continue;
            }
            lines.add(String.format(Locale.ROOT, "  %-7s %6d x  %8.1f ms total  %6.3f ms avg  %7.2f ms max  %5.2f ms per second",
                    phase, c, nanos(phase) / 1e6, nanos(phase) / 1e6 / c, maxNanos(phase) / 1e6, nanos(phase) / 1e6 / seconds));
        }
        if (count(BUILD) > 0) {
            lines.add(String.format(Locale.ROOT, "  build split: blocks %.1f ms, tile entity tags %.1f ms (%d tags), other %.1f ms",
                    nanos(BLOCKS) / 1e6, nanos(TILE_TAGS) / 1e6, count(TILE_TAGS), otherBuildNanos() / 1e6));
        }
        List<String> top = topTileKeys(8);
        if (!top.isEmpty()) {
            lines.add("  costliest tile entity tags:");
            for (String t : top) {
                lines.add("    " + t);
            }
        }
        return lines;
    }

    public void reset(long now) {
        phases.clear();
        tileKeys.clear();
        since = now;
    }
}
