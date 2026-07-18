package com.universeexe.verity.item;

import com.universeexe.verity.UniverseVerity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class VerityVariants {
    private static final List<String> VALID = List.of(
            "crazy_talking", "happy", "happy_sleep", "happy_talking", "hurt", "neutral", "noface",
            "serious_1", "serious_2", "serious_3", "serious_talking", "evil", "evil_talking",
            "smiling_evil", "crazy", "neutral_talking"
    );

    private VerityVariants() {
    }

    public static String fromStack(ItemStack stack) {
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(VerityItem.TAG_VARIANT)) {
                return sanitize(tag.getString(VerityItem.TAG_VARIANT));
            }
        }
        return "happy";
    }

    public static ResourceLocation entityTexture(String variant) {
        return new ResourceLocation(UniverseVerity.MOD_ID, "textures/entity/" + sanitize(variant) + ".png");
    }

    public static String sanitize(String variant) {
        if (variant == null || variant.isBlank()) {
            return "happy";
        }
        String v = variant.trim().toLowerCase().replace('-', '_');
        if (v.endsWith(".png")) {
            v = v.substring(0, v.length() - 4);
        }
        return VALID.contains(v) ? v : "happy";
    }
}
