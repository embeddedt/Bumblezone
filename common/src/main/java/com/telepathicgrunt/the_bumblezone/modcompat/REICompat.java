package com.telepathicgrunt.the_bumblezone.modcompat;

import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.entities.queentrades.QueensTradeManager;
import com.telepathicgrunt.the_bumblezone.entities.queentrades.TradeEntryReducedObj;
import com.telepathicgrunt.the_bumblezone.items.recipes.IncenseCandleRecipe;
import com.telepathicgrunt.the_bumblezone.modcompat.rei.QueenRandomizerTradesREICategory;
import com.telepathicgrunt.the_bumblezone.modcompat.rei.QueenTradesREICategory;
import com.telepathicgrunt.the_bumblezone.modcompat.rei.REIQueenRandomizerTradesInfo;
import com.telepathicgrunt.the_bumblezone.modcompat.rei.REIQueenTradesInfo;
import com.telepathicgrunt.the_bumblezone.modinit.BzCreativeTabs;
import com.telepathicgrunt.the_bumblezone.modinit.BzFluids;
import com.telepathicgrunt.the_bumblezone.modinit.BzItems;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.client.BuiltinClientPlugin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.Map;

public class REICompat implements REIClientPlugin {

    public static final CategoryIdentifier<REIQueenTradesInfo> QUEEN_TRADES = CategoryIdentifier.of(Bumblezone.MODID, "queen_trades");
    public static final CategoryIdentifier<REIQueenRandomizerTradesInfo> QUEEN_RANDOMIZE_TRADES = CategoryIdentifier.of(Bumblezone.MODID, "queen_color_randomizer_trades");

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        BzCreativeTabs.CUSTOM_CREATIVE_TAB_ITEMS.forEach(item -> addInfo(item.get()));
        addInfo(BzItems.PILE_OF_POLLEN.get());
        addInfo(BzFluids.SUGAR_WATER_FLUID.get());
        addInfo(BzFluids.ROYAL_JELLY_FLUID.get());
        addInfo(BzFluids.HONEY_FLUID.get());

        registry.getRecipeManager().byKey(new ResourceLocation(Bumblezone.MODID, "incense_candle_from_super_candles"))
                .ifPresent(recipe -> registerExtraRecipes(recipe, registry, true));

        registry.getRecipeManager().byKey(new ResourceLocation(Bumblezone.MODID, "incense_candle"))
                .ifPresent(recipe -> registerExtraRecipes(recipe, registry, false));

        if (!QueensTradeManager.QUEENS_TRADE_MANAGER.tradeReduced.isEmpty()) {
            for (Map.Entry<Item, WeightedRandomList<TradeEntryReducedObj>> trade : QueensTradeManager.QUEENS_TRADE_MANAGER.tradeReduced.entrySet()) {
                for (TradeEntryReducedObj tradeResult : trade.getValue().unwrap()) {
                    if (!tradeResult.randomizerTrade()) {
                        List<ItemStack> rewardCollection = tradeResult.items().stream().map(e -> new ItemStack(e, tradeResult.count())).toList();
                        registry.add(new REIQueenTradesInfo(List.of(EntryIngredients.of(trade.getKey())), List.of(EntryIngredients.ofItemStacks(rewardCollection)), tradeResult.xpReward(), tradeResult.weight(), tradeResult.totalGroupWeight()), QUEEN_TRADES);
                    }
                }
            }
        }

        if (!QueensTradeManager.QUEENS_TRADE_MANAGER.tradeReduced.isEmpty()) {
            for (TradeEntryReducedObj tradeEntry : QueensTradeManager.QUEENS_TRADE_MANAGER.tradeRandomizer) {
                List<ItemStack> randomizeStack = tradeEntry.items().stream().map(Item::getDefaultInstance).toList();
                for (ItemStack input : randomizeStack) {
                    registry.add(new REIQueenRandomizerTradesInfo(List.of(EntryIngredients.of(input)), List.of(EntryIngredients.ofItemStacks(randomizeStack)), 1, randomizeStack.size()), QUEEN_RANDOMIZE_TRADES);
                }
            }
        }

    }

    private static void addInfo(Item item) {
        BuiltinClientPlugin.getInstance().registerInformation(
                EntryStacks.of(item),
                Component.translatable(BuiltInRegistries.ITEM.getKey(item).toString()),
                (text) -> {
                    text.add(Component.translatable(Bumblezone.MODID + "." + BuiltInRegistries.ITEM.getKey(item).getPath() + ".jei_description"));
                    return text;
                });
    }

    private static void addInfo(Fluid fluid) {
        BuiltinClientPlugin.getInstance().registerInformation(
                EntryStacks.of(fluid, 1),
                Component.translatable(BuiltInRegistries.FLUID.getKey(fluid).toString()),
                (text) -> {
                    text.add(Component.translatable(Bumblezone.MODID + "." + BuiltInRegistries.FLUID.getKey(fluid).getPath() + ".jei_description"));
                    return text;
                });
    }

    private static void registerExtraRecipes(Recipe<?> baseRecipe, DisplayRegistry registry, boolean oneRecipeOnly) {
        if (baseRecipe instanceof IncenseCandleRecipe incenseCandleRecipe) {
            List<CraftingRecipe> extraRecipes = FakeIncenseCandleRecipeCreator.constructFakeRecipes(incenseCandleRecipe, oneRecipeOnly);
            extraRecipes.forEach(registry::add);
        }
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new QueenTradesREICategory());
        registry.add(new QueenRandomizerTradesREICategory());

        registry.addWorkstations(QUEEN_TRADES, EntryStacks.of(BzItems.BEE_QUEEN_SPAWN_EGG.get()));
        registry.addWorkstations(QUEEN_RANDOMIZE_TRADES, EntryStacks.of(BzItems.BEE_QUEEN_SPAWN_EGG.get()));
    }
}