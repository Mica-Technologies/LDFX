package com.micatechnologies.minecraft.ldfx.pauser;

import com.micatechnologies.minecraft.ldfx.Ldfx;
import com.micatechnologies.minecraft.ldfx.config.LdfxConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;

/**
 * Decides once per server tick whether the server pauser freezes the server, and every pauser
 * mixin reads that one answer for the rest of the tick. Taken over from Server Utility Mod (SUM),
 * whose pauser this replaces.
 *
 * <p>Two levels:
 * <ul>
 *   <li><b>Deep pause</b>: nobody has been online for {@code empty_grace_seconds} (30 by default,
 *       so mods can finish what the last logout started). {@code MixinMinecraftServerPauser} skips the world loop,
 *       the {@code ServerTickEvent} posts and autosaves, leaving networking and scheduled tasks so the
 *       next login still gets in. The world is saved once as the pause begins. Mods that tick from
 *       tick events (Immersive Vehicles, IC2, FAWE's queue) stop too. With {@code pause_dynmap},
 *       Dynmap's renders and zoom-out processing pause as well ({@link DynmapPause}).</li>
 *   <li><b>Soft pause</b>: every online player is AFK and {@code pause_when_all_afk} is on. The worlds
 *       and the entity tracker freeze, but tick events keep firing for the connected players.</li>
 * </ul>
 *
 * <p>Steps aside while SUM's own pauser is enabled, so the two never run together. Dedicated
 * servers only: a singleplayer world is never empty while it is open. Server thread only.</p>
 */
public final class ServerPauser {

    private static boolean paused;
    private static boolean deepPaused;
    private static boolean loggedPaused;
    private static Boolean sumPauserActive;
    private static int deepTicks;
    private static final EmptyGrace EMPTY_GRACE = new EmptyGrace();
    private static boolean loggedGrace;

    private ServerPauser() {
    }

    /**
     * Makes this tick's decision. Called at the start of {@code MinecraftServer.tick()}.
     *
     * @return true when this tick is the first of a deep pause, so the caller saves once
     */
    public static boolean beginTick(MinecraftServer server) {
        boolean wasDeep = deepPaused;
        LdfxConfig c = LdfxConfig.get();
        paused = false;
        deepPaused = false;
        if (!c.pauserEnabled || !(server instanceof DedicatedServer) || server.getPlayerList() == null || sumPauserActive()) {
            endDynmapPause(wasDeep);
            return false;
        }

        int players = server.getPlayerList().getCurrentPlayerCount();
        long graceNanos = c.pauserEmptyGraceSeconds * 1_000_000_000L;
        long now = System.nanoTime();
        boolean emptyLongEnough = EMPTY_GRACE.update(players, graceNanos, now);
        boolean inGrace = EMPTY_GRACE.waiting(graceNanos, now);
        if (inGrace && !loggedGrace && !wasDeep) {
            Ldfx.LOGGER.info("LDFX pauser: nobody online; pausing in {} s unless someone joins", c.pauserEmptyGraceSeconds);
        }
        loggedGrace = inGrace;
        String reason = null;
        if (players <= 0 && emptyLongEnough) {
            reason = "0 players online";
            deepPaused = true;
        } else if (players > 0 && c.pauserWhenAllAfk && AfkTracker.allAfk(server, c.pauserAfkSeconds)) {
            reason = "all " + players + " online player(s) AFK";
        }
        paused = reason != null;

        if (paused != loggedPaused) {
            loggedPaused = paused;
            if (paused) {
                Ldfx.LOGGER.info("LDFX pauser: world frozen ({}{})", reason, deepPaused ? ", tick events suspended" : "");
            } else {
                Ldfx.LOGGER.info("LDFX pauser: world resumed ({} player(s) online)", players);
            }
        }

        if (deepPaused) {
            if (!wasDeep) {
                McmcpPauseNotice.declare("LDFX pauser");
            }
            if (c.pauserDynmap && (!wasDeep || ++deepTicks % 100 == 0)) {
                // Re-applied every 5 s in case Dynmap recomputes its TPS pause on its own.
                DynmapPause.pause();
            }
        } else {
            endDynmapPause(wasDeep);
        }
        return deepPaused && !wasDeep;
    }

    private static void endDynmapPause(boolean wasDeep) {
        if (wasDeep) {
            deepTicks = 0;
            McmcpPauseNotice.declare(null);
            if (LdfxConfig.get().pauserDynmap) {
                DynmapPause.resume();
            }
        }
    }

    /** True when the worlds are frozen this tick, at either level. */
    public static boolean isPaused() {
        return paused;
    }

    /** True when nobody is online and the whole server tick, tick events included, is suspended. */
    public static boolean isDeepPaused() {
        return deepPaused;
    }

    /** SUM's pauser, when SUM is installed with its pauser enabled; checked once, after configs load. */
    private static boolean sumPauserActive() {
        if (sumPauserActive == null) {
            boolean active = false;
            try {
                Class<?> sum = Class.forName("com.micatechnologies.minecraft.sum.SumConfig");
                active = (Boolean) sum.getMethod("isPauserEnabled").invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                // SUM not installed, or a SUM without its own pauser.
            } catch (ReflectiveOperationException | RuntimeException e) {
                Ldfx.LOGGER.warn("LDFX pauser: could not read SUM's pauser setting; assuming it is off", e);
            }
            sumPauserActive = active;
            if (active) {
                Ldfx.LOGGER.info("LDFX pauser: Server Utility Mod's pauser is enabled, so LDFX's stays off");
            }
        }
        return sumPauserActive;
    }
}
