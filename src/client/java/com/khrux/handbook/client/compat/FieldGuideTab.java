package com.khrux.handbook.client.compat;

import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.gui.screens.BookScreen;
import com.evandev.fieldguide.client.gui.screens.FieldGuideCategoryScreen;
import com.evandev.fieldguide.client.gui.widget.BookTextAreaWidget;
import com.evandev.fieldguide.config.ClientConfig;
import com.khrux.handbook.client.ClientSheen;
import com.khrux.handbook.client.gui.components.HandbookTabButton;
import com.khrux.handbook.client.gui.components.SheenRibbons;
import com.khrux.handbook.client.gui.screens.HandbookScreen;
import com.khrux.handbook.client.gui.screens.HandbookTab;
import com.khrux.handbook.client.renderer.PageSnapshot;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

public class FieldGuideTab {
	private static int sketchedVersion;
	private static @Nullable BookTextAreaWidget typing;
	private static int refreshCooldown;

	public static void bootstrap() {
		ScreenEvents.BEFORE_INIT.register(FieldGuideTab::beforeInit);
		ScreenEvents.AFTER_INIT.register(FieldGuideTab::afterInit);
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			if (refreshCooldown > 0) {
				refreshCooldown--;
			} else if (sketchedVersion != ClientSheen.getEntryVersion()) {
				sketchedVersion = ClientSheen.getEntryVersion();
				refreshCooldown = 20;
				FieldGuideSketch.refresh();
			}
		});
	}

	public static void open() {
		Minecraft.getInstance().gui.setScreen(new FieldGuideCategoryScreen());
	}

	private static void beforeInit(final Minecraft minecraft, final Screen screen, final int width, final int height) {
		ClientConfig.get().showInventoryButton = false;
		ClientConfig.get().showPauseMenuButton = false;
		if (screen instanceof BookScreen) {
			FieldGuideBook.apply(minecraft);
			ClientFieldGuideManager.getCategories().values().removeIf(category -> category.getId().getPath().equals("intro"));
		}
	}

	public static boolean isGuideScreen(final @Nullable Screen screen) {
		return screen instanceof BookScreen;
	}

	public static void leave(final Screen screen, final Runnable action) {
		PageSnapshot.leave(HandbookTab.FIELD_GUIDE, (screen.width - HandbookScreen.WIDTH) / 2, (screen.height - HandbookScreen.HEIGHT) / 2, action);
	}

	private static void syncTextInput(final Minecraft minecraft, final Screen screen) {
		BookTextAreaWidget focused = null;
		for (GuiEventListener child : screen.children()) {
			if (child instanceof BookTextAreaWidget area && area.isFocused() && area.isEditable()) {
				focused = area;
			}
		}

		setTyping(minecraft, focused);
	}

	private static void setTyping(final Minecraft minecraft, final @Nullable BookTextAreaWidget area) {
		if (typing == area) {
			return;
		}

		if (typing != null) {
			minecraft.onTextInputFocusChange(typing, false);
		}

		if (area != null) {
			minecraft.onTextInputFocusChange(area, true);
		}

		typing = area;
	}

	private static void afterInit(final Minecraft minecraft, final Screen screen, final int width, final int height) {
		if (screen instanceof BookScreen) {
			int left = (width - HandbookScreen.WIDTH) / 2;
			int top = (height - HandbookScreen.HEIGHT) / 2;
			HandbookTabButton.addMainTabs(left + HandbookScreen.COVER_LEFT, top + HandbookScreen.COVER_TOP, HandbookTab.FIELD_GUIDE, Screens.getWidgets(screen)::add);
			SheenRibbons.add(left, top, Screens.getWidgets(screen)::add);
			ScreenEvents.remove(screen).register(current -> {
				PageSnapshot.capture(HandbookTab.FIELD_GUIDE, left, top);
				setTyping(minecraft, null);
			});
			ScreenEvents.afterTick(screen).register(current -> syncTextInput(minecraft, current));
			ScreenKeyboardEvents.allowKeyPress(screen).register((current, event) -> {
				if (event.key() == InputConstants.KEY_ESCAPE && current.shouldCloseOnEsc()) {
					leave(current, current::onClose);
					return false;
				}

				return true;
			});
			ScreenMouseEvents.afterMouseClick(screen).register((current, event, consumed) -> {
				syncTextInput(minecraft, current);
				return consumed;
			});
			ScrollWheelHandler scrollWheelHandler = new ScrollWheelHandler();
			ScreenMouseEvents.allowMouseScroll(screen).register((current, mouseX, mouseY, scrollX, scrollY) -> {
				int wheel = scrollWheelHandler.onMouseScroll(scrollX, scrollY).y;
				for (int i = 0; i < Math.abs(wheel); i++) {
					current.mouseScrolled(mouseX, mouseY, 0.0, Math.signum(wheel));
				}

				return false;
			});
			ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, a) -> {
				HandbookScreen.extractPageEdges(graphics, left, top, HandbookTab.FIELD_GUIDE);
				PageSnapshot.frameExtracted();
			});
		}
	}
}
