package com.khrux.handbook.mixin;

import java.util.List;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
	@Accessor("tabInfos")
	List<RecipeBookComponent.TabInfo> handbook$getTabInfos();

	@Invoker("selectMatchingRecipes")
	void handbook$selectMatchingRecipes(RecipeCollection collection, StackedItemContents stackedContents);

	@Invoker("tryPlaceRecipe")
	boolean handbook$tryPlaceRecipe(RecipeCollection collection, RecipeDisplayId recipe, boolean useMaxItems);
}
