package com.universeexe.verity.client.render;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.animation.VerityExpressionState;
import com.universeexe.verity.entity.VerityEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Maps expression / talk state to verity-5.7.2 face PNG paths under {@code textures/entity/}.
 */
@OnlyIn(Dist.CLIENT)
public final class VerityFaceTextures {
    private VerityFaceTextures() {
    }

    public static ResourceLocation forEntity(VerityEntity entity) {
        // Trust mood faces take priority over transient expression variants (except hurt/talking).
        String faceVariant = entity.getFaceVariant();
        if (faceVariant != null && "hurt".equalsIgnoreCase(faceVariant)) {
            return new ResourceLocation(UniverseVerity.MOD_ID, "textures/entity/hurt.png");
        }
        if (entity.isVisuallyTalking()) {
            String base = baseVariant(entity.getRenderExpression(), faceVariant);
            return new ResourceLocation(UniverseVerity.MOD_ID, "textures/entity/" + talkingVariant(base) + ".png");
        }
        String moodFace = entity.getMoodState().faceTextureName();
        return new ResourceLocation(UniverseVerity.MOD_ID, "textures/entity/verity/" + moodFace + ".png");
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
            case "hurt", "noface" -> base;
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
                 "smiling_evil", "crazy", "neutral_talking" -> v;
            default -> "happy";
        };
    }
}
