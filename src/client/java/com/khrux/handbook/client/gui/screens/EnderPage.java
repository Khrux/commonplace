package com.khrux.handbook.client.gui.screens;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.ClientHandbook;
import com.khrux.handbook.client.ClientPassphrases;
import com.khrux.handbook.client.gui.components.DyeSwatches;
import com.khrux.handbook.client.gui.components.EnderInk;
import com.khrux.handbook.client.gui.components.HandbookTabButton;
import com.khrux.handbook.client.gui.components.InkButton;
import com.khrux.handbook.network.protocol.PassphraseSlotsPayload;
import com.khrux.handbook.world.entity.player.PassphraseSlot;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

public class EnderPage {
	public static final int STARS_X = 13;
	public static final int STARS_Y = 20;
	public static final int STARS_WIDTH = 274;
	public static final int STARS_HEIGHT = 164;
	private static final int PUPIL_COLOR = 0xFFD8FFF4;
	private static final int OPTION_HOLLOW_COLOR = 0xFFB2B0AA;
	private static final int FOLLOWER_ROWS = 10;
	private static final int TABS_TOP = 20;
	private static final int TAB_SPACING = 25;
	private static final int TAB_HEIGHT = 24;
	private static final int TAB_REACH = 20;
	private static final double DRAG_START = 4.0;
	private static final Identifier PAGE_LOCATION = Handbook.id("textures/gui/ender_page.png");
	private static final Identifier PASSPHRASE_ICON = Handbook.id("textures/gui/icon/passphrase.png");
	private static final String[] DRAFTS = new String[PassphraseSlot.SLOTS];
	private static int selected;
	private static int followerPage;
	private static int pressedSlot = -1;
	private static double pressX;
	private static double pressY;
	private static boolean dragging;
	private static double dragX;
	private static double dragY;
	private final Font font = Minecraft.getInstance().font;
	private final HandbookScreen screen;
	private final int left;
	private final int top;
	private @Nullable EditBox passphraseBox;
	private @Nullable InkButton followButton;
	private @Nullable InkButton leaveButton;
	private @Nullable InkButton shareButton;
	private @Nullable PageButton backButton;
	private @Nullable PageButton forwardButton;

	public EnderPage(final HandbookScreen screen, final int left, final int top) {
		this.screen = screen;
		this.left = left;
		this.top = top;
	}

	public static Component getPullOut(final PassphraseSlot slot) {
		if (slot.isEmpty()) {
			return Component.translatable("handbook.ender.empty_slot");
		}

		String passphrase = ClientPassphrases.getPassphrase(slot.hash());
		return passphrase == null ? Component.translatable("handbook.ender.hidden_passphrase") : Component.literal(passphrase);
	}

	public static int getSlotColor(final PassphraseSlot slot) {
		int color = DyeColor.byId(slot.color()).getTextureDiffuseColor();
		return slot.isEmpty() ? ARGB.color(110, color) : color;
	}

	public void init(final Consumer<AbstractWidget> widgets) {
		for (int i = 0; i < PassphraseSlot.SLOTS; i++) {
			PassphraseSlot slot = ClientHandbook.get().passphrases().get(i).slot();
			int index = i;
			widgets.accept(HandbookTabButton.passphrase(
				this.left + HandbookScreen.COVER_LEFT, this.top + TABS_TOP + i * TAB_SPACING, i == selected, true, PASSPHRASE_ICON, getSlotColor(slot), getPullOut(slot), () -> {
					selected = index;
					followerPage = 0;
					this.screen.rebuild();
				}
			));
		}

		this.passphraseBox = new EditBox(this.font, this.left + 27, this.top + 51, 108, 10, Component.translatable("handbook.ender.passphrase"));
		this.passphraseBox.setMaxLength(PassphraseSlot.MAX_PASSPHRASE_LENGTH);
		this.passphraseBox.setBordered(false);
		this.passphraseBox.setTextColor(EnderInk.color(this.left + 27, this.top + 51));
		this.passphraseBox.setTextShadow(false);
		this.passphraseBox.setValue(this.getDraft());
		this.passphraseBox.setResponder(value -> DRAFTS[selected] = value);
		widgets.accept(this.passphraseBox);
		this.followButton = new InkButton(this.left + 26, this.top + 65, Component.translatable("handbook.ender.follow"), this::follow);
		widgets.accept(this.followButton);
		Component leave = Component.translatable("handbook.ender.leave");
		this.leaveButton = new InkButton(this.left + 138 - this.font.width(leave), this.top + 65, leave, () -> {
			DRAFTS[selected] = "";
			ClientPassphrases.follow(selected, "");
			this.screen.rebuild();
		});
		widgets.accept(this.leaveButton);
		widgets.accept(new DyeSwatches(this.left + 29, this.top + 86, () -> this.getSlot().color(), color -> ClientPassphrases.changeSettings(selected, color, this.getSlot().enderInk())));
		for (boolean enderInk : List.of(true, false)) {
			InkButton button = new InkButton(
				this.left + 34,
				this.top + (enderInk ? 131 : 142),
				Component.translatable(enderInk ? "handbook.ender.ender_ink" : "handbook.ender.plain_ink"),
				() -> ClientPassphrases.changeSettings(selected, this.getSlot().color(), enderInk)
			);
			button.setTooltip(Tooltip.create(Component.translatable(enderInk ? "handbook.ender.ender_ink.description" : "handbook.ender.plain_ink.description")));
			widgets.accept(button);
		}

		Component share = Component.translatable("handbook.ender.share_plain_ink");
		this.shareButton = new InkButton(this.left + 81 - this.font.width(share) / 2, this.top + 158, share, () -> ClientPassphrases.sharePlainInk(selected));
		this.shareButton.setTooltip(Tooltip.create(Component.translatable("handbook.ender.share_plain_ink.description")));
		widgets.accept(this.shareButton);
		this.backButton = new PageButton(this.left + 194, this.top + 163, false, button -> followerPage--, true);
		widgets.accept(this.backButton);
		this.forwardButton = new PageButton(this.left + 244, this.top + 163, true, button -> followerPage++, true);
		widgets.accept(this.forwardButton);
		this.tick();
	}

	private PassphraseSlot getSlot() {
		return ClientHandbook.get().passphrases().get(selected).slot();
	}

	private String getDraft() {
		if (DRAFTS[selected] != null) {
			return DRAFTS[selected];
		}

		PassphraseSlot slot = this.getSlot();
		String passphrase = slot.isEmpty() ? null : ClientPassphrases.getPassphrase(slot.hash());
		return passphrase == null ? "" : passphrase;
	}

	private void follow() {
		if (this.passphraseBox == null || this.passphraseBox.getValue().isEmpty()) {
			return;
		}

		ClientPassphrases.follow(selected, this.passphraseBox.getValue());
		DRAFTS[selected] = null;
		followerPage = 0;
		this.passphraseBox.setFocused(false);
	}

	public boolean keyPressed(final KeyEvent event) {
		if (this.passphraseBox != null && this.passphraseBox.isFocused() && (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)) {
			this.follow();
			return true;
		}

		return false;
	}

	public void tick() {
		PassphraseSlotsPayload.SlotView view = ClientHandbook.get().passphrases().get(selected);
		PassphraseSlot slot = view.slot();
		String value = this.passphraseBox == null ? "" : this.passphraseBox.getValue();
		this.followButton.visible = !value.isEmpty() && !ClientPassphrases.hash(value).equals(slot.hash());
		this.leaveButton.visible = !slot.isEmpty();
		this.shareButton.visible = !slot.isEmpty();
		int pages = Math.max(1, (view.followers().size() + FOLLOWER_ROWS - 1) / FOLLOWER_ROWS);
		followerPage = Math.clamp(followerPage, 0, pages - 1);
		this.backButton.visible = followerPage > 0;
		this.forwardButton.visible = followerPage < pages - 1;
		this.passphraseBox.setTextColor(EnderInk.color(this.left + 27, this.top + 51));
	}

	private int slotAt(final double x, final double y) {
		int tabLeft = this.left + HandbookScreen.COVER_LEFT - TAB_REACH;
		int offset = (int)Math.floor(y - this.top - TABS_TOP);
		if (x < tabLeft || x >= this.left + HandbookScreen.COVER_LEFT + 8 || offset < 0) {
			return -1;
		}

		int index = offset / TAB_SPACING;
		return index < PassphraseSlot.SLOTS && offset % TAB_SPACING < TAB_HEIGHT ? index : -1;
	}

	public void mouseClicked(final MouseButtonEvent event) {
		int index = event.button() == InputConstants.MOUSE_BUTTON_LEFT ? this.slotAt(event.x(), event.y()) : -1;
		pressedSlot = index;
		pressX = event.x();
		pressY = event.y();
		dragging = false;
	}

	public boolean mouseDragged(final MouseButtonEvent event) {
		if (pressedSlot < 0) {
			return false;
		}

		if (!dragging && Math.abs(event.x() - pressX) + Math.abs(event.y() - pressY) > DRAG_START) {
			dragging = true;
		}

		dragX = event.x();
		dragY = event.y();
		return dragging;
	}

	public boolean mouseReleased(final MouseButtonEvent event) {
		int from = pressedSlot;
		boolean dropped = dragging;
		pressedSlot = -1;
		dragging = false;
		if (from < 0 || !dropped) {
			return false;
		}

		int to = this.slotAt(event.x(), event.y());
		if (to >= 0 && to != from) {
			ClientPassphrases.swap(from, to);
			String draft = DRAFTS[from];
			DRAFTS[from] = DRAFTS[to];
			DRAFTS[to] = draft;
			selected = to;
			this.screen.rebuild();
		}

		return true;
	}

	private void extractDrag(final GuiGraphicsExtractor graphics) {
		if (!dragging || pressedSlot < 0) {
			return;
		}

		int target = this.slotAt(dragX, dragY);
		if (target >= 0 && target != pressedSlot) {
			int tabLeft = this.left + HandbookScreen.COVER_LEFT - TAB_REACH;
			int tabTop = this.top + TABS_TOP + target * TAB_SPACING;
			EnderInk.ring(graphics, tabLeft + 14, tabTop + 12, 11.0F, EnderInk.color(tabLeft, tabTop));
		}

		int color = ARGB.color(200, getSlotColor(ClientHandbook.get().passphrases().get(pressedSlot).slot()));
		graphics.blit(RenderPipelines.GUI_TEXTURED, PASSPHRASE_ICON, (int)dragX - 8, (int)dragY - 8, 0.0F, 0.0F, 16, 16, 16, 16, color);
	}

	public void extractBackground(final GuiGraphicsExtractor graphics) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, PAGE_LOCATION, this.left, this.top, 0.0F, 0.0F, HandbookScreen.WIDTH, HandbookScreen.HEIGHT, HandbookScreen.WIDTH, HandbookScreen.HEIGHT);
		EnderInk.stars(graphics, this.left + STARS_X, this.top + STARS_Y, STARS_WIDTH, STARS_HEIGHT, this.left + HandbookScreen.SEAM_X);
	}

	public void extractRenderState(final GuiGraphicsExtractor graphics) {
		this.extractPage(graphics);
		this.extractDrag(graphics);
	}

	private void extractPage(final GuiGraphicsExtractor graphics) {
		PassphraseSlotsPayload.SlotView view = ClientHandbook.get().passphrases().get(selected);
		PassphraseSlot slot = view.slot();
		EnderInk.centeredText(graphics, this.font, Component.translatable("handbook.ender.slot", selected + 1), this.left + 80, this.top + 30);
		if (this.passphraseBox != null && this.passphraseBox.getValue().isEmpty() && !this.passphraseBox.isFocused()) {
			Component hint = slot.isEmpty() ? Component.translatable("handbook.ender.passphrase_hint") : Component.translatable("handbook.ender.hidden_passphrase");
			EnderInk.text(graphics, this.font, hint, this.left + 27, this.top + 51, 110);
		}

		if (this.leaveButton.visible) {
			int start = this.followButton.visible ? this.followButton.getX() + this.followButton.getWidth() + 4 : this.left + 24;
			int end = this.leaveButton.getX() - 4;
			int y = this.top + 69;
			graphics.fill(start, y, end, y + 1, EnderInk.color(start, y, 200));
			graphics.fill(end - 2, y - 2, end - 1, y - 1, EnderInk.color(end, y));
			graphics.fill(end - 1, y - 1, end, y, EnderInk.color(end, y));
			graphics.fill(end - 1, y + 1, end, y + 2, EnderInk.color(end, y));
			graphics.fill(end - 2, y + 2, end - 1, y + 3, EnderInk.color(end, y));
		}

		EnderInk.text(graphics, this.font, Component.translatable("handbook.ender.color"), this.left + 22, this.top + 77, 255);
		EnderInk.text(graphics, this.font, Component.translatable("handbook.ender.ink"), this.left + 22, this.top + 121, 255);
		this.extractOption(graphics, this.left + 27, this.top + 136, slot.enderInk());
		this.extractOption(graphics, this.left + 27, this.top + 147, !slot.enderInk());
		EnderInk.centeredText(graphics, this.font, Component.translatable("handbook.ender.followers", view.followers().size()), this.left + 220, this.top + 30);
		if (view.followers().isEmpty()) {
			int y = this.top + 50;
			for (FormattedCharSequence line : this.font.split(Component.translatable("handbook.ender.no_followers"), 120)) {
				graphics.text(this.font, line, this.left + 163, y, EnderInk.color(this.left + 163, y, 150), false);
				y += 10;
			}

			return;
		}

		List<PassphraseSlotsPayload.FollowerView> followers = view.followers();
		int start = followerPage * FOLLOWER_ROWS;
		for (int i = start; i < Math.min(followers.size(), start + FOLLOWER_ROWS); i++) {
			PassphraseSlotsPayload.FollowerView follower = followers.get(i);
			int y = this.top + 50 + (i - start) * 11;
			this.extractInkMark(graphics, this.left + 167, y + 3, follower.enderInk());
			EnderInk.text(graphics, this.font, Component.literal(follower.name()), this.left + 176, y, follower.online() ? 255 : 110);
		}

		if (followers.size() > FOLLOWER_ROWS) {
			EnderInk.centeredText(graphics, this.font, Component.literal(followerPage + 1 + "/" + ((followers.size() + FOLLOWER_ROWS - 1) / FOLLOWER_ROWS)), this.left + 231, this.top + 166);
		}
	}

	private void extractOption(final GuiGraphicsExtractor graphics, final int x, final int y, final boolean chosen) {
		EnderInk.diamond(graphics, x, y, 3, EnderInk.color(x, y, chosen ? 255 : 150));
		if (!chosen) {
			EnderInk.diamond(graphics, x, y, 2, OPTION_HOLLOW_COLOR);
		} else {
			graphics.fill(x, y, x + 1, y + 1, PUPIL_COLOR);
		}
	}

	private void extractInkMark(final GuiGraphicsExtractor graphics, final int x, final int y, final boolean enderInk) {
		if (enderInk) {
			EnderInk.diamond(graphics, x, y, 3, EnderInk.color(x, y));
			graphics.fill(x, y, x + 1, y + 1, PUPIL_COLOR);
		} else {
			EnderInk.ring(graphics, x, y, 3.0F, EnderInk.color(x, y, 190));
		}
	}

}
