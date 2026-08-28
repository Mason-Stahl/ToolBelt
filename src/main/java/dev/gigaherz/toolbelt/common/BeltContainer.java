package dev.gigaherz.toolbelt.common;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Presents a belt ItemStack's stored contents as a vanilla Container, writing changes
 * back through the holder so the caller decides where the belt actually lives
 * (held in hand, a specific inventory slot, etc). Menus and the swap logic both go
 * through this so they apply the same read/write rules.
 */
public class BeltContainer implements Container
{
    public interface Holder
    {
        ItemStack getBelt();

        void setBelt(ItemStack stack);
    }

    private final Holder holder;
    private final int slots;

    public BeltContainer(Holder holder, int slots)
    {
        this.holder = holder;
        this.slots = slots;
    }

    private NonNullList<ItemStack> contents()
    {
        NonNullList<ItemStack> items = NonNullList.withSize(slots, ItemStack.EMPTY);
        ItemContainerContents contents = holder.getBelt().get(DataComponents.CONTAINER);
        if (contents != null)
            contents.copyInto(items);
        return items;
    }

    @Override
    public int getContainerSize()
    {
        return slots;
    }

    @Override
    public boolean isEmpty()
    {
        for (ItemStack stack : contents())
            if (!stack.isEmpty())
                return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return slot >= 0 && slot < slots ? contents().get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count)
    {
        ItemStack existing = getItem(slot);
        if (existing.isEmpty())
            return ItemStack.EMPTY;

        ItemStack taken;
        if (count >= existing.getCount())
        {
            taken = existing.copy();
            setItem(slot, ItemStack.EMPTY);
        }
        else
        {
            taken = existing.split(count);
            setItem(slot, existing);
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        ItemStack existing = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack newStack)
    {
        if (slot < 0 || slot >= slots)
            return;

        NonNullList<ItemStack> items = contents();
        items.set(slot, newStack);

        ItemStack belt = holder.getBelt().copy();
        belt.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        holder.setBelt(belt);
    }

    @Override
    public int getMaxStackSize()
    {
        return 64;
    }

    @Override
    public void setChanged()
    {
    }

    @Override
    public boolean stillValid(Player player)
    {
        return !holder.getBelt().isEmpty();
    }

    @Override
    public void clearContent()
    {
        ItemStack belt = holder.getBelt().copy();
        belt.remove(DataComponents.CONTAINER);
        holder.setBelt(belt);
    }
}
