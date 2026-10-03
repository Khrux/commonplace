package com.khrux.commonplace.client.compat;

import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.gui.screens.BookScreen;
import com.evandev.fieldguide.client.gui.screens.FieldGuideCategoryScreen;
import com.evandev.fieldguide.client.gui.widget.BookTextAreaWidget;
import com.evandev.fieldguide.client.gui.widget.PageTurnButton;
import com.evandev.fieldguide.config.ClientConfig;
import com.khrux.commonplace.client.ClientHandbook;
import com.khrux.commonplace.client.ClientSheen;
import com.khrux.commonplace.client.gui.components.HandbookTabButton;
import com.khrux.commonplace.client.gui.components.SheenRibbons;
import com.khrux.commonplace.client.gui.screens.HandbookScreen;
import com.khrux.commonplace.client.gui.screens.HandbookTab;
import com.khrux.commonplace.client.renderer.PageSnapshot;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

public class FieldGuideTab {
	private static final int PAGE_ARROW_Y = 166;
	private static final int PAGE_BUTTON_BELOW = 150;
	private static int sketchedVersion;
	private static @Nullable BookTextAreaWidget typing;
	private static int refreshCooldown;

	public static void bootstrap() {
		ScreenEvents.BEFORE_INIT.register(FieldGuideTab::beforeInit);
		ScreenEvents.AFTER_INIT.register(FieldGuideTab::afterInit);
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			if (refreshCooldown > 0) {
				refreshCooldown--;
			} else if (sketchedVersion != ClientHandbook.get().sheen().getEntryVersion()) {
				sketchedVersion = ClientHandbook.get().sheen().getEntryVersion();
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

	private static List<FieldGuidePageArrow> replacePageButtons(final Screen screen, final int left, final int top) {
		List<AbstractWidget> widgets = Screens.getWidgets(screen);
		List<FieldGuidePageArrow> arrows = new ArrayList<>();
		for (AbstractWidget widget : List.copyOf(widgets)) {
			if (widget instanceof PageTurnButton page && page.getWidth() == 16 && page.getY() >= top + PAGE_BUTTON_BELOW) {
				widgets.remove(page);
				FieldGuidePageArrow arrow = new FieldGuidePageArrow(page, page.getX() > left + HandbookScreen.WIDTH / 2, top + PAGE_ARROW_Y);
				arrow.sync();
				widgets.add(arrow);
				arrows.add(arrow);
			}
		}

		return arrows;
	}

	private static void afterInit(final Minecraft minecraft, final Screen screen, final int width, final int height) {
		if (screen instanceof BookScreen) {
			int left = (width - HandbookScreen.WIDTH) / 2;
			int top = (height - HandbookScreen.HEIGHT) / 2;
			HandbookTabButton.addMainTabs(left + HandbookScreen.COVER_LEFT, top + HandbookScreen.COVER_TOP, HandbookTab.FIELD_GUIDE, Screens.getWidgets(screen)::add);
			SheenRibbons.add(left, top, Screens.getWidgets(screen)::add);
			List<FieldGuidePageArrow> arrows = replacePageButtons(screen, left, top);
			ScreenEvents.beforeExtract(screen).register((current, graphics, mouseX, mouseY, a) -> arrows.forEach(FieldGuidePageArrow::sync));
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
