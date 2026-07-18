package com.universeexe.verity.registry;

import com.universeexe.verity.UniverseVerity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VeritySounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, UniverseVerity.MOD_ID);

    public static final RegistryObject<SoundEvent> BOX_ANYONE_OUT_THERE = register("verity.box.anyone_out_there");
    public static final RegistryObject<SoundEvent> BOX_CAN_YOU_HEAR_ME = register("verity.box.can_you_hear_me");
    public static final RegistryObject<SoundEvent> BOX_IS_SOMEONE_THERE = register("verity.box.is_someone_there");
    public static final RegistryObject<SoundEvent> BOX_RUSTLE_1 = register("verity.box.rustle_1");
    public static final RegistryObject<SoundEvent> BOX_RUSTLE_2 = register("verity.box.rustle_2");
    public static final RegistryObject<SoundEvent> BOX_KNOCK_1 = register("verity.box.knock_1");
    public static final RegistryObject<SoundEvent> BOX_KNOCK_2 = register("verity.box.knock_2");
    public static final RegistryObject<SoundEvent> BOX_SHIFT = register("verity.box.shift");
    public static final RegistryObject<SoundEvent> BOX_THUMP = register("verity.box.thump");
    public static final RegistryObject<SoundEvent> REVEAL_BOX_OPEN = register("verity.reveal.box_open");
    public static final RegistryObject<SoundEvent> REVEAL_MOVEMENT = register("verity.reveal.movement");
    public static final RegistryObject<SoundEvent> GREETING_PERSONAL_HELPER = register("verity.greeting.personal_helper");
    public static final RegistryObject<SoundEvent> INTRO_VIDEO_AUDIO = register("verity.intro.video_audio");

    private static final Map<String, RegistryObject<SoundEvent>> BY_ID = new LinkedHashMap<>();

    static {
        BY_ID.put("verity.box.anyone_out_there", BOX_ANYONE_OUT_THERE);
        BY_ID.put("verity.box.can_you_hear_me", BOX_CAN_YOU_HEAR_ME);
        BY_ID.put("verity.box.is_someone_there", BOX_IS_SOMEONE_THERE);
        BY_ID.put("verity.box.rustle_1", BOX_RUSTLE_1);
        BY_ID.put("verity.box.rustle_2", BOX_RUSTLE_2);
        BY_ID.put("verity.box.knock_1", BOX_KNOCK_1);
        BY_ID.put("verity.box.knock_2", BOX_KNOCK_2);
        BY_ID.put("verity.box.shift", BOX_SHIFT);
        BY_ID.put("verity.box.thump", BOX_THUMP);
        BY_ID.put("verity.reveal.box_open", REVEAL_BOX_OPEN);
        BY_ID.put("verity.reveal.movement", REVEAL_MOVEMENT);
        BY_ID.put("verity.greeting.personal_helper", GREETING_PERSONAL_HELPER);
        BY_ID.put("verity.intro.video_audio", INTRO_VIDEO_AUDIO);
    }

    private VeritySounds() {
    }

    private static RegistryObject<SoundEvent> register(String id) {
        return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(UniverseVerity.MOD_ID, id)));
    }

    public static Map<String, RegistryObject<SoundEvent>> all() {
        return BY_ID;
    }

    public static RegistryObject<SoundEvent> byId(String id) {
        return BY_ID.get(id);
    }
}
