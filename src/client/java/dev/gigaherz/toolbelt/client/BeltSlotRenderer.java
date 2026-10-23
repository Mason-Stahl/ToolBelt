package dev.gigaherz.toolbelt.client;

import dev.gigaherz.toolbelt.mixin.client.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Draws a slot frame for the belt slot. The vanilla inventory textures have no frame at that
 * spot, so it's drawn on top of the background before slots are drawn.
 */
public final class BeltSlotRenderer
{
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    private BeltSlotRenderer()
    {
    }

    public static void drawFrame(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int slotX, int slotY)
    {
        var accessor = (AbstractContainerScreenAccessor) screen;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                accessor.toolbelt$getLeftPos() + slotX - 1, accessor.toolbelt$getTopPos() + slotY - 1, 18, 18);
    }
}
