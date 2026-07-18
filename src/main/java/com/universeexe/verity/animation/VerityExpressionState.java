package com.universeexe.verity.animation;

import java.util.Locale;
import java.util.Optional;

/**
 * Named facial expression states for Verity's ball form.
 * Default idle automatic face is {@link #HAPPY} (JAR smiley).
 * {@link #NEUTRAL} remains available via {@code /verity expression set}.
 * Active this version: HAPPY, GREETING, LISTENING, BLINK, LONG_BLINK.
 * Others are reserved hooks for later progression.
 */
public enum VerityExpressionState {
    NEUTRAL,
    HAPPY,
    GREETING,
    LISTENING,
    THINKING,
    CONFUSED,
    CONCERNED,
    SURPRISED,
    BLINK,
    LONG_BLINK,
    BLANK,
    WATCHING,
    OFF;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isBlink() {
        return this == BLINK || this == LONG_BLINK;
    }

    public static Optional<VerityExpressionState> fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (VerityExpressionState state : values()) {
            if (state.id().equals(key)) {
                return Optional.of(state);
            }
        }
        return Optional.empty();
    }

    /** Expressions exposed to {@code /verity expression set}. */
    public static final VerityExpressionState[] COMMAND_CHOICES = {
            NEUTRAL, HAPPY, GREETING, LISTENING, THINKING, CONFUSED, CONCERNED,
            SURPRISED, BLINK, LONG_BLINK, BLANK, WATCHING, OFF
    };
}
