package com.khrux.handbook.client;

import com.khrux.handbook.HandbookConfig;
import com.khrux.handbook.client.gui.screens.HandbookScreen;
import com.khrux.handbook.client.gui.screens.HandbookTab;
import com.khrux.handbook.client.renderer.HeldHandbookRenderer;
import com.khrux.handbook.world.entity.player.HandbookAttachmentTypes;
import com.khrux.handbook.world.item.HandbookItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public class HandbookTabControls {
	public static void bootstrap() {
		ClientTickEvents.END_CLIENT_TICK.register(HandbookTabControls::tick);
	}

	private static void tick(final Minecraft minecraft) {
		for (HandbookTab tab : HandbookTab.values()) {
			KeyMapping key = HandbookKeyMappings.getTabKey(tab);
			while (key.consumeClick()) {
				if (minecraft.player != null && hasHandbook(minecraft.player)) {
					HandbookScreen.select(tab);
				}
			}
		}
	}

	private static boolean hasHandbook(final LocalPlayer player) {
		return !player.getAttachedOrElse(HandbookAttachmentTypes.HANDBOOK_SLOT, ItemStack.EMPTY).isEmpty()
			|| player.getInventory().contains(itemStack -> itemStack.is(HandbookItems.HANDBOOK));
	}

	private static boolean isTabScrollKeyDown() {
		return switch (HandbookConfig.get().tabScrollKey) {
			case ALT -> InputConstants.isKeyDown(InputConstants.KEY_LALT) || InputConstants.isKeyDown(InputConstants.KEY_RALT);
			case CONTROL -> InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
			case SHIFT -> InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
			case OFF -> false;
		};
	}

	public static boolean scroll(final Minecraft minecraft, final double yoffset) {
		LocalPlayer player = minecraft.player;
		if (
			yoffset == 0.0
				|| !isTabScrollKeyDown()
				|| !player.getMainHandItem().is(HandbookItems.HANDBOOK) && !player.getOffhandItem().is(HandbookItems.HANDBOOK)
		) {
			return false;
		}

		int direction = yoffset < 0.0 ? 1 : -1;
		HandbookTab current = HandbookScreen.getLastTab();
		HandbookTab next = current.step(direction);
		if (next != current) {
			HandbookScreen.setLastTab(next);
			HeldHandbookRenderer.turnPage(current, direction);
			if (HandbookConfig.get().pageTurnSound) {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
			}
		}

		return true;
	}
}
