package com.telepathicgrunt.the_bumblezone.modcompat.recipecategories;

import com.mojang.blaze3d.vertex.PoseStack;
import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.modcompat.JEIIntegration;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.library.gui.elements.DrawableBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class QueenRandomizeTradesJEICategory implements IRecipeCategory<QueenRandomizerTradesInfo> {

    public static final int RECIPE_WIDTH = 136;
    public static final int RECIPE_HEIGHT = 28;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component localizedName;

    public QueenRandomizeTradesJEICategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(new ResourceLocation(Bumblezone.MODID, "textures/gui/queen_randomizer_trades_jei.png"), 0, 0, RECIPE_WIDTH, RECIPE_HEIGHT);
        this.localizedName = Component.translatable("the_bumblezone.jei.bee_queen_color_randomizing_trades");

        DrawableBuilder iconBuilder = new DrawableBuilder(new ResourceLocation("the_bumblezone", "textures/gui/bee_queen_randomize_trades.png"), 0, 0, 16, 16);
        iconBuilder.setTextureSize(16, 16);
        this.icon = iconBuilder.build();
    }

    @Override
    public RecipeType<QueenRandomizerTradesInfo> getRecipeType() {
        return JEIIntegration.QUEEN_RANDOMIZE_TRADES;
    }

    @Override
    public Component getTitle() {
        return this.localizedName;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public void draw(QueenRandomizerTradesInfo recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY) {
        Minecraft.getInstance().font.draw(stack, Component.translatable("the_bumblezone.jei.queen_trade_colors", recipe.randomizes().size()), 86, 11, 0xFF808080);
    }

    @Override
    public List<Component> getTooltipStrings(QueenRandomizerTradesInfo recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX > 32 && mouseX < 54 && mouseY > 6 && mouseY < 22) {
            String percent = String.valueOf((double)(recipe.weight()) / (recipe.totalGroupWeight()) * 100);
            return List.of(Component.translatable("the_bumblezone.jei.queen_trade_chance", percent.substring(0, Math.min(percent.length(), 5))));
        }
        return IRecipeCategory.super.getTooltipStrings(recipe, recipeSlotsView, mouseX, mouseY);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, QueenRandomizerTradesInfo recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 6).addIngredient(VanillaTypes.ITEM_STACK, recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 64, 6).addItemStacks(recipe.randomizes());
    }
}