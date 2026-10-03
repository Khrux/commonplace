package com.khrux.commonplace.client;

import com.khrux.commonplace.world.entity.player.CommonplaceAttachmentTypes;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SpyglassZoom {
	private static boolean zooming;

	public static void bootstrap() {
		ClientTickEvents.END_CLIENT_TICK.register(SpyglassZoom::tick);
	}

	public static boolean isZooming() {
		return zooming;
	}

	private static void tick(final Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		boolean zoom = player != null
			&& minecraft.gui.screen() == null
			&& CommonplaceKeyMappings.SPYGLASS_ZOOM.isDown()
			&& player.getAttachedOrElse(CommonplaceAttachmentTypes.SPYGLASS_SLOT, ItemStack.EMPTY).is(Items.SPYGLASS);
		if (zoom != zooming && player != null) {
			player.playSound(zoom ? SoundEvents.SPYGLASS_USE : SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
		}

		zooming = zoom;
	}
}
