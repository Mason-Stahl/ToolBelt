package dev.gigaherz.toolbelt.mixin.client;

import dev.gigaherz.toolbelt.client.BeltSlotRenderer;
import dev.gigaherz.toolbelt.slot.BeltSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin
{
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void toolbelt$drawBeltSlotFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci)
    {
        BeltSlotRenderer.drawFrame((InventoryScreen) (Object) this, graphics, BeltSlot.INVENTORY_X, BeltSlot.INVENTORY_Y);
    }
}
