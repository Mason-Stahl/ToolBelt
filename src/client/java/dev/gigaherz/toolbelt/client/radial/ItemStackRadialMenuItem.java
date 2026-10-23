package dev.gigaherz.toolbelt.client.radial;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A belt slot in the radial menu. Draws the stored item, or {@code altText} when the slot is empty.
 */
public class ItemStackRadialMenuItem extends TextRadialMenuItem
{
    private final int slot;
    private final ItemStack stack;

    public ItemStackRadialMenuItem(GenericRadialMenu owner, int slot, ItemStack stack, Component altText)
    {
        super(owner, altText, 0x7FFFFFFF);
        this.slot = slot;
        this.stack = stack;
    }

    public int getSlot()
    {
        return slot;
    }

    public ItemStack getStack()
    {
        return stack;
    }

    @Override
    public void extractRenderState(DrawingContext context)
    {
        if (!stack.isEmpty())
        {
            int x = (int) context.x() - 8;
            int y = (int) context.y() - 8;
            context.graphics().item(stack, x, y);
            context.graphics().itemDecorations(context.font(), stack, x, y);
        }
        else
        {
            super.extractRenderState(context);
        }
    }

    @Override
    public void prepareTooltip(DrawingContext context)
    {
        if (!stack.isEmpty())
            context.graphics().setTooltipForNextFrame(context.font(), stack, (int) context.x(), (int) context.y());
    }
}
