package com.micatechnologies.minecraft.ldfx;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
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
    }
}
