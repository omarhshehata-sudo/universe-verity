package com.universeexe.verity.event;

import com.universeexe.verity.animation.VerityExpressionState;
import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.item.VerityItem;
import com.universeexe.verity.item.VerityVariants;
import com.universeexe.verity.registry.VerityEntities;
import com.universeexe.verity.registry.VerityItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Throw / place held Verity — ported from verity-5.7.2 ModEvents
 * ({@code rightClickAir}, {@code onPlayerBlockInteract}).
 */
public final class VerityItemInteractions {
    public VerityItemInteractions() {
    }

    /**
     * Shift + right-click air: throw Verity with look-vector × 1.5
     * (JAR: {@code SoundEvents.ENDER_DRAGON_FLAP}).
     */
    @SubscribeEvent
    public void rightClickAir(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) {
            return;
        }
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(VerityItems.VERITY_ITEM.get())) {
            return;
        }
        // Prefer block-place when aiming at a block (avoids throw+place double spawn).
        HitResult hit = player.pick(player.getBlockReach(), 0.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return;
        }

        String variant = VerityVariants.fromStack(stack);
        stack.shrink(1);
        Vec3 launchVelocity = player.getLookAngle().normalize().scale(1.5);

        VerityEntity spawned = VerityEntities.VERITY.get().create(player.level());
        if (spawned == null) {
            return;
        }
        BlockPos spawnBlock = player.blockPosition().offset(0, 1, 0);
        spawned.setPos(Vec3.atCenterOf(spawnBlock));
        spawned.setFaceVariant(variant);
        spawned.setExpression(VerityExpressionState.HAPPY);
        spawned.setTalking(false);
        spawned.getPersistentData().putBoolean("WasThrown", true);
        spawned.setOwnerUUID(player.getUUID());
        player.level().addFreshEntity(spawned);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS, 1.0f, 1.0f);
        spawned.setDeltaMovement(launchVelocity);
        spawned.hasImpulse = true;

        if (player instanceof ServerPlayer serverPlayer) {
            VerityPlayerData.setVerityUuid(serverPlayer, spawned.getUUID());
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Right-click block: place Verity on the clicked face (must be air).
     */
    @SubscribeEvent
    public void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(VerityItems.VERITY_ITEM.get())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel().isClientSide) {
            return;
        }

        Direction face = event.getFace();
        BlockPos spawnPos = face != null ? event.getPos().relative(face) : event.getPos().above();
        ServerLevel level = (ServerLevel) event.getLevel();
        if (!level.getBlockState(spawnPos).isAir()) {
            return;
        }

        String variant = VerityVariants.fromStack(stack);
        player.swing(event.getHand(), true);
        stack.shrink(1);
        level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP,
                SoundSource.BLOCKS, 1.0f, 0.8f);

        VerityEntity spawned = VerityEntities.VERITY.get().create(level);
        if (spawned == null) {
            return;
        }
        spawned.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0f, 0.0f);
        spawned.setFaceVariant(variant);
        spawned.setExpression(VerityExpressionState.HAPPY);
        spawned.setTalking(false);
        spawned.getPersistentData().putBoolean("WasThrown", false);
        spawned.setOwnerUUID(player.getUUID());
        if (stack.hasTag() && stack.getTag() != null && stack.getTag().contains(VerityItem.TAG_NAME)) {
            spawned.setCustomName(net.minecraft.network.chat.Component.literal(
                    stack.getTag().getString(VerityItem.TAG_NAME)));
            spawned.setCustomNameVisible(true);
        }
        level.addFreshEntity(spawned);

        if (player instanceof ServerPlayer serverPlayer) {
            VerityPlayerData.setVerityUuid(serverPlayer, spawned.getUUID());
        }
    }
}
