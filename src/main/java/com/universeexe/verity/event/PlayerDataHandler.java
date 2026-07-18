package com.universeexe.verity.event;

import com.universeexe.verity.data.VerityPlayerData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerDataHandler {
    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        VerityPlayerData.copy(event.getOriginal(), event.getEntity());
    }
}
