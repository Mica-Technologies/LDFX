package com.micatechnologies.minecraft.ldfx.fps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IdlePolicyTest {

    private static final long S = 1_000_000_000L;

    @Test
    void noCapWhileActiveAndInFront() {
        IdlePolicy p = new IdlePolicy(300, 10, 15, 0);
        assertEquals(0, p.capFps(true, 299 * S));
    }

    @Test
    void idleAfterTheWait() {
        IdlePolicy p = new IdlePolicy(300, 10, 15, 0);
        assertEquals(10, p.capFps(true, 300 * S));
        p.activity(301 * S);
        assertEquals(0, p.capFps(true, 302 * S));
    }

    @Test
    void backgroundCapsAtOnceAndTheLowerCapWins() {
        IdlePolicy p = new IdlePolicy(300, 10, 15, 0);
        assertEquals(15, p.capFps(false, S));
        assertEquals(10, p.capFps(false, 400 * S));
    }

    @Test
    void zeroBackgroundLeavesItAlone() {
        IdlePolicy p = new IdlePolicy(300, 10, 0, 0);
        assertEquals(0, p.capFps(false, S));
    }

    @Test
    void forcedUntilTheNextActivity() {
        IdlePolicy p = new IdlePolicy(300, 10, 15, 0);
        p.force();
        assertEquals(10, p.capFps(true, S));
        p.activity(2 * S);
        assertEquals(0, p.capFps(true, 3 * S));
    }

    @Test
    void zeroIdleSecondsNeverIdles() {
        IdlePolicy p = new IdlePolicy(0, 10, 15, 0);
        assertEquals(0, p.capFps(true, 100_000 * S));
    }

    @Test
    void waitsSpaceFramesAtTheCap() {
        IdlePolicy p = new IdlePolicy(300, 10, 15, 0);
        long frame = S / 10;
        // The first capped frame waits until one frame after the previous one ended.
        assertEquals(frame, p.waitNanos(10, 0));
        // After sleeping that long, the next frame is timed from there.
        assertEquals(frame - frame / 4, p.waitNanos(10, frame + frame / 4));
        assertTrue(p.waitNanos(0, 5 * S) == 0);
    }
}
