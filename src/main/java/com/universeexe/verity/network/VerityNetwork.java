package com.universeexe.verity.network;

import com.universeexe.verity.UniverseVerity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class VerityNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UniverseVerity.MOD_ID, "voice"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int nextId;

    private VerityNetwork() {
    }

    public static void register() {
        nextId = 0;
        CHANNEL.registerMessage(nextId++, PlayVoicePacket.class,
                PlayVoicePacket::encode, PlayVoicePacket::decode, PlayVoicePacket::handle);
        CHANNEL.registerMessage(nextId++, InterruptVoicePacket.class,
                InterruptVoicePacket::encode, InterruptVoicePacket::decode, InterruptVoicePacket::handle);
        CHANNEL.registerMessage(nextId++, PlaybackStatusPacket.class,
                PlaybackStatusPacket::encode, PlaybackStatusPacket::decode, PlaybackStatusPacket::handle);
        CHANNEL.registerMessage(nextId++, VoiceDebugSyncPacket.class,
                VoiceDebugSyncPacket::encode, VoiceDebugSyncPacket::decode, VoiceDebugSyncPacket::handle);
    }
}
