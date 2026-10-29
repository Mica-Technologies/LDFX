package com.micatechnologies.minecraft.ldfx.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.micatechnologies.minecraft.ldfx.chunk.ChunkSendProfiler;
import com.micatechnologies.minecraft.ldfx.chunk.ChunkSendStats;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Times generating a new chunk on the server thread ({@code debug.chunk_send_profiling}). */
@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServerProfiling {

    @WrapOperation(method = "provideChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/gen/IChunkGenerator;generateChunk(II)Lnet/minecraft/world/chunk/Chunk;"))
    private Chunk ldfx$timeGenerate(IChunkGenerator generator, int x, int z, Operation<Chunk> op) {
        long t = System.nanoTime();
        Chunk c = op.call(generator, x, z);
        ChunkSendProfiler.STATS.add(ChunkSendStats.GENERATE, System.nanoTime() - t);
        return c;
    }
}
