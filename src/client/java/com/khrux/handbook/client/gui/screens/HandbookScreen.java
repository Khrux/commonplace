package com.khrux.handbook.client.gui.screens;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.ClientNotebook;
import com.khrux.handbook.client.HandbookKeyMappings;
import com.khrux.handbook.client.compat.FieldGuideTab;
import com.khrux.handbook.client.gui.components.AtlasMap;
import com.khrux.handbook.client.gui.components.HandbookTabButton;
import com.khrux.handbook.client.renderer.PageSnapshot;
import com.khrux.handbook.world.item.HandbookItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

public class HandbookScreen extends Screen {
	public static final int WIDTH = 300;
	public static final int HEIGHT = 200;
	public static final int COVER_LEFT = 3;
	public static final int COVER_TOP = 12;
	public static final int BOOK_WIDTH = 295;
	public static final Identifier BOOK_LOCATION = Handbook.id("textures/gui/book.png");
	public static final Identifier COVER_LOCATION = Handbook.id("textures/gui/book_cover.png");
	public static final int SEAM_X = 150;
	private static final int PAGE_EDGE_COLOR = 0xFFA07A48;
	private static final int SEAM_SHADOW = 6;
	private static HandbookTab lastTab = HandbookTab.RECIPES;
	private final @Nullable Screen parent;
	private final HandbookTab tab;
	private final ScrollWheelHandler scrollWheelHandler = new ScrollWheelHandler();
	private @Nullable RecipesPage recipes;
	private @Nullable NotesPage notes;
	private int left;
	private int top;

	private HandbookScreen(final @Nullable Screen parent, final HandbookTab tab) {
		super(Component.translatable("item.handbook.handbook"));
		this.parent = parent;
		this.tab = tab;
	}

	public static HandbookTab getLastTab() {
		return lastTab;
	}

	public static void setLastTab(final HandbookTab tab) {
		lastTab = tab;
	}

	public static void open(final @Nullable Screen parent) {
		select(lastTab, parent);
	}

	public static void select(final HandbookTab tab) {
		select(tab, null);
	}

	private static void select(final HandbookTab tab, final @Nullable Screen parent) {
		HandbookTab chosen = tab.isAvailable() ? tab : HandbookTab.RECIPES;
		lastTab = chosen;
		if (chosen == HandbookTab.FIELD_GUIDE) {
			FieldGuideTab.open();
			return;
		}

		Minecraft.getInstance().gui.setScreen(new HandbookScreen(parent, chosen));
	}

	@Override
	protected void init() {
		this.recipes = null;
		this.notes = null;
		this.left = (this.width - WIDTH) / 2;
		this.top = (this.height - HEIGHT) / 2;
		HandbookTabButton.addMainTabs(this.left + COVER_LEFT, this.top + COVER_TOP, this.tab, this::addRenderableWidget);
		if (this.tab == HandbookTab.ATLAS) {
			this.addRenderableWidget(new AtlasMap(this.left + 17, this.top + 19, 267, 166));
		} else if (this.tab == HandbookTab.RECIPES) {
			this.recipes = new RecipesPage(this::rebuild, this.left, this.top, null);
			this.recipes.init(this::addRenderableWidget);
		} else if (this.tab == HandbookTab.NOTES) {
			this.notes = new NotesPage(this, this.left, this.top);
			this.notes.init(this::addRenderableWidget);
		}
	}

	public static void centeredInk(final GuiGraphicsExtractor graphics, final Font font, final Component text, final int x, final int y) {
		graphics.text(font, text, x - font.width(text) / 2, y, RecipesPage.INK_COLOR, false);
	}

	public static void centeredInk(final GuiGraphicsExtractor graphics, final Font font, final FormattedCharSequence text, final int x, final int y) {
		graphics.text(font, text, x - font.width(text) / 2, y, RecipesPage.INK_COLOR, false);
	}

	public void rebuild() {
		this.rebuildWidgets();
	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_LOCATION, this.left, this.top, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
		graphics.blit(RenderPipelines.GUI_TEXTURED, COVER_LOCATION, this.left, this.top, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT, HandbookItem.getColor(this.minecraft.player));
		extractPageEdges(graphics, this.left, this.top, this.tab);
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		if (this.tab == HandbookTab.ATLAS) {
			this.extractSeam(graphics);
		}

		if (this.recipes != null) {
			this.recipes.extractRenderState(graphics, mouseX, mouseY);
		}

		if (this.notes != null) {
			this.notes.extractRenderState(graphics);
		}
	}

	public static void extractPageEdges(final GuiGraphicsExtractor graphics, final int left, final int top, final HandbookTab tab) {
		int edges = 0;
		int before = 0;
		for (HandbookTab other : HandbookTab.values()) {
			if (other.isAvailable()) {
				if (other.ordinal() < tab.ordinal()) {
					before++;
				}

				edges++;
			}
		}

		for (int i = 0; i < edges; i++) {
			int x = i < before ? left + 12 + i * 2 : left + 288 - (edges - 1 - i) * 2;
			graphics.fill(x, top + 18, x + 1, top + 186, PAGE_EDGE_COLOR);
		}
	}

	private void extractSeam(final GuiGraphicsExtractor graphics) {
		int seam = this.left + SEAM_X;
		for (int i = 0; i < SEAM_SHADOW; i++) {
			int color = (SEAM_SHADOW - i) * 12 << 24 | 0x503820;
			graphics.fill(seam - i - 1, this.top + 19, seam - i, this.top + 185, color);
			graphics.fill(seam + i, this.top + 19, seam + i + 1, this.top + 185, color);
		}
	}

	@Override
	public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
		Vector2i wheel = this.scrollWheelHandler.onMouseScroll(scrollX, scrollY);
		for (int i = 0; i < Math.abs(wheel.y); i++) {
			super.mouseScrolled(x, y, 0.0, Math.signum(wheel.y));
		}

		return true;
	}

	@Override
	public boolean keyPressed(final KeyEvent event) {
		for (HandbookTab other : HandbookTab.values()) {
			if (other != this.tab && HandbookKeyMappings.getTabKey(other).matches(event)) {
				select(other, this.parent);
				return true;
			}
		}

		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		if (this.tab == HandbookTab.RECIPES) {
			PageSnapshot.capture(this.tab, this.left, this.top);
		}

		ClientNotebook.send();
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
