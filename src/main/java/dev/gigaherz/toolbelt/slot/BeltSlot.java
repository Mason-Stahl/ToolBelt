package dev.gigaherz.toolbelt.slot;

import dev.gigaherz.toolbelt.ToolBelt;
import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The belt slot added to the player's inventory screen. Only accepts a tool belt.
 */
public class BeltSlot extends Slot
{
    /** Position in the survival inventory screen: directly above the offhand slot. */
    public static final int INVENTORY_X = 77;
    public static final int INVENTORY_Y = 44;
    /** Position in the creative inventory tab: mirrored across the player model from the offhand slot. */
    public static final int CREATIVE_X = 127;
    public static final int CREATIVE_Y = 20;

    public static final Identifier EMPTY_ICON = ToolBelt.id("empty_belt_slot_background");

    public BeltSlot(Player player, int x, int y)
    {
        super(new BeltSlotContainer(player), 0, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        return stack.getItem() instanceof ToolBeltItem;
    }

    @Override
    public int getMaxStackSize()
    {
        return 1;
    }

    @Override
    public @Nullable Identifier getNoItemIcon()
    {
        return EMPTY_ICON;
    }

    /** Index of the belt slot in {@code menu}, or -1 if it has none. */
    public static int indexIn(AbstractContainerMenu menu)
    {
        for (int i = menu.slots.size() - 1; i >= 0; i--)
        {
            if (menu.slots.get(i) instanceof BeltSlot)
                return i;
        }
        return -1;
    }
}
