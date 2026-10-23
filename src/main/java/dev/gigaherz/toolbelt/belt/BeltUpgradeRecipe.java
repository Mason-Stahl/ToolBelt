package dev.gigaherz.toolbelt.belt;

import com.mojang.serialization.MapCodec;
import dev.gigaherz.toolbelt.ToolBelt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Crafting-grid upgrade: one belt plus one pouch (shapeless) gives the same belt with one more slot.
 * Contents, dye, and name carry over. Replaces the reference's anvil and SewingKit upgrade paths.
 */
public class BeltUpgradeRecipe extends CustomRecipe
{
    public static final BeltUpgradeRecipe INSTANCE = new BeltUpgradeRecipe();
    public static final RecipeSerializer<BeltUpgradeRecipe> SERIALIZER =
            new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

    private BeltUpgradeRecipe()
    {
    }

    private static ItemStack findBelt(CraftingInput input)
    {
        ItemStack belt = ItemStack.EMPTY;
        boolean hasPouch = false;
        for (int i = 0; i < input.size(); i++)
        {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;

            if (stack.is(ToolBelt.BELT) && belt.isEmpty())
                belt = stack;
            else if (stack.is(ToolBelt.POUCH) && !hasPouch)
                hasPouch = true;
            else
                return ItemStack.EMPTY;
        }

        if (!hasPouch || belt.isEmpty() || ToolBeltItem.getBeltSize(belt) >= ToolBeltItem.MAX_SIZE)
            return ItemStack.EMPTY;
        return belt;
    }

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        return !findBelt(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input)
    {
        ItemStack belt = findBelt(input);
        if (belt.isEmpty())
            return ItemStack.EMPTY;
        return ToolBeltItem.makeUpgradedStack(belt.copyWithCount(1));
    }

    @Override
    public RecipeSerializer<BeltUpgradeRecipe> getSerializer()
    {
        return SERIALIZER;
    }
}
