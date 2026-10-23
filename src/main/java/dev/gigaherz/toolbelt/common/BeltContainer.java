package dev.gigaherz.toolbelt.common;

import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Presents a belt ItemStack's stored contents as a vanilla Container. Changes are written
 * back through the holder, so the caller decides where the belt lives (an inventory slot,
 * etc). The belt menu and the swap logic both go through this so they share the same rules.
 *
 * <p>Vanilla menu code sometimes edits the stack returned by {@link #getItem} in place and
 * then calls {@link #setChanged}, so this keeps live stacks and writes them back on change.
 */
public class BeltContainer implements Container
{
    public interface Holder
    {
        ItemStack getBelt();

        void setBelt(ItemStack stack);
    }

    private final Holder holder;
    private NonNullList<ItemStack> items = NonNullList.create();
    private ItemStack loadedFrom = null;

    public BeltContainer(Holder holder)
    {
        this.holder = holder;
    }

    /** Reloads the cached stacks if the belt stack was replaced from outside. */
    private NonNullList<ItemStack> items()
    {
        ItemStack belt = holder.getBelt();
        if (belt != loadedFrom)
        {
            int size = belt.getItem() instanceof ToolBeltItem ? ToolBeltItem.getBeltSize(belt) : 0;
            items = NonNullList.withSize(size, ItemStack.EMPTY);
            ItemContainerContents contents = belt.get(DataComponents.CONTAINER);
            if (contents != null)
                contents.copyInto(items);
            loadedFrom = belt;
        }
        return items;
    }

    private void save()
    {
        ItemStack belt = holder.getBelt();
        if (!(belt.getItem() instanceof ToolBeltItem))
            return;

        ItemContainerContents contents = ItemContainerContents.fromItems(items);
        if (contents.equals(belt.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)))
            return;

        ItemStack updated = belt.copy();
        updated.set(DataComponents.CONTAINER, contents);
        holder.setBelt(updated);
        loadedFrom = holder.getBelt();
    }

    @Override
    public int getContainerSize()
    {
        return items().size();
    }

    @Override
    public boolean isEmpty()
    {
        for (ItemStack stack : items())
            if (!stack.isEmpty())
                return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot)
    {
        NonNullList<ItemStack> list = items();
        return slot >= 0 && slot < list.size() ? list.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count)
    {
        ItemStack existing = getItem(slot);
        if (existing.isEmpty() || count <= 0)
            return ItemStack.EMPTY;

        ItemStack taken = existing.split(count);
        save();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        ItemStack existing = getItem(slot);
        if (existing.isEmpty())
            return ItemStack.EMPTY;
        items.set(slot, ItemStack.EMPTY);
        save();
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack newStack)
    {
        NonNullList<ItemStack> list = items();
        if (slot < 0 || slot >= list.size())
            return;

        list.set(slot, newStack);
        save();
    }

    @Override
    public int getMaxStackSize()
    {
        return 64;
    }

    @Override
    public void setChanged()
    {
        items();
        save();
    }

    @Override
    public boolean stillValid(Player player)
    {
        return holder.getBelt().getItem() instanceof ToolBeltItem;
    }

    @Override
    public void clearContent()
    {
        items().replaceAll(s -> ItemStack.EMPTY);
        save();
    }
}
