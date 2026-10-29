package com.micatechnologies.minecraft.ldfx.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkSendStatsTest {

    private static final long MS = 1_000_000L;

    @Test
    void countsTotalsAndMaximaPerPhase() {
        ChunkSendStats s = new ChunkSendStats(0);
        s.add(ChunkSendStats.BUILD, 2 * MS);
        s.add(ChunkSendStats.BUILD, 5 * MS);
        assertEquals(2, s.count(ChunkSendStats.BUILD));
        assertEquals(7 * MS, s.nanos(ChunkSendStats.BUILD));
        assertEquals(5 * MS, s.maxNanos(ChunkSendStats.BUILD));
        assertEquals(0, s.count(ChunkSendStats.LOAD));
    }

    @Test
    void otherBuildTimeIsWhatBlocksAndTagsDoNotExplain() {
        ChunkSendStats s = new ChunkSendStats(0);
        s.add(ChunkSendStats.BUILD, 10 * MS);
        s.add(ChunkSendStats.BLOCKS, 3 * MS);
        s.addTileTag("littletiles:blocklittletiles", 4 * MS);
        assertEquals(3 * MS, s.otherBuildNanos());
        assertEquals(4 * MS, s.nanos(ChunkSendStats.TILE_TAGS));
    }

    @Test
    void otherBuildTimeNeverNegative() {
        ChunkSendStats s = new ChunkSendStats(0);
        s.add(ChunkSendStats.BUILD, 1 * MS);
        s.add(ChunkSendStats.BLOCKS, 3 * MS);
        assertEquals(0, s.otherBuildNanos());
    }

    @Test
    void ranksTileKeysByTotalTime() {
        ChunkSendStats s = new ChunkSendStats(0);
        s.addTileTag("minecraft:chest", 1 * MS);
        s.addTileTag("minecraft:chest", 1 * MS);
        s.addTileTag("littletiles:blocklittletiles", 9 * MS);
        s.addTileTag("csm:sign", 3 * MS);
        List<String> top = s.topTileKeys(2);
        assertEquals(2, top.size());
        assertTrue(top.get(0).startsWith("littletiles:blocklittletiles: 9.0 ms over 1 tags"), top.get(0));
        assertTrue(top.get(1).startsWith("csm:sign"), top.get(1));
    }

    @Test
    void reportPerSecondUsesTheWindowAndResetClears() {
        ChunkSendStats s = new ChunkSendStats(0);
        s.add(ChunkSendStats.SEND, 20 * MS);
        List<String> lines = s.report(10_000 * MS);
        assertTrue(lines.get(0).contains("10.0 s"), lines.get(0));
        assertTrue(lines.stream().anyMatch(l -> l.contains("send") && l.contains("2.00 ms per second")), String.join("\n", lines));
        s.reset(10_000 * MS);
        assertEquals(0, s.count(ChunkSendStats.SEND));
        assertTrue(s.report(11_000 * MS).stream().anyMatch(l -> l.contains("send") && l.contains("none")));
    }
}
