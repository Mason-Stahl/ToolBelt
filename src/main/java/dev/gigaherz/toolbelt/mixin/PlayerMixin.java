package dev.gigaherz.toolbelt.mixin;

import dev.gigaherz.toolbelt.slot.BeltSlotAttachment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops the belt slot's contents on death, like the rest of the inventory (unless keepInventory).
 */
@Mixin(Player.class)
public abstract class PlayerMixin
{
    @Inject(method = "dropEquipment", at = @At("TAIL"))
    private void toolbelt$dropBeltSlot(ServerLevel level, CallbackInfo ci)
    {
        if (!level.getGameRules().get(GameRules.KEEP_INVENTORY))
            BeltSlotAttachment.dropOnDeath((Player) (Object) this, level);
    }
}
