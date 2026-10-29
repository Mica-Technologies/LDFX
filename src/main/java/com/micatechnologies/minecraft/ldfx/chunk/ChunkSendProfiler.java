package com.micatechnologies.minecraft.ldfx.chunk;

import com.micatechnologies.minecraft.ldfx.Ldfx;
import com.micatechnologies.minecraft.ldfx.config.LdfxConfig;
import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Main-thread timing of chunk sending, when {@code debug.chunk_send_profiling} is on: the timing
 * mixins feed {@link #STATS}, a summary goes to the log every {@code report_seconds}, and
 * {@code /ldfxstats chunks [reset]} shows the current window.
 */
public final class ChunkSendProfiler {

    public static final ChunkSendStats STATS = new ChunkSendStats(System.nanoTime());

    private long lastReport = System.nanoTime();

    /** Tag key for a tile entity: its registered id, else its class name. */
    public static String key(TileEntity te) {
        ResourceLocation id = TileEntity.getKey(te.getClass());
        return id != null ? id.toString() : te.getClass().getName();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastReport >= LdfxConfig.get().chunkProfilingReportSeconds * 1_000_000_000L) {
            if (STATS.count(ChunkSendStats.SEND) > 0 || STATS.count(ChunkSendStats.UPDATE) > 0 || STATS.count(ChunkSendStats.LOAD) > 0) {
                for (String line : STATS.report(now)) {
                    Ldfx.LOGGER.info("LDFX {}", line);
                }
            }
            STATS.reset(now);
            lastReport = now;
        }
    }

    /** {@code /ldfxstats chunks [reset]}. */
    public static final class Command extends CommandBase {

        @Override
        public String getName() {
            return "ldfxstats";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/ldfxstats chunks [reset]";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
            if (args.length == 1) {
                return getListOfStringsMatchingLastWord(args, "chunks");
            }
            if (args.length == 2) {
                return getListOfStringsMatchingLastWord(args, "reset");
            }
            return Collections.emptyList();
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
            long now = System.nanoTime();
            for (String line : STATS.report(now)) {
                sender.sendMessage(new TextComponentString(line));
            }
            if (args.length >= 2 && "reset".equals(args[1])) {
                STATS.reset(now);
                sender.sendMessage(new TextComponentString("Chunk send statistics reset."));
            }
        }
    }
}
