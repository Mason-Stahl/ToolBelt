package dev.gigaherz.toolbelt.common;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * A slot that can't be taken from or placed into. Used for the belt while its menu is open.
 */
class LockedSlot extends Slot
{
    public LockedSlot(Container container, int index, int x, int y)
    {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPickup(Player player)
    {
        return false;
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        return false;
    }

    @Override
    public boolean allowModification(Player player)
    {
        return false;
    }

    @Override
    public Optional<ItemStack> tryRemove(int count, int maxAmount, Player player)
    {
        return Optional.empty();
    }

    @Override
    public ItemStack safeTake(int count, int maxAmount, Player player)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack safeInsert(ItemStack stack)
    {
        return stack;
    }

    @Override
    public ItemStack safeInsert(ItemStack stack, int amount)
    {
        return stack;
    }

    @Override
    public ItemStack remove(int amount)
    {
        return ItemStack.EMPTY;
    }
}
