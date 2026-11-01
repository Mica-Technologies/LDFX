package com.micatechnologies.minecraft.ldfx.pauser;

import java.lang.reflect.Method;

/**
 * Tells MCMCP, when it is installed, that the server stopped ticking on purpose.
 *
 * <p>The deep pause skips {@code ServerTickEvent}, which is how MCMCP counts ticks, so its watchdog
 * sees a silent server thread. Without a declaration it inspects that thread's stack once a second
 * to tell a pause from a stall, and MCMCP 2026.10.30+1 to 2026.10.31 did that with a stop-the-world
 * heap walk (MCMCP #170). Declared, MCMCP takes the pause at its word and looks at nothing.
 * Reflection only, so LDFX does not depend on MCMCP; anything unexpected turns this off quietly.</p>
 */
final class McmcpPauseNotice {

    private static Method declare;
    private static boolean unavailable;

    private McmcpPauseNotice() {
    }

    /** @param pausedBy who paused the server, or null when it ticks again */
    static void declare(String pausedBy) {
        if (unavailable) {
            return;
        }
        try {
            if (declare == null) {
                declare = Class.forName("com.micatechnologies.minecraft.mcmcp.perf.IdlePause").getMethod("declare", String.class);
            }
            declare.invoke(null, pausedBy);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // MCMCP absent, or one without IdlePause.declare.
            unavailable = true;
        }
    }
}
