package dev.gigaherz.toolbelt.mixin.client;

import dev.gigaherz.toolbelt.client.BeltSlotRenderer;
import dev.gigaherz.toolbelt.slot.BeltSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * The creative inventory tab lays out the player's slots by index. Without this, the belt slot
 * (an extra index) would land on top of a hotbar slot.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin
{
    @ModifyArgs(method = "selectTab",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
    private void toolbelt$placeBeltSlot(Args args)
    {
        if (args.get(0) instanceof BeltSlot)
        {
            args.set(2, BeltSlot.CREATIVE_X);
            args.set(3, BeltSlot.CREATIVE_Y);
        }
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void toolbelt$drawBeltSlotFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci)
    {
        var self = (CreativeModeInventoryScreen) (Object) this;
        if (self.isInventoryOpen())
            BeltSlotRenderer.drawFrame(self, graphics, BeltSlot.CREATIVE_X, BeltSlot.CREATIVE_Y);
    }
}
