package com.universeexe.verity.registry;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.item.VerityItem;
import com.universeexe.verity.item.VerityQuestIconItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VerityItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, UniverseVerity.MOD_ID);

    public static final RegistryObject<Item> VERITY_ITEM = ITEMS.register("verity_item",
            () -> new VerityItem(new Item.Properties()));

    public static final RegistryObject<Item> VERITY_BOX_QUEST_ICON = ITEMS.register("verity_box_quest_icon",
            () -> new VerityQuestIconItem(new Item.Properties()));

    public static final RegistryObject<Item> VERITY_VOICE_QUEST_ICON = ITEMS.register("verity_voice_quest_icon",
            () -> new VerityQuestIconItem(new Item.Properties()));

    public static final RegistryObject<Item> VERITY_SOUND_QUEST_ICON = ITEMS.register("verity_sound_quest_icon",
            () -> new VerityQuestIconItem(new Item.Properties()));

    private VerityItems() {
    }
}
