package com.micatechnologies.minecraft.ldfx.fps;

/**
 * Decides the frame rate cap from activity and window focus, and how long to wait each frame to
 * hold it. No Minecraft types, so it is unit-tested.
 *
 * <p>The cap is 0 (no cap) while the player is active and the window is in front. It drops to the
 * background rate when the window is not active, to the idle rate after {@code idleSeconds}
 * without input, and to the idle rate at once while the player has asked for it with the
 * keybind or command (until their next input).</p>
 */
public final class IdlePolicy {

    private final long idleNanos;
    private final int idleFps;
    private final int backgroundFps;

    private long lastActivity;
    private boolean forced;
    private long lastFrameEnd;

    public IdlePolicy(double idleSeconds, int idleFps, int backgroundFps, long now) {
        this.idleNanos = idleSeconds <= 0 ? Long.MAX_VALUE : (long) (idleSeconds * 1e9);
        this.idleFps = idleFps;
        this.backgroundFps = backgroundFps;
        this.lastActivity = now;
        this.lastFrameEnd = now;
    }

    /** The player did something; ends a manual reduction. */
    public void activity(long now) {
        lastActivity = now;
        forced = false;
    }

    /** Reduce now, until the next activity. */
    public void force() {
        forced = true;
    }

    public boolean isForced() {
        return forced;
    }

    /** The frame rate cap right now, or 0 for none. */
    public int capFps(boolean windowActive, long now) {
        int cap = 0;
        if (forced || now - lastActivity >= idleNanos) {
            cap = idleFps;
        }
        if (!windowActive && backgroundFps > 0 && (cap == 0 || backgroundFps < cap)) {
            cap = backgroundFps;
        }
        return cap;
    }

    /** Nanoseconds to wait so that frames under {@code capFps} are spaced 1/cap apart; 0 if none. */
    public long waitNanos(int capFps, long now) {
        if (capFps <= 0) {
            lastFrameEnd = now;
            return 0;
        }
        long frame = 1_000_000_000L / capFps;
        long wait = lastFrameEnd + frame - now;
        if (wait <= 0 || wait > frame) {
            lastFrameEnd = now;
            return 0;
        }
        lastFrameEnd += frame;
        return wait;
    }

    public boolean idle(long now) {
        return now - lastActivity >= idleNanos;
    }
}
