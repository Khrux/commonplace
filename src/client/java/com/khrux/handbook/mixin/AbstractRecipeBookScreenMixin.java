package com.khrux.handbook.mixin;

import com.khrux.handbook.client.gui.components.StationBookButton;
import com.khrux.handbook.client.gui.screens.HandbookScreen;
import com.khrux.handbook.client.gui.screens.RecipesPage;
import com.khrux.handbook.client.gui.screens.StationRecipes;
import com.khrux.handbook.world.inventory.HandbookSlot;
import com.khrux.handbook.world.inventory.ToolSlot;
import com.khrux.handbook.world.item.HandbookItem;
import com.khrux.handbook.world.item.HandbookItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin<T extends RecipeBookMenu> extends AbstractContainerScreen<T> {
	@Unique
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	@Unique
	private static final int GHOST_COLOR = 0xA08B8B8B;
	@Unique
	private static final int PANEL_WIDTH = 148;
	@Unique
	private static final int PANEL_HEIGHT = 180;
	@Unique
	private static final int PAGE_WIDTH = 145;
	@Unique
	private static final int COVER_EDGE = 3;
	@Unique
	private static final int TAB_REACH = 20;
	@Shadow
	@Final
	private RecipeBookComponent<?> recipeBookComponent;
	@Unique
	private @Nullable RecipesPage stationRecipes;
	@Unique
	private int panelLeft;
	@Unique
	private int panelTop;

	private AbstractRecipeBookScreenMixin(final T menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title);
	}

	@Shadow
	protected abstract ScreenPosition getRecipeBookButtonPosition();

	@Inject(method = "initButton", at = @At("HEAD"), cancellable = true)
	private void replaceRecipeButtonWithHandbookSlot(final CallbackInfo ci) {
		if (!((Object) this instanceof InventoryScreen)) {
			this.replaceRecipeBookWithHandbook();
			ci.cancel();
			return;
		}

		if (this.recipeBookComponent.isVisible()) {
			this.recipeBookComponent.toggleVisibility();
			this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
		}

		int x = this.leftPos + HandbookSlot.X - 1;
		int y = this.topPos + HandbookSlot.Y - 1;
		ToolSlot.setRevealed(false);
		this.addRenderableOnly((graphics, mouseX, mouseY, a) -> {
			boolean overBook = mouseX >= x && mouseY >= y && mouseX < x + 18 && mouseY < y + 18;
			boolean overTools = mouseX >= x && mouseY >= y && mouseX < this.leftPos + ToolSlot.COMPASS_X + 17 && mouseY < y + 18;
			ToolSlot.setRevealed(overBook || overTools && this.revealedTools());
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);
		});
		for (Slot slot : this.menu.slots) {
			if (slot instanceof ToolSlot toolSlot) {
				this.addToolSlotFrame(toolSlot);
			}
		}

		ci.cancel();
	}

	@Unique
	private void replaceRecipeBookWithHandbook() {
		if (this.recipeBookComponent.isVisible()) {
			this.recipeBookComponent.toggleVisibility();
		}

		RecipeBookType type = this.menu.getRecipeBookType();
		boolean open = StationRecipes.isOpen(type);
		this.leftPos = open ? 177 + (this.width - this.imageWidth - 200) / 2 : (this.width - this.imageWidth) / 2;
		this.stationRecipes = null;
		ScreenPosition button = this.getRecipeBookButtonPosition();
		this.addRenderableWidget(new StationBookButton(button.x(), button.y(), () -> {
			StationRecipes.toggle(type);
			this.rebuildWidgets();
		}));
		if (!open) {
			return;
		}

		this.panelLeft = Math.max(this.leftPos - PANEL_WIDTH - 2, TAB_REACH);
		this.panelTop = this.topPos + (this.imageHeight - PANEL_HEIGHT) / 2;
		int canvasLeft = this.panelLeft - HandbookScreen.COVER_LEFT;
		int canvasTop = this.panelTop - HandbookScreen.COVER_TOP;
		this.addRenderableOnly((graphics, mouseX, mouseY, a) -> this.extractPanel(graphics));
		RecipesPage page = new RecipesPage(this::rebuildWidgets, canvasLeft, canvasTop, new StationRecipes(this.recipeBookComponent, this.menu));
		page.init(this::addRenderableWidget);
		this.addRenderableOnly((graphics, mouseX, mouseY, a) -> page.extractRenderState(graphics, mouseX, mouseY));
		this.stationRecipes = page;
	}

	@Unique
	private void extractPanel(final GuiGraphicsExtractor graphics) {
		int color = HandbookItem.getColor(this.minecraft.player);
		int u = HandbookScreen.COVER_LEFT;
		int v = HandbookScreen.COVER_TOP;
		int edgeU = HandbookScreen.COVER_LEFT + HandbookScreen.BOOK_WIDTH - COVER_EDGE;
		graphics.blit(RenderPipelines.GUI_TEXTURED, HandbookScreen.BOOK_LOCATION, this.panelLeft, this.panelTop, u, v, PAGE_WIDTH, PANEL_HEIGHT, HandbookScreen.WIDTH, HandbookScreen.HEIGHT);
		graphics.blit(RenderPipelines.GUI_TEXTURED, HandbookScreen.COVER_LOCATION, this.panelLeft, this.panelTop, u, v, PAGE_WIDTH, PANEL_HEIGHT, HandbookScreen.WIDTH, HandbookScreen.HEIGHT, color);
		graphics.blit(RenderPipelines.GUI_TEXTURED, HandbookScreen.BOOK_LOCATION, this.panelLeft + PAGE_WIDTH, this.panelTop, edgeU, v, COVER_EDGE, PANEL_HEIGHT, HandbookScreen.WIDTH, HandbookScreen.HEIGHT);
		graphics.blit(
			RenderPipelines.GUI_TEXTURED, HandbookScreen.COVER_LOCATION, this.panelLeft + PAGE_WIDTH, this.panelTop, edgeU, v, COVER_EDGE, PANEL_HEIGHT, HandbookScreen.WIDTH, HandbookScreen.HEIGHT, color
		);
	}

	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void keepClicksOnHandbook(final double mx, final double my, final int xo, final int yo, final CallbackInfoReturnable<Boolean> cir) {
		if (
			this.stationRecipes != null
				&& mx >= this.panelLeft - TAB_REACH
				&& mx < this.panelLeft + PANEL_WIDTH
				&& my >= this.panelTop
				&& my < this.panelTop + PANEL_HEIGHT
		) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void typeInHandbookSearch(final KeyEvent event, final CallbackInfoReturnable<Boolean> cir) {
		if (this.stationRecipes != null && this.stationRecipes.isSearching()) {
			cir.setReturnValue(this.stationRecipes.searchKeyPressed(event));
		}
	}

	@Inject(method = "containerTick", at = @At("TAIL"))
	private void refreshHandbookRecipes(final CallbackInfo ci) {
		if (this.stationRecipes != null) {
			this.stationRecipes.tick();
		}
	}

	@Inject(method = "recipesUpdated", at = @At("TAIL"))
	private void updateHandbookRecipes(final CallbackInfo ci) {
		if (this.stationRecipes != null) {
			this.stationRecipes.recipesUpdated();
		}
	}

	@Unique
	private boolean revealedTools() {
		for (Slot slot : this.menu.slots) {
			if (slot instanceof ToolSlot) {
				return slot.isActive();
			}
		}

		return false;
	}

	@Unique
	private void addToolSlotFrame(final ToolSlot slot) {
		int x = this.leftPos + slot.x - 1;
		int y = this.topPos + slot.y - 1;
		ItemStack ghost = new ItemStack(slot.getTool());
		this.addRenderableOnly((graphics, mouseX, mouseY, a) -> {
			if (!slot.isActive()) {
				return;
			}

			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);
			if (!slot.hasItem()) {
				graphics.item(ghost, x + 1, y + 1);
				graphics.fill(x + 1, y + 1, x + 17, y + 17, GHOST_COLOR);
			}
		});
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void openHandbook(final MouseButtonEvent event, final boolean doubleClick, final CallbackInfoReturnable<Boolean> cir) {
		if (
			(Object) this instanceof InventoryScreen
				&& event.button() == InputConstants.MOUSE_BUTTON_RIGHT
				&& this.hoveredSlot != null
				&& this.hoveredSlot.getItem().is(HandbookItems.HANDBOOK)
				&& this.menu.getCarried().isEmpty()
		) {
			HandbookScreen.open(this);
			cir.setReturnValue(true);
		}
	}
}
