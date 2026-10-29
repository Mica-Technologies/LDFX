package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.config.LdfxConfig;
import net.minecraft.tileentity.TileEntityHopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Idle hoppers back off. Vanilla sets a hopper's 8-tick cooldown only when it moved something, so
 * a hopper with nothing to do scans the container it feeds, the container above it and the item
 * entities over it again every single tick. After an attempt that moved nothing, it now waits
 * {@code idle_backoff_ticks} before trying again; hoppers that are moving items keep vanilla's
 * timing exactly.
 */
@Mixin(TileEntityHopper.class)
public abstract class MixinTileEntityHopperIdle {

    @Shadow
    private int transferCooldown;

    @Shadow
    public abstract void setTransferCooldown(int ticks);

    @Inject(method = "updateHopper", at = @At("RETURN"))
    private void ldfx$backOffWhenIdle(CallbackInfoReturnable<Boolean> cir) {
        TileEntityHopper self = (TileEntityHopper) (Object) this;
        if (!cir.getReturnValueZ() && transferCooldown <= 0 && self.getWorld() != null && !self.getWorld().isRemote) {
            setTransferCooldown(LdfxConfig.get().hopperIdleTicks);
        }
    }
}
