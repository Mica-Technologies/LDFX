package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.entity.MobActivity;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Entity activation range: mobs far from every player skip most ticks (Spigot's idea). They still
 * age, and get a full tick once every {@code wake_ticks}, so they never freeze for good; anything
 * that is fighting, hurt, burning, ridden, leashed, named or swimming always ticks
 * ({@link MobActivity#ldfx$skipTick()}). Only mobs ({@code EntityLiving}) are affected: players,
 * items, vehicles and other entities tick as usual.
 */
@Mixin(World.class)
public abstract class MixinWorldActivationRange {

    @Redirect(method = "updateEntityWithOptionalForce",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onUpdate()V"))
    private void ldfx$maybeSkip(Entity entity) {
        if (entity instanceof MobActivity && ((MobActivity) entity).ldfx$skipTick()) {
            return;
        }
        entity.onUpdate();
    }
}
