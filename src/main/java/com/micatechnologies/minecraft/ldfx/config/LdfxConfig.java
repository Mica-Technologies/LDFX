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

    private LdfxConfig(Configuration c) {
        String cat = "smooth_chunk_loading";
        c.setCategoryComment(cat, "Client: apply the server's packets under a time budget per frame instead of all at once,\n"
                + "so a burst of incoming chunks no longer freezes a frame. Order is always kept.");
        smoothChunkLoading = c.getBoolean("enabled", cat, true, "Turn the feature on.");
        taskBudgetMs = c.getFloat("budget_ms", cat, 4f, 0.5f, 50f, "Milliseconds per frame with no backlog.");
        taskBudgetMaxMs = c.getFloat("max_budget_ms", cat, 12f, 0.5f, 100f, "Most milliseconds per frame while catching up on a backlog.");
        taskMinPerFrame = c.getInt("min_tasks_per_frame", cat, 8, 1, 10000, "Tasks run every frame regardless of the budget.");
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
