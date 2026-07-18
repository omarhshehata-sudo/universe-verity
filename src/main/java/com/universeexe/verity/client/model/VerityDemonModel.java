package com.universeexe.verity.client.model;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.entity.VerityDemonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class VerityDemonModel extends GeoModel<VerityDemonEntity> {
    @Override
    public ResourceLocation getModelResource(VerityDemonEntity animatable) {
        return new ResourceLocation(UniverseVerity.MOD_ID, "geo/entity/verity_demon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VerityDemonEntity animatable) {
        return new ResourceLocation(UniverseVerity.MOD_ID, "textures/entity/verity_demon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(VerityDemonEntity animatable) {
        return new ResourceLocation(UniverseVerity.MOD_ID, "animations/entity/verity_demon.animation.json");
    }

    @Override
    public void setCustomAnimations(VerityDemonEntity animatable, long instanceId,
                                    AnimationState<VerityDemonEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        CoreGeoBone head = this.getAnimationProcessor().getBone("head");
        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head != null && entityData != null) {
            head.setRotX(entityData.headPitch() * ((float) Math.PI / 180f));
            head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180f));
        }
    }
}
