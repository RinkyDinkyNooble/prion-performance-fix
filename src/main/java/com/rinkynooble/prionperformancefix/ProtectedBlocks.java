package com.rinkynooble.prionperformancefix;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blocks Prion can't turn into its tissue.
 *
 * <p>Prion places its blocks with plain {@code setBlockAndUpdate} calls that replace whatever is
 * there: cysts, a Hive's rings, the floor under a rooting Hive, kills and infected deaths. A block is
 * spared when it holds a block entity (chests, spawners, keypads, machines), can't be broken, resists
 * explosions at least as well as obsidian (reinforced blocks), or is in the
 * {@code prionperformancefix:prion_immune} tag. Prion's own blocks stay replaceable, so its tissue
 * still grows over itself as before.
 */
public final class ProtectedBlocks {
    // The constructor works on every Forge 47; fromNamespaceAndPath only exists in later builds.
    @SuppressWarnings("removal")
    public static final TagKey<Block> PRION_IMMUNE = TagKey.create(Registries.BLOCK, new ResourceLocation(PrionPerformanceFix.MODID, "prion_immune"));
    private static final String PRION = "prionmod";
    private static final float OBSIDIAN_RESISTANCE = 1200.0F;

    /** Prion's blocks, found once its registrations are in (a level only exists after that). */
    private static volatile Block[] prionBlocks;

    private ProtectedBlocks() {
    }

    public static boolean isPrionBlock(Block block) {
        for (Block prion : prionBlocks()) {
            if (prion == block) {
                return true;
            }
        }
        return false;
    }

    /** Whether the block at this position must stay as it is when Prion places one of its blocks there. */
    public static boolean spares(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(PRION_IMMUNE)) {
            return true;
        }
        if (isPrionBlock(state.getBlock())) {
            return false;
        }
        return state.hasBlockEntity()
                || state.getDestroySpeed(level, pos) < 0
                || state.getBlock().getExplosionResistance() >= OBSIDIAN_RESISTANCE;
    }

    private static Block[] prionBlocks() {
        Block[] blocks = prionBlocks;
        if (blocks == null) {
            blocks = BuiltInRegistries.BLOCK.entrySet().stream()
                    .filter(entry -> entry.getKey().location().getNamespace().equals(PRION))
                    .map(entry -> entry.getValue())
                    .toArray(Block[]::new);
            prionBlocks = blocks;
        }
        return blocks;
    }
}
