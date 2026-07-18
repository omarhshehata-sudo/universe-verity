package com.universeexe.verity.client.render;

import com.universeexe.verity.client.model.VerityDemonModel;
import com.universeexe.verity.entity.VerityDemonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VerityDemonRenderer extends GeoEntityRenderer<VerityDemonEntity> {
    public VerityDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new VerityDemonModel());
        this.shadowRadius = 0.7f;
    }

    @Override
    public boolean shouldShowName(VerityDemonEntity animatable) {
        return false;
    }
}
