package dev.gigaherz.toolbelt;

import com.mojang.serialization.Codec;
import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import dev.gigaherz.toolbelt.common.BeltRules;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
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
    public static final Item BELT = new ToolBeltItem(new Item.Properties().stacksTo(1).setId(BELT_KEY));

    public static final DataComponentType<Integer> BELT_SIZE = DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build();

    @Override
    public void onInitialize()
    {
        Registry.register(BuiltInRegistries.ITEM, BELT_KEY, BELT);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("belt_size"), BELT_SIZE);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> output.accept(BELT));

        BeltRules.load();
    }

    public static Identifier id(String path)
    {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
