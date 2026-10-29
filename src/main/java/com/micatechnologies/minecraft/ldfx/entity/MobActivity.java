package com.micatechnologies.minecraft.ldfx.entity;

/**
 * Server-side activity data LDFX keeps on every mob (implemented on {@code EntityLiving} by
 * {@code MixinEntityLivingActivity}): the squared distance to the nearest player, refreshed once
 * a second per mob (spread across ticks by entity id), so the AI and activation range checks
 * cost nothing per tick.
 */
public interface MobActivity {

    /** Squared distance to the nearest player in this world; {@link Double#MAX_VALUE} with none. */
    double ldfx$nearestPlayerDistSq();

    /** Whether the activation range lets this mob skip this tick. */
    boolean ldfx$skipTick();
}
