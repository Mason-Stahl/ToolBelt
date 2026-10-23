package dev.gigaherz.toolbelt.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.gigaherz.toolbelt.ToolBelt;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-only radial menu options, stored in config/toolbelt-client.json.
 * Missing fields keep their defaults, and the file is rewritten so new options show up.
 */
public final class ClientConfig
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Releasing the menu key activates the hovered slot. Otherwise a click is needed. */
    public static boolean releaseToSwap = true;
    /** Keep the mouse from leaving the outer circle. */
    public static boolean clipMouseToCircle = false;
    /** Clicking outside the outer circle still activates the slot in that direction. */
    public static boolean allowClickOutsideBounds = false;
    /** Always show every belt slot, including empty ones, so you can pick where to insert. */
    public static boolean displayEmptySlots = false;
    /** Draw the menu as squares instead of a ring. */
    public static boolean minecraftHasNoCircles = false;

    private static final class Data
    {
        boolean releaseToSwap = true;
        boolean clipMouseToCircle = false;
        boolean allowClickOutsideBounds = false;
        boolean displayEmptySlots = false;
        boolean minecraftHasNoCircles = false;
    }

    private ClientConfig()
    {
    }

    public static void load()
    {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("toolbelt-client.json");
        Data data = null;
        if (Files.exists(path))
        {
            try (var reader = Files.newBufferedReader(path))
            {
                data = GSON.fromJson(reader, Data.class);
            }
            catch (IOException | RuntimeException e)
            {
                ToolBelt.LOGGER.warn("Failed to read {}, using defaults", path, e);
            }
        }
        if (data == null)
            data = new Data();

        releaseToSwap = data.releaseToSwap;
        clipMouseToCircle = data.clipMouseToCircle;
        allowClickOutsideBounds = data.allowClickOutsideBounds;
        displayEmptySlots = data.displayEmptySlots;
        minecraftHasNoCircles = data.minecraftHasNoCircles;

        try
        {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(data));
        }
        catch (IOException e)
        {
            ToolBelt.LOGGER.warn("Failed to write {}", path, e);
        }
    }
}
