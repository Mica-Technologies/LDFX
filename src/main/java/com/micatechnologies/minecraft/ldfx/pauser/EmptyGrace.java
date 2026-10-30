package com.micatechnologies.minecraft.ldfx.pauser;

/**
 * The wait between the server becoming empty and the deep pause, so mods can finish what a
 * logout starts (Dynmap marking the player gone, saves of player data, a last sync) while the
 * server still ticks. The clock starts when the player count reaches zero, including at startup,
 * and resets as soon as anyone is online. No Minecraft types, so it is unit-tested.
 */
public final class EmptyGrace {

    private long emptySince = -1;

    /**
     * @param players  players online this tick
     * @param graceNanos how long the server must stay empty first; 0 pauses at once
     * @param now  {@link System#nanoTime()}
     * @return true once the server has been empty for the whole grace period
     */
    public boolean update(int players, long graceNanos, long now) {
        if (players > 0) {
            emptySince = -1;
            return false;
        }
        if (emptySince < 0) {
            emptySince = now;
        }
        return now - emptySince >= graceNanos;
    }

    /** True while the server is empty but still inside its grace period. */
    public boolean waiting(long graceNanos, long now) {
        return emptySince >= 0 && now - emptySince < graceNanos;
    }
}
