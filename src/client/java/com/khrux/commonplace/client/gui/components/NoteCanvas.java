package com.khrux.commonplace.client.gui.components;

import com.khrux.commonplace.client.ClientHandbook;
import com.khrux.commonplace.client.ClientNotebook;
import com.khrux.commonplace.client.gui.screens.NotesPage;
import com.khrux.commonplace.client.gui.screens.RecipesPage;
import com.khrux.commonplace.world.entity.player.NotePage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.DyeColor;

public class NoteCanvas extends AbstractWidget {
	private static final int TEXT_PADDING = 4;
	private final int scale;
	private final int book;
	private final int page;
	private int lastX = -1;
	private int lastY = -1;

	public NoteCanvas(final int x, final int y, final int scale, final int book, final int page) {
		super(x, y, NotePage.CANVAS_WIDTH * scale, NotePage.CANVAS_HEIGHT * scale, Component.translatable("commonplace.tab.notes"));
		this.scale = scale;
		this.book = book;
		this.page = page;
	}

	public static int getInkColor(final int color) {
		return 0xFF000000 | DyeColor.byId(color).getTextColor();
	}

	@Override
	protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		NotePage notePage = ClientHandbook.get().notebook().get(this.book, this.page);
		for (int y = 0; y < NotePage.CANVAS_HEIGHT; y++) {
			int start = 0;
			while (start < NotePage.CANVAS_WIDTH) {
				int color = notePage.getPixel(start, y);
				int end = start + 1;
				while (end < NotePage.CANVAS_WIDTH && notePage.getPixel(end, y) == color) {
					end++;
				}

				if (color != 0) {
					int x0 = this.getX() + start * this.scale;
					int y0 = this.getY() + y * this.scale;
					graphics.fill(x0, y0, this.getX() + end * this.scale, y0 + this.scale, getInkColor(color));
				}

				start = end;
			}
		}

		if (NotesPage.getTool() != NotesPage.Tool.TEXT) {
			graphics.textWithWordWrap(
				Minecraft.getInstance().font,
				FormattedText.of(notePage.text()),
				this.getX() + TEXT_PADDING,
				this.getY() + TEXT_PADDING,
				this.width - TEXT_PADDING * 2,
				RecipesPage.INK_COLOR,
				false
			);
		}
	}

	@Override
	public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
		this.lastX = -1;
		this.paint(event.x(), event.y());
	}

	@Override
	protected void onDrag(final MouseButtonEvent event, final double dx, final double dy) {
		this.paint(event.x(), event.y());
	}

	@Override
	public void onRelease(final MouseButtonEvent event) {
		this.lastX = -1;
	}

	private void paint(final double mouseX, final double mouseY) {
		int x = (int)Math.floor((mouseX - this.getX()) / this.scale);
		int y = (int)Math.floor((mouseY - this.getY()) / this.scale);
		if (this.lastX < 0) {
			this.dab(x, y);
		} else {
			int steps = Math.max(Math.abs(x - this.lastX), Math.abs(y - this.lastY));
			for (int i = 1; i <= steps; i++) {
				this.dab(this.lastX + (x - this.lastX) * i / steps, this.lastY + (y - this.lastY) * i / steps);
			}
		}

		this.lastX = x;
		this.lastY = y;
	}

	private void dab(final int x, final int y) {
		boolean erasing = NotesPage.getTool() == NotesPage.Tool.ERASER;
		int radius = erasing ? 1 : 0;
		int color = erasing ? 0 : NotesPage.getInk();
		for (int dy = -radius; dy <= radius; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				int px = x + dx;
				int py = y + dy;
				if (px >= 0 && px < NotePage.CANVAS_WIDTH && py >= 0 && py < NotePage.CANVAS_HEIGHT && ClientHandbook.get().notebook().get(this.book, this.page).getPixel(px, py) != color) {
					ClientHandbook.get().notebook().paint(this.book, this.page, px, py, color);
				}
			}
		}
	}

	@Override
	protected void updateWidgetNarration(final NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
