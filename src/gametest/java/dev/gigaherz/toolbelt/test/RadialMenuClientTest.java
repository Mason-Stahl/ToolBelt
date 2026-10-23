package dev.gigaherz.toolbelt.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import dev.gigaherz.toolbelt.client.RadialMenuScreen;
import dev.gigaherz.toolbelt.client.ToolBeltClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Opens the radial menu with the real key binding, hovers the top slot, clicks, and checks the
 * server swapped the held sword with the pickaxe stored in the belt. Screenshots go to
 * run/clientGameTest/screenshots.
 */
public class RadialMenuClientTest implements FabricClientGameTest
{
    @Override
    public void runTest(ClientGameTestContext context)
    {
        try (TestSingleplayerContext world = context.worldBuilder().create())
        {
            world.getConnection().waitForChunksRender();

            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var inv = player.getInventory();
                inv.setSelectedSlot(0);
                inv.setItem(0, new ItemStack(Items.IRON_SWORD));

                ItemStack belt = ToolBeltItem.of(4);
                NonNullList<ItemStack> contents = NonNullList.withSize(4, ItemStack.EMPTY);
                contents.set(0, new ItemStack(Items.DIAMOND_PICKAXE));
                contents.set(1, new ItemStack(Items.DIAMOND_AXE));
                contents.set(2, new ItemStack(Items.DIAMOND_SHOVEL));
                belt.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
                inv.setItem(5, belt);
            });
            context.waitTicks(10);

            context.getInput().holdKey(ToolBeltClient.OPEN_TOOL_MENU_KEYBIND);
            context.waitForScreen(RadialMenuScreen.class);
            context.waitTicks(10);
            context.takeScreenshot("toolbelt_radial_open");

            // Hover the first slot (straight up from the center), in window coordinates.
            double[] target = context.computeOnClient(mc -> {
                var window = mc.getWindow();
                double scale = window.getScreenWidth() / (double) window.getGuiScaledWidth();
                return new double[] {window.getScreenWidth() / 2.0, window.getScreenHeight() / 2.0 - 45 * scale};
            });
            context.getInput().setCursorPos(target[0], target[1]);
            context.waitTicks(5);
            context.takeScreenshot("toolbelt_radial_hover");

            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.waitTicks(10);
            context.getInput().releaseKey(ToolBeltClient.OPEN_TOOL_MENU_KEYBIND);
            context.waitTicks(10);
            world.getConnection().waitForServerboundPackets();

            String result = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                ItemStack hand = player.getMainHandItem();
                ItemStack belt = player.getInventory().getItem(5);
                var stored = belt.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
                NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
                stored.copyInto(items);
                return "hand=" + hand + " belt0=" + items.get(0);
            });
            System.out.println("TOOLBELT_TEST_RESULT " + result);
            context.takeScreenshot("toolbelt_after_swap");

            if (!result.contains("hand=1 minecraft:diamond_pickaxe") || !result.contains("belt0=1 minecraft:iron_sword"))
                throw new AssertionError("Swap did not happen: " + result);

            // Empty belt and empty hand: nothing to do, so the menu stays closed and a message shows.
            world.getServer().runOnServer(server -> {
                var inv = server.getPlayerList().getPlayers().getFirst().getInventory();
                inv.setItem(0, ItemStack.EMPTY);
                inv.setItem(5, ToolBeltItem.of(2));
            });
            context.waitTicks(10);
            context.getInput().holdKey(ToolBeltClient.OPEN_TOOL_MENU_KEYBIND);
            context.waitTicks(5);
            boolean opened = context.computeOnClient(mc -> mc.gui.screen() instanceof RadialMenuScreen);
            context.takeScreenshot("toolbelt_radial_empty");
            context.getInput().releaseKey(ToolBeltClient.OPEN_TOOL_MENU_KEYBIND);
            if (opened)
                throw new AssertionError("Radial menu opened with nothing to swap");
        }
    }
}
