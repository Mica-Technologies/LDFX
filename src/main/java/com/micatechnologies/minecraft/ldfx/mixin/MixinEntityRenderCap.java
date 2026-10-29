package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.config.LdfxConfig;
import com.micatechnologies.minecraft.ldfx.entity.EntityRenderCaps;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the configured per-type render distance caps ({@link EntityRenderCaps}).
 *
 * <p>{@code isInRangeToRender3d} is where every entity renderer asks whether an entity is close
 * enough to draw, before any frustum test, and almost no entity overrides it (unlike
 * {@code isInRangeToRenderDist}, which many do), so one check here covers every type.</p>
 */
@Mixin(Entity.class)
public abstract class MixinEntityRenderCap {

    @Unique
    private static EntityRenderCaps ldfx$caps;

    @Shadow
    public double posX;

    @Shadow
    public double posY;

    @Shadow
    public double posZ;

    @Inject(method = "isInRangeToRender3d", at = @At("HEAD"), cancellable = true)
    private void ldfx$capRenderDistance(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        EntityRenderCaps caps = ldfx$caps;
        if (caps == null) {
            caps = ldfx$caps = new EntityRenderCaps(LdfxConfig.get().entityRenderDistances);
        }
        if (caps.isEmpty()) {
            return;
        }
        double dx = posX - x;
        double dy = posY - y;
        double dz = posZ - z;
        if (caps.beyondCap((Entity) (Object) this, dx * dx + dy * dy + dz * dz)) {
            cir.setReturnValue(false);
        }
    }
}
