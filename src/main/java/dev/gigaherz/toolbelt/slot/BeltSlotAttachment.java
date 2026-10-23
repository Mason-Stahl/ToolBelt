package dev.gigaherz.toolbelt.slot;

import dev.gigaherz.toolbelt.ToolBelt;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * The item in a player's belt slot, stored as a Fabric data attachment on the player.
 * It is saved with the player and synced to that player's own client.
 */
public final class BeltSlotAttachment
{
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<ItemStack> BELT_SLOT = AttachmentRegistry.<ItemStack>builder()
            .persistent(ItemStack.CODEC)
            .syncWith(ItemStack.OPTIONAL_STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
            .buildAndRegister(ToolBelt.id("belt_slot"));

    private BeltSlotAttachment()
    {
    }

    public static void init()
    {
        // Keep the belt across respawn only when the rest of the inventory is kept too.
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            boolean keep = alive || oldPlayer.level().getGameRules().get(GameRules.KEEP_INVENTORY);
            ItemStack stack = get(oldPlayer);
            if (keep && !stack.isEmpty())
                set(newPlayer, stack.copy());
        });
    }

    public static ItemStack get(Player player)
    {
        return player.getAttachedOrElse(BELT_SLOT, ItemStack.EMPTY);
    }

    public static void set(Player player, ItemStack stack)
    {
        if (stack.isEmpty())
            player.removeAttached(BELT_SLOT);
        else
            player.setAttached(BELT_SLOT, stack);
    }

    /** Called when a player dies without keepInventory, alongside the vanilla inventory drop. */
    public static void dropOnDeath(Player player, ServerLevel level)
    {
        ItemStack stack = get(player);
        if (stack.isEmpty())
            return;

        set(player, ItemStack.EMPTY);
        ItemEntity drop = player.createItemStackToDrop(stack, true, false);
        if (drop != null)
            level.addFreshEntity(drop);
    }
}
