package com.khrux.commonplace.client;

import com.khrux.commonplace.CommonplaceConfig;
import com.khrux.commonplace.client.gui.screens.HandbookScreen;
import com.khrux.commonplace.client.gui.screens.HandbookTab;
import com.khrux.commonplace.client.renderer.HeldHandbookRenderer;
import com.khrux.commonplace.world.entity.player.CommonplaceAttachmentTypes;
import com.khrux.commonplace.world.item.CommonplaceItems;
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
			KeyMapping key = CommonplaceKeyMappings.getTabKey(tab);
			while (key.consumeClick()) {
				if (minecraft.player != null && hasHandbook(minecraft.player)) {
					HandbookScreen.select(tab);
				}
			}
		}
	}

	private static boolean hasHandbook(final LocalPlayer player) {
		return !player.getAttachedOrElse(CommonplaceAttachmentTypes.HANDBOOK_SLOT, ItemStack.EMPTY).isEmpty()
			|| player.getInventory().contains(itemStack -> itemStack.is(CommonplaceItems.HANDBOOK));
	}

	private static boolean isTabScrollKeyDown() {
		return switch (CommonplaceConfig.get().tabScrollKey) {
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
				|| !player.getMainHandItem().is(CommonplaceItems.HANDBOOK) && !player.getOffhandItem().is(CommonplaceItems.HANDBOOK)
		) {
			return false;
		}

		int direction = yoffset < 0.0 ? 1 : -1;
		HandbookTab current = HandbookScreen.getLastTab();
		HandbookTab next = current.step(direction);
		if (next != current) {
			HandbookScreen.setLastTab(next);
			HeldHandbookRenderer.turnPage(current, direction);
			if (CommonplaceConfig.get().pageTurnSound) {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
			}
		}

		return true;
	}
}
