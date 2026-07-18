package com.universeexe.verity.entity;

import com.universeexe.verity.animation.VerityExpressionState;
import com.universeexe.verity.config.VerityCommonConfig;
import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.item.VerityItem;
import com.universeexe.verity.registry.VerityItems;
import com.universeexe.verity.registry.VeritySounds;
import com.universeexe.verity.util.VerityDebug;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.UUID;

/**
 * Ball-form Verity. Visuals use the verity-5.7.2 procedural sphere + face textures
 * (see {@link com.universeexe.verity.client.render.VerityRenderer}).
 */
public class VerityEntity extends PathfinderMob {
    public static final float TARGET_WIDTH = 0.60f;
    public static final float TARGET_HEIGHT = 0.60f;
    /** Legacy Figura mesh diameter retained for size debug strings. */
    public static final float SOURCE_MESH_BLOCKS = 11.0f / 16.0f;
    /** Matches verity-5.7.2 bounce curve length (client). */
    public static final int BOUNCE_DURATION_TICKS = 50;

    private static final EntityDataAccessor<String> DATA_ANIMATION =
            SynchedEntityData.defineId(VerityEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_ANIM_TOKEN =
            SynchedEntityData.defineId(VerityEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_EXPRESSION =
            SynchedEntityData.defineId(VerityEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_VISUAL_DEBUG =
            SynchedEntityData.defineId(VerityEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOUNCE_START =
            SynchedEntityData.defineId(VerityEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_TALKING =
            SynchedEntityData.defineId(VerityEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_FACE_VARIANT =
            SynchedEntityData.defineId(VerityEntity.class, EntityDataSerializers.STRING);

    private static final int GREETING_DURATION_TICKS = 128;

    @Nullable
    private UUID ownerUuid;
    private boolean revealCompleted = true;
    private boolean greetingStarted;
    private boolean greetingCompleted;
    private int greetingStageTicks;
    private int faceTicksRemaining;
    private float targetYRot;
    private int interactionCooldown;
    private int progressionVersion = 1;
    private boolean pendingGreeting;
    private int talkTicksRemaining;
    /** After greeting/talk: wait until settled, then force JAR default friendly smile ({@code happy}). */
    private boolean pendingDefaultSmile;

    // Client-only cosmetic state
    private int clientBlinkCooldown;
    private int clientBlinkTicksRemaining;
    private boolean clientLongBlink;
    private VerityExpressionState clientForcedExpression;
    public int clientBounceTicks = -1;
    public int clientIntroTicks;
    public int clientIntroDelay;
    public float clientRollAngle;
    public float clientRollAngleO;

    public VerityEntity(EntityType<? extends VerityEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
        this.xpReward = 0;
        if (level.isClientSide) {
            this.clientBlinkCooldown = 60 + level.random.nextInt(80);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        // Intentionally empty — stationary story entity by default.
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ANIMATION, "idle");
        this.entityData.define(DATA_ANIM_TOKEN, 0);
        // JAR default idle face is happy (smiley), not blank/neutral.
        this.entityData.define(DATA_EXPRESSION, VerityExpressionState.HAPPY.id());
        this.entityData.define(DATA_VISUAL_DEBUG, false);
        this.entityData.define(DATA_BOUNCE_START, -1000);
        this.entityData.define(DATA_TALKING, false);
        this.entityData.define(DATA_FACE_VARIANT, "auto");
    }

    public void beginPostReveal(ServerPlayer owner) {
        this.ownerUuid = owner.getUUID();
        this.revealCompleted = true;
        this.pendingGreeting = VerityCommonConfig.ENABLE_GREETING.get();
        this.greetingStarted = false;
        this.greetingCompleted = false;
        this.greetingStageTicks = -VerityCommonConfig.GREETING_DELAY_TICKS.get();
        this.faceTicksRemaining = 12;
        this.targetYRot = yawToward(owner);
        setExpression(VerityExpressionState.GREETING);
        triggerAnimation("reveal");
        triggerBounce();
    }

    public void triggerBounce() {
        this.entityData.set(DATA_BOUNCE_START, 1);
    }

    public int getBounceStartTick() {
        return this.entityData.get(DATA_BOUNCE_START);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (this.level().isClientSide && DATA_BOUNCE_START.equals(key) && getBounceStartTick() > 0) {
            this.clientBounceTicks = BOUNCE_DURATION_TICKS;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (VerityCommonConfig.KEEP_VERITY_STATIONARY_AFTER_REVEAL.get()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.getNavigation().stop();
        }

        if (this.level().isClientSide) {
            tickClientVisuals();
            return;
        }

        if (interactionCooldown > 0) {
            interactionCooldown--;
        }
        if (talkTicksRemaining > 0) {
            talkTicksRemaining--;
            if (talkTicksRemaining == 0) {
                setTalking(false);
                if (!pendingGreeting && !(greetingStarted && !greetingCompleted)) {
                    requestDefaultSmile();
                }
            }
        }

        if (getBounceStartTick() > 0 && this.tickCount > 100) {
            this.entityData.set(DATA_BOUNCE_START, -1000);
        }

        tryApplyDefaultSmile();

        ServerPlayer owner = findOwner();
        if (faceTicksRemaining > 0 && owner != null) {
            faceTicksRemaining--;
            targetYRot = yawToward(owner);
            float current = this.getYRot();
            float next = Mth.rotLerp(0.25f, current, targetYRot);
            this.setYRot(next);
            this.setYBodyRot(next);
            this.setYHeadRot(next);
        } else if (VerityCommonConfig.LOOK_AT_OWNER_AFTER_REVEAL.get() && owner != null
                && owner.distanceTo(this) < VerityCommonConfig.GREETING_HEARING_DISTANCE.get()) {
            float desired = yawToward(owner);
            float next = Mth.rotLerp(0.08f, this.getYRot(), desired);
            this.setYRot(next);
            this.setYBodyRot(next);
            this.setYHeadRot(next);
        }

        if (pendingGreeting || (greetingStarted && !greetingCompleted)) {
            greetingStageTicks++;
            if (!greetingStarted && greetingStageTicks >= 0) {
                startGreeting(owner);
            } else if (greetingStarted && !greetingCompleted && greetingStageTicks >= GREETING_DURATION_TICKS) {
                greetingCompleted = true;
                pendingGreeting = false;
                setTalking(false);
                requestDefaultSmile();
                if (owner != null) {
                    VerityPlayerData.setGreetingCompleted(owner, true);
                }
            }
        }
    }

    /**
     * JAR default idle face is {@code happy} ({@code VARIANT_DEFAULT} in verity-5.7.2).
     * Expression snaps back immediately when not talking; idle animation waits for settle.
     */
    private void requestDefaultSmile() {
        // Drop talking/listening leftovers immediately so the smiley shows while settling.
        setTalking(false);
        setExpression(VerityExpressionState.HAPPY);
        // Keep face variant on auto so /verity expression set can still remap textures.
        setFaceVariant("auto");
        pendingDefaultSmile = true;
        tryApplyDefaultSmile();
    }

    private void tryApplyDefaultSmile() {
        if (!pendingDefaultSmile) {
            return;
        }
        // Wait out the procedural bounce (50 ticks) before locking idle anim.
        if (getBounceStartTick() > 0 && this.tickCount <= BOUNCE_DURATION_TICKS + 2) {
            return;
        }
        applyDefaultSmile();
        pendingDefaultSmile = false;
    }

    private void applyDefaultSmile() {
        setTalking(false);
        setExpression(VerityExpressionState.HAPPY);
        setFaceVariant("auto");
        triggerAnimation("idle");
    }

    private void tickClientVisuals() {
        this.clientRollAngleO = this.clientRollAngle;
        double dx = this.getX() - this.xOld;
        double dz = this.getZ() - this.zOld;
        double distanceMoved = Math.sqrt(dx * dx + dz * dz);
        if (distanceMoved > 0.005) {
            this.clientRollAngle += (float) (distanceMoved * 150.0);
        }

        if (this.clientBounceTicks > 0) {
            this.clientBounceTicks--;
        } else if (this.clientBounceTicks == 0) {
            this.clientBounceTicks = -1;
            this.clientIntroDelay = 10;
        }
        if (this.clientIntroDelay > 0) {
            this.clientIntroDelay--;
            if (this.clientIntroDelay == 0) {
                this.clientIntroTicks = 90;
            }
        }
        if (this.clientIntroTicks > 0) {
            this.clientIntroTicks--;
        }

        tickClientBlink();
    }

    private void tickClientBlink() {
        if (clientForcedExpression != null && clientForcedExpression.isBlink()) {
            return;
        }
        VerityExpressionState base = getSyncedExpression();
        if (base == VerityExpressionState.GREETING || isVisuallyTalking()) {
            clientBlinkTicksRemaining = 0;
            return;
        }
        if (clientBlinkTicksRemaining > 0) {
            clientBlinkTicksRemaining--;
            return;
        }
        if (clientBlinkCooldown > 0) {
            clientBlinkCooldown--;
            return;
        }
        boolean longBlink = this.random.nextFloat() < 0.08f;
        clientLongBlink = longBlink;
        clientBlinkTicksRemaining = longBlink ? (7 + this.random.nextInt(5)) : (2 + this.random.nextInt(2));
        clientBlinkCooldown = 60 + this.random.nextInt(80);
        if (!longBlink && this.random.nextFloat() < 0.18f) {
            clientBlinkCooldown = 4 + this.random.nextInt(6);
        }
    }

    private void startGreeting(@Nullable ServerPlayer owner) {
        if (!VerityCommonConfig.ENABLE_GREETING.get()) {
            greetingStarted = true;
            greetingCompleted = true;
            pendingGreeting = false;
            requestDefaultSmile();
            return;
        }
        greetingStarted = true;
        greetingStageTicks = 0;
        setExpression(VerityExpressionState.GREETING);
        triggerAnimation("greeting");
        setTalking(true);
        talkTicksRemaining = GREETING_DURATION_TICKS;
        float volume = VerityCommonConfig.GREETING_VOLUME.get().floatValue()
                * (VerityCommonConfig.GREETING_HEARING_DISTANCE.get().floatValue() / 16f);
        this.level().playSound(null, getX(), getY(), getZ(),
                VeritySounds.GREETING_PERSONAL_HELPER.get(), SoundSource.NEUTRAL, volume, 1.0f);
        if (owner != null) {
            VerityPlayerData.setGreetingPlayed(owner, true);
        }
        VerityDebug.log("Played Verity greeting from {}", this.getUUID());
    }

    public void replayGreeting() {
        greetingStarted = false;
        greetingCompleted = false;
        pendingGreeting = true;
        greetingStageTicks = -5;
        setExpression(VerityExpressionState.GREETING);
    }

    public void stopGreeting() {
        greetingCompleted = true;
        pendingGreeting = false;
        setTalking(false);
        requestDefaultSmile();
    }

    private float yawToward(Player player) {
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        return (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
    }

    @Nullable
    public ServerPlayer findOwner() {
        if (ownerUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    public void triggerAnimation(String name) {
        this.entityData.set(DATA_ANIMATION, name);
        this.entityData.set(DATA_ANIM_TOKEN, this.entityData.get(DATA_ANIM_TOKEN) + 1);
        if ("reveal".equals(name)) {
            triggerBounce();
        }
        if ("talk".equals(name) || "greeting".equals(name)) {
            setTalking(true);
        } else if ("idle".equals(name) || "listening".equals(name)) {
            // leave talking to talkTicksRemaining unless greeting active
        }
    }

    public void setExpression(VerityExpressionState state) {
        this.entityData.set(DATA_EXPRESSION, state.id());
        if (this.level().isClientSide) {
            this.clientForcedExpression = null;
        }
    }

    public void setExpressionFromCommand(String id) {
        VerityExpressionState.fromId(id).ifPresent(state -> {
            // Cancel pending auto-happy so debug overrides are not overwritten next tick.
            pendingDefaultSmile = false;
            setFaceVariant("auto");
            setExpression(state);
        });
    }

    public void setFaceVariant(String variant) {
        this.entityData.set(DATA_FACE_VARIANT, variant == null || variant.isBlank() ? "auto" : variant);
    }

    public String getFaceVariant() {
        return this.entityData.get(DATA_FACE_VARIANT);
    }

    public void resetExpression() {
        applyDefaultSmile();
    }

    public VerityExpressionState getSyncedExpression() {
        return VerityExpressionState.fromId(this.entityData.get(DATA_EXPRESSION))
                .orElse(VerityExpressionState.HAPPY);
    }

    public VerityExpressionState getRenderExpression() {
        if (this.level().isClientSide) {
            if (clientForcedExpression != null) {
                return clientForcedExpression;
            }
            if (clientBlinkTicksRemaining > 0) {
                return clientLongBlink ? VerityExpressionState.LONG_BLINK : VerityExpressionState.BLINK;
            }
        }
        return getSyncedExpression();
    }

    public void setClientForcedExpression(@Nullable VerityExpressionState state) {
        this.clientForcedExpression = state;
    }

    public void setVisualDebug(boolean enabled) {
        this.entityData.set(DATA_VISUAL_DEBUG, enabled);
    }

    public boolean isVisualDebug() {
        return this.entityData.get(DATA_VISUAL_DEBUG);
    }

    public void setTalking(boolean talking) {
        this.entityData.set(DATA_TALKING, talking);
    }

    public boolean isTalking() {
        return this.entityData.get(DATA_TALKING);
    }

    /**
     * Talking mouth only while the synced talking flag is set.
     * Do not infer from leftover anim names or post-bounce intro ticks — those left a
     * talking mouth after speech ended.
     */
    public boolean isVisuallyTalking() {
        return isTalking();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (interactionCooldown > 0) {
            return InteractionResult.CONSUME;
        }

        // Empty-hand pickup → held item (verity-5.7.2 EntityInteract pathway).
        if (player.getItemInHand(hand).isEmpty()
                && ownerUuid != null
                && ownerUuid.equals(player.getUUID())) {
            if (isTalking()) {
                player.displayClientMessage(
                        Component.literal("\u00a7cYou can't do this while he's talking."), true);
                this.level().playSound(null, player.blockPosition(),
                        net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.9f);
                return InteractionResult.FAIL;
            }
            interactionCooldown = VerityCommonConfig.INTERACTION_COOLDOWN_TICKS.get();
            ItemStack stack = new ItemStack(VerityItems.VERITY_ITEM.get());
            VerityItem.writeVariant(stack, faceVariantForItem());
            stack.getOrCreateTag().putString(VerityItem.TAG_NAME, VerityItem.DISPLAY_NAME);
            stack.setHoverName(Component.literal(VerityItem.DISPLAY_NAME));
            this.level().playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            if (player instanceof ServerPlayer serverPlayer) {
                VerityPlayerData.setVerityUuid(serverPlayer, null);
            }
            this.discard();
            player.setItemInHand(hand, stack);
            player.swing(hand, true);
            player.displayClientMessage(Component.translatable("message.universe_verity.verity_picked_up"), true);
            return InteractionResult.CONSUME;
        }

        interactionCooldown = VerityCommonConfig.INTERACTION_COOLDOWN_TICKS.get();
        if (ownerUuid != null && ownerUuid.equals(player.getUUID())) {
            this.faceTicksRemaining = 8;
            this.targetYRot = yawToward(player);
            // Keep base smiley; talking flag swaps in happy_talking while speaking.
            setExpression(VerityExpressionState.HAPPY);
            triggerAnimation("talk");
            setTalking(true);
            talkTicksRemaining = 40;
            player.displayClientMessage(Component.translatable("message.universe_verity.verity_looks_at_you"), true);
        } else {
            setExpression(VerityExpressionState.HAPPY);
            triggerAnimation("idle");
        }
        return InteractionResult.CONSUME;
    }

    private String faceVariantForItem() {
        String override = getFaceVariant();
        if (override != null && !override.isBlank() && !"auto".equalsIgnoreCase(override)) {
            return override;
        }
        return switch (getSyncedExpression()) {
            case HAPPY, GREETING -> "happy";
            case LISTENING, WATCHING -> "serious_1";
            default -> "happy";
        };
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (VerityCommonConfig.PROTECT_VERITY.get()) {
            if (source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
                return super.hurt(source, amount);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) {
            tag.putUUID("OwnerUUID", ownerUuid);
        }
        tag.putBoolean("RevealCompleted", revealCompleted);
        tag.putBoolean("GreetingStarted", greetingStarted);
        tag.putBoolean("GreetingCompleted", greetingCompleted);
        tag.putInt("GreetingStageTicks", greetingStageTicks);
        tag.putBoolean("PendingGreeting", pendingGreeting);
        tag.putInt("ProgressionVersion", progressionVersion);
        tag.putString("CurrentAnimation", this.entityData.get(DATA_ANIMATION));
        tag.putString("CurrentExpression", this.entityData.get(DATA_EXPRESSION));
        tag.putString("FaceVariant", this.entityData.get(DATA_FACE_VARIANT));
        tag.putBoolean("InvulnerableStoryEntity", true);
        tag.putBoolean("StationaryIntroductionState", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            ownerUuid = tag.getUUID("OwnerUUID");
        }
        revealCompleted = !tag.contains("RevealCompleted") || tag.getBoolean("RevealCompleted");
        greetingStarted = tag.getBoolean("GreetingStarted");
        greetingCompleted = tag.getBoolean("GreetingCompleted");
        greetingStageTicks = tag.getInt("GreetingStageTicks");
        pendingGreeting = tag.getBoolean("PendingGreeting");
        progressionVersion = tag.contains("ProgressionVersion") ? tag.getInt("ProgressionVersion") : 1;
        if (tag.contains("CurrentAnimation")) {
            this.entityData.set(DATA_ANIMATION, tag.getString("CurrentAnimation"));
        }
        if (tag.contains("CurrentExpression")) {
            this.entityData.set(DATA_EXPRESSION, tag.getString("CurrentExpression"));
        }
        if (tag.contains("FaceVariant")) {
            this.entityData.set(DATA_FACE_VARIANT, tag.getString("FaceVariant"));
        }
        if (greetingStarted && !greetingCompleted) {
            greetingCompleted = true;
            pendingGreeting = false;
        }
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUuid = uuid;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUuid;
    }

    public boolean isGreetingStarted() {
        return greetingStarted;
    }

    public boolean isGreetingCompleted() {
        return greetingCompleted;
    }

    public boolean isRevealCompleted() {
        return revealCompleted;
    }

    public String getSyncedAnimation() {
        return this.entityData.get(DATA_ANIMATION);
    }

    public float getModelScaleX() {
        return TARGET_WIDTH / SOURCE_MESH_BLOCKS;
    }

    public float getModelScaleY() {
        return TARGET_HEIGHT / SOURCE_MESH_BLOCKS;
    }

    public float getModelScaleZ() {
        return TARGET_WIDTH / SOURCE_MESH_BLOCKS;
    }

    public String describeSize() {
        return String.format(Locale.ROOT,
                "sphere=%.2f hitbox=%.2fx%.2f sourceMesh=%.4f uniformScale=%.3f pipeline=procedural_sphere",
                TARGET_WIDTH, getBbWidth(), getBbHeight(),
                SOURCE_MESH_BLOCKS, getModelScaleX());
    }

}
