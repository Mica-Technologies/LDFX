package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.chunk.ChunkSendProfiler;
import com.micatechnologies.minecraft.ldfx.chunk.ChunkSendStats;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Times the server thread's chunk sending per {@link PlayerChunkMapEntry}
 * ({@code debug.chunk_send_profiling}): sending a new chunk to its players ({@code send}, only calls that sent it), and
 * sending block changes in an already-sent chunk ({@code update}, only calls with changes).
 */
@Mixin(PlayerChunkMapEntry.class)
public abstract class MixinPlayerChunkMapEntryProfiling {

    @Shadow
    private Chunk chunk;

    @Shadow
    private boolean sentToPlayers;

    @Shadow
    private int changes;

    @Unique
    private long ldfx$sendStart = -1;

    @Unique
    private long ldfx$updateStart = -1;

    @Inject(method = "sendToPlayers", at = @At("HEAD"))
    private void ldfx$sendHead(CallbackInfoReturnable<Boolean> cir) {
        ldfx$sendStart = sentToPlayers ? -1 : System.nanoTime();
    }

    @Inject(method = "sendToPlayers", at = @At("RETURN"))
    private void ldfx$sendReturn(CallbackInfoReturnable<Boolean> cir) {
        if (ldfx$sendStart >= 0 && sentToPlayers) {
            ChunkSendProfiler.STATS.add(ChunkSendStats.SEND, System.nanoTime() - ldfx$sendStart);
        }
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void ldfx$updateHead(CallbackInfo ci) {
        ldfx$updateStart = changes != 0 && chunk != null ? System.nanoTime() : -1;
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void ldfx$updateReturn(CallbackInfo ci) {
        if (ldfx$updateStart >= 0) {
            ChunkSendProfiler.STATS.add(ChunkSendStats.UPDATE, System.nanoTime() - ldfx$updateStart);
        }
    }
}
