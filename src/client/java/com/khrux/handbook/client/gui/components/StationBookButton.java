package com.khrux.handbook.client.gui.components;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.world.item.HandbookItem;
import com.khrux.handbook.world.item.HandbookItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public class StationBookButton extends AbstractButton {
	private static final Identifier SPRITE = Handbook.id("slot");
	private static final Component NAME = Component.translatable("handbook.recipes.station");
	private final Runnable onPress;
	private final ItemStack icon = new ItemStack(HandbookItems.HANDBOOK);

	public StationBookButton(final int x, final int y, final Runnable onPress) {
		super(x, y, 20, 18, NAME);
		this.onPress = onPress;
		this.icon.set(DataComponents.DYED_COLOR, new DyedItemColor(HandbookItem.getColor(Minecraft.getInstance().player)));
		this.setTooltip(Tooltip.create(NAME));
	}

	@Override
	public void onPress(final InputWithModifiers input) {
		this.onPress.run();
	}

	@Override
	protected void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, this.getX(), this.getY(), this.width, this.height);
		graphics.item(this.icon, this.getX() + 2, this.getY() + 1);
		if (this.isHoveredOrFocused()) {
			graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, 0x303F2A1C);
		}
	}

	@Override
	protected void updateWidgetNarration(final NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
