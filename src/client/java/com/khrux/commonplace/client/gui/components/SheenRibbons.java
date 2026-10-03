package com.khrux.commonplace.client.gui.components;

import com.khrux.commonplace.client.ClientHandbook;
import com.khrux.commonplace.client.ClientPassphrases;
import com.khrux.commonplace.client.ClientSheen;
import com.khrux.commonplace.client.gui.screens.EnderPage;
import com.khrux.commonplace.client.gui.screens.HandbookTab;
import com.khrux.commonplace.world.entity.player.PassphraseSlot;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public class SheenRibbons extends AbstractWidget {
	private static final int RIBBON_WIDTH = 7;
	private static final int RIBBON_SPACING = 11;
	private static final int RIBBON_LENGTH = 7;
	private static final int SELECTED_LENGTH = 11;
	private static final int EDGE_COLOR = 0x60000000;

	private SheenRibbons(final int x, final int y) {
		super(x, y, PassphraseSlot.SLOTS * RIBBON_SPACING, SELECTED_LENGTH, Component.translatable("commonplace.ender.sheen"));
	}

	public static void add(final int bookLeft, final int bookTop, final Consumer<AbstractWidget> widgets) {
		if (HandbookTab.ENDER.isAvailable()) {
			widgets.accept(new SheenRibbons(bookLeft + 222, bookTop + 191));
		}
	}

	@Override
	protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		int hovered = this.slotAt(mouseX, mouseY);
		for (int i = 0; i < PassphraseSlot.SLOTS; i++) {
			PassphraseSlot slot = ClientHandbook.get().passphrases().get(i).slot();
			if (slot.isEmpty()) {
				continue;
			}

			int x = this.getX() + i * RIBBON_SPACING;
			int length = ClientHandbook.get().sheen().getHighlighted() == i ? SELECTED_LENGTH : RIBBON_LENGTH;
			int color = ARGB.opaque(EnderPage.getSlotColor(slot));
			int bottom = this.getY() + length;
			graphics.fill(x, this.getY(), x + RIBBON_WIDTH, bottom - 2, color);
			graphics.fill(x, bottom - 2, x + 2, bottom, color);
			graphics.fill(x + RIBBON_WIDTH - 2, bottom - 2, x + RIBBON_WIDTH, bottom, color);
			graphics.fill(x + RIBBON_WIDTH - 1, this.getY(), x + RIBBON_WIDTH, bottom - 2, EDGE_COLOR);
			if (hovered == i) {
				graphics.setTooltipForNextFrame(Minecraft.getInstance().font, EnderPage.getPullOut(slot), mouseX, mouseY);
			}
		}
	}

	private int slotAt(final double mouseX, final double mouseY) {
		if (mouseX < this.getX() || mouseY < this.getY() || mouseX >= this.getX() + this.width || mouseY >= this.getY() + this.height) {
			return -1;
		}

		int index = (int)(mouseX - this.getX()) / RIBBON_SPACING;
		return index < PassphraseSlot.SLOTS && !ClientHandbook.get().passphrases().get(index).slot().isEmpty() ? index : -1;
	}

	@Override
	public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
		int index = this.slotAt(event.x(), event.y());
		if (index >= 0) {
			ClientHandbook.get().sheen().toggleHighlight(index);
		}
	}

	@Override
	protected void updateWidgetNarration(final NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
