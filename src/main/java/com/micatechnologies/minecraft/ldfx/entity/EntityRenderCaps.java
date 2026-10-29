package com.micatechnologies.minecraft.ldfx.entity;

import com.micatechnologies.minecraft.ldfx.Ldfx;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;

/**
 * Per-entity-type render distance caps, from the config's {@code entity_render_distance} list.
 *
 * <p>Vanilla draws decorative entities very far: an item frame's range is 64 blocks times its
 * size times the render distance weight, so at render distance 16 it draws as far as the chunks
 * do. In a city full of item frames, armour stands and paintings that is a lot of drawing for
 * things too small to see. The cap is looked up once per entity class.</p>
 */
public final class EntityRenderCaps {

    private static final double NONE = -1;

    private final Map<ResourceLocation, Double> byId = new HashMap<>();
    private final Map<Class<? extends Entity>, Double> byClass = new IdentityHashMap<>();

    public EntityRenderCaps(String[] entries) {
        for (String entry : entries) {
            int eq = entry.lastIndexOf('=');
            if (eq <= 0) {
                Ldfx.LOGGER.warn("LDFX: ignoring entity render distance '{}': expected id=blocks", entry);
                continue;
            }
            try {
                double blocks = Double.parseDouble(entry.substring(eq + 1).trim());
                byId.put(new ResourceLocation(entry.substring(0, eq).trim()), blocks * blocks);
            } catch (NumberFormatException e) {
                Ldfx.LOGGER.warn("LDFX: ignoring entity render distance '{}': not a number", entry);
            }
        }
    }

    public boolean isEmpty() {
        return byId.isEmpty();
    }

    /** Whether an entity this far away (squared blocks) is beyond its type's cap. */
    public boolean beyondCap(Entity entity, double distanceSq) {
        Double cap = byClass.get(entity.getClass());
        if (cap == null) {
            ResourceLocation id = EntityList.getKey(entity);
            Double found = id == null ? null : byId.get(id);
            cap = found == null ? NONE : found;
            byClass.put(entity.getClass(), cap);
        }
        return cap >= 0 && distanceSq > cap;
    }
}
