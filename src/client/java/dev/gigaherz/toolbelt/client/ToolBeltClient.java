package dev.gigaherz.toolbelt.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.gigaherz.toolbelt.ToolBelt;
import dev.gigaherz.toolbelt.common.BeltContainer;
import dev.gigaherz.toolbelt.common.BeltFinder;
import dev.gigaherz.toolbelt.common.BeltRules;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;

public class ToolBeltClient implements ClientModInitializer
{
    public static KeyMapping OPEN_TOOL_MENU_KEYBIND;
    public static KeyMapping CYCLE_TOOL_MENU_LEFT_KEYBIND;
    public static KeyMapping CYCLE_TOOL_MENU_RIGHT_KEYBIND;

    private static boolean toolMenuKeyWasDown = false;

    @Override
    public void onInitializeClient()
    {
        ClientConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(ToolBelt.id("toolbelt"));
        OPEN_TOOL_MENU_KEYBIND = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.toolbelt.open", InputConstants.KEY_R, category));
        CYCLE_TOOL_MENU_LEFT_KEYBIND = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.toolbelt.cycle.left", InputConstants.UNKNOWN.getValue(), category));
        CYCLE_TOOL_MENU_RIGHT_KEYBIND = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.toolbelt.cycle.right", InputConstants.UNKNOWN.getValue(), category));

        MenuScreens.register(ToolBelt.BELT_MENU, BeltScreen::new);

        ClientTickEvents.START_CLIENT_TICK.register(ToolBeltClient::handleKeys);

        // Hide the crosshair while the radial menu is up; it would sit in the middle of the ring.
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, deltaTracker) -> {
            if (!(Minecraft.getInstance().gui.screen() instanceof RadialMenuScreen))
                original.extractRenderState(graphics, deltaTracker);
        });
    }

    /** Drops queued presses so closing the menu doesn't immediately reopen it. */
    public static void wipeOpen()
    {
        while (OPEN_TOOL_MENU_KEYBIND != null && OPEN_TOOL_MENU_KEYBIND.consumeClick())
        {
            // discard
        }
    }

    private static void handleKeys(Minecraft mc)
    {
        if (mc.player == null || mc.gui.screen() != null)
        {
            // Treat the key as held so releasing it after a screen closes can't reopen the menu.
            toolMenuKeyWasDown = true;
            wipeOpen();
            return;
        }

        boolean toolMenuKeyIsDown = OPEN_TOOL_MENU_KEYBIND.isDown();
        if (toolMenuKeyIsDown && !toolMenuKeyWasDown && BeltRules.isItemStackAllowed(mc.player.getMainHandItem()))
        {
            int beltSlot = BeltFinder.findBeltSlot(mc.player);
            if (beltSlot != BeltFinder.NONE)
            {
                boolean beltEmpty = new BeltContainer(BeltFinder.holder(mc.player, beltSlot)).isEmpty();
                if (beltEmpty && mc.player.getMainHandItem().isEmpty())
                    mc.player.sendOverlayMessage(Component.translatable("text.toolbelt.nothing_to_swap"));
                else
                    mc.gui.setScreen(new RadialMenuScreen(beltSlot));
            }
        }
        toolMenuKeyWasDown = toolMenuKeyIsDown;
        wipeOpen();
    }
}
