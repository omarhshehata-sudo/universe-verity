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

    public static final RegistryObject<SoundEvent> BOX_HELLOOO = register("verity.box.hellooo");
    public static final RegistryObject<SoundEvent> BOX_IS_SOMEONE_OUT_THERE = register("verity.box.is_someone_out_there");
    public static final RegistryObject<SoundEvent> BOX_I_CAN_HEAR_YOU_MOVING = register("verity.box.i_can_hear_you_moving");
    public static final RegistryObject<SoundEvent> BOX_COULD_YOU_OPEN_THIS = register("verity.box.could_you_open_this");
    public static final RegistryObject<SoundEvent> BOX_PLEASE = register("verity.box.please");
    public static final RegistryObject<SoundEvent> BOX_YOURE_STILL_THERE = register("verity.box.youre_still_there");
    public static final RegistryObject<SoundEvent> BOX_OH_YOU_FOUND_THE_OPENING = register("verity.box.oh_you_found_the_opening");
    public static final RegistryObject<SoundEvent> BOX_RUSTLE_1 = register("verity.box.rustle_1");
    public static final RegistryObject<SoundEvent> BOX_RUSTLE_2 = register("verity.box.rustle_2");
    public static final RegistryObject<SoundEvent> BOX_KNOCK_1 = register("verity.box.knock_1");
    public static final RegistryObject<SoundEvent> BOX_KNOCK_2 = register("verity.box.knock_2");
    public static final RegistryObject<SoundEvent> BOX_SHIFT = register("verity.box.shift");
    public static final RegistryObject<SoundEvent> BOX_THUMP = register("verity.box.thump");
    public static final RegistryObject<SoundEvent> REVEAL_BOX_OPEN = register("verity.reveal.box_open");
    public static final RegistryObject<SoundEvent> REVEAL_MOVEMENT = register("verity.reveal.movement");
    public static final RegistryObject<SoundEvent> GREETING_PERSONAL_HELPER = register("verity.greeting.personal_helper");
    public static final RegistryObject<SoundEvent> VOICE_OKAY_WHERE_ARE_WE_GOING = register("verity.voice.okay_where_are_we_going");
    public static final RegistryObject<SoundEvent> VOICE_ALRIGHT_RIGHT_BEHIND_YOU = register("verity.voice.alright_right_behind_you");
    public static final RegistryObject<SoundEvent> VOICE_LEAD_THE_WAY = register("verity.voice.lead_the_way");
    public static final RegistryObject<SoundEvent> INTRO_VIDEO_AUDIO = register("verity.intro.video_audio");

    private static final Map<String, RegistryObject<SoundEvent>> BY_ID = new LinkedHashMap<>();

    static {
        BY_ID.put("verity.box.hellooo", BOX_HELLOOO);
        BY_ID.put("verity.box.is_someone_out_there", BOX_IS_SOMEONE_OUT_THERE);
        BY_ID.put("verity.box.i_can_hear_you_moving", BOX_I_CAN_HEAR_YOU_MOVING);
        BY_ID.put("verity.box.could_you_open_this", BOX_COULD_YOU_OPEN_THIS);
        BY_ID.put("verity.box.please", BOX_PLEASE);
        BY_ID.put("verity.box.youre_still_there", BOX_YOURE_STILL_THERE);
        BY_ID.put("verity.box.oh_you_found_the_opening", BOX_OH_YOU_FOUND_THE_OPENING);
        BY_ID.put("verity.box.rustle_1", BOX_RUSTLE_1);
        BY_ID.put("verity.box.rustle_2", BOX_RUSTLE_2);
        BY_ID.put("verity.box.knock_1", BOX_KNOCK_1);
        BY_ID.put("verity.box.knock_2", BOX_KNOCK_2);
        BY_ID.put("verity.box.shift", BOX_SHIFT);
        BY_ID.put("verity.box.thump", BOX_THUMP);
        BY_ID.put("verity.reveal.box_open", REVEAL_BOX_OPEN);
        BY_ID.put("verity.reveal.movement", REVEAL_MOVEMENT);
        BY_ID.put("verity.greeting.personal_helper", GREETING_PERSONAL_HELPER);
        BY_ID.put("verity.voice.okay_where_are_we_going", VOICE_OKAY_WHERE_ARE_WE_GOING);
        BY_ID.put("verity.voice.alright_right_behind_you", VOICE_ALRIGHT_RIGHT_BEHIND_YOU);
        BY_ID.put("verity.voice.lead_the_way", VOICE_LEAD_THE_WAY);
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
