package com.micatechnologies.minecraft.ldfx.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.micatechnologies.minecraft.ldfx.chunk.ChunkSendProfiler;
import com.micatechnologies.minecraft.ldfx.chunk.ChunkSendStats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Times each tile entity's Forge {@code onLoad} while a loaded chunk is being added to the world,
 * by id ({@code debug.chunk_send_profiling}). Calls outside a chunk load (placing a block) are not
 * counted.
 */
@Mixin(World.class)
public abstract class MixinWorldTileOnLoadProfiling {

    @WrapOperation(method = "addTileEntity", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/tileentity/TileEntity;onLoad()V"))
    private void ldfx$timeTileOnLoad(TileEntity te, Operation<Void> op) {
        if (!ChunkSendProfiler.inChunkOnLoad) {
            op.call(te);
            return;
        }
        long t = System.nanoTime();
        op.call(te);
        ChunkSendProfiler.STATS.addKeyed(ChunkSendStats.TILE_ON_LOAD, ChunkSendProfiler.key(te), System.nanoTime() - t);
    }
}
