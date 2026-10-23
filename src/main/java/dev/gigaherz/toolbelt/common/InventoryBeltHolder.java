package dev.gigaherz.toolbelt.common;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * A belt that lives in a fixed slot of a player's inventory.
 */
public record InventoryBeltHolder(Inventory inventory, int slot) implements BeltContainer.Holder
{
    @Override
    public ItemStack getBelt()
    {
        return inventory.getItem(slot);
    }

    @Override
    public void setBelt(ItemStack stack)
    {
        inventory.setItem(slot, stack);
    }
}
