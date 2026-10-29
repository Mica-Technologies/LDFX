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
    /** Mobs far from players decide on AI goals less often. */
    public final boolean aiThrottle;
    public final int aiNearBlocks;
    public final int aiFarBlocks;
    public final int aiMidRate;
    public final int aiFarRate;
    /** Spigot-style entity activation range (off by default). */
    public final boolean activationRange;
    public final int activationMonsterBlocks;
    public final int activationOtherBlocks;
    public final int activationWakeTicks;
    public final String[] activationExempt;
    /** Freeze the server while nobody is online (taken over from Server Utility Mod). */
    public final boolean pauserEnabled;
    public final boolean pauserWhenAllAfk;
    public final int pauserAfkSeconds;
    public final boolean pauserDynmap;
    /** Idle hoppers back off instead of retrying every tick. */
    public final boolean hopperIdle;
    public final int hopperIdleTicks;
    /** Lower the frame rate while idle or in the background (FPS Reducer's feature set). */
    public final boolean fpsReducer;
    public final double fpsIdleSeconds;
    public final int fpsIdleFps;
    public final int fpsBackgroundFps;
    /** "off", "riding_or_flying" or "always": which player movement counts as activity. */
    public final String fpsMovement;
    public final boolean fpsHeldMouseCounts;
    /** Master volume multiplier while the window is in the background; 1 leaves it alone. */
    public final double fpsBackgroundVolume;
    public final boolean fpsHud;
    public final boolean fpsHudCpu;
    public final String fpsHudCorner;
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

        cat = "server_mobs";
        c.setCategoryComment(cat, "Server (and singleplayer): cheaper mob AI and ticking far from players.");
        aiThrottle = c.getBoolean("distance_scaled_ai", cat, true,
                "Mobs far from every player decide on new AI goals less often (vanilla: every 3 ticks). Goals already running\n"
                + "still update every tick.");
        aiNearBlocks = c.getInt("ai_near_blocks", cat, 32, 8, 256, "Within this distance of a player, mobs keep vanilla's 3-tick interval.");
        aiFarBlocks = c.getInt("ai_far_blocks", cat, 64, 16, 512, "Beyond this distance, mobs use ai_far_interval.");
        aiMidRate = c.getInt("ai_mid_interval", cat, 6, 3, 40, "Ticks between goal decisions between ai_near_blocks and ai_far_blocks.");
        aiFarRate = c.getInt("ai_far_interval", cat, 12, 3, 100, "Ticks between goal decisions beyond ai_far_blocks.");
        activationRange = c.getBoolean("activation_range", cat, false,
                "Spigot-style activation range: mobs beyond the range from every player skip most ticks (they still age and\n"
                + "get a full tick every wake_ticks). Mobs that are fighting, hurt, burning, ridden, leashed, named or swimming\n"
                + "always tick. Off by default: far mobs visibly pause.");
        activationMonsterBlocks = c.getInt("activation_monster_blocks", cat, 32, 8, 256, "Range for hostile mobs.");
        activationOtherBlocks = c.getInt("activation_other_blocks", cat, 24, 8, 256, "Range for every other mob (animals, villagers, NPCs).");
        activationWakeTicks = c.getInt("activation_wake_ticks", cat, 20, 1, 200, "An inactive mob still gets one full tick this often.");
        activationExempt = c.getStringList("activation_exempt", cat, new String[0],
                "Entity ids that always tick, for example customnpcs:customnpc.");

        cat = "server_pauser";
        c.setCategoryComment(cat, "Dedicated servers: freeze the server while nobody is online, so an empty server costs almost nothing.\n"
                + "World loop, tick events (which many mods tick from) and autosaves stop; logins still work and the first player\n"
                + "to join resumes everything. The world is saved once as the pause begins. Stays off while Server Utility Mod's\n"
                + "own pauser is enabled.");
        pauserEnabled = c.getBoolean("enabled", cat, true, "Turn the feature on.");
        pauserWhenAllAfk = c.getBoolean("pause_when_all_afk", cat, false,
                "Also freeze the worlds (not tick events) while every online player is AFK. Off by default so AFK farms keep running.");
        pauserAfkSeconds = c.getInt("afk_seconds", cat, 300, 10, 86400, "Seconds without moving, looking around, chatting or a command before a player counts as AFK.");
        pauserDynmap = c.getBoolean("pause_dynmap", cat, false,
                "Also pause Dynmap's renders and zoom-out processing while nobody is online (it resumes at the first login).\n"
                + "A Dynmap with a backlog of tiles otherwise keeps an empty server busy.");

        cat = "hoppers";
        c.setCategoryComment(cat, "Server (and singleplayer): hopper performance.");
        hopperIdle = c.getBoolean("idle_backoff", cat, true,
                "A hopper that found nothing to move waits idle_backoff_ticks before looking again (vanilla looks every tick).\n"
                + "Hoppers that are moving items keep vanilla's timing.");
        hopperIdleTicks = c.getInt("idle_backoff_ticks", cat, 4, 1, 8, "Ticks an idle hopper waits.");

        cat = "fps_reducer";
        c.setCategoryComment(cat, "Client: lower the frame rate while you are away or the game is in the background, and the volume while\n"
                + "it is in the background (the feature set of FPS Reducer, which LDFX replaces). Your own options are never changed.\n"
                + "Steps aside if LDOG or FPS Reducer is installed. '/ldfx fps on|off|reduce|status' controls it for the session.");
        fpsReducer = c.getBoolean("enabled", cat, true, "Turn the feature on.");
        fpsIdleSeconds = c.getFloat("idle_seconds", cat, 300f, 0f, 3600f, "Seconds without input before the frame rate drops; 0 never drops it for idling.");
        fpsIdleFps = c.getInt("idle_fps", cat, 10, 1, 120, "Frame rate while idle, or after the 'Reduce FPS now' key.");
        fpsBackgroundFps = c.getInt("background_fps", cat, 10, 0, 120, "Frame rate while the window is in the background; 0 leaves it alone.");
        fpsMovement = c.getString("movement_counts", cat, "riding_or_flying",
                "Which player movement counts as activity: off, riding_or_flying (so a long flight or ride does not idle), or always.",
                new String[] {"off", "riding_or_flying", "always"});
        fpsHeldMouseCounts = c.getBoolean("held_mouse_button_counts", cat, true, "A mouse button held down counts as activity.");
        fpsBackgroundVolume = c.getFloat("background_volume", cat, 0.2f, 0f, 1f,
                "Master volume multiplier while the window is in the background; 1 leaves it alone, 0 mutes.");
        fpsHud = c.getBoolean("hud", cat, false, "Show the frame rate on screen.");
        fpsHudCpu = c.getBoolean("hud_cpu", cat, false, "Also show the game's CPU use.");
        fpsHudCorner = c.getString("hud_corner", cat, "top_left", "Where the HUD sits.",
                new String[] {"top_left", "top_right", "bottom_left", "bottom_right"});
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
