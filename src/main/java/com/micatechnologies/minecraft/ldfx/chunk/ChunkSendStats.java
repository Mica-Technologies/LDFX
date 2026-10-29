package com.micatechnologies.minecraft.ldfx.chunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Accumulates where the server's main thread spends time loading and sending chunks, for the
 * {@code debug.chunk_send_profiling} switch. No Minecraft types, so it is unit-tested.
 *
 * <p>Loading: {@code load} is the main-thread half of a chunk load from disk (the file read is
 * on Forge's chunk IO threads), split into {@code entities} (creating its tile entities and
 * entities from NBT), {@code data_event} (mods' {@code ChunkDataEvent.Load} handlers),
 * {@code on_load} (adding them to the world, with tile entities' {@code onLoad}, and mods'
 * {@code ChunkEvent.Load} handlers), {@code populate} and {@code callbacks} (whoever asked for
 * the chunk, such as the player chunk map). {@code generate} is generating a new chunk.
 * Creating tile entities and entities and tile entity {@code onLoad} are also counted per id.</p>
 *
 * <p>Sending: {@code build} (constructing a
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
    public static final String LOAD_ENTITIES = "entities";
    public static final String LOAD_DATA_EVENT = "data_event";
    public static final String LOAD_ON_LOAD = "on_load";
    public static final String LOAD_POPULATE = "populate";
    public static final String LOAD_CALLBACKS = "callbacks";
    public static final String GENERATE = "generate";
    public static final String TILE_CREATE = "tile_create";
    public static final String ENTITY_CREATE = "entity_create";
    public static final String TILE_ON_LOAD = "tile_on_load";
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
    private final Map<String, Map<String, Acc>> keyed = new HashMap<>();
    private long since;

    public ChunkSendStats(long now) {
        since = now;
    }

    public void add(String phase, long nanos) {
        phases.computeIfAbsent(phase, p -> new Acc()).add(nanos);
    }

    /** Adds to {@code phase} and to {@code key} within it, for the per-id rankings. */
    public void addKeyed(String phase, String key, long nanos) {
        add(phase, nanos);
        keyed.computeIfAbsent(phase, p -> new HashMap<>()).computeIfAbsent(key, k -> new Acc()).add(nanos);
    }

    public void addTileTag(String key, long nanos) {
        addKeyed(TILE_TAGS, key, nanos);
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

    /** Load time not accounted for by the named parts (structures, bookkeeping). */
    public long otherLoadNanos() {
        return Math.max(0, nanos(LOAD) - nanos(LOAD_ENTITIES) - nanos(LOAD_DATA_EVENT) - nanos(LOAD_ON_LOAD)
                - nanos(LOAD_POPULATE) - nanos(LOAD_CALLBACKS));
    }

    public List<String> topTileKeys(int n) {
        return topKeys(TILE_TAGS, n, "tags");
    }

    /** The {@code n} keys of {@code phase} that cost the most in total, as "key: ms over count (max)". */
    public List<String> topKeys(String phase, int n, String noun) {
        List<Map.Entry<String, Acc>> list = new ArrayList<>(keyed.getOrDefault(phase, new HashMap<>()).entrySet());
        list.sort((a, b) -> Long.compare(b.getValue().nanos, a.getValue().nanos));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(n, list.size()); i++) {
            Acc a = list.get(i).getValue();
            out.add(String.format(Locale.ROOT, "%s: %.1f ms over %d %s (max %.2f ms)",
                    list.get(i).getKey(), a.nanos / 1e6, a.count, noun, a.maxNanos / 1e6));
        }
        return out;
    }

    /** A multi-line report of everything since the last reset. */
    public List<String> report(long now) {
        double seconds = Math.max(1e-9, (now - since) / 1e9);
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ROOT, "Chunk loading and sending on the server thread over %.1f s:", seconds));
        for (String phase : new String[] {LOAD, GENERATE, SEND, BUILD, UPDATE}) {
            lines.add(phaseLine(phase, seconds));
        }
        if (count(LOAD) > 0) {
            lines.add(String.format(Locale.ROOT,
                    "  load split: entities %.1f ms (%d tile entities, %d entities), data event %.1f ms, on load %.1f ms, populate %.1f ms, callbacks %.1f ms, other %.1f ms",
                    nanos(LOAD_ENTITIES) / 1e6, count(TILE_CREATE), count(ENTITY_CREATE), nanos(LOAD_DATA_EVENT) / 1e6,
                    nanos(LOAD_ON_LOAD) / 1e6, nanos(LOAD_POPULATE) / 1e6, nanos(LOAD_CALLBACKS) / 1e6, otherLoadNanos() / 1e6));
        }
        addTop(lines, "costliest tile entities to create", topKeys(TILE_CREATE, 6, "created"));
        addTop(lines, "costliest entities to create", topKeys(ENTITY_CREATE, 6, "created"));
        addTop(lines, "costliest tile entity onLoad", topKeys(TILE_ON_LOAD, 6, "calls"));
        if (count(BUILD) > 0) {
            lines.add(String.format(Locale.ROOT, "  build split: blocks %.1f ms, tile entity tags %.1f ms (%d tags), other %.1f ms",
                    nanos(BLOCKS) / 1e6, nanos(TILE_TAGS) / 1e6, count(TILE_TAGS), otherBuildNanos() / 1e6));
        }
        addTop(lines, "costliest tile entity tags", topTileKeys(6));
        return lines;
    }

    private String phaseLine(String phase, double seconds) {
        long c = count(phase);
        if (c == 0) {
            return String.format(Locale.ROOT, "  %-8s none", phase);
        }
        return String.format(Locale.ROOT, "  %-8s %6d x  %8.1f ms total  %6.3f ms avg  %7.2f ms max  %5.2f ms per second",
                phase, c, nanos(phase) / 1e6, nanos(phase) / 1e6 / c, maxNanos(phase) / 1e6, nanos(phase) / 1e6 / seconds);
    }

    private static void addTop(List<String> lines, String title, List<String> top) {
        if (!top.isEmpty()) {
            lines.add("  " + title + ":");
            for (String t : top) {
                lines.add("    " + t);
            }
        }
    }

    public void reset(long now) {
        phases.clear();
        keyed.clear();
        since = now;
    }
}
