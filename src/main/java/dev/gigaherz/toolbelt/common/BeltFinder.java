package dev.gigaherz.toolbelt.common;

import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Finds the belt the radial menu should use. With no custom belt slot in this port, the belt
 * is a normal inventory item: the first belt in the hotbar, then the main inventory, then the
 * offhand. The selected hotbar slot is skipped because that is the item being swapped.
 */
public final class BeltFinder
{
    public static final int NONE = -1;

    private BeltFinder()
    {
    }

    public static int findBeltSlot(Player player)
    {
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

    /** True if {@code slot} is a usable belt location for the radial menu. */
    public static boolean isBeltSlot(Player player, int slot)
    {
        Inventory inv = player.getInventory();
        boolean validIndex = (slot >= 0 && slot < Inventory.INVENTORY_SIZE) || slot == Inventory.SLOT_OFFHAND;
        if (!validIndex || slot == inv.getSelectedSlot())
            return false;
        return inv.getItem(slot).getItem() instanceof ToolBeltItem;
    }
}
