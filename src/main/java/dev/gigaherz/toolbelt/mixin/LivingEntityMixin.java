package dev.gigaherz.toolbelt.mixin;

import dev.gigaherz.toolbelt.slot.BeltSlotAttachment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * On death, moves the belt from the belt slot into a free inventory space before any death loot
 * runs. Grave mods and data packs (e.g. Vanilla Tweaks Graves, which snapshots the inventory from
 * the player loot table) then treat it as a normal inventory item.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void toolbelt$moveBeltIntoInventoryOnDeath(ServerLevel level, DamageSource source, CallbackInfo ci)
    {
        if ((Object) this instanceof Player player && !level.getGameRules().get(GameRules.KEEP_INVENTORY))
            BeltSlotAttachment.prepareForDeath(player);
    }

    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void toolbelt$endDeathSnapshot(ServerLevel level, DamageSource source, CallbackInfo ci)
    {
        BeltSlotAttachment.endDeathSnapshot();
    }
}
