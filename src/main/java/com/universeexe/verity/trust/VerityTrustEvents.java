package com.universeexe.verity.trust;

import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.registry.VerityItems;
import com.universeexe.verity.voice.VerityVoiceMemory;
import com.universeexe.verity.voice.VerityVoiceMemoryFlag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-side trust hooks: tick countdown, liked-item gifts, dangerous drops, abandon check.
 */
public final class VerityTrustEvents {
    public static final TagKey<Item> LIKED_ITEMS = TagKey.create(
            Registries.ITEM, new ResourceLocation("universe_verity", "verity_liked_items"));

    private int abandonCheckCooldown;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            Optional<VerityEntity> verity = VerityTrustManager.findOwnedVerity(player);
            VerityTrustManager.tickCountdown(player.serverLevel(), player, verity.orElse(null));
        }
        if (++abandonCheckCooldown >= 200) {
            abandonCheckCooldown = 0;
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                checkAbandon(player);
            }
        }
    }

    private void checkAbandon(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        Optional<VerityEntity> verityOpt = VerityTrustManager.findOwnedVerity(player);
        if (verityOpt.isEmpty()) {
            return;
        }
        VerityEntity verity = verityOpt.get();
        if (!verity.isAlive() || verity.level() != player.level()) {
            return;
        }
        if (player.distanceTo(verity) < VerityTrustKeys.ABANDON_DISTANCE) {
            return;
        }
        // Same dimension, intentionally far for a full Minecraft day.
        var tag = VerityPlayerData.get(player);
        long day = player.serverLevel().getDayTime() / VerityTrustKeys.DAY_TICKS;
        long mark = tag.getLong(VerityTrustKeys.ABANDON_DAY_MARK);
        if (mark == 0L) {
            tag.putLong(VerityTrustKeys.ABANDON_DAY_MARK, day);
            return;
        }
        if (day > mark) {
            tag.putLong(VerityTrustKeys.ABANDON_DAY_MARK, day);
            VerityTrustManager.addTrustDefault(player, verity, TrustReason.ABANDONED_VERITY);
        }
    }

    @SubscribeEvent
    public void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (!(event.getTarget() instanceof VerityEntity verity)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.getUUID().equals(verity.getOwnerUUID())) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.is(LIKED_ITEMS)) {
            return;
        }
        if (VerityTrustManager.addTrustDefault(player, verity, TrustReason.GAVE_LIKED_ITEM)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
    }

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // If Verity ball item remains inside a non-player container after close → stored.
        if (event.getContainer() == player.inventoryMenu) {
            return;
        }
        for (ItemStack stack : event.getContainer().getItems()) {
            if (stack.is(VerityItems.VERITY_ITEM.get())) {
                onStoredInContainer(player);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof VerityEntity verity)) {
            return;
        }
        if (event.getDistance() < 6.0f) {
            return;
        }
        UUID owner = verity.getOwnerUUID();
        if (owner == null) {
            return;
        }
        ServerPlayer player = verity.level().getServer() == null ? null
                : verity.level().getServer().getPlayerList().getPlayer(owner);
        if (player != null) {
            VerityTrustManager.addTrustDefault(player, verity, TrustReason.DANGEROUS_DROP);
        }
    }

    /**
     * Called when Verity ball item is stored into a container inventory.
     */
    public static void onStoredInContainer(ServerPlayer player) {
        Optional<VerityEntity> verity = VerityTrustManager.findOwnedVerity(player);
        VerityTrustManager.addTrustDefault(player, verity.orElse(null), TrustReason.STORED_IN_CONTAINER);
    }

    public static void onOpenedBox(ServerPlayer player, VerityEntity verity) {
        VerityTrustManager.addTrustDefault(player, verity, TrustReason.OPENED_BOX);
        VerityVoiceMemory.forPlayer(player).setFlag(player, VerityVoiceMemoryFlag.PLAYER_OPENED_BOX);
    }

    public static void onGentlePickup(ServerPlayer player, VerityEntity verity) {
        VerityTrustManager.addTrustDefault(player, verity, TrustReason.GENTLE_PICKUP);
    }

    public static void onFirstGreeting(ServerPlayer player, @Nullable VerityEntity verity) {
        VerityTrustManager.addTrustDefault(player, verity, TrustReason.FIRST_GREETING);
        VerityVoiceMemory.forPlayer(player).setFlag(player, VerityVoiceMemoryFlag.PLAYER_FIRST_GREETING);
    }

    public static void onPlayerHitVerity(ServerPlayer player, @Nullable VerityEntity verity) {
        VerityTrustManager.addTrustDefault(player, verity, TrustReason.HIT_VERITY);
        VerityVoiceMemory.forPlayer(player).setFlag(player, VerityVoiceMemoryFlag.PLAYER_HIT_VERITY);
    }

    public static void onPlayerDroppedVerity(ServerPlayer player) {
        VerityVoiceMemory.forPlayer(player).setFlag(player, VerityVoiceMemoryFlag.PLAYER_DROPPED_VERITY);
    }

    public static void onPlayerAbandonedVerity(ServerPlayer player) {
        VerityVoiceMemory.forPlayer(player).setFlag(player, VerityVoiceMemoryFlag.PLAYER_ABANDONED_VERITY);
    }
}
