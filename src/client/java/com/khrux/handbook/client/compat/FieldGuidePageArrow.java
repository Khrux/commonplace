package com.khrux.handbook.client.compat;

import com.evandev.fieldguide.client.gui.widget.PageTurnButton;
import com.khrux.handbook.client.gui.screens.RecipesPage;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.sounds.SoundManager;

public class FieldGuidePageArrow extends ImageButton {
	public static final int WIDTH = 12;
	public static final int HEIGHT = 17;
	private final PageTurnButton page;

	public FieldGuidePageArrow(final PageTurnButton page, final boolean forward, final int y) {
		super(page.getX() + (page.getWidth() - WIDTH) / 2, y, WIDTH, HEIGHT, forward ? RecipesPage.PAGE_FORWARD_SPRITES : RecipesPage.PAGE_BACKWARD_SPRITES, button -> {});
		this.page = page;
	}

	public void sync() {
		this.visible = this.page.visible;
		this.active = this.page.active;
	}

	@Override
	public void onPress(final InputWithModifiers input) {
		this.page.onPress(input);
	}

	@Override
	public void playDownSound(final SoundManager soundManager) {
		this.page.playDownSound(soundManager);
	}
}
