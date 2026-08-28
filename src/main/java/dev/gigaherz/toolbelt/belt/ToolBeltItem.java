package dev.gigaherz.toolbelt.belt;

import dev.gigaherz.toolbelt.ToolBelt;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.Objects;
import java.util.function.Consumer;

public class ToolBeltItem extends Item
{
    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 9;

    public ToolBeltItem(Properties properties)
    {
        super(properties);
    }

    public static ItemStack of(int size)
    {
        return setBeltSize(new ItemStack(ToolBelt.BELT), size);
    }

    public static int getBeltSize(ItemStack stack)
    {
        int size = Objects.requireNonNullElse(stack.get(ToolBelt.BELT_SIZE), MIN_SIZE);
        return Math.clamp(size, MIN_SIZE, MAX_SIZE);
    }

    public static ItemStack setBeltSize(ItemStack stack, int newSize)
    {
        newSize = Math.clamp(newSize, MIN_SIZE, MAX_SIZE);
        int oldSize = getBeltSize(stack);
        if (newSize != oldSize)
        {
            ItemContainerContents oldInv = stack.get(DataComponents.CONTAINER);
            if (oldInv != null)
            {
                int fill = Math.min(oldSize, newSize);
                NonNullList<ItemStack> items = NonNullList.withSize(fill, ItemStack.EMPTY);
                oldInv.copyInto(items);
                stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            }
            stack.set(ToolBelt.BELT_SIZE, newSize);
        }
        return stack;
    }

    public static ItemStack makeUpgradedStack(ItemStack stack)
    {
        int size = getBeltSize(stack);
        if (size >= MAX_SIZE)
            return stack.copy();

        ItemStack upgraded = stack.copy();
        setBeltSize(upgraded, size + 1);
        return upgraded;
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag)
    {
        int size = getBeltSize(stack);
        consumer.accept(Component.translatable("text.toolbelt.tooltip", size - MIN_SIZE, size));
    }
}
