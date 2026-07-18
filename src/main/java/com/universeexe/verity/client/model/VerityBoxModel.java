package com.universeexe.verity.client.model;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.entity.VerityBoxEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib 4.8 caches baked geos/animations under the short asset id (e.g. {@code verity_box}),
 * loaded from {@code assets/<modid>/geckolib/geo/} and {@code .../geckolib/animations/}.
 */
public class VerityBoxModel extends DefaultedEntityGeoModel<VerityBoxEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(UniverseVerity.MOD_ID, "textures/entity/verity_box.png");

    public VerityBoxModel() {
        super(new ResourceLocation(UniverseVerity.MOD_ID, "verity_box"));
    }

    @Override
    public ResourceLocation getTextureResource(VerityBoxEntity animatable) {
        return TEXTURE;
    }
}
