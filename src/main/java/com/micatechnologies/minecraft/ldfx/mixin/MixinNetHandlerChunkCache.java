package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.chunk.ClientChunkCache;
import java.util.ArrayList;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Hands chunks the server unloads to the client chunk cache ({@link ClientChunkCache}).
 *
 * <p>Vanilla unloads the chunk and marks its column for a render rebuild, which also rebuilds
 * the edges of its 8 neighbours, only for the chunk to come back as the player returns. Here the
 * chunk is still unloaded for real (entities and tile entities leave the world), its entity lists
 * are emptied so nothing is drawn from them, and its blocks are kept with the meshes already
 * built. If the cache is full the vanilla rebuild happens as before.</p>
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerChunkCache {

    @Redirect(method = "processChunkUnload",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/multiplayer/WorldClient;doPreChunk(IIZ)V"))
    private void ldfx$cacheUnloadedChunk(WorldClient world, int x, int z, boolean load) {
        ChunkProviderClient provider = world.getChunkProvider();
        if (load || !(provider instanceof ClientChunkCache)) {
            world.doPreChunk(x, z, load);
            return;
        }
        ClientChunkCache cache = (ClientChunkCache) provider;
        Chunk chunk = cache.ldfx$getLiveChunk(x, z);
        provider.unloadChunk(x, z);
        if (chunk == null || chunk.isEmpty()) {
            world.markBlockRangeForRenderUpdate(x * 16, 0, z * 16, x * 16 + 15, 256, z * 16 + 15);
            return;
        }
        for (ClassInheritanceMultiMap<Entity> list : chunk.getEntityLists()) {
            for (Entity e : new ArrayList<>(list)) {
                list.remove(e);
            }
        }
        if (!cache.ldfx$retain(x, z, chunk)) {
            // Cache full: rebuild as vanilla does.
            world.markBlockRangeForRenderUpdate(x * 16, 0, z * 16, x * 16 + 15, 256, z * 16 + 15);
        }
    }
}
