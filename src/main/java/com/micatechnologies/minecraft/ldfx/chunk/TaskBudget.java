package com.micatechnologies.minecraft.ldfx.chunk;

/**
 * Decides how many of the client's queued server tasks run this frame.
 *
 * <p>Vanilla runs the whole queue every frame, so 100 chunks arriving together are all applied in
 * one frame. This runs tasks in order until the frame's budget is spent, always at least
 * {@code minTasks}, and leaves the rest queued for the next frame. The budget grows with the
 * backlog so a long burst still drains quickly; nothing is ever reordered or dropped.</p>
 */
public final class TaskBudget {

    /** Backlog size at which the budget reaches its maximum. */
    static final int FULL_BACKLOG = 512;

    private final long baseNanos;
    private final long maxNanos;
    private final int minTasks;

    private long frameStart;
    private long frameBudget;
    private int ran;

    public TaskBudget(double baseMs, double maxMs, int minTasks) {
        this.baseNanos = (long) (baseMs * 1e6);
        this.maxNanos = Math.max(baseNanos, (long) (maxMs * 1e6));
        this.minTasks = Math.max(1, minTasks);
    }

    /** Starts a frame with {@code backlog} tasks waiting. */
    public void startFrame(long now, int backlog) {
        frameStart = now;
        ran = 0;
        double t = Math.min(1, backlog / (double) FULL_BACKLOG);
        frameBudget = baseNanos + (long) ((maxNanos - baseNanos) * t);
    }

    /** Whether one more task may run now; counts it if so. */
    public boolean tryRun(long now) {
        if (ran < minTasks || now - frameStart < frameBudget) {
            ran++;
            return true;
        }
        return false;
    }

    public int ranThisFrame() {
        return ran;
    }

    long frameBudgetNanos() {
        return frameBudget;
    }
}
