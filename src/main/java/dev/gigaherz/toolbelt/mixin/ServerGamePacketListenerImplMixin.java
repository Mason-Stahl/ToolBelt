package dev.gigaherz.toolbelt.mixin;

import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import dev.gigaherz.toolbelt.slot.BeltSlot;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla only accepts creative-mode inventory edits for slots 1-45. Accept them for the belt
 * slot too, with the same rule as the slot itself (empty, or a single belt).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin
{
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
                    shift = At.Shift.AFTER),
            cancellable = true)
    private void toolbelt$creativeBeltSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci)
    {
        int beltIndex = BeltSlot.indexIn(player.inventoryMenu);
        if (beltIndex < 0 || packet.slotNum() != beltIndex)
            return;

        ci.cancel();
        if (!player.hasInfiniteMaterials())
            return;

        ItemStack stack = packet.itemStack();
        boolean valid = stack.isEmpty() || (stack.getItem() instanceof ToolBeltItem && stack.getCount() == 1);
        if (!valid)
            return;

        player.inventoryMenu.getSlot(beltIndex).setByPlayer(stack);
        player.inventoryMenu.setRemoteSlot(beltIndex, stack);
        player.inventoryMenu.broadcastChanges();
    }
}
