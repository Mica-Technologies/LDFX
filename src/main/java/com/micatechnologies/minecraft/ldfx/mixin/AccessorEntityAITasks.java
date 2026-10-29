package com.micatechnologies.minecraft.ldfx.mixin;

import net.minecraft.entity.ai.EntityAITasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityAITasks.class)
public interface AccessorEntityAITasks {

    @Accessor("tickRate")
    void ldfx$setTickRate(int rate);
}
