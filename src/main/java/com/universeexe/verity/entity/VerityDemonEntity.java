package com.universeexe.verity.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

/**
 * Demon-form visuals from verity-5.7.2. Chase/AI goals are intentionally not ported —
 * idle/walk presentation only, spawnable via command for visual approval.
 */
public class VerityDemonEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> DATA_ANIM =
            SynchedEntityData.defineId(VerityDemonEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public VerityDemonEntity(EntityType<? extends VerityDemonEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // No chase / break / jump goals from the source JAR.
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ANIM, "idle");
    }

    public void setVisualAnimation(String name) {
        this.entityData.set(DATA_ANIM, name == null || name.isBlank() ? "idle" : name);
    }

    public String getVisualAnimation() {
        return this.entityData.get(DATA_ANIM);
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("VisualAnimation", getVisualAnimation());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("VisualAnimation")) {
            setVisualAnimation(tag.getString("VisualAnimation"));
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
