package com.universeexe.verity.util;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.registry.VeritySounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

public final class VerityAssetValidator {
    private VerityAssetValidator() {
    }

    public static void validate() {
        UniverseVerity.LOGGER.info("Validating Universe: Verity asset registrations...");
        if (!ModList.get().isLoaded("geckolib")) {
            VerityDebug.warn("GeckoLib is not loaded. Entity models will not render correctly.");
        }
        VeritySounds.all().forEach((id, sound) -> {
            if (sound.get() == null) {
                VerityDebug.warn("Sound event failed to resolve: {}", id);
            } else {
                VerityDebug.log("Registered sound {}", new ResourceLocation(UniverseVerity.MOD_ID, id));
            }
        });
        UniverseVerity.LOGGER.info("Universe: Verity asset validation complete. Voice assets: provided. Movement SFX: generated placeholders documented in provided_assets/ASSET_MAPPING.md");
    }
}
