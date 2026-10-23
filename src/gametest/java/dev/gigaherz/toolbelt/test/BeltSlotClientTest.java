package dev.gigaherz.toolbelt.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.gigaherz.toolbelt.ToolBelt;
import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import dev.gigaherz.toolbelt.client.RadialMenuScreen;
import dev.gigaherz.toolbelt.client.ToolBeltClient;
import dev.gigaherz.toolbelt.slot.BeltSlot;
import dev.gigaherz.toolbelt.slot.BeltSlotAttachment;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Covers the dedicated belt slot: it shows in both inventory screens, the radial menu prefers it,
 * shift-click fills it, it survives save/reload, and it drops on death.
 * Screenshots go to build/run/clientGameTest/screenshots.
 */
public class BeltSlotClientTest implements FabricClientGameTest
{
    private static ServerPlayer player(MinecraftServer server)
    {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static ItemStack beltWith(ItemStack... tools)
    {
        ItemStack belt = ToolBeltItem.of(3);
        NonNullList<ItemStack> contents = NonNullList.withSize(3, ItemStack.EMPTY);
        for (int i = 0; i < tools.length; i++)
            contents.set(i, tools[i]);
        belt.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return belt;
    }

    private static ItemStack firstInBelt(ItemStack belt)
    {
        NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
        belt.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items.getFirst();
    }

    private static void check(boolean condition, String message)
    {
        if (!condition)
            throw new AssertionError(message);
    }

    @Override
    public void runTest(ClientGameTestContext context)
    {
        try
        {
            run(context);
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void run(ClientGameTestContext context) throws ReflectiveOperationException
    {
        TestWorldSave save;
        try (TestSingleplayerContext world = context.worldBuilder().create())
        {
            save = world.getWorldSave();
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode survival @a");

            // Belt slot holds a belt with a pickaxe; an empty belt also sits in the inventory.
            world.getServer().runOnServer(server -> {
                ServerPlayer p = player(server);
                p.getInventory().setSelectedSlot(0);
                p.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                p.getInventory().setItem(3, ToolBeltItem.of(2));
                BeltSlotAttachment.set(p, beltWith(new ItemStack(Items.DIAMOND_PICKAXE)));
            });
            context.waitTicks(10);

            // Survival inventory shows the slot above the offhand.
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(InventoryScreen.class);
            context.waitTicks(5);
            context.takeScreenshot("belt_slot_survival_inventory");
            boolean clientSees = context.computeOnClient(mc ->
                    BeltSlot.indexIn(mc.player.inventoryMenu) >= 0 && BeltSlotAttachment.get(mc.player).is(ToolBelt.BELT));
            check(clientSees, "client does not see the belt slot contents");
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(null);

            // Radial menu uses the belt slot's belt, not the empty one in the inventory.
            context.getInput().holdKey(ToolBeltClient.OPEN_TOOL_MENU_KEYBIND);
            context.waitForScreen(RadialMenuScreen.class);
            context.waitTicks(10);
            double[] target = context.computeOnClient(mc -> {
                var window = mc.getWindow();
                double scale = window.getScreenWidth() / (double) window.getGuiScaledWidth();
                return new double[] {window.getScreenWidth() / 2.0, window.getScreenHeight() / 2.0 - 45 * scale};
            });
            context.getInput().setCursorPos(target[0], target[1]);
            context.waitTicks(5);
            context.takeScreenshot("belt_slot_radial");
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.waitTicks(10);
            context.getInput().releaseKey(ToolBeltClient.OPEN_TOOL_MENU_KEYBIND);
            context.waitTicks(10);
            world.getConnection().waitForServerboundPackets();

            world.getServer().runOnServer(server -> {
                ServerPlayer p = player(server);
                check(p.getMainHandItem().is(Items.DIAMOND_PICKAXE), "hand should hold the pickaxe, has " + p.getMainHandItem());
                check(firstInBelt(BeltSlotAttachment.get(p)).is(Items.IRON_SWORD), "belt slot's belt should hold the sword");
            });

            // Shift-click a belt from the inventory into an empty belt slot.
            world.getServer().runOnServer(server -> {
                ServerPlayer p = player(server);
                ItemStack old = BeltSlotAttachment.get(p).copy();
                BeltSlotAttachment.set(p, ItemStack.EMPTY);
                p.getInventory().setItem(9, old);
                p.inventoryMenu.quickMoveStack(p, 9);
                check(BeltSlotAttachment.get(p).is(ToolBelt.BELT), "shift-click did not move the belt into the belt slot");
                check(p.getInventory().getItem(9).isEmpty(), "belt was duplicated by shift-click");
            });
        }

        // Save, reload: the belt slot must persist.
        try (TestSingleplayerContext world = save.open())
        {
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server ->
                    check(firstInBelt(BeltSlotAttachment.get(player(server))).is(Items.IRON_SWORD), "belt slot lost on reload"));
            context.waitTicks(5);
            boolean synced = context.computeOnClient(mc -> BeltSlotAttachment.get(mc.player).is(ToolBelt.BELT));
            check(synced, "belt slot not synced to client after reload");

            // Creative inventory tab shows the slot in its own spot.
            world.getServer().runCommand("gamemode creative @a");
            context.waitTicks(5);
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(CreativeModeInventoryScreen.class);
            context.<ReflectiveOperationException>runOnClient(mc -> {
                var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.INVENTORY);
                var method = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
                method.setAccessible(true);
                method.invoke(mc.gui.screen(), tab);
            });
            context.waitTicks(5);
            context.takeScreenshot("belt_slot_creative_inventory");
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitForScreen(null);

            // Death without keepInventory drops the belt slot's belt.
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("gamerule keep_inventory false");
            world.getServer().runCommand("kill @a");
            context.waitTicks(5);
            world.getServer().runOnServer(server -> {
                ServerPlayer p = player(server);
                check(BeltSlotAttachment.get(p).isEmpty(), "belt slot should be empty after death");
                boolean dropped = !p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8),
                        e -> e.getItem().is(ToolBelt.BELT) && firstInBelt(e.getItem()).is(Items.IRON_SWORD)).isEmpty();
                check(dropped, "belt slot's belt was not dropped on death");
            });

            // Respawn, then get the dropped belt back (as a grave would). It should return to the belt slot.
            context.clickScreenButton("deathScreen.respawn");
            context.waitTicks(20);
            world.getServer().runOnServer(server -> {
                ServerPlayer p = player(server);
                ItemEntity drop = server.overworld().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(64),
                        e -> e.getItem().is(ToolBelt.BELT) && firstInBelt(e.getItem()).is(Items.IRON_SWORD)).stream().findFirst().orElse(null);
                check(drop != null, "could not find the dropped belt");
                check(drop.getItem().has(ToolBelt.RETURN_TO_BELT_SLOT), "dropped belt is missing the return marker");
                p.getInventory().setItem(12, drop.getItem().copy());
                drop.discard();
            });
            context.waitTicks(5);
            world.getServer().runOnServer(server -> {
                ServerPlayer p = player(server);
                ItemStack inSlot = BeltSlotAttachment.get(p);
                check(inSlot.is(ToolBelt.BELT) && firstInBelt(inSlot).is(Items.IRON_SWORD), "belt did not return to the belt slot");
                check(!inSlot.has(ToolBelt.RETURN_TO_BELT_SLOT), "return marker was not cleared");
                check(p.getInventory().getItem(12).isEmpty(), "belt was duplicated when returning to the belt slot");
            });
        }
    }
}
