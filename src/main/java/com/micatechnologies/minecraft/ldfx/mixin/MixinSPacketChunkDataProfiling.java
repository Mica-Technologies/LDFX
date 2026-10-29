package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.chunk.ChunkSendProfiler;
import com.micatechnologies.minecraft.ldfx.chunk.ChunkSendStats;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Times the server-side construction of a chunk packet ({@code debug.chunk_send_profiling}): the
 * whole build from the size calculation on, the block data copy, and each tile entity's update
 * tag, credited to its tile entity id. Only loaded while profiling is on.
 */
@Mixin(SPacketChunkData.class)
public abstract class MixinSPacketChunkDataProfiling {

    @Unique
    private long ldfx$buildStart;

    @Unique
    private long ldfx$blocksStart;

    @Inject(method = "<init>(Lnet/minecraft/world/chunk/Chunk;I)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/network/play/server/SPacketChunkData;calculateChunkSize(Lnet/minecraft/world/chunk/Chunk;ZI)I"))
    private void ldfx$startBuild(Chunk chunk, int filter, CallbackInfo ci) {
        ldfx$buildStart = System.nanoTime();
    }

    @Inject(method = "<init>(Lnet/minecraft/world/chunk/Chunk;I)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/network/play/server/SPacketChunkData;extractChunkData(Lnet/minecraft/network/PacketBuffer;Lnet/minecraft/world/chunk/Chunk;ZI)I"))
    private void ldfx$startBlocks(Chunk chunk, int filter, CallbackInfo ci) {
        ldfx$blocksStart = System.nanoTime();
    }

    @Inject(method = "<init>(Lnet/minecraft/world/chunk/Chunk;I)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/network/play/server/SPacketChunkData;extractChunkData(Lnet/minecraft/network/PacketBuffer;Lnet/minecraft/world/chunk/Chunk;ZI)I",
                     shift = At.Shift.AFTER))
    private void ldfx$endBlocks(Chunk chunk, int filter, CallbackInfo ci) {
        ChunkSendProfiler.STATS.add(ChunkSendStats.BLOCKS, System.nanoTime() - ldfx$blocksStart);
    }

    @Redirect(method = "<init>(Lnet/minecraft/world/chunk/Chunk;I)V",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntity;getUpdateTag()Lnet/minecraft/nbt/NBTTagCompound;"))
    private NBTTagCompound ldfx$timeTag(TileEntity te) {
        long start = System.nanoTime();
        NBTTagCompound tag = te.getUpdateTag();
        ChunkSendProfiler.STATS.addTileTag(ChunkSendProfiler.key(te), System.nanoTime() - start);
        return tag;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/chunk/Chunk;I)V", at = @At("RETURN"))
    private void ldfx$endBuild(Chunk chunk, int filter, CallbackInfo ci) {
        ChunkSendProfiler.STATS.add(ChunkSendStats.BUILD, System.nanoTime() - ldfx$buildStart);
    }
}
