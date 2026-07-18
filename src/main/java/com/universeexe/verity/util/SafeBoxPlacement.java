package com.universeexe.verity.util;

import com.universeexe.verity.config.VerityCommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class SafeBoxPlacement {
    private static final int[] DISTANCES = {4, 3, 5, 2, 6};
    private static final float BOX_WIDTH = 1.25f;
    private static final float BOX_HEIGHT = 1.0f;

    private SafeBoxPlacement() {
    }

    public record Placement(Vec3 position, float yRot) {
    }

    public static Optional<Placement> find(ServerLevel level, Player player) {
        float useYaw = player.yBodyRot;

        for (int distance : orderedDistances()) {
            Optional<Placement> direct = tryForward(level, player, useYaw, distance);
            if (direct.isPresent()) {
                return direct;
            }
        }

        for (int angle = 15; angle <= 75; angle += 15) {
            for (int sign : new int[]{1, -1}) {
                float testYaw = useYaw + angle * sign;
                for (int distance : orderedDistances()) {
                    Optional<Placement> arc = tryForward(level, player, testYaw, distance);
                    if (arc.isPresent()) {
                        return arc;
                    }
                }
            }
        }

        // Small spiral around the player, still preferring forward half.
        BlockPos origin = player.blockPosition();
        for (int r = 2; r <= 6; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }
                    Vec3 candidate = new Vec3(origin.getX() + dx + 0.5, player.getY(), origin.getZ() + dz + 0.5);
                    Vec3 toCandidate = candidate.subtract(player.position()).normalize();
                    Vec3 look = Vec3.directionFromRotation(0, useYaw);
                    if (toCandidate.dot(look) < -0.15) {
                        continue; // behind player
                    }
                    Optional<Placement> spiral = tryExact(level, player, candidate.x, candidate.z, useYaw);
                    if (spiral.isPresent()) {
                        return spiral;
                    }
                }
            }
        }

        VerityDebug.log("No safe box placement found for {}", player.getGameProfile().getName());
        return Optional.empty();
    }

    private static int[] orderedDistances() {
        int preferred = VerityCommonConfig.PREFERRED_SPAWN_DISTANCE.get();
        int[] ordered = new int[DISTANCES.length];
        ordered[0] = preferred;
        int idx = 1;
        for (int d : DISTANCES) {
            if (d != preferred) {
                ordered[idx++] = d;
            }
        }
        return ordered;
    }

    private static Optional<Placement> tryForward(ServerLevel level, Player player, float yaw, int distance) {
        float rad = yaw * ((float) Math.PI / 180f);
        double fx = -Mth.sin(rad);
        double fz = Mth.cos(rad);
        double x = player.getX() + fx * distance;
        double z = player.getZ() + fz * distance;
        return tryExact(level, player, x, z, yaw);
    }

    private static Optional<Placement> tryExact(ServerLevel level, Player player, double x, double z, float playerYaw) {
        int baseY = Mth.floor(player.getY());
        for (int dy = -3; dy <= 3; dy++) {
            int y = baseY + dy;
            BlockPos supportPos = BlockPos.containing(x, y - 1, z);
            BlockPos standPos = BlockPos.containing(x, y, z);
            if (!level.isLoaded(standPos) || !level.isLoaded(supportPos)) {
                continue;
            }
            if (!isSupport(level, supportPos)) {
                continue;
            }
            if (!hasClearance(level, standPos)) {
                continue;
            }
            AABB box = new AABB(
                    x - BOX_WIDTH / 2.0, y, z - BOX_WIDTH / 2.0,
                    x + BOX_WIDTH / 2.0, y + BOX_HEIGHT, z + BOX_WIDTH / 2.0
            );
            if (box.intersects(player.getBoundingBox().inflate(0.15))) {
                continue;
            }
            if (!level.noCollision(box)) {
                continue;
            }
            if (!level.getEntities((Entity) null, box, e -> e != player && e.isAlive()).isEmpty()) {
                continue;
            }
            if (isHazardNearby(level, standPos)) {
                continue;
            }
            float faceYaw = yawToward(x, z, player.getX(), player.getZ());
            VerityDebug.log("Accepted box spawn at {}, {}, {} facing {}", x, y, z, faceYaw);
            return Optional.of(new Placement(new Vec3(x, y, z), faceYaw));
        }
        return Optional.empty();
    }

    private static boolean isSupport(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.isFaceSturdy(level, pos, Direction.UP)) {
            return false;
        }
        if (state.is(BlockTags.LEAVES)) {
            return false;
        }
        return !isHazardBlock(state);
    }

    private static boolean hasClearance(ServerLevel level, BlockPos standPos) {
        return isReplaceableSafe(level, standPos) && isReplaceableSafe(level, standPos.above());
    }

    private static boolean isReplaceableSafe(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isHazardBlock(state)) {
            return false;
        }
        if (state.isAir() || state.canBeReplaced()) {
            return true;
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isHazardNearby(ServerLevel level, BlockPos standPos) {
        for (BlockPos check : BlockPos.betweenClosed(standPos.offset(-1, -1, -1), standPos.offset(1, 1, 1))) {
            if (isHazardBlock(level.getBlockState(check))) {
                return true;
            }
        }
        // Avoid dangerous drops: air for several blocks below support
        BlockPos below = standPos.below();
        int air = 0;
        for (int i = 0; i < 4; i++) {
            BlockPos p = below.below(i);
            if (level.getBlockState(p).isAir()) {
                air++;
            } else {
                break;
            }
        }
        return air >= 3;
    }

    private static boolean isHazardBlock(BlockState state) {
        return state.is(Blocks.WATER)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.MAGMA_BLOCK);
    }

    private static float yawToward(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
    }
}
