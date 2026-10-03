package com.khrux.commonplace.client.gui.components;

import com.khrux.commonplace.client.gui.screens.HandbookScreen;
import com.khrux.commonplace.client.gui.screens.NotesPage;
import com.khrux.commonplace.client.gui.screens.RecipesPage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class InkPalette extends AbstractWidget {
	private static final int COLORS = 15;
	private static final int SWATCH = 8;
	private final HandbookScreen screen;

	public InkPalette(final int x, final int y, final HandbookScreen screen) {
		super(x, y, COLORS * SWATCH, SWATCH, Component.translatable("commonplace.notes.ink"));
		this.screen = screen;
	}

	@Override
	protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		for (int i = 0; i < COLORS; i++) {
			int x = this.getX() + i * SWATCH;
			graphics.fill(x + 1, this.getY() + 1, x + SWATCH - 1, this.getY() + SWATCH - 1, NoteCanvas.getInkColor(i + 1));
			if (i + 1 == NotesPage.getColor() && NotesPage.getTool() == NotesPage.Tool.PEN) {
				graphics.outline(x, this.getY(), SWATCH, SWATCH, RecipesPage.INK_COLOR);
			}
		}
	}

	@Override
	public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
		int index = (int)(event.x() - this.getX()) / SWATCH;
		if (index >= 0 && index < COLORS) {
			NotesPage.setColor(index + 1);
			this.screen.rebuild();
		}
	}

	@Override
	protected void updateWidgetNarration(final NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
