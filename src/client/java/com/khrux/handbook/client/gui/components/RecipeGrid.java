package com.khrux.handbook.client.gui.components;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.gui.screens.RecipesPage;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

public class RecipeGrid extends AbstractWidget {
	public static final int COLUMNS = 5;
	public static final int ROWS = 5;
	public static final int PER_PAGE = COLUMNS * ROWS;
	private static final int CELL = 25;
	private static final Identifier SLOT_MANY_CRAFTABLE_SPRITE = Handbook.id("recipe_slot_many_craftable");
	private static final Identifier SLOT_CRAFTABLE_SPRITE = Handbook.id("recipe_slot_craftable");
	private static final Identifier SLOT_MANY_UNCRAFTABLE_SPRITE = Handbook.id("recipe_slot_many_uncraftable");
	private static final Identifier SLOT_UNCRAFTABLE_SPRITE = Handbook.id("recipe_slot_uncraftable");
	private static final Identifier SLOT_SELECTED_SPRITE = Handbook.id("recipe_slot_selected");
	private static final int BOX = 23;
	private final RecipesPage page;

	public RecipeGrid(final int x, final int y, final RecipesPage page) {
		super(x, y, COLUMNS * CELL, ROWS * CELL, Component.translatable("handbook.tab.recipes"));
		this.page = page;
	}

	@Override
	protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		RecipeCollection variants = this.page.getVariants();
		if (variants != null) {
			this.extractVariants(graphics, variants, mouseX, mouseY);
			return;
		}

		List<RecipeCollection> collections = this.page.getVisibleCollections();
		for (int i = 0; i < collections.size(); i++) {
			RecipeCollection collection = collections.get(i);
			int x = this.getX() + i % COLUMNS * CELL;
			int y = this.getY() + i / COLUMNS * CELL;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, getSprite(collection), x + 1, y + 1, BOX, BOX);
			if (this.page.isSelected(collection)) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SELECTED_SPRITE, x + 1, y + 1, BOX, BOX);
			}

			ItemStack result = this.page.getResult(collection);
			graphics.fakeItem(result, x + 4, y + 4);
			graphics.fill(x + 4, y + 4, x + 20, y + 20, RecipesPage.SEPIA_WASH);

			if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
				graphics.setTooltipForNextFrame(Minecraft.getInstance().font, result, mouseX, mouseY);
			}
		}
	}

	private void extractVariants(final GuiGraphicsExtractor graphics, final RecipeCollection variants, final int mouseX, final int mouseY) {
		List<RecipeDisplayEntry> entries = variants.getSelectedRecipes(RecipeCollection.CraftableStatus.ANY);
		for (int i = 0; i < entries.size() && i < PER_PAGE; i++) {
			RecipeDisplayEntry entry = entries.get(i);
			int x = this.getX() + i % COLUMNS * CELL;
			int y = this.getY() + i / COLUMNS * CELL;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, variants.isCraftable(entry.id()) ? SLOT_CRAFTABLE_SPRITE : SLOT_UNCRAFTABLE_SPRITE, x + 1, y + 1, BOX, BOX);
			ItemStack result = this.page.getResult(entry);
			graphics.fakeItem(result, x + 4, y + 4);
			graphics.fill(x + 4, y + 4, x + 20, y + 20, RecipesPage.SEPIA_WASH);
			if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
				graphics.setTooltipForNextFrame(Minecraft.getInstance().font, result, mouseX, mouseY);
			}
		}
	}

	private static Identifier getSprite(final RecipeCollection collection) {
		boolean many = collection.getRecipes().size() > 1;
		if (collection.hasCraftable()) {
			return many ? SLOT_MANY_CRAFTABLE_SPRITE : SLOT_CRAFTABLE_SPRITE;
		}

		return many ? SLOT_MANY_UNCRAFTABLE_SPRITE : SLOT_UNCRAFTABLE_SPRITE;
	}

	@Override
	public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
		int column = (int)(event.x() - this.getX()) / CELL;
		int row = (int)(event.y() - this.getY()) / CELL;
		int index = row * COLUMNS + column;
		RecipeCollection variants = this.page.getVariants();
		if (variants != null) {
			List<RecipeDisplayEntry> entries = variants.getSelectedRecipes(RecipeCollection.CraftableStatus.ANY);
			if (column < COLUMNS && index < entries.size()) {
				this.page.placeVariant(entries.get(index), event.hasShiftDown());
			} else {
				this.page.closeVariants();
			}

			return;
		}

		List<RecipeCollection> collections = this.page.getVisibleCollections();
		if (column < COLUMNS && index < collections.size()) {
			this.page.select(collections.get(index), event.hasShiftDown());
		}
	}

	@Override
	protected void updateWidgetNarration(final NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
