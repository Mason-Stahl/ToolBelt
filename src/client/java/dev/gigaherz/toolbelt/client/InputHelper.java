package dev.gigaherz.toolbelt.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.system.MemoryStack;

/**
 * Reads physical key and mouse state directly. Opening any screen releases all key mappings,
 * so while the radial menu is open {@link KeyMapping#isDown()} can't be used.
 * Minecraft 26.3 uses SDL for input, so mouse access goes through SDL rather than GLFW.
 */
public final class InputHelper
{
    private InputHelper()
    {
    }

    public static boolean isPhysicallyDown(KeyMapping mapping)
    {
        if (mapping == null || mapping.isUnbound())
            return false;

        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(mapping);
        int value = key.getValue();
        return switch (key.getType())
        {
            case KEYBOARD ->
            {
                try
                {
                    yield value >= 0 && InputConstants.isKeyDown(value);
                }
                catch (IndexOutOfBoundsException e)
                {
                    yield false;
                }
            }
            case MOUSE -> value >= 1 && value <= 32 && (mouseButtonFlags() & (1 << (value - 1))) != 0;
        };
    }

    private static int mouseButtonFlags()
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            return SDLMouse.SDL_GetMouseState(stack.mallocFloat(1), stack.mallocFloat(1));
        }
    }

    /** Moves the cursor to a position given in GUI-scaled coordinates. */
    public static void setGuiMousePosition(double guiX, double guiY)
    {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        float x = (float) (guiX * window.getScreenWidth() / window.getGuiScaledWidth());
        float y = (float) (guiY * window.getScreenHeight() / window.getGuiScaledHeight());
        SDLMouse.SDL_WarpMouseInWindow(window.handle(), x, y);
    }
}
