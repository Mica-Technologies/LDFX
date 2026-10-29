package com.micatechnologies.minecraft.ldfx.chunk;

import javax.annotation.Nullable;
import net.minecraft.world.chunk.Chunk;

/**
 * The client chunk cache, implemented on {@code ChunkProviderClient} by a mixin.
 *
 * <p>Chunks the server unloads are unloaded for real (their entities and tile entities leave the
 * world, so nothing ticks or leaks), but their blocks are kept for drawing until the player is
 * out of render range. The rest of the game sees a cached chunk as loaded, read-only scenery.</p>
 */
public interface ClientChunkCache {

    /** A chunk the server has sent and not unloaded; never a cached one. */
    @Nullable
    Chunk ldfx$getLiveChunk(int x, int z);

    /** Takes a chunk the server has just unloaded into the cache; false if the cache is full. */
    boolean ldfx$retain(int x, int z, Chunk chunk);
}
