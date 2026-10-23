package dev.gigaherz.toolbelt.mixin;

import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import dev.gigaherz.toolbelt.slot.BeltSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the belt slot to the player's inventory menu (after all vanilla slots, so vanilla
 * indexes are unchanged) and lets shift-click move a belt into it.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractCraftingMenu
{
    private InventoryMenuMixin(MenuType<?> menuType, int containerId, int width, int height)
    {
        super(menuType, containerId, width, height);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void toolbelt$addBeltSlot(Inventory inventory, boolean active, Player owner, CallbackInfo ci)
    {
        this.addSlot(new BeltSlot(owner, BeltSlot.INVENTORY_X, BeltSlot.INVENTORY_Y));
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void toolbelt$quickMoveIntoBeltSlot(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir)
    {
        // Only from the main inventory and hotbar (vanilla slots 9-44).
        if (slotIndex < InventoryMenu.INV_SLOT_START || slotIndex >= InventoryMenu.USE_ROW_SLOT_END)
            return;

        int beltIndex = BeltSlot.indexIn(this);
        if (beltIndex < 0 || this.slots.get(beltIndex).hasItem())
            return;

        Slot slot = this.slots.get(slotIndex);
        ItemStack stack = slot.getItem();
        if (!(stack.getItem() instanceof ToolBeltItem))
            return;

        ItemStack clicked = stack.copy();
        if (!this.moveItemStackTo(stack, beltIndex, beltIndex + 1, false))
            return; // let vanilla handle it

        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY, clicked);
        else
            slot.setChanged();
        slot.onTake(player, stack);
        cir.setReturnValue(clicked);
    }
}
