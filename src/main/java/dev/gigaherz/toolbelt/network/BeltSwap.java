package dev.gigaherz.toolbelt.network;

import dev.gigaherz.toolbelt.common.BeltContainer;
import dev.gigaherz.toolbelt.common.BeltFinder;
import dev.gigaherz.toolbelt.common.BeltRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side handling of {@link SwapItemsPayload}. Everything the client sent is re-checked;
 * an invalid request is dropped without touching the inventory. Changes reach the client
 * through normal player inventory sync.
 */
public final class BeltSwap
{
    private BeltSwap()
    {
    }

    public static void handle(ServerPlayer player, int beltSlot, int swapWith)
    {
        if (!player.isAlive() || player.isSpectator())
            return;

        // The radial menu is not a container menu; if one is open, the request is stale or forged.
        if (player.containerMenu != player.inventoryMenu)
            return;

        if (!BeltFinder.isBeltSlot(player, beltSlot))
            return;

        BeltContainer belt = new BeltContainer(BeltFinder.holder(player, beltSlot));
        int size = belt.getContainerSize();
        if (swapWith != SwapItemsPayload.INSERT && (swapWith < 0 || swapWith >= size))
            return;

        ItemStack inHand = player.getMainHandItem().copy();
        if (!BeltRules.isItemStackAllowed(inHand))
            return;

        if (swapWith == SwapItemsPayload.INSERT)
            insert(player, belt, inHand);
        else
            swap(player, belt, swapWith, inHand);
    }

    /** Merges into matching stacks first-come, then fills the first empty slot. Leftovers stay in hand. */
    private static void insert(ServerPlayer player, BeltContainer belt, ItemStack inHand)
    {
        if (inHand.isEmpty())
            return;

        for (int i = 0; i < belt.getContainerSize() && !inHand.isEmpty(); i++)
        {
            ItemStack inSlot = belt.getItem(i);
            if (inSlot.isEmpty())
            {
                belt.setItem(i, inHand);
                inHand = ItemStack.EMPTY;
            }
            else if (ItemEntity.areMergable(inSlot, inHand))
            {
                int max = inSlot.getMaxStackSize();
                int total = inSlot.getCount() + inHand.getCount();
                int kept = Math.min(total, max);
                belt.setItem(i, inSlot.copyWithCount(kept));
                inHand = total > max ? inHand.copyWithCount(total - max) : ItemStack.EMPTY;
            }
        }

        player.setItemInHand(InteractionHand.MAIN_HAND, inHand);
    }

    private static void swap(ServerPlayer player, BeltContainer belt, int slot, ItemStack inHand)
    {
        ItemStack inSlot = belt.getItem(slot).copy();
        if (inSlot.isEmpty() && inHand.isEmpty())
            return;

        belt.setItem(slot, inHand);
        player.setItemInHand(InteractionHand.MAIN_HAND, inSlot);
    }
}
