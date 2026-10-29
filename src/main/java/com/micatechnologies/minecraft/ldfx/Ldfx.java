package com.micatechnologies.minecraft.ldfx;

import com.micatechnologies.minecraft.ldfx.config.LdfxConfig;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main mod class for LDFX (Limitless Development: Fixes &amp; Optimizations).
 *
 * <p>LDFX fixes vanilla and Forge bugs and removes performance bottlenecks that other 1.12.2
 * optimization mods leave alone, chiefly around chunk loading and the client's handling of
 * server data. Almost all of it is mixins registered by {@code LdfxCoreMod}, which apply before
 * this class is constructed.</p>
 *
 * <p><b>Either side, or both.</b> Client patches help on any server, and server patches help any
 * client, so {@code acceptableRemoteVersions = "*"} lets each side run without the other.</p>
 */
@Mod(modid = Tags.MODID,
     version = Tags.VERSION,
     name = Tags.MODNAME,
     acceptedMinecraftVersions = "[1.12.2]",
     acceptableRemoteVersions = "*")
public class Ldfx {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MODID);

    @Mod.Instance(Tags.MODID)
    public static Ldfx instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("{} {} loaded", Tags.MODNAME, Tags.VERSION);
        if (event.getSide().isClient() && LdfxConfig.get().terrainOffThread) {
            applyTerrainOffThread();
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    /** Forge reads its config again when it is saved from the mod list; set the flag again after. */
    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.PostConfigChangedEvent event) {
        if ("forge".equals(event.getModID())) {
            applyTerrainOffThread();
        }
    }

    private static void applyTerrainOffThread() {
        if (!ForgeModContainer.alwaysSetupTerrainOffThread) {
            ForgeModContainer.alwaysSetupTerrainOffThread = true;
            LOGGER.info("LDFX: turned on Forge's alwaysSetupTerrainOffThread (chunk_updates.terrain_off_thread)");
        }
    }
}
