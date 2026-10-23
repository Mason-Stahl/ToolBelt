package dev.gigaherz.toolbelt.client.radial;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Where and how a radial menu item should draw itself. {@code x}/{@code y} is the item's center.
 */
public record DrawingContext(GuiGraphicsExtractor graphics, int width, int height, float x, float y, Font font)
{
}
