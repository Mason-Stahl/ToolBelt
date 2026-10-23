package dev.gigaherz.toolbelt.common;

import dev.gigaherz.toolbelt.ToolBelt;
import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The belt's inventory screen. The belt being edited stays in its player inventory slot,
 * which is shown locked so it can't be moved (or put inside itself) while open.
 */
public class BeltMenu extends AbstractContainerMenu
{
    public static final int PLAYER_INV_LEFT = 8;
    public static final int PLAYER_INV_TOP = 51;
    public static final int HOTBAR_TOP = PLAYER_INV_TOP + 3 * SLOT_SIZE + 4;
    public static final int BELT_SLOTS_TOP = 20;

    private final Inventory playerInventory;
    private final int beltSlot;
    public final int beltSize;

    public static void open(ServerPlayer player, int slot)
    {
        if (slot < 0 || slot >= Inventory.INVENTORY_SIZE)
            return;

        ItemStack belt = player.getInventory().getItem(slot);
        if (!(belt.getItem() instanceof ToolBeltItem))
            return;

        Component title = belt.getHoverName();
        player.openMenu(new ExtendedMenuProvider<Integer>()
        {
            @Override
            public Integer getScreenOpeningData(ServerPlayer player)
            {
                return slot;
            }

            @Override
            public Component getDisplayName()
            {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player)
            {
                return new BeltMenu(id, inventory, slot);
            }
        });
    }

    public BeltMenu(int id, Inventory playerInventory, Integer beltSlot)
    {
        super(ToolBelt.BELT_MENU, id);
        this.playerInventory = playerInventory;
        this.beltSlot = beltSlot;

        boolean validSlot = beltSlot >= 0 && beltSlot < Inventory.INVENTORY_SIZE
                && playerInventory.getItem(beltSlot).getItem() instanceof ToolBeltItem;
        this.beltSize = validSlot ? ToolBeltItem.getBeltSize(playerInventory.getItem(beltSlot)) : 0;

        if (beltSize > 0)
        {
            BeltContainer belt = new BeltContainer(new InventoryBeltHolder(playerInventory, beltSlot));
            int xoff = ((SLOTS_PER_ROW - beltSize) * SLOT_SIZE) / 2;
            for (int k = 0; k < beltSize; ++k)
            {
                this.addSlot(new Slot(belt, k, PLAYER_INV_LEFT + xoff + k * SLOT_SIZE, BELT_SLOTS_TOP)
                {
                    @Override
                    public boolean mayPlace(ItemStack stack)
                    {
                        return BeltRules.isItemStackAllowed(stack);
                    }
                });
            }
        }

        bindPlayerInventory();
    }

    private void bindPlayerInventory()
    {
        for (int row = 0; row < 3; ++row)
        {
            for (int column = 0; column < SLOTS_PER_ROW; ++column)
            {
                int index = column + (row + 1) * SLOTS_PER_ROW;
                addPlayerSlot(index, PLAYER_INV_LEFT + column * SLOT_SIZE, PLAYER_INV_TOP + row * SLOT_SIZE);
            }
        }

        for (int column = 0; column < SLOTS_PER_ROW; ++column)
        {
            addPlayerSlot(column, PLAYER_INV_LEFT + column * SLOT_SIZE, HOTBAR_TOP);
        }
    }

    private void addPlayerSlot(int index, int x, int y)
    {
        this.addSlot(index == beltSlot
                ? new LockedSlot(playerInventory, index, x, y)
                : new Slot(playerInventory, index, x, y));
    }

    @Override
    public boolean stillValid(Player player)
    {
        if (beltSize <= 0 || !player.isAlive())
            return false;
        ItemStack stack = playerInventory.getItem(beltSlot);
        return stack.getItem() instanceof ToolBeltItem && ToolBeltItem.getBeltSize(stack) == beltSize;
    }

    @Override
    public void clicked(int slot, int button, ContainerInput input, Player player)
    {
        // Number-key swaps target a hotbar slot by index; never let one move the open belt.
        if (input == ContainerInput.SWAP && button == beltSlot)
            return;
        super.clicked(slot, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        if (index < 0 || index >= this.slots.size())
            return ItemStack.EMPTY;

        Slot slot = this.slots.get(index);
        if (!slot.hasItem() || slot instanceof LockedSlot)
            return ItemStack.EMPTY;

        ItemStack containedStack = slot.getItem();
        ItemStack originalStack = containedStack.copy();

        boolean moved = index < beltSize
                ? this.moveItemStackTo(containedStack, beltSize, this.slots.size(), true)
                : this.moveItemStackTo(containedStack, 0, beltSize, false);
        if (!moved)
            return ItemStack.EMPTY;

        if (containedStack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();

        return originalStack;
    }
}
