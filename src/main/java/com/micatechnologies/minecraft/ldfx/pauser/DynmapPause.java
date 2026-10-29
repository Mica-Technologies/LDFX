package com.micatechnologies.minecraft.ldfx.pauser;

import com.micatechnologies.minecraft.ldfx.Ldfx;
import java.lang.reflect.Method;

/**
 * Pauses Dynmap's rendering while the server is deep-paused, when {@code pause_dynmap} is on.
 *
 * <p>Dynmap's API can pause full/radius renders and update renders, but not zoom-out processing
 * ({@code freshenZoomOutFiles}), which is what keeps an idle server busy when a map has a
 * backlog: rewriting zoomed-out tiles, one image encode per tile. All three, zoom-out included,
 * pause when Dynmap believes the server's TPS is under its limits, so LDFX reports a TPS of 0
 * through {@code MapManager.updateTPS} and 20 on resume. Dynmap normally refreshes that value from
 * its server tick handler, which the deep pause already skips. Reflection only, so LDFX does not
 * depend on Dynmap; anything unexpected disables the hook with one warning.</p>
 */
final class DynmapPause {

    private static boolean failed;
    private static boolean paused;

    private DynmapPause() {
    }

    static void pause() {
        if (setTps(0.0) && !paused) {
            paused = true;
            Ldfx.LOGGER.info("LDFX pauser: Dynmap rendering and zoom-out processing paused");
        }
    }

    static void resume() {
        if (paused && setTps(20.0)) {
            paused = false;
            Ldfx.LOGGER.info("LDFX pauser: Dynmap rendering resumed");
        }
    }

    private static boolean setTps(double tps) {
        if (failed) {
            return false;
        }
        try {
            Class<?> mm = Class.forName("org.dynmap.MapManager");
            Object instance = mm.getField("mapman").get(null);
            if (instance == null) {
                return false;
            }
            Method update = mm.getMethod("updateTPS", double.class);
            update.invoke(instance, tps);
            return true;
        } catch (ClassNotFoundException e) {
            failed = true;
            return false;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            Ldfx.LOGGER.warn("LDFX pauser: cannot pause Dynmap (unexpected Dynmap version); leaving it running", e);
            return false;
        }
    }
}
