package dev.gigaherz.toolbelt.test;

import dev.gigaherz.toolbelt.ToolBelt;
import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import dev.gigaherz.toolbelt.slot.BeltSlotAttachment;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Checks that a belt in the belt slot ends up inside a Vanilla Tweaks grave on death.
 * Only runs when TOOLBELT_GRAVES_PACK points at an unzipped Graves data pack folder.
 */
public class GravesCompatClientTest implements FabricClientGameTest
{
    @Override
    public void runTest(ClientGameTestContext context)
    {
        String packPath = System.getenv("TOOLBELT_GRAVES_PACK");
        if (packPath == null || packPath.isBlank())
        {
            System.out.println("TOOLBELT_TEST_RESULT graves test skipped (TOOLBELT_GRAVES_PACK not set)");
            return;
        }

        try (TestSingleplayerContext world = context.worldBuilder().create())
        {
            world.getConnection().waitForChunksRender();
            copyDir(Path.of(packPath), world.getWorldSave().getSaveDirectory().resolve("datapacks/graves"));
            world.getServer().runCommand("reload");
            context.waitTicks(20);
            world.getServer().runCommand("datapack enable \"file/graves\"");
            context.waitTicks(40);
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("gamerule keep_inventory false");

            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                ItemStack belt = ToolBeltItem.of(2);
                NonNullList<ItemStack> contents = NonNullList.withSize(2, ItemStack.EMPTY);
                contents.set(0, new ItemStack(Items.DIAMOND_PICKAXE));
                belt.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
                BeltSlotAttachment.set(player, belt);
                player.getInventory().setItem(10, new ItemStack(Items.DIRT, 5));
            });
            context.waitTicks(5);

            world.getServer().runCommand("kill @a");
            context.waitTicks(20);
            // A dead player is removed from the world and its area can unload; respawn so the grave stays loaded.
            context.clickScreenButton("deathScreen.respawn");
            context.waitTicks(40);

            String result = world.getServer().computeOnServer(server -> {
                var level = server.overworld();
                StringBuilder debug = new StringBuilder();
                long beltsOnGround = 0;
                boolean beltInGrave = false;
                int graves = 0;
                for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(-64, -128, -64, 64, 128, 64), e -> true))
                {
                    debug.append(entity.getType()).append(entity.entityTags()).append(entity.blockPosition())
                            .append(entity instanceof Display.ItemDisplay d ? d.getItemStack().getComponents().toString() : "").append(" | ");
                    if (entity instanceof ItemEntity item && item.getItem().is(ToolBelt.BELT))
                        beltsOnGround++;
                    if (!(entity instanceof Display.ItemDisplay display) || !display.entityTags().contains("graves.grave"))
                        continue;
                    graves++;
                    BundleContents stored = display.getItemStack().get(DataComponents.BUNDLE_CONTENTS);
                    if (stored != null)
                        for (var entry : stored.items())
                            beltInGrave |= entry.create().is(ToolBelt.BELT);
                }
                return "graves=" + graves + " beltInGrave=" + beltInGrave + " beltsOnGround=" + beltsOnGround
                        + (beltInGrave ? "" : " entities: " + debug);
            });
            System.out.println("TOOLBELT_TEST_RESULT " + result);
            if (!result.contains("beltInGrave=true") || !result.contains("beltsOnGround=0"))
                throw new AssertionError("Belt did not end up in the grave: " + result);

            // Open the grave by right-clicking it; the belt should end up back in the belt slot.
            world.getServer().runCommand("tp @a 0 -60 2");
            context.waitTicks(10);
            context.getInput().lookAt(new BlockPos(0, -60, 0));
            context.waitTicks(5);
            context.getInput().pressKey(options -> options.keyUse);
            context.waitTicks(40);
            String restored = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                ItemStack inSlot = BeltSlotAttachment.get(player);
                NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
                inSlot.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
                return "beltSlot=" + inSlot + " first=" + items.getFirst() + " marker=" + inSlot.has(ToolBelt.RETURN_TO_BELT_SLOT)
                        + " inv0=" + player.getInventory().getItem(0) + " dirt=" + player.getInventory().countItem(Items.DIRT);
            });
            System.out.println("TOOLBELT_TEST_RESULT grave opened: " + restored);
            if (!restored.contains("beltSlot=1 toolbelt:belt") || !restored.contains("first=1 minecraft:diamond_pickaxe")
                    || !restored.contains("marker=false") || !restored.contains("inv0=0 minecraft:air"))
                throw new AssertionError("Belt did not return to the belt slot after opening the grave: " + restored);

            // Second death with a completely full inventory: the belt must still go into the grave and come back.
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var inv = player.getInventory();
                for (int i = 0; i < 35; i++)
                    inv.setItem(i, new ItemStack(Items.DIRT, 64));
                inv.setItem(35, new ItemStack(Items.COBBLESTONE, 1));
            });
            world.getServer().runCommand("tp @a 6 -60 6");
            context.waitTicks(10);
            world.getServer().runCommand("kill @a");
            context.waitTicks(20);
            context.clickScreenButton("deathScreen.respawn");
            context.waitTicks(40);

            BlockPos grave = world.getServer().computeOnServer(server -> {
                for (var entity : server.overworld().getEntitiesOfClass(Display.ItemDisplay.class,
                        new net.minecraft.world.phys.AABB(-32, -128, -32, 32, 128, 32), d -> d.entityTags().contains("graves.grave")))
                {
                    BundleContents stored = entity.getItemStack().get(DataComponents.BUNDLE_CONTENTS);
                    if (stored != null && stored.items().stream().anyMatch(t -> t.create().is(ToolBelt.BELT)))
                        return entity.blockPosition();
                }
                return null;
            });
            if (grave == null)
                throw new AssertionError("Full inventory: belt did not end up in a grave");

            world.getServer().runCommand("tp @a " + grave.getX() + " " + grave.getY() + " " + (grave.getZ() + 2));
            context.waitTicks(10);
            context.getInput().lookAt(grave);
            context.waitTicks(5);
            context.getInput().pressKey(options -> options.keyUse);
            context.waitTicks(60);
            String full = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                ItemStack inSlot = BeltSlotAttachment.get(player);
                NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
                inSlot.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
                return "beltSlot=" + inSlot + " first=" + items.getFirst()
                        + " dirt=" + player.getInventory().countItem(Items.DIRT)
                        + " cobble=" + player.getInventory().countItem(Items.COBBLESTONE)
                        + " beltsInInventory=" + player.getInventory().countItem(ToolBelt.BELT);
            });
            System.out.println("TOOLBELT_TEST_RESULT full inventory grave opened: " + full);
            if (!full.contains("beltSlot=1 toolbelt:belt") || !full.contains("first=1 minecraft:diamond_pickaxe")
                    || !full.contains("dirt=2240") || !full.contains("cobble=1") || !full.contains("beltsInInventory=0"))
                throw new AssertionError("Full inventory death lost or misplaced items: " + full);
        }
    }

    private static void copyDir(Path from, Path to)
    {
        try (Stream<Path> files = Files.walk(from))
        {
            for (Path source : (Iterable<Path>) files::iterator)
            {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source))
                    Files.createDirectories(target);
                else
                    Files.copy(source, target);
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
