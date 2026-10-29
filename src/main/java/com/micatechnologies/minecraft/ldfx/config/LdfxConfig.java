package com.micatechnologies.minecraft.ldfx.config;

import java.io.File;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;

/**
 * LDFX's settings, from {@code config/ldfx.cfg}.
 *
 * <p>Read once, the first time anything asks, which is usually the mixin plugin deciding which
 * patches to apply: that happens before mods are constructed, so this cannot use Forge's
 * annotation config. Every feature has an {@code enabled} switch; a switched-off feature's
 * mixins are not applied at all. Changing the file takes a restart.</p>
 */
public final class LdfxConfig {

    private static LdfxConfig instance;

    /** Spread the client's incoming packet work (chunks above all) over frames. */
    public final boolean smoothChunkLoading;
    /** Milliseconds per frame for applying server packets when there is no backlog. */
    public final double taskBudgetMs;
    /** The most the budget grows to when a backlog builds up. */
    public final double taskBudgetMaxMs;
    /** Tasks always run per frame, however slow they are, so the queue always drains. */
    public final int taskMinPerFrame;
    /** Rebuild only the sections a partial chunk packet changed. */
    public final boolean partialRebuilds;
    /** Update a chunk the server resends in place instead of leaking the old one. */
    public final boolean chunkResendFix;
    /** Remove unloaded entities from the world with a hash set instead of a list scan. */
    public final boolean fastEntityUnload;
    /** Per entity type: the farthest it is drawn, as "id=blocks". */
    public final String[] entityRenderDistances;
    /** Turn on Forge's alwaysSetupTerrainOffThread: no chunk meshes built on the main thread. */
    public final boolean terrainOffThread;
    /** Keep the blocks of chunks the server unloads, for drawing. */
    public final boolean chunkCache;
    /** Most chunks kept in the client chunk cache. */
    public final int chunkCacheMax;

    private LdfxConfig(Configuration c) {
        String cat = "smooth_chunk_loading";
        c.setCategoryComment(cat, "Client: apply the server's packets under a time budget per frame instead of all at once,\n"
                + "so a burst of incoming chunks no longer freezes a frame. Order is always kept.");
        smoothChunkLoading = c.getBoolean("enabled", cat, true, "Turn the feature on.");
        taskBudgetMs = c.getFloat("budget_ms", cat, 4f, 0.5f, 50f, "Milliseconds per frame with no backlog.");
        taskBudgetMaxMs = c.getFloat("max_budget_ms", cat, 12f, 0.5f, 100f, "Most milliseconds per frame while catching up on a backlog.");
        taskMinPerFrame = c.getInt("min_tasks_per_frame", cat, 8, 1, 10000, "Tasks run every frame regardless of the budget.");

        cat = "chunk_updates";
        c.setCategoryComment(cat, "Client: how chunks the server updates are applied.");
        partialRebuilds = c.getBoolean("partial_rebuilds", cat, true,
                "Rebuild only the 16-block sections a partial chunk update changed, instead of the whole column and its 8 neighbours.");
        chunkResendFix = c.getBoolean("resend_fix", cat, true,
                "When the server resends a chunk the client already has, update it in place instead of leaking the old chunk's tile entities and hiding its entities.");

        cat = "entities";
        c.setCategoryComment(cat, "Entity handling on the client and the server.");
        fastEntityUnload = c.getBoolean("fast_unload", cat, true,
                "Remove unloaded entities from the world in linear time (vanilla compares every loaded entity with every unloaded one).");
        entityRenderDistances = c.getStringList("render_distance", cat,
                new String[] {"minecraft:item_frame=64", "minecraft:armor_stand=64", "minecraft:painting=96"},
                "Client: the farthest each entity type is drawn, in blocks, as id=blocks. Empty the list to keep vanilla's distances.");

        terrainOffThread = c.getBoolean("terrain_off_thread", "chunk_updates", true,
                "Turn on Forge's alwaysSetupTerrainOffThread, so chunk sections near the player are rebuilt on the worker threads\n"
                + "instead of the main thread (vanilla builds them in the frame, which stutters when many change at once).");

        cat = "client_chunk_cache";
        c.setCategoryComment(cat, "Client: keep the blocks of chunks the server unloads, so flying back over an area does not rebuild it,\n"
                + "and areas you have seen stay visible out to your render distance even past the server's view distance.\n"
                + "Cached chunks are scenery: no entities, no ticking; the server's copy replaces them when it sends one.");
        chunkCache = c.getBoolean("enabled", cat, true, "Turn the feature on.");
        chunkCacheMax = c.getInt("max_chunks", cat, 4096, 64, 65536, "Most chunks kept.");
        if (c.hasChanged()) {
            c.save();
        }
    }

    public static synchronized LdfxConfig get() {
        if (instance == null) {
            File home = Launch.minecraftHome != null ? Launch.minecraftHome : new File(".");
            instance = new LdfxConfig(new Configuration(new File(home, "config/ldfx.cfg")));
        }
        return instance;
    }
}
