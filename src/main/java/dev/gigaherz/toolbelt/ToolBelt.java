package dev.gigaherz.toolbelt;

import com.mojang.serialization.Codec;
import dev.gigaherz.toolbelt.belt.BeltUpgradeRecipe;
import dev.gigaherz.toolbelt.belt.ToolBeltItem;
import dev.gigaherz.toolbelt.common.BeltMenu;
import dev.gigaherz.toolbelt.common.BeltRules;
import dev.gigaherz.toolbelt.network.BeltSwap;
import dev.gigaherz.toolbelt.network.SwapItemsPayload;
import dev.gigaherz.toolbelt.slot.BeltSlotAttachment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
    public static final ResourceKey<Item> POUCH_KEY = ResourceKey.create(Registries.ITEM, id("pouch"));

    public static final Item BELT = new ToolBeltItem(new Item.Properties().stacksTo(1).setId(BELT_KEY));
    public static final Item POUCH = new Item(new Item.Properties().setId(POUCH_KEY));

    public static final DataComponentType<Integer> BELT_SIZE = DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build();

    /**
     * The opening data is the player inventory slot that holds the belt being edited.
     */
    public static final ExtendedMenuType<BeltMenu, Integer> BELT_MENU =
            new ExtendedMenuType<>(BeltMenu::new, ByteBufCodecs.VAR_INT);

    @Override
    public void onInitialize()
    {
        Registry.register(BuiltInRegistries.ITEM, BELT_KEY, BELT);
        Registry.register(BuiltInRegistries.ITEM, POUCH_KEY, POUCH);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("belt_size"), BELT_SIZE);
        Registry.register(BuiltInRegistries.MENU, id("belt_container"), BELT_MENU);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("belt_upgrade"), BeltUpgradeRecipe.SERIALIZER);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> {
                    output.accept(BELT);
                    output.accept(POUCH);
                });

        PayloadTypeRegistry.serverboundPlay().register(SwapItemsPayload.TYPE, SwapItemsPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SwapItemsPayload.TYPE,
                (payload, context) -> BeltSwap.handle(context.player(), payload.beltSlot(), payload.swapWith()));

        BeltSlotAttachment.init();

        BeltRules.load();
    }

    public static Identifier id(String path)
    {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
