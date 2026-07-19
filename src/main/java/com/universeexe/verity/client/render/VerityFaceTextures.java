package com.universeexe.verity.client.render;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.animation.VerityExpressionState;
import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.trust.MoodState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Maps synced trust mood / talk / brief overrides to verity-5.7.3 sphere face PNGs
 * under {@code textures/entity/}. Clients never compute trust — they only read {@code DATA_MOOD}.
 */
@OnlyIn(Dist.CLIENT)
public final class VerityFaceTextures {
    private VerityFaceTextures() {
    }

    public static ResourceLocation forEntity(VerityEntity entity) {
        String faceVariant = entity.getFaceVariant();
        if (faceVariant != null && "hurt".equalsIgnoreCase(faceVariant)) {
            return forVariant("hurt");
        }
        String resolved = resolveVariant(
                entity.getRenderExpression(),
                faceVariant,
                entity.getMoodState()
        );
        if (entity.isVisuallyTalking()) {
            resolved = talkingVariant(resolved);
        }
        return forVariant(resolved);
    }

    /** Picks idle face from explicit override, synced mood, or transient expression (blink/blank). */
    public static String resolveVariant(
            VerityExpressionState expression,
            String overrideVariant,
            MoodState mood
    ) {
        if (overrideVariant != null && !overrideVariant.isBlank()
                && !"auto".equalsIgnoreCase(overrideVariant)) {
            return sanitize(overrideVariant);
        }
        if (expression == VerityExpressionState.BLANK || expression == VerityExpressionState.OFF) {
            return "noface";
        }
        if (expression == VerityExpressionState.BLINK || expression == VerityExpressionState.LONG_BLINK) {
            return "happy_sleep";
        }
        MoodState synced = mood == null ? MoodState.MEH : mood;
        return sanitize(synced.legacyFaceVariant());
    }

    public static ResourceLocation forVariant(String variant) {
        String safe = sanitize(variant);
        return new ResourceLocation(UniverseVerity.MOD_ID, "textures/entity/" + safe + ".png");
    }

    public static String baseVariant(VerityExpressionState expression, String overrideVariant) {
        if (overrideVariant != null && !overrideVariant.isBlank()
                && !"auto".equalsIgnoreCase(overrideVariant)) {
            return sanitize(overrideVariant);
        }
        return switch (expression) {
            case HAPPY, GREETING -> "happy";
            case LISTENING -> "serious_1";
            case THINKING -> "serious_2";
            case CONFUSED -> "crazy";
            case CONCERNED -> "serious_3";
            case SURPRISED -> "crazy";
            case BLINK, LONG_BLINK -> "happy_sleep";
            case BLANK, OFF -> "noface";
            case WATCHING -> "serious_1";
            default -> "neutral";
        };
    }

    public static String talkingVariant(String base) {
        return switch (base) {
            case "happy", "happy_sleep" -> "happy_talking";
            case "crazy" -> "crazy_talking";
            case "evil", "smiling_evil" -> "evil_talking";
            case "serious_1", "serious_2", "serious_3" -> "serious_talking";
            case "neutral" -> "neutral_talking";
            case "hurt", "noface", "verity_demon" -> base;
            default -> base.endsWith("_talking") ? base : base + "_talking";
        };
    }

    public static String sanitize(String variant) {
        if (variant == null || variant.isBlank()) {
            return "happy";
        }
        String v = variant.trim().toLowerCase().replace('-', '_');
        if (v.endsWith(".png")) {
            v = v.substring(0, v.length() - 4);
        }
        return switch (v) {
            case "crazy_talking", "happy", "happy_sleep", "happy_talking", "hurt", "neutral", "noface",
                 "serious_1", "serious_2", "serious_3", "serious_talking", "evil", "evil_talking",
                 "smiling_evil", "crazy", "neutral_talking", "verity_demon" -> v;
            default -> "happy";
        };
    }
}
