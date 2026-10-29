package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.pauser.ServerPauser;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server pauser, soft level ({@link ServerPauser}): freezes each world while every online player is
 * AFK. With nobody online the deep pause skips the world loop altogether, so these never run then.
 *
 * <p>Both halves of a world's tick are cancelled: {@code tick()} (time, weather, scheduled block
 * updates, random ticks, spawning, chunk unloading) and {@code updateEntities()} (every entity and
 * every ticking tile entity; vanilla's own idle shutdown of it never happens while a chunk is
 * force-loaded). Mods' own tick event handlers keep running for the connected players.</p>
 */
@Mixin(WorldServer.class)
public abstract class MixinWorldServerPauser {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ldfx$pauseTick(CallbackInfo ci) {
        if (ServerPauser.isPaused()) {
            ci.cancel();
        }
    }

    @Inject(method = "updateEntities", at = @At("HEAD"), cancellable = true)
    private void ldfx$pauseEntities(CallbackInfo ci) {
        if (ServerPauser.isPaused()) {
            ci.cancel();
        }
    }
}
