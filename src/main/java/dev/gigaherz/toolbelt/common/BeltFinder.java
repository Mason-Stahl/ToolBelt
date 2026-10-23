package dev.gigaherz.toolbelt.common;

import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import dev.gigaherz.toolbelt.slot.BeltSlotAttachment;
import dev.gigaherz.toolbelt.slot.BeltSlotContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Finds the belt the radial menu should use: the dedicated belt slot first, then the first belt
 * in the hotbar, main inventory, and offhand. The selected hotbar slot is skipped because that is
 * the item being swapped.
 *
 * <p>Locations are plain ints so they fit in a packet: inventory slot indexes, or {@link #BELT_SLOT}.
 */
public final class BeltFinder
{
    public static final int NONE = -1;
    /** Location id for the dedicated belt slot (outside the vanilla inventory index range). */
    public static final int BELT_SLOT = 1000;

    private BeltFinder()
    {
    }

    public static int findBeltSlot(Player player)
    {
        if (isBeltSlot(player, BELT_SLOT))
            return BELT_SLOT;

        Inventory inv = player.getInventory();
        int selected = inv.getSelectedSlot();

        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++)
        {
            if (i != selected && isBeltSlot(player, i))
                return i;
        }

        if (isBeltSlot(player, Inventory.SLOT_OFFHAND))
            return Inventory.SLOT_OFFHAND;

        return NONE;
    }

    /** True if {@code location} currently holds a belt usable by the radial menu. */
    public static boolean isBeltSlot(Player player, int location)
    {
        if (location == BELT_SLOT)
            return BeltSlotAttachment.get(player).getItem() instanceof ToolBeltItem;

        Inventory inv = player.getInventory();
        boolean validIndex = (location >= 0 && location < Inventory.INVENTORY_SIZE) || location == Inventory.SLOT_OFFHAND;
        if (!validIndex || location == inv.getSelectedSlot())
            return false;
        return inv.getItem(location).getItem() instanceof ToolBeltItem;
    }

    /** Read/write access to the belt at {@code location}. Check {@link #isBeltSlot} first. */
    public static BeltContainer.Holder holder(Player player, int location)
    {
        if (location == BELT_SLOT)
            return new BeltSlotContainer(player);
        return new InventoryBeltHolder(player.getInventory(), location);
    }
}
