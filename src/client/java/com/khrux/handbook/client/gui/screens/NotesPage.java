package com.khrux.handbook.client.gui.screens;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.HandbookConfig;
import com.khrux.handbook.client.ClientNotebook;
import com.khrux.handbook.client.ClientPassphrases;
import com.khrux.handbook.client.gui.components.HandbookTabButton;
import com.khrux.handbook.client.gui.components.InkPalette;
import com.khrux.handbook.client.gui.components.NoteCanvas;
import com.khrux.handbook.network.protocol.NoteEditPayload;
import com.khrux.handbook.world.entity.player.NotePage;
import com.khrux.handbook.world.entity.player.PassphraseSlot;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.util.Mth;

public class NotesPage {
	private static final int CANVAS_SCALE = 2;
	private static final int BLACK_INK = 15;
	private static final Identifier PASSPHRASE_ICON = Handbook.id("textures/gui/icon/passphrase.png");
	private static int book = NoteEditPayload.PERSONAL;
	private static int spread;
	private static NotesPage.Tool tool = NotesPage.Tool.PEN;
	private static int color = 15;
	private final HandbookScreen screen;
	private final Font font = Minecraft.getInstance().font;
	private final int left;
	private final int top;
	private final List<NotesPage.TextBox> textBoxes = new ArrayList<>();
	private boolean syncingText;

	public NotesPage(final HandbookScreen screen, final int left, final int top) {
		this.screen = screen;
		this.left = left;
		this.top = top;
	}

	public static int getBook() {
		return isAvailable(book) ? book : NoteEditPayload.PERSONAL;
	}

	public static int getSpread() {
		return Math.min(spread, ClientNotebook.getPageCount(getBook()) / 2 - 1);
	}

	public static NotesPage.Tool getTool() {
		return isTextOnly() ? NotesPage.Tool.TEXT : tool;
	}

	public static int getColor() {
		return color;
	}

	public static int getInk() {
		return getBook() != NoteEditPayload.PERSONAL && ClientNotebook.getOptions().colors() == HandbookConfig.NoteColors.BLACK ? BLACK_INK : color;
	}

	public static void setColor(final int newColor) {
		color = newColor;
		if (tool == NotesPage.Tool.ERASER) {
			tool = NotesPage.Tool.PEN;
		}
	}

	private static boolean isTextOnly() {
		return getBook() != NoteEditPayload.PERSONAL && ClientNotebook.getOptions().colors() == HandbookConfig.NoteColors.TEXT;
	}

	private static boolean isAvailable(final int candidate) {
		return candidate == NoteEditPayload.PERSONAL || HandbookTab.ENDER.isAvailable() && !ClientPassphrases.get(candidate).slot().isEmpty();
	}

	public void init(final Consumer<AbstractWidget> widgets) {
		this.textBoxes.clear();
		int current = getBook();
		int tabY = this.top + 20;
		widgets.accept(
			HandbookTabButton.side(this.left + HandbookScreen.COVER_LEFT, tabY, current == NoteEditPayload.PERSONAL, HandbookTab.NOTES.getIconTexture(), Component.translatable("handbook.notes.personal"), () -> this.selectBook(NoteEditPayload.PERSONAL))
		);
		for (int i = 0; i < PassphraseSlot.SLOTS; i++) {
			if (!isAvailable(i)) {
				continue;
			}

			int index = i;
			PassphraseSlot slot = ClientPassphrases.get(i).slot();
			tabY += 25;
			widgets.accept(HandbookTabButton.passphrase(
				this.left + HandbookScreen.COVER_LEFT, tabY, current == i, false, PASSPHRASE_ICON, EnderPage.getSlotColor(slot), EnderPage.getPullOut(slot), () -> this.selectBook(index)
			));
		}

		int shownSpread = getSpread();
		for (int side = 0; side < 2; side++) {
			int page = shownSpread * 2 + side;
			int x = this.left + (side == 0 ? 16 : 156);
			int y = this.top + 19;
			NoteCanvas canvas = new NoteCanvas(x, y, CANVAS_SCALE, current, page);
			canvas.active = getTool() != NotesPage.Tool.TEXT;
			widgets.accept(canvas);
			if (getTool() == NotesPage.Tool.TEXT) {
				widgets.accept(this.createTextBox(x, y, current, page));
			}
		}

		boolean blackOnly = current != NoteEditPayload.PERSONAL && ClientNotebook.getOptions().colors() == HandbookConfig.NoteColors.BLACK;
		if (!isTextOnly() && !blackOnly) {
			widgets.accept(new InkPalette(this.left + 16, this.top + 169, this.screen));
		}

		if (!isTextOnly()) {
			for (NotesPage.Tool entry : NotesPage.Tool.values()) {
				ImageButton button = new ImageButton(this.left + 158 + entry.ordinal() * 16, this.top + 168, 12, 12, new WidgetSprites(entry.sprite), pressed -> {
					tool = entry;
					this.screen.rebuild();
				});
				button.setTooltip(Tooltip.create(entry.getName()));
				widgets.accept(button);
			}
		}

		PageButton backButton = new PageButton(this.left + 212, this.top + 168, false, button -> this.turn(-1), true);
		backButton.visible = shownSpread > 0;
		widgets.accept(backButton);
		PageButton forwardButton = new PageButton(this.left + 262, this.top + 168, true, button -> this.turn(1), true);
		forwardButton.visible = shownSpread < ClientNotebook.getPageCount(current) / 2 - 1;
		widgets.accept(forwardButton);
	}

	private void selectBook(final int selected) {
		ClientNotebook.flush();
		book = selected;
		this.screen.rebuild();
	}

	private MultiLineEditBox createTextBox(final int x, final int y, final int bookIndex, final int page) {
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
		box.setValue(ClientNotebook.get(bookIndex, page).text());
		box.setValueListener(value -> {
			if (!this.syncingText) {
				ClientNotebook.setText(bookIndex, page, value);
			}
		});
		this.textBoxes.add(new NotesPage.TextBox(box, bookIndex, page));
		return box;
	}

	public void tick() {
		if (getBook() != book) {
			book = NoteEditPayload.PERSONAL;
			this.screen.rebuild();
			return;
		}

		for (NotesPage.TextBox textBox : this.textBoxes) {
			String stored = ClientNotebook.get(textBox.book(), textBox.page()).text();
			if (!textBox.box().isFocused() && !textBox.box().getValue().equals(stored)) {
				this.syncingText = true;
				textBox.box().setValue(stored);
				this.syncingText = false;
			}
		}
	}

	private void turn(final int direction) {
		ClientNotebook.flush();
		spread = Mth.clamp(getSpread() + direction, 0, ClientNotebook.getPageCount(getBook()) / 2 - 1);
		this.screen.rebuild();
	}

	public void extractRenderState(final GuiGraphicsExtractor graphics) {
		if (!isTextOnly()) {
			int x = this.left + 158 + getTool().ordinal() * 16;
			graphics.outline(x - 1, this.top + 167, 14, 14, RecipesPage.INK_COLOR);
		}

		HandbookScreen.centeredInk(graphics, this.font, Component.literal(String.valueOf(getSpread() + 1)), this.left + 249, this.top + 171);
	}

	private record TextBox(MultiLineEditBox box, int book, int page) {
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
