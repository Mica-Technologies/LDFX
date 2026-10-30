package com.micatechnologies.minecraft.ldfx.pauser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmptyGraceTest {

    private static final long S = 1_000_000_000L;

    @Test
    void pausesOnlyAfterTheServerStaysEmptyForTheGrace() {
        EmptyGrace g = new EmptyGrace();
        assertFalse(g.update(0, 30 * S, 100 * S));
        assertTrue(g.waiting(30 * S, 100 * S));
        assertFalse(g.update(0, 30 * S, 129 * S));
        assertTrue(g.update(0, 30 * S, 130 * S));
        assertFalse(g.waiting(30 * S, 130 * S));
    }

    @Test
    void aPlayerOnlineResetsTheClock() {
        EmptyGrace g = new EmptyGrace();
        g.update(0, 30 * S, 0);
        assertFalse(g.update(1, 30 * S, 20 * S));
        assertFalse(g.waiting(30 * S, 20 * S));
        assertFalse(g.update(0, 30 * S, 40 * S));
        assertFalse(g.update(0, 30 * S, 69 * S));
        assertTrue(g.update(0, 30 * S, 70 * S));
    }

    @Test
    void zeroGracePausesAtOnce() {
        EmptyGrace g = new EmptyGrace();
        assertTrue(g.update(0, 0, 5 * S));
    }

    @Test
    void neverPausesWhilePlayersAreOnline() {
        EmptyGrace g = new EmptyGrace();
        for (long t = 0; t < 120; t += 10) {
            assertFalse(g.update(3, 30 * S, t * S));
        }
    }
}
