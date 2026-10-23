package dev.gigaherz.toolbelt.slot;

import dev.gigaherz.toolbelt.common.BeltContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A one-slot Container view of a player's belt slot attachment. Also serves as the belt
 * holder when the belt in the slot is used by the radial menu.
 */
public class BeltSlotContainer implements Container, BeltContainer.Holder
{
    private final Player player;

    public BeltSlotContainer(Player player)
    {
        this.player = player;
    }

    @Override
    public ItemStack getBelt()
    {
        return BeltSlotAttachment.get(player);
    }

    @Override
    public void setBelt(ItemStack stack)
    {
        BeltSlotAttachment.set(player, stack);
    }

    @Override
    public int getContainerSize()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return getBelt().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return slot == 0 ? getBelt() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count)
    {
        ItemStack existing = getItem(slot);
        if (existing.isEmpty() || count <= 0)
            return ItemStack.EMPTY;
        ItemStack taken = existing.split(count);
        setBelt(existing.isEmpty() ? ItemStack.EMPTY : existing.copy());
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        ItemStack existing = getItem(slot);
        if (slot == 0)
            setBelt(ItemStack.EMPTY);
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack stack)
    {
        if (slot == 0)
            setBelt(stack);
    }

    @Override
    public int getMaxStackSize()
    {
        return 1;
    }

    @Override
    public void setChanged()
    {
        // Menu code may edit the stack in place; set it again so the change is saved and synced.
        ItemStack stack = getBelt();
        setBelt(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    @Override
    public void clearContent()
    {
        setBelt(ItemStack.EMPTY);
    }
}
