package com.micatechnologies.minecraft.ldfx.fps;

import com.micatechnologies.minecraft.ldfx.Ldfx;
import com.micatechnologies.minecraft.ldfx.config.LdfxConfig;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/**
 * Lowers the frame rate while the player is away or the game is in the background, and the
 * volume while it is in the background: the feature set of FPS Reducer (bre2el), which has had
 * no 1.12.2 update since 2021, reimplemented from its behaviour.
 *
 * <p>Nothing here touches {@code options.txt}: the cap is held by waiting at the end of each
 * frame ({@code MixinMinecraftFramePacing}), and the volume through the sound system's master
 * gain, so the player's own settings are never changed or saved.</p>
 */
public final class FpsReducer {

    /** Set while the feature runs; the frame pacing mixin reads it. */
    public static volatile FpsReducer instance;

    private final LdfxConfig config;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final IdlePolicy policy;
    private final KeyBinding reduceKey = new KeyBinding("key.ldfx.reduce_fps", Keyboard.KEY_NONE, "key.categories.ldfx");

    private boolean enabled = true;
    private boolean wasActive = true;
    private double lastX;
    private double lastY;
    private double lastZ;
    private float lastYaw;
    private float lastPitch;
    private double cpuLoad = -1;
    private long cpuSampledAt;

    private FpsReducer(LdfxConfig config) {
        this.config = config;
        this.policy = new IdlePolicy(config.fpsIdleSeconds, config.fpsIdleFps, config.fpsBackgroundFps, System.nanoTime());
    }

    public static void start(LdfxConfig config) {
        FpsReducer r = new FpsReducer(config);
        ClientRegistry.registerKeyBinding(r.reduceKey);
        ClientCommandHandler.instance.registerCommand(r.new Command());
        MinecraftForge.EVENT_BUS.register(r);
        instance = r;
    }

    /** Called at the end of every frame: waits as long as the current cap asks for. */
    public void endOfFrame() {
        if (!enabled) {
            return;
        }
        long now = System.nanoTime();
        long wait = policy.waitNanos(policy.capFps(Display.isActive(), now), now);
        if (wait > 0) {
            try {
                Thread.sleep(wait / 1_000_000L, (int) (wait % 1_000_000L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void activity() {
        policy.activity(System.nanoTime());
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        // The key's own press and release must not count as activity, or releasing it would
        // end the reduction it just started.
        if (reduceKey.getKeyCode() != Keyboard.KEY_NONE && Keyboard.getEventKey() == reduceKey.getKeyCode()) {
            if (reduceKey.isPressed()) {
                policy.force();
            }
            return;
        }
        activity();
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        activity();
    }

    @SubscribeEvent
    public void onGuiKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        activity();
    }

    @SubscribeEvent
    public void onGuiMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        activity();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (config.fpsHeldMouseCounts && Display.isActive()
                && (Mouse.isButtonDown(0) || Mouse.isButtonDown(1) || Mouse.isButtonDown(2))) {
            activity();
        }
        EntityPlayerSP player = mc.player;
        if (player != null) {
            boolean moved = player.posX != lastX || player.posY != lastY || player.posZ != lastZ
                    || player.rotationYaw != lastYaw || player.rotationPitch != lastPitch;
            boolean counts = "always".equals(config.fpsMovement)
                    || ("riding_or_flying".equals(config.fpsMovement) && (player.isRiding() || player.capabilities.isFlying));
            if (moved && counts) {
                activity();
            }
            lastX = player.posX;
            lastY = player.posY;
            lastZ = player.posZ;
            lastYaw = player.rotationYaw;
            lastPitch = player.rotationPitch;
        }
        boolean active = Display.isActive();
        if (active != wasActive) {
            wasActive = active;
            applyVolume(active);
            if (active) {
                activity();
            }
        }
    }

    private void applyVolume(boolean active) {
        if (config.fpsBackgroundVolume >= 1) {
            return;
        }
        float master = mc.gameSettings.getSoundLevel(SoundCategory.MASTER);
        mc.getSoundHandler().setSoundLevel(SoundCategory.MASTER,
                active || !enabled ? master : (float) (master * config.fpsBackgroundVolume));
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!config.fpsHud || event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.gameSettings.showDebugInfo) {
            return;
        }
        String text = Minecraft.getDebugFPS() + " FPS";
        if (config.fpsHudCpu) {
            long now = System.nanoTime();
            if (now - cpuSampledAt > 1_000_000_000L) {
                cpuSampledAt = now;
                cpuLoad = processCpuLoad();
            }
            if (cpuLoad >= 0) {
                text += String.format(Locale.ROOT, "  CPU %d%%", Math.round(cpuLoad * 100));
            }
        }
        ScaledResolution res = event.getResolution();
        int w = mc.fontRenderer.getStringWidth(text);
        boolean right = config.fpsHudCorner.endsWith("right");
        boolean bottom = config.fpsHudCorner.startsWith("bottom");
        int x = right ? res.getScaledWidth() - w - 3 : 3;
        int y = bottom ? res.getScaledHeight() - 12 : 3;
        mc.fontRenderer.drawStringWithShadow(text, x, y, 0xFFFFFF);
    }

    private static double processCpuLoad() {
        try {
            java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuLoad();
            }
        } catch (Throwable ignored) {
            // Not available on this JVM.
        }
        return -1;
    }

    /** {@code /ldfx fps [on|off|reduce|status]}: control the reducer for this session. */
    private final class Command extends CommandBase {

        @Override
        public String getName() {
            return "ldfx";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/ldfx fps [on|off|reduce|status]";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return true;
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
            if (args.length == 1) {
                return getListOfStringsMatchingLastWord(args, "fps");
            }
            if (args.length == 2) {
                return getListOfStringsMatchingLastWord(args, "on", "off", "reduce", "status");
            }
            return Collections.emptyList();
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
            String sub = args.length >= 2 && "fps".equals(args[0]) ? args[1] : "status";
            switch (sub) {
                case "on":
                    enabled = true;
                    break;
                case "off":
                    enabled = false;
                    applyVolume(true);
                    break;
                case "reduce":
                    enabled = true;
                    policy.force();
                    break;
                default:
                    break;
            }
            long now = System.nanoTime();
            int cap = policy.capFps(Display.isActive(), now);
            sender.sendMessage(new TextComponentString("LDFX FPS reducer: " + (enabled ? "on" : "off for this session")
                    + (enabled ? (cap > 0 ? ", capped at " + cap + " FPS" : ", not reducing now") : "")
                    + " (idle after " + Math.round(config.fpsIdleSeconds) + " s, idle " + config.fpsIdleFps
                    + " FPS, background " + config.fpsBackgroundFps + " FPS)"));
            Ldfx.LOGGER.debug("LDFX: /ldfx fps {}", sub);
        }
    }
}
