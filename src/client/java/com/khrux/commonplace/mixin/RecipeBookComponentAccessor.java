package com.khrux.commonplace.mixin;

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
	List<RecipeBookComponent.TabInfo> commonplace$getTabInfos();

	@Invoker("selectMatchingRecipes")
	void commonplace$selectMatchingRecipes(RecipeCollection collection, StackedItemContents stackedContents);

	@Invoker("tryPlaceRecipe")
	boolean commonplace$tryPlaceRecipe(RecipeCollection collection, RecipeDisplayId recipe, boolean useMaxItems);
}
