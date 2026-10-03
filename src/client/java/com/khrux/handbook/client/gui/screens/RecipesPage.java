package com.khrux.handbook.client.gui.screens;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.gui.components.HandbookTabButton;
import com.khrux.handbook.client.gui.components.RecipeGrid;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;
import org.jspecify.annotations.Nullable;

public class RecipesPage {
	public static final int INK_COLOR = 0xFF3F2A1C;
	public static final int SEPIA_WASH = 0x38EEE3C7;
	private static final int GHOST_COLOR = 0x70F4EAD2;
	private static final int HINT_COLOR = 0x903F2A1C;
	private static final Identifier SLOT_SPRITE = Handbook.id("slot");
	private static final Identifier RESULT_SLOT_SPRITE = Handbook.id("result_slot");
	private static final Identifier ARROW_SPRITE = Handbook.id("recipe_arrow");
	private static final Identifier FLAME_SPRITE = Handbook.id("flame");
	private static final Identifier SEARCH_SPRITE = Handbook.id("search");
	private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
		Handbook.id("filter_enabled"), Handbook.id("filter_disabled"), Handbook.id("filter_enabled_highlighted"), Handbook.id("filter_disabled_highlighted")
	);
	public static final WidgetSprites PAGE_FORWARD_SPRITES = new WidgetSprites(Handbook.id("page_forward"), Handbook.id("page_forward_highlighted"));
	public static final WidgetSprites PAGE_BACKWARD_SPRITES = new WidgetSprites(Handbook.id("page_backward"), Handbook.id("page_backward_highlighted"));
	private static final List<RecipesPage.Category> CATEGORIES = List.of(
		RecipesPage.Category.of("all", Items.COMPASS, List.of()),
		RecipesPage.Category.of("equipment", Items.IRON_AXE, List.of(RecipeBookCategories.CRAFTING_EQUIPMENT)),
		RecipesPage.Category.of("building_blocks", Items.BRICKS, List.of(RecipeBookCategories.CRAFTING_BUILDING_BLOCKS)),
		RecipesPage.Category.of("misc", Items.LAVA_BUCKET, List.of(RecipeBookCategories.CRAFTING_MISC)),
		RecipesPage.Category.of("redstone", Items.REDSTONE, List.of(RecipeBookCategories.CRAFTING_REDSTONE)),
		RecipesPage.Category.of(
			"other",
			Items.FURNACE,
			List.of(
				SearchRecipeBookCategory.FURNACE,
				SearchRecipeBookCategory.BLAST_FURNACE,
				RecipeBookCategories.SMOKER_FOOD,
				RecipeBookCategories.STONECUTTER,
				RecipeBookCategories.SMITHING,
				RecipeBookCategories.CAMPFIRE
			)
		)
	);
	private static final RecipesPage.State HANDBOOK = new RecipesPage.State();
	private static final RecipesPage.State STATION = new RecipesPage.State();
	private static @Nullable RecipeCollection selected;
	private static int selectedRecipe;
	private final Runnable rebuild;
	private final @Nullable StationRecipes station;
	private final RecipesPage.State state;
	private final List<RecipesPage.Category> categories;
	private int timesChanged;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final Font font = this.minecraft.font;
	private final int left;
	private final int top;
	private final ContextMap context;
	private final List<RecipeCollection> collections = new ArrayList<>();
	private @Nullable EditBox searchBox;

	public RecipesPage(final Runnable rebuild, final int left, final int top, final @Nullable StationRecipes station) {
		this.rebuild = rebuild;
		this.left = left;
		this.top = top;
		this.station = station;
		this.state = station == null ? HANDBOOK : STATION;
		this.categories = station == null ? CATEGORIES : station.getTabs().stream().map(RecipesPage.Category::of).toList();
		this.state.category = Math.min(this.state.category, this.categories.size() - 1);
		this.context = SlotDisplayContext.fromLevel(this.minecraft.level);
		this.timesChanged = this.minecraft.player.getInventory().getTimesChanged();
		this.filter();
	}

	public void init(final Consumer<AbstractWidget> widgets) {
		for (int i = 0; i < this.categories.size(); i++) {
			RecipesPage.Category entry = this.categories.get(i);
			int index = i;
			widgets.accept(HandbookTabButton.side(this.left + HandbookScreen.COVER_LEFT, this.top + 20 + i * 25, i == this.state.category, entry.icon(), entry.name(), () -> {
				this.state.category = index;
				this.state.page = 0;
				this.state.variants = null;
				this.rebuild.run();
			}));
		}

		this.searchBox = new EditBox(this.font, this.left + 29, this.top + 24, 82, 10, Component.translatable("itemGroup.search"));
		this.searchBox.setMaxLength(50);
		this.searchBox.setBordered(false);
		this.searchBox.setTextColor(INK_COLOR);
		this.searchBox.setTextShadow(false);
		this.searchBox.setValue(this.state.search);
		this.searchBox.setResponder(value -> {
			this.state.search = value;
			this.state.page = 0;
			this.state.variants = null;
			this.filter();
		});
		widgets.accept(this.searchBox);
		ImageButton filterButton = new ImageButton(this.left + 115, this.top + 20, 26, 16, this.state.filtering ? FILTER_SPRITES : this.swappedFilterSprites(), button -> {
			this.state.filtering = !this.state.filtering;
			this.state.page = 0;
			this.state.variants = null;
			this.rebuild.run();
		});
		filterButton.setTooltip(Tooltip.create(Component.translatable(this.state.filtering ? "gui.recipebook.toggleRecipes.craftable" : "gui.recipebook.toggleRecipes.all")));
		widgets.accept(filterButton);
		widgets.accept(new RecipeGrid(this.left + 17, this.top + 39, this));
		widgets.accept(new ImageButton(this.left + 50, this.top + 166, 12, 17, PAGE_BACKWARD_SPRITES, button -> this.turnPage(-1)));
		widgets.accept(new ImageButton(this.left + 97, this.top + 166, 12, 17, PAGE_FORWARD_SPRITES, button -> this.turnPage(1)));
		if (this.station == null && selected != null && selected.getRecipes().size() > 1) {
			widgets.accept(new ImageButton(this.left + 192, this.top + 166, 12, 17, PAGE_BACKWARD_SPRITES, button -> this.turnRecipe(-1)));
			widgets.accept(new ImageButton(this.left + 239, this.top + 166, 12, 17, PAGE_FORWARD_SPRITES, button -> this.turnRecipe(1)));
		}
	}

	private WidgetSprites swappedFilterSprites() {
		return new WidgetSprites(FILTER_SPRITES.disabled(), FILTER_SPRITES.enabled(), FILTER_SPRITES.disabledFocused(), FILTER_SPRITES.enabledFocused());
	}

	private void filter() {
		this.collections.clear();
		List<ExtendedRecipeBookCategory> categories = this.categories.get(this.state.category).categories();
		List<RecipeCollection> source = new ArrayList<>();
		if (categories.isEmpty()) {
			source.addAll(this.minecraft.player.getRecipeBook().getCollections());
		} else {
			for (ExtendedRecipeBookCategory entry : categories) {
				source.addAll(this.minecraft.player.getRecipeBook().getCollection(entry));
			}
		}

		StackedItemContents contents = this.station == null ? new StackedItemContents() : this.station.contents();
		if (this.station == null) {
			this.minecraft.player.getInventory().fillStackedContents(contents);
		}

		String query = this.state.search.toLowerCase(Locale.ROOT);
		for (RecipeCollection collection : source) {
			if (this.station == null) {
				collection.selectRecipes(contents, display -> true);
			} else {
				this.station.select(collection, contents);
				if (!collection.hasAnySelected()) {
					continue;
				}
			}

			if (this.state.filtering && !collection.hasCraftable()) {
				continue;
			}

			ItemStack result = this.getResult(collection);
			if (!result.isEmpty() && (query.isEmpty() || result.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))) {
				this.collections.add(collection);
			}
		}
	}

	public ItemStack getResult(final RecipeCollection collection) {
		for (RecipeDisplayEntry entry : collection.getRecipes()) {
			List<ItemStack> results = entry.resultItems(this.context);
			if (!results.isEmpty()) {
				return results.getFirst();
			}
		}

		return ItemStack.EMPTY;
	}

	public List<RecipeCollection> getVisibleCollections() {
		int start = this.state.page * RecipeGrid.PER_PAGE;
		return this.collections.subList(Math.min(start, this.collections.size()), Math.min(start + RecipeGrid.PER_PAGE, this.collections.size()));
	}

	public static @Nullable RecipeCollection getSelected() {
		return selected;
	}

	public boolean isSelected(final RecipeCollection collection) {
		return this.station == null && collection == selected;
	}

	public void select(final RecipeCollection collection, final boolean useMaxItems) {
		if (this.station == null) {
			selected = collection;
			selectedRecipe = 0;
			this.rebuild.run();
			return;
		}

		List<RecipeDisplayEntry> recipes = collection.getSelectedRecipes(RecipeCollection.CraftableStatus.ANY);
		if (recipes.size() == 1) {
			this.station.place(collection, recipes.getFirst().id(), useMaxItems);
		} else if (!recipes.isEmpty()) {
			this.state.variants = collection;
		}
	}

	public @Nullable RecipeCollection getVariants() {
		return this.state.variants;
	}

	public void placeVariant(final RecipeDisplayEntry entry, final boolean useMaxItems) {
		RecipeCollection variants = this.state.variants;
		this.state.variants = null;
		if (this.station != null && variants != null) {
			this.station.place(variants, entry.id(), useMaxItems);
		}
	}

	public void closeVariants() {
		this.state.variants = null;
	}

	public ItemStack getResult(final RecipeDisplayEntry entry) {
		List<ItemStack> results = entry.resultItems(this.context);
		return results.isEmpty() ? ItemStack.EMPTY : results.getFirst();
	}

	public boolean isSearching() {
		return this.searchBox != null && this.searchBox.isFocused();
	}

	public boolean searchKeyPressed(final KeyEvent event) {
		if (event.key() == InputConstants.KEY_ESCAPE) {
			this.searchBox.setFocused(false);
			return true;
		}

		this.searchBox.keyPressed(event);
		return true;
	}

	public void tick() {
		int changed = this.minecraft.player.getInventory().getTimesChanged();
		if (changed != this.timesChanged) {
			this.timesChanged = changed;
			this.filter();
		}
	}

	public void recipesUpdated() {
		this.filter();
	}

	private int getPageCount() {
		return Math.max(1, (this.collections.size() + RecipeGrid.PER_PAGE - 1) / RecipeGrid.PER_PAGE);
	}

	private void turnPage(final int direction) {
		this.state.page = Math.floorMod(this.state.page + direction, this.getPageCount());
		this.state.variants = null;
	}

	private void turnRecipe(final int direction) {
		if (selected != null) {
			selectedRecipe = Math.floorMod(selectedRecipe + direction, selected.getRecipes().size());
		}
	}

	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SEARCH_SPRITE, this.left + 17, this.top + 23, 9, 9);
		graphics.fill(this.left + 17, this.top + 34, this.left + 112, this.top + 35, 0x803F2A1C);
		if (this.searchBox != null && this.searchBox.getValue().isEmpty() && !this.searchBox.isFocused()) {
			graphics.text(this.font, Component.translatable("gui.recipebook.search_hint"), this.left + 29, this.top + 24, HINT_COLOR, false);
		}

		HandbookScreen.centeredInk(graphics, this.font, Component.literal(this.state.page + 1 + "/" + this.getPageCount()), this.left + 80, this.top + 170);
		if (this.station != null || selected == null || selected.getRecipes().isEmpty()) {
			return;
		}

		RecipeDisplayEntry entry = selected.getRecipes().get(Math.min(selectedRecipe, selected.getRecipes().size() - 1));
		int x = this.left + 157;
		int y = this.top + 22;
		for (FormattedCharSequence line : this.font.split(this.getResult(selected).getHoverName(), 124)) {
			HandbookScreen.centeredInk(graphics, this.font, line, x + 64, y);
			y += 10;
		}

		this.extractDisplay(graphics, entry.display(), x, this.top + 50, mouseX, mouseY);
		ItemStack station = entry.display().craftingStation().resolveForFirstStack(this.context);
		if (!station.isEmpty()) {
			graphics.item(station, x + 4, this.top + 124);
			graphics.text(this.font, station.getHoverName(), x + 24, this.top + 128, INK_COLOR, false);
		}

		if (selected.getRecipes().size() > 1) {
			HandbookScreen.centeredInk(graphics, this.font, Component.literal(selectedRecipe + 1 + "/" + selected.getRecipes().size()), this.left + 222, this.top + 170);
		}
	}

	private void extractDisplay(final GuiGraphicsExtractor graphics, final RecipeDisplay display, final int x, final int y, final int mouseX, final int mouseY) {
		switch (display) {
			case ShapedCraftingRecipeDisplay shaped -> {
				this.extractGrid(graphics, x + 10, y);
				for (int i = 0; i < shaped.ingredients().size(); i++) {
					this.extractGhost(graphics, shaped.ingredients().get(i), x + 11 + i % shaped.width() * 18, y + 1 + i / shaped.width() * 18, mouseX, mouseY);
				}

				this.extractResult(graphics, display.result(), x + 68, y + 19, x + 95, y + 14, mouseX, mouseY);
			}
			case ShapelessCraftingRecipeDisplay shapeless -> {
				this.extractGrid(graphics, x + 10, y);
				for (int i = 0; i < shapeless.ingredients().size(); i++) {
					this.extractGhost(graphics, shapeless.ingredients().get(i), x + 11 + i % 3 * 18, y + 1 + i / 3 * 18, mouseX, mouseY);
				}

				this.extractResult(graphics, display.result(), x + 68, y + 19, x + 95, y + 14, mouseX, mouseY);
			}
			case FurnaceRecipeDisplay furnace -> {
				this.extractSlot(graphics, furnace.ingredient(), x + 29, y, mouseX, mouseY);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FLAME_SPRITE, x + 33, y + 20, 10, 11);
				this.extractSlot(graphics, furnace.fuel(), x + 29, y + 34, mouseX, mouseY);
				this.extractResult(graphics, display.result(), x + 56, y + 18, x + 86, y + 13, mouseX, mouseY);
			}
			case SmithingRecipeDisplay smithing -> {
				this.extractSlot(graphics, smithing.template(), x + 4, y + 17, mouseX, mouseY);
				this.extractSlot(graphics, smithing.base(), x + 22, y + 17, mouseX, mouseY);
				this.extractSlot(graphics, smithing.addition(), x + 40, y + 17, mouseX, mouseY);
				this.extractResult(graphics, display.result(), x + 64, y + 19, x + 92, y + 13, mouseX, mouseY);
			}
			case StonecutterRecipeDisplay stonecutter -> {
				this.extractSlot(graphics, stonecutter.input(), x + 29, y + 17, mouseX, mouseY);
				this.extractResult(graphics, display.result(), x + 54, y + 19, x + 84, y + 13, mouseX, mouseY);
			}
			default -> this.extractResult(graphics, display.result(), x + 30, y + 19, x + 60, y + 13, mouseX, mouseY);
		}
	}

	private void extractGrid(final GuiGraphicsExtractor graphics, final int x, final int y) {
		for (int i = 0; i < 9; i++) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + i % 3 * 18, y + i / 3 * 18, 18, 18);
		}
	}

	private void extractSlot(final GuiGraphicsExtractor graphics, final SlotDisplay display, final int x, final int y, final int mouseX, final int mouseY) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);
		this.extractGhost(graphics, display, x + 1, y + 1, mouseX, mouseY);
	}

	private void extractGhost(final GuiGraphicsExtractor graphics, final SlotDisplay display, final int x, final int y, final int mouseX, final int mouseY) {
		ItemStack stack = this.cycle(display);
		if (stack.isEmpty()) {
			return;
		}

		graphics.fakeItem(stack, x, y);
		graphics.fill(x, y, x + 16, y + 16, GHOST_COLOR);
		this.extractTooltip(graphics, stack, x, y, mouseX, mouseY);
	}

	private void extractResult(
		final GuiGraphicsExtractor graphics, final SlotDisplay display, final int arrowX, final int arrowY, final int x, final int y, final int mouseX, final int mouseY
	) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_SPRITE, arrowX, arrowY, 22, 15);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, RESULT_SLOT_SPRITE, x, y, 26, 26);
		ItemStack stack = this.cycle(display);
		graphics.item(stack, x + 5, y + 5);
		graphics.fill(x + 5, y + 5, x + 21, y + 21, SEPIA_WASH);
		graphics.itemDecorations(this.font, stack, x + 5, y + 5);
		this.extractTooltip(graphics, stack, x + 5, y + 5, mouseX, mouseY);
	}

	private ItemStack cycle(final SlotDisplay display) {
		List<ItemStack> stacks = display.resolveForStacks(this.context);
		return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get((int)(Util.getMillis() / 1000L % stacks.size()));
	}

	private void extractTooltip(final GuiGraphicsExtractor graphics, final ItemStack stack, final int x, final int y, final int mouseX, final int mouseY) {
		if (!stack.isEmpty() && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
			graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
		}
	}

	private record Category(Component name, ItemStack icon, List<ExtendedRecipeBookCategory> categories) {
		private static RecipesPage.Category of(final String key, final Item icon, final List<ExtendedRecipeBookCategory> categories) {
			return new RecipesPage.Category(Component.translatable("handbook.recipes." + key), new ItemStack(icon), categories);
		}

		private static RecipesPage.Category of(final RecipeBookComponent.TabInfo tab) {
			Component name = tab.category() instanceof SearchRecipeBookCategory ? Component.translatable("handbook.recipes.all") : tab.primaryIcon().getHoverName();
			return new RecipesPage.Category(name, tab.primaryIcon(), List.of(tab.category()));
		}
	}

	private static class State {
		private int category;
		private int page;
		private boolean filtering;
		private String search = "";
		private @Nullable RecipeCollection variants;
	}
}
