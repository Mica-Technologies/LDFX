package com.micatechnologies.minecraft.ldfx.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.micatechnologies.minecraft.ldfx.chunk.ChunkSendProfiler;
import com.micatechnologies.minecraft.ldfx.chunk.ChunkSendStats;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Times creating each tile entity and entity of a chunk being loaded, by id
 * ({@code debug.chunk_send_profiling}). {@code loadEntities} is Forge's main-thread half of
 * reading a chunk; a passenger is counted with the entity it rides.
 */
@Mixin(AnvilChunkLoader.class)
public abstract class MixinAnvilChunkLoaderProfiling {

    @WrapOperation(method = "loadEntities", remap = false, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/tileentity/TileEntity;create(Lnet/minecraft/world/World;Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/tileentity/TileEntity;"))
    private TileEntity ldfx$timeTileCreate(World world, NBTTagCompound nbt, Operation<TileEntity> op) {
        long t = System.nanoTime();
        TileEntity te = op.call(world, nbt);
        ChunkSendProfiler.STATS.addKeyed(ChunkSendStats.TILE_CREATE, nbt.getString("id"), System.nanoTime() - t);
        return te;
    }

    @WrapOperation(method = "loadEntities", remap = false, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/chunk/storage/AnvilChunkLoader;readChunkEntity(Lnet/minecraft/nbt/NBTTagCompound;Lnet/minecraft/world/World;Lnet/minecraft/world/chunk/Chunk;)Lnet/minecraft/entity/Entity;"))
    private Entity ldfx$timeEntityCreate(NBTTagCompound nbt, World world, Chunk chunk, Operation<Entity> op) {
        long t = System.nanoTime();
        Entity e = op.call(nbt, world, chunk);
        ChunkSendProfiler.STATS.addKeyed(ChunkSendStats.ENTITY_CREATE, nbt.getString("id"), System.nanoTime() - t);
        return e;
    }
}
