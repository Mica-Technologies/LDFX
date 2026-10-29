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
    void otherLoadTimeIsWhatTheNamedPartsDoNotExplain() {
        ChunkSendStats s = new ChunkSendStats(0);
        s.add(ChunkSendStats.LOAD, 20 * MS);
        s.add(ChunkSendStats.LOAD_ENTITIES, 5 * MS);
        s.add(ChunkSendStats.LOAD_DATA_EVENT, 1 * MS);
        s.add(ChunkSendStats.LOAD_ON_LOAD, 6 * MS);
        s.add(ChunkSendStats.LOAD_POPULATE, 2 * MS);
        s.add(ChunkSendStats.LOAD_CALLBACKS, 3 * MS);
        assertEquals(3 * MS, s.otherLoadNanos());
        s.add(ChunkSendStats.LOAD_ON_LOAD, 10 * MS);
        assertEquals(0, s.otherLoadNanos());
    }

    @Test
    void keyedTablesAreSeparateAndFeedTheirPhase() {
        ChunkSendStats s = new ChunkSendStats(0);
        s.addKeyed(ChunkSendStats.TILE_CREATE, "csm:signal", 4 * MS);
        s.addKeyed(ChunkSendStats.ENTITY_CREATE, "mts:builder", 7 * MS);
        s.addTileTag("minecraft:chest", 1 * MS);
        assertEquals(4 * MS, s.nanos(ChunkSendStats.TILE_CREATE));
        assertEquals(1, s.count(ChunkSendStats.ENTITY_CREATE));
        assertEquals(1, s.topKeys(ChunkSendStats.TILE_CREATE, 5, "created").size());
        assertTrue(s.topKeys(ChunkSendStats.ENTITY_CREATE, 5, "created").get(0).startsWith("mts:builder: 7.0 ms over 1 created"));
        assertTrue(s.topKeys(ChunkSendStats.TILE_ON_LOAD, 5, "calls").isEmpty());
        assertTrue(s.topTileKeys(5).get(0).startsWith("minecraft:chest"));
    }

    @Test
    void reportShowsLoadSplitAndRankings() {
        ChunkSendStats s = new ChunkSendStats(0);
        s.add(ChunkSendStats.LOAD, 10 * MS);
        s.add(ChunkSendStats.LOAD_ENTITIES, 4 * MS);
        s.addKeyed(ChunkSendStats.TILE_CREATE, "littletiles:tile", 3 * MS);
        s.addKeyed(ChunkSendStats.TILE_ON_LOAD, "ie:connector", 2 * MS);
        String all = String.join("\n", s.report(1_000 * MS));
        assertTrue(all.contains("load split: entities 4.0 ms (1 tile entities, 0 entities)"), all);
        assertTrue(all.contains("costliest tile entities to create:\n    littletiles:tile: 3.0 ms over 1 created"), all);
        assertTrue(all.contains("costliest tile entity onLoad:\n    ie:connector"), all);
        assertTrue(!all.contains("costliest entities to create"), all);
        s.reset(1_000 * MS);
        assertTrue(s.topKeys(ChunkSendStats.TILE_CREATE, 5, "created").isEmpty());
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
