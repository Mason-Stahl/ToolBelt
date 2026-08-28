package dev.gigaherz.toolbelt.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides which items are allowed into a belt slot. Whitelist and blacklist are
 * loaded from config/toolbelt.json (created with empty defaults on first run).
 */
public class BeltRules
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Set<Item> whitelist = Set.of();
    private static Set<Item> blacklist = Set.of();
    private static boolean allowAllNonStackableItems = true;

    private record Config(java.util.List<String> whitelist, java.util.List<String> blacklist, boolean allowAllNonStackableItems)
    {
        static Config defaults()
        {
            return new Config(java.util.List.of(), java.util.List.of(), true);
        }
    }

    public static void load()
    {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("toolbelt.json");
        Config config;
        if (Files.exists(path))
        {
            try (var reader = Files.newBufferedReader(path))
            {
                config = GSON.fromJson(reader, Config.class);
                if (config == null)
                    config = Config.defaults();
            }
            catch (IOException e)
            {
                LOGGER.warn("Failed to read {}, using defaults", path, e);
                config = Config.defaults();
            }
        }
        else
        {
            config = Config.defaults();
            try
            {
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(config));
            }
            catch (IOException e)
            {
                LOGGER.warn("Failed to write default {}", path, e);
            }
        }

        whitelist = config.whitelist().stream().map(BeltRules::parseItem).filter(i -> i != Items.AIR).collect(Collectors.toSet());
        blacklist = config.blacklist().stream().map(BeltRules::parseItem).filter(i -> i != Items.AIR).collect(Collectors.toSet());
        allowAllNonStackableItems = config.allowAllNonStackableItems();
    }

    private static Item parseItem(String id)
    {
        Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(Items.AIR);
        if (item == Items.AIR)
            LOGGER.warn("Could not find item {} referenced in toolbelt.json", id);
        return item;
    }

    public static boolean isItemStackAllowed(ItemStack stack)
    {
        if (stack.isEmpty())
            return true;

        Item item = stack.getItem();

        if (whitelist.contains(item))
            return true;

        if (blacklist.contains(item))
            return false;

        if (item instanceof BundleItem)
            return false;

        if (item instanceof ToolBeltItem)
            return false;

        return allowAllNonStackableItems && stack.getMaxStackSize() == 1;
    }
}
