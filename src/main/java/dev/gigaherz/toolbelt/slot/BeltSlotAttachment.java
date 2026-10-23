package dev.gigaherz.toolbelt.slot;

import dev.gigaherz.toolbelt.ToolBelt;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
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
    public static final AttachmentType<ItemStack> BELT_SLOT = AttachmentRegistry.create(ToolBelt.id("belt_slot"),
            builder -> builder
                    .persistent(ItemStack.CODEC)
                    .syncWith(ItemStack.OPTIONAL_STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

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

    /**
     * While a player's death loot runs with a full inventory, the belt still in the belt slot.
     * Grave mods/data packs that snapshot the player's saved inventory at death see it through
     * {@link #deathSnapshotBelt}, so they collect it like any inventory item.
     */
    private static final ThreadLocal<DeathSnapshot> DEATH_SNAPSHOT = new ThreadLocal<>();

    private record DeathSnapshot(Player player, ItemStack belt)
    {
    }

    /**
     * Runs at the start of death loot (without keepInventory). Moves the belt into a free
     * inventory space; if there is none, marks it and exposes it in the death-time inventory
     * snapshot instead. Either way it is marked to return to the belt slot afterwards.
     */
    public static void prepareForDeath(Player player)
    {
        moveToInventoryForDeath(player);

        ItemStack remaining = get(player);
        if (!remaining.isEmpty())
        {
            remaining.set(ToolBelt.RETURN_TO_BELT_SLOT, Unit.INSTANCE);
            DEATH_SNAPSHOT.set(new DeathSnapshot(player, remaining));
        }
    }

    public static void endDeathSnapshot()
    {
        DEATH_SNAPSHOT.remove();
    }

    /** The belt to add to {@code player}'s saved inventory right now, or empty. */
    public static ItemStack deathSnapshotBelt(Player player)
    {
        DeathSnapshot snapshot = DEATH_SNAPSHOT.get();
        return snapshot != null && snapshot.player() == player ? snapshot.belt() : ItemStack.EMPTY;
    }

    /**
     * Moves the belt into a free main inventory slot so it drops (and gets picked up by grave
     * mods) with the rest of the inventory. Leaves it in place if the inventory is full;
     * {@link #dropOnDeath} then drops it separately.
     */
    public static void moveToInventoryForDeath(Player player)
    {
        ItemStack stack = get(player);
        if (stack.isEmpty())
            return;

        int free = player.getInventory().getFreeSlot();
        if (free < 0)
            return;

        set(player, ItemStack.EMPTY);
        stack.set(ToolBelt.RETURN_TO_BELT_SLOT, Unit.INSTANCE);
        player.getInventory().setItem(free, stack);
    }

    /**
     * If {@code stack} is a marked belt sitting in the player's inventory, moves it back into an
     * empty belt slot. The marker is always cleared, so a belt that can't go back stays put.
     */
    public static void returnToBeltSlot(ServerPlayer player, ItemStack stack)
    {
        if (!stack.has(ToolBelt.RETURN_TO_BELT_SLOT) || !player.isAlive())
            return;

        stack.remove(ToolBelt.RETURN_TO_BELT_SLOT);

        var inv = player.getInventory();
        if (!get(player).isEmpty())
            return;
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            if (inv.getItem(i) == stack)
            {
                inv.setItem(i, ItemStack.EMPTY);
                set(player, stack);
                return;
            }
        }
    }

    /** Called when a player dies without keepInventory, alongside the vanilla inventory drop. */
    public static void dropOnDeath(Player player, ServerLevel level)
    {
        ItemStack stack = get(player);
        if (stack.isEmpty())
            return;

        set(player, ItemStack.EMPTY);
        stack.set(ToolBelt.RETURN_TO_BELT_SLOT, Unit.INSTANCE);
        ItemEntity drop = player.createItemStackToDrop(stack, true, false);
        if (drop != null)
            level.addFreshEntity(drop);
    }
}
