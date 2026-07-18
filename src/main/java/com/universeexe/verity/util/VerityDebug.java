package com.universeexe.verity.util;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.config.VerityCommonConfig;

public final class VerityDebug {
    private static boolean commandOverride;

    private VerityDebug() {
    }

    public static void setCommandOverride(boolean value) {
        commandOverride = value;
    }

    public static boolean isEnabled() {
        return commandOverride || VerityCommonConfig.DEBUG_LOGGING.get() || VerityCommonConfig.DEBUG_REVEAL_LOGGING.get();
    }

    public static void log(String message, Object... args) {
        if (isEnabled()) {
            UniverseVerity.LOGGER.info("[VerityDebug] " + message, args);
        }
    }

    public static void warn(String message, Object... args) {
        UniverseVerity.LOGGER.warn("[Verity] " + message, args);
    }
}
