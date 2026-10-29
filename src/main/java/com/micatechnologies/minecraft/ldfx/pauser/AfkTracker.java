package com.micatechnologies.minecraft.ldfx.pauser;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Who has been idle, for the soft pause. A player is active when they move, look around, chat or
 * run a command. Movement is read from the player's position and rotation once a tick in
 * {@link #allAfk}, which works while the worlds are frozen because players' movement packets are
 * still processed. Server thread only.
 */
public final class AfkTracker {

    private static final class Seen {
        double x;
        double y;
        double z;
        float yaw;
        float pitch;
        long activeAt;
    }

    private static final Map<UUID, Seen> SEEN = new HashMap<>();

    private AfkTracker() {
    }

    private static void active(EntityPlayerMP player) {
        Seen s = SEEN.computeIfAbsent(player.getUniqueID(), id -> new Seen());
        s.activeAt = System.currentTimeMillis();
    }

    /** True when every online player has been idle for at least {@code afkSeconds}. */
    static boolean allAfk(MinecraftServer server, int afkSeconds) {
        long now = System.currentTimeMillis();
        boolean all = true;
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            Seen s = SEEN.get(p.getUniqueID());
            if (s == null) {
                s = new Seen();
                s.activeAt = now;
                SEEN.put(p.getUniqueID(), s);
            }
            if (p.posX != s.x || p.posY != s.y || p.posZ != s.z || p.rotationYaw != s.yaw || p.rotationPitch != s.pitch) {
                s.x = p.posX;
                s.y = p.posY;
                s.z = p.posZ;
                s.yaw = p.rotationYaw;
                s.pitch = p.rotationPitch;
                s.activeAt = now;
            }
            if (now - s.activeAt < afkSeconds * 1000L) {
                all = false;
            }
        }
        return all;
    }

    /** Forge event handlers: chat, commands, joins and leaves. */
    public static final class Events {

        @SubscribeEvent
        public void onChat(ServerChatEvent event) {
            active(event.getPlayer());
        }

        @SubscribeEvent
        public void onCommand(CommandEvent event) {
            if (event.getSender() instanceof EntityPlayerMP) {
                active((EntityPlayerMP) event.getSender());
            }
        }

        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.player instanceof EntityPlayerMP) {
                active((EntityPlayerMP) event.player);
            }
        }

        @SubscribeEvent
        public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            SEEN.remove(event.player.getUniqueID());
        }
    }
}
