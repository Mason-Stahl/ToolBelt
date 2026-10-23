package dev.gigaherz.toolbelt.mixin;

import dev.gigaherz.toolbelt.ToolBelt;
import dev.gigaherz.toolbelt.slot.BeltSlotAttachment;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * A belt that left the belt slot on death is picked up straight into an empty belt slot,
 * even when the rest of the inventory is full.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin
{
    @Shadow
    private int pickupDelay;

    @Shadow
    private @Nullable UUID target;

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void toolbelt$pickUpIntoBeltSlot(Player player, CallbackInfo ci)
    {
        var self = (ItemEntity) (Object) this;
        if (self.level().isClientSide() || pickupDelay != 0 || (target != null && !target.equals(player.getUUID())))
            return;

        ItemStack stack = self.getItem();
        if (!stack.has(ToolBelt.RETURN_TO_BELT_SLOT) || !player.isAlive() || !BeltSlotAttachment.get(player).isEmpty())
            return;

        ItemStack belt = stack.copyWithCount(1);
        belt.remove(ToolBelt.RETURN_TO_BELT_SLOT);
        BeltSlotAttachment.set(player, belt);
        player.take(self, 1);
        stack.shrink(1);
        if (stack.isEmpty())
            self.discard();
        player.awardStat(Stats.ITEM_PICKED_UP.get(belt.getItem()), 1);
        player.onItemPickup(self);
        ci.cancel();
    }
}
