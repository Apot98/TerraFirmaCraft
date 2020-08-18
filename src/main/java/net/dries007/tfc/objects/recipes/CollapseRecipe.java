/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.objects.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.objects.TFCTags;
import net.dries007.tfc.objects.entities.TFCFallingBlockEntity;
import net.dries007.tfc.util.collections.IndirectHashCollection;
import net.dries007.tfc.util.support.SupportManager;
import net.dries007.tfc.world.tracker.CollapseData;
import net.dries007.tfc.world.tracker.WorldTrackerCapability;

/**
 * This handles logic relating to block collapses.
 * The recipe itself only handles *transformations*, not actually if a block can or cannot collapse. Those are handled by tags.
 *
 * @see SupportManager
 * @see TFCFallingBlockEntity
 */
public class CollapseRecipe extends SimpleBlockRecipe
{
    public static final IndirectHashCollection<Block, CollapseRecipe> CACHE = new IndirectHashCollection<>(recipe -> recipe.getBlockIngredient().getValidBlocks());
    private static final Random RANDOM = new Random();

    public static Optional<CollapseRecipe> getRecipe(World world, BlockRecipeWrapper wrapper)
    {
        return CACHE.getAll(wrapper.getState().getBlock()).stream().filter(recipe -> recipe.matches(wrapper, world)).findFirst();
    }

    /**
     * Called to attempt to trigger a collapse, from when a player mines a block.
     *
     * @return true if a collapse occurred.
     */
    public static boolean tryTriggerCollapse(World world, BlockPos pos)
    {
        if (!world.isRemote() && world.isAreaLoaded(pos, 32))
        {
            if (RANDOM.nextFloat() < TFCConfig.SERVER.collapseTriggerChance.get())
            {
                // Random radius
                int radX = (RANDOM.nextInt(5) + 4) / 2;
                int radY = (RANDOM.nextInt(3) + 2) / 2;
                int radZ = (RANDOM.nextInt(5) + 4) / 2;
                for (BlockPos checking : SupportManager.findUnsupportedPositions(world, pos.add(-radX, -radY, -radZ), pos.add(radX, radY, radZ))) // 9x5x9 max
                {
                    if (canStartCollapse(world, checking))
                    {
                        startCollapse(world, checking);
                        world.playSound(null, pos, TFCSounds.ROCK_SLIDE_LONG.get(), SoundCategory.BLOCKS, 1.0f, 1.0f);
                        return true; // Don't need to check other blocks
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if a single block is possible to be the locus of a collapse
     */
    public static boolean canStartCollapse(IWorld world, BlockPos pos)
    {
        return TFCTags.Blocks.CAN_START_COLLAPSE.contains(world.getBlockState(pos).getBlock()) && TFCFallingBlockEntity.canFallThrough(world, pos.down());
    }

    /**
     * Starts a collapse from a given location
     * - at this point, any supports are ignored completely.
     * - many more blocks can collapse, even if they can't trigger or start collapses.
     * - this is much more in-depth than previous implementations, and searches aggressively for next-tick collapse blocks
     */
    public static void startCollapse(World world, BlockPos centerPos)
    {
        int radius = TFCConfig.SERVER.collapseMinRadius.get() + RANDOM.nextInt(TFCConfig.SERVER.collapseRadiusVariance.get());
        int radiusSquared = radius * radius;
        List<BlockPos> secondaryPositions = new ArrayList<>();

        // Initially only scan on the bottom layer, and advance upwards
        for (BlockPos pos : BlockPos.getAllInBoxMutable(centerPos.add(-radius, -4, -radius), centerPos.add(radius, -4, radius)))
        {
            boolean foundEmpty = false; // If we've found a space to collapse into
            for (int y = 0; y <= 8; y++)
            {
                BlockPos posAt = pos.up(y);
                BlockState stateAt = world.getBlockState(posAt);
                if (foundEmpty && TFCTags.Blocks.CAN_COLLAPSE.contains(stateAt.getBlock()))
                {
                    // Check for a possible collapse
                    if (posAt.distanceSq(centerPos) < radiusSquared && RANDOM.nextFloat() < TFCConfig.SERVER.collapsePropagateChance.get())
                    {
                        if (collapseBlock(world, posAt, stateAt))
                        {
                            // This column has started to collapse. Mark the next block above as unstable for the "follow up"
                            secondaryPositions.add(posAt.up());
                            break;
                        }
                    }
                }
                if (TFCFallingBlockEntity.canFallThrough(world, posAt))
                {
                    foundEmpty = true;
                }
            }
        }

        if (!secondaryPositions.isEmpty())
        {
            world.getCapability(WorldTrackerCapability.CAPABILITY).ifPresent(cap -> cap.addCollapseData(new CollapseData(centerPos, secondaryPositions, radiusSquared)));
        }
    }

    /**
     * Responsible for actually collapsing a singular block. Queries the collapse recipe, replaces the block, and spawns the falling block entity
     *
     * @return true if the collapse actually occurred
     */
    public static boolean collapseBlock(World world, BlockPos pos, BlockState state)
    {
        BlockRecipeWrapper wrapper = new BlockRecipeWrapper(world, pos, state);
        return getRecipe(world, wrapper).map(recipe -> {
            BlockState collapseState = recipe.getBlockCraftingResult(wrapper);
            world.setBlockState(pos, collapseState); // Required as the falling block entity will replace the block in it's first tick
            world.addEntity(new TFCFallingBlockEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, collapseState));
            return true;
        }).orElse(false);
    }

    public CollapseRecipe(ResourceLocation id, IBlockIngredient ingredient, BlockState outputState, boolean copyInputState)
    {
        super(id, ingredient, outputState, copyInputState);
    }

    @Override
    public IRecipeSerializer<?> getSerializer()
    {
        return TFCRecipeSerializers.COLLAPSE.get();
    }

    @Override
    public IRecipeType<?> getType()
    {
        return TFCRecipeTypes.COLLAPSE;
    }

    public static class Serializer extends SimpleBlockRecipe.Serializer<CollapseRecipe>
    {
        @Override
        protected CollapseRecipe create(ResourceLocation id, IBlockIngredient ingredient, BlockState state, boolean copyInputState)
        {
            return new CollapseRecipe(id, ingredient, state, copyInputState);
        }
    }
}