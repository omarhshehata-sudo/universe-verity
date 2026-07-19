package com.universeexe.verity.client.subtitle;

import com.universeexe.verity.registry.VeritySounds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Set;

/**
 * Central styling for Verity dialogue subtitles shown to the player.
 */
@OnlyIn(Dist.CLIENT)
public final class VeritySubtitles {
    /** Bright yellow used for all Verity dialogue subtitles. */
    public static final TextColor YELLOW = TextColor.fromRgb(0xFFFF55);
    public static final Style YELLOW_STYLE = Style.EMPTY.withColor(YELLOW);

    /** Round smiley (U+263A WHITE SMILING FACE). */
    public static final String SMILEY = "\u263A";

    private static final String SUBTITLE_PREFIX = "subtitles.universe_verity.";

    /** Environmental / non-dialogue subtitle keys (no smiley or yellow styling). */
    private static final Set<String> NON_DIALOGUE_KEYS = Set.of(
            "subtitles.universe_verity.box.rustling",
            "subtitles.universe_verity.box.knocking",
            "subtitles.universe_verity.box.shifting",
            "subtitles.universe_verity.verity.reveal_box_open",
            "subtitles.universe_verity.verity.reveal_box_click",
            "subtitles.universe_verity.verity.reveal_impact",
            "subtitles.universe_verity.verity.reveal_movement",
            "subtitles.universe_verity.countdown.transform"
    );

    private VeritySubtitles() {
    }

    public static boolean isVerityDialogueKey(String key) {
        return key != null
                && key.startsWith(SUBTITLE_PREFIX)
                && !NON_DIALOGUE_KEYS.contains(key);
    }

    public static boolean isVerityDialogueSound(String soundId) {
        return isVerityDialogueKey(VeritySounds.subtitleKeyFor(soundId));
    }

    public static Component fromKey(String key) {
        if (!isVerityDialogueKey(key)) {
            return Component.translatable(key);
        }
        return format(Component.translatable(key));
    }

    public static Component format(Component line) {
        if (line == null || line.equals(Component.empty())) {
            return Component.empty();
        }
        MutableComponent text = line.copy().withStyle(style -> style.withColor(YELLOW).withItalic(false));
        return Component.literal(SMILEY + " ")
                .withStyle(YELLOW_STYLE)
                .append(text);
    }
}
