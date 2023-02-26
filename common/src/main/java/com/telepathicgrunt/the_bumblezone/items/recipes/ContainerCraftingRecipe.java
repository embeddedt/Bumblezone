package com.telepathicgrunt.the_bumblezone.items.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.telepathicgrunt.the_bumblezone.modinit.BzRecipes;
import com.telepathicgrunt.the_bumblezone.utils.PlatformHooks;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.Map;
import java.util.Objects;

import static java.util.Map.entry;

public class ContainerCraftingRecipe extends ShapelessRecipe {
    private final String group;
    private final ItemStack recipeOutput;
    private final NonNullList<Ingredient> recipeItems;
    public static final Map<Item, Item> HARDCODED_EDGECASES_WITHOUT_CONTAINERS_SET = Map.ofEntries(
            entry(Items.POWDER_SNOW_BUCKET, Items.BUCKET),
            entry(Items.AXOLOTL_BUCKET, Items.BUCKET),
            entry(Items.COD_BUCKET, Items.BUCKET),
            entry(Items.PUFFERFISH_BUCKET, Items.BUCKET),
            entry(Items.SALMON_BUCKET, Items.BUCKET),
            entry(Items.TROPICAL_FISH_BUCKET, Items.BUCKET),
            entry(Items.SUSPICIOUS_STEW, Items.BOWL),
            entry(Items.MUSHROOM_STEW, Items.BOWL),
            entry(Items.RABBIT_STEW, Items.BOWL),
            entry(Items.BEETROOT_SOUP, Items.BOWL),
            entry(Items.POTION, Items.GLASS_BOTTLE),
            entry(Items.SPLASH_POTION, Items.GLASS_BOTTLE),
            entry(Items.LINGERING_POTION, Items.GLASS_BOTTLE),
            entry(Items.EXPERIENCE_BOTTLE, Items.GLASS_BOTTLE)
    );

    public ContainerCraftingRecipe(ResourceLocation idIn, String groupIn, CraftingBookCategory craftingBookCategory, ItemStack recipeOutputIn, NonNullList<Ingredient> recipeItemsIn) {
        super(idIn, groupIn, craftingBookCategory, recipeOutputIn, recipeItemsIn);
        this.group = groupIn;
        this.recipeOutput = recipeOutputIn;
        this.recipeItems = recipeItemsIn;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BzRecipes.CONTAINER_CRAFTING_RECIPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return recipeItems;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> remainingInv = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
        int containerOutput = PlatformHooks.hasCraftingRemainder(recipeOutput) ? recipeOutput.getCount() : 0;

        for(int i = 0; i < remainingInv.size(); ++i) {
            ItemStack craftingInput = inv.getItem(i);
            ItemStack craftingContainer = PlatformHooks.getCraftingRemainder(craftingInput);
            ItemStack recipeContainer = PlatformHooks.getCraftingRemainder(recipeOutput);
            if (craftingContainer.isEmpty() && HARDCODED_EDGECASES_WITHOUT_CONTAINERS_SET.containsKey(craftingInput.getItem())) {
                craftingContainer = HARDCODED_EDGECASES_WITHOUT_CONTAINERS_SET.get(craftingInput.getItem()).getDefaultInstance();
            }
            if (recipeContainer.isEmpty() && HARDCODED_EDGECASES_WITHOUT_CONTAINERS_SET.containsKey(recipeOutput.getItem())) {
                recipeContainer = HARDCODED_EDGECASES_WITHOUT_CONTAINERS_SET.get(recipeOutput.getItem()).getDefaultInstance();
            }

            if (!craftingContainer.isEmpty()) {
                if(containerOutput > 0 &&
                    (recipeOutput.getItem() == craftingContainer.getItem() ||
                    recipeContainer.getItem() == craftingInput.getItem() ||
                    recipeContainer.getItem() == craftingContainer.getItem()))
                {
                    containerOutput--;
                }
                else {
                    remainingInv.set(i, craftingContainer);
                }
            }
        }

        return remainingInv;
    }

    public static JsonObject itemStackFromJson(ItemStack itemStack) {
        JsonObject json = new JsonObject();
        json.addProperty("count", itemStack.getCount());
        json.addProperty("item", BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString());
        return json;
    }

    public static class Serializer implements RecipeSerializer<ContainerCraftingRecipe>, BzRecipeSerializer<ContainerCraftingRecipe> {
        @Override
        public ContainerCraftingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String s = GsonHelper.getAsString(json, "group", "");
            CraftingBookCategory craftingBookCategory = Objects.requireNonNullElse(
                    CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null)), CraftingBookCategory.MISC
            );
            NonNullList<Ingredient> DefaultedList = getIngredients(GsonHelper.getAsJsonArray(json, "ingredients"));
            if (DefaultedList.isEmpty()) {
                throw new JsonParseException("No ingredients for shapeless recipe");
            }
            else {
                ItemStack itemstack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
                return new ContainerCraftingRecipe(recipeId, s, craftingBookCategory, itemstack, DefaultedList);
            }
        }

        public JsonObject toJson(ContainerCraftingRecipe recipe) {
            JsonObject json = new JsonObject();
            json.addProperty("group", recipe.getGroup());

            JsonArray array = new JsonArray();
            recipe.recipeItems.stream().map(Ingredient::toJson).forEach(array::add);
            json.add("ingredients", array);

            json.add("result", ContainerCraftingRecipe.itemStackFromJson(recipe.recipeOutput));
            return json;
        }

        private static NonNullList<Ingredient> getIngredients(JsonArray jsonElements) {
            NonNullList<Ingredient> defaultedList = NonNullList.create();

            for (int i = 0; i < jsonElements.size(); ++i) {
                Ingredient ingredient = Ingredient.fromJson(jsonElements.get(i));
                if (!ingredient.isEmpty()) {
                    defaultedList.add(ingredient);
                }
            }

            return defaultedList;
        }

        @Override
        public ContainerCraftingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            String s = buffer.readUtf(32767);
            CraftingBookCategory craftingBookCategory = buffer.readEnum(CraftingBookCategory.class);
            int i = buffer.readVarInt();
            NonNullList<Ingredient> defaultedList = NonNullList.withSize(i, Ingredient.EMPTY);

            for (int j = 0; j < defaultedList.size(); ++j) {
                defaultedList.set(j, Ingredient.fromNetwork(buffer));
            }

            ItemStack itemstack = buffer.readItem();
            return new ContainerCraftingRecipe(recipeId, s, craftingBookCategory, itemstack, defaultedList);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ContainerCraftingRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeEnum(recipe.category());
            buffer.writeVarInt(recipe.recipeItems.size());

            for (Ingredient ingredient : recipe.recipeItems) {
                ingredient.toNetwork(buffer);
            }

            buffer.writeItem(recipe.recipeOutput);
        }
    }
}