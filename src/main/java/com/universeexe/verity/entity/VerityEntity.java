package com.universeexe.verity.entity;

import com.universeexe.verity.animation.VerityExpressionState;
import com.universeexe.verity.trust.MoodState;
import com.universeexe.verity.config.VerityCommonConfig;
import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.item.VerityItem;
import com.universeexe.verity.registry.VerityItems;
import com.universeexe.verity.registry.VeritySounds;
import com.universeexe.verity.util.VerityDebug;
import com.universeexe.verity.voice.VerityQueuedVoiceEvent;
import com.universeexe.verity.voice.VerityVoiceCategory;
import com.universeexe.verity.voice.VerityVoiceContext;
import com.universeexe.verity.voice.VerityVoiceDirector;
import com.universeexe.verity.voice.VerityVoiceVariant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
import java.util.ArrayDeque;
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
    /**
     * Must be synched — Forge {@code persistentData} is server-only. 1.0.13 kept WasThrown in
     * persistentData, so clients still ran the stationary velocity lock every tick.
     */
    private static final EntityDataAccessor<Boolean> DATA_WAS_THROWN =
            SynchedEntityData.defineId(VerityEntity.class, EntityDataSerializers.BOOLEAN);
    /** Server-authoritative mood for face rendering (trust system). */
    private static final EntityDataAccessor<String> DATA_MOOD =
            SynchedEntityData.defineId(VerityEntity.class, EntityDataSerializers.STRING);

    private static final int GREETING_DURATION_TICKS = 128;
    /** Minimum ticks after throw before WasThrown may clear (prevents instant settle lock). */
    private static final int THROW_SETTLE_MIN_TICKS = 12;

    /** HELLO response busy ticks (clip length + small tail). */
    public static final int DUR_HELLO_HOPING = 58;
    public static final int DUR_HELLO_AGAIN = 25;
    public static final int DUR_YOUR_VOICE_SOUNDS_EXACTLY = 58;
    public static final int DUR_I_MEAN_IMAGINED_IT = 39;
    /** FOLLOW response busy ticks (clip length + small tail). */
    public static final int DUR_OKAY_WHERE_ARE_WE_GOING = 45;
    public static final int DUR_ALRIGHT_RIGHT_BEHIND_YOU = 60;
    public static final int DUR_LEAD_THE_WAY = 26;
    private static final int HELLO_GAP_AFTER_MAIN_TICKS = 8;
    private static final int HELLO_WHISPER_PAUSE_TICKS = 12;
    /** Hurt face duration after bounce/damage before returning to happy idle. */
    private static final int HURT_FACE_DURATION_TICKS = 200;

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
    /** Delayed fall bounce from verity-5.7.2 {@code causeFallDamage} (1-tick schedule). */
    private int pendingFallBounceTicks;
    private double pendingFallBounceY = -1.0;
    /** Brief hurt face after wall/fall bounce, then restore happy idle. */
    private int hurtFaceResetTicks;
    /** Server tick when setWasThrown(true) was last applied. */
    private int thrownAtTick = -1000;
    /** When true, Verity pathfinds toward the owning player and rolls while moving. */
    private boolean followingOwner;
    private int followRollAnimCooldown;
    /** Server-side sequenced voice lines (HELLO follow-up, etc.). */
    private final ArrayDeque<VoiceCue> voiceCueQueue = new ArrayDeque<>();
    private int voiceCueCooldownTicks;
    /** True while the voice director owns an active line on this entity. */
    private boolean voiceDirectorBusy;

    private record VoiceCue(net.minecraft.sounds.SoundEvent sound, int durationTicks, int pauseAfterTicks) {
    }

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
        // Match JAR: noCulling + persistence. Do NOT setNoAi — Mob.isEffectiveAi() gates
        // movement dampening oddly and JAR Verity has AI enabled with goals.
        this.noCulling = true;
        this.setPersistenceRequired();
        this.setInvulnerable(true);
        // Ball form always uses normal Minecraft gravity (break floor → fall).
        this.setNoGravity(false);
        this.xpReward = 0;
        if (level.isClientSide) {
            this.clientBlinkCooldown = 60 + level.random.nextInt(80);
        }
    }

    @Override
    public boolean isNoGravity() {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Match verity-5.7.2: health 20, movement 0.25, follow 32 (not 0.0 speed).
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
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
        this.entityData.define(DATA_WAS_THROWN, false);
        this.entityData.define(DATA_MOOD, MoodState.MEH.name());
    }

    public void setMoodState(MoodState mood) {
        this.entityData.set(DATA_MOOD, mood == null ? MoodState.MEH.name() : mood.name());
    }

    public MoodState getMoodState() {
        return MoodState.fromName(this.entityData.get(DATA_MOOD));
    }

    public void setWasThrown(boolean thrown) {
        this.entityData.set(DATA_WAS_THROWN, thrown);
        this.getPersistentData().putBoolean("WasThrown", thrown);
        if (thrown) {
            this.thrownAtTick = this.tickCount;
        }
    }

    public boolean isWasThrown() {
        return this.entityData.get(DATA_WAS_THROWN);
    }

    public void beginPostReveal(ServerPlayer owner) {
        this.ownerUuid = owner.getUUID();
        this.revealCompleted = true;
        this.pendingGreeting = false;
        this.greetingStarted = false;
        this.greetingCompleted = false;
        this.greetingStageTicks = 0;
        this.faceTicksRemaining = 12;
        this.targetYRot = yawToward(owner);
        setExpression(VerityExpressionState.GREETING);
        com.universeexe.verity.trust.MoodState mood =
                com.universeexe.verity.trust.VerityTrustManager.getMood(owner);
        setMoodState(mood);
        setFaceVariant(mood.legacyFaceVariant());
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
        // JAR: capture motion before LivingEntity collision so wall bounce can reverse it.
        Vec3 motionBeforeCollision = this.getDeltaMovement();
        super.tick();

        boolean wasThrown = isWasThrown();

        // CLIENT: never velocity-lock. WasThrown used to live only in server persistentData,
        // so clients always thought !wasThrown and zeroed throw momentum every tick (1.0.13).
        if (this.level().isClientSide) {
            tickClientVisuals();
            return;
        }

        // After bounce settles (with grace), clear WasThrown so look-at / optional stationary resume.
        if (wasThrown
                && this.onGround()
                && pendingFallBounceTicks <= 0
                && (this.tickCount - thrownAtTick) >= THROW_SETTLE_MIN_TICKS) {
            Vec3 m = this.getDeltaMovement();
            if (m.horizontalDistanceSqr() < 1.0E-3 && Math.abs(m.y) < 0.08) {
                setWasThrown(false);
                wasThrown = false;
            }
        }
        // Optional stationary lock: server-only, never while thrown or following.
        // Only damp horizontal motion while on ground — never zero Y or hold midair.
        // Breaking the block under Verity must let him fall with normal gravity.
        if (!wasThrown && !followingOwner && VerityCommonConfig.KEEP_VERITY_STATIONARY_AFTER_REVEAL.get()) {
            this.getNavigation().stop();
            if (this.onGround()) {
                Vec3 m = this.getDeltaMovement();
                this.setDeltaMovement(0.0, m.y, 0.0);
            }
        }

        if (followingOwner) {
            tickFollowOwner();
        }

        // Wall bounce — verity-5.7.2 VerityEntity.tick (horizontalCollision + 0.6 restitution).
        if (this.horizontalCollision) {
            double newX = this.getDeltaMovement().x;
            double newZ = this.getDeltaMovement().z;
            boolean bounced = false;
            if (Math.abs(motionBeforeCollision.x) > 0.1 && Math.abs(newX) < 0.02) {
                newX = -motionBeforeCollision.x * 0.6;
                bounced = true;
            }
            if (Math.abs(motionBeforeCollision.z) > 0.1 && Math.abs(newZ) < 0.02) {
                newZ = -motionBeforeCollision.z * 0.6;
                bounced = true;
            }
            if (bounced) {
                this.setDeltaMovement(newX, this.getDeltaMovement().y, newZ);
                this.hasImpulse = true;
                applyBounceHurtFace();
            }
        }

        // JAR: zero motion in void (Y <= -63).
        if (this.blockPosition().getY() <= -63) {
            this.setDeltaMovement(0.0, 0.0, 0.0);
            this.hasImpulse = true;
        }

        // 1-tick delayed fall bounce from causeFallDamage.
        if (pendingFallBounceTicks > 0) {
            pendingFallBounceTicks--;
            if (pendingFallBounceTicks == 0 && pendingFallBounceY >= 0.0) {
                this.setDeltaMovement(this.getDeltaMovement().x, pendingFallBounceY, this.getDeltaMovement().z);
                this.hasImpulse = true;
                this.setOnGround(false);
                applyBounceHurtFace();
                pendingFallBounceY = -1.0;
            }
        }
        if (hurtFaceResetTicks > 0) {
            hurtFaceResetTicks--;
            if (hurtFaceResetTicks == 0) {
                clearHurtFaceToHappy();
            }
        } else if ("hurt".equalsIgnoreCase(getFaceVariant())) {
            // Recover stuck hurt face (e.g. saved NBT without timer).
            clearHurtFaceToHappy();
        }

        if (interactionCooldown > 0) {
            interactionCooldown--;
        }
        tickVoiceCueQueue();
        if (talkTicksRemaining > 0) {
            talkTicksRemaining--;
            if (talkTicksRemaining == 0) {
                setTalking(false);
                if (!pendingGreeting && !(greetingStarted && !greetingCompleted) && voiceCueQueue.isEmpty()) {
                    requestDefaultSmile();
                }
            }
        }

        if (getBounceStartTick() > 0 && this.tickCount > 100) {
            this.entityData.set(DATA_BOUNCE_START, -1000);
        }

        tryApplyDefaultSmile();

        ServerPlayer owner = findOwner();
        // JAR: LookAtPlayerGoal disabled while WasThrown — skip owner-facing while airborne/thrown.
        if (!wasThrown) {
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

    private void applyBounceHurtFace() {
        applyHurtFace();
        this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 1.0f, 1.0f);
    }

    /**
     * Show hurt face, then auto-return to happy idle after {@link #HURT_FACE_DURATION_TICKS}.
     * Calling again while already hurt resets the 10s timer.
     */
    public void applyHurtFace() {
        if (this.level().isClientSide) {
            return;
        }
        pendingDefaultSmile = false;
        setTalking(false);
        talkTicksRemaining = 0;
        setFaceVariant("hurt");
        setExpression(VerityExpressionState.HAPPY);
        hurtFaceResetTicks = HURT_FACE_DURATION_TICKS;
    }

    private void clearHurtFaceToHappy() {
        hurtFaceResetTicks = 0;
        pendingDefaultSmile = false;
        setTalking(false);
        // Force happy texture (not only "auto") so clients never keep a stuck hurt PNG.
        setFaceVariant("happy");
        setExpression(VerityExpressionState.HAPPY);
        if (!isTalking() && voiceCueQueue.isEmpty()
                && !(greetingStarted && !greetingCompleted)) {
            triggerAnimation("idle");
        }
    }

    /**
     * Fall bounce — verity-5.7.2 {@code causeFallDamage}: after 1 tick,
     * {@code min(sqrt(fallDistance) * 0.22, 0.7)} upward impulse.
     */
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        if (!this.level().isClientSide) {
            if (fallDistance > 0.75f) {
                pendingFallBounceY = Math.min(Math.sqrt(fallDistance) * 0.22, 0.7);
                pendingFallBounceTicks = 1;
            }
            this.resetFallDistance();
        }
        return false;
    }

    /**
     * JAR default idle face is {@code happy} ({@code VARIANT_DEFAULT} in verity-5.7.2).
     * Expression snaps back immediately when not talking; idle animation waits for settle.
     */
    private void requestDefaultSmile() {
        // Keep hurt face until its own 10s timer expires.
        if (hurtFaceResetTicks > 0) {
            setTalking(false);
            pendingDefaultSmile = false;
            return;
        }
        // Drop talking/listening leftovers immediately so the smiley shows while settling.
        setTalking(false);
        setExpression(VerityExpressionState.HAPPY);
        // Keep face variant on auto so /verity expression set can still remap textures.
        setFaceVariant("auto");
        pendingDefaultSmile = true;
        tryApplyDefaultSmile();
    }

    private void tryApplyDefaultSmile() {
        if (!pendingDefaultSmile || hurtFaceResetTicks > 0) {
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
        if (hurtFaceResetTicks <= 0) {
            setFaceVariant("auto");
        }
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
                com.universeexe.verity.trust.VerityTrustEvents.onGentlePickup(serverPlayer, this);
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
                boolean damaged = super.hurt(source, amount);
                if (damaged && !this.level().isClientSide) {
                    applyHurtFace();
                    notifyTrustHit(source);
                }
                return damaged;
            }
            // Protected: still flash hurt face on player hits so he isn't expression-stuck.
            if (!this.level().isClientSide && source.getEntity() instanceof Player) {
                applyHurtFace();
                notifyTrustHit(source);
            }
            return false;
        }
        boolean damaged = super.hurt(source, amount);
        if (damaged && !this.level().isClientSide) {
            applyHurtFace();
            notifyTrustHit(source);
        }
        return damaged;
    }

    private void notifyTrustHit(DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (ownerUuid == null || !ownerUuid.equals(attacker.getUUID())) {
            return;
        }
        com.universeexe.verity.trust.VerityTrustManager.addTrustDefault(
                attacker, this, com.universeexe.verity.trust.TrustReason.HIT_VERITY);
    }

    @Override
    public boolean isPushable() {
        // JAR VerityEntity.isPushable() → true (needed so throw spawn isn't glued in the player).
        return true;
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
        tag.putInt("HurtFaceResetTicks", hurtFaceResetTicks);
        tag.putBoolean("WasThrown", isWasThrown());
        tag.putBoolean("FollowingOwner", followingOwner);
        tag.putBoolean("InvulnerableStoryEntity", true);
        tag.putBoolean("StationaryIntroductionState", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Clear any persisted NoGravity from older builds / tools.
        this.setNoGravity(false);
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
        if (tag.contains("HurtFaceResetTicks")) {
            hurtFaceResetTicks = Math.max(0, tag.getInt("HurtFaceResetTicks"));
        } else if ("hurt".equalsIgnoreCase(this.entityData.get(DATA_FACE_VARIANT))) {
            // Older saves could store hurt with no timer — expire after ~10s from load.
            hurtFaceResetTicks = HURT_FACE_DURATION_TICKS;
        }
        if (hurtFaceResetTicks <= 0 && "hurt".equalsIgnoreCase(this.entityData.get(DATA_FACE_VARIANT))) {
            clearHurtFaceToHappy();
        }
        if (tag.contains("WasThrown")) {
            setWasThrown(tag.getBoolean("WasThrown"));
        } else if (this.getPersistentData().getBoolean("WasThrown")) {
            setWasThrown(true);
        }
        followingOwner = tag.getBoolean("FollowingOwner");
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

    /**
     * Enable talking face + body stretch for a fixed tick window (voice-line reactions).
     * Clears any active hurt face so speech shows the talking expression.
     */
    public void beginTalkingForTicks(int ticks) {
        if (this.level().isClientSide) {
            return;
        }
        if (hurtFaceResetTicks > 0) {
            hurtFaceResetTicks = 0;
            setFaceVariant("auto");
        }
        pendingDefaultSmile = false;
        setExpression(VerityExpressionState.HAPPY);
        setTalking(true);
        this.talkTicksRemaining = Math.max(1, ticks);
        // Keep greeting anim name while the intro greeting is active.
        if (!(greetingStarted && !greetingCompleted)) {
            this.entityData.set(DATA_ANIMATION, "talk");
            this.entityData.set(DATA_ANIM_TOKEN, this.entityData.get(DATA_ANIM_TOKEN) + 1);
        }
    }

    /** Play a single Verity voice line with talking animation for {@code durationTicks}. */
    public void playVoiceLine(net.minecraft.sounds.SoundEvent sound, int durationTicks) {
        enqueueVoiceCue(sound, durationTicks, 0);
    }

    public void prepareForQuestGreeting() {
        if (this.level().isClientSide) {
            return;
        }
        this.faceTicksRemaining = 40;
        setExpression(VerityExpressionState.HAPPY);
        setFaceVariant("happy");
        triggerAnimation("greeting");
        setTalking(true);
    }

    /**
     * HELLO_VERITY response routed through Quest 2 voice director pools/conversations.
     */
    public void playHelloVoiceResponse(boolean includeWhisperFollowup) {
        if (this.level().isClientSide) {
            return;
        }
        ServerPlayer owner = findOwner();
        if (owner == null) {
            return;
        }
        clearVoiceCueQueue();
        com.universeexe.verity.quest.VerityQuestManager.handleHelloIntent(owner, this, "hello");
        VerityDebug.log("Hello voice response from {}", this.getUUID());
    }

    public boolean isVoiceDirectorBusy() {
        return voiceDirectorBusy;
    }

    public void setVoiceDirectorBusy(boolean voiceDirectorBusy) {
        this.voiceDirectorBusy = voiceDirectorBusy;
    }

    public void clearVoiceCueQueue() {
        voiceCueQueue.clear();
        voiceCueCooldownTicks = 0;
    }

    public void enqueueVoiceCue(net.minecraft.sounds.SoundEvent sound, int durationTicks, int pauseAfterTicks) {
        if (sound == null || this.level().isClientSide) {
            return;
        }
        voiceCueQueue.addLast(new VoiceCue(sound, Math.max(1, durationTicks), Math.max(0, pauseAfterTicks)));
        tryFlushVoiceCueQueue();
    }

    private void tickVoiceCueQueue() {
        if (voiceCueQueue.isEmpty()) {
            return;
        }
        ServerPlayer owner = findOwner();
        if (owner == null) {
            return;
        }
        if (!VerityVoiceDirector.isPlayerBusy(owner.getUUID()) && voiceCueCooldownTicks <= 0) {
            tryFlushVoiceCueQueue();
        }
        if (voiceCueCooldownTicks > 0) {
            voiceCueCooldownTicks--;
        }
    }

    private void tryFlushVoiceCueQueue() {
        if (voiceCueQueue.isEmpty() || this.level().isClientSide) {
            return;
        }
        ServerPlayer owner = findOwner();
        if (owner == null || VerityVoiceDirector.isPlayerBusy(owner.getUUID())) {
            return;
        }
        VerityQueuedVoiceEvent.Builder builder = VerityQueuedVoiceEvent.builder(
                "entity_cues",
                VerityVoiceCategory.PLAYER_INTERACTION,
                voiceContextForOwner(owner)
        );
        VoiceCue cue;
        while ((cue = voiceCueQueue.pollFirst()) != null) {
            String soundId = VeritySounds.resolveId(cue.sound());
            if (soundId == null) {
                continue;
            }
            builder.add(
                    VerityVoiceVariant.simple(
                            soundId,
                            soundId,
                            cue.durationTicks(),
                            VeritySounds.subtitleKeyFor(soundId),
                            0.95f,
                            1.0f
                    ),
                    cue.pauseAfterTicks()
            );
        }
        VerityVoiceDirector.requestEvent(owner, builder.build());
    }

    private VerityVoiceContext voiceContextForOwner(ServerPlayer owner) {
        return VerityVoiceContext.atEntity(
                owner,
                getId(),
                getX(),
                getY(),
                getZ(),
                SoundSource.NEUTRAL,
                false
        );
    }

    /**
     * Begin following the owning player. Clears stationary lock while active.
     * Visual roll comes from horizontal movement ({@link com.universeexe.verity.client.util.VerityRollCalculator}).
     */
    public void startFollowingOwner() {
        this.followingOwner = true;
        this.followRollAnimCooldown = 0;
        setWasThrown(false);
        setExpression(VerityExpressionState.HAPPY);
        triggerAnimation("roll_normal");
    }

    public void stopFollowing() {
        if (!this.followingOwner) {
            this.getNavigation().stop();
            return;
        }
        this.followingOwner = false;
        this.followRollAnimCooldown = 0;
        this.getNavigation().stop();
        triggerAnimation("stop_settle");
        requestDefaultSmile();
    }

    public boolean isFollowingOwner() {
        return followingOwner;
    }

    private void tickFollowOwner() {
        ServerPlayer owner = findOwner();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
            stopFollowing();
            return;
        }

        double distSq = this.distanceToSqr(owner);
        // Stay close: stop pathing inside ~2 blocks, resume beyond ~2.5 (hysteresis).
        if (distSq < 4.0) {
            this.getNavigation().stop();
            if (followRollAnimCooldown <= 0 && !"idle".equals(getSyncedAnimation())
                    && !"stop_settle".equals(getSyncedAnimation())) {
                triggerAnimation("idle");
            }
            return;
        }

        this.getNavigation().moveTo(owner, 1.15D);
        updateFollowRollAnimation();
    }

    private void updateFollowRollAnimation() {
        if (followRollAnimCooldown > 0) {
            followRollAnimCooldown--;
            return;
        }
        Vec3 motion = this.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 0.02 && !this.getNavigation().isInProgress()) {
            return;
        }
        String anim;
        if (horizontal < 0.08) {
            anim = "roll_slow";
        } else if (horizontal < 0.18) {
            anim = "roll_normal";
        } else {
            anim = "roll_fast";
        }
        if (!anim.equals(getSyncedAnimation())) {
            triggerAnimation(anim);
        }
        followRollAnimCooldown = 8;
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
