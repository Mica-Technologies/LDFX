package com.micatechnologies.minecraft.ldfx.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TaskBudgetTest {

    private static final long MS = 1_000_000L;

    @Test
    void alwaysRunsTheMinimumEvenWhenOverBudget() {
        TaskBudget b = new TaskBudget(4, 12, 3);
        b.startFrame(0, 10);
        long late = 100 * MS;
        assertTrue(b.tryRun(late));
        assertTrue(b.tryRun(late));
        assertTrue(b.tryRun(late));
        assertFalse(b.tryRun(late));
        assertEquals(3, b.ranThisFrame());
    }

    @Test
    void runsWhileInsideTheBudget() {
        TaskBudget b = new TaskBudget(4, 12, 1);
        b.startFrame(0, 0);
        assertTrue(b.tryRun(0));
        assertTrue(b.tryRun(3 * MS));
        assertFalse(b.tryRun(4 * MS));
    }

    @Test
    void budgetGrowsWithTheBacklog() {
        TaskBudget b = new TaskBudget(4, 12, 1);
        b.startFrame(0, 0);
        assertEquals(4 * MS, b.frameBudgetNanos());
        b.startFrame(0, TaskBudget.FULL_BACKLOG / 2);
        assertEquals(8 * MS, b.frameBudgetNanos());
        b.startFrame(0, TaskBudget.FULL_BACKLOG * 10);
        assertEquals(12 * MS, b.frameBudgetNanos());
    }

    @Test
    void eachFrameStartsFresh() {
        TaskBudget b = new TaskBudget(4, 12, 1);
        b.startFrame(0, 0);
        assertTrue(b.tryRun(0));
        assertFalse(b.tryRun(10 * MS));
        b.startFrame(20 * MS, 0);
        assertTrue(b.tryRun(21 * MS));
    }
}
