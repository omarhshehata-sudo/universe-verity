package com.universeexe.verity.registry;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.entity.VerityBoxEntity;
import com.universeexe.verity.entity.VerityDemonEntity;
import com.universeexe.verity.entity.VerityEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VerityEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, UniverseVerity.MOD_ID);

    public static final RegistryObject<EntityType<VerityBoxEntity>> VERITY_BOX = ENTITY_TYPES.register("verity_box",
            () -> EntityType.Builder.<VerityBoxEntity>of(VerityBoxEntity::new, MobCategory.MISC)
                    .sized(1.25f, 1.0f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .fireImmune()
                    .build("verity_box"));

    public static final RegistryObject<EntityType<VerityEntity>> VERITY = ENTITY_TYPES.register("verity",
            () -> EntityType.Builder.<VerityEntity>of(VerityEntity::new, MobCategory.MISC)
                    .sized(VerityEntity.TARGET_WIDTH, VerityEntity.TARGET_HEIGHT)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .fireImmune()
                    .build("verity"));

    public static final RegistryObject<EntityType<VerityDemonEntity>> VERITY_DEMON = ENTITY_TYPES.register("verity_demon",
            () -> EntityType.Builder.<VerityDemonEntity>of(VerityDemonEntity::new, MobCategory.MONSTER)
                    .sized(0.8f, 2.2f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .fireImmune()
                    .build("verity_demon"));

    private VerityEntities() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(VERITY.get(), VerityEntity.createAttributes().build());
        event.put(VERITY_DEMON.get(), VerityDemonEntity.createAttributes().build());
    }
}
