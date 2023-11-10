/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.rotation.RotationSinkBlockEntity;
import net.dries007.tfc.common.blocks.devices.QuernBlock;
import net.dries007.tfc.common.recipes.QuernRecipe;
import net.dries007.tfc.common.recipes.inventory.ItemStackInventory;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.rotation.NetworkAction;
import net.dries007.tfc.util.rotation.Node;
import net.dries007.tfc.util.rotation.Rotation;
import net.dries007.tfc.util.rotation.SinkNode;

import static net.dries007.tfc.TerraFirmaCraft.MOD_ID;

public class QuernBlockEntity extends TickableInventoryBlockEntity<ItemStackHandler> implements RotationSinkBlockEntity
{
    public static final int SLOT_HANDSTONE = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;

    private static final Component NAME = Component.translatable(MOD_ID + ".block_entity.quern");

    public static void serverTick(Level level, BlockPos pos, BlockState state, QuernBlockEntity quern)
    {
        quern.checkForLastTickSync();
        if (quern.needsStateUpdate)
        {
            quern.updateHandstone();
        }
        if (quern.recipeTimer > 0)
        {
            ServerLevel serverLevel = (ServerLevel) level;
            final ItemStack inputStack = quern.inventory.getStackInSlot(SLOT_INPUT);
            if (!inputStack.isEmpty())
            {
                sendParticle(serverLevel, pos, inputStack, 1);
            }

            quern.recipeTimer--;
            if (quern.recipeTimer == 0)
            {
                quern.finishGrinding();
                Helpers.playSound(level, pos, SoundEvents.ARMOR_STAND_FALL);

                ItemStack handstone = quern.inventory.getStackInSlot(SLOT_HANDSTONE);
                ItemStack undamagedHandstoneStack = handstone.copy();
                Helpers.damageItem(handstone, 1);

                if (!quern.hasHandstone())
                {
                    Helpers.playSound(level, pos, SoundEvents.STONE_BREAK);
                    Helpers.playSound(level, pos, SoundEvents.ITEM_BREAK);
                    sendParticle(serverLevel, pos, undamagedHandstoneStack, 15);
                }
                quern.setAndUpdateSlots(SLOT_HANDSTONE);
            }
        }
        if (quern.isConnectedToNetwork() || quern.recipeTimer > 0)
        {
            quern.grindTick++;
        }
        else
        {
            if (quern.grindTick > 0)
            {
                quern.markForSync();
            }
            quern.grindTick = 0;
        }
        if (quern.isConnectedToNetwork() && !quern.isGrinding() && level.getGameTime() % 10 == 0)
        {
            quern.startGrinding();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, QuernBlockEntity quern)
    {
        if (quern.recipeTimer > 0)
        {
            quern.recipeTimer--;
        }
        if (quern.grindTick > 0)
        {
            quern.grindTick++;
        }
    }

    private static void sendParticle(ServerLevel level, BlockPos pos, ItemStack item, int count)
    {
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item), pos.getX() + 0.5D, pos.getY() + 0.875D, pos.getZ() + 0.5D, count, Helpers.triangle(level.random) / 2.0D, level.random.nextDouble() / 4.0D, Helpers.triangle(level.random) / 2.0D, 0.15f);
    }

    private final SinkNode node;

    private int recipeTimer;
    private int grindTick;
    private boolean needsStateUpdate = false;

    public QuernBlockEntity(BlockPos pos, BlockState state)
    {
        super(TFCBlockEntities.QUERN.get(), pos, state, defaultInventory(3), NAME);

        this.recipeTimer = 0;
        this.grindTick = 0;
        this.node = new SinkNode(pos, Direction.UP) {
            @Override
            public String toString()
            {
                return "Quern[pos=%s]".formatted(pos());
            }
        };
    }

    public void updateHandstone()
    {
        assert level != null;
        BlockState state = level.getBlockState(worldPosition);
        BlockState newState = Helpers.setProperty(state, QuernBlock.HAS_HANDSTONE, hasHandstone());
        if (hasHandstone() != state.getValue(QuernBlock.HAS_HANDSTONE))
        {
            level.setBlockAndUpdate(worldPosition, newState);
        }
        needsStateUpdate = false;
    }

    @Override
    public void setAndUpdateSlots(int slot)
    {
        super.setAndUpdateSlots(slot);
        needsStateUpdate = true;
    }

    @Override
    public int getSlotStackLimit(int slot)
    {
        return slot == SLOT_HANDSTONE ? 1 : 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return slot != SLOT_HANDSTONE || Helpers.isItem(stack.getItem(), TFCTags.Items.HANDSTONE);
    }

    @Override
    public void loadAdditional(CompoundTag nbt)
    {
        recipeTimer = nbt.getInt("recipeTimer");
        grindTick = nbt.getInt("grindTick");
        super.loadAdditional(nbt);
        needsStateUpdate = true;
    }

    @Override
    public void saveAdditional(CompoundTag nbt)
    {
        nbt.putInt("recipeTimer", recipeTimer);
        nbt.putInt("grindTick", grindTick);
        super.saveAdditional(nbt);
    }

    @Override
    public boolean canInteractWith(Player player)
    {
        return super.canInteractWith(player) && recipeTimer == 0;
    }

    public int getGrindTick()
    {
        return grindTick;
    }

    /**
     * @return if a recipe is being executed
     */
    public boolean isGrinding()
    {
        return recipeTimer > 0;
    }

    /**
     * @return if the quern is either rotating normally or receiving power to rotate
     */
    public boolean isVisuallyGrinding()
    {
        return grindTick > 0;
    }

    public boolean hasHandstone()
    {
        return !inventory.getStackInSlot(SLOT_HANDSTONE).isEmpty();
    }

    /**
     * Attempts to start grinding. Returns {@code true} if it did.
     */
    public boolean startGrinding()
    {
        assert level != null;
        final ItemStack inputStack = inventory.getStackInSlot(SLOT_INPUT);

        if (!inputStack.isEmpty())
        {
            final ItemStackInventory wrapper = new ItemStackInventory(inputStack);
            final QuernRecipe recipe = QuernRecipe.getRecipe(level, wrapper);
            if (recipe != null && recipe.matches(wrapper, level))
            {
                recipeTimer = 90;
                markForSync();
                return true;
            }
        }
        return false;
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        performNetworkAction(NetworkAction.REMOVE);
    }

    @Override
    public void onChunkUnloaded()
    {
        super.onChunkUnloaded();
        performNetworkAction(NetworkAction.REMOVE);
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        performNetworkAction(NetworkAction.ADD);
    }

    @Override
    public Node getRotationNode()
    {
        return node;
    }

    @Override
    public float getRotationAngle(float partialTick)
    {
        return isConnectedToNetwork()
            ? RotationSinkBlockEntity.super.getRotationAngle(partialTick)
            : Mth.TWO_PI * recipeTimer / 90f;
    }

    public boolean isConnectedToNetwork()
    {
        final Rotation rotation = node.rotation();
        return rotation != null && rotation.speed() != 0;
    }

    private void finishGrinding()
    {
        assert level != null;
        final ItemStack inputStack = inventory.getStackInSlot(SLOT_INPUT);
        if (!inputStack.isEmpty())
        {
            final ItemStackInventory wrapper = new ItemStackInventory(inputStack);
            final QuernRecipe recipe = QuernRecipe.getRecipe(level, wrapper);
            if (recipe != null && recipe.matches(wrapper, level))
            {
                ItemStack outputStack = recipe.assemble(wrapper, level.registryAccess());
                outputStack = Helpers.mergeInsertStack(inventory, SLOT_OUTPUT, outputStack);
                if (!outputStack.isEmpty() && !level.isClientSide)
                {
                    Helpers.spawnItem(level, worldPosition, outputStack);
                }

                // Shrink the input stack after the recipe is done assembling
                inputStack.shrink(1);
                markForSync();
            }
        }
    }
}
