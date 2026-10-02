package com.khrux.handbook.client.gui.screens;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.ClientNotebook;
import com.khrux.handbook.client.gui.components.HandbookTabButton;
import com.khrux.handbook.client.gui.components.InkPalette;
import com.khrux.handbook.client.gui.components.NoteCanvas;
import com.khrux.handbook.world.entity.player.NotePage;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class NotesPage {
	private static final int CANVAS_SCALE = 2;
	private static int spread;
	private static NotesPage.Tool tool = NotesPage.Tool.PEN;
	private static int color = 15;
	private final HandbookScreen screen;
	private final Font font = Minecraft.getInstance().font;
	private final int left;
	private final int top;

	public NotesPage(final HandbookScreen screen, final int left, final int top) {
		this.screen = screen;
		this.left = left;
		this.top = top;
	}

	public static int getSpread() {
		return spread;
	}

	public static NotesPage.Tool getTool() {
		return tool;
	}

	public static int getColor() {
		return color;
	}

	public static void setColor(final int newColor) {
		color = newColor;
		if (tool == NotesPage.Tool.ERASER) {
			tool = NotesPage.Tool.PEN;
		}
	}

	public void init(final Consumer<AbstractWidget> widgets) {
		widgets.accept(
			HandbookTabButton.side(this.left + HandbookScreen.COVER_LEFT, this.top + 20, true, HandbookTab.NOTES.getIconTexture(), Component.translatable("handbook.notes.personal"), () -> {})
		);
		for (int side = 0; side < 2; side++) {
			int page = spread * 2 + side;
			int x = this.left + (side == 0 ? 16 : 156);
			int y = this.top + 19;
			NoteCanvas canvas = new NoteCanvas(x, y, CANVAS_SCALE, page);
			canvas.active = tool != NotesPage.Tool.TEXT;
			widgets.accept(canvas);
			if (tool == NotesPage.Tool.TEXT) {
				widgets.accept(this.createTextBox(x, y, page));
			}
		}

		widgets.accept(new InkPalette(this.left + 16, this.top + 169, this.screen));
		for (NotesPage.Tool entry : NotesPage.Tool.values()) {
			ImageButton button = new ImageButton(this.left + 158 + entry.ordinal() * 16, this.top + 168, 12, 12, new WidgetSprites(entry.sprite), pressed -> {
				ClientNotebook.send();
				tool = entry;
				this.screen.rebuild();
			});
			button.setTooltip(Tooltip.create(entry.getName()));
			widgets.accept(button);
		}

		widgets.accept(new PageButton(this.left + 212, this.top + 168, false, button -> this.turn(-1), true));
		widgets.accept(new PageButton(this.left + 262, this.top + 168, true, button -> this.turn(1), true));
	}

	private MultiLineEditBox createTextBox(final int x, final int y, final int page) {
		MultiLineEditBox box = MultiLineEditBox.builder()
			.setX(x)
			.setY(y)
			.setShowBackground(false)
			.setShowDecorations(false)
			.setTextShadow(false)
			.setTextColor(RecipesPage.INK_COLOR)
			.setCursorColor(RecipesPage.INK_COLOR)
			.build(this.font, NotePage.CANVAS_WIDTH * CANVAS_SCALE, NotePage.CANVAS_HEIGHT * CANVAS_SCALE, Component.translatable("handbook.tab.notes"));
		box.setCharacterLimit(NotePage.MAX_TEXT_LENGTH);
		box.setLineLimit(NotePage.CANVAS_HEIGHT * CANVAS_SCALE / this.font.lineHeight - 1);
		box.setValue(ClientNotebook.get(page).text());
		box.setValueListener(value -> ClientNotebook.put(page, ClientNotebook.get(page).withText(value)));
		return box;
	}

	private void turn(final int direction) {
		ClientNotebook.send();
		spread = Math.floorMod(spread + direction, NotePage.PAGES / 2);
		this.screen.rebuild();
	}

	public void extractRenderState(final GuiGraphicsExtractor graphics) {
		int x = this.left + 158 + tool.ordinal() * 16;
		graphics.outline(x - 1, this.top + 167, 14, 14, RecipesPage.INK_COLOR);
		HandbookScreen.centeredInk(graphics, this.font, Component.literal(String.valueOf(spread + 1)), this.left + 249, this.top + 171);
	}

	public enum Tool {
		PEN("pen"),
		ERASER("eraser"),
		TEXT("text");

		private final Identifier sprite;
		private final Component name;

		Tool(final String name) {
			this.sprite = Handbook.id(name);
			this.name = Component.translatable("handbook.notes." + name);
		}

		public Component getName() {
			return this.name;
		}
	}
}
