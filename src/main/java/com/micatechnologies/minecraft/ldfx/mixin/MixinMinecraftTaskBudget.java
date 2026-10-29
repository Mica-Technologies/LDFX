package com.micatechnologies.minecraft.ldfx.mixin;

import com.micatechnologies.minecraft.ldfx.chunk.TaskBudget;
import com.micatechnologies.minecraft.ldfx.config.LdfxConfig;
import java.util.Queue;
import java.util.concurrent.FutureTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smooth chunk loading: runs the queued server packets under a time budget per frame.
 *
 * <p>Every play packet is applied on the main thread through {@code scheduledTasks}, and vanilla
 * drains that queue completely at the top of every frame. Chunk data is the heavy part (building
 * the chunk, creating its tile entities, every tile entity's update tag), so a burst of chunks
 * becomes one long frame. Here the drain loop stops once the frame's {@link TaskBudget} is
 * spent; the rest stay queued, in order, for the next frame. While there is no world or the
 * "Loading terrain" screen is up, vanilla's full drain is kept so joining is not slowed.</p>
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftTaskBudget {

    @Shadow
    @Final
    private Queue<FutureTask<?>> scheduledTasks;

    @Shadow
    public WorldClient world;

    @Shadow
    public GuiScreen currentScreen;

    @Unique
    private TaskBudget ldfx$budget;

    @Unique
    private boolean ldfx$budgeted;

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void ldfx$startFrame(CallbackInfo ci) {
        ldfx$budgeted = world != null && !(currentScreen instanceof GuiDownloadTerrain);
        if (ldfx$budgeted) {
            if (ldfx$budget == null) {
                LdfxConfig c = LdfxConfig.get();
                ldfx$budget = new TaskBudget(c.taskBudgetMs, c.taskBudgetMaxMs, c.taskMinPerFrame);
            }
            int backlog;
            synchronized (scheduledTasks) {
                backlog = scheduledTasks.size();
            }
            ldfx$budget.startFrame(System.nanoTime(), backlog);
        }
    }

    @Redirect(method = "runGameLoop", at = @At(value = "INVOKE", target = "Ljava/util/Queue;isEmpty()Z"))
    private boolean ldfx$budgetedIsEmpty(Queue<?> queue) {
        if (queue.isEmpty()) {
            return true;
        }
        // Reporting "empty" ends vanilla's drain loop; what is left runs next frame.
        return ldfx$budgeted && !ldfx$budget.tryRun(System.nanoTime());
    }
}
