package com.universeexe.verity.item;

import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.registry.VerityEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * Held / dropped Verity ball (ported from verity-5.7.2 VerityItem).
 */
public class VerityItem extends Item {
    public static final String TAG_VARIANT = "VerityVariant";
    public static final String TAG_NAME = "VerityName";
    public static final String DISPLAY_NAME = "Verity\u2122";

    public VerityItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void writeVariant(ItemStack stack, String variant) {
        stack.getOrCreateTag().putString(TAG_VARIANT, VerityVariants.sanitize(variant));
    }

    public static String readVariant(ItemStack stack) {
        return VerityVariants.fromStack(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        // Match JAR: always show Verity™ (Unicode TRADE MARK SIGN).
        return Component.literal(DISPLAY_NAME);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level().isClientSide) {
            return false;
        }
        if (entity.onGround()) {
            CompoundTag data = entity.getPersistentData();
            int groundTicks = data.getInt("VerityGroundTicks") + 1;
            data.putInt("VerityGroundTicks", groundTicks);
            if (groundTicks >= 20) {
                ServerLevel level = (ServerLevel) entity.level();
                BlockPos pos = entity.blockPosition();
                VerityEntity spawned = VerityEntities.VERITY.get().create(level);
                if (spawned != null) {
                    spawned.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
                    spawned.setFaceVariant(readVariant(stack));
                    spawned.setExpression(com.universeexe.verity.animation.VerityExpressionState.HAPPY);
                    Player nearest = level.getNearestPlayer(entity, 16.0);
                    if (nearest != null) {
                        spawned.setOwnerUUID(nearest.getUUID());
                    }
                    level.addFreshEntity(spawned);
                    spawned.triggerBounce();
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, 0.8f);
                entity.discard();
                return true;
            }
        } else if (entity.getPersistentData().contains("VerityGroundTicks")) {
            entity.getPersistentData().putInt("VerityGroundTicks", 0);
        }
        // JAR: bounce out of void
        if (entity.blockPosition().getY() <= -63) {
            entity.setDeltaMovement(0.0, 1.0, 0.0);
            entity.hasImpulse = true;
        }
        return false;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new com.universeexe.verity.item.client.VerityItemRenderer();
            }
        });
    }
}
