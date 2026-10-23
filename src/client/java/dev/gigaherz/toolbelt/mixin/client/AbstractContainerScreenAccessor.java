package dev.gigaherz.toolbelt.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor
{
    @Accessor("leftPos")
    int toolbelt$getLeftPos();

    @Accessor("topPos")
    int toolbelt$getTopPos();
}
