package com.khrux.handbook.client.compat;

import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.client.gui.screens.BookScreen;
import com.evandev.fieldguide.client.gui.screens.FieldGuideCategoryScreen;
import com.khrux.handbook.client.gui.components.HandbookTabButton;
import com.khrux.handbook.client.gui.screens.HandbookScreen;
import com.khrux.handbook.client.gui.screens.HandbookTab;
import com.khrux.handbook.client.renderer.PageSnapshot;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class FieldGuideTab {
	public static void bootstrap() {
		ScreenEvents.BEFORE_INIT.register(FieldGuideTab::beforeInit);
		ScreenEvents.AFTER_INIT.register(FieldGuideTab::afterInit);
	}

	public static void open() {
		Minecraft.getInstance().gui.setScreen(new FieldGuideCategoryScreen());
	}

	private static void beforeInit(final Minecraft minecraft, final Screen screen, final int width, final int height) {
		ClientConfig.get().showInventoryButton = false;
		ClientConfig.get().showPauseMenuButton = false;
		if (screen instanceof BookScreen) {
			ClientFieldGuideManager.getCategories().values().removeIf(category -> category.getId().getPath().equals("intro"));
		}
	}

	private static void afterInit(final Minecraft minecraft, final Screen screen, final int width, final int height) {
		if (screen instanceof BookScreen) {
			int left = (width - HandbookScreen.WIDTH) / 2;
			int top = (height - HandbookScreen.HEIGHT) / 2;
			HandbookTabButton.addMainTabs(left + HandbookScreen.COVER_LEFT, top + HandbookScreen.COVER_TOP, HandbookTab.FIELD_GUIDE, Screens.getWidgets(screen)::add);
			ScreenEvents.remove(screen).register(current -> PageSnapshot.capture(HandbookTab.FIELD_GUIDE, left, top));
			ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, a) -> HandbookScreen.extractPageEdges(graphics, left, top, HandbookTab.FIELD_GUIDE));
		}
	}
}
