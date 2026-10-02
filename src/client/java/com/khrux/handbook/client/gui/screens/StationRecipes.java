package com.khrux.handbook.client.gui.screens;

import com.khrux.handbook.mixin.RecipeBookComponentAccessor;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

public class StationRecipes {
	private static final Set<RecipeBookType> OPEN = EnumSet.noneOf(RecipeBookType.class);
	private final RecipeBookComponent<?> component;
	private final RecipeBookMenu menu;

	public StationRecipes(final RecipeBookComponent<?> component, final RecipeBookMenu menu) {
		this.component = component;
		this.menu = menu;
	}

	public static boolean isOpen(final RecipeBookType type) {
		return OPEN.contains(type);
	}

	public static void toggle(final RecipeBookType type) {
		if (!OPEN.remove(type)) {
			OPEN.add(type);
		}
	}

	public List<RecipeBookComponent.TabInfo> getTabs() {
		return ((RecipeBookComponentAccessor)this.component).handbook$getTabInfos();
	}

	public StackedItemContents contents() {
		StackedItemContents contents = new StackedItemContents();
		Minecraft.getInstance().player.getInventory().fillStackedContents(contents);
		this.menu.fillCraftSlotsStackedContents(contents);
		return contents;
	}

	public void select(final RecipeCollection collection, final StackedItemContents contents) {
		((RecipeBookComponentAccessor)this.component).handbook$selectMatchingRecipes(collection, contents);
	}

	public void place(final RecipeCollection collection, final RecipeDisplayId recipe, final boolean useMaxItems) {
		((RecipeBookComponentAccessor)this.component).handbook$tryPlaceRecipe(collection, recipe, useMaxItems);
	}
}
