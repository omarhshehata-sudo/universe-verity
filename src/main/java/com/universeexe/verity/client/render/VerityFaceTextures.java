package com.universeexe.verity.client.render;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.animation.VerityExpressionState;
import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.trust.MoodState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Maps synced face variant + talk state to verity-5.7.3 sphere PNGs under {@code textures/entity/}.
 *
 * <p>JAR rules ({@code VerityEntity.getTextureRL} / {@code setVariant}):
 * <ul>
 *   <li>Default idle = {@code happy} ({@code VARIANT_DEFAULT})</li>
 *   <li>Explicit {@code hurt} override from landing / damage</li>
 *   <li>While talking: {@code happy}→{@code happy_talking}, {@code crazy}→{@code crazy_talking},
 *       {@code evil}→{@code evil_talking}, serious_*→{@code serious_talking}, else keep base name</li>
 *   <li>Trust mood faces apply only when server sets an explicit non-auto variant via trust updates</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class VerityFaceTextures {
    /** JAR {@code VARIANT_DEFAULT}. */
    public static final String DEFAULT_VARIANT = "happy";

    private VerityFaceTextures() {
    }

    public static ResourceLocation forEntity(VerityEntity entity) {
        String faceVariant = entity.getFaceVariant();
        if (faceVariant != null && "hurt".equalsIgnoreCase(faceVariant)) {
            return forVariant("hurt");
        }
        String base = resolveBaseVariant(
                entity.getRenderExpression(),
                faceVariant,
                entity.getMoodState()
        );
        if (entity.isVisuallyTalking()) {
            base = talkingVariant(base);
        }
        return forVariant(base);
    }

    /**
     * JAR idle face: explicit synced variant, else {@code happy}. Mood is used only when trust
     * pushed a concrete variant (not {@code auto}).
     */
    public static String resolveBaseVariant(
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
        if (synced != MoodState.MEH && synced != MoodState.HAPPY && synced != MoodState.FRIENDLY) {
            return sanitize(synced.legacyFaceVariant());
        }
        return DEFAULT_VARIANT;
    }

    /** @deprecated use {@link #resolveBaseVariant} */
    @Deprecated
    public static String resolveVariant(
            VerityExpressionState expression,
            String overrideVariant,
            MoodState mood
    ) {
        return resolveBaseVariant(expression, overrideVariant, mood);
    }

    /** Trust mood → PNG when server explicitly applies mood face (not {@code auto}). */
    public static String moodLegacyVariant(MoodState mood) {
        MoodState synced = mood == null ? MoodState.MEH : mood;
        return sanitize(synced.legacyFaceVariant());
    }

    public static ResourceLocation forVariant(String variant) {
        String safe = sanitize(variant);
        return new ResourceLocation(UniverseVerity.MOD_ID, "textures/entity/" + safe + ".png");
    }

    public static String baseVariant(VerityExpressionState expression, String overrideVariant) {
        return resolveBaseVariant(expression, overrideVariant, MoodState.MEH);
    }

    /** Matches verity-5.7.3 {@code getTextureRL} talking swap. */
    public static String talkingVariant(String base) {
        return switch (sanitize(base)) {
            case "happy", "happy_sleep" -> "happy_talking";
            case "crazy" -> "crazy_talking";
            case "evil", "smiling_evil" -> "evil_talking";
            case "serious_1", "serious_2", "serious_3" -> "serious_talking";
            case "neutral" -> "neutral_talking";
            case "hurt", "noface", "verity_demon" -> base;
            default -> base.endsWith("_talking") ? base : base;
        };
    }

    public static String sanitize(String variant) {
        if (variant == null || variant.isBlank()) {
            return DEFAULT_VARIANT;
        }
        String v = variant.trim().toLowerCase().replace('-', '_');
        if (v.endsWith(".png")) {
            v = v.substring(0, v.length() - 4);
        }
        return switch (v) {
            case "crazy_talking", "happy", "happy_sleep", "happy_talking", "hurt", "neutral", "noface",
                 "serious_1", "serious_2", "serious_3", "serious_talking", "evil", "evil_talking",
                 "smiling_evil", "crazy", "neutral_talking", "verity_demon" -> v;
            default -> DEFAULT_VARIANT;
        };
    }
}
