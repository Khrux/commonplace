package com.khrux.handbook.client.gui.screens;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.ClientHandbook;
import com.khrux.handbook.client.ClientNotebook;
import com.khrux.handbook.client.ClientPassphrases;
import com.khrux.handbook.client.HandbookKeyMappings;
import com.khrux.handbook.client.compat.FieldGuideTab;
import com.khrux.handbook.client.gui.components.AtlasMap;
import com.khrux.handbook.client.gui.components.HandbookTabButton;
import com.khrux.handbook.client.gui.components.SheenRibbons;
import com.khrux.handbook.client.renderer.PageSnapshot;
import com.khrux.handbook.world.item.HandbookItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
	public static final Identifier ENDER_BOOK_LOCATION = Handbook.id("textures/gui/book_ender.png");
	public static final int SEAM_X = 150;
	private static final int PAGE_EDGE_COLOR = 0xFFA07A48;
	private static final int SEAM_SHADOW = 6;
	private static HandbookTab lastTab = HandbookTab.RECIPES;
	private final @Nullable Screen parent;
	private final HandbookTab tab;
	private final ScrollWheelHandler scrollWheelHandler = new ScrollWheelHandler();
	private @Nullable RecipesPage recipes;
	private @Nullable NotesPage notes;
	private @Nullable EnderPage ender;
	private int left;
	private int top;
	private int passphraseVersion;

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
		Runnable open = chosen == HandbookTab.FIELD_GUIDE ? FieldGuideTab::open : () -> Minecraft.getInstance().gui.setScreen(new HandbookScreen(parent, chosen));
		Screen current = Minecraft.getInstance().gui.screen();
		if (current instanceof HandbookScreen screen && screen.hasSnapshot()) {
			PageSnapshot.leave(screen.tab, screen.left, screen.top, open);
		} else if (HandbookTab.FIELD_GUIDE.isAvailable() && FieldGuideTab.isGuideScreen(current)) {
			FieldGuideTab.leave(current, open);
		} else {
			open.run();
		}
	}

	private boolean hasSnapshot() {
		return this.tab == HandbookTab.RECIPES || this.tab == HandbookTab.ENDER;
	}

	@Override
	protected void init() {
		this.passphraseVersion = ClientHandbook.get().passphrases().getVersion();
		this.recipes = null;
		this.notes = null;
		this.ender = null;
		this.left = (this.width - WIDTH) / 2;
		this.top = (this.height - HEIGHT) / 2;
		HandbookTabButton.addMainTabs(this.left + COVER_LEFT, this.top + COVER_TOP, this.tab, this::addRenderableWidget);
		SheenRibbons.add(this.left, this.top, this::addRenderableWidget);
		if (this.tab == HandbookTab.ATLAS) {
			this.addRenderableWidget(new AtlasMap(this.left + 17, this.top + 19, 267, 166));
		} else if (this.tab == HandbookTab.RECIPES) {
			this.recipes = new RecipesPage(this::rebuild, this.left, this.top, null);
			this.recipes.init(this::addRenderableWidget);
		} else if (this.tab == HandbookTab.NOTES) {
			this.notes = new NotesPage(this, this.left, this.top);
			this.notes.init(this::addRenderableWidget);
		} else if (this.tab == HandbookTab.ENDER) {
			this.ender = new EnderPage(this, this.left, this.top);
			this.ender.init(this::addRenderableWidget);
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
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.tab == HandbookTab.ENDER ? ENDER_BOOK_LOCATION : BOOK_LOCATION, this.left, this.top, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
		graphics.blit(RenderPipelines.GUI_TEXTURED, COVER_LOCATION, this.left, this.top, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT, HandbookItem.getColor(this.minecraft.player));
		extractPageEdges(graphics, this.left, this.top, this.tab);
		if (this.ender != null) {
			this.ender.extractBackground(graphics);
		}
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

		if (this.ender != null) {
			this.ender.extractRenderState(graphics);
		}

		PageSnapshot.frameExtracted();
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
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		if (this.ender != null) {
			this.ender.mouseClicked(event);
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
		return this.ender != null && this.ender.mouseDragged(event) || super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(final MouseButtonEvent event) {
		return this.ender != null && this.ender.mouseReleased(event) || super.mouseReleased(event);
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
		if (this.ender != null && this.ender.keyPressed(event)) {
			return true;
		}

		if (this.getFocused() instanceof EditBox || this.getFocused() instanceof MultiLineEditBox) {
			return super.keyPressed(event);
		}

		for (HandbookTab other : HandbookTab.values()) {
			if (other != this.tab && HandbookKeyMappings.getTabKey(other).matches(event)) {
				select(other, this.parent);
				return true;
			}
		}

		return super.keyPressed(event);
	}

	@Override
	public void tick() {
		if (this.passphraseVersion != ClientHandbook.get().passphrases().getVersion()) {
			this.rebuild();
			return;
		}

		if (this.ender != null) {
			this.ender.tick();
		}

		if (this.notes != null) {
			this.notes.tick();
		}
	}

	@Override
	public void removed() {
		if (this.hasSnapshot()) {
			PageSnapshot.capture(this.tab, this.left, this.top);
		}

		ClientHandbook.get().notebook().flush();
	}

	@Override
	public void onClose() {
		if (this.hasSnapshot()) {
			PageSnapshot.leave(this.tab, this.left, this.top, () -> this.minecraft.gui.setScreen(this.parent));
		} else {
			this.minecraft.gui.setScreen(this.parent);
		}
	}
}
