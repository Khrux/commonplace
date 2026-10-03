package com.khrux.commonplace.client.gui.components;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

public class DyeSwatches extends AbstractWidget {
	private static final int COLUMNS = 8;
	private static final int SWATCH = 13;
	private static final int RIM_COLOR = 0xC0202C2A;
	private final IntSupplier selected;
	private final IntConsumer onSelect;

	public DyeSwatches(final int x, final int y, final IntSupplier selected, final IntConsumer onSelect) {
		super(x, y, COLUMNS * SWATCH, DyeColor.VALUES.size() / COLUMNS * SWATCH, Component.translatable("commonplace.ender.color"));
		this.selected = selected;
		this.onSelect = onSelect;
	}

	@Override
	protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		for (DyeColor color : DyeColor.VALUES) {
			int x = this.getX() + color.getId() % COLUMNS * SWATCH + SWATCH / 2;
			int y = this.getY() + color.getId() / COLUMNS * SWATCH + SWATCH / 2;
			EnderInk.blot(graphics, x, y, 4.5F, RIM_COLOR);
			EnderInk.blot(graphics, x, y, 3.5F, color.getTextureDiffuseColor());
			if (color.getId() == this.selected.getAsInt()) {
				EnderInk.ring(graphics, x, y, 6.0F, EnderInk.color(x, y));
			}
		}
	}

	@Override
	public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
		int column = (int)(event.x() - this.getX()) / SWATCH;
		int row = (int)(event.y() - this.getY()) / SWATCH;
		int id = row * COLUMNS + column;
		if (column >= 0 && column < COLUMNS && id >= 0 && id < DyeColor.VALUES.size()) {
			this.onSelect.accept(id);
		}
	}

	@Override
	protected void updateWidgetNarration(final NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
