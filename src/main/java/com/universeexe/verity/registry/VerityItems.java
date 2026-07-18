package com.universeexe.verity.registry;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.item.VerityItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VerityItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, UniverseVerity.MOD_ID);

    public static final RegistryObject<Item> VERITY_ITEM = ITEMS.register("verity_item",
            () -> new VerityItem(new Item.Properties()));

    private VerityItems() {
    }
}
