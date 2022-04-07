/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.recipes.outputs;

import net.minecraft.world.item.ItemStack;

import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.items.SoupItem;

public enum EmptyBowlModifier implements ItemStackModifier.SingleInstance<EmptyBowlModifier>
{
    INSTANCE;

    @Override
    public ItemStack apply(ItemStack stack, ItemStack input)
    {
        return input.getCapability(FoodCapability.CAPABILITY)
            .filter(cap -> cap instanceof SoupItem.SoupHandler)
            .map(cap -> ((SoupItem.SoupHandler) cap).getBowl())
            .orElse(ItemStack.EMPTY);
    }

    @Override
    public EmptyBowlModifier instance()
    {
        return INSTANCE;
    }
}
