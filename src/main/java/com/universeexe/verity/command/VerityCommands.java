package com.universeexe.verity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.universeexe.verity.animation.VerityExpressionState;
import com.universeexe.verity.config.VerityCommonConfig;
import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.entity.VerityBoxEntity;
import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.entity.VerityRevealStage;
import com.universeexe.verity.registry.VerityEntities;
import com.universeexe.verity.registry.VeritySounds;
import com.universeexe.verity.util.SafeBoxPlacement;
import com.universeexe.verity.util.VerityDebug;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class VerityCommands {
    private static final Set<String> BOX_ANIMS = Set.of(
            "idle", "shake", "open", "rustle_small", "knock", "shift", "interaction_reaction"
    );
    private static final Set<String> VERITY_ANIMS = Set.of(
            "idle", "reveal", "talk", "greeting", "listening",
            "blink", "long_blink", "roll_slow", "roll_normal", "roll_fast", "stop_settle"
    );
    private static final SuggestionProvider<CommandSourceStack> SOUND_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(VeritySounds.all().keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> ANIM_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(BOX_ANIMS, builder);
    private static final SuggestionProvider<CommandSourceStack> VERITY_ANIM_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(VERITY_ANIMS, builder);
    private static final SuggestionProvider<CommandSourceStack> EXPRESSION_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(VerityExpressionState.COMMAND_CHOICES)
                            .map(VerityExpressionState::id)
                            .collect(Collectors.toList()),
                    builder);

    private VerityCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("verity")
                .then(Commands.literal("intro")
                        .then(Commands.literal("spawn").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> spawnIntro(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> resetIntro(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("replay").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> replayIntro(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("remove").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> removeBox(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("status")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> status(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("skip").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> skipIntro(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("box")
                        .then(Commands.literal("locate")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> locateBox(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("animate").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("animation", StringArgumentType.word())
                                                .suggests(ANIM_SUGGESTIONS)
                                                .executes(ctx -> animateBox(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "animation")))))))
                .then(Commands.literal("reveal")
                        .then(Commands.literal("trigger").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> triggerReveal(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> resetReveal(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("status")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> revealStatus(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("skipanimation").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> skipRevealAnim(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("entity")
                        .then(Commands.literal("locate")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> locateVerity(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("remove").requires(s -> s.hasPermission(2))
                                .executes(ctx -> removeNearestOrSelf(ctx.getSource()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> removeVerity(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("respawn").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> respawnVerity(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("spawnball").requires(s -> s.hasPermission(2))
                                .executes(ctx -> spawnBall(ctx.getSource())))
                        .then(Commands.literal("spawndemon").requires(s -> s.hasPermission(2))
                                .executes(ctx -> spawnDemon(ctx.getSource())))
                        .then(Commands.literal("giveitem").requires(s -> s.hasPermission(2))
                                .executes(ctx -> giveVerityItem(ctx.getSource())))
                        .then(Commands.literal("size").requires(s -> s.hasPermission(2))
                                .executes(ctx -> printSize(ctx.getSource()))))
                .then(Commands.literal("animation")
                        .then(Commands.literal("play").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("animation", StringArgumentType.word())
                                        .suggests(VERITY_ANIM_SUGGESTIONS)
                                        .executes(ctx -> playVerityAnimation(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "animation"))))))
                .then(Commands.literal("expression")
                        .then(Commands.literal("set").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("expression", StringArgumentType.word())
                                        .suggests(EXPRESSION_SUGGESTIONS)
                                        .executes(ctx -> setExpression(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "expression")))))
                        .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                                .executes(ctx -> resetExpression(ctx.getSource()))))
                .then(Commands.literal("visual")
                        .then(Commands.literal("debug").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setVisualDebug(ctx.getSource(),
                                                BoolArgumentType.getBool(ctx, "enabled"))))))
                .then(Commands.literal("greeting")
                        .then(Commands.literal("replay").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> replayGreeting(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("stop").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> stopGreeting(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("status")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> greetingStatus(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("sound").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("play")
                                .then(Commands.argument("sound_id", StringArgumentType.greedyString())
                                        .suggests(SOUND_SUGGESTIONS)
                                        .executes(ctx -> playSound(ctx.getSource(), StringArgumentType.getString(ctx, "sound_id"))))))
                .then(Commands.literal("debug").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                    VerityDebug.setCommandOverride(enabled);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Verity debug = " + enabled), true);
                                    return 1;
                                })))
        );
    }

    private static int spawnIntro(CommandSourceStack source, ServerPlayer player) {
        Optional<VerityBoxEntity> existing = findBox(player);
        if (existing.isPresent() && VerityCommonConfig.ALLOW_ONE_BOX_PER_PLAYER.get()) {
            source.sendFailure(Component.translatable("message.universe_verity.box_already_exists"));
            return 0;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        Optional<SafeBoxPlacement.Placement> placement = SafeBoxPlacement.find(level, player);
        if (placement.isEmpty()) {
            source.sendFailure(Component.literal("No safe placement found."));
            return 0;
        }
        VerityBoxEntity box = VerityEntities.VERITY_BOX.get().create(level);
        if (box == null) {
            return 0;
        }
        SafeBoxPlacement.Placement place = placement.get();
        box.moveTo(place.position().x, place.position().y, place.position().z, place.yRot(), 0);
        box.configureSpawn(player.getUUID(), place.yRot());
        level.addFreshEntity(box);
        VerityPlayerData.setIntroStarted(player, true);
        VerityPlayerData.setBoxUuid(player, box.getUUID());
        source.sendSuccess(() -> Component.literal("Spawned Verity box at " + format(place.position())), true);
        return 1;
    }

    private static int resetIntro(CommandSourceStack source, ServerPlayer player) {
        removeOwnedBox(player);
        removeOwnedVerity(player);
        VerityPlayerData.resetIntroduction(player);
        source.sendSuccess(() -> Component.translatable("message.universe_verity.intro_reset"), true);
        return 1;
    }

    private static int replayIntro(CommandSourceStack source, ServerPlayer player) {
        Optional<VerityBoxEntity> box = findBox(player);
        if (box.isEmpty()) {
            source.sendFailure(Component.translatable("message.universe_verity.box_missing"));
            return 0;
        }
        box.get().replayIntro();
        source.sendSuccess(() -> Component.translatable("message.universe_verity.intro_replayed"), true);
        return 1;
    }

    private static int removeBox(CommandSourceStack source, ServerPlayer player) {
        if (!removeOwnedBox(player)) {
            source.sendFailure(Component.translatable("message.universe_verity.box_missing"));
            return 0;
        }
        VerityPlayerData.setBoxUuid(player, null);
        source.sendSuccess(() -> Component.literal("Removed owned Verity box."), true);
        return 1;
    }

    private static int skipIntro(CommandSourceStack source, ServerPlayer player) {
        Optional<VerityBoxEntity> box = findBox(player);
        if (box.isEmpty()) {
            source.sendFailure(Component.translatable("message.universe_verity.box_missing"));
            return 0;
        }
        box.get().skipIntro();
        VerityPlayerData.setIntroCompleted(player, true);
        source.sendSuccess(() -> Component.literal("Skipped intro sequence."), true);
        return 1;
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        Optional<VerityBoxEntity> box = findBox(player);
        String boxUuid = VerityPlayerData.getBoxUuid(player).map(UUID::toString).orElse("none");
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(player.getUUID()).append('\n');
        sb.append("IntroStarted: ").append(VerityPlayerData.isIntroStarted(player)).append('\n');
        sb.append("IntroCompleted: ").append(VerityPlayerData.isIntroCompleted(player)).append('\n');
        sb.append("BoxUUID: ").append(boxUuid).append('\n');
        sb.append("BoxLoaded: ").append(box.isPresent()).append('\n');
        if (box.isPresent()) {
            VerityBoxEntity b = box.get();
            sb.append("BoxPos: ").append(format(b.position())).append('\n');
            sb.append("Stage: ").append(b.getIntroStage()).append('\n');
            sb.append("StageTicks: ").append(b.getStageTicks()).append('\n');
            sb.append("LastVoice: ").append(b.getLastVoiceLine()).append('\n');
            sb.append("IdleCooldown: ").append(b.getIdleCooldown()).append('\n');
            sb.append("Distance: ").append(String.format("%.2f", player.distanceTo(b))).append('\n');
        }
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int locateBox(CommandSourceStack source, ServerPlayer player) {
        Optional<VerityBoxEntity> box = findBox(player);
        if (box.isEmpty()) {
            source.sendFailure(Component.translatable("message.universe_verity.box_missing"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Box at " + format(box.get().position())), false);
        return 1;
    }

    private static int animateBox(CommandSourceStack source, ServerPlayer player, String animation) {
        if (!BOX_ANIMS.contains(animation)) {
            source.sendFailure(Component.literal("Unknown animation: " + animation));
            return 0;
        }
        Optional<VerityBoxEntity> box = findBox(player);
        if (box.isEmpty()) {
            source.sendFailure(Component.translatable("message.universe_verity.box_missing"));
            return 0;
        }
        box.get().triggerAnimation(animation);
        source.sendSuccess(() -> Component.literal("Triggered animation " + animation), true);
        return 1;
    }

    private static int triggerReveal(CommandSourceStack source, ServerPlayer player) {
        Optional<VerityBoxEntity> box = findBox(player);
        if (box.isEmpty()) {
            source.sendFailure(Component.translatable("message.universe_verity.box_missing"));
            return 0;
        }
        if (VerityPlayerData.getVerityUuid(player).isPresent() && findVerity(player).isPresent()) {
            source.sendFailure(Component.literal("Verity already exists."));
            return 0;
        }
        box.get().beginReveal(player);
        return 1;
    }

    private static int resetReveal(CommandSourceStack source, ServerPlayer player) {
        removeOwnedVerity(player);
        removeOwnedBox(player);
        VerityPlayerData.resetIntroduction(player);
        spawnIntro(source, player);
        source.sendSuccess(() -> Component.literal("Reveal reset; sealed box respawned."), true);
        return 1;
    }

    private static int revealStatus(CommandSourceStack source, ServerPlayer player) {
        Optional<VerityBoxEntity> box = findBox(player);
        Optional<VerityEntity> verity = findVerity(player);
        StringBuilder sb = new StringBuilder();
        sb.append("BoxUUID: ").append(VerityPlayerData.getBoxUuid(player).map(UUID::toString).orElse("none")).append('\n');
        sb.append("BoxLoaded: ").append(box.isPresent()).append('\n');
        sb.append("RevealStarted: ").append(VerityPlayerData.isRevealStarted(player)).append('\n');
        sb.append("RevealCompleted: ").append(VerityPlayerData.isRevealCompleted(player)).append('\n');
        if (box.isPresent()) {
            sb.append("RevealStage: ").append(box.get().getRevealStage()).append('\n');
            sb.append("RevealTicks: ").append(box.get().getRevealTicks()).append('\n');
        }
        sb.append("VerityUUID: ").append(VerityPlayerData.getVerityUuid(player).map(UUID::toString).orElse("none")).append('\n');
        sb.append("VerityLoaded: ").append(verity.isPresent()).append('\n');
        if (verity.isPresent()) {
            sb.append("VerityPos: ").append(format(verity.get().position())).append('\n');
            sb.append("GreetingStarted: ").append(verity.get().isGreetingStarted()).append('\n');
            sb.append("GreetingCompleted: ").append(verity.get().isGreetingCompleted()).append('\n');
        }
        sb.append("OwnerUUID: ").append(player.getUUID());
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int skipRevealAnim(CommandSourceStack source, ServerPlayer player) {
        Optional<VerityBoxEntity> box = findBox(player);
        if (box.isEmpty()) {
            source.sendFailure(Component.translatable("message.universe_verity.box_missing"));
            return 0;
        }
        if (findVerity(player).isPresent()) {
            box.get().discard();
            VerityPlayerData.setBoxUuid(player, null);
            source.sendSuccess(() -> Component.literal("Verity already present; removed box."), true);
            return 1;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        VerityBoxEntity b = box.get();
        VerityEntity verity = VerityEntities.VERITY.get().create(level);
        if (verity == null) {
            source.sendFailure(Component.translatable("message.universe_verity.reveal_failed"));
            return 0;
        }
        verity.moveTo(b.getX(), b.getY(), b.getZ(), b.getYRot(), 0);
        verity.setOwnerUUID(player.getUUID());
        if (!level.addFreshEntity(verity)) {
            source.sendFailure(Component.translatable("message.universe_verity.reveal_failed"));
            return 0;
        }
        VerityPlayerData.markRevealComplete(player, verity.getUUID());
        verity.beginPostReveal(player);
        b.setRevealStage(VerityRevealStage.REVEAL_COMPLETE);
        b.discard();
        source.sendSuccess(() -> Component.literal("Skipped reveal animation."), true);
        return 1;
    }

    private static int locateVerity(CommandSourceStack source, ServerPlayer player) {
        Optional<VerityEntity> verity = findVerity(player);
        if (verity.isEmpty()) {
            source.sendFailure(Component.literal("No owned Verity entity found."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Verity at " + format(verity.get().position())), false);
        return 1;
    }

    private static int removeVerity(CommandSourceStack source, ServerPlayer player) {
        if (!removeOwnedVerity(player)) {
            source.sendFailure(Component.literal("No owned Verity entity found."));
            return 0;
        }
        VerityPlayerData.setVerityUuid(player, null);
        VerityPlayerData.setRevealCompleted(player, false);
        VerityPlayerData.setRevealStarted(player, false);
        source.sendSuccess(() -> Component.literal("Removed owned Verity."), true);
        return 1;
    }

    private static int respawnVerity(CommandSourceStack source, ServerPlayer player) {
        if (findVerity(player).isPresent()) {
            source.sendFailure(Component.literal("Verity already loaded."));
            return 0;
        }
        if (!VerityPlayerData.isRevealCompleted(player)) {
            source.sendFailure(Component.literal("Reveal not completed; cannot respawn."));
            return 0;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        VerityEntity verity = VerityEntities.VERITY.get().create(level);
        if (verity == null) {
            return 0;
        }
        verity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
        verity.setOwnerUUID(player.getUUID());
        level.addFreshEntity(verity);
        VerityPlayerData.setVerityUuid(player, verity.getUUID());
        verity.beginPostReveal(player);
        verity.stopGreeting(); // recovery should not force greeting replay
        source.sendSuccess(() -> Component.literal("Respawned Verity near player."), true);
        return 1;
    }

    private static int replayGreeting(CommandSourceStack source, ServerPlayer player) {
        Optional<VerityEntity> verity = findVerity(player);
        if (verity.isEmpty()) {
            source.sendFailure(Component.literal("No owned Verity entity found."));
            return 0;
        }
        verity.get().replayGreeting();
        source.sendSuccess(() -> Component.translatable("message.universe_verity.greeting_replayed"), true);
        return 1;
    }

    private static int stopGreeting(CommandSourceStack source, ServerPlayer player) {
        Optional<VerityEntity> verity = findVerity(player);
        if (verity.isEmpty()) {
            source.sendFailure(Component.literal("No owned Verity entity found."));
            return 0;
        }
        verity.get().stopGreeting();
        source.sendSuccess(() -> Component.literal("Stopped greeting schedule."), true);
        return 1;
    }

    private static int greetingStatus(CommandSourceStack source, ServerPlayer player) {
        Optional<VerityEntity> verity = findVerity(player);
        StringBuilder sb = new StringBuilder();
        sb.append("GreetingPlayed(data): ").append(VerityPlayerData.isGreetingPlayed(player)).append('\n');
        sb.append("GreetingCompleted(data): ").append(VerityPlayerData.isGreetingCompleted(player)).append('\n');
        if (verity.isPresent()) {
            sb.append("EntityGreetingStarted: ").append(verity.get().isGreetingStarted()).append('\n');
            sb.append("EntityGreetingCompleted: ").append(verity.get().isGreetingCompleted()).append('\n');
        } else {
            sb.append("Entity: missing\n");
        }
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int playSound(CommandSourceStack source, String soundId) {
        RegistryObject<SoundEvent> sound = VeritySounds.byId(soundId);
        if (sound == null) {
            source.sendFailure(Component.literal("Unknown sound id: " + soundId));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        Vec3 pos = source.getPosition();
        Entity anchor = null;
        if (player != null) {
            anchor = findBox(player).map(e -> (Entity) e).orElse(findVerity(player).orElse(null));
        }
        if (anchor != null) {
            pos = anchor.position();
        }
        source.getLevel().playSound(null, pos.x, pos.y, pos.z, sound.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        Vec3 finalPos = pos;
        source.sendSuccess(() -> Component.literal("Played " + soundId + " at " + format(finalPos)), true);
        return 1;
    }

    private static Optional<VerityBoxEntity> findBox(ServerPlayer player) {
        Optional<UUID> id = VerityPlayerData.getBoxUuid(player);
        if (id.isPresent()) {
            for (ServerLevel level : player.server.getAllLevels()) {
                Entity e = level.getEntity(id.get());
                if (e instanceof VerityBoxEntity box) {
                    return Optional.of(box);
                }
            }
        }
        // fallback nearest owned
        if (player.level() instanceof ServerLevel level) {
            return level.getEntitiesOfClass(VerityBoxEntity.class, player.getBoundingBox().inflate(64),
                            b -> player.getUUID().equals(b.getOwnerUUID()))
                    .stream().findFirst();
        }
        return Optional.empty();
    }

    private static Optional<VerityEntity> findVerity(ServerPlayer player) {
        Optional<UUID> id = VerityPlayerData.getVerityUuid(player);
        if (id.isPresent()) {
            for (ServerLevel level : player.server.getAllLevels()) {
                Entity e = level.getEntity(id.get());
                if (e instanceof VerityEntity verity) {
                    return Optional.of(verity);
                }
            }
        }
        if (player.level() instanceof ServerLevel level) {
            return level.getEntitiesOfClass(VerityEntity.class, player.getBoundingBox().inflate(64),
                            v -> player.getUUID().equals(v.getOwnerUUID()))
                    .stream().findFirst();
        }
        return Optional.empty();
    }

    private static boolean removeOwnedBox(ServerPlayer player) {
        Optional<VerityBoxEntity> box = findBox(player);
        box.ifPresent(Entity::discard);
        VerityPlayerData.setBoxUuid(player, null);
        return box.isPresent();
    }

    private static boolean removeOwnedVerity(ServerPlayer player) {
        Optional<VerityEntity> verity = findVerity(player);
        verity.ifPresent(Entity::discard);
        VerityPlayerData.setVerityUuid(player, null);
        return verity.isPresent();
    }

    private static int spawnBall(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Player required."));
            return 0;
        }
        VerityEntity verity = VerityEntities.VERITY.get().create(level);
        if (verity == null) {
            source.sendFailure(Component.literal("Failed to create Verity."));
            return 0;
        }
        float yaw = player.getYRot();
        double dist = 1.6D;
        double x = player.getX() - Mth.sin(yaw * ((float) Math.PI / 180F)) * dist;
        double z = player.getZ() + Mth.cos(yaw * ((float) Math.PI / 180F)) * dist;
        double y = player.getY();
        verity.moveTo(x, y, z, yaw + 180.0f, 0);
        verity.setOwnerUUID(player.getUUID());
        verity.setExpression(VerityExpressionState.HAPPY);
        verity.triggerAnimation("idle");
        if (!level.addFreshEntity(verity)) {
            source.sendFailure(Component.literal("Failed to spawn Verity."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned Verity ball at " + format(verity.position())
                + " (" + verity.describeSize() + ")"), true);
        return 1;
    }

    private static int spawnDemon(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Player required."));
            return 0;
        }
        com.universeexe.verity.entity.VerityDemonEntity demon = VerityEntities.VERITY_DEMON.get().create(level);
        if (demon == null) {
            source.sendFailure(Component.literal("Failed to create demon form."));
            return 0;
        }
        float yaw = player.getYRot();
        double dist = 2.0D;
        double x = player.getX() - Mth.sin(yaw * ((float) Math.PI / 180F)) * dist;
        double z = player.getZ() + Mth.cos(yaw * ((float) Math.PI / 180F)) * dist;
        demon.moveTo(x, player.getY(), z, yaw + 180.0f, 0);
        demon.setVisualAnimation("idle");
        if (!level.addFreshEntity(demon)) {
            source.sendFailure(Component.literal("Failed to spawn demon form."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned Verity demon form (visuals only) at "
                + format(demon.position())), true);
        return 1;
    }

    private static int giveVerityItem(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Player required."));
            return 0;
        }
        net.minecraft.world.item.ItemStack stack =
                new net.minecraft.world.item.ItemStack(com.universeexe.verity.registry.VerityItems.VERITY_ITEM.get());
        com.universeexe.verity.item.VerityItem.writeVariant(stack, "happy");
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.literal("Gave held Verity item."), true);
        return 1;
    }

    private static int removeNearestOrSelf(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Player required."));
            return 0;
        }
        if (removeOwnedVerity(player)) {
            source.sendSuccess(() -> Component.literal("Removed owned Verity."), true);
            return 1;
        }
        if (player.level() instanceof ServerLevel level) {
            Optional<VerityEntity> nearest = level.getEntitiesOfClass(VerityEntity.class,
                            player.getBoundingBox().inflate(16))
                    .stream().findFirst();
            if (nearest.isPresent()) {
                nearest.get().discard();
                source.sendSuccess(() -> Component.literal("Removed nearest Verity."), true);
                return 1;
            }
        }
        source.sendFailure(Component.literal("No Verity entity found."));
        return 0;
    }

    private static int printSize(CommandSourceStack source) {
        Optional<VerityEntity> verity = findNearbyVerity(source);
        if (verity.isEmpty()) {
            source.sendFailure(Component.literal("No Verity nearby. Use /verity entity spawnball first."));
            return 0;
        }
        VerityEntity v = verity.get();
        String msg = String.format(Locale.ROOT,
                "Verity size: %s | onGround=%s | shadowHint=0.28",
                v.describeSize(), v.onGround());
        source.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int playVerityAnimation(CommandSourceStack source, String animation) {
        if (!VERITY_ANIMS.contains(animation)) {
            source.sendFailure(Component.literal("Unknown animation: " + animation));
            return 0;
        }
        Optional<VerityEntity> verity = findNearbyVerity(source);
        if (verity.isEmpty()) {
            source.sendFailure(Component.literal("No Verity nearby."));
            return 0;
        }
        verity.get().triggerAnimation(animation);
        source.sendSuccess(() -> Component.literal("Playing animation.verity." + animation), true);
        return 1;
    }

    private static int setExpression(CommandSourceStack source, String expression) {
        Optional<VerityExpressionState> state = VerityExpressionState.fromId(expression);
        if (state.isEmpty() || Arrays.stream(VerityExpressionState.COMMAND_CHOICES)
                .noneMatch(s -> s == state.get())) {
            source.sendFailure(Component.literal("Unknown expression: " + expression));
            return 0;
        }
        Optional<VerityEntity> verity = findNearbyVerity(source);
        if (verity.isEmpty()) {
            source.sendFailure(Component.literal("No Verity nearby."));
            return 0;
        }
        verity.get().setExpressionFromCommand(state.get().id());
        if (state.get().isBlink()) {
            verity.get().triggerAnimation(state.get() == VerityExpressionState.LONG_BLINK
                    ? "long_blink" : "blink");
        }
        source.sendSuccess(() -> Component.literal("Expression set to " + state.get().id()), true);
        return 1;
    }

    private static int resetExpression(CommandSourceStack source) {
        Optional<VerityEntity> verity = findNearbyVerity(source);
        if (verity.isEmpty()) {
            source.sendFailure(Component.literal("No Verity nearby."));
            return 0;
        }
        verity.get().resetExpression();
        verity.get().triggerAnimation("idle");
        source.sendSuccess(() -> Component.literal("Expression reset to happy."), true);
        return 1;
    }

    private static int setVisualDebug(CommandSourceStack source, boolean enabled) {
        Optional<VerityEntity> verity = findNearbyVerity(source);
        if (verity.isEmpty()) {
            source.sendFailure(Component.literal("No Verity nearby."));
            return 0;
        }
        verity.get().setVisualDebug(enabled);
        source.sendSuccess(() -> Component.literal("Verity visual debug = " + enabled), true);
        return 1;
    }

    private static Optional<VerityEntity> findNearbyVerity(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            Optional<VerityEntity> owned = findVerity(player);
            if (owned.isPresent()) {
                return owned;
            }
            if (player.level() instanceof ServerLevel level) {
                return level.getEntitiesOfClass(VerityEntity.class, player.getBoundingBox().inflate(24))
                        .stream().findFirst();
            }
        }
        return Optional.empty();
    }

    private static String format(Vec3 pos) {
        return String.format("%.2f %.2f %.2f", pos.x, pos.y, pos.z);
    }
}
