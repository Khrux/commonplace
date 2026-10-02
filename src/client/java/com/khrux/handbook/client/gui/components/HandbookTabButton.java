package com.khrux.handbook.client.gui.components;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.gui.screens.HandbookScreen;
import com.khrux.handbook.client.gui.screens.HandbookTab;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class HandbookTabButton extends AbstractButton {
	private static final Identifier TOP_SPRITE = Handbook.id("tab_top");
	private static final Identifier TOP_SELECTED_SPRITE = Handbook.id("tab_top_selected");
	private static final Identifier SIDE_SPRITE = Handbook.id("tab_side");
	private static final Identifier SIDE_SELECTED_SPRITE = Handbook.id("tab_side_selected");
	private final Identifier sprite;
	private final ItemStack icon;
	private @Nullable Identifier iconTexture;
	private final int iconX;
	private final int iconY;
	private final Runnable onPress;

	private HandbookTabButton(
		final int x,
		final int y,
		final int width,
		final int height,
		final Identifier sprite,
		final ItemStack icon,
		final int iconX,
		final int iconY,
		final Component name,
		final Runnable onPress
	) {
		super(x, y, width, height, name);
		this.sprite = sprite;
		this.icon = icon;
		this.iconX = iconX;
		this.iconY = iconY;
		this.onPress = onPress;
		this.setTooltip(Tooltip.create(name));
	}

	public static void addMainTabs(final int coverLeft, final int coverTop, final HandbookTab selected, final Consumer<AbstractWidget> widgets) {
		int x = coverLeft + 10;
		for (HandbookTab tab : HandbookTab.values()) {
			if (!tab.isAvailable()) {
				continue;
			}

			widgets.accept(top(x, coverTop, tab == selected, tab.getIconTexture(), tab.getName(), () -> HandbookScreen.select(tab)));
			x += 26;
		}
	}

	public static HandbookTabButton top(final int x, final int bookTop, final boolean selected, final Identifier icon, final Component name, final Runnable onPress) {
		HandbookTabButton button = selected
			? new HandbookTabButton(x, bookTop - 20, 24, 27, TOP_SELECTED_SPRITE, ItemStack.EMPTY, 4, 3, name, onPress)
			: new HandbookTabButton(x, bookTop - 18, 24, 24, TOP_SPRITE, ItemStack.EMPTY, 4, 2, name, onPress);
		button.iconTexture = icon;
		return button;
	}

	public static HandbookTabButton side(final int bookLeft, final int y, final boolean selected, final Identifier icon, final Component name, final Runnable onPress) {
		HandbookTabButton button = side(bookLeft, y, selected, ItemStack.EMPTY, name, onPress);
		button.iconTexture = icon;
		return button;
	}

	public static HandbookTabButton side(final int bookLeft, final int y, final boolean selected, final ItemStack icon, final Component name, final Runnable onPress) {
		if (selected) {
			return new HandbookTabButton(bookLeft - 20, y, 28, 24, SIDE_SELECTED_SPRITE, icon, 3, 4, name, onPress);
		}

		return new HandbookTabButton(bookLeft - 18, y, 25, 24, SIDE_SPRITE, icon, 2, 4, name, onPress);
	}

	@Override
	public void onPress(final InputWithModifiers input) {
		this.onPress.run();
	}

	@Override
	protected void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX(), this.getY(), this.width, this.height);
		if (this.iconTexture != null) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.iconTexture, this.getX() + this.iconX, this.getY() + this.iconY, 0.0F, 0.0F, 16, 16, 16, 16);
		} else {
			graphics.item(this.icon, this.getX() + this.iconX, this.getY() + this.iconY);
		}
	}

	@Override
	protected void updateWidgetNarration(final NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
