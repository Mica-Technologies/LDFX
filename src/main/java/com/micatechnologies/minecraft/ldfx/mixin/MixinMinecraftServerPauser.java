package com.micatechnologies.minecraft.ldfx.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.micatechnologies.minecraft.ldfx.Ldfx;
import com.micatechnologies.minecraft.ldfx.pauser.ServerPauser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server pauser, deep level ({@link ServerPauser}): while nobody is online, suspends everything in
 * the server tick except what a login needs.
 *
 * <p>Freezing the worlds alone is not enough: many mods tick from {@code ServerTickEvent} or
 * {@code WorldTickEvent} rather than from an entity or tile entity (Immersive Vehicles' physics,
 * IC2's energy net, FAWE's and BuildCraft's queues). While deep-paused:
 * <ul>
 *   <li>the {@code ServerTickEvent} START and END posts are skipped;</li>
 *   <li>the per-dimension loop sees no dimensions, which skips {@code WorldTickEvent}, the world
 *       tick, entities, tile entities and the entity tracker;</li>
 *   <li>autosaves are skipped; the world is saved once as the pause begins.</li>
 * </ul>
 * Scheduled tasks, networking (logins, status pings) and the player list still run, so the first
 * player to connect gets in and the next tick resumes normally.</p>
 *
 * <p>Chainable MixinExtras injectors rather than {@code @Redirect} or cancelling {@code tick()}, so
 * other mods' injections into the same methods still apply. Ported from Server Utility Mod.</p>
 */
@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServerPauser {

    @Inject(method = "tick", at = @At("HEAD"))
    private void ldfx$decidePause(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (ServerPauser.beginTick(server)) {
            Ldfx.LOGGER.info("LDFX pauser: saving before suspending ticks");
            server.getPlayerList().saveAllPlayerData();
            server.saveAllWorlds(true);
        }
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onPreServerTick()V", remap = false))
    private boolean ldfx$skipPreServerTick(FMLCommonHandler handler) {
        return !ServerPauser.isDeepPaused();
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onPostServerTick()V", remap = false))
    private boolean ldfx$skipPostServerTick(FMLCommonHandler handler) {
        return !ServerPauser.isDeepPaused();
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/management/PlayerList;saveAllPlayerData()V"))
    private boolean ldfx$skipAutosavePlayers(PlayerList playerList) {
        return !ServerPauser.isDeepPaused();
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/MinecraftServer;saveAllWorlds(Z)V"))
    private boolean ldfx$skipAutosaveWorlds(MinecraftServer server, boolean isSilent) {
        return !ServerPauser.isDeepPaused();
    }

    @ModifyExpressionValue(method = "updateTimeLightAndEntities", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/common/DimensionManager;getIDs(Z)[Ljava/lang/Integer;", remap = false))
    private Integer[] ldfx$skipWorldLoop(Integer[] ids) {
        return ServerPauser.isDeepPaused() ? new Integer[0] : ids;
    }
}
