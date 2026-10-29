package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.config.LdfxConfig;
import com.micatechnologies.minecraft.ldfx.entity.MobActivity;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Distance-scaled AI decisions, and the data the activation range needs ({@link MobActivity}).
 *
 * <p>Vanilla asks every idle AI goal of every mob whether it should start every 3 ticks
 * ({@code EntityAITasks.tickRate}), wherever the mob is; wandering and target searches make that
 * the bulk of AI time. Mobs far from every player get a longer interval (6 ticks beyond 32
 * blocks, 12 beyond 64 by default). Goals that are already running still update every tick, so a
 * mob that is doing something keeps doing it smoothly. This is what Performant 1.10 did on
 * 1.12.2, reimplemented from its behaviour.</p>
 */
@Mixin(EntityLiving.class)
public abstract class MixinEntityLivingActivity extends EntityLivingBase implements MobActivity {

    @Unique
    private static Set<ResourceLocation> ldfx$exempt;

    @Shadow
    @Final
    public EntityAITasks tasks;

    @Shadow
    @Final
    public EntityAITasks targetTasks;

    @Shadow
    public abstract boolean getLeashed();

    @Shadow
    public abstract EntityLivingBase getAttackTarget();

    @Unique
    private double ldfx$playerDistSq = -1;

    @Unique
    private int ldfx$lastRate = 3;

    @Unique
    private int ldfx$exemptState;

    protected MixinEntityLivingActivity(World world) {
        super(world);
    }

    @Override
    public double ldfx$nearestPlayerDistSq() {
        if (ldfx$playerDistSq < 0 || (ticksExisted + getEntityId()) % 20 == 0) {
            EntityPlayer p = world.getClosestPlayerToEntity(this, -1);
            ldfx$playerDistSq = p == null ? Double.MAX_VALUE : getDistanceSq(p);
        }
        return ldfx$playerDistSq;
    }

    @Inject(method = "updateEntityActionState", at = @At("HEAD"))
    private void ldfx$scaleAiRate(CallbackInfo ci) {
        LdfxConfig c = LdfxConfig.get();
        if (!c.aiThrottle) {
            return;
        }
        double d = ldfx$nearestPlayerDistSq();
        int rate = d > (double) c.aiFarBlocks * c.aiFarBlocks ? c.aiFarRate
                : d > (double) c.aiNearBlocks * c.aiNearBlocks ? c.aiMidRate : 3;
        if (rate != ldfx$lastRate) {
            ldfx$lastRate = rate;
            ((AccessorEntityAITasks) tasks).ldfx$setTickRate(rate);
            ((AccessorEntityAITasks) targetTasks).ldfx$setTickRate(rate);
        }
    }

    @Override
    public boolean ldfx$skipTick() {
        LdfxConfig c = LdfxConfig.get();
        if (!c.activationRange || world.isRemote || ticksExisted % c.activationWakeTicks == 0) {
            return false;
        }
        if (isBeingRidden() || isRiding() || hurtTime > 0 || isBurning() || isInWater() || getLeashed()
                || hasCustomName() || getAttackTarget() != null || ldfx$isExempt()) {
            return false;
        }
        int range = this instanceof IMob ? c.activationMonsterBlocks : c.activationOtherBlocks;
        return ldfx$nearestPlayerDistSq() > (double) range * range;
    }

    @Unique
    private boolean ldfx$isExempt() {
        if (ldfx$exemptState == 0) {
            if (ldfx$exempt == null) {
                Set<ResourceLocation> s = new HashSet<>();
                for (String id : LdfxConfig.get().activationExempt) {
                    s.add(new ResourceLocation(id.trim()));
                }
                ldfx$exempt = s;
            }
            ResourceLocation id = EntityList.getKey(this);
            ldfx$exemptState = id != null && ldfx$exempt.contains(id) ? 1 : 2;
        }
        return ldfx$exemptState == 1;
    }
}
