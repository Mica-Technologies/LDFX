package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.chunk.ClientChunkCache;
import com.micatechnologies.minecraft.ldfx.config.LdfxConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the blocks of chunks the server unloads, for drawing ({@link ClientChunkCache}).
 *
 * <p>{@code getLoadedChunk} falls back to the cache, so rendering and block reads see cached
 * chunks; {@code loadChunk} drops the cached copy before vanilla loads the fresh one. Once a
 * second, cached chunks beyond the render distance (plus a margin) are dropped, oldest first
 * past the size cap.</p>
 */
@Mixin(ChunkProviderClient.class)
public abstract class MixinChunkProviderClientCache implements ClientChunkCache {

    @Shadow
    @Final
    private Long2ObjectMap<Chunk> loadedChunks;

    @Unique
    private final Long2ObjectOpenHashMap<Chunk> ldfx$cached = new Long2ObjectOpenHashMap<>();

    @Unique
    private int ldfx$ticks;

    @Override
    @Nullable
    public Chunk ldfx$getLiveChunk(int x, int z) {
        return loadedChunks.get(ChunkPos.asLong(x, z));
    }

    @Override
    public boolean ldfx$retain(int x, int z, Chunk chunk) {
        int max = LdfxConfig.get().chunkCacheMax;
        if (ldfx$cached.size() >= max) {
            ldfx$evict(0);
            if (ldfx$cached.size() >= max) {
                return false;
            }
        }
        ldfx$cached.put(ChunkPos.asLong(x, z), chunk);
        return true;
    }

    @Inject(method = "getLoadedChunk", at = @At("RETURN"), cancellable = true)
    private void ldfx$fallBackToCache(int x, int z, CallbackInfoReturnable<Chunk> cir) {
        if (cir.getReturnValue() == null && !ldfx$cached.isEmpty()) {
            Chunk cached = ldfx$cached.get(ChunkPos.asLong(x, z));
            if (cached != null) {
                cir.setReturnValue(cached);
            }
        }
    }

    @Inject(method = "loadChunk", at = @At("HEAD"))
    private void ldfx$dropCachedCopy(int x, int z, CallbackInfoReturnable<Chunk> cir) {
        ldfx$cached.remove(ChunkPos.asLong(x, z));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ldfx$evictFarChunks(CallbackInfoReturnable<Boolean> cir) {
        if (++ldfx$ticks % 20 == 0 && !ldfx$cached.isEmpty()) {
            ldfx$evict(2);
        }
    }

    /** Drops cached chunks farther than the render distance plus {@code margin} chunks. */
    @Unique
    private void ldfx$evict(int margin) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            ldfx$cached.clear();
            return;
        }
        int px = player.chunkCoordX;
        int pz = player.chunkCoordZ;
        int range = Minecraft.getMinecraft().gameSettings.renderDistanceChunks + margin;
        ObjectIterator<Long2ObjectMap.Entry<Chunk>> it = ldfx$cached.long2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            Chunk c = it.next().getValue();
            if (Math.abs(c.x - px) > range || Math.abs(c.z - pz) > range) {
                it.remove();
            }
        }
    }
}
