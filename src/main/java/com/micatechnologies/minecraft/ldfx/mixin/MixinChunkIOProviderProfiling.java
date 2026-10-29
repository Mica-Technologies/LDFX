package com.micatechnologies.minecraft.ldfx.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.micatechnologies.minecraft.ldfx.chunk.ChunkSendProfiler;
import com.micatechnologies.minecraft.ldfx.chunk.ChunkSendStats;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Times the main-thread half of a chunk load from disk ({@code debug.chunk_send_profiling}):
 * Forge reads and parses the file on its chunk IO threads, then {@code syncCallback} runs on the
 * server thread to create the chunk's entities, fire the load events, add everything to the world,
 * populate and hand the chunk to whoever asked for it. Each of those is timed separately.
 */
@Mixin(targets = "net.minecraftforge.common.chunkio.ChunkIOProvider", remap = false)
public abstract class MixinChunkIOProviderProfiling {

    @Shadow
    private Chunk chunk;

    @Unique
    private long ldfx$start = -1;

    @Inject(method = "syncCallback", at = @At("HEAD"))
    private void ldfx$head(CallbackInfo ci) {
        ldfx$start = chunk != null ? System.nanoTime() : -1;
    }

    @Inject(method = "syncCallback", at = @At("RETURN"))
    private void ldfx$return(CallbackInfo ci) {
        if (ldfx$start >= 0) {
            ChunkSendProfiler.STATS.add(ChunkSendStats.LOAD, System.nanoTime() - ldfx$start);
            ldfx$start = -1;
        }
    }

    @WrapOperation(method = "syncCallback", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/storage/AnvilChunkLoader;loadEntities(Lnet/minecraft/world/World;Lnet/minecraft/nbt/NBTTagCompound;Lnet/minecraft/world/chunk/Chunk;)V"))
    private void ldfx$timeEntities(AnvilChunkLoader loader, World world, NBTTagCompound nbt, Chunk c, Operation<Void> op) {
        long t = System.nanoTime();
        op.call(loader, world, nbt, c);
        ChunkSendProfiler.STATS.add(ChunkSendStats.LOAD_ENTITIES, System.nanoTime() - t);
    }

    @WrapOperation(method = "syncCallback", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z"))
    private boolean ldfx$timeDataEvent(EventBus bus, Event event, Operation<Boolean> op) {
        long t = System.nanoTime();
        boolean r = op.call(bus, event);
        ChunkSendProfiler.STATS.add(ChunkSendStats.LOAD_DATA_EVENT, System.nanoTime() - t);
        return r;
    }

    @WrapOperation(method = "syncCallback", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;onLoad()V", remap = true))
    private void ldfx$timeOnLoad(Chunk c, Operation<Void> op) {
        long t = System.nanoTime();
        ChunkSendProfiler.inChunkOnLoad = true;
        try {
            op.call(c);
        } finally {
            ChunkSendProfiler.inChunkOnLoad = false;
        }
        ChunkSendProfiler.STATS.add(ChunkSendStats.LOAD_ON_LOAD, System.nanoTime() - t);
    }

    @WrapOperation(method = "syncCallback", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;populate(Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/gen/IChunkGenerator;)V",
            remap = true))
    private void ldfx$timePopulate(Chunk c, IChunkProvider provider, IChunkGenerator generator, Operation<Void> op) {
        long t = System.nanoTime();
        op.call(c, provider, generator);
        ChunkSendProfiler.STATS.add(ChunkSendStats.LOAD_POPULATE, System.nanoTime() - t);
    }

    @Unique
    private long ldfx$callbacksStart;

    /** The second call (ordinal 1): the first runs the callbacks of a chunk that failed to load. */
    @Inject(method = "syncCallback", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/chunkio/ChunkIOProvider;runCallbacks()V", ordinal = 1))
    private void ldfx$callbacksHead(CallbackInfo ci) {
        ldfx$callbacksStart = System.nanoTime();
    }

    @Inject(method = "syncCallback", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/chunkio/ChunkIOProvider;runCallbacks()V", ordinal = 1, shift = At.Shift.AFTER))
    private void ldfx$callbacksReturn(CallbackInfo ci) {
        ChunkSendProfiler.STATS.add(ChunkSendStats.LOAD_CALLBACKS, System.nanoTime() - ldfx$callbacksStart);
    }
}
