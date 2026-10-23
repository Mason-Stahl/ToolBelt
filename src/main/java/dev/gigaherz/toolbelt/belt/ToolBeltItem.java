package dev.gigaherz.toolbelt.belt;

import dev.gigaherz.toolbelt.ToolBelt;
import dev.gigaherz.toolbelt.common.BeltMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import dev.gigaherz.toolbelt.slot.BeltSlotAttachment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

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

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand)
    {
        if (hand != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        return openBeltScreen(player);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        if (context.getHand() != InteractionHand.MAIN_HAND || context.getPlayer() == null)
            return InteractionResult.PASS;
        return openBeltScreen(context.getPlayer());
    }

    private static InteractionResult openBeltScreen(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
            BeltMenu.open(serverPlayer, serverPlayer.getInventory().getSelectedSlot());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot)
    {
        if (entity instanceof ServerPlayer player)
            BeltSlotAttachment.returnToBeltSlot(player, stack);
    }

    /** Editing the belt's contents while holding it shouldn't replay the equip animation. */
    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack)
    {
        return false;
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
