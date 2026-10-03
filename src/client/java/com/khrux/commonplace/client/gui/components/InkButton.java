package com.khrux.commonplace.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public class InkButton extends AbstractButton {
	private final Runnable onPress;

	public InkButton(final int x, final int y, final Component message, final Runnable onPress) {
		super(x, y, Minecraft.getInstance().font.width(message), 10, message);
		this.onPress = onPress;
	}

	@Override
	public void onPress(final InputWithModifiers input) {
		this.onPress.run();
	}

	@Override
	protected void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		EnderInk.text(graphics, Minecraft.getInstance().font, this.getMessage(), this.getX(), this.getY() + 1, 255);
		if (this.isHoveredOrFocused()) {
			EnderInk.wavyLine(graphics, this.getX(), this.getX() + this.width, this.getY() + 10, 200);
		}
	}

	@Override
	protected void updateWidgetNarration(final NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
