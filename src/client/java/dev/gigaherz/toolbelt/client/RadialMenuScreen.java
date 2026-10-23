package dev.gigaherz.toolbelt.client;

import dev.gigaherz.toolbelt.client.radial.GenericRadialMenu;
import dev.gigaherz.toolbelt.client.radial.IRadialMenuHost;
import dev.gigaherz.toolbelt.client.radial.ItemStackRadialMenuItem;
import dev.gigaherz.toolbelt.client.radial.RadialMenuItem;
import dev.gigaherz.toolbelt.client.radial.TextRadialMenuItem;
import dev.gigaherz.toolbelt.common.BeltContainer;
import dev.gigaherz.toolbelt.common.BeltFinder;
import dev.gigaherz.toolbelt.common.BeltRules;
import dev.gigaherz.toolbelt.network.SwapItemsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The tool swap menu, open while the menu key is held. Choosing a slot asks the server to swap;
 * nothing is changed locally, the result arrives through normal inventory sync.
 */
public class RadialMenuScreen extends Screen
{
    private int beltSlot;
    private ItemStack beltStack;

    /**
     * The menu opens on a key press, so the key starts held. Releases arrive as screen input
     * events (key mappings are all released while a screen is open, so they can't be polled).
     */
    private boolean openKeyHeld = true;

    private boolean needsRecheckStacks = true;
    private final List<ItemStackRadialMenuItem> cachedMenuItems = new ArrayList<>();
    private final TextRadialMenuItem insertMenuItem;
    private final GenericRadialMenu menu;

    public RadialMenuScreen(int beltSlot)
    {
        super(Component.translatable("text.toolbelt"));

        Minecraft mc = Minecraft.getInstance();
        this.beltSlot = beltSlot;
        this.beltStack = BeltFinder.holder(mc.player, beltSlot).getBelt();

        menu = new GenericRadialMenu(mc, new IRadialMenuHost()
        {
            @Override
            public Screen getScreen()
            {
                return RadialMenuScreen.this;
            }

            @Override
            public Font getFontRenderer()
            {
                return font;
            }
        })
        {
            @Override
            public void onClickOutside()
            {
                close();
            }
        };

        insertMenuItem = new TextRadialMenuItem(menu, Component.translatable("text.toolbelt.insert"))
        {
            @Override
            public boolean onClick()
            {
                return trySwap(SwapItemsPayload.INSERT, ItemStack.EMPTY);
            }
        };
    }

    @Override
    public void removed()
    {
        super.removed();
        ToolBeltClient.wipeOpen();
    }

    @Override
    public void onClose()
    {
        ToolBeltClient.wipeOpen();
        super.onClose();
    }

    @Override
    public void tick()
    {
        super.tick();

        menu.tick();

        if (menu.isClosed())
        {
            onClose();
            return;
        }
        if (!menu.isReady())
            return;

        // Losing focus can look like a key release; close without swapping.
        if (!minecraft.isWindowActive())
        {
            menu.close();
            return;
        }

        if (!refreshBelt())
        {
            menu.close();
            return;
        }

        if (!openKeyHeld)
        {
            if (ClientConfig.releaseToSwap)
                menu.clickItem();
            else
                menu.close();
        }
    }

    /** Tracks the belt in case it moved or changed. Returns false if the menu can't stay open. */
    private boolean refreshBelt()
    {
        if (minecraft.player == null || !BeltRules.isItemStackAllowed(minecraft.player.getMainHandItem()))
            return false;

        if (!BeltFinder.isBeltSlot(minecraft.player, beltSlot))
        {
            beltSlot = BeltFinder.findBeltSlot(minecraft.player);
            if (beltSlot == BeltFinder.NONE)
                return false;
        }

        ItemStack stack = BeltFinder.holder(minecraft.player, beltSlot).getBelt();
        if (stack != beltStack) // reference comparison intended: any replacement means contents may differ
        {
            beltStack = stack;
            needsRecheckStacks = true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (ToolBeltClient.OPEN_TOOL_MENU_KEYBIND.matches(event))
            return true; // key repeat while held
        if (menu.isReady() && ToolBeltClient.CYCLE_TOOL_MENU_LEFT_KEYBIND.matches(event))
        {
            menu.cyclePrevious();
            return true;
        }
        if (menu.isReady() && ToolBeltClient.CYCLE_TOOL_MENU_RIGHT_KEYBIND.matches(event))
        {
            menu.cycleNext();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event)
    {
        if (ToolBeltClient.OPEN_TOOL_MENU_KEYBIND.matches(event))
        {
            openKeyHeld = false;
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event)
    {
        if (ToolBeltClient.OPEN_TOOL_MENU_KEYBIND.matchesMouse(event))
            openKeyHeld = false;
        else
            menu.clickItem();
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        // No darkening: the world stays visible behind the ring.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float frameDelta)
    {
        super.extractRenderState(graphics, mouseX, mouseY, frameDelta);

        // Screens receive the frame delta here; the menu animates against the tick fraction instead.
        float partialTicks = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        ItemStack inHand = minecraft.player.getMainHandItem();
        if (BeltRules.isItemStackAllowed(inHand) && menu.isReady())
        {
            if (needsRecheckStacks)
                rebuildItems(inHand);

            boolean hasAddButton = !ClientConfig.displayEmptySlots
                    && !inHand.isEmpty()
                    && !cachedMenuItems.stream().allMatch(RadialMenuItem::isVisible);
            if (insertMenuItem.isVisible() != hasAddButton)
                insertMenuItem.setVisible(hasAddButton);

            boolean noneVisible = cachedMenuItems.stream().noneMatch(RadialMenuItem::isVisible);
            menu.setCentralText(noneVisible ? Component.translatable("text.toolbelt.empty") : null);

        }

        menu.extractRenderState(graphics, partialTicks, mouseX, mouseY);
    }

    private void rebuildItems(ItemStack inHand)
    {
        BeltContainer inventory = new BeltContainer(BeltFinder.holder(minecraft.player, beltSlot));

        cachedMenuItems.clear();
        for (int i = 0; i < inventory.getContainerSize(); i++)
            cachedMenuItems.add(createSlotItem(i, inventory.getItem(i).copy(), inHand));

        menu.clear();
        menu.addAll(cachedMenuItems);
        menu.add(insertMenuItem);

        needsRecheckStacks = false;
    }

    private ItemStackRadialMenuItem createSlotItem(int slot, ItemStack inSlot, ItemStack inHand)
    {
        ItemStackRadialMenuItem item = new ItemStackRadialMenuItem(menu, slot, inSlot, Component.translatable("text.toolbelt.empty"))
        {
            @Override
            public boolean onClick()
            {
                return trySwap(getSlot(), getStack());
            }
        };
        item.setVisible(!inSlot.isEmpty() || ClientConfig.displayEmptySlots);

        String key;
        if (!inHand.isEmpty())
            key = inSlot.isEmpty() ? "text.toolbelt.insert" : "text.toolbelt.swap";
        else
            key = inSlot.isEmpty() ? "text.toolbelt.empty" : "text.toolbelt.extract";
        item.setCentralText(Component.translatable(key));
        return item;
    }

    private boolean trySwap(int slotNumber, ItemStack itemMouseOver)
    {
        ItemStack inHand = minecraft.player.getMainHandItem();
        if (!BeltRules.isItemStackAllowed(inHand) || !menu.isReady())
            return false;

        if (!inHand.isEmpty() || !itemMouseOver.isEmpty())
            ClientPlayNetworking.send(new SwapItemsPayload(beltSlot, slotNumber));

        // Close at once after a selection; the closing animation is only for cancelling.
        menu.closeImmediately();
        onClose();
        return true;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
