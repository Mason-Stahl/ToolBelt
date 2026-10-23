package dev.gigaherz.toolbelt.client;

import dev.gigaherz.toolbelt.ToolBelt;
import dev.gigaherz.toolbelt.common.BeltMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class BeltScreen extends AbstractContainerScreen<BeltMenu>
{
    private static final Identifier GUI_TEXTURE = ToolBelt.id("textures/gui/belt.png");

    public BeltScreen(BeltMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title, 176, 133);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // The texture's belt row is drawn separately so it matches the belt's size.
        int slots = this.menu.beltSize;
        int rowWidth = slots * 18;
        int x = 7 + ((9 - slots) * 18) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, left + x, top + 19, 0, this.imageHeight, rowWidth, 18, 256, 256);
    }
}
