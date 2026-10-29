package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.fps.FpsReducer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Holds the FPS reducer's frame rate cap ({@link FpsReducer}) by waiting at the end of each
 * frame, after vanilla's own limiter. Waiting here, rather than changing the frame limit, never
 * touches the player's settings and works whatever other mods do to {@code getLimitFramerate}.
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftFramePacing {

    @Inject(method = "runGameLoop", at = @At("TAIL"))
    private void ldfx$paceFrame(CallbackInfo ci) {
        FpsReducer reducer = FpsReducer.instance;
        if (reducer != null) {
            reducer.endOfFrame();
        }
    }
}
