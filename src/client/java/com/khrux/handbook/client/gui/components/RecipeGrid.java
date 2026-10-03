package com.khrux.handbook.client.gui.components;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.ClientHandbook;
import com.khrux.handbook.client.ClientSheen;
import com.khrux.handbook.client.gui.screens.RecipesPage;
import com.khrux.handbook.client.renderer.InkMasks;
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
import net.minecraft.util.ARGB;
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
	private static final int WASH_ALPHA = 22;
	private static final float INK_DEPTH = 0.2F;
	private static final float INK_BELOW = 0.65F;
	private static final int DIMMED_COLOR = 0xB0F4EAD2;
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
			extractSheen(graphics, getSprite(collection), x, y, getSheen(collection.getRecipes()));

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
			extractSheen(graphics, variants.isCraftable(entry.id()) ? SLOT_CRAFTABLE_SPRITE : SLOT_UNCRAFTABLE_SPRITE, x, y, getSheen(List.of(entry)));
			if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
				graphics.setTooltipForNextFrame(Minecraft.getInstance().font, result, mouseX, mouseY);
			}
		}
	}

	private static int getSheen(final List<RecipeDisplayEntry> entries) {
		int result = ClientSheen.NONE;
		for (RecipeDisplayEntry entry : entries) {
			int sheen = ClientHandbook.get().sheen().getRecipeSheen(entry.id().index());
			if (sheen != ClientSheen.NONE && sheen != ClientSheen.DIMMED) {
				return sheen;
			}

			if (sheen == ClientSheen.DIMMED) {
				result = sheen;
			}
		}

		return result;
	}

	private static void extractSheen(final GuiGraphicsExtractor graphics, final Identifier sprite, final int x, final int y, final int sheen) {
		if (sheen == ClientSheen.DIMMED) {
			graphics.fill(x + 1, y + 1, x + 1 + BOX, y + 1 + BOX, DIMMED_COLOR);
			return;
		}

		InkMasks.Mask mask = sheen == ClientSheen.NONE ? null : InkMasks.get(sprite.withPath(path -> "textures/gui/sprites/" + path + ".png"), INK_BELOW);
		if (mask == null) {
			return;
		}

		graphics.fill(x + 4, y + 4, x + 20, y + 20, ARGB.color(WASH_ALPHA, sheen));
		int ink = ARGB.srgbLerp(INK_DEPTH, sheen, 0xFF000000);
		graphics.blit(RenderPipelines.GUI_TEXTURED, mask.location(), x + 1, y + 1, 0.0F, 0.0F, BOX, BOX, mask.width(), mask.height(), mask.width(), mask.height(), ink);
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
