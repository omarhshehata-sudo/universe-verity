package com.universeexe.verity.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Monster / demon form used by the trust countdown transformation.
 * Prioritizes pursuing its owner; does not freely hunt unrelated players.
 */
public class VerityDemonEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> DATA_ANIM =
            SynchedEntityData.defineId(VerityDemonEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID ownerUuid;

    public VerityDemonEntity(EntityType<? extends VerityDemonEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setNoGravity(false);
    }

    @Override
    public boolean isNoGravity() {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                player -> ownerUuid != null && ownerUuid.equals(player.getUUID())));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ANIM, "idle");
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUuid = uuid;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUuid;
    }

    public void setVisualAnimation(String name) {
        this.entityData.set(DATA_ANIM, name == null || name.isBlank() ? "idle" : name);
    }

    public String getVisualAnimation() {
        return this.entityData.get(DATA_ANIM);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && ownerUuid != null && this.tickCount % 20 == 0) {
            ServerPlayer owner = this.level().getServer() == null ? null
                    : this.level().getServer().getPlayerList().getPlayer(ownerUuid);
            if (owner != null && owner.level() == this.level() && this.distanceTo(owner) > 4.0f) {
                this.getNavigation().moveTo(owner, 1.15D);
                setVisualAnimation("walk");
            } else if (!this.getNavigation().isInProgress()) {
                setVisualAnimation("idle");
            }
        }
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("VisualAnimation", getVisualAnimation());
        if (ownerUuid != null) {
            tag.putUUID("OwnerUUID", ownerUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("VisualAnimation")) {
            setVisualAnimation(tag.getString("VisualAnimation"));
        }
        if (tag.hasUUID("OwnerUUID")) {
            ownerUuid = tag.getUUID("OwnerUUID");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::predicate));
    }

    private PlayState predicate(AnimationState<VerityDemonEntity> state) {
        String anim = getVisualAnimation();
        String name = switch (anim) {
            case "walk", "chase", "attack", "crawl", "climb", "leap", "death" -> anim;
            default -> "idle";
        };
        if (state.isMoving() && "idle".equals(name)) {
            name = "walk";
        }
        state.getController().setAnimation(RawAnimation.begin().thenLoop(name));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
