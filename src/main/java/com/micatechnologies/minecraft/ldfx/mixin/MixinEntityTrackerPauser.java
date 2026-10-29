package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.pauser.ServerPauser;
import net.minecraft.entity.EntityTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skips the entity tracker while the pauser has the worlds frozen: nothing moves, nothing to send. */
@Mixin(EntityTracker.class)
public abstract class MixinEntityTrackerPauser {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ldfx$pauseTracker(CallbackInfo ci) {
        if (ServerPauser.isPaused()) {
            ci.cancel();
        }
    }
}
