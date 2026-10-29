package com.micatechnologies.minecraft.ldfx.mixin;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Removes unloaded entities from the world's entity list in linear time.
 *
 * <p>Each tick, {@code World.updateEntities} calls
 * {@code loadedEntityList.removeAll(unloadedEntityList)}. Both are {@code ArrayList}s, so every
 * loaded entity is looked up in the unloaded list one by one: loaded x unloaded comparisons.
 * When chunks unload in bulk (a player flying away, a server unloading a dimension) that is
 * millions of comparisons in one tick. Looking them up in a hash set gives the same result
 * ({@code Entity} equality is by entity id, and a {@code HashSet} uses the same
 * {@code equals}/{@code hashCode}) in loaded + unloaded steps.</p>
 */
@Mixin(World.class)
public abstract class MixinWorldEntityUnload {

    /** Below this many entities to remove, the plain list lookup is already cheap. */
    private static final int SET_THRESHOLD = 8;

    @Redirect(method = "updateEntities",
              at = @At(value = "INVOKE", target = "Ljava/util/List;removeAll(Ljava/util/Collection;)Z", ordinal = 0))
    private boolean ldfx$removeUnloaded(List<?> loaded, Collection<?> unloaded) {
        if (unloaded.size() < SET_THRESHOLD) {
            return loaded.removeAll(unloaded);
        }
        return loaded.removeAll(new HashSet<>(unloaded));
    }
}
