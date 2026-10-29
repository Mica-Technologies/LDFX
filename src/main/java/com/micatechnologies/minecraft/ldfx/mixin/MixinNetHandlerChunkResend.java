package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.chunk.ClientChunkCache;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes a full chunk sent again for a chunk the client already holds.
 *
 * <p>The server resends a whole chunk when a single tick changes blocks in all 16 of its
 * sections (a tall WorldEdit or FAWE edit, for instance). Vanilla then creates a brand new
 * {@code Chunk} over the loaded one without unloading it: the old chunk's tile entities stay in
 * the world's lists (still ticking and rendering, a leak) and its entities lose their chunk, so
 * they stop rendering until they cross into another chunk. {@code Chunk.read} already updates a
 * loaded chunk in place (Forge refreshes its tile entities there), so the fix is to keep the
 * loaded chunk and only create one when there is none.</p>
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerChunkResend {

    @Redirect(method = "handleChunkData",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/multiplayer/WorldClient;doPreChunk(IIZ)V"))
    private void ldfx$keepLoadedChunk(WorldClient world, int x, int z, boolean load) {
        ChunkProviderClient provider = world.getChunkProvider();
        Chunk live = provider instanceof ClientChunkCache
                ? ((ClientChunkCache) provider).ldfx$getLiveChunk(x, z)
                : provider.getLoadedChunk(x, z);
        if (load && live != null) {
            return;
        }
        world.doPreChunk(x, z, load);
    }
}
