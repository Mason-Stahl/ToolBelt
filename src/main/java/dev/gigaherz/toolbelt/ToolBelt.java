package dev.gigaherz.toolbelt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolBelt implements ModInitializer
{
    public static final String MODID = "toolbelt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final ResourceKey<Item> BELT_KEY = ResourceKey.create(Registries.ITEM, id("belt"));
    public static final Item BELT = new Item(new Item.Properties().stacksTo(1).setId(BELT_KEY));

    @Override
    public void onInitialize()
    {
        Registry.register(BuiltInRegistries.ITEM, BELT_KEY, BELT);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> output.accept(BELT));
    }

    public static Identifier id(String path)
    {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
