package dev.gigaherz.toolbelt.mixin;

import dev.gigaherz.toolbelt.slot.BeltSlotAttachment;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Only while a player's death loot is running with a full inventory: adds the belt-slot belt to
 * the saved inventory list (under the last inventory slot), so grave data packs that read the
 * player's inventory at death collect it. Never affects normal saving.
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin
{
    @Shadow
    @Final
    public Player player;

    @Inject(method = "save", at = @At("TAIL"))
    private void toolbelt$addBeltToDeathSnapshot(ValueOutput.TypedOutputList<ItemStackWithSlot> output, CallbackInfo ci)
    {
        var belt = BeltSlotAttachment.deathSnapshotBelt(player);
        if (!belt.isEmpty())
            output.add(new ItemStackWithSlot(Inventory.INVENTORY_SIZE - 1, belt));
    }
}
