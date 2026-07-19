package com.universeexe.verity.entity;

import com.universeexe.verity.config.VerityCommonConfig;
import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.registry.VerityEntities;
import com.universeexe.verity.registry.VeritySounds;
import com.universeexe.verity.util.VerityDebug;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class VerityBoxEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<String> DATA_ANIMATION =
            SynchedEntityData.defineId(VerityBoxEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_REVEAL_STAGE =
            SynchedEntityData.defineId(VerityBoxEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_ANIM_TOKEN =
            SynchedEntityData.defineId(VerityBoxEntity.class, EntityDataSerializers.INT);

    /** verity-5.7.3 JAR: spawn + box_open + discard at tick 40 from click. */
    public static final int JAR_SPAWN_TICK = 40;
    /** JAR scheduled impacts from click (also at +15/+35/+50 from spawn). */
    public static final int JAR_IMPACT_1_TICK = 55;
    public static final int JAR_IMPACT_0_TICK = 75;
    public static final int JAR_IMPACT_2_TICK = 90;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID ownerUuid;
    private VerityBoxStage introStage = VerityBoxStage.INITIAL_SILENCE;
    private int stageTicks;
    private boolean introCompleted;
    private VerityRevealStage revealStage = VerityRevealStage.SEALED;
    private int revealTicks;
    private String lastVoiceLine = "";
    private int idleCooldown;
    private int interactionCooldown;
    private int voiceBusyTicks;
    private int knockFollowupTicks;
    private int progressionVersion = 1;
    private boolean debugState;
    private double spawnX, spawnY, spawnZ;
    private float originalFacing;
    private int clientAnimToken = -1;
    private String clientPlayingAnim = "idle";
    private boolean revealReactPlayed;
    private boolean revealOpenPlayed;
    private boolean revealSpawnHandled;
    private boolean revealImpact1Played;
    private boolean revealImpact0Played;
    private boolean revealImpact2Played;
    @Nullable
    private UUID spawnedVerityUuid;
    /** Prevents muffled waiting lines from overlapping the reveal click. */
    private boolean waitingDialogueStopped;
    /** Server-side lock so one-shots finish before the next trigger (stops mid-shake restarts). */
    private int animLockTicks;
    /** Prevents SFX spam overlapping a still-playing movement clip. */
    private int sfxCooldownTicks;

    public VerityBoxEntity(EntityType<? extends VerityBoxEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ANIMATION, "idle");
        this.entityData.define(DATA_REVEAL_STAGE, VerityRevealStage.SEALED.name());
        this.entityData.define(DATA_ANIM_TOKEN, 0);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.level().isClientSide) {
            return;
        }

        if (interactionCooldown > 0) {
            interactionCooldown--;
        }
        if (voiceBusyTicks > 0) {
            voiceBusyTicks--;
        }
        if (animLockTicks > 0) {
            animLockTicks--;
        }
        if (sfxCooldownTicks > 0) {
            sfxCooldownTicks--;
        }
        if (knockFollowupTicks > 0) {
            knockFollowupTicks--;
            if (knockFollowupTicks == 0) {
                playMovement(VeritySounds.BOX_KNOCK_2.get(), 0.62f, 1.04f, 6);
                triggerAnimation("knock", true);
            }
        }

        if (revealStage == VerityRevealStage.SEALED) {
            if (!introCompleted) {
                VerityBoxSequence.tick(this);
            } else if (VerityCommonConfig.IDLE_CALLING_ENABLED.get()) {
                tickIdle();
            }
        } else if (revealStage == VerityRevealStage.ERROR_RECOVERY) {
            // Failed reveal must not permanently silence the sealed box.
            resumeSealedIdleAfterFailedReveal();
        } else if (revealStage != VerityRevealStage.REVEAL_COMPLETE) {
            tickReveal();
        }
    }

    /**
     * Forever while sealed: activity burst → ~5–6s quiet → burst again.
     * No max-event / give-up timeout — only reveal stops this loop.
     */
    private void tickIdle() {
        sanitizeIdleCooldown();
        if (idleCooldown > 0) {
            idleCooldown--;
            return;
        }
        // Wait out the current burst (voice / anim / sfx) without arming a huge cooldown.
        if (isVoiceBusy() || animLockTicks > 0 || sfxCooldownTicks > 0) {
            return;
        }
        // Keep calling / shaking until the box is opened — even if the owner walks a bit away.
        ServerPlayer owner = findOwner();
        boolean ownerNearby = owner != null
                && owner.distanceTo(this) <= VerityCommonConfig.MAXIMUM_HEARING_DISTANCE.get();

        // Fixed sealed voice sequence already ran; idle is visual agitation only (shake SFX silenced).
        int roll = this.random.nextInt(100);
        float near = ownerNearby ? 1.0f : 0.55f;
        if (!ownerNearby) {
            if (roll < 50) {
                playMovement(VeritySounds.BOX_RUSTLE_1.get(), 0.28f * near, 1.0f, 10);
                triggerAnimation("rustle_small");
            } else if (roll < 80) {
                playMovement(VeritySounds.BOX_KNOCK_1.get(), 0.32f * near, 1.0f, 8);
                triggerAnimation("knock");
            } else {
                playMovement(VeritySounds.BOX_SHIFT.get(), 0.30f * near, 1.0f, 10);
                triggerAnimation("shift");
            }
        } else if (roll < 30) {
            playMovement(VeritySounds.BOX_RUSTLE_1.get(), 0.48f, 1.0f, 10);
            triggerAnimation("rustle_small");
        } else if (roll < 55) {
            playMovement(VeritySounds.BOX_THUMP.get(), 0.42f, 0.98f, 14);
            triggerAnimation("shake");
        } else if (roll < 78) {
            playMovement(VeritySounds.BOX_KNOCK_1.get(), 0.58f, 1.0f, 8);
            triggerAnimation("knock");
            scheduleKnockFollowup(7);
        } else {
            playMovement(VeritySounds.BOX_SHIFT.get(), 0.52f, 1.0f, 10);
            triggerAnimation("shift");
        }
        armIdlePauseAfterBurst();
    }

    private void resumeSealedIdleAfterFailedReveal() {
        setRevealStage(VerityRevealStage.SEALED);
        introCompleted = true;
        revealReactPlayed = false;
        revealOpenPlayed = false;
        revealSpawnHandled = false;
        revealImpact1Played = false;
        revealImpact0Played = false;
        revealImpact2Played = false;
        spawnedVerityUuid = null;
        revealTicks = 0;
        knockFollowupTicks = 0;
        voiceBusyTicks = 0;
        sfxCooldownTicks = 0;
        animLockTicks = 0;
        resetIdleCooldown();
        triggerAnimation("idle");
        VerityDebug.log("Reveal failed; resuming sealed idle agitation for box {}", this.getUUID());
    }

    private void tickReveal() {
        revealTicks++;
        int spawnAt = VerityCommonConfig.VERITY_SPAWN_TICK.get();
        int removeAt = VerityCommonConfig.BOX_REMOVAL_TICK.get();

        if (revealStage == VerityRevealStage.REVEAL_STARTING && revealTicks >= 1) {
            setRevealStage(VerityRevealStage.BOX_OPENING);
        }
        if (revealStage == VerityRevealStage.BOX_OPENING && revealTicks >= spawnAt && !revealSpawnHandled) {
            revealSpawnHandled = true;
            setRevealStage(VerityRevealStage.VERITY_SPAWNING);
            if (!spawnVerity()) {
                setRevealStage(VerityRevealStage.ERROR_RECOVERY);
                revealOpenPlayed = false;
                revealSpawnHandled = false;
                revealReactPlayed = false;
                VerityDebug.warn("Verity spawn failed for box {}", this.getUUID());
                return;
            }
            if (!revealOpenPlayed) {
                revealOpenPlayed = true;
                playMovement(VeritySounds.REVEAL_BOX_OPEN.get(), 1.0f, 1.0f, 24);
            }
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.POOF,
                        this.getX(),
                        this.getY() + 1.0D,
                        this.getZ(),
                        20,
                        0.25D,
                        0.25D,
                        0.25D,
                        0.02D
                );
            }
        }
        playScheduledRevealImpacts();
        if (revealStage == VerityRevealStage.VERITY_SPAWNING && revealTicks >= removeAt) {
            setRevealStage(VerityRevealStage.REVEAL_COMPLETE);
            this.discard();
        }
    }

    /** JAR ModEvents: impact_1 @55, impact_0 @75, impact_2 @90 from box click. */
    private void playScheduledRevealImpacts() {
        VerityEntity verity = findSpawnedVerity();
        if (verity == null) {
            return;
        }
        if (!revealImpact1Played && revealTicks >= JAR_IMPACT_1_TICK) {
            revealImpact1Played = true;
            verity.playRevealImpact(VeritySounds.REVEAL_IMPACT_1.get());
        }
        if (!revealImpact0Played && revealTicks >= JAR_IMPACT_0_TICK) {
            revealImpact0Played = true;
            verity.playRevealImpact(VeritySounds.REVEAL_IMPACT_0.get());
        }
        if (!revealImpact2Played && revealTicks >= JAR_IMPACT_2_TICK) {
            revealImpact2Played = true;
            verity.playRevealImpact(VeritySounds.REVEAL_IMPACT_2.get());
        }
    }

    @Nullable
    private VerityEntity findSpawnedVerity() {
        if (spawnedVerityUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(spawnedVerityUuid);
        return entity instanceof VerityEntity verity ? verity : null;
    }

    private boolean spawnVerity() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        ServerPlayer owner = findOwner();
        if (owner == null) {
            VerityDebug.log("Owner absent during reveal; pausing before spawn");
            revealTicks = Math.min(revealTicks, VerityCommonConfig.VERITY_SPAWN_TICK.get() - 1);
            setRevealStage(VerityRevealStage.BOX_OPENING);
            return false;
        }
        Optional<UUID> existing = VerityPlayerData.getVerityUuid(owner);
        if (existing.isPresent()) {
            Entity found = serverLevel.getEntity(existing.get());
            if (found instanceof VerityEntity) {
                VerityDebug.log("Verity already exists for owner {}", owner.getGameProfile().getName());
                VerityPlayerData.markRevealComplete(owner, existing.get());
                return true;
            }
        }

        VerityEntity verity = VerityEntities.VERITY.get().create(serverLevel);
        if (verity == null) {
            return false;
        }
        BlockPos spawnPos = this.blockPosition();
        verity.moveTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                this.getYRot(),
                0.0F
        );
        verity.setOwnerUUID(this.ownerUuid);
        verity.setYBodyRot(this.getYRot());
        verity.setYHeadRot(this.getYRot());
        if (!serverLevel.addFreshEntity(verity)) {
            return false;
        }
        VerityPlayerData.markRevealComplete(owner, verity.getUUID());
        VerityPlayerData.setGreetingPlayed(owner, false);
        VerityPlayerData.setGreetingCompleted(owner, false);
        spawnedVerityUuid = verity.getUUID();
        verity.beginPostReveal(owner);
        VerityDebug.log("Spawned Verity {} for {}", verity.getUUID(), owner.getGameProfile().getName());
        return true;
    }

    public boolean beginReveal(ServerPlayer player) {
        if (revealStage != VerityRevealStage.SEALED && revealStage != VerityRevealStage.ERROR_RECOVERY) {
            return false;
        }
        if (!VerityCommonConfig.ENABLE_BOX_REVEAL.get()) {
            return false;
        }
        if (VerityCommonConfig.OWNER_ONLY_REVEAL.get() && (ownerUuid == null || !ownerUuid.equals(player.getUUID()))) {
            player.displayClientMessage(Component.translatable("message.universe_verity.not_your_box"), true);
            smallReaction();
            return false;
        }
        if (VerityCommonConfig.REVEAL_REQUIRES_EMPTY_HAND.get()
                && !player.getMainHandItem().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.universe_verity.use_empty_hand"), true);
            return false;
        }
        if (VerityPlayerData.isRevealCompleted(player) && VerityPlayerData.getVerityUuid(player).isPresent()) {
            return false;
        }

        VerityPlayerData.setRevealStarted(player, true);
        VerityBoxSequence.stopWaitingDialogue(this, player);
        introCompleted = true;
        voiceBusyTicks = 0;
        knockFollowupTicks = 0;
        sfxCooldownTicks = 0;
        // Idle loop is gated by revealStage != SEALED — never use a permanent cooldown "give up".
        idleCooldown = 0;
        revealReactPlayed = false;
        revealOpenPlayed = false;
        revealSpawnHandled = false;
        revealImpact1Played = false;
        revealImpact0Played = false;
        revealImpact2Played = false;
        spawnedVerityUuid = null;
        setRevealStage(VerityRevealStage.REVEAL_STARTING);
        revealTicks = 0;
        playMovement(VeritySounds.BOX_CLICK.get(), 0.7f, 1.0f, 16);
        triggerAnimation("open", true);
        player.displayClientMessage(Component.translatable("message.universe_verity.reveal_started"), true);
        VerityDebug.log("Reveal started for {} on box {}", player.getGameProfile().getName(), this.getUUID());
        return true;
    }

    public void smallReaction() {
        if (interactionCooldown > 0 || revealStage.isRevealActive() || animLockTicks > 0) {
            return;
        }
        interactionCooldown = VerityCommonConfig.INTERACTION_COOLDOWN_TICKS.get();
        if (VerityCommonConfig.INTERACTION_RESPONSE_ENABLED.get()) {
            playMovement(VeritySounds.BOX_RUSTLE_2.get(), 0.42f, 1.0f, 12);
            triggerAnimation("interaction_reaction");
        }
    }

    private void playLocal(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (sound == null || this.level().isClientSide) {
            return;
        }
        // Keep volume in a sane Minecraft range (old maxDist multiplier made opens blast/distort).
        float vol = Math.min(0.95f, Math.max(0.05f, volume));
        this.level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.BLOCKS, vol, pitch);
    }

    /**
     * Movement / reveal SFX with a short cooldown so clips cannot stack every tick.
     * Shake/agitation clips (rustle, knock, shift, thump, reveal-movement) are silenced —
     * visual animations and muffled voice lines still run.
     */
    private void playMovement(net.minecraft.sounds.SoundEvent sound, float volume, float pitch, int cooldownTicks) {
        if (sfxCooldownTicks > 0) {
            return;
        }
        sfxCooldownTicks = Math.max(1, cooldownTicks);
        if (sound == null || isSilencedShakeSfx(sound)) {
            return;
        }
        playLocal(sound, volume, pitch);
    }

    private static boolean isSilencedShakeSfx(net.minecraft.sounds.SoundEvent sound) {
        return sound == VeritySounds.BOX_RUSTLE_1.get()
                || sound == VeritySounds.BOX_RUSTLE_2.get()
                || sound == VeritySounds.BOX_KNOCK_1.get()
                || sound == VeritySounds.BOX_KNOCK_2.get()
                || sound == VeritySounds.BOX_SHIFT.get()
                || sound == VeritySounds.BOX_THUMP.get()
                || sound == VeritySounds.REVEAL_MOVEMENT.get();
    }

    @Nullable
    public ServerPlayer findOwner() {
        if (ownerUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerUuid);
        if (entity instanceof ServerPlayer player) {
            return player;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    public void triggerAnimation(String name) {
        triggerAnimation(name, false);
    }

    public void triggerAnimation(String name, boolean force) {
        if (name == null || name.isEmpty()) {
            name = "idle";
        }
        if (!force && animLockTicks > 0 && !"open".equals(name)) {
            return;
        }
        this.entityData.set(DATA_ANIMATION, name);
        this.entityData.set(DATA_ANIM_TOKEN, this.entityData.get(DATA_ANIM_TOKEN) + 1);
        animLockTicks = animationLockTicks(name);
    }

    private static int animationLockTicks(String name) {
        return switch (name) {
            case "rustle_small" -> 14;
            case "knock" -> 9;
            case "shift" -> 12;
            case "shake" -> 34;
            case "interaction_reaction" -> 18;
            case "open" -> 40; // JAR open anim length = 2.0s
            default -> 0;
        };
    }

    public void resetIdleCooldown() {
        int min = VerityCommonConfig.clampedIdleMinSeconds() * 20;
        int max = VerityCommonConfig.clampedIdleMaxSeconds() * 20;
        idleCooldown = min + this.random.nextInt(Math.max(1, max - min + 1));
    }

    /** Quiet gap after the current burst finishes (defaults: 5–6 seconds). */
    private void armIdlePauseAfterBurst() {
        int min = VerityCommonConfig.clampedIdleMinSeconds() * 20;
        int max = VerityCommonConfig.clampedIdleMaxSeconds() * 20;
        int quiet = min + this.random.nextInt(Math.max(1, max - min + 1));
        int busy = Math.max(Math.max(animLockTicks, voiceBusyTicks), sfxCooldownTicks);
        idleCooldown = busy + quiet;
    }

    /** Caps absurd/persisted cooldowns (e.g. old Integer.MAX_VALUE "stop forever" values). */
    private void sanitizeIdleCooldown() {
        int maxQuiet = VerityCommonConfig.clampedIdleMaxSeconds() * 20;
        int maxAllowed = maxQuiet + 80; // burst length headroom
        if (idleCooldown < 0 || idleCooldown > maxAllowed) {
            resetIdleCooldown();
        }
    }

    public void replayIntro() {
        introCompleted = false;
        introStage = VerityBoxStage.INITIAL_SILENCE;
        stageTicks = 0;
        voiceBusyTicks = 0;
        lastVoiceLine = "";
        idleCooldown = 0;
        setRevealStage(VerityRevealStage.SEALED);
        revealTicks = 0;
        triggerAnimation("idle");
    }

    public void skipIntro() {
        introCompleted = true;
        introStage = VerityBoxStage.IDLE_CALLING;
        stageTicks = VerityBoxSequence.TICK_IDLE;
        voiceBusyTicks = 0;
        resetIdleCooldown();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (revealStage.isRevealActive()) {
            return InteractionResult.CONSUME;
        }
        if (VerityCommonConfig.ENABLE_BOX_REVEAL.get()
                && ownerUuid != null
                && ownerUuid.equals(player.getUUID())
                && revealStage == VerityRevealStage.SEALED) {
            beginReveal(serverPlayer);
            return InteractionResult.CONSUME;
        }
        smallReaction();
        if (ownerUuid == null || !ownerUuid.equals(player.getUUID())) {
            serverPlayer.displayClientMessage(Component.translatable("message.universe_verity.not_your_box"), true);
        } else {
            serverPlayer.displayClientMessage(Component.translatable("message.universe_verity.box_moves"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (VerityCommonConfig.PROTECT_INTRO_BOX.get()) {
            if (source.getEntity() instanceof Player player && !player.getAbilities().instabuild) {
                smallReaction();
                playMovement(VeritySounds.BOX_THUMP.get(), 0.36f, 0.85f, 10);
                return false;
            }
            if (!(source.getEntity() instanceof Player player && player.getAbilities().instabuild)) {
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean canBeCollidedWith() {
        // Let Verity fall beside the box without clipping through solid box geometry.
        if (revealStage.isRevealActive()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        // Stay put after placement.
    }

    @Override
    public boolean shouldShowName() {
        return debugState;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            ownerUuid = tag.getUUID("OwnerUUID");
        }
        introStage = VerityBoxStage.fromName(tag.getString("IntroStage"));
        stageTicks = tag.getInt("StageTicks");
        introCompleted = tag.getBoolean("IntroCompleted");
        revealStage = VerityRevealStage.fromName(tag.getString("RevealStage"));
        this.entityData.set(DATA_REVEAL_STAGE, revealStage.name());
        revealTicks = tag.getInt("RevealTicks");
        lastVoiceLine = tag.getString("LastVoiceLine");
        idleCooldown = tag.getInt("IdleCooldown");
        interactionCooldown = tag.getInt("InteractionCooldown");
        voiceBusyTicks = tag.getInt("VoiceBusyTicks");
        progressionVersion = tag.contains("ProgressionVersion") ? tag.getInt("ProgressionVersion") : 1;
        debugState = tag.getBoolean("DebugState");
        spawnX = tag.getDouble("SpawnX");
        spawnY = tag.getDouble("SpawnY");
        spawnZ = tag.getDouble("SpawnZ");
        originalFacing = tag.getFloat("OriginalFacing");
        if (tag.contains("Animation")) {
            this.entityData.set(DATA_ANIMATION, tag.getString("Animation"));
        }
        // Avoid overlapping audio immediately after load.
        if (voiceBusyTicks < 10) {
            voiceBusyTicks = 10;
        }
        // Drop legacy "stop forever" cooldowns so sealed boxes keep agitating after reload.
        sanitizeIdleCooldown();
        if (revealStage == VerityRevealStage.ERROR_RECOVERY) {
            resumeSealedIdleAfterFailedReveal();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putUUID("OwnerUUID", ownerUuid);
        }
        tag.putString("IntroStage", introStage.name());
        tag.putInt("StageTicks", stageTicks);
        tag.putBoolean("IntroCompleted", introCompleted);
        tag.putString("RevealStage", revealStage.name());
        tag.putInt("RevealTicks", revealTicks);
        tag.putString("LastVoiceLine", lastVoiceLine == null ? "" : lastVoiceLine);
        tag.putInt("IdleCooldown", idleCooldown);
        tag.putInt("InteractionCooldown", interactionCooldown);
        tag.putInt("VoiceBusyTicks", voiceBusyTicks);
        tag.putInt("ProgressionVersion", progressionVersion);
        tag.putBoolean("DebugState", debugState);
        tag.putDouble("SpawnX", spawnX);
        tag.putDouble("SpawnY", spawnY);
        tag.putDouble("SpawnZ", spawnZ);
        tag.putFloat("OriginalFacing", originalFacing);
        tag.putString("Animation", this.entityData.get(DATA_ANIMATION));
    }

    public void configureSpawn(UUID owner, float yRot) {
        this.ownerUuid = owner;
        this.setYRot(yRot);
        this.yRotO = yRot;
        this.originalFacing = yRot;
        this.spawnX = getX();
        this.spawnY = getY();
        this.spawnZ = getZ();
        playMovement(VeritySounds.BOX_THUMP.get(), 0.32f, 0.88f, 12);
    }

    // ---- Geo ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Slight blend window so bone snaps between one-shots feel physical, not instantaneous.
        controllers.add(new AnimationController<>(this, "main", 5, this::predicate));
    }

    private PlayState predicate(AnimationState<VerityBoxEntity> state) {
        int token = this.entityData.get(DATA_ANIM_TOKEN);
        // Only push a new RawAnimation when the server token changes.
        // Calling setAnimation every tick was restarting shake/open and causing visual glitches.
        if (token == clientAnimToken) {
            return PlayState.CONTINUE;
        }
        clientAnimToken = token;
        String anim = this.entityData.get(DATA_ANIMATION);
        clientPlayingAnim = anim == null || anim.isEmpty() ? "idle" : anim;
        AnimationController<?> controller = state.getController();
        controller.forceAnimationReset();
        String play = clientPlayingAnim;
        String full = "animation.verity_box." + play;
        if ("idle".equals(play)) {
            controller.setAnimation(RawAnimation.begin().thenLoop(full));
        } else if ("open".equals(play)) {
            controller.setAnimation(RawAnimation.begin().thenPlayAndHold(full));
        } else {
            controller.setAnimation(RawAnimation.begin().thenPlay(full).thenLoop("animation.verity_box.idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ---- accessors ----
    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUuid = uuid;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUuid;
    }

    public void setIntroStage(VerityBoxStage stage) {
        this.introStage = stage;
    }

    public VerityBoxStage getIntroStage() {
        return introStage;
    }

    public void setStageTicks(int ticks) {
        this.stageTicks = ticks;
    }

    public int getStageTicks() {
        return stageTicks;
    }

    public void setIntroCompleted(boolean value) {
        this.introCompleted = value;
    }

    public boolean isIntroCompleted() {
        return introCompleted;
    }

    public void setRevealStage(VerityRevealStage stage) {
        this.revealStage = stage;
        this.entityData.set(DATA_REVEAL_STAGE, stage.name());
    }

    public VerityRevealStage getRevealStage() {
        return revealStage;
    }

    public int getRevealTicks() {
        return revealTicks;
    }

    public int nextBoxRandomInt(int bound) {
        return this.random.nextInt(bound);
    }

    public boolean isWaitingDialogueStopped() {
        return waitingDialogueStopped;
    }

    public void setWaitingDialogueStopped(boolean waitingDialogueStopped) {
        this.waitingDialogueStopped = waitingDialogueStopped;
    }

    public void setLastVoiceLine(String id) {
        this.lastVoiceLine = id;
    }

    public String getLastVoiceLine() {
        return lastVoiceLine;
    }

    public int getIdleCooldown() {
        return idleCooldown;
    }

    public void setVoiceBusyTicks(int ticks) {
        this.voiceBusyTicks = ticks;
    }

    public boolean isVoiceBusy() {
        return voiceBusyTicks > 0;
    }

    public void scheduleKnockFollowup(int ticks) {
        this.knockFollowupTicks = ticks;
    }

    public void setDebugState(boolean value) {
        this.debugState = value;
        this.setCustomNameVisible(value);
        if (value) {
            this.setCustomName(Component.literal("Verity Intro Debug"));
        }
    }

    public String getSyncedAnimation() {
        return this.entityData.get(DATA_ANIMATION);
    }
}
